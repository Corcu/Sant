package calypsox.tk.upload.uploader;

import calypsox.repoccp.ReconCCPConstants;
import calypsox.tk.ccp.ClearingTradeFilterAdapter;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.upload.mapper.repo.RepoOnAmendMapper;
import calypsox.tk.upload.validator.ReprocessKwdValidator;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;


/**
 *
 */
public class UploadCalypsoTradeRepo extends com.calypso.tk.upload.uploader.UploadCalypsoTradeRepo implements ClearingTradeFilterAdapter, ReprocessKwdValidator {


	public SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	public SimpleDateFormat sdfFullCouponDate = new SimpleDateFormat("MM-dd-yyyy");

	public static String KW_INITIAL_STUB = "InitialStub";
	public static String KW_FINAL_STUB = "FinalStub";
	public static String KW_STUB_PERIOD = "StubPeriod";
	public static String KW_START_DATE_LAST_STUB = "StartDateLastStub";
	public static String KW_FIRST_FIXING_CUSTOMIZED = "FirstFixingCustomized";
	public static String KW_FIRST_FIXING_RESET_DATE = "FirstFixingResetDate";
	public static String KW_FIRST_FIXING_PAYMENT_DATE = "FirstFixingPaymentDate";

	public static String SHORT_COUPON = "shortCoupon";
	public static String FULL_COUPON = "fullCoupon";
	public static String LONG_COUPON = "longCoupon";

	private static final String FO_SYSTEM_RV = "MRX3";

	private static final String GC_POOLING_KEYWORD = "isGCPooling";
	private static final String GC_POOLING = "GC_POOLING";
	private static final String KW_RE_RATE_METHOD = "ReRateMethod";

	@Override
	protected void beforeSave(CalypsoObject calypsoObject, Vector<BOException> errors) {
		if(Optional.ofNullable(this.trade).isPresent()
				&& Repo.SUBTYPE_TRIPARTY.equalsIgnoreCase(this.trade.getProductSubType())
				&& Action.NEW.equals(this.trade.getAction())
				&& this.trade.getLongId()<=0
				&& this.trade.getLongId()<=0
				&& !"true".equalsIgnoreCase(this.trade.getKeywordValue(GC_POOLING_KEYWORD)))
		{
			try {
				long tradeID = DSConnection.getDefault().getRemoteAccess().allocateLongSeed("trade", 1);
				this.trade.setAllocatedLongSeed(tradeID);
				this.trade.setInternalReference(String.valueOf(tradeID));
			} catch (CalypsoServiceException e) {
				Log.error(this,"Error setting Trade ID " + e);
			}
		}
	}

	private static final String FO_SYSTEM_RF = "MRXFI";

	private static final String PARTIAL_RETURN_ACTION = "PARTIAL_RETURN";
	public static final String UNDO_TERMINATE_ACTION = "UNDO_TERMINATE";
	private static final String CANCEL_ACTION_ACTION = "CANCEL_ACTION";

	public static HashSet<String> removeDuplicateFeesActions = new HashSet<>();
	static {
		removeDuplicateFeesActions.add(Action.S_TERMINATE);
		removeDuplicateFeesActions.add(Action.S_RERATE);
		removeDuplicateFeesActions.add(Action.S_REPRICE);
		removeDuplicateFeesActions.add(PARTIAL_RETURN_ACTION);
		removeDuplicateFeesActions.add(UNDO_TERMINATE_ACTION);
		removeDuplicateFeesActions.add(CANCEL_ACTION_ACTION);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Trade buildRepoTrade(CalypsoTrade jaxbCalypsoTrade, Vector<BOException> errors) {
		Trade trade = super.buildRepoTrade(jaxbCalypsoTrade, errors);
		Repo repo = (Repo)trade.getProduct();
		StubRule stubRule =null;
		JDate firstFixingResetDate;
		try {
			new RepoOnAmendMapper(repo).mapOnAmendIfActivated(jaxbCalypsoTrade);
			firstFixingResetDate = getFirstFixingResetDate();
			if(firstFixingResetDate!=null && Util.isTrue(trade.getKeywordValue(KW_FIRST_FIXING_CUSTOMIZED),false)) {
				if(!Util.isEmpty(trade.getKeywordValue(KW_FIRST_FIXING_RESET_DATE))) {
					stubRule=StubRule.F_FULL_COUPON;
					repo.getCash().setSecCode("FirstFullCoupon", sdfFullCouponDate.format(firstFixingResetDate.getDate()));
				}
			}
		} catch (CalypsoServiceException | ParseException e) {
			Log.error(this, e);
		}

		//Force SHORT_FIRST for Repos EVERGREEN
		if (stubRule == null && repo.getMaturityType().equals(Repo.EVERGREEN)){
			stubRule = StubRule.F_SHORT_FIRST;
		}

		if(stubRule == null)
			stubRule = getStubRule(trade);

		if (stubRule != null && repo.getCash() != null)
			fillStubRule(repo, stubRule);

		setRateUseObsShift(repo);
		return trade;
	}


	/**
	 *  Specific logic for BothEnds StubPeriod
	 * @param repo
	 * @param stubRule
	 */
	protected void fillStubRule(Repo repo, StubRule stubRule) {

		if (stubRule.equals(StubRule.F_SPECIFIC_BOTH)) {
			try {
				repo.getCash().setStubRule(stubRule);
				repo.getCash().setStartStubDate(JDate.valueOf(sdf.parse(trade.getKeywordValue(KW_FIRST_FIXING_PAYMENT_DATE))));
				repo.getCash().setEndStubDate(JDate.valueOf(sdf.parse(trade.getKeywordValue(KW_START_DATE_LAST_STUB))));
			} catch (ParseException e) {
				Log.error(this.getClass().getSimpleName() + " - Can't parse BothEndsStub dates", e);
			}
		} else {
			repo.getCash().setStubRule(stubRule);
		}
	}

	/**
	 * Clearing Trade's amendment uses a different action for workflow filtering purposes.
	 * If any financial change is done, the trade cannot be auto amended. AMEND_RECON action has an AuditFilter
	 * set to check this.
	 * @param trade
	 * @param calypsoTrade
	 */
	protected void setTradeAction(Trade trade, CalypsoTrade calypsoTrade){
		if(isClearedTrade(trade)){
			setAmendTradeAction(trade,calypsoTrade);
		}
	}

	protected void setAmendTradeAction(Trade trade, CalypsoTrade calypsoTrade){
		if(Action.AMEND.equals(trade.getAction())&&
				!isReprocess(calypsoTrade)){
			trade.setAction(Action.valueOf(ReconCCPConstants.WF_AMEND_RECON));
		}
	}

	public JDate getFirstFixingResetDate() throws ParseException {
		if(!Util.isEmpty(trade.getKeywordValue(KW_FIRST_FIXING_RESET_DATE))) {
			return JDate.valueOf(sdf.parse(trade.getKeywordValue(KW_FIRST_FIXING_RESET_DATE)));
		}
		return null;
	}


	/**
	 * XML keyword.InitialStub	XML keyword.FinalStub	Calypso Output
	 * shortCoupon										SHORT FIRST
	 * longCoupon	 									LONG FIRST
	 * shortCoupon				SHORT LAST
	 * longCoupon				LONG LAST
	 * fullCoupon	 									FULL COUPON
	 * fullCoupon				FULL COUPON
	 *
	 * @param trade
	 * @return
	 */
	public StubRule getStubRule(Trade trade) {

		String stubPeriod = trade.getKeywordValue(KW_STUB_PERIOD);
		String initialStub = trade.getKeywordValue(KW_INITIAL_STUB);
		String finalStub = trade.getKeywordValue(KW_FINAL_STUB);

		if (!Util.isEmpty(stubPeriod) && stubPeriod.startsWith("bothSides")) {
			return StubRule.F_SPECIFIC_BOTH;
		} else {
			if (!Util.isEmpty(initialStub)) {
				if (initialStub.equals(SHORT_COUPON)) {
					return StubRule.F_SHORT_FIRST;
				} else if (initialStub.equals(LONG_COUPON)) {
					return StubRule.F_LONG_FIRST;
				} else if (initialStub.equals(FULL_COUPON)) {
					return StubRule.F_FULL_COUPON;
				}
			}
			if (!Util.isEmpty(finalStub)) {
				if (finalStub.equals(SHORT_COUPON)) {
					return StubRule.F_SHORT_LAST;
				} else if (finalStub.equals(LONG_COUPON)) {
					return StubRule.F_LONG_LAST;
				} else if (finalStub.equals(FULL_COUPON)) {
					return StubRule.F_FULL_COUPON;
				}
			}
		}

		return StubRule.F_NONE;
	}

	public boolean isFeeAlreadyOnTrade(Fee incomingFee, Trade trade) {
		for(com.calypso.tk.bo.Fee existingFee: trade.getFeesList()) {
			String existingFeeType = existingFee.getType();
			String incomingFeeType = incomingFee.getFeeType();

			if(existingFeeType!=null && incomingFeeType!=null && existingFeeType.equals(incomingFeeType)) {
				double incomingFeeAmount = incomingFee.getFeeAmount();
				double existingFeeAmount = existingFee.getAmount();

				if( incomingFee.getFeeDate()!=null) {
					Date incomingFeeDate = incomingFee.getFeeDate().toGregorianCalendar().getTime();
					JDate existingFeeDate = existingFee.getDate();
					if(incomingFeeDate!=null && existingFeeDate!=null) {
						boolean isSameFee = true;
						if(incomingFeeAmount!=existingFeeAmount) {
							isSameFee=false;
						}
						if(isSameFee && !existingFeeDate.equals(JDate.valueOf(incomingFeeDate))) {
							isSameFee=false;
						}
						if(isSameFee)
							return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public Vector<com.calypso.tk.bo.Fee> getFees(Trade trade, CalypsoTrade calypsoTrade, Connection dbCon) {
		if(removeDuplicateFeesActions.contains(calypsoTrade.getAction())) {
			TradeFee feeData = calypsoTrade.getTradeFee();
			if (feeData != null) {
				List<Fee> feeList = feeData.getFee();
				ListIterator<Fee> feeIterator = feeList.listIterator();

				while(feeIterator.hasNext()) {
					Fee fee = feeIterator.next();
					if(isFeeAlreadyOnTrade(fee,trade)) {
						feeIterator.remove();
					}
				}
			}
		}
		return super.getFees(trade, calypsoTrade, dbCon);
	}


	@Override
	public void upload(CalypsoObject object, Vector<BOException> errors, Object dbCon, boolean saveToDB1) {
		checkCptyChange();
		super.upload(object, errors, dbCon, saveToDB1);

		setKeywords();
		modifyExternalRef();

		Repo repo = (Repo)trade.getProduct();
		CollateralConfig collateralConfig = getCollateralConfig(this.trade);

		clearRepoTripartyCollateralInfo(repo);
		setDFPonRepoTriparty(repo);

		//SCIBCALLAC-3938 & SCIBCALLAC-3963: Do not add collateral info on triparty trades nor gc pooling
		String isGCPooling = trade.getKeywordValue(GC_POOLING_KEYWORD);
		if (collateralConfig != null) {
			if (repo.isTriparty() && (Util.isEmpty(isGCPooling) || "false".equalsIgnoreCase(isGCPooling) || trade.getMirrorBook() != null)){
				repo.setDeliveryType("DFP");
				Log.info(this, "Not assigning colleteral config info, is triparty: ["+repo.isTriparty()+"], " +
						"isGCPooling: ["+isGCPooling+"], has mirror book: ["+trade.getMirrorBook()+"]");
			}else{
				setCollateralContractInfo(trade, repo, collateralConfig.getId());
			}
		}

		modifyRateIndex(repo);
		setTradeAction(this.trade,this.calypsoTrade);
	}

	public void modifyRateIndex(Repo repo){
		final RateIndex rateIndex = Optional.ofNullable(repo).map(Repo::getCash).map(Cash::getRateIndex).orElse(null);
		Cash cash = Optional.ofNullable(repo).map(Repo::getCash).orElse(null);

		if(Optional.ofNullable(cash).isPresent() && Optional.ofNullable(rateIndex).isPresent() && "true".equalsIgnoreCase(rateIndex.getDefaults().getAttribute("RFR"))){
			try{
				String final_rate_dec = Optional.ofNullable(rateIndex.getDefaults().getAttribute("FINAL_RATE_DEC")).orElse("");
				String final_rate_rounding_method = Optional.ofNullable(rateIndex.getDefaults().getAttribute("FINAL_RATE_ROUNDING_METHOD")).orElse("");

				if("true".equalsIgnoreCase(rateIndex.getDefaults().getAttribute("RFRAverage"))){
					cash.setAveragingResetB(true);
					cash.setAveragingResetMethod(rateIndex.getDefaults().getAvgMethod().getAveragingMethod());
					cash.setSampleFrequency(rateIndex.getDefaults().getAvgMethod().getFrequency());
					cash.setSamplePeriodRule(rateIndex.getDefaults().getAvgMethod().getPeriodRule());
					cash.setCompoundingMethod("None");
					cash.setCompoundFrequency(Frequency.F_NONE);
					if(Util.isEmpty(final_rate_dec) && Util.isEmpty(final_rate_rounding_method)){
						final_rate_dec = "4";
						final_rate_rounding_method = "NEAREST";
					}
				}
				if(!Util.isEmpty(final_rate_dec) && !Util.isEmpty(final_rate_rounding_method)){
					cash.setSecCode("RATE_ROUNDING_DEC",final_rate_dec);
					cash.setSecCode("RATE_ROUNDING",final_rate_rounding_method);
				}
			}catch (Exception e){
				Log.error(this,"Error loading RateIndex: " + e.getCause());
			}
			setCashResetTimingAndCompoundingMethodForRfR(cash);
			setCashResetLagFromIndexForRfR(cash,rateIndex);
		}
	}

	private void setCashResetTimingAndCompoundingMethodForRfR(Cash cash){
		if(isCompoundingCashRateType(cash)){
			cash.setCompoundingMethod("SimpleSpread");
			cash.setResetTiming("BEG_PER");
		}
	}

	private void setCashResetLagFromIndexForRfR(Cash cash,RateIndex rateIndex){
		cash.setUseCustomResetOffsetB(false);
		cash.setPmtOffsetBusDayB(rateIndex.getResetBusLagB());
		cash.setResetOffset(rateIndex.getResetDays());
	}

	private boolean isCompoundingCashRateType(Cash cash){
		return Optional.ofNullable(cash)
				.map(Cash::getCompoundFrequency)
				.map(Frequency.F_NONE::equals)
				.map(isNoneFrequency -> !isNoneFrequency)
				.orElse(false);
	}

	@Override
	public TradeArray applyTerminateAction(Trade trade, CalypsoTrade jaxbCalypsoTrade, Vector<BOException> errors) {
		modifyTerminationAction(trade,jaxbCalypsoTrade);
		return super.applyTerminateAction(trade, jaxbCalypsoTrade, errors);
	}

	public void modifyTerminationAction(Trade trade,CalypsoTrade jaxbCalypsoTrade) {
		if(isTermAndRepo(trade,jaxbCalypsoTrade)){
			Termination termination = jaxbCalypsoTrade.getTermination();
			String terminationLeg = termination.getTerminationLeg();
			switch (terminationLeg){
				case "PAY":
					terminationLeg = "REC";
					break;
				case "REC":
					terminationLeg = "PAY";
					break;
			}
			termination.setTerminationLeg(terminationLeg);
			if(Optional.ofNullable(termination.getSettleInterestAmount()).isPresent()){
				termination.setSettleInterestAmount(termination.getSettleInterestAmount()*-1);
			}
			jaxbCalypsoTrade.setTermination(termination);
		}
	}

	private boolean isTermAndRepo(Trade trade,CalypsoTrade jaxbCalypsoTrade ){
		boolean isRepoDirection = Optional.ofNullable(trade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getDirection).filter("Repo"::equalsIgnoreCase).isPresent();
		boolean isActionTerminate = Optional.ofNullable(jaxbCalypsoTrade).map(CalypsoTrade::getAction).filter("TERMINATE"::equalsIgnoreCase).isPresent();
		return isRepoDirection && isActionTerminate;
	}

	private void modifyExternalRef() {
//		Action tradeAction = trade.getAction();
//		if (tradeAction != null && tradeAction.toString().equals(UNDO_TERMINATE_ACTION)) {
//			String extRef = trade.getExternalReference();
//			if (!Util.isEmpty(extRef) && !extRef.startsWith("C")) {
//				trade.setExternalReference("C" + extRef);
//			}
//		}
	}

	protected void setKeywords() {
		trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE, CollateralStaticAttributes.ISMA);
		addFOSystemKeyword();
		addCollateralExclude();
		addGCPoolingKeyword();
		addReRateKeyword();
	}

	private void addReRateKeyword(){
		final Collection<LegalEntityAttribute> legalEntityAttributes = Optional.ofNullable(trade.getCounterParty())
				.map(LegalEntity::getLegalEntityAttributes).orElse(new Vector<LegalEntityAttribute>());
		if(!Util.isEmpty(legalEntityAttributes)){
			final String reRate_method = legalEntityAttributes.stream()
					.filter(att -> att.getAttributeType().equalsIgnoreCase("RERATE_METHOD"))
					.findFirst().map(LegalEntityAttribute::getAttributeValue).orElse("");
			trade.addKeyword(KW_RE_RATE_METHOD , reRate_method);
		}
	}


	private void addGCPoolingKeyword(){
		final String isGcPooling = ((Repo) trade.getProduct()).getSecurity().getSecCode(GC_POOLING);
		if("Y".equalsIgnoreCase(isGcPooling) ||
				"YES".equalsIgnoreCase(isGcPooling)
				|| "true".equalsIgnoreCase(isGcPooling)){
			trade.addKeyword(GC_POOLING_KEYWORD , "true");
		}
	}

	private void addFOSystemKeyword() {
		Repo repo = (Repo)trade.getProduct();
		if (repo == null) {
			return;
		}
		if (repo.getUnderlyingProduct() instanceof Equity) {
			trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, FO_SYSTEM_RV);
		}
		else {
			trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, FO_SYSTEM_RF);
		}
	}


	private CollateralConfig getCollateralConfig(Trade trade) {
		CollateralConfig marginCallConfig = null;
		int mccId = 0;

		try {
			mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

		} catch (Exception e) {
			Log.error(this, e);
			mccId = 0;
		}

		if (mccId == 0) {
			ArrayList<String> errorMsgs = new ArrayList<String>();
			try {
				marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
			} catch (RemoteException e) {
				Log.error(this, "Could not find MarginCall Config : " + e.toString());
			}
		} else {
			marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
		}

		return marginCallConfig;
	}

	/**
	 * CollateralConfig related attributes are set
	 * @param trade
	 * @param repo
	 * @param collateralConfigId
	 */
	private void setCollateralContractInfo(Trade trade, Repo repo, int collateralConfigId){
		repo.setMarginCallContractId(trade, collateralConfigId);
		trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, collateralConfigId);
		trade.setInternalReference(String.valueOf(collateralConfigId));
	}

	private void setRateUseObsShift(Repo repo){
		String obsShift= Optional.ofNullable(repo).map(SecFinance::getRateIndex).map(RateIndex::getDefaults).map(rid->rid.getAttribute("ObservationShift"))
				.orElse("false");
		if(Boolean.parseBoolean(obsShift)){
			repo.getCash().setSecCode("IS_SAMPLE_PERIOD_SHIFT","true");
		}
	}

	private void addCollateralExclude() {
		if (null != trade && isCollateralExclude()) {
			trade.addKeyword("CollateralExclude", "true");
		} else {
			Objects.requireNonNull(trade).addKeyword("CollateralExclude", "");
		}
	}

	private boolean isCollateralExclude(){
		if(null!=trade){
			LegalEntity counterParty = trade.getCounterParty();
			if(null!=counterParty){
				Vector<LegalEntityAttribute> legalEntityAttributes = BOCache.getLegalEntityAttributes(DSConnection.getDefault(), counterParty.getId());
				if(!com.calypso.tk.core.Util.isEmpty(legalEntityAttributes)){
					String attributeValue = legalEntityAttributes.stream()
							.filter(att -> "COLLATERAL_EXCLUDE".equalsIgnoreCase(att.getAttributeType()))
							.map(LegalEntityAttribute::getAttributeValue).findFirst().orElse("");
					return "true".equalsIgnoreCase(attributeValue);
				}
			}
		}
		return false;
	}

	/**
	 * Reset MC_CONTRACT_NUMBER kw if AMEND is received with other cpty
	 */
	private void checkCptyChange() {
		if (null != trade) {
			if (null != trade.getCounterParty() && !trade.getCounterParty().getCode().equalsIgnoreCase(calypsoTrade.getTradeCounterParty())) {
				trade.addKeyword("MC_CONTRACT_NUMBER", "");
				trade.setInternalReference("");
			}
		}
	}

	/**
	 * Set delivery Type DFP by default for Repo Triaprty
	 */
	private void setDFPonRepoTriparty(Repo repo){
		if(repo.isTriparty()){
			repo.setDeliveryType("DFP");
		}
	}

	/**
	 * Clear MARGIN_CALL_CONFIG_ID and margin call contract id to 0, in case repo is triparty
	 */
	private void clearRepoTripartyCollateralInfo(Repo repo){
		//If repo is triparty, clear the collateral info
		if(repo.isTriparty()){
			repo.setMarginCallContractId(this.trade, 0);
			this.trade.addKeyword("MARGIN_CALL_CONFIG_ID", 0);
		}
	}
}

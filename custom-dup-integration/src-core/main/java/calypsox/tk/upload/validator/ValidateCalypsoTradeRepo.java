package calypsox.tk.upload.validator;

import calypsox.tk.upload.mapper.MurexRepoMapper;
import calypsox.tk.upload.uploader.UploadCalypsoTradeRepo;
import calypsox.tk.upload.validator.ccp.ClearingTradeUploadValidator;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.services.UploaderKeyManager;
import com.calypso.tk.upload.util.UploaderTradeUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class ValidateCalypsoTradeRepo extends com.calypso.tk.upload.validator.ValidateCalypsoTradeRepo implements ClearingTradeUploadValidator, ReprocessKwdValidator {
	public static String EXISTING_TRADE_WHERE_CLAUSE_TERMINATED = "trade.external_reference= ? and product_desc.product_type = ? and trade.trade_status = " + ioSQL.string2SQLString("TERMINATED");
	public static String EXISTING_TRADE_WHERE_CLAUSE_NOT_TERMINATED = "trade.external_reference= ? and product_desc.product_type = ? and trade.trade_status <> " + ioSQL.string2SQLString("TERMINATED");
	private static String MUREX_REPO_ACTION_PREFIX = "I";
	/**
	 * validate : unique external reference per product.
	 */
	public void validate(CalypsoObject object, Vector<BOException> errors) {
		this.calypsoTrade = (CalypsoTrade) object;
		
		String tradeAction = calypsoTrade.getAction();
		String mxLastEventKW = getKeyWordValue(MurexRepoMapper.WKF_MXLASTEVENT_KEYWORD_REPO_MUREX);
		
		_trade = null;

		if (!Util.isEmpty(mxLastEventKW) && mxLastEventKW.endsWith(MurexRepoMapper.MX_LAST_EVENT_PORTFOLIO_ASSIGNMENT_VALUE)) {
			if (tradeAction != null && (tradeAction.equals(Action.S_NEW) || tradeAction.equals(Action.S_CANCEL))) {
				_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, false, EXISTING_TRADE_WHERE_CLAUSE_NOT_TERMINATED, errors);
			}
			else if (tradeAction != null && tradeAction.equals(UploadCalypsoTradeRepo.UNDO_TERMINATE_ACTION)) {
				_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, false, EXISTING_TRADE_WHERE_CLAUSE_TERMINATED, errors);
			}
			else {
				_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, isUndoTerminate(), errors);
			}
		}
		else {
			_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, isUndoTerminate() || isRerate(), errors);
		}

		if(!isActionAccepted(mxLastEventKW,calypsoTrade,_trade)){
			errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00004", parseMurexAction(mxLastEventKW), _trade != null ? _trade.getLongId() : null!=calypsoTrade.getTradeId() ? calypsoTrade.getTradeId() : 0L));
			return;
		}
		
		if ((calypsoTrade.getTradeId() != null || _trade != null) && "NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
			errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00011", UploaderTradeUtil.getID(this.calypsoTrade), _trade != null ? _trade.getLongId() : null!=calypsoTrade.getTradeId() ? calypsoTrade.getTradeId() : 0L));
			return;
		}
		if ((calypsoTrade.getTradeId() == null && _trade == null) && !"NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
			String productType = Util.isEmpty(this.calypsoTrade.getExternalReference()) ? "TradeId" : "External Reference";
			errors.add(ErrorExceptionUtils.createException("21002", productType, "00002", UploaderTradeUtil.getID(this.calypsoTrade), 0L));
			return;
		}


		
		if (_trade != null) {
			calypsoTrade.setTradeId(_trade.getLongId());
		}

		if(!isReprocess(calypsoTrade)) {
			validateTerminateClearingTrade(calypsoTrade, errors);
		}
		String externalReference = calypsoTrade.getExternalReference();
		calypsoTrade.setExternalReference(null);
		super.validate(calypsoTrade, errors);
		calypsoTrade.setExternalReference(externalReference);
	}



	public String getKeyWordValue(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(final Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword.getKeywordValue();
				}
			}
		}
		return null;

	}

	/**
	 * Filter Actions to accept by repo type.
	 * Need to create a DV as follows: Repo(RepoType)MurexAcceptedActions or Repo(RepoType)MurexExcludedActions
	 *
	 * @param tradeAction
	 * @param
	 * @return
	 */
	private boolean isActionAccepted(String tradeAction, CalypsoTrade productTrade, Trade trade){

		String repoType = Optional.ofNullable(productTrade).map(CalypsoTrade::getProduct).map(Product::getRepo).map(Repo::getRepoType).
				orElse(Optional.ofNullable(trade).map(Trade::getProduct).map(com.calypso.tk.core.Product::getSubType).orElse(""));

		if(!Util.isEmpty(repoType) && !Util.isEmpty(tradeAction)){
			tradeAction = parseMurexAction(tradeAction);
			final List<String> repoMurexActionAccepted = DomainValues.values("Repo"+repoType+"MurexAcceptedActions");
			final List<String> repoMurexActionExcluded = DomainValues.values("Repo"+repoType+"MurexExcludedActions");
			if(!Util.isEmpty(repoMurexActionAccepted)){
				return Arrays.stream(repoMurexActionAccepted.toArray()).map(Object::toString).anyMatch(tradeAction::equalsIgnoreCase);
			}
			if(!Util.isEmpty(repoMurexActionExcluded)){
				return Arrays.stream(repoMurexActionAccepted.toArray()).map(Object::toString).noneMatch(tradeAction::equalsIgnoreCase);
			}
		}
		return true;
	}

	private String parseMurexAction(String murexAction){
		try {
			if(!Util.isEmpty(murexAction) && murexAction.contains(MUREX_REPO_ACTION_PREFIX)) {
				murexAction = murexAction.substring(murexAction.indexOf(MUREX_REPO_ACTION_PREFIX)+1);
				return murexAction.trim();
			}
		}catch (Exception e){
			Log.error(this,"Error : " +e);
		}
		return murexAction;
	}
	
	public boolean isUndoTerminate() {
		return calypsoTrade.getAction().equals("UNDO_TERMINATE");
	}

	public boolean isRerate() {
		return calypsoTrade.getAction().equals("RERATE");
	}

	@Override
	public void validateReRateAction(CalypsoObject object, Trade trade, String action, Vector<BOException> errors, long tradeId) {
		CalypsoTrade jaxbCalypsoTrade = (CalypsoTrade)object;
		String uniqueKey = UploaderKeyManager.getUniqueKey(jaxbCalypsoTrade);
		if (trade.getProduct() != null && trade.getProduct() instanceof com.calypso.tk.product.Repo) {
			trade = trade.clone();
			com.calypso.tk.product.Repo repo = (com.calypso.tk.product.Repo)trade.getProduct();
			//this.isTradeTerminated(trade, errors, uniqueKey); //Apply Action Rerate on Terminate Repo
			this.hasCash(repo, errors, tradeId, uniqueKey);
			this.isRebateRerateAllowed(repo, errors, tradeId, uniqueKey);
			if (com.calypso.infra.util.Util.isEmpty(errors)) {
				this.validateReRate(trade, jaxbCalypsoTrade, action, errors, uniqueKey, tradeId);
				ReRate reRate = jaxbCalypsoTrade.getReRate();
				if (reRate != null) {
					XMLGregorianCalendar effectiveDate = reRate.getResetDate();
					XMLGregorianCalendar agreementDate = reRate.getAgreementDate();
					if (effectiveDate != null && agreementDate != null && effectiveDate.compare(agreementDate) == -1) {
						errors.add(ErrorExceptionUtils.createException("21001", "Effective Date", "00425", "Agreement Date: " + agreementDate + " Effective Date: " + effectiveDate, tradeId));
					}
				}
			}
		} else {
			errors.add(ErrorExceptionUtils.createException("21001", "Invalid Trade", "00005", "The trade (TradeId " + tradeId + ") fetched from DB is not of type Repo", tradeId, uniqueKey));
		}
	}

	@Override
	public void validateGenericAction(CalypsoObject object, Trade trade, Vector<BOException> errors, Connection dbConnection, long tradeId) {
		CalypsoTrade calypsoTrade = (CalypsoTrade)object;
		String action = UploaderTradeUtil.isValidAction(dbConnection, calypsoTrade.getAction());
		if (!Util.isEmpty(this.calypsoTrade.getMirrorBook())) {
			this.validateMirrorBook(this.calypsoTrade, errors, tradeId);
		} else {
			this.validateCounterParty(errors, dbConnection, tradeId, this.calypsoTrade);
		}

		if (null != trade && UploaderTradeUtil.isNotNull(calypsoTrade.getTradeBook()) && !trade.getBook().equals(UploaderTradeUtil.getBook(dbConnection, calypsoTrade.getTradeBook()))) {
			errors.add(ErrorExceptionUtils.createException("21001", "Book", "00007", calypsoTrade.getTradeBook(), tradeId));
		}

		TradeKeywords tradeKeywordJxb = calypsoTrade.getTradeKeywords();
		if (tradeKeywordJxb != null) {
			UploaderTradeUtil.validateKeyWord(calypsoTrade, dbConnection, errors, tradeId);
		}

		TradeFee tradeFeeJxb = calypsoTrade.getTradeFee();
		if (tradeFeeJxb != null) {
			UploaderTradeUtil.validateFee(calypsoTrade, dbConnection, errors, tradeId);
		}

		if (!"TERMINATE".equalsIgnoreCase(action) && !"PARTIAL_TERMINATE".equalsIgnoreCase(action)) {
			if (!"NOVATE".equalsIgnoreCase(action) && !"PARTIAL_NOVATE".equalsIgnoreCase(action)) {
				if (!"EXERCISE".equalsIgnoreCase(action) && !"EXPIRE".equalsIgnoreCase(action)) {
					if ("ROLLOVER".equalsIgnoreCase(action)) {
						this.validateRollOverAction(object, trade, action, errors, tradeId);
					} else if ("RATEINDEX_UPDATE".equalsIgnoreCase(action)) {
						super.validateGenericAction(object, trade, errors, dbConnection, tradeId);
					} else if ("CLOSEANDREOPEN".equalsIgnoreCase(action)) {
						this.validateCloseAndOpenAction(object, trade, action, errors, tradeId);
					} else if ("REPRICE".equalsIgnoreCase(action)) {
						this.validateRepriceAction(object, trade, action, errors, tradeId);
					} else if ("PARTIAL_RETURN".equalsIgnoreCase(action)) {
						this.validatePartialReturnAction(object, trade, action, errors, tradeId);
					} else if ("RERATE".equalsIgnoreCase(action)) {
						this.validateReRateAction(object, trade, action, errors, tradeId);
					} else if ("SUBSTITUTION".equalsIgnoreCase(action)) {
						this.validateSubstitutionAction(object, trade, action, errors, tradeId);
					} else if ("SECURITY_MARGIN".equalsIgnoreCase(action)) {
						this.validateSecurityMarginAction(object, trade, action, errors, tradeId);
					} else if ("INTEREST_CLEANUP".equalsIgnoreCase(action)) {
						this.validateInterestCleanupAction(object, trade, action, errors, tradeId);
					} else if ("FEE_RERATE".equalsIgnoreCase(action)) {
						this.validateFeeReRateAction(object, trade, action, errors, tradeId);
					} else if ("CANCEL_ACTION".equalsIgnoreCase(action)) {
						this.validateCancelAction(object, trade, action, errors, tradeId);
					} else if ("KNOCK_IN".equalsIgnoreCase(action)) {
						this.validateKnockInAction(object, trade, action, errors, tradeId);
					} else if ("KNOCK_OUT".equalsIgnoreCase(action)) {
						this.validateKnockOutAction(object, trade, action, errors, tradeId);
					} else if ("TRIGGER_IN".equalsIgnoreCase(action)) {
						this.validateTriggerInAction(object, trade, action, errors, tradeId);
					} else if ("TRIGGER_OUT".equalsIgnoreCase(action)) {
						this.validateTriggerOutAction(object, trade, action, errors, tradeId);
					} else if ("ALLOCATE".equals(action)) {
						super.validateGenericAction(object, trade, errors, dbConnection, tradeId);
					} else if ("RATERESET".equals(action)) {
						this.validateRateresetAction(object, trade, action, errors, tradeId);
					} else if ("TAKEUP".equalsIgnoreCase(action)) {
						this.validateTakeUpAction(object, trade, action, errors, tradeId);
					} else if ("PRINCIPAL_CHANGE".equalsIgnoreCase(action)) {
						this.validatePrincipalChangeAction(object, trade, action, errors, tradeId);
					} else if ("EXPOSURE_ADJUSTMENT".equalsIgnoreCase(action)) {
						this.validateExposureAdjustmentAction(object, trade, action, errors, tradeId);
					} else if ("CALL_EXERCISE".equalsIgnoreCase(action)) {
						this.validateCallExerciseAction(object, trade, action, errors, tradeId);
					} else if ("ASSIGN".equalsIgnoreCase(action)) {
						this.validateAssignAction(object, trade, action, errors, tradeId);
					} else if (!"AMEND".equalsIgnoreCase(action) && !"CANCEL".equalsIgnoreCase(action) && !"UNDO_TERMINATE".equalsIgnoreCase(action) && !"UNDO_ROLL".equalsIgnoreCase(action) && !"MWEXIT".equalsIgnoreCase(action) && !"DECLEAR".equalsIgnoreCase(action) && !"UNEXERCISE".equalsIgnoreCase(action) && !"VOID".equalsIgnoreCase(action) && !"UNDO_EXPIRE".equals(action) && !"RATEINDEX_UPDATE".equals(action)) {
						errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00103", UploaderTradeUtil.getID(calypsoTrade), tradeId));
					}
				} else {
					this.validateExerciseAction(object, trade, action, errors, tradeId);
				}
			} else {
				this.validateNovationAction(object, trade, errors, dbConnection, tradeId);
			}
		} else {
			this.validateTerminateAction(object, trade, errors, dbConnection, tradeId);
		}
	}
}

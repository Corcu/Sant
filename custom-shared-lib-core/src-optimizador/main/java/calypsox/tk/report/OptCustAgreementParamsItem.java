/* Actualizado por David Porras Mart?nez 23-11-11 */

package calypsox.tk.report;

import com.calypso.tk.refdata.CollateralConfig;

public class OptCustAgreementParamsItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	public static final String OPT_MARGINCALL_ITEM = "OptAgreementParamsItem";

	public static final String OPT_MARGIN_CALL_CONFIG = "MarginCallConfig";

	// Contracts and Counterparties
	private String marginCallContract;
	private String owner;
	private String ownerName;
	private String counterparty;
	private String contractType;
	private String masterAgreeDescription;
	private String contractCcy;
	// private double mtaCpty;
	private String mtaCpty;
	// private double mtaOwner;
	private String mtaOwner;
	// private double thresholdCpty;
	private String thresholdCpty;
	// private double thresholdOwner;
	private String thresholdOwner;
	// private double initialMargin;
	private String initialMargin;
	private String calcPeriod;
	private String firstCalcDate;
	private String assetType;
	private String oneWay;
	private String headClone;
	private String contractLongName;
	private String masterAgreementShortName;
	private String notUsed;
	private String masterSignedDate;
	private String deliveryRounding;
	// private double deliveryRoundingCpty;
	private String deliveryRoundingCpty;
	// private double deliveryRoundingOwner;
	private String deliveryRoundingOwner;
	// private double toleranceAmount;
	private String toleranceAmount;
	// private double independentAmountCpty;
	private String independentAmountCpty;
	// private double independentAmountOwner;
	private String independentAmountOwner;

	// Instruments
	private String instrument;

	// Cash
	private String header;
	private String transactionId;
	private String transactionType;
	private String action;
	private String reconciliationType;
	// private JDate transactionDate;
	private String transactionDate;
	// private JDate maturityDate;
	private String maturityDate;
	private String isReceived; // receivedDelivered
	// private double collateralAmount; //collAmount
	private String collateralAmount;
	// private String dealTable; //???
	// private double independentAmount; //added
	private String independentAmount;
	private String source; // added

	// Security positions (Bonds)
	// private String bondId; //transactionId
	private String issuer;
	// private JDate bondMaturityDate;
	private String bondMaturityDate;
	private String isin;
	// private double dirtyPrice;
	private String dirtyPrice;
	// private double haircut;
	private String haircut;
	// new field
	private String concilia;

	// GSM: Adaptation to Optimization. 19/02/14
	private CollateralConfig colConfig;
	private String eligibleAgencies;
	private String haircutRuleName;
	private boolean rehypothecable;
	private boolean longTerm_SP;
	private boolean longTerm_Fitch;
	private boolean longTerm_Moody;
	private String ctpyMtaType;
	private String ownerMtaType;
	private String ctpyMtaCcy;
	private String ownerMtaCcy;
	private String ctpyThresholdType;
	private String ownerThresholdType;
	private int ownerCashOffset;
	private int ctpyCashOffset;
	private String InitialMarginCcy;
	private int ownerSecurityOffset;
	private String book;
	private boolean excludeFromOptimizer;
	private String EODPricingEnvironment;
	private String intradayPricingEnvironment;
	private String toleranceAmountCcy;

	public String getToleranceAmountCcy() {
		return this.toleranceAmountCcy;
	}

	public void setToleranceAmountCcy(String toleranceAmountCcy) {
		this.toleranceAmountCcy = toleranceAmountCcy;
	}

	public String getBook() {
		return this.book;
	}

	public void setBook(String book) {
		this.book = book;
	}

	public boolean isExcludeFromOptimizer() {
		return this.excludeFromOptimizer;
	}

	public void setExcludeFromOptimizer(boolean excludeFromOptimizer) {
		this.excludeFromOptimizer = excludeFromOptimizer;
	}

	public String getEODPricingEnvironment() {
		return this.EODPricingEnvironment;
	}

	public void setEODPricingEnvironment(String eODPricingEnvironment) {
		this.EODPricingEnvironment = eODPricingEnvironment;
	}

	public String getIntradayPricingEnvironment() {
		return this.intradayPricingEnvironment;
	}

	public void setIntradayPricingEnvironment(String intradayPricingEnvironment) {
		this.intradayPricingEnvironment = intradayPricingEnvironment;
	}

	public int getOwnerSecurityOffset() {
		return this.ownerSecurityOffset;
	}

	public void setOwnerSecurityOffset(int ownerSecurityOffset) {
		this.ownerSecurityOffset = ownerSecurityOffset;
	}

	public int getCtpySecurityOffset() {
		return this.ctpySecurityOffset;
	}

	public void setCtpySecurityOffset(int ctpySecurityOffset) {
		this.ctpySecurityOffset = ctpySecurityOffset;
	}

	private int ctpySecurityOffset;

	// GSM: end

	public String getInitialMarginCcy() {
		return this.InitialMarginCcy;
	}

	public void setInitialMarginCcy(String initialMarginCcy) {
		this.InitialMarginCcy = initialMarginCcy;
	}

	public OptCustAgreementParamsItem() {
	}

	public String getConcilia() {
		return this.concilia;
	}

	public int getOwnerCashOffset() {
		return this.ownerCashOffset;
	}

	public void setOwnerCashOffset(int ownerCashOffset) {
		this.ownerCashOffset = ownerCashOffset;
	}

	public int getCtpyCashOffset() {
		return this.ctpyCashOffset;
	}

	public void setCtpyCashOffset(int ctpyCashOffset) {
		this.ctpyCashOffset = ctpyCashOffset;
	}

	/**
	 * @param concilia
	 */
	/**
	 * @param concilia
	 */
	public void setConcilia(final String concilia) {
		this.concilia = concilia;
	}

	public String getHaircut() {
		return this.haircut;
	}

	public void setHaircut(final String haircut) {
		this.haircut = haircut;
	}

	public String getDirtyPrice() {
		return this.dirtyPrice;
	}

	public void setDirtyPrice(final String dirtyPrice) {
		this.dirtyPrice = dirtyPrice;
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(final String isin) {
		this.isin = isin;
	}

	public String getHeader() {
		return this.header;
	}

	public void setHeader(final String header) {
		this.header = header;
	}

	/*
	 * public String getLongNameCounterparty() { return longNameCounterparty; }
	 * 
	 * public void setLongNameCounterparty(String longNameCounterparty) { this.longNameCounterparty =
	 * longNameCounterparty; }
	 */

	public String getMasterSignedDate() {
		return this.masterSignedDate;
	}

	public void setMasterSignedDate(final String masterSignedDate) {
		this.masterSignedDate = masterSignedDate;
	}

	public String getToleranceAmount() {
		return this.toleranceAmount;
	}

	public void setToleranceAmount(final String toleranceAmount) {
		this.toleranceAmount = toleranceAmount;
	}

	public String getHeadClone() {
		return this.headClone;
	}

	public void setHeadClone(final String headClone) {
		this.headClone = headClone;
	}

	public String getNotUsed() {
		return this.notUsed;
	}

	public void setNotUsed(final String notUsed) {
		this.notUsed = notUsed;
	}

	public String getDeliveryRounding() {
		return this.deliveryRounding;
	}

	public void setDeliveryRounding(final String deliveryRounding) {
		this.deliveryRounding = deliveryRounding;
	}

	public String getDeliveryRoundingCpty() {
		return this.deliveryRoundingCpty;
	}

	public void setDeliveryRoundingCpty(final String deliveryRoundingCpty) {
		this.deliveryRoundingCpty = deliveryRoundingCpty;
	}

	public String getDeliveryRoundingOwner() {
		return this.deliveryRoundingOwner;
	}

	public void setDeliveryRoundingOwner(final String deliveryRoundingOwner) {
		this.deliveryRoundingOwner = deliveryRoundingOwner;
	}

	public String getIndependentAmountCpty() {
		return this.independentAmountCpty;
	}

	public void setIndependentAmountCpty(final String independentAmountCpty) {
		this.independentAmountCpty = independentAmountCpty;
	}

	public String getIndependentAmountOwner() {
		return this.independentAmountOwner;
	}

	public void setIndependentAmountOwner(final String independentAmountOwner) {
		this.independentAmountOwner = independentAmountOwner;
	}

	public String getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTransactionType() {
		return this.transactionType;
	}

	public void setTransactionType(final String transactionType) {
		this.transactionType = transactionType;
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(final String action) {
		this.action = action;
	}

	public String getReconciliationType() {
		return this.reconciliationType;
	}

	public void setReconciliationType(final String reconciliationType) {
		this.reconciliationType = reconciliationType;
	}

	public String getTransactionDate() {
		return this.transactionDate;
	}

	public void setTransactionDate(final String transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getMaturityDate() {
		return this.maturityDate;
	}

	public void setMaturityDate(final String maturityDate) {
		this.maturityDate = maturityDate;
	}

	public String getCollateralAmount() {
		return this.collateralAmount;
	}

	public void setCollateralAmount(final String collateralAmount) {
		this.collateralAmount = collateralAmount;
	}

	public String getIsReceived() {
		return this.isReceived;
	}

	public void setIsReceived(final String isReceived) {
		this.isReceived = isReceived;
	}

	/*
	 * public String getDealTable() { return dealTable; }
	 * 
	 * public void setDealTable(String dealTable) { this.dealTable = dealTable; }
	 */

	/*
	 * public double getPrice() { return price; }
	 * 
	 * public void setPrice(double price) { this.price = price; }
	 */
	/*
	 * public String getBondId() { return bondId; }
	 * 
	 * public void setBondId(String bondId) { this.bondId = bondId; }
	 */
	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(final String issuer) {
		this.issuer = issuer;
	}

	public String getBondMaturityDate() {
		return this.bondMaturityDate;
	}

	public void setBondMaturityDate(final String bondMaturityDate) {
		this.bondMaturityDate = bondMaturityDate;
	}

	public String getMarginCallContract() {
		return this.marginCallContract;
	}

	public void setMarginCallContract(final String marginCallContract) {
		this.marginCallContract = marginCallContract;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(final String owner) {
		this.owner = owner;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public void setOwnerName(final String ownerName) {
		this.ownerName = ownerName;
	}

	public String getCounterparty() {
		return this.counterparty;
	}

	public void setCounterparty(final String counterparty) {
		this.counterparty = counterparty;
	}

	public String getContractType() {
		return this.contractType;
	}

	public void setContractType(final String contractType) {
		this.contractType = contractType;
	}

	public String getMasterAgreeDescription() {
		return this.masterAgreeDescription;
	}

	public void setMasterAgreeDescription(final String masterAgreeDescription) {
		this.masterAgreeDescription = masterAgreeDescription;
	}

	public String getContractCcy() {
		return this.contractCcy;
	}

	public void setContractCcy(final String contractCcy) {
		this.contractCcy = contractCcy;
	}

	public String getMtaCpty() {
		return this.mtaCpty;
	}

	public void setMtaCpty(final String mtaCpty) {
		this.mtaCpty = mtaCpty;
	}

	public String getMtaOwner() {
		return this.mtaOwner;
	}

	public void setMtaOwner(final String mtaOwner) {
		this.mtaOwner = mtaOwner;
	}

	public String getThresholdCpty() {
		return this.thresholdCpty;
	}

	public void setThresholdCpty(final String thresholdCpty) {
		this.thresholdCpty = thresholdCpty;
	}

	public String getThresholdOwner() {
		return this.thresholdOwner;
	}

	public void setThresholdOwner(final String thresholdOwner) {
		this.thresholdOwner = thresholdOwner;
	}

	public String getInitialMargin() {
		return this.initialMargin;
	}

	public void setInitialMargin(final String initialMargin) {
		this.initialMargin = initialMargin;
	}

	public String getCalcPeriod() {
		return this.calcPeriod;
	}

	public void setCalcPeriod(final String calcPeriod) {
		this.calcPeriod = calcPeriod;
	}

	public String getFirstCalcDate() {
		return this.firstCalcDate;
	}

	public void setFirstCalcDate(final String firstCalcDate) {
		this.firstCalcDate = firstCalcDate;
	}

	public String getAssetType() {
		return this.assetType;
	}

	public void setAssetType(final String assetType) {
		this.assetType = assetType;
	}

	public String getOneWay() {
		return this.oneWay;
	}

	public void setOneWay(final String oneWay) {
		this.oneWay = oneWay;
	}

	public String getContractLongName() {
		return this.contractLongName;
	}

	public void setContractLongName(final String contractLongName) {
		this.contractLongName = contractLongName;
	}

	public String getMasterAgreementShortName() {
		return this.masterAgreementShortName;
	}

	public void setMasterAgreementShortName(final String masterAgreementShortName) {
		this.masterAgreementShortName = masterAgreementShortName;
	}

	public String getInstrument() {
		return this.instrument;
	}

	public void setInstrument(final String instrument) {
		this.instrument = instrument;
	}

	/*
	 * public String getInstrumentDescription() { return instrumentDescription; }
	 * 
	 * public void setInstrumentDescription(String instrumentDescription) { this.instrumentDescription =
	 * instrumentDescription; }
	 */
	public String getIndependentAmount() {
		return this.independentAmount;
	}

	public void setIndependentAmount(final String independentAmount) {
		this.independentAmount = independentAmount;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public void setCollateralConfig(final CollateralConfig marginCall) {
		this.colConfig = marginCall;
	}

	// GSM: 19/02/2014. New Optimization fields
	/**
	 * @return the colConfig
	 */
	public CollateralConfig getCollateralConfig() {
		return this.colConfig;
	}

	/**
	 * @param the
	 *            haircutRuleName
	 */
	public void setHaircutRuleName(String hCRuleName) {
		this.haircutRuleName = hCRuleName;
	}

	/**
	 * @param the
	 *            rehypothecable
	 */
	public void setRehypothecable(boolean rehypo) {
		this.rehypothecable = rehypo;
	}

	/**
	 * @param the
	 *            longTerm_SP
	 */
	public void setLongTermSP(boolean longTermSP) {
		this.longTerm_SP = longTermSP;
	}

	/**
	 * @param the
	 *            longTerm_Fitch
	 */
	public void setLongTermFitch(boolean longTermFitch) {
		this.longTerm_Fitch = longTermFitch;
	}

	/**
	 * @param the
	 *            longTerm_Moody
	 */
	public void setLongTermMoody(boolean longTermMoody) {
		this.longTerm_Moody = longTermMoody;
	}

	/**
	 * @return the haircutRuleName
	 */
	public String getHaircutRuleName() {
		return this.haircutRuleName;
	}

	/**
	 * @return the rehypothecable
	 */
	public boolean isRehypothecable() {
		return this.rehypothecable;
	}

	/**
	 * @return the longTerm_SP
	 */
	public boolean isLongTerm_SP() {
		return this.longTerm_SP;
	}

	/**
	 * @return the longTerm_Fitch
	 */
	public boolean isLongTerm_Fitch() {
		return this.longTerm_Fitch;
	}

	/**
	 * @return the longTerm_Moody
	 */
	public boolean isLongTerm_Moody() {
		return this.longTerm_Moody;
	}

	/**
	 * @return the eligibleAgencies
	 */
	public String getEligibleAgencies() {
		return this.eligibleAgencies;
	}

	/**
	 * @param eligibleAgencies
	 *            the eligibleAgencies to set
	 */
	public void setEligibleAgencies(String eligibleAgencies) {
		this.eligibleAgencies = eligibleAgencies;
	}

	/**
	 * @return the ctpyMtaType
	 */
	public String getCtpyMtaType() {
		return this.ctpyMtaType;
	}

	/**
	 * @param ctpyMtaType
	 *            the ctpyMtaType to set
	 */
	public void setCtpyMtaType(String ctpyMtaType) {
		this.ctpyMtaType = ctpyMtaType;
	}

	/**
	 * @return the ctpyThresholdType
	 */
	public String getCtpyThresholdType() {
		return this.ctpyThresholdType;
	}

	/**
	 * @param ctpyThresholdType
	 *            the ctpyThresholdType to set
	 */
	public void setCtpyThresholdType(String ctpyThresholdType) {
		this.ctpyThresholdType = ctpyThresholdType;
	}

	/**
	 * @return the ownerMtaType
	 */
	public String getOwnerMtaType() {
		return this.ownerMtaType;
	}

	/**
	 * @param ownerMtaType
	 *            the ownerMtaType to set
	 */
	public void setOwnerMtaType(String ownerMtaType) {
		this.ownerMtaType = ownerMtaType;
	}

	/**
	 * @return the ownerThresholdType
	 */
	public String getOwnerThresholdType() {
		return this.ownerThresholdType;
	}

	/**
	 * @param ownerThresholdType
	 *            the ownerThresholdType to set
	 */
	public void setOwnerThresholdType(String ownerThresholdType) {
		this.ownerThresholdType = ownerThresholdType;
	}

	public void setCtpyMtaCcy(String ctpyMTACcy) {
		this.ctpyMtaCcy = ctpyMTACcy;

	}

	public void setOwnerMtaCcy(String ownerMTACcy) {
		this.ownerMtaCcy = ownerMTACcy;

	}

	public String getCtpyMtaCcy() {
		return this.ctpyMtaCcy;

	}

	public String getOwnerMtaCcy() {
		return this.ownerMtaCcy;
	}

	// GSM: 19/02/2014 block end

	/*
	 * public String getCurrency() { return currency; }
	 * 
	 * public void setCurrency(String currency) { this.currency = currency; }
	 */
	/*
	 * public String getCptyLongName() { return cptyLongName; }
	 * 
	 * public void setCptyLongName(String cptyLongName) { this.cptyLongName = cptyLongName; }
	 */
}

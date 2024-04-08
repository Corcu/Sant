/* Actualizado por David Porras Mart?nez 23-11-11 */

package calypsox.tk.report;


public class Opt_Collateral_MarginCallItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	public static final String OPTCOL_MARGINCALL_ITEM = "Opt_Collateral_MarginCallItem";

	// Contracts and Counterparties
	private String marginCallContract;
	private String owner;
	private String ownerName;
	private String counterparty;
	private String contractType;
	private String masterAgreeDescription;
	private String contractCcy;
	private String book;
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

	public Opt_Collateral_MarginCallItem() {
	}

	public String getConcilia() {
		return this.concilia;
	}

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
	
	public String getBook() {
		return this.book;
	}

	public void setBook(final String book) {
		this.book = book;
	}

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

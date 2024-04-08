package calypsox.tk.report;

public class CSAExposureItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;
	/**
	 * Identify the object ReposTradeItem.
	 */
	public static final String CSA_EXPOSURE_ITEM = "CSAExposureItem";

	// Customized columns.
	private String notional;
	private String tradeCurr;
	private String notional2;
	private String tradeCurr2;
	private String underlying;
	private String mtmDate;
	private String mtmDatesp;
	private String mtmValue;
	private String mtmCurr;
	private String tradeGroupID;
	private String tradeDate;
	private String tradeDatesp;
	private String startTradeDatesp;
	private String matDate;
	private String matDatesp;
	private String payRec;
	// private double couponRate;
	// private String couponPer;
	private String floatRate;
	private String floatRate2;
	// private String floatIndexPer;
	// private String float2IndexPer;
	// private double spread;
	// private String daycount;
	// private double floatPayPer;
	private String strikePrice;
	private String optionType;

	private String excercise;

	// private int tradeID2;
	// private String freeText1;
	// private String freeText2;
	// private String freeText3;
	// private String freeText4;
	// private String freeText5;
	// private int dtccCpID;

	// private String settlementType;
	// private String deliveryPoint;
	// private String settlementExposure;

	// new
	private String collatAgree;
	private String collatAgreeType;
	private String owner;
	private String indAmount;
	private String baseCcy;
	private String structure;
	private String valAgent;
	private String cpty;
	private String tradeID;

	// GSM: new portfolio reconciliation
	private String usiReference;
	private String sdMsp;
	private String UsParty;
	private String dfaApplicable;
	private String fcNfc;
	private String emirApplicable;
	// GSM: 22/08/13. Added the 7? field for Port. Reconciliation
	private String uti;

	public CSAExposureItem() {
	}

	public String getNotional() {
		return this.notional;
	}

	public void setNotional(final String notional) {
		this.notional = notional;
	}

	public String getTradeCurr() {
		return this.tradeCurr;
	}

	public void setTradeCurr(final String tradeCurr) {
		this.tradeCurr = tradeCurr;
	}

	public String getNotional2() {
		return this.notional2;
	}

	public void setNotional2(final String notional2) {
		this.notional2 = notional2;
	}

	public String getTradeCurr2() {
		return this.tradeCurr2;
	}

	public void setTradeCurr2(final String tradeCurr2) {
		this.tradeCurr2 = tradeCurr2;
	}

	public String getMtmDate() {
		return this.mtmDate;
	}
	public String getMtmDatesp() {
		return this.mtmDatesp;
	}

	public void setMtmDate(final String mtmDate) {
		this.mtmDate = mtmDate;
	}
	
	public void setMtmDatesp(final String mtmDatesp) {
		this.mtmDatesp = mtmDatesp;
	}

	public String getMtmValue() {
		return this.mtmValue;
	}

	public void setMtmValue(final String mtmValue) {
		this.mtmValue = mtmValue;
	}

	public String getMtmCurr() {
		return this.mtmCurr;
	}

	public void setMtmCurr(final String mtmCurr) {
		this.mtmCurr = mtmCurr;
	}

	public String getTradeGroupID() {
		return this.tradeGroupID;
	}

	public void setTradeGroupID(final String tradeGroupID) {
		this.tradeGroupID = tradeGroupID;
	}

	public String getUnderlying() {
		return this.underlying;
	}

	public void setUnderlying(final String underlying) {
		this.underlying = underlying;
	}

	public String getTradeDate() {
		return this.tradeDate;
	}
	
	public String getStartTradeDatesp() {
		return this.startTradeDatesp;
	}
	
	
	public void setStartTradeDatesp(String startTradeDatesp) {
		 this.startTradeDatesp = startTradeDatesp;
	}
	
	public String getTradeDatesp() {
		return this.tradeDatesp;
	}

	public void setTradeDate(final String tradeDate) {
		this.tradeDate = tradeDate;
	}
	
	public void setTradeDatesp(final String tradeDatesp) {
		this.tradeDatesp = tradeDatesp;
	}

	public String getMatDate() {
		return this.matDate;
	}
	
	public String getMatDatesp() {
		return this.matDatesp;
	}

	public void setMatDate(final String matDate) {
		this.matDate = matDate;
	}
	
	public void setMatDatesp(final String matDatesp) {
		this.matDatesp = matDatesp;
	}

	/*
	 * public String getPayRec() { return this.payRec; }
	 * 
	 * public void setPayRec(final String payRec) { this.payRec = payRec; }
	 * 
	 * public double getCouponRate() { return this.couponRate; }
	 * 
	 * public void setCouponRate(final double couponRate) { this.couponRate = couponRate; }
	 * 
	 * public String getCouponPer() { return this.couponPer; }
	 * 
	 * public void setCouponPer(final String couponPer) { this.couponPer = couponPer; }
	 */

	// New getters and Setters - PORTFOLIO RECONCILIATION
	/**
	 * @return the usiReference
	 */
	public String getUsiReference() {
		return this.usiReference;
	}

	/**
	 * @param usiReference
	 *            the usiReference to set
	 */
	public void setUsiReference(String usiReference) {
		this.usiReference = usiReference;
	}

	/**
	 * @return the sdMsp
	 */
	public String getSdMsp() {
		return this.sdMsp;
	}

	/**
	 * @param sdMsp
	 *            the sdMsp to set
	 */
	public void setSdMsp(String sdMsp) {
		this.sdMsp = sdMsp;
	}

	/**
	 * @return the usParty
	 */
	public String getUsParty() {
		return this.UsParty;
	}

	/**
	 * @param usParty
	 *            the usParty to set
	 */
	public void setUsParty(String usParty) {
		this.UsParty = usParty;
	}

	/**
	 * @return the dfaApplicable
	 */
	public String getDfaApplicable() {
		return this.dfaApplicable;
	}

	/**
	 * @param dfaApplicable
	 *            the dfaApplicable to set
	 */
	public void setDfaApplicable(String dfaApplicable) {
		this.dfaApplicable = dfaApplicable;
	}

	/**
	 * @return the fcNfc
	 */
	public String getFcNfc() {
		return this.fcNfc;
	}

	/**
	 * @param fcNfc
	 *            the fcNfc to set
	 */
	public void setFcNfc(String fcNfc) {
		this.fcNfc = fcNfc;
	}

	/**
	 * @return the emirApplicable
	 */
	public String getEmirApplicable() {
		return this.emirApplicable;
	}

	/**
	 * @param emirApplicable
	 *            the emirApplicable to set
	 */
	public void setEmirApplicable(String emirApplicable) {
		this.emirApplicable = emirApplicable;
	}

	public String getFloatRate() {
		return this.floatRate;
	}

	public void setFloatRate(final String floatRate) {
		this.floatRate = floatRate;
	}

	public String getFloatRate2() {
		return this.floatRate2;
	}

	public void setFloatRate2(final String floatRate2) {
		this.floatRate2 = floatRate2;
	}

	/*
	 * public String getFloatIndexPer() { return this.floatIndexPer; }
	 * 
	 * public void setFloatIndexPer(final String floatIndexPer) { this.floatIndexPer = floatIndexPer; }
	 * 
	 * public String getFloat2IndexPer() { return this.float2IndexPer; }
	 * 
	 * public void setFloat2IndexPer(final String float2IndexPer) { this.float2IndexPer = float2IndexPer; }
	 * 
	 * public double getSpread() { return this.spread; }
	 * 
	 * public void setSpread(final double spread) { this.spread = spread; }
	 * 
	 * public String getDaycount() { return this.daycount; }
	 * 
	 * public void setDaycount(final String daycount) { this.daycount = daycount; }
	 * 
	 * public double getFloatPayPer() { return this.floatPayPer; }
	 * 
	 * public void setFloatPayPer(final double floatPayPer) { this.floatPayPer = floatPayPer; }
	 */

	public String getStrikePrice() {
		return this.strikePrice;
	}

	public void setStrikePrice(final String strikePrice) {
		this.strikePrice = strikePrice;
	}

	public String getOptionType() {
		return this.optionType;
	}

	public void setOptionType(final String optionType) {
		this.optionType = optionType;
	}

	public String getExcercise() {
		return this.excercise;
	}

	public void setExcercise(final String excercise) {
		this.excercise = excercise;
	}

	public String getPayRec() {
		return this.payRec;
	}

	public void setPayRec(final String payRec) {
		this.payRec = payRec;
	}

	// new
	public String getCollatAgreeType() {
		return this.collatAgreeType;
	}

	public void setCollatAgreeType(final String collatAgreeType) {
		this.collatAgreeType = collatAgreeType;
	}

	public String getCollatAgree() {
		return this.collatAgree;
	}

	public void setCollatAgree(final String collatAgree) {
		this.collatAgree = collatAgree;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(final String owner) {
		this.owner = owner;
	}

	public String getIndAmount() {
		return this.indAmount;
	}

	public void setIndAmount(final String indAmount) {
		this.indAmount = indAmount;
	}

	public String getBaseCcy() {
		return this.baseCcy;
	}

	public void setBaseCcy(final String baseCcy) {
		this.baseCcy = baseCcy;
	}

	public String getStructure() {
		return this.structure;
	}

	public void setStructure(final String structure) {
		this.structure = structure;
	}

	public String getValAgent() {
		return this.valAgent;
	}

	public void setValAgent(final String valAgent) {
		this.valAgent = valAgent;
	}

	public String getCpty() {
		return this.cpty;
	}

	public void setCpty(final String cpty) {
		this.cpty = cpty;
	}

	public String getTradeID() {
		return this.tradeID;
	}

	public void setTradeID(final String tradeID) {
		this.tradeID = tradeID;
	}

	/**
	 * @return the uti
	 */
	public String getUti() {
		return this.uti;
	}

	/**
	 * @param uti
	 *            the uti to set
	 */
	public void setUti(String uti) {
		this.uti = uti;
	}

}

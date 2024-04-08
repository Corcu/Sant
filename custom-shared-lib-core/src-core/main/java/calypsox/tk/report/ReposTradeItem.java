package calypsox.tk.report;

public class ReposTradeItem {
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
	public static final String REPOS_TRADE_ITEM = "ReposTradeItem";

	// Customized columns.
	private String mtmValue;
	private String mtmCurr;
	private String productClass;
	private String cash;
	private String tradeCurr2;
	private String direction;

	// new
	private String collatAgree;
	private String collatAgreeType;
	private String owner;
	private String indAmount;
	private String baseCcy;
	private String tradeDate;
	private String matDate;
	private String underlying;
	private double closingPrice;
	private String rate;
	private String valAgent;
	private String structure;
	private String mtmDate;
	private double dirtyPrice;
	private String intRate;
	private String nominal;
	private String haircut;
	private String cpty;
	private String tradeID;

	public ReposTradeItem() {
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getMtmValue() {
		return this.mtmValue;
	}

	public void setMtmValue(String mtmValue) {
		this.mtmValue = mtmValue;
	}

	public String getMtmCurr() {
		return this.mtmCurr;
	}

	public void setMtmCurr(String mtmCurr) {
		this.mtmCurr = mtmCurr;
	}

	public String getProductClass() {
		return this.productClass;
	}

	public void setProductClass(String productClass) {
		this.productClass = productClass;
	}

	public String getCash() {
		return this.cash;
	}

	public void setCash(String cash) {
		this.cash = cash;
	}

	public String getTradeCurr2() {
		return this.tradeCurr2;
	}

	public void setTradeCurr2(String tradeCurr2) {
		this.tradeCurr2 = tradeCurr2;
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

	public String getTradeDate() {
		return this.tradeDate;
	}

	public void setTradeDate(final String tradeDate) {
		this.tradeDate = tradeDate;
	}

	public String getMatDate() {
		return this.matDate;
	}

	public void setMatDate(final String matDate) {
		this.matDate = matDate;
	}

	public String getUnderlying() {
		return this.underlying;
	}

	public void setUnderlying(final String underlying) {
		this.underlying = underlying;
	}

	public double getClosingPrice() {
		return this.closingPrice;
	}

	public void setClosingPrice(final double closingPrice) {
		this.closingPrice = closingPrice;
	}

	public String getRate() {
		return this.rate;
	}

	public void setRate(final String rate) {
		this.rate = rate;
	}

	public String getValAgent() {
		return this.valAgent;
	}

	public void setValAgent(final String valAgent) {
		this.valAgent = valAgent;
	}

	public String getStructure() {
		return this.structure;
	}

	public void setStructure(final String structure) {
		this.structure = structure;
	}

	public String getMtmDate() {
		return this.mtmDate;
	}

	public void setMtmDate(final String mtmDate) {
		this.mtmDate = mtmDate;
	}

	public double getDirtyPrice() {
		return this.dirtyPrice;
	}

	public void setDirtyPrice(final double dirtyPrice) {
		this.dirtyPrice = dirtyPrice;
	}

	public String getIntRate() {
		return this.intRate;
	}

	public void setIntRate(final String intRate) {
		this.intRate = intRate;
	}

	public String getNominal() {
		return this.nominal;
	}

	public void setNominal(final String nominal) {
		this.nominal = nominal;
	}

	public String getHaircut() {
		return this.haircut;
	}

	public void setHaircut(final String haircut) {
		this.haircut = haircut;
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

}

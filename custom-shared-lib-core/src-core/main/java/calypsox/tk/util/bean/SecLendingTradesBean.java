package calypsox.tk.util.bean;

public class SecLendingTradesBean {

	private String action;
	private String foSystem;
	private String numFrontId;
	private String owner;
	private String counterparty;
	private String instrument;
	private String portfolio;
	private String valueDate;
	private String tradeDate;
	private String maturityDate;
	private String direction;
	private String nominal; // 12
	private String nominalCcy;
	private String mtm;
	private String mtmCcy;
	private String mtmDate;
	private String boSystem;
	private String boReference;
	private String underlyingType;
	private String underlying;
	private String closingPrice;
	private String structureId;
	private String independentAmount; // 23
	private String independentAmountCcy;
	private String independentAmountPayRecieve;
	private String closingPriceAtStar;
	private String transactionType;
	private String openTerm;
	private String qtyNom;
	private String dividendPct;
	private String terminable;
	private String substitution;
	private String noticeDays;
	private String haircut; // 34
	private String haircutDirection;
	private String lastModified;
	private String tradeVersion;
	private String trader;
	private String susiDate;
	
	private String slbBundle;//40, for SLB trades 
	
	// constructor
	public SecLendingTradesBean(String[] values) {
		
		setAction(values[0]);
		setFoSystem(values[1]);
		setNumFrontId(values[2]);
		setOwner(values[3]);
		setCounterparty(values[4]);
		setInstrument(values[5]);
		setPortfolio(values[6]);
		setValueDate(values[7]);
		setTradeDate(values[8]);
		setMaturityDate(values[9]);
		setDirection(values[10]);
		setNominal(values[11]);
		setNominalCcy(values[12]);
		setMtm(values[13]);
		setMtmCcy(values[14]);
		setMtmDate(values[15]);
		setBoSystem(values[16]);
		setBoReference(values[17]);
		setUnderlyingType(values[18]);
		setUnderlying(values[19]);
		setClosingPrice(values[20]);
		setStructureId(values[21]);
		setIndependentAmount(values[22]);
		setIndependentAmountCcy(values[23]);
		setIndependentAmountPayRecieve(values[24]);
		setClosingPriceAtStar(values[25]);
		setTransactionType(values[26]);
		setOpenTerm(values[27]);
		setQtyNom(values[28]);
		setDividendPct(values[29]);
		setTerminable(values[30]);
		setSubstitution(values[31]);
		setNoticeDays(values[32]);
		setHaircut(values[33]);
		setHaircutDirection(values[34]);
		setLastModified(values[35]);
		setTradeVersion(values[36]);
		setTrader(values[37]);
		setSusiDate(values[38]);
		
	}
	
	// constructor SLB
public SecLendingTradesBean(String[] values, boolean is_SLB) {
		
		this(values);
		
		if (is_SLB) {
			setSLBBundle(values[39]);
		}
	}
	
	// getters and setters
	public String getAction() {
		return this.action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}

	public String getFoSystem() {
		return this.foSystem;
	}

	public void setFoSystem(String foSystem) {
		this.foSystem = foSystem;
	}

	public String getNumFrontId() {
		return this.numFrontId;
	}

	public void setNumFrontId(String numFrontId) {
		this.numFrontId = numFrontId;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCounterparty() {
		return this.counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public String getInstrument() {
		return this.instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getPortfolio() {
		return this.portfolio;
	}

	public void setPortfolio(String portfolio) {
		this.portfolio = portfolio;
	}

	public String getValueDate() {
		return this.valueDate;
	}

	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}

	public String getTradeDate() {
		return this.tradeDate;
	}

	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}

	public String getMaturityDate() {
		return this.maturityDate;
	}

	public void setMaturityDate(String maturityDate) {
		this.maturityDate = maturityDate;
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getNominal() {
		return this.nominal;
	}

	public void setNominal(String nominal) {
		this.nominal = nominal;
	}

	public String getNominalCcy() {
		return this.nominalCcy;
	}

	public void setNominalCcy(String nominalCcy) {
		this.nominalCcy = nominalCcy;
	}

	public String getMtm() {
		return this.mtm;
	}

	public void setMtm(String mtm) {
		this.mtm = mtm;
	}

	public String getMtmCcy() {
		return this.mtmCcy;
	}

	public void setMtmCcy(String mtmCcy) {
		this.mtmCcy = mtmCcy;
	}

	public String getMtmDate() {
		return this.mtmDate;
	}

	public void setMtmDate(String mtmDate) {
		this.mtmDate = mtmDate;
	}

	public String getBoSystem() {
		return this.boSystem;
	}

	public void setBoSystem(String boSystem) {
		this.boSystem = boSystem;
	}

	public String getBoReference() {
		return this.boReference;
	}

	public void setBoReference(String boReference) {
		this.boReference = boReference;
	}

	public String getUnderlyingType() {
		return this.underlyingType;
	}

	public void setUnderlyingType(String underlyingType) {
		this.underlyingType = underlyingType;
	}

	public String getUnderlying() {
		return this.underlying;
	}

	public void setUnderlying(String underlying) {
		this.underlying = underlying;
	}

	public String getClosingPrice() {
		return this.closingPrice;
	}

	public void setClosingPrice(String closingPrice) {
		this.closingPrice = closingPrice;
	}

	public String getStructureId() {
		return this.structureId;
	}

	public void setStructureId(String structureId) {
		this.structureId = structureId;
	}

	public String getIndependentAmount() {
		return this.independentAmount;
	}

	public void setIndependentAmount(String independentAmount) {
		this.independentAmount = independentAmount;
	}

	public String getIndependentAmountCcy() {
		return this.independentAmountCcy;
	}

	public void setIndependentAmountCcy(String independentAmountCcy) {
		this.independentAmountCcy = independentAmountCcy;
	}

	public String getIndependentAmountPayRecieve() {
		return this.independentAmountPayRecieve;
	}

	public void setIndependentAmountPayRecieve(String independentAmountPayRecieve) {
		this.independentAmountPayRecieve = independentAmountPayRecieve;
	}

	public String getClosingPriceAtStar() {
		return this.closingPriceAtStar;
	}

	public void setClosingPriceAtStar(String closingPriceAtStar) {
		this.closingPriceAtStar = closingPriceAtStar;
	}

	public String getTransactionType() {
		return this.transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getOpenTerm() {
		return this.openTerm;
	}

	public void setOpenTerm(String openTerm) {
		this.openTerm = openTerm;
	}

	public String getQtyNom() {
		return this.qtyNom;
	}

	public void setQtyNom(String qtyNom) {
		this.qtyNom = qtyNom;
	}

	public String getDividendPct() {
		return this.dividendPct;
	}

	public void setDividendPct(String dividendPct) {
		this.dividendPct = dividendPct;
	}

	public String getTerminable() {
		return this.terminable;
	}

	public void setTerminable(String terminable) {
		this.terminable = terminable;
	}

	public String getSubstitution() {
		return this.substitution;
	}

	public void setSubstitution(String substitution) {
		this.substitution = substitution;
	}

	public String getNoticeDays() {
		return this.noticeDays;
	}

	public void setNoticeDays(String noticeDays) {
		this.noticeDays = noticeDays;
	}

	public String getHaircut() {
		return this.haircut;
	}

	public void setHaircut(String haircut) {
		this.haircut = haircut;
	}

	public String getHaircutDirection() {
		return this.haircutDirection;
	}

	public void setHaircutDirection(String haircutDirection) {
		this.haircutDirection = haircutDirection;
	}

	public String getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getTradeVersion() {
		return this.tradeVersion;
	}

	public void setTradeVersion(String tradeVersion) {
		this.tradeVersion = tradeVersion;
	}

	public String getTrader() {
		return this.trader;
	}

	public void setTrader(String trader) {
		this.trader = trader;
	}

	public String getSusiDate() {
		return this.susiDate;
	}

	public void setSusiDate(String susiDate) {
		this.susiDate = susiDate;
	}
	
	//for SL trades
	public String getSLBBundle() {
		return this.slbBundle;
	}

	public void setSLBBundle(String slbBundle) {
		this.slbBundle = slbBundle;
	}
	
	
	
	
	

}

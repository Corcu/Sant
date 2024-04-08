package calypsox.tk.util.bean;

public class RepoBean {

	// original fields
	private String action; // 1
	private String foSystem; // 2
	private String numFrontId; // 3
	private String processingOrg; // 4
	private String counterparty; // 5
	private String instrument; // 6
	private String portfolio; // 7
	private String tradeDate; // 8
	private String valueDate; // 9
	private String maturityDate; // 10
	private String direction; // 11
	private String isin; // 12
	private String closingPriceStart; // 13
	private String nominalSec; // 14
	private String nominalSecCcy; // 15
	private String haircut; // 16
	private String haircutDirection; // 17
	private String repoAmount; // 18
	private String repoRate; // 19
	private String repoCcy; // 20
	private String date; // 21
	private String closingPriceDaily; // 22
	private String repoCashVal; // 23
	private String mtm; // 24
	private String mtmCcy; // 25
	private String custodian; // 26
	private String account; // 27
	private String boSystem; // 28
	private String boReference; // 29
	private String repoType; // 30
	private String calcBasis; // 31
	private String repoFreq; // 32
	private String structureId; // 33
	private String independentAmount; // 34
	private String independentAmountCcy; // 35

	// repo enhancements new fields
	private String repoAccruedInterest; // 36
	private String haircutFormula; // 37
	private String cleanPrice; // 38
	private String bondAccruedInterest; // 39
	private String rigCode; // 40
	private String capitalFactor; // 41

	// constructor
	public RepoBean(String[] values) {

		setAction(values[0]);
		setFoSystem(values[1]);
		setNumFrontId(values[2]);
		setProcessingOrg(values[3]);
		setCounterparty(values[4]);
		setInstrument(values[5]);
		setPortfolio(values[6]);
		setTradeDate(values[7]);
		setValueDate(values[8]);
		setMaturityDate(values[9]);
		setDirection(values[10]);
		setIsin(values[11]);
		setClosingPriceStart(values[12]);
		setNominalSec(values[13]);
		setNominalSecCcy(values[14]);
		setHaircut(values[15]);
		setHaircutDirection(values[16]);
		setRepoAmount(values[17]);
		setRepoRate(values[18]);
		setRepoCcy(values[19]);
		setDate(values[20]);
		setClosingPriceDaily(values[21]);
		setRepoCashVal(values[22]);
		setMtm(values[23]);
		setMtmCcy(values[24]);
		setCustodian(values[25]);
		setAccount(values[26]);
		setBoSystem(values[27]);
		setBoReference(values[28]);
		setRepoType(values[29]);
		setCalcBasis(values[30]);
		setRepoFreq(values[31]);
		setStructureId(values[32]);
		setIndependentAmount(values[33]);
		setIndependentAmountCcy(values[34]);
		setRepoAccruedInterest(values[35]);
		setHaircutFormula(values[36]);
		setCleanPrice(values[37]);
		setBondAccruedInterest(values[38]);
		setRigCode(values[39]);
		setCapitalFactor(values[40]);

	}

	public String getCapitalFactor() {
		return this.capitalFactor;
	}

	public void setCapitalFactor(String capitalFactor) {
		this.capitalFactor = capitalFactor;
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

	public String getProcessingOrg() {
		return this.processingOrg;
	}

	public void setProcessingOrg(String processingOrg) {
		this.processingOrg = processingOrg;
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

	public String getTradeDate() {
		return this.tradeDate;
	}

	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}

	public String getValueDate() {
		return this.valueDate;
	}

	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
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

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getClosingPriceStart() {
		return this.closingPriceStart;
	}

	public void setClosingPriceStart(String closingPriceStart) {
		this.closingPriceStart = closingPriceStart;
	}

	public String getNominalSec() {
		return this.nominalSec;
	}

	public void setNominalSec(String nominalSec) {
		this.nominalSec = nominalSec;
	}

	public String getNominalSecCcy() {
		return this.nominalSecCcy;
	}

	public void setNominalSecCcy(String nominalSecCcy) {
		this.nominalSecCcy = nominalSecCcy;
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

	public String getRepoAmount() {
		return this.repoAmount;
	}

	public void setRepoAmount(String repoAmount) {
		this.repoAmount = repoAmount;
	}

	public String getRepoRate() {
		return this.repoRate;
	}

	public void setRepoRate(String repoRate) {
		this.repoRate = repoRate;
	}

	public String getRepoCcy() {
		return this.repoCcy;
	}

	public void setRepoCcy(String repoCcy) {
		this.repoCcy = repoCcy;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getClosingPriceDaily() {
		return this.closingPriceDaily;
	}

	public void setClosingPriceDaily(String closingPriceDaily) {
		this.closingPriceDaily = closingPriceDaily;
	}

	public String getRepoCashVal() {
		return this.repoCashVal;
	}

	public void setRepoCashVal(String repoCashVal) {
		this.repoCashVal = repoCashVal;
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

	public String getCustodian() {
		return this.custodian;
	}

	public void setCustodian(String custodian) {
		this.custodian = custodian;
	}

	public String getAccount() {
		return this.account;
	}

	public void setAccount(String account) {
		this.account = account;
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

	public String getRepoType() {
		return this.repoType;
	}

	public void setRepoType(String repoType) {
		this.repoType = repoType;
	}

	public String getCalcBasis() {
		return this.calcBasis;
	}

	public void setCalcBasis(String calcBasis) {
		this.calcBasis = calcBasis;
	}

	public String getRepoFreq() {
		return this.repoFreq;
	}

	public void setRepoFreq(String repoFreq) {
		this.repoFreq = repoFreq;
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

	public String getRepoAccruedInterest() {
		return this.repoAccruedInterest;
	}

	public void setRepoAccruedInterest(String repoAccruedInterest) {
		this.repoAccruedInterest = repoAccruedInterest;
	}

	public String getHaircutFormula() {
		return this.haircutFormula;
	}

	public void setHaircutFormula(String haircutFormula) {
		this.haircutFormula = haircutFormula;
	}

	public String getCleanPrice() {
		return this.cleanPrice;
	}

	public void setCleanPrice(String cleanPrice) {
		this.cleanPrice = cleanPrice;
	}

	public String getBondAccruedInterest() {
		return this.bondAccruedInterest;
	}

	public void setBondAccruedInterest(String bondAccruedInterest) {
		this.bondAccruedInterest = bondAccruedInterest;
	}

	public String getRigCode() {
		return this.rigCode;
	}

	public void setRigCode(String rigCode) {
		this.rigCode = rigCode;
	}
}

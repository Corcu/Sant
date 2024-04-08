package calypsox.tk.util.bean;

import com.calypso.tk.core.Book;

/**
 * Bean class to represent a trade to be imported from a flat file
 *
 * @author aela
 * @version 2.1
 */
public class InterfaceTradeBean implements Cloneable {

    // -------------field name ------------------ position
    private String action; // 1
    private String foSystem; // 2
    private String numFrontId; // 3
    private String processingOrg; // 4
    private String counterparty; // 5
    private String instrument; // 6
    private String portfolio; // 7
    private String valueDate; // 8
    private String tradeDate; // 9
    private String maturityDate; // 10
    private String direction; // 11
    private String nominal; // 12
    private String nominalCcy; // 13
    private String mtm; // 14
    private String mtmCcy; // 15
    private String mtmDate; // 16
    private String boSystem; // 17
    private String boReference; // 18
    private String underlayingType; // 19
    private String underlaying; // 20
    private String closingPriceDaily; // 21
    private String structureId; // 22
    private String independentAmount; // 23
    private String independentAmountCcy; // 24
    private String independentAmountPayReceive; // 25
    private String closingPriceStart; // 26
    private String nominalSec; // 27
    private String nominalSecCcy; // 28
    private String haircut; // 29
    private String haircutDirection; // 30
    private String repoRate; // 31
    private String callPut; // 32
    private String lastModified; // 33
    private String tradeVersion; // 34
    // NEW FIELDS - DFA & EMIR
    private String usi; // 35
    private String sdMsp; // 36
    private String usParty; // 37
    private String dfa; // 38
    private String fcNfc; // 39
    private String emir; // 40
    // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
    private String uti; // 41
    // NEW FIELDS - RIG
    private String rigCode; // 42
    // GSM: 24/04/2014. PdV adaptation in exposure importation (3 more fields)
    private String lotSize; // 43
    private String accruedCoupon; // 44

    // PDV new fields
    // DELIVERY_TYPE: DVP or FOP
    private String deliveryType; // 45
    // IS_FINANCEMENT: SI or NO
    private String isFinancement; // 46

    //ACD 10/05/2016 IM
    private String upi; // 47

    //SLB 25/09/18:
    private String slbBundle;//48

    // class variables
    private int lineNumber;
    private String lineContent;
    private int lineNumber2;
    private String lineContent2;

    // PDV: exposure trade from Murex
    private boolean isPDV = false;
    //SLB
    private boolean isSLB = false;

    private String sbsProduct;//optinal
    private String sbsdMsbsd;//optional
    private String dayCountConvention;//optional
    private String swapAgentId ;//optional
    private String swapAgent  ;//optional

    protected InterfaceTradeBean legTwo;
    protected boolean errorChecks = false;
    protected boolean warningChecks = false;
    protected long tradeId;
    protected String npvMtm;

    protected Book book;

    public InterfaceTradeBean() {
        this.boReference = "";
        this.lineContent = "";
        this.tradeId = 0;
        this.legTwo = null;
    }

    // constructor
    public InterfaceTradeBean(String[] values) {

        setAction(values[0]);
        setFoSystem(values[1]);
        setNumFrontId(values[2]);
        setProcessingOrg(values[3]);
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
        setUnderlayingType(values[18]);
        setUnderlaying(values[19]);
        setClosingPriceDaily(values[20]);

        setStructureId(values[21]);
        setIndependentAmount(values[22]);
        setIndependentAmountCcy(values[23]);
        setIndependentAmountPayReceive(values[24]);
        setClosingPriceStart(values[25]);
        setNominalSec(values[26]);
        setNominalSecCcy(values[27]);
        setHaircut(values[28]);
        setHaircutDirection(values[29]);
        setRepoRate(values[30]);
        setCallPut(values[31]);
        setLastModified(values[32]);
        setTradeVersion(values[33]);

        // NEW FIELDS - DFA & EMIR
        setUsi(values[34]);
        setSdMsp(values[35]);
        setUsParty(values[36]);
        setDfa(values[37]);
        setFcNfc(values[38]);
        setEmir(values[39]);
        // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
        setUti(values[40]);
        // RIG CODE
        setRigCode(values[41]);

        // GSM: 24/04/2014. PdV adaptation in exposure importation (3 more fields)
        setLotSize(values[42]);
        setAccruedCoupon(values[43]);

    }

    // constructor
    public InterfaceTradeBean(String[] values, boolean is_SLB) {

        this(values);
        //SLB: 25/09/18 attrib SLB
        if (is_SLB) {
            setSLBBundle(values[47]);
        }


    }

    /**
     * @return the action
     */
    public String getAction() {
        return this.action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the foSystem
     */
    public String getFoSystem() {
        return this.foSystem;
    }

    /**
     * @param foSystem the foSystem to set
     */
    public void setFoSystem(String foSystem) {
        this.foSystem = foSystem;
    }

    /**
     * @return the numFrontId
     */
    public String getNumFrontId() {
        return this.numFrontId;
    }

    /**
     * @param numFrontId the numFrontId to set
     */
    public void setNumFrontId(String numFrontId) {
        this.numFrontId = numFrontId;
    }

    /**
     * @return the processingOrg
     */
    public String getProcessingOrg() {
        return this.processingOrg;
    }

    /**
     * @param processingOrg the processingOrg to set
     */
    public void setProcessingOrg(String processingOrg) {
        this.processingOrg = processingOrg;
    }

    /**
     * @return the counterparty
     */
    public String getCounterparty() {
        return this.counterparty;
    }

    /**
     * @param counterparty the counterparty to set
     */
    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    /**
     * @return the instrument
     */
    public String getInstrument() {
        return this.instrument;
    }

    /**
     * @param instrument the instrument to set
     */
    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    /**
     * @return the portfolio
     */
    public String getPortfolio() {
        return this.portfolio;
    }

    /**
     * @param portfolio the portfolio to set
     */
    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    /**
     * @return the valueDate
     */
    public String getValueDate() {
        return this.valueDate;
    }

    /**
     * @param valueDate the valueDate to set
     */
    public void setValueDate(String valueDate) {
        this.valueDate = valueDate;
    }

    /**
     * @return the tradeDate
     */
    public String getTradeDate() {
        return this.tradeDate;
    }

    /**
     * @param tradeDate the tradeDate to set
     */
    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    /**
     * @return the maturityDate
     */
    public String getMaturityDate() {
        return this.maturityDate;
    }

    /**
     * @param maturityDate the maturityDate to set
     */
    public void setMaturityDate(String maturityDate) {
        this.maturityDate = maturityDate;
    }

    /**
     * @return the direction
     */
    public String getDirection() {
        return this.direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * @return the nominal
     */
    public String getNominal() {
        return this.nominal;
    }

    /**
     * @param nominal the nominal to set
     */
    public void setNominal(String nominal) {
        this.nominal = nominal;
    }

    /**
     * @return the nominalCcy
     */
    public String getNominalCcy() {
        return this.nominalCcy;
    }

    /**
     * @param nominalCcy the nominalCcy to set
     */
    public void setNominalCcy(String nominalCcy) {
        this.nominalCcy = nominalCcy;
    }

    /**
     * @return the mtm
     */
    public String getMtm() {
        return this.mtm;
    }

    /**
     * @param mtm the mtm to set
     */
    public void setMtm(String mtm) {
        this.mtm = mtm;
    }

    /**
     * @return the mtmCcy
     */
    public String getMtmCcy() {
        return this.mtmCcy;
    }

    /**
     * @param mtmCcy the mtmCcy to set
     */
    public void setMtmCcy(String mtmCcy) {
        this.mtmCcy = mtmCcy;
    }

    /**
     * @return the mtmDate
     */
    public String getMtmDate() {
        return this.mtmDate;
    }

    /**
     * @param mtmDate the mtmDate to set
     */
    public void setMtmDate(String mtmDate) {
        this.mtmDate = mtmDate;
    }

    /**
     * @return the boSystem
     */
    public String getBoSystem() {
        return this.boSystem;
    }

    /**
     * @param boSystem the boSystem to set
     */
    public void setBoSystem(String boSystem) {
        this.boSystem = boSystem;
    }

    /**
     * @return the boReference
     */
    public String getBoReference() {
        return this.boReference;
    }

    /**
     * @param boReference the boReference to set
     */
    public void setBoReference(String boReference) {
        this.boReference = boReference;
    }

    /**
     * @return the underlayingType
     */
    public String getUnderlayingType() {
        return this.underlayingType;
    }

    /**
     * @param underlayingType the underlayingType to set
     */
    public void setUnderlayingType(String underlayingType) {
        this.underlayingType = underlayingType;
    }

    /**
     * @return the underlaying
     */
    public String getUnderlaying() {
        return this.underlaying;
    }

    /**
     * @param underlaying the underlaying to set
     */
    public void setUnderlaying(String underlaying) {
        this.underlaying = underlaying;
    }

    /**
     * @return the closingPriceDaily
     */
    public String getClosingPriceDaily() {
        return this.closingPriceDaily;
    }

    /**
     * @param closingPriceDaily the closingPriceDaily to set
     */
    public void setClosingPriceDaily(String closingPriceDaily) {
        this.closingPriceDaily = closingPriceDaily;
    }

    /**
     * @return the structureId
     */
    public String getStructureId() {
        return this.structureId;
    }

    /**
     * @param structureId the structureId to set
     */
    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    /**
     * @return the independentAmount
     */
    public String getIndependentAmount() {
        return this.independentAmount;
    }

    /**
     * @param independentAmount the independentAmount to set
     */
    public void setIndependentAmount(String independentAmount) {
        this.independentAmount = independentAmount;
    }

    /**
     * @return the independentAmountCcy
     */
    public String getIndependentAmountCcy() {
        return this.independentAmountCcy;
    }

    /**
     * @param independentAmountCcy the independentAmountCcy to set
     */
    public void setIndependentAmountCcy(String independentAmountCcy) {
        this.independentAmountCcy = independentAmountCcy;
    }

    /**
     * @return the independentAmountPayReceive
     */
    public String getIndependentAmountPayReceive() {
        return this.independentAmountPayReceive;
    }

    /**
     * @param independentAmountPayReceive the independentAmountPayReceive to set
     */
    public void setIndependentAmountPayReceive(String independentAmountPayReceive) {
        this.independentAmountPayReceive = independentAmountPayReceive;
    }

    /**
     * @return the closingPriceStart
     */
    public String getClosingPriceStart() {
        return this.closingPriceStart;
    }

    /**
     * @param closingPriceStart the closingPriceStart to set
     */
    public void setClosingPriceStart(String closingPriceStart) {
        this.closingPriceStart = closingPriceStart;
    }

    /**
     * @return the nominalSec
     */
    public String getNominalSec() {
        return this.nominalSec;
    }

    /**
     * @param nominalSec the nominalSec to set
     */
    public void setNominalSec(String nominalSec) {
        this.nominalSec = nominalSec;
    }

    /**
     * @return the nominalSecCcy
     */
    public String getNominalSecCcy() {
        return this.nominalSecCcy;
    }

    /**
     * @param nominalSecCcy the nominalSecCcy to set
     */
    public void setNominalSecCcy(String nominalSecCcy) {
        this.nominalSecCcy = nominalSecCcy;
    }

    /**
     * @return the haircut
     */
    public String getHaircut() {
        return this.haircut;
    }

    /**
     * @param haircut the haircut to set
     */
    public void setHaircut(String haircut) {
        this.haircut = haircut;
    }

    /**
     * @return the haircutDirection
     */
    public String getHaircutDirection() {
        return this.haircutDirection;
    }

    /**
     * @param haircutDirection the haircutDirection to set
     */
    public void setHaircutDirection(String haircutDirection) {
        this.haircutDirection = haircutDirection;
    }

    /**
     * @return the repoRate
     */
    public String getRepoRate() {
        return this.repoRate;
    }

    /**
     * @param repoRate the repoRate to set
     */
    public void setRepoRate(String repoRate) {
        this.repoRate = repoRate;
    }

    /**
     * @return the callPut
     */
    public String getCallPut() {
        return this.callPut;
    }

    /**
     * @param callPut the callPut to set
     */
    public void setCallPut(String callPut) {
        this.callPut = callPut;
    }

    /**
     * @return the lastModified
     */
    public String getLastModified() {
        return this.lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the tradeVersion
     */
    public String getTradeVersion() {
        return this.tradeVersion;
    }

    /**
     * @param tradeVersion the tradeVersion to set
     */
    public void setTradeVersion(String tradeVersion) {
        this.tradeVersion = tradeVersion;
    }

    /**
     * @return usi the usi unique id for operations
     */
    public String getUsi() {
        return this.usi;
    }

    /**
     * @param usi the usi unique id to set
     */
    public void setUsi(String usi) {
        this.usi = usi;
    }

    /**
     * @return the sdMsp
     */
    public String getSdMsp() {
        return this.sdMsp;
    }

    /**
     * @param sdMsp the sdMsp to set
     */
    public void setSdMsp(String sdMsp) {
        this.sdMsp = sdMsp;
    }

    /**
     * @return the usParty
     */
    public String getUsParty() {
        return this.usParty;
    }

    /**
     * @param usParty the usParty to set
     */
    public void setUsParty(String usParty) {
        this.usParty = usParty;
    }

    /**
     * @return the dfa, affected by the dodd frank act
     */
    public String getDfa() {
        return this.dfa;
    }

    /**
     * @param dfa the dfa to set
     */
    public void setDfa(String dfa) {
        this.dfa = dfa;
    }

    /**
     * @return the fcNfc
     */
    public String getFcNfc() {
        return this.fcNfc;
    }

    /**
     * @param fcNfc the fcNfc to set
     */
    public void setFcNfc(String fcNfc) {
        this.fcNfc = fcNfc;
    }

    /**
     * @return the emir
     */
    public String getEmir() {
        return this.emir;
    }

    /**
     * @param emir the emir to set
     */
    public void setEmir(String emir) {
        this.emir = emir;
    }

    /**
     * @return the rigCode
     */
    public String getRigCode() {
        return this.rigCode;
    }

    /**
     * @param rigCode the rigCode to set
     */
    public void setRigCode(String rigCode) {
        this.rigCode = rigCode;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * @return the lineNumber
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /**
     * @param lineNumber the lineNumber to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * @return the lineContent
     */
    public String getLineContent() {
        return this.lineContent;
    }

    /**
     * @param lineContent the lineContent to set
     */
    public void setLineContent(String lineContent) {
        this.lineContent = lineContent;
    }

    /**
     * @return the lineNumber2
     */
    public int getLineNumber2() {
        return this.lineNumber2;
    }

    /**
     * @param lineNumber2 the lineNumber2 to set
     */
    public void setLineNumber2(int lineNumber2) {
        this.lineNumber2 = lineNumber2;
    }

    /**
     * @return the lineContent2
     */
    public String getLineContent2() {
        return this.lineContent2;
    }

    /**
     * @param lineContent2 the lineContent2 to set
     */
    public void setLineContent2(String lineContent2) {
        this.lineContent2 = lineContent2;
    }

    /**
     * @return the legTwo
     */
    public InterfaceTradeBean getLegTwo() {
        return this.legTwo;
    }

    /**
     * @param legTwo the legTwo to set
     */
    public void setLegTwo(InterfaceTradeBean legTwo) {
        this.legTwo = legTwo;
    }

    /**
     * @return the book
     */
    public Book getBook() {
        return this.book;
    }

    /**
     * @param book the book to set
     */
    public void setBook(Book book) {
        this.book = book;
    }

    /**
     * /**
     *
     * @return the tradeId
     */
    public long getTradeId() {
        return this.tradeId;
    }

    /**
     * @param tradeId the tradeId to set
     */
    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    /**
     * @return the warningChecks
     */
    public boolean isWarningChecks() {
        return this.warningChecks;
    }

    /**
     * @param warningChecks the warningChecks to set
     */
    public void setWarningChecks(boolean warningChecks) {
        this.warningChecks = warningChecks;
    }

    /**
     * @return the errorChecks
     */
    public boolean isErrorChecks() {
        return this.errorChecks;
    }

    /**
     * @param errorChecks the errorChecks to set
     */
    public void setErrorChecks(boolean errorChecks) {
        this.errorChecks = errorChecks;
    }

    /**
     * @return the uti setted reference
     */
    public String getUti() {
        return this.uti;
    }

    /**
     * @param uti the uti to set
     */
    public void setUti(String uti) {
        this.uti = uti;
    }

    /**
     * @return the lotSize
     */
    public String getLotSize() {
        return this.lotSize;
    }

    /**
     * @param lotSize the lotSize to set
     */
    public void setLotSize(String lotSize) {
        this.lotSize = lotSize;
    }

    /**
     * @return the accruedCoupon
     */
    public String getAccruedCoupon() {
        return this.accruedCoupon;
    }

    /**
     * @param accruedCoupon the accruedCoupon to set
     */
    public void setAccruedCoupon(String accruedCoupon) {
        this.accruedCoupon = accruedCoupon;
    }

    /**
     * @param factoredMtm the accruedCoupon to set
     */
    public void setMtmNpv(String factoredMtm) {
        this.npvMtm = factoredMtm;
    }

    /**
     * @return the factoredMtm
     */
    public String getMtmNpv() {
        return this.npvMtm;
    }

    public String getDeliveryType() {
        return this.deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getIsFinancement() {
        return this.isFinancement;
    }

    public void setIsFinancement(String isFinancement) {
        this.isFinancement = isFinancement;
    }

    public String getNpvMtm() {
        return this.npvMtm;
    }

    public void setNpvMtm(String npvMtm) {
        this.npvMtm = npvMtm;
    }

    public boolean isPDV() {
        return this.isPDV;
    }

    public void setPDV(boolean isPDV) {
        this.isPDV = isPDV;
    }

    public String getBoKey() {
        return getBoReference().trim() + getBoSystem().trim();
    }

    //IM
    public String getUpi() {
        return upi;
    }

    public void setUpi(String upi) {
        this.upi = upi;
    }

    public void fillPDVFields(String[] values) {
        // ALLOCATION FIELDS
        if (this.isPDV) {
            setDeliveryType(values[44]);
            setIsFinancement(values[45]);
        }
    }

    //ACD 10/05/2016 IM
    public void UPIField(String[] values) {
        setUpi(values[46]);
    }

    //for SL trades
    public boolean isSLB() {
        return this.isSLB;
    }


    public void setIsSLB(boolean isSLB) {
        this.isSLB = isSLB;

    }

    //TODO Pendiente de confiramción MUREX 44/47
    public void fillSLBFields(String[] values) {
        if (this.isSLB) {
            setSLBBundle(values[44]);
        }

    }

    public String getSbsProduct() {
        return sbsProduct;
    }

    public String getSbsdMsbsd() {
        return sbsdMsbsd;
    }

    public String getdayCountConvention() {
        return dayCountConvention;
    }

    public String getSwapAgentId() {
        return swapAgentId;
    }

    public String getSwapAgent() {
        return swapAgent;
    }

    public void fillsbsdMsbsd(String[] values) {
        this.sbsdMsbsd = "";
        if(47<values.length){
            this.sbsdMsbsd = values[47];
        }
    }

    public void fillsbsProduct(String[] values) {
        this.sbsProduct = "";
        if(48<values.length){
            this.sbsProduct = values[48];
        }
    }

    public void filldayCountConvention(String[] values) {
        this.dayCountConvention = "";
        if(49<values.length){
            this.dayCountConvention = values[49];
        }
    }

    public void fillSwapAgentId(String[] values) {
        this.swapAgentId = "";
        if(50<values.length){
            this.swapAgentId = values[50];
        }
    }

    public void fillSwapAgent(String[] values) {
        this.swapAgent = "";
        if(51<values.length){
            this.swapAgent = values[51];
        }
    }

    public String getSLBBundle() {
        return this.slbBundle;
    }

    public void setSLBBundle(String slbBundle) {
        this.slbBundle = slbBundle;
    }


}

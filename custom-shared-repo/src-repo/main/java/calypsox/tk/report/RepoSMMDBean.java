package calypsox.tk.report;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.fieldentry.FieldEntry;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

public class RepoSMMDBean {

    String status = "";
    String ptiOldValue = "";
    boolean ptiUpdatedOnD = false;
    String prtryTradeid = "";
    String cptyName = "";
    String cptyLocation = "";
    String txnType = "";
    String cashPrincipal = "";
    String rateType = "";
    String dealRate = "";
    String brokeredDeal = "";
    String collateralCCy = "";
    String secValue = "";
    String haircutValue = "";
    String basisPoint = "";
    String index = "";
    Trade fatherRepo = null;
    JDate maturityDate = null;
    JDatetime eventDate = null;
    JDatetime tradeDate = null;
    JDatetime settlementDate = null;
    boolean showJMin = false;
    boolean isPLedge = false;

    public void init(Trade trade, Repo repo,JDate valDate){

        setPrtryTradeid(trade);
        setMaturityDate(valDate,repo);
        setNameLocation(trade);
        setTxnType(repo);
        setRate(valDate,repo);
        setBrokeredDeal(trade);
        setCollateralCCy(repo);
        setCashPrincipal(repo);
        setValues(valDate,trade);
        setShowJMin(trade);
        setCollateralNominal(valDate,repo,trade);
        setDates(trade,repo);
    }

    public void init(Trade trade, Product product,JDate valDate){
        Product security = null;
        if(Optional.ofNullable(product).isPresent()){
            if(product instanceof Repo){
                Repo repo = (Repo) product;
                security = repo.getSecurity();
                setMaturityDate(valDate,repo);
                setTxnType(repo);
                setRate(valDate,repo);
                setCashPrincipal(repo);
                setCollateralNominal(valDate,repo,trade);
                setDates(trade,repo);
                setValues(valDate,trade);

            }else if(product instanceof Pledge){
                isPLedge = true;
                fatherRepo = this.loadFatherTrade(trade);
                if(null!=fatherRepo){
                    Repo fatherRepoProduct = (Repo) fatherRepo.getProduct();
                    initPledgeFromRepo(trade,fatherRepo);
                    setValues(valDate,fatherRepo);
                    setRate(valDate,fatherRepoProduct);
                    setCashPrincipal(fatherRepoProduct);
                }
                Pledge pledge = (Pledge)product;
                security = pledge.getSecurity();
                setMaturityDate(pledge);
                setMaturityDate(pledge);
                setTxnType(pledge);
                setDates(trade,pledge);
                setPledgeCollateralNominal(trade);
            }
        }

        setCollateralCCy(security);
        setPrtryTradeid(trade);
        setNameLocation(trade);
        setBrokeredDeal(trade);
        setShowJMin(trade);
    }

    private void initPledgeFromRepo(Trade pledge,Trade repo){
        if(null!=pledge && null!=repo){
            pledge.addKeyword("MurexTradeID",repo.getKeywordValue("MurexTradeID"));
        }
    }

    private Trade loadFatherTrade(Trade trade){
        try {
            final long fatherLongId = Long.parseLong(trade.getInternalReference());
            final Trade fatherRepo = DSConnection.getDefault().getRemoteTrade().getTrade(fatherLongId);
            return null!=fatherRepo && fatherRepo.getProduct() instanceof Repo ? fatherRepo : null;
        }catch (Exception e){
            Log.error(this,"Error: " + e);
        }
        return null;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public void setPrtryTradeid(Trade trade){
        if(RepoSMMDReport.NEWT.equalsIgnoreCase(status)){
            prtryTradeid = trade.getKeywordValue("SMMDUpdatePTI");
        }else if(RepoSMMDReport.AMND.equalsIgnoreCase(status)
                || RepoSMMDReport.CANC.equalsIgnoreCase(status)){
            if(ptiUpdatedOnD){
                prtryTradeid = ptiOldValue;
            }else {
                prtryTradeid = trade.getKeywordValue("SMMDUpdatePTI");
            }
        }
    }

    public void setNameLocation(Trade trade){
        LegalEntity counterParty = trade.getCounterParty();
        if(null!=counterParty){
            Vector<LegalEntityAttribute> legalEntityAttributes = (Vector<LegalEntityAttribute>) counterParty.getLegalEntityAttributes();
            if (!Util.isEmpty(legalEntityAttributes)) {
                boolean present = legalEntityAttributes.stream().filter(att -> "LEI".equalsIgnoreCase(att.getAttributeType())).findFirst().map(LegalEntityAttribute::getAttributeValue).isPresent();
                if(!present){
                    cptyName = counterParty.getName();
                    cptyLocation = getCptyCountry(trade);
                }
            }
        }
    }

    public void setTxnType(Repo repo){
        String direction = repo.getDirection(Repo.REPO,repo.getSign());
        if("Repo".equalsIgnoreCase(direction)){
            txnType = "BORR";
        }else if("Reverse".equalsIgnoreCase(direction)){
            txnType = "LEND";
        }
    }
    public void setTxnType(Pledge pledge){
        if(pledge.getQuantity()>=0){
            txnType = "LEND";
        }else {
            txnType = "BORR";
        }
    }

    public void setRate(JDate valDate,Repo repo){
        Cash cash = repo.getCash();
        rateType = null!=cash && cash.getFixedRateB() ? "FIXE" : "VARI";
        if("FIXE".equalsIgnoreCase(rateType)){
            dealRate = formatDealRate(cash.getSpecificFixedRate(valDate)*100);
        }
        if("VARI".equalsIgnoreCase(rateType)){
            String smmdIndex = SecFinanceTradeUtil.getInstance().getSMMDIndex(cash);
            index = SecFinanceTradeUtil.getInstance().getSMMDSusiIndex(smmdIndex);
        }
    }


    public void setCashPrincipal(Repo repo){
        Cash cash = repo.getCash();
        cashPrincipal = formatAmount(Math.abs(cash.getLatestPrincipalAmount()));
    }

    public void setPledgeCollateralNominal(Trade trade){
        this.secValue = formatAmount(Math.abs(trade.computeNominal()));
    }

    public void setValues(JDate effectiveDate,Trade trade){
        if(null!=trade){
            SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();
            SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade, effectiveDate.getJDatetime(TimeZone.getDefault()), null, context);

            if (null != externalSecFinanceTradeEntry) {
                //Object secNominal = Optional.ofNullable(externalSecFinanceTradeEntry.get("Sec. Nominal")).map(FieldEntry::getValue).orElse("");
                Object haircutValue = Optional.ofNullable(externalSecFinanceTradeEntry.get("Sec. Haircut Value")).map(FieldEntry::getValue).orElse("");
                Object cash = Optional.ofNullable(externalSecFinanceTradeEntry.get("Cash. Spread")).map(FieldEntry::getValue).orElse("");

                if("VARI".equalsIgnoreCase(rateType)) {
                    String value= String.valueOf(cash);
                    if(null!=value && !"null".equalsIgnoreCase(value) && !Util.isEmpty(value)){
                        if(value.contains(",")){
                            value = value.replace(",",".");
                        }
                        basisPoint = formatAmount(Double.parseDouble(value));
                    }else {
                        basisPoint = "0";
                    }
                }
                if(haircutValue instanceof Rate){
                    Double value = ((Rate) haircutValue).get();
                    this.haircutValue = formatDealRate(value*100);
                }
            }
        }
    }

    public void setBrokeredDeal(Trade trade){
        String mxGlobalID = trade.getKeywordValue("MurexGlobalId");
        if(!Util.isEmpty(mxGlobalID) && mxGlobalID.contains("TOMS-")){
            brokeredDeal = "BROK";
        }else{
            brokeredDeal = "BILA";
        }
    }
    public void setCollateralCCy(Product security){
        collateralCCy = Optional.ofNullable(security).map(Product::getCurrency).orElse("");
    }

    /**
     * Format Amount
     * @param value
     * @return
     */
    public String formatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("#############");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        myFormatter.setRoundingMode(RoundingMode.DOWN);
        if (value != null && value!=0.00) {
            String format = myFormatter.format(value);
            //format = format.replace(".","");
            //format = format.replaceFirst("^0+(?!$)", "");
            return format;
        } else {
            return "";
        }
    }

    /**
     * Format Double .013
     * @param value
     * @return
     */
    public String formatDealRate(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("###.000");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        myFormatter.setRoundingMode(RoundingMode.DOWN);
        if (value != null) {
            if (value == 0.0) {
                return ".000";
            }
            return myFormatter.format(value);
        }
        return ".000";
    }

    public void setCollateralNominal(JDate effectiveDate, Repo repo,Trade trade){
        Vector collaterals = repo.getCollaterals(effectiveDate);
        if(trade.getStatus().toString().equalsIgnoreCase(Status.TERMINATED)
        && Util.isEmpty(collaterals)){
            collaterals =  repo.getCollaterals();
        }
        if(!Util.isEmpty(collaterals)){
            Collateral collateral = (Collateral)collaterals.get(0);
            double quantity = collateral.getQuantity(effectiveDate);
            Product security = collateral.getSecurity();
            if(security instanceof Bond){
                double faceValue = ((Bond) security).getFaceValue(effectiveDate);
                this.secValue = formatAmount(Math.abs(quantity*faceValue));
            }
        }
    }

    public void setMaturityDate(JDate effectiveDate, Repo repo){
        if(repo.isOpen()) {
            Locale aDefault = Locale.getDefault();
            JDate jDate = null;
            if("en".equalsIgnoreCase(aDefault.toString())){
                jDate = Util.stringToJDate("12/31/9999");
            }else {
                jDate = Util.stringToJDate("31/12/9999");
            }
            this.maturityDate = jDate;
        }else {
            maturityDate = repo.getEndDate();
        }

    }

    public void setMaturityDate(Pledge pledge){
        maturityDate = pledge.getEndDate();
    }

    /**
     * Exclude by Legal Entity Attribute SECTORCONTABLE and DV SectorContable
     *
     * @param trade
     * @return
     */
    private void setShowJMin(Trade trade) {
        Vector<LegalEntityAttribute> legalEntityAttributes = (Vector<LegalEntityAttribute>) trade.getCounterParty().getLegalEntityAttributes();
        String sectorcontable = "";
        if (!Util.isEmpty(legalEntityAttributes)) {
            sectorcontable = legalEntityAttributes.stream().filter(att -> "SECTORCONTABLE".equalsIgnoreCase(att.getAttributeType())).findFirst().map(LegalEntityAttribute::getAttributeValue).orElse("");
        }
        Vector<String> sleepBookNames = LocalCache.getDomainValues(DSConnection.getDefault(), "RepoSMMDSectorContableLCR");
        if (!Util.isEmpty(sleepBookNames)) {
            showJMin = sleepBookNames.contains(sectorcontable);
        }
    }

    private String getCptyCountry(Trade trade) {
        String country = trade.getCounterParty().getCountry();
        String isoCode = BOCache.getCountry(DSConnection.getDefault(), country).getISOCode();
        return !Util.isEmpty(isoCode) ? isoCode : "";
    }

    private void setDates(Trade trade,Repo repo){
        if(!"NEWT".equalsIgnoreCase(this.status)){
            this.tradeDate = trade.getTradeDate();
            this.settlementDate = repo.getStartDate().getJDatetime(TimeZone.getDefault());
        }else {
            this.tradeDate = this.eventDate;
            this.settlementDate = this.eventDate;
        }
    }

    private void setDates(Trade trade,Pledge pledge){
        if(!"NEWT".equalsIgnoreCase(this.status)){
            this.tradeDate = trade.getTradeDate();
            this.settlementDate = pledge.getStartDate().getJDatetime(TimeZone.getDefault());
        }else {
            this.tradeDate = this.eventDate;
            this.settlementDate = this.eventDate;
        }
    }

    public boolean isPtiUpdatedOnD() {
        return ptiUpdatedOnD;
    }

    public void setPtiUpdatedOnD(boolean ptiUpdatedOnD) {
        this.ptiUpdatedOnD = ptiUpdatedOnD;
    }

    public String getPtiOldValue() {
        return ptiOldValue;
    }

    public void setPtiOldValue(String ptiOldValue) {
        this.ptiOldValue = ptiOldValue;
    }

    public JDatetime getEventDate() {
        return eventDate;
    }

    public void setEventDate(JDatetime eventDate) {
        this.eventDate = eventDate;
    }
}

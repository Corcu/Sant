package calypsox.tk.bo.fiflow.builder.trade;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTradeSecurityHandler;
import calypsox.tk.bo.fiflow.staticdata.FIFlowStaticData;
import calypsox.tk.bo.util.StringUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.util.FdnUtilProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class BondTcyccTradeBasedBuilder {

    Trade trade;
    JDate valDate;

    PricingEnv pricingEnv;

    private final FIFlowTradeSecurityHandler securityWrapper = new FIFlowTradeSecurityHandler();
    private final String rigCodeKwdName = "RIG_CODE";
    private final String mxGIDKwdName = "Mx GID";
    private final String roiKwdName = "Mx ROI";

    public BondTcyccTradeBasedBuilder(Trade trade, JDate valDate,PricingEnv pricingEnv) {
        this.trade = trade;
        this.valDate = valDate;
        this.pricingEnv=pricingEnv;
        if (trade != null) {
            this.securityWrapper.initRelatedSecutityData(trade.getProduct());
            this.securityWrapper.initRelatedSecPricesData(getPreviousBusinessDay());
        }
    }

    //Persona
    public String buildTipersonField() {
        return getLEExternalRef(trade.getCounterParty())
                .map(s -> s.substring(0, 1)).orElse("");
    }

    public Integer buildCdPersonField() {
        String jAgentStr = getLEExternalRef(trade.getCounterParty())
                .map(s -> s.substring(1)).orElse("0");
        return Integer.parseInt(jAgentStr);
    }

    /**
     * @param legalEntity
     * @return
     */
    protected Optional<String> getLEExternalRef(LegalEntity legalEntity) {
        return Optional.ofNullable(legalEntity).map(LegalEntity::getExternalRef);
    }

    /*
     * Dates
     */
    public JDate buildFVtoOperField() {
        return Optional.ofNullable(this.trade).map(Trade::getSettleDate).orElse(null);
    }

    public JDatetime buildFConOperField() {
        return Optional.ofNullable(this.trade).map(Trade::getTradeDate).orElse(null);
    }
    public String buildHConOperField() {
        JDatetime date = Optional.ofNullable(this.trade).map(Trade::getTradeDate).orElse(null);
        return date != null ? formatHConOperField(date) : "000000";
    }

    private String formatHConOperField(JDatetime date) {
    return formatTime(date.getHours()) + formatTime(date.getMinutes()) + formatTime(date.getSeconds());
    }

    private String formatTime(int field) {
        String out = String.valueOf(field);
        return out != null ? out.length()< 2 ? "0" + out : out : "00";
    }

    /*
     * Trade Details
     */

    public String buildIdSentOp() {
        boolean isNegativeNominal = isNegative(buildIPrinOpe());
        int idSendFlj;
        if (isNegativeNominal) {
            idSendFlj = 4;
        } else {
            idSendFlj = 3;
        }
        return String.valueOf(idSendFlj);
    }

    private boolean isNegative(double d) {
        return Double.compare(d, 0.0) < 0;
    }

    public String buildCdstrOpeField() {
        String accBookName = Optional.ofNullable(this.trade).map(Trade::getBook)
                .map(Book::getAccountingBook).map(AccountingBook::getName).orElse("");
        String porfType=FIFlowStaticData.PortfolioType.OTROS_ACTIVOS.getMappedValue();
        try{
            porfType=FIFlowStaticData.PortfolioType.valueOf(FIFlowStaticData.PortfolioType.formatPortfolioType(accBookName)).getMappedValue();
        }catch(IllegalArgumentException exc){
            Log.error(this,exc.getCause());
        }
        return porfType;
    }

    public String buildCcReferREFInterna() {
        String refInternaCode = "REF_INTERNA";
        return securityWrapper.getSecCodeFromBond(refInternaCode);
    }

    public String buildCdPortfo() {
        return Optional.ofNullable(this.trade).map(Trade::getBook)
                .map(Book::getName).orElse("");
    }

    public String buildCodDivisa() {
        return Optional.ofNullable(this.securityWrapper.getTradeSecurity())
                .map(Bond::getCurrency)
                .map(this::overrideUdiCurrency)
                .orElse("");
    }

    public String buildCodDiviLiq() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
    }

    /**
     * @return Clean price
     */
    public Double buildImprLimp() {
        if(this.trade.getProduct().getSecCode("ISIN").startsWith("MXBIGO")){
            return Double.parseDouble(this.trade.getKeywordValue("MxInitialCleanPrice")) * 10;
        }
        return Optional.of(this.trade)
                .map(Trade::getTradePrice)
                .map(this.securityWrapper::getSecurityPriceDisplayValue)
                .map(this::adjustPriceBase)
                .orElse(0.0D);
    }

    /**
     * @return Dirty price
     */
    public Double buildImprSuci() {
        if(this.trade.getProduct().getSecCode("ISIN").startsWith("MXBIGO")){
            return Double.parseDouble(this.trade.getKeywordValue("MxInitialCleanPrice")) * 10;
        }
        return Optional.ofNullable(this.trade)
                .map(Trade::getNegociatedPrice)
                .map(this.securityWrapper::getSecurityPriceDisplayValue)
                .map(this::adjustPriceBase)
                .orElse(0.0D);
    }

    public Long buildnTituloo() {
        double nTitulosD = Optional.ofNullable(this.trade).map(Trade::getQuantity).orElse(0.0D);
        BigDecimal nTitulooBD = BigDecimal.valueOf(nTitulosD);
        nTitulooBD = nTitulooBD.setScale(0, RoundingMode.HALF_UP);
        return nTitulooBD.longValue();
    }

    public Double buildIPrinOpe() {
        return Optional.ofNullable(this.trade)
                .map(t -> t.computeNominal(t.getSettleDate()))
                .map(nominal -> nominal *
                        this.securityWrapper.getNotionalIndexFactor(this.trade.getSettleDate(),this.pricingEnv))
                .map(this::convertUDItoMXNNominal)
                .orElse(0.0D);
    }

    /**
     *
     * @return Mx initial accrued coupon that already comes indexed
     */
    public Double buildImcpCorr() {
       return Optional.ofNullable(this.trade)
               .map(this::computePreciseTradeAccrual)
               .map(this::overrideUDIAccruedCoupon)
               .orElse(0.0D);
    }

    public Double buildImpEfeOpe() {
        if(this.trade.getProduct().getSecCode("ISIN").startsWith("MXBIGO")){
            Bond bond = (Bond) trade.getProduct();
            double quantity = trade.getQuantity();
            double nominal = quantity * bond.getFaceValue(trade.getSettleDate());
            return Double.parseDouble(this.trade.getKeywordValue("MxInitialCleanPrice")) * 10 * nominal / 100;
        }

        return Optional.ofNullable(this.trade)
                .map(this.securityWrapper::getPrincipal)
                .map(this::overrideUDIPrincipal)
                .orElse(0.0D);
    }

    /**
     * For quote retrieval
     *
     * @return JDate
     */
    private JDate getPreviousBusinessDay() {
        Vector<String> holidays = Util.string2Vector("SYSTEM");
        return this.valDate.addBusinessDays(-1, holidays);
    }

    /**
     * Get RigCode
     *
     * @return
     */
    public String buildCdIroiField() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(roiKwdName)).orElse("");
    }

    /**
     * Get RigCode
     *
     * @return
     */
    public String buildCdRigField() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(rigCodeKwdName)).orElse("");
    }

    /**
     *
     * @return
     */
    public String buildIdStripField() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(mxGIDKwdName)).orElse("");
    }

    /**
     * build is internal 0, external 1
     *
     * @return
     */
    public String buildInOpinex() {
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "0" : "1";
    }

    /**
     * Cancel = 1, <> Cancel = 2
     *
     * @return
     */
    public String buildCestoPbo() {
        return Optional.ofNullable(trade).map(Trade::getStatus)
                .map(Status::getStatus)
                .filter(Status.CANCELED::equals)
                .map(status->String.valueOf(1))
                .orElse(String.valueOf(2));
    }

    private double adjustPriceBase(DisplayValue displayValue) {
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }

    protected double computePreciseTradeAccrual(Trade trade) {
        BigDecimal unitAccrual = BigDecimal.valueOf(trade.getAccrual());
        double bondFaceValue=Optional.ofNullable(this.securityWrapper.getTradeSecurity())
                .map(bond->bond.getFaceValue(trade.getSettleDate()))
                .orElse(0.0D);
        BigDecimal notional = BigDecimal.valueOf(bondFaceValue * Math.abs(trade.getQuantity()));
        BigDecimal totalAccrual = unitAccrual.multiply(notional);
        return totalAccrual.doubleValue();
    }

    /**
     * UDI hotshit fixes, removed when UDI bond treatment gets fixed in Calypso
     * @param trade
     * @return
     */
    private double convertUDItoMXNNominal(double udiNominal){
        double finalNominal=udiNominal;
        if(this.securityWrapper.isUDIBond()){
            finalNominal=Optional.ofNullable(this.trade)
                    .map(t -> t.getKeywordValue("MxCapitalFactor"))
                    .map(this::parseDoubleFactor)
                    .map(mxnFactor-> mxnFactor*udiNominal)
                    .orElse(udiNominal);
        }
        return finalNominal;
    }

    /**
     * UDI hotshit fixes, removed when UDI bond treatment gets fixed in Calypso
     * @param trade
     * @return
     */
    private String overrideUdiCurrency(String currentCurrency){
        String overridenCurrency=currentCurrency;
        if(this.securityWrapper.isUDIBond()){
            overridenCurrency="MXN";
        }
        return overridenCurrency;
    }

    /**
     * UDI hotshit fixes, removed when UDI bond treatment gets fixed in Calypso
     * @param trade
     * @return
     */
    private double overrideUDIAccruedCoupon(double accruedCoupon){
        double overridenAccruedCoupon=accruedCoupon;
        if(this.securityWrapper.isUDIBond()){
            overridenAccruedCoupon=getInitialAccruedCouponKwd();
        }
        return overridenAccruedCoupon;
    }

    private double getInitialAccruedCouponKwd(){
        return Optional.ofNullable(this.trade)
                .map(t -> t.getKeywordValue("MxInitialAccruedCoupon"))
                .map(this::parseDoubleFactor)
                .orElse(0.0d);
    }
    /**
     * UDI hotshit fixes, removed when UDI bond treatment gets fixed in Calypso
     * @param trade
     * @return
     */
    private double overrideUDIPrincipal(double accruedCoupon){
        double overridenAccruedCoupon=accruedCoupon;
        if(this.securityWrapper.isUDIBond()){
            overridenAccruedCoupon=Optional.ofNullable(this.securityWrapper.getTradeSecurity())
                    .map(bond -> bond.calcSettlementAmount(this.trade))
                    .map(Math::abs)
                    .map(settleAmt -> settleAmt - getInitialAccruedCouponKwd())
                    .orElse(0.0d);
        }
        return overridenAccruedCoupon;
    }

    private double parseDoubleFactor(String numberToParse){
        double doubleValue;
        try{
            doubleValue=Double.parseDouble(numberToParse);
        }catch(NumberFormatException exc){
            doubleValue=1.0d;
        }
        return doubleValue;
    }

}

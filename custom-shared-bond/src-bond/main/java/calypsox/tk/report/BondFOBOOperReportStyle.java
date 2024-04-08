package calypsox.tk.report;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTradeSecurityHandler;
import calypsox.tk.report.util.UtilReport;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.bo.TradeRoleFinder;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.FdnUtilProvider;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author dmenendd
 */
public class BondFOBOOperReportStyle extends TradeReportStyle {
    public static final String FHCONCILIA = "FHCONCILIA";
    public static final String INSTRUMENTO = "INSTRUMENTO";
    public static final String ROOTCONTRACT = "ROOTCONTRACT";
    public static final String INTERNA = "INTERNA";
    public static final String DIRECTION = "DIRECTION";
    public static final String FHSTL = "FHSTL";
    public static final String SETTAMOUNT = "SETTAMOUNT";
    public static final String STLCURR = "STLCURR";
    public static final String STOCKEX = "STOCKEX";
    public static final String QUANTITY = "QUANTITY";
    public static final String CLEANPRICE = "CLEANPRICE";
    public static final String DIRTYPRICE = "DIRTYPRICE";
    public static final String NOMINAL = "NOMINAL";
    public static final String ACCRCOUPON = "ACCRCOUPON";
    public static final String GROSSAMT = "GROSSAMT";
    public static final String BROKER = "BROKER";
    public static final String NOM_NO_INDX = "NOM_NO_INDX";
    public static final String MARCA_INDX = "MARCA_INDX";
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String YIELD = "Yield";
    final String dualCCYKwdName = "Dual_CCY";
    private final FIFlowTradeSecurityHandler securityWrapper = new FIFlowTradeSecurityHandler();

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        final Trade trade = row.getProperty(ReportRow.TRADE);
        if (!(trade.getProduct() instanceof Bond)) {
            return null;
        }

        final Bond bond = (Bond) trade.getProduct();
        final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

        if (trade != null) {
            this.securityWrapper.initRelatedSecutityData(trade.getProduct());
            this.securityWrapper.initRelatedSecPricesData(getPreviousBusinessDay(valDate));
        }

        //Capital Factor values
        double calypsoPoolFactor = calculatePoolFactor(bond, trade);
        double calypsoInflationFactor = getInflationFactor(bond, trade, pricingEnv);
        double calypsoCapitalFactor = calypsoPoolFactor*calypsoInflationFactor;

        if (columnName.equals(FHCONCILIA)) {
            return formatDate(valDate);
        }
        if (columnName.equals(INSTRUMENTO)) {
            return Optional.ofNullable(trade).map(t -> loadInstrument(trade)).orElse("");
        }
        if (columnName.equals(ROOTCONTRACT)) {
            String allocatedFrom = Optional.ofNullable(trade).map(t -> t.getKeywordValue("AllocatedFrom")).orElse("");
            if (allocatedFrom != null && !allocatedFrom.isEmpty()) {
                return getAllocatedValue(trade, allocatedFrom);
            }
            String rootContract = Optional.ofNullable(trade).map(t -> t.getKeywordValue("MurexRootContract")).orElse("");
            return validateIsMirror(trade, rootContract);
        }
        if (columnName.equals(INTERNA)) {
            return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "Y" : "N";
        }
        if (columnName.equals(DIRECTION)) {
            return bond != null ? bond.getBuySell(trade) == 1 ? "B" : "S" : "";
        }
        if (columnName.equals(FHSTL)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getSettleDate())).orElse("");
        }
        if (columnName.equals(SETTAMOUNT)) {
            return "true".equalsIgnoreCase(trade.getKeywordValue(dualCCYKwdName)) ? formatResult(buildTotalAmount(trade))
                    : formatResult(buildSettlementAmt(trade, row));
        }
        if (columnName.equals(STLCURR)) {
            return "true".equalsIgnoreCase(trade.getKeywordValue(dualCCYKwdName)) ? Optional.ofNullable(trade).map(Trade::getTradeCurrency).orElse("")
                    : Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
        }
        if (columnName.equals(STOCKEX)) {
            //Empty
            return "";
        }
        if (columnName.equals(QUANTITY)) {
            double quantity = Optional.ofNullable(trade).map(Trade::getQuantity).orElse(0.0D);
            return formatResult(quantity / calculatePoolFactor(bond, trade));
        }
        if (columnName.equals(CLEANPRICE)) {
            double cleanPrice = Optional.of(trade)
                    .map(Trade::getTradePrice)
                    .map(this.securityWrapper::getSecurityPriceDisplayValue)
                    .map(this::adjustPriceBase)
                    .orElse(0.0D);
            return formatResult(cleanPrice/calypsoInflationFactor, '.', 4);
        }
        if (columnName.equals(DIRTYPRICE)) {
            double dirty = Optional.ofNullable(trade)
                    .map(Trade::getNegociatedPrice)
                    .map(this.securityWrapper::getSecurityPriceDisplayValue)
                    .map(this::adjustPriceBase)
                    .orElse(0.0D);
            return formatResult(dirty/calypsoInflationFactor, '.', 4);
        }
        if (columnName.equals(NOMINAL)) {
            double nominal = Optional.ofNullable(bond).map(b -> b.computeNominal(trade))
                    .orElse(0.0D);
            return formatResult(nominal / calculatePoolFactor(bond, trade));
        }
        if (columnName.equals(ACCRCOUPON)) {
            Double dirtyPrice = Optional.ofNullable(trade).map(t -> t.getNegociatedPrice() * 100).orElse(0.0D);
            Double cleanPrice = Optional.ofNullable(trade).map(t -> t.getTradePrice() * 100).orElse(0.0D);
            return formatResult((dirtyPrice - cleanPrice) / calypsoInflationFactor, '.', 6);
        }
        if (columnName.equals(GROSSAMT)) {
            return formatResult(getSetllementAmount(trade, bond));
        }
        if (columnName.equals(BROKER)) {
            Vector<LegalEntity> brokers = TradeRoleFinder.getTradeRoles("Broker", trade, new Vector(), DSConnection.getDefault());
            return (brokers != null && brokers.size() > 0) ? brokers.get(0).getCode() : "";
        }
        if (columnName.equals(NOM_NO_INDX)) {
            return Optional.ofNullable(trade).map(t -> formatResult(t.computeNominal())).orElse(0.0D);
        }
        if (columnName.equals(MARCA_INDX)) {
            return Optional.ofNullable(bond.getNotionalIndex()).isPresent() ? "S" : "N";
        }
        if (columnName.equals(PRINCIPAL)) {
            return formatResult(bond.computeNominal(trade)*calculatePoolFactor(bond,trade));
        }
        if (columnName.equals(YIELD)){
            return formatResult(((DisplayValue) super.getColumnValue(row, columnName, errors)).get() * 100);
        }
        return formatResult(super.getColumnValue(row, columnName, errors));
    }

    /**
     *
     * @param jDate
     * @return
     */
    private String formatDate(JDate jDate){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }

    /**
     *
     * @param o
     * @return
     */
    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }

    /**
     *
     * @param trade
     * @return
     */
    private String loadInstrument(Trade trade){
        return "true".equalsIgnoreCase(trade.getKeywordValue("BondForward")) ?
                "Bond Forward" : "Bond Spot";
    }

    /**
     *
     * @param trade
     * @return
     */
    private double calculatePoolFactor(Bond bond, Trade trade){
        return bond.getCurrentFactor(JDate.valueOf(trade.getSettleDate()));
    }

    private double getInflationFactor (Bond bond, Trade trade, PricingEnv pricingEnv) {
        try {
            return bond.getNotionalIndexFactor(JDate.valueOf(trade.getSettleDate()), pricingEnv.getQuoteSet());
        } catch (FlowGenerationException e) {
            Log.error(this,e.getCause());;
            return 1;
        }
    }
    /**
     *
     * @param strAmortizationType
     * @return
     */
    public boolean isAmortizationSinking(String strAmortizationType) {
        return (strAmortizationType ==null || "Bullet".equals(strAmortizationType)) ? false : true;
    }

    /**
     *
     * @param trade
     * @param bond
     * @return
     */
    public Double getSetllementAmount(Trade trade, Bond bond) {
        Double dirtyPrice = Optional.ofNullable(trade)
                .map(Trade::getNegociatedPrice)
                .map(this.securityWrapper::getSecurityPriceDisplayValue)
                .map(this::adjustPriceBase)
                .orElse(0.0D);
        Double titule = Optional.ofNullable(trade).map(Trade::getQuantity).orElse(0.0D);
        Double faceValue = Optional.ofNullable(bond).map(Bond::getFaceValue).orElse(0.0D);
        return - titule * faceValue * dirtyPrice;
    }

    /**
     *
     * @param trade
     * @param valueInput
     * @return
     */
    public String validateIsMirror(Trade trade, String valueInput){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() &&
                trade.getMirrorTradeLongId()!=0 &&
                trade.getLongId()>trade.getMirrorTradeLongId() ? setIfMirror(valueInput) : valueInput;

    }

    /**
     *
     * @param valueToChange
     * @return
     */
    public String setIfMirror(String valueToChange) {
        return !Util.isEmpty(valueToChange) ? "-" + valueToChange : "";
    }

    /**
     *
     * @param d
     * @param decimalSeparator
     * @return
     */
    public static Object formatResult(double d, char decimalSeparator, int numberDecimals) {
        switch(numberDecimals) {
            case 4:
                return formatNumberDecimals(new Double(d), decimalSeparator, "0.0000");
            case 6:
                return formatNumberDecimals(new Double(d), decimalSeparator, "0.000000");
        }
        return null;
    }

    /**
     *
     * @param number
     * @param decimalSeparator
     * @return
     */
    public static Object formatNumberDecimals(Number number, char decimalSeparator, String format) {
        if(number instanceof Double) {
            DecimalFormat df = new DecimalFormat(format);
            df.setGroupingUsed(false);
            DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
            newSymbols.setDecimalSeparator(decimalSeparator);
            df.setDecimalFormatSymbols(newSymbols);
            if (((Double) number).isNaN()){
                number = 0.0D;
            }
            return df.format(number);
        }
        return number;
    }

    private String getAllocatedValue(Trade trade, String allocatedFrom) {
        Trade tradeParent = null;
        try {
            tradeParent = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(allocatedFrom));
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        if (tradeParent == null) {
            return "";
        }
        Vector<TradeRoleAllocation> allocations = tradeParent.getRoleAllocations();
        if (allocations == null || allocations.isEmpty()) {
            return "";
        }
        Collections.sort(allocations, new MiComparator());
        String result = "";
        for (int i = 0; i < allocations.size(); i++) {
            if (allocations.get(i).getRelatedTradeId() == trade.getLongId()) {
                String rootContract = Optional.ofNullable(tradeParent).map(t -> t.getKeywordValue("MurexRootContract")).orElse("");
                result = rootContract + "." + (i + 1);
            }
        }
        return result;
    }

    /**
     *
     * @param trade
     * @return
     */
    public Double buildTotalAmount(Trade trade) {
        final Bond product = (Bond) trade.getProduct();
        Double cleanPrice = Optional.ofNullable(trade).map(Trade::getTradePrice).orElse(0.0D);
        Double titule = Optional.ofNullable(trade).map(Trade::getQuantity).orElse(0.0D);
        Double faceValue = Optional.ofNullable(product).map(Bond::getFaceValue).orElse(0.0D);
        Double accrual = Optional.of(trade).map(Trade::getAccrual).orElse(0.0D);

        return - titule * faceValue * (cleanPrice + accrual);
    }

    /**
     *
     * @param trade
     * @param row
     * @return
     */
    public Double buildSettlementAmt(Trade trade, ReportRow row) {
        TradeReportStyle rt = new TradeReportStyle();
        return Optional.ofNullable(trade).map(t -> rt.getColumnValue(row, TradeReportStyle.SETTLEMENT_AMOUNT, new Vector<String>()))
                .filter(amt -> amt instanceof Amount)
                .map(amt -> Double.valueOf(((Amount) amt).get()))
                .orElse(0.0);
    }

    /**
     * For quote retrieval
     *
     * @return JDate
     */
    private JDate getPreviousBusinessDay(JDate valDate) {
        Vector<String> holidays = Util.string2Vector("SYSTEM");
        return valDate.addBusinessDays(-1, holidays);
    }

    /**
     *
     * @param displayValue
     * @return
     */
    private double adjustPriceBase(DisplayValue displayValue) {
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }

    class MiComparator implements Comparator<TradeRoleAllocation> {
        @Override
        public int compare(TradeRoleAllocation t1, TradeRoleAllocation t2) {
            // Comparar en orden descendente

            return Long.compare(t1.getRelatedTradeId(), t2.getRelatedTradeId());
        }
    }

}

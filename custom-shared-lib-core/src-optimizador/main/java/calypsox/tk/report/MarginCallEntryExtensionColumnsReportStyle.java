package calypsox.tk.report;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallEntryBaseReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

public class MarginCallEntryExtensionColumnsReportStyle extends MarginCallEntryReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    // COLUMNS
    public static final String IM_THRESHOLD = "IM THRESHOLD SIGNED (Santander)";
    public static final String IM_CCY = "IM  CCY";
    public static final String IM_THRESHOLD_USD = "IM THRESHOLD IN USD (Santander)";
    public static final String IM_CALCULATED_USD = "IM CALCULATED IN USD (Santander)";
    public static final String IM_EXCHANGED_USD = "IM EXCHANGED IN USD (Santander)";
    public static final String FX_RATE = "FX RATE";
    public static final String VAL_DATE_COL = "Value date of the collateral";
    //New Columns
    public static final String IM_AMOUNT = "Initial_margin_amount";
    public static final String IM_CCY_NEW = "Initial_margin_currency";
    public static final String VM_AMOUNT = "variation_margin_amount";
    public static final String VM_CCY = "variation_margin_currency";
    public static final String EXCESS_AMOUNT = "excess_collateral_amount";

    // CONSTANTS
    private static final String EUR = "EUR";
    private static final String USD = "USD";
    private static final String PE_DEFAULT = "DirtyPrice";
    private static final String THRESHOLD_AMOUNT = "Threshold Amount";
    private static final String NET_BALANCE = "Net Balance";
    private static final String TRIP_AGG_AMT = "Attributes.Triparty Agreed Amount";
    private static final String PRINCING_ENV = "PrincingEnv";
    private static final String PRINCING_ENV_DV = "ThresoldReportPrincingEnv";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        Object rst = null;

        try {
            // Get the ValDate
            JDatetime valDatetime = row.getProperty("ValuationDatetime");
            JDate valDate = JDate.getNow();
            if(valDatetime!=null){
                valDate = valDatetime.getJDate(TimeZone.getDefault());
            }

            // Get the PrincingEnv
            String pricingEnv = null;
            // First, try to get the PE configured in the report
            pricingEnv = (String)row.getProperty("PricingEnv");
            // Second, try to get the PE from the DomianValue ThresoldReportPrincingEnv
            if(StringUtils.isBlank(pricingEnv)){
                final Vector<String> pe_dv = LocalCache
                        .getDomainValues(DSConnection.getDefault(), PRINCING_ENV_DV);
                if(pe_dv!=null && pe_dv.size()>0){
                    pricingEnv = pe_dv.get(0);
                }
            }
            // Third, use the default PE, DirtyPrice
            if(StringUtils.isBlank(pricingEnv)){
                pricingEnv = PE_DEFAULT;
            }

            if (IM_THRESHOLD.equals(columnName)) {

                boolean isPO = getIsPOIM(row);

                MarginCallEntryReportStyle style = new MarginCallEntryReportStyle();
                Object value = null;
                if(isPO){
                    value = style.getColumnValue(row, "MarginCallConfig.PO Threshold Amount", errors);
                }else{
                    value = style.getColumnValue(row, "MarginCallConfig.LE Threshold Amount", errors);
                }

                rst = value;
            }

            if(IM_CCY.equals(columnName)){
                boolean isPO = getIsPOIM(row);

                MarginCallEntryReportStyle style = new MarginCallEntryReportStyle();
                Object value = null;
                if(isPO){
                    value = style.getColumnValue(row, "MarginCallConfig.PO Threshold Currency", errors);
                }else{
                    value = style.getColumnValue(row, "MarginCallConfig.LE Threshold Currency", errors);
                }

                rst = value;
            }

            if (IM_THRESHOLD_USD.equals(columnName)) {
                Object valueAmount = this.getColumnValue(row, IM_THRESHOLD, errors);
                Object valueCcy = this.getColumnValue(row, IM_CCY, errors);
                String valueCcySt = "";
                if(valueCcy instanceof  String){
                    valueCcySt = (String)valueCcy;
                }

                if("USD".equals(valueCcySt)){
                    rst = valueAmount;
                }else{
                    if(valueAmount instanceof Amount){
                        Amount amt = (Amount)valueAmount;
                        final double valorMoneda = getValorMoneda(row, valueCcySt, pricingEnv);
                        Amount rstAmt = new Amount(amt.get()*valorMoneda);
                        rst = rstAmt;

                    }
                }


            }

            if (IM_CALCULATED_USD.equals(columnName)) {
                MarginCallEntryReportStyle style = new MarginCallEntryReportStyle();
                Object value = style.getColumnValue(row, NET_BALANCE, errors);

                // It check if it is neccesary converted the value
                Object valueCcy = style.getColumnValue(row, "Contract Currency", errors);
                String valueCcySt = "";
                if(valueCcy instanceof  String){
                    valueCcySt = (String)valueCcy;
                }

                if("USD".equals(valueCcySt)){
                    rst = value;
                }else {
                    if (value instanceof Amount) {
                        Amount amt = (Amount) value;
                        final double valorMoneda = getValorMoneda(row, valueCcySt, pricingEnv);
                        rst = new Amount(amt.get() * valorMoneda);

                    }
                }
            }

            if (IM_EXCHANGED_USD.equals(columnName)) {
                MarginCallEntryReportStyle style = new MarginCallEntryReportStyle();
                Object value = style.getColumnValue(row, TRIP_AGG_AMT, errors);

                // It check if it is neccesary converted the value
                Object valueCcy = style.getColumnValue(row, "Contract Currency", errors);
                String valueCcySt = "";
                if(valueCcy instanceof  String) {
                    valueCcySt = (String) valueCcy;
                }


                if("USD".equals(valueCcySt)){
                    rst = value;
                }else {
                    if(value instanceof Amount){
                            Amount amt = (Amount)value;
                            final double valorMoneda = getValorMoneda(row, valueCcySt, pricingEnv);
                            rst = new Amount(amt.get() * valorMoneda);

                        }
                }
            }

            if (FX_RATE.equals(columnName)) {

                MarginCallEntryReportStyle style = new MarginCallEntryReportStyle();
                Amount value = new Amount(1);

                // It check if it is neccesary converted the value
                Object valueCcy = style.getColumnValue(row, "Contract Currency", errors);
                String valueCcySt = "";
                if(valueCcy instanceof  String) {
                    valueCcySt = (String) valueCcy;
                }


                if("USD".equals(valueCcySt)){
                    rst = value;
                }else {
                    final double valorMoneda = getValorMoneda(row, valueCcySt, pricingEnv);
                        rst = new Amount(valorMoneda);
                }
            }
            if (VAL_DATE_COL.equals(columnName)) {
                CollateralConfig collateralConfig = getCollateralConfig(row);
                int idCOntract = collateralConfig.getId();

                String from, where, order;

                from = "trade, (select a.trade_id tradeId from trade a \n" + 
                		"inner join margin_call_allocation b on a.trade_id=b.trade_id\n" + 
                		"inner join margin_call_entries c on b.mc_entry_id=c.id\n" + 
                		"where c.mcc_id="+idCOntract+" order by trade_date_time desc) tdTrade";
                where = "tdTrade.tradeId = trade.trade_id and rownum<=1";                
                List whereList = new ArrayList();
                whereList.add(where);               
                TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(from, where,null,null);

                if(trades!=null && !trades.isEmpty()){
                    rst = trades.get(0).getTradeDate().getJDate(TimeZone.getDefault());
                }
            }

            if(IM_AMOUNT.equals(columnName)){
                CollateralConfig collateralConfig = getCollateralConfig(row);
                String type = null==collateralConfig ? null : collateralConfig.getAdditionalField("GUARANTEE_TYPE");
                if(StringUtils.isNotBlank(type) && type.equalsIgnoreCase("Initial Margin")){
                    Object result = super.getColumnValue(row, MarginCallEntryBaseReportStyle.PREV_TOTAL_MRG, errors);
                    if(result instanceof Amount){
                        rst = getFormattedAmount((Amount)result);
                    }
                } else{
                    rst = "";
                }
            }

            if(IM_CCY_NEW.equals(columnName)){
                CollateralConfig collateralConfig = getCollateralConfig(row);
                String type = null==collateralConfig ? null : collateralConfig.getAdditionalField("GUARANTEE_TYPE");
                if(StringUtils.isNotBlank(type) && type.equalsIgnoreCase("Initial Margin")){
                    rst = super.getColumnValue(row, MarginCallEntryBaseReportStyle.CONTRACT_CURRENCY, errors);
                }else{
                    rst = "";
                }
            }

            if(VM_AMOUNT.equals(columnName)){
                CollateralConfig collateralConfig = getCollateralConfig(row);
                if(null!=collateralConfig){
                    String type = collateralConfig.getAdditionalField("GUARANTEE_TYPE");
                    if(StringUtils.isBlank(type) || type.equalsIgnoreCase("Variation Margin")){
                        Object result = super.getColumnValue(row, MarginCallEntryBaseReportStyle.PREV_TOTAL_MRG, errors);
                        if(result instanceof Amount){
                            rst = getFormattedAmount((Amount)result);
                        }
                    } else{
                        rst = "";
                    }
                } else{
                    rst = "";
                }
            }

            if(VM_CCY.equals(columnName)){
                CollateralConfig collateralConfig = getCollateralConfig(row);
                if(null!=collateralConfig){
                    String type = collateralConfig.getAdditionalField("GUARANTEE_TYPE");
                    if(StringUtils.isBlank(type) || type.equalsIgnoreCase("Variation Margin")){
                        rst = super.getColumnValue(row, MarginCallEntryBaseReportStyle.CONTRACT_CURRENCY, errors);
                    } else{
                        rst = "";
                    }
                } else{
                    rst = "";
                }
            }

            if(EXCESS_AMOUNT.equals(columnName)){
                Object result = super.getColumnValue(row, MarginCallEntryBaseReportStyle.CONSTITUTED_MRG, errors);
                if(result instanceof Amount){
                    rst = getFormattedAmount((Amount)result);
                }
            }
            
        } catch (Exception e) {
            Log.error(this, e);
        }

        return rst;
    }

    private boolean getIsPOIM(ReportRow row) {

        boolean isPO = false;

        MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty("Default");
        CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),entry.getCollateralConfigId());
        String csdType = collateralConfig.getAdditionalField("IM_CSD_TYPE");

        if("PO".equals(csdType)){
            isPO = true;
        }

        return isPO;

    }

    /**
     * Get the FX quote for the parameter fromCcy to USD
     *
     * @param row
     * @param fromCcy
     * @param pricingEnv
     * @return
     */
    private double getValorMoneda(ReportRow row, String fromCcy, String pricingEnv){
        double ret = 0.0;
        try {
            MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty("Default");
            JDate processDate = entry.getProcessDatetime().getJDate(TimeZone.getDefault());
            ret = SantReportingUtil.getFXRate(fromCcy, USD, processDate, pricingEnv);
        } catch (MarketDataException e) {
            ret = 0.0;
            Log.error(this, e);
        }
        return ret;
    }
    
    /**
     * @param row
     * @return CollateralConfig
     */
    private CollateralConfig getCollateralConfig(ReportRow row) {

        if (row.getProperty("MarginCallConfig") != null)
            return (CollateralConfig) row.getProperty("MarginCallConfig");

        final MarginCallEntryDTO entry = getEntryDTO(row);
        if (entry == null) {
            return null;
        }
        CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                entry.getCollateralConfigId());
        return collateralConfig;
    }

    /**
     * @param row
     * @return MCEntryDTO
     */
    private MarginCallEntryDTO getEntryDTO(ReportRow row) {
        MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty("Default");
        return entry;
    }

    /**
     * @param amount
     * @return
     */
    private static String getFormattedAmount(final Amount amount){
        String result = "";
        final DecimalFormat myFormatter = new DecimalFormat("###0.#####");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if(null!=amount){
            result = myFormatter.format(amount.get());
            if(result.startsWith("-")){
                if(result.contains(".")){
                    if(result.length()>20){
                        result = result.substring(0, 20);
                    }
                } else{
                    if(result.length()>19){
                        result = result.substring(0, 19);
                    }
                }
            } else{
                if(result.contains(".")){
                    if(result.length()>19){
                        result = result.substring(0, 19);
                    }
                } else{
                    if(result.length()>18){
                        result = result.substring(0, 18);
                    }
                }
            }

        }
        return result;
    }

}

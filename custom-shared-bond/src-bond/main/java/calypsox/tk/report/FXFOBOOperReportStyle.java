package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import com.calypso.tk.bo.TradeRoleFinder;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FX;
import com.calypso.tk.product.FXBased;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;

/**
 * @author dmenendd
 */
public class FXFOBOOperReportStyle extends TradeReportStyle {

    public static final String GLOBAL_ID = "GLOBAL ID";
    public static final String EXTERNAL_ID = "EXTERNAL ID";
    public static final String CODIGO_OPERACION_FX = "CODIGO OPERACION FX";
    public static final String CODIGO_ORIGINAL_OPERACION_FX = "CODIGO ORIGINAL OPERACION FX";
    public static final String ID_MUREX = "ID MUREX";
    public static final String TIMESTAMP_CONTRATACION = "TIMESTAMP CONTRATACION";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String GCLS_CONTRAPARTIDA = "GCLS CONTRAPARTIDA";
    public static final String TIPO_PERSONA = "TIPO PERSONA";
    public static final String CONTRAPARTIDA = "CONTRAPARTIDA";
    public static final String IMPORTE_EMISION = "IMPORTE EMISION";
    public static final String DIVISA_EMISION = "DIVISA EMISION";
    public static final String IMPORTE_LIQUIDACION = "IMPORTE LIQUIDACION";
    public static final String DIVISA_LIQUIDACION = "DIVISA LIQUIDACION";
    public static final String SENTIDO = "SENTIDO";
    public static final String FECHA_VALOR = "FECHA VALOR";
    public static final String CAMBIO_APLICADO = "CAMBIO APLICADO";
    public static final String VIGENCIA_FX = "VIGENCIA FX";


    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        final Trade trade = row.getProperty(ReportRow.TRADE);
        final FX fx = (FX) trade.getProduct();

        if (columnName.equals(GLOBAL_ID)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue("Contract ID")).orElse("");
        }
        if (columnName.equals(EXTERNAL_ID)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue("Mx External ID")).orElse("");
        }
        if (columnName.equals(CODIGO_OPERACION_FX)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue("Mx External ID")).orElse("");
        }
        if (columnName.equals(CODIGO_ORIGINAL_OPERACION_FX)) {
            return Optional.ofNullable(trade).map(t -> t.getExternalReference()).orElse("");
        }
        if (columnName.equals(ID_MUREX)) {
            return Optional.ofNullable(trade).map(t -> t.getExternalReference()).orElse("");
        }
        if (columnName.equals(TIMESTAMP_CONTRATACION)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getTradeDate().getJDate())).orElse("");
        }
        if (columnName.equals(PORTFOLIO)) {
            return Optional.ofNullable(trade).map(t -> t.getBook().getName()).orElse("");
        }
        if (columnName.equals(GCLS_CONTRAPARTIDA)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty().getCode()).orElse("");
        }
        if (columnName.equals(TIPO_PERSONA)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty().getExternalRef().substring(0,1)).orElse("");
        }
        if (columnName.equals(CONTRAPARTIDA)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty().getExternalRef().substring(1)).orElse("");
        }
        if (columnName.equals(IMPORTE_EMISION)) {
            double importeEmision = Optional.ofNullable(trade).map(t -> (Math.abs(t.getQuantity()))).orElse((0.0D));
            return formatResult(importeEmision,',',4);
        }
        if (columnName.equals(DIVISA_EMISION)) {
            return Optional.ofNullable(trade).map(t -> t.getNegotiatedCurrency()).orElse("");
        }
        if (columnName.equals(IMPORTE_LIQUIDACION)) {
            double importeLiquidacion = Optional.ofNullable(trade).map(t -> (Math.abs(t.getAccrual()))).orElse((0.0D));
            return formatResult(importeLiquidacion,',',4);
        }
        if (columnName.equals(DIVISA_LIQUIDACION)) {
            return Optional.ofNullable(trade).map(t -> t.getSettleCurrency()).orElse("");
        }
        if (columnName.equals(SENTIDO)) {
            int buySell = fx.getBuySell(trade);
            return buySell==1 ? "COMPRA" : "VENTA";
        }
        if (columnName.equals(FECHA_VALOR)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getSettleDate())).orElse("");
        }
        if (columnName.equals(CAMBIO_APLICADO)) {
            double fxRate = Optional.ofNullable(trade).map(t -> (t.getNegociatedPrice())).orElse((0.0D));
            return formatResult(fxRate,',',6);
        }
        if (columnName.equals(VIGENCIA_FX)) {
            return Optional.ofNullable(trade).map(t -> t.getStatus()).orElse(Status.valueOf(""));
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
        return UtilReport.formatResult(o, ',');
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

    private String getBondCurrency(Trade trade){
        Trade bondTrade = getOtherMultiCcyTrade(trade);
        if(bondTrade != null) {
            Bond bond = (Bond) bondTrade.getProduct();
            return bond.getSecurity().getCurrency();
        }
        FXBased fx  = (FXBased) trade.getProduct();
        return fx.getCurrencyPair().getPrimaryCode();
    }

}

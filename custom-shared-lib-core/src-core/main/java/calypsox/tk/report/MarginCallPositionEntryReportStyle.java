package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.DisplayRate;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

public class MarginCallPositionEntryReportStyle extends com.calypso.tk.report.MarginCallPositionEntryReportStyle {

    /**
     * Default serial version
     */
    private static final long serialVersionUID = 1L;

    public static final String MEX_QUANTITY = "Mex Quantity";
    public static final String MEX_DIRTY_PRICE = "Mex Dirty Price";
    public static final String MEX_HAIRCUT = "Mex Haircut";
    public static final String MEX_ISIN_NOMINAL = "Mex ISIN Nominal";
    public static final String MEX_CCY_NOMINAL = "Mex Ccy Nominal";
    public static final String MEX_FX_RATE = "Mex FX Rate";
    public static final String MEX_BASE_CCY_BALANCE = "Mex Base Ccy Balance";

    private static final String QUANTITY = "Quantity";
    private static final String CLEAN_PRICE = "Clean Price";
    private static final String HAIRCUT = "Haircut";
    private static final String NOMINAL = "Nominal";
    private static final String ALL_IN_VALUE = "All-In Value";
    private static final String FX_RATE = "FX Rate";
    private static final String CONTRACT_VALUE = "Contract Value";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if (MEX_QUANTITY.equals(columnName)) {
            Object quantity = super.getColumnValue(row, QUANTITY, errors);
            return applyCustomFormat(quantity);
        } else if (MEX_DIRTY_PRICE.equals(columnName)) {
            Object dirtyPrice = super.getColumnValue(row, CLEAN_PRICE, errors);
            return applyCustomFormat(dirtyPrice, "#0.0###############");                // all of the decimals (mantissa is limited to 16 digits in 'double')
        } else if (MEX_HAIRCUT.equals(columnName)) {
            Object haircut = super.getColumnValue(row, HAIRCUT, errors);
            return applyCustomFormat(haircut);
        } else if (MEX_ISIN_NOMINAL.equals(columnName)) {
            Object isinNominal = super.getColumnValue(row, NOMINAL, errors);
            return applyCustomFormat(isinNominal);
        } else if (MEX_CCY_NOMINAL.equals(columnName)) {
            Object ccyNominal = super.getColumnValue(row, ALL_IN_VALUE, errors);
            return applyCustomFormat(ccyNominal);
        } else if (MEX_FX_RATE.equals(columnName)) {
            Object fxRate = super.getColumnValue(row, FX_RATE, errors);
            return applyCustomFormat(fxRate, "#0.0###############");                    // all of the decimals (mantissa is limited to 16 digits in 'double')
        } else if (MEX_BASE_CCY_BALANCE.equals(columnName)) {
            Object baseCcyBalance = super.getColumnValue(row, CONTRACT_VALUE, errors);
            return applyCustomFormat(baseCcyBalance);
        } else if (CLEAN_PRICE.equals(columnName)) {
            return getCleanPriceFormat(getDoubleValue(super.getColumnValue(row, CLEAN_PRICE, errors)));
        }

        return super.getColumnValue(row, columnName, errors);

    }

    private String getCleanPriceFormat(Double cleanPrice){
        NumberFormat formatter = NumberFormat.getInstance(Locale.ITALIAN);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        return formatter.format(cleanPrice);
    }
    private Object getCleanPrice(ReportRow row, Vector errors) {
        return CollateralUtilities.formatNumber(getDoubleValue(super.getColumnValue(row, CLEAN_PRICE, errors)), "#0.00", Locale.FRENCH);
    }

    private String applyCustomFormat(Object value) {
        if (value != null) {
            double doubleValue = getDoubleValue(value);
            return CollateralUtilities.formatNumber(doubleValue, "#0.00", Locale.US);
        } else {
            return "";
        }

    }

    private String applyCustomFormat(Object value, final String pattern) {
        if (value != null) {
            double doubleValue = getDoubleValue(value);
            final NumberFormat numberFormatter = new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US));
            return numberFormatter.format(doubleValue);
        } else {
            return "";
        }
    }

    private double getDoubleValue(Object value) {
        double newValue = 0.0D;
        if (value instanceof DisplayRate) {
            DisplayRate rate = (DisplayRate) value;
            newValue = rate.get();
        } else if (value instanceof DisplayValue) {
            DisplayValue dv = (DisplayValue) value;
            newValue = dv.get();
        }
        return newValue;
    }

}

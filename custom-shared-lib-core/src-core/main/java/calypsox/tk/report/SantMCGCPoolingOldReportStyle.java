package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

/**
 * 
 * @author Juan Angel Torija
 * 
 */
@SuppressWarnings("rawtypes")
public class SantMCGCPoolingOldReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String CESTA = "CESTA";
	public static final String ISIN = "ISIN";
	public static final String DIVISA = "DIVISA";
	public static final String DIRECTION = "DIRECTION";
	public static final String COLLATERAL_VALUE_CESTA = "COLLATERAL_VALUE_CESTA";
	public static final String NOMINAL_ISIN = "NOMINAL_ISIN";
	public static final String COLLATERAL_VALUE_ISIN = "COLLATERAL_VALUE_ISIN";
	public static final String PORCENTAJE_ISIN = "PORCENTAJE_ISIN";
	public static final String NOMINAL_CESTA = "NOMINAL_CESTA";

	private static final String EMPTY = "";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		SantMCGCPoolingOldItem santMCGCPoolingOldItem = (SantMCGCPoolingOldItem) row.getProperty(SantMCGCPoolingOldReport.TYPE);

		if (CESTA.equals(columnName)) {
			return santMCGCPoolingOldItem.getIsinCesta();
		} else if (ISIN.equals(columnName)) {
			return santMCGCPoolingOldItem.getIsinAlloc();
		} else if (DIVISA.equals(columnName)) {
			return santMCGCPoolingOldItem.getCurrency();
		} else if (DIRECTION.equals(columnName)) {
			return santMCGCPoolingOldItem.getDirection();
		} else if (COLLATERAL_VALUE_CESTA.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingOldItem.getCollateralCestaAlloc());
		} else if (NOMINAL_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingOldItem.getNominal());
		} else if (COLLATERAL_VALUE_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingOldItem.getCollateralValue());
		} else if (NOMINAL_CESTA.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingOldItem.getNominalCesta());
		} else if (PORCENTAJE_ISIN.equals(columnName)) {
			return roundDecimalAmount(
					(santMCGCPoolingOldItem.getCollateralValue() / santMCGCPoolingOldItem.getCollateralCestaAlloc()) * 100,
					12);
		}
		return "";
	}

	/**
	 * Round decimal amount to a given decimal digits
	 * 
	 * @param decimal
	 * @param decimalDigits
	 * @return
	 */
	protected String roundDecimalAmount(final double decimal, final Integer decimalDigits) {

		if (decimalDigits > 0) {

			final DecimalFormat df = new DecimalFormat();

			df.setMinimumFractionDigits(decimalDigits);
			df.setMaximumFractionDigits(decimalDigits);
			df.setMaximumIntegerDigits(5);
			df.setMinimumIntegerDigits(5);
			df.setGroupingUsed(false);

			final DecimalFormatSymbols custom = new DecimalFormatSymbols();
			custom.setDecimalSeparator(',');
			df.setDecimalFormatSymbols(custom);

			return df.format(Double.valueOf(decimal));

		}
		return EMPTY;
	}
}

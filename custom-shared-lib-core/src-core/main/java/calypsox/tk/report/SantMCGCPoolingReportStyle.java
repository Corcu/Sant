package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

/**
 * 
 * @author Juan Angel Torija
 * 
 */
@SuppressWarnings("rawtypes")
public class SantMCGCPoolingReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String CONTRATO = "CONTRATO";
	public static final String ISIN = "ISIN";
	public static final String DIVISA = "DIVISA";
	public static final String DIRECTION = "DIRECTION";
	public static final String COLLATERAL_VALUE_CESTA = "COLLATERAL_VALUE_CESTA";
	public static final String NOMINAL_ISIN = "NOMINAL_ISIN";
	public static final String COLLATERAL_VALUE_ISIN = "COLLATERAL_VALUE_ISIN";
	public static final String PORCENTAJE_ISIN = "PORCENTAJE_ISIN";
	public static final String PORCENTAJE_ISIN_DIV100 = "PORCENTAJE_ISIN_DIV100";
	public static final String NOMINAL_CESTA = "NOMINAL_CESTA";

	private static final String EMPTY = "";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		SantMCGCPoolingItem santMCGCPoolingItem = (SantMCGCPoolingItem) row.getProperty(SantMCGCPoolingReport.TYPE);

		if (CONTRATO.equals(columnName)) {
			return santMCGCPoolingItem.getIsinCesta();
		} else if (ISIN.equals(columnName)) {
			return santMCGCPoolingItem.getIsinAlloc();
		} else if (DIVISA.equals(columnName)) {
			return santMCGCPoolingItem.getCurrency();
		} else if (DIRECTION.equals(columnName)) {
			return santMCGCPoolingItem.getDirection();
		} else if (COLLATERAL_VALUE_CESTA.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingItem.getCollateralCestaAlloc());
		} else if (NOMINAL_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingItem.getNominal());
		} else if (COLLATERAL_VALUE_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingItem.getCollateralValue());
		} else if (NOMINAL_CESTA.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCGCPoolingItem.getNominalCesta());
		} else if (PORCENTAJE_ISIN.equals(columnName)) {
			if((santMCGCPoolingItem.getCollateralValue()!=null && santMCGCPoolingItem.getCollateralValue().doubleValue()==0) || santMCGCPoolingItem.getCollateralValue() == null) {
				final String porcnt = roundDecimalAmount(Double.valueOf(0), 12);
				return porcnt;
			}else {
				return roundDecimalAmount((santMCGCPoolingItem.getCollateralValue() / santMCGCPoolingItem.getCollateralCestaAlloc()) * 100,
						12);
			}
		}
		else if (PORCENTAJE_ISIN_DIV100.equals(columnName)) {
			if((santMCGCPoolingItem.getCollateralValue()!=null && santMCGCPoolingItem.getCollateralValue().doubleValue()==0) || santMCGCPoolingItem.getCollateralValue() == null) {
				final String porcnt = roundDecimalAmountDIV100(Double.valueOf(0), 12);
				return porcnt;
			}else {
				return roundDecimalAmountDIV100((santMCGCPoolingItem.getCollateralValue() / santMCGCPoolingItem.getCollateralCestaAlloc()),
						12);
			}
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

	/**
	 * Round decimal amount to a given decimal digits
	 *
	 * @param decimal
	 * @param decimalDigits
	 * @return
	 */
	protected String roundDecimalAmountDIV100(final double decimal, final Integer decimalDigits) {

		if (decimalDigits > 0) {

			final DecimalFormat df = new DecimalFormat();

			df.setMinimumFractionDigits(decimalDigits);
			df.setMaximumFractionDigits(decimalDigits);
			df.setMaximumIntegerDigits(5);
			df.setMinimumIntegerDigits(1);
			df.setGroupingUsed(false);

			final DecimalFormatSymbols custom = new DecimalFormatSymbols();
			custom.setDecimalSeparator(',');
			df.setDecimalFormatSymbols(custom);

			return df.format(Double.valueOf(decimal));

		}
		return EMPTY;
	}
}

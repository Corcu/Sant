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
public class SantMCTripartyReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String FATHERID = "FATHERID";
	public static final String ISIN = "ISIN";
	public static final String DIVISA = "DIVISA";
	public static final String DIRECTION = "DIRECTION";
	public static final String COLLATERAL_VALUE_FATHER = "COLLATERAL_VALUE_FATHER";
	public static final String COLLATERAL_VALUE_FATHER_MTM = "COLLATERAL_VALUE_FATHER_MTM";
	public static final String NOMINAL_ISIN = "NOMINAL_ISIN";
	public static final String COLLATERAL_VALUE_ISIN = "COLLATERAL_VALUE_ISIN";
	public static final String PORCENTAJE_ISIN = "PORCENTAJE_ISIN";

	private static final String EMPTY = "";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		SantMCTripartyItem santMCTripartyItem = (SantMCTripartyItem) row.getProperty(SantMCTripartyReport.TYPE);

		if (FATHERID.equals(columnName)) {
			return santMCTripartyItem.getId();
		} else if (ISIN.equals(columnName)) {
			return santMCTripartyItem.getIsin();
		} else if (DIVISA.equals(columnName)) {
			return santMCTripartyItem.getCurrency();
		} else if (DIRECTION.equals(columnName)) {
			return santMCTripartyItem.getDirection();
		} else if (COLLATERAL_VALUE_FATHER.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCTripartyItem.getCollateralFatherAlloc());
		} else if (COLLATERAL_VALUE_FATHER_MTM.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCTripartyItem.getCollateralFather());
		} else if (NOMINAL_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCTripartyItem.getNominal());
		} else if (COLLATERAL_VALUE_ISIN.equals(columnName)) {
			return CollateralUtilities.formatDouble(santMCTripartyItem.getCollateralValue());
		} else if (PORCENTAJE_ISIN.equals(columnName)) {
			return roundDecimalAmount(
					(santMCTripartyItem.getCollateralValue() / santMCTripartyItem.getCollateralFatherAlloc()) * 100, 12);
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

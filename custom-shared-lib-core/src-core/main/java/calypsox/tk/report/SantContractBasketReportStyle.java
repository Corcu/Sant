package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;


@SuppressWarnings("rawtypes")
public class SantContractBasketReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	private static final String ACUERDO = "ACUERDO";
    private static final String ISIN = "ISIN";
    private static final String DIVISA = "DIVISA";
    private static final String CONTRAPARTIDA = "CONTRAPARTIDA";


	private static final String EMPTY = "";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        AcuerdoIsinItem contractBasket = row.getProperty(SantContractBasketReport.TYPE);

		if (ACUERDO.equals(columnName)) {
			return contractBasket.getAcuerdo();
		} else if (ISIN.equals(columnName)) {
			return contractBasket.getIsin();
		} else if (DIVISA.equals(columnName)) {
			return contractBasket.getCcy();
		} else if (CONTRAPARTIDA.equals(columnName)) {
            return contractBasket.getCpty();
        }
		return "";
	}

	/**
	 * Round decimal amount to a given decimal digits
	 * 
	 * @param decimal value to round
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
	protected String roundDecimalAmountNoMinInetger(final double decimal, final Integer decimalDigits) {

		if (decimalDigits > 0) {

			final DecimalFormat df = new DecimalFormat();

			df.setMinimumFractionDigits(2);
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

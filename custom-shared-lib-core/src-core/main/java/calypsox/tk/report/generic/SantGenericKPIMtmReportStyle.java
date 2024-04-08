package calypsox.tk.report.generic;

import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

import calypsox.tk.report.KPIMtmReportItem;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public abstract class SantGenericKPIMtmReportStyle extends ReportStyle {

	private static final long serialVersionUID = 9199798746171036578L;

	public static final String REPORT_DATE = "Report Date";
	public static final String AGREEMENT_OWNER = "Agreement Owner";
	public static final String DEAL_OWNER = "Deal Owner";

	public static final String USD_MTM_SUM = "USD_MTM_SUM";
	public static final String EUR_MTM_SUM = "EUR_MTM_SUM";

	public static final String[] DEFAULTS_COLUMNS = { REPORT_DATE, AGREEMENT_OWNER, DEAL_OWNER, USD_MTM_SUM,
			EUR_MTM_SUM };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final KPIMtmReportItem item = (KPIMtmReportItem) row.getProperty(ReportRow.DEFAULT);

		if (columnName.equals(REPORT_DATE)) {
			return row.getProperty(SantGenericKPIMtmReportTemplate.VALUATION_DATE);
		} else if (columnName.equals(AGREEMENT_OWNER)) {
			return item.getAgrOwner();
		} else if (columnName.equals(DEAL_OWNER)) {
			return item.getDealOwner();
		} else if (columnName.equals(USD_MTM_SUM)) {
			return formatNumber(item.getUsdMTMSum());
		} else if (columnName.equals(EUR_MTM_SUM)) {
			return formatNumber(item.getEurMTMSum());
		}

		return null;

	}

	private static String formatNumber(final double number) {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		nf.setGroupingUsed(true);
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);
		String retVal = nf.format(number);

		// @TODO Need to see if there is a better way of doing this
		if (retVal.equals("-,00") || retVal.equals("-0,00")) {
			retVal = ",00";
		}

		return retVal;
	}
}

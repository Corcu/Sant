package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.delinquent.SantDelinquentItem;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantDelinquentMarginCallReportStyle extends ReportStyle {

	private static final long serialVersionUID = -2202944192501415729L;

	public static final String AGREEMENT_NAME = "Agreement Name";
	public static final String DESCRIPTION = "Description";
	public static final String BASE_CCY = "Base Ccy";
	public static final String EVENT_DATE = "Event Date";
	public static final String GROSS_EXPOSURE = "Gross Exposure";
	public static final String CPTY_MTM = "Cpty MtM";
	public static final String AGREED_MTM = "Agreed MtM";
	public static final String MARGIN_AMOUNT = "Margin Amount";
	public static final String VALUE_DIFF = "Value Difference";
	public static final String INDEPENDENT = "Independent Amount";
	public static final String THRESHOLD = "Threshold";
	public static final String PREV_MARG_HELD = "Prev Margin Held";
	public static final String PREV_MARG_PENDING = "Prev Margin Pending";
	public static final String ADDITIONAL_ALLOC_EXEC = "Additional Allocations Executed";
	public static final String POSSIBLE_COLLAT = "Possible Collateral";
	public static final String DELINQUENT_SINCE = "Delinquent Since";
	public static final String COMMENT = "Comments";
	public static final String CONTRACT_STATUS = "Contract Status";

	public static final String[] DEFAULT_COLUMNS = { AGREEMENT_NAME, DESCRIPTION, BASE_CCY, EVENT_DATE, GROSS_EXPOSURE,
			CPTY_MTM, AGREED_MTM, MARGIN_AMOUNT, VALUE_DIFF, INDEPENDENT, THRESHOLD, PREV_MARG_HELD, PREV_MARG_PENDING,
			ADDITIONAL_ALLOC_EXEC, POSSIBLE_COLLAT, DELINQUENT_SINCE, COMMENT, CONTRACT_STATUS };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow reportRow, final String columnName, final Vector errors)
			throws InvalidParameterException {

		SantDelinquentItem delinquent = (SantDelinquentItem) reportRow.getProperty("SantDelinquentItem");

		return delinquent.getColumnValue(columnName);
	}

}

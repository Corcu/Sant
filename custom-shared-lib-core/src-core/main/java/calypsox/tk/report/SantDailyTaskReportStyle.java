package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.dailytask.SantDailyTaskItem;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantDailyTaskReportStyle extends ReportStyle {

	private static final long serialVersionUID = -4641202961624280162L;

	public static final String ENTRY_STATUS = "Entry Status";
	public static final String MARGIN_CALL_SITUATION = "Margin Call Situation";
	public static final String PROCESS_DATE = "Process Date";
	public static final String EVENT_DATE = "Event Date";
	public static final String EXEC_ALLOC_AMOUNT = "Executed Allocation Amount";
	public static final String NON_EXEC_ALLOC_AMOUNT = "Not Executed Allocation Amount";
	public static final String NOT_ALLOC_AMOUNT = "Not allocated amount";
	public static final String BASE_CURRENCY = "Base Currency";
	public static final String GROSS_EXPOSURE_PRIOR_TOLEANCE = "Gross Exposure (Prior Tolerance)";
	public static final String GROSS_EXPOSURE_AFTER_TOLEANCE = "Gross Exposure (After Tolerance)";
	public static final String INDEPENDENT = "Independent";
	public static final String THRESHOLD = "Threshold";
	public static final String MINIMUM_TRANSFER = "Minimum Transfer";
	public static final String BALANCE = "Balance";
	public static final String AGREEMENT = "Collateral Agreement";
	public static final String HEAD_CLONE = "Head/Clone Ind.";
	public static final String AGREEMENT_DESC = "Coll Agreement Desc";
	public static final String OWNER = "Owner";
	public static final String VAL_AGENT = "Valuation Agent";
	public static final String CALC_AGENT = "Calc Agent Id";
	public static final String MARGIN_CALL = "Margin Call";
	public static final String MARGIN_CALLCalc = "Margin Call Calc";

	public static final String[] DEFAULT_COLUMNS = { ENTRY_STATUS, MARGIN_CALL_SITUATION, PROCESS_DATE, EVENT_DATE,
			EXEC_ALLOC_AMOUNT, NON_EXEC_ALLOC_AMOUNT, NOT_ALLOC_AMOUNT, BASE_CURRENCY, GROSS_EXPOSURE_PRIOR_TOLEANCE,
			GROSS_EXPOSURE_AFTER_TOLEANCE, INDEPENDENT, THRESHOLD, MINIMUM_TRANSFER, BALANCE, AGREEMENT, HEAD_CLONE,
			AGREEMENT_DESC, OWNER, VAL_AGENT, CALC_AGENT, MARGIN_CALL,MARGIN_CALLCalc  };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final SantDailyTaskItem item = (SantDailyTaskItem) row.getProperty("SantDailyTaskItem");
		return item.getColumnValue(columnName);

	}
}

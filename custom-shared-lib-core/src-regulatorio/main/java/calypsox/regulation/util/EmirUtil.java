package calypsox.regulation.util;

import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

public class EmirUtil {



	private static final String TEMPLATE_ERROR = "Template not assign.";
	private static final String PROCESS_DATE_ERROR = "Process Start Date cannot be empty.";
	/** Error message for Submitter Report field. */
	private static final String SUBMITTER_VALUE_ERROR = "Submitter Report value is mandatory.";
	private static final String GROUPING_REPORT_ERROR = "Replace Owner selector should be filled if Groping Submitter Report is selected.";

	/**
	 * Row Properties
	 */
	public static final String TRADE_NAME = "Trade";
	public static final String MARGIN_CALL_CONFIG_NAME = "MarginCallConfig";
	public static final String MARGIN_CALL_ENTRY_NAME = "MarginCallEntry";
	public static final String MARGIN_CALL_DETAIL_ENTRY_NAME = "MarginCallDetailEntry";
	public static final String PROCESSDATE = "ProcessDate";
	public static final String ISDELTA = "isDelta";
	public static final String TRADE_ACTION = "trade_action";
	/**
	 * CollateralConfig additional field
	 */
	public static final String EMIR_COLLATERAL_VALUE = "EMIR_COLLATERAL_VALUE";


	public static void checkReportTemplate(ReportTemplate template, final Vector<String> errors) {
		if (template == null) {
			addErrors(errors, TEMPLATE_ERROR);
		}
	}

	/**
	 *
	 * @param errors
	 * @return true if process date has been read from the template
	 */
	public static void computeProcessDate(final JDate processDate, final Vector<String> errors) {
		if (processDate == null) {
			addErrors(errors, PROCESS_DATE_ERROR);
		}
	}

	/**
	 *
	 * @param errors
	 * @return true if Submitter Report field is
	 */
	public static void checkSubmitterReportField(String submitterValue, final Vector<String> errors) {
		if (Util.isEmpty(submitterValue)) {
			errors.add(SUBMITTER_VALUE_ERROR);
			Log.error(EmirUtil.class.getSimpleName(), SUBMITTER_VALUE_ERROR);
		}
	}

	public static void checkGroupingReport(String groupingReport, String replaceOwner, final Vector<String> errors) {
		if (!Util.isEmpty(groupingReport) && Util.isEmpty(replaceOwner)) {
			errors.add(GROUPING_REPORT_ERROR);
			Log.error(EmirUtil.class.getSimpleName(), GROUPING_REPORT_ERROR);
		}
	}

	/**
	 * Add error to the errors list and log the error
	 *
	 * @param errors
	 * @param error
	 */
	public static void addErrors(final Vector<String> errors, final String error) {
		errors.add(error);
		Log.error(EmirUtil.class.getSimpleName(), error);
	}

}

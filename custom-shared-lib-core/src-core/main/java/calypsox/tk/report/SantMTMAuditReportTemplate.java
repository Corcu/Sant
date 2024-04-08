package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantMTMAuditReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 8063284024922087378L;
	public static String MTM_VAL_DATE = "MTM_VAL_DATE";
	public static String AGR_IDS = "AGR_IDS";
	public static String MTM_CURRENCY = "MTM_CURRENCY";
	public static String MTM_CHANGE_REASON = "MTM_CHANGE_REASON";

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantMTMAuditReportStyle.DEFAULTS_COLUMNS);
	}
}

package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantDailyTaskReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 9173236303846629864L;

	@Override
	public void setDefaults() {
		setColumns(SantDailyTaskReportStyle.DEFAULT_COLUMNS);
	}

}

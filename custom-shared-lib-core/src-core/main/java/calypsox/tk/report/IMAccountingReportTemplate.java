package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class IMAccountingReportTemplate extends ReportTemplate{
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	 public void setDefaults() {
		super.setDefaults();
		setColumns(IMAccountingReportStyle.DEFAULTS_COLUMNS);
	}
}

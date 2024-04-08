package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class CumbreAccountingReportTemplate extends ReportTemplate {

	/** UID*/
	private static final long serialVersionUID = 1L;
	
	@Override
	 public void setDefaults() {
		super.setDefaults();
		setColumns(CumbreAccountingReportStyle.getCumbreDefaultColumns());
	}

}

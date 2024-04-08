package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantCreditRatingReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -1L;

	@Override
	public void setDefaults() {
		setColumns(SantCreditRatingReportStyle.DEFAULT_COLUMNS);
	}

}

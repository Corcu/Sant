
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantNoMTMVariationReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 123L;
	
	public static final String HOLIDAYS = "HOLIDAYS";
	
	
    @Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantNoMTMVariationReportStyle.DEFAULTS_COLUMNS);
	}
}

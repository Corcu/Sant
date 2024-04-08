package calypsox.tk.report;

import com.calypso.tk.report.MarginCallDetailEntryReportTemplate;
import com.calypso.tk.report.ReportTemplate;

public class SantEmirCLMReportTemplate extends MarginCallDetailEntryReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4649891461655157039L;

	public void setDefaults() {
		super.setDefaults();
		// Set defaults
		put(ReportTemplate.TITLE, "Sant EMIR CLM Report");
	}
}

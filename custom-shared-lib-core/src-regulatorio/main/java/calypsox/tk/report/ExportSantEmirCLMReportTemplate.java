package calypsox.tk.report;

import com.calypso.tk.report.MarginCallDetailEntryReportTemplate;
import com.calypso.tk.report.ReportTemplate;

public class ExportSantEmirCLMReportTemplate extends MarginCallDetailEntryReportTemplate {

    public void setDefaults() {
        super.setDefaults();
        // Set defaults
        put(ReportTemplate.TITLE, "Sant EMIR CLM Report");
    }

}

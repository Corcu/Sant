package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

@SuppressWarnings("serial")
public class ELBEIsinCollatTemplate extends MarginCallReportTemplate {
    @Override
    public void setDefaults() {
	super.setDefaults();
	setColumns(ELBEIsinCollatReportStyle.DEFAULTS_COLUMNS);
    }
}

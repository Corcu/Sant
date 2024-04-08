package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;


@SuppressWarnings("serial")
public class Opt_Collateral_MarginCallReportTemplate extends MarginCallReportTemplate {
	@Override
    public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_Collateral_MarginCallReportStyle.DEFAULTS_COLUMNS);
    }
}

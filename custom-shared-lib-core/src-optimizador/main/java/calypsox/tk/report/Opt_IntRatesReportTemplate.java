package calypsox.tk.report;

import com.calypso.tk.report.QuoteReportTemplate;

@SuppressWarnings("serial")
public class Opt_IntRatesReportTemplate extends QuoteReportTemplate {
	@Override
    public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_IntRatesReportStyle.DEFAULTS_COLUMNS);
    }
}

package calypsox.tk.report;

import com.calypso.tk.report.QuoteReportTemplate;

@SuppressWarnings("serial")
public class Opt_FXClosingPricesReportTemplate extends QuoteReportTemplate {
	@Override
    public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_FXClosingPricesReportStyle.DEFAULT_COLUMNS);
    }
}

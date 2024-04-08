package calypsox.tk.report;

import com.calypso.tk.report.BondReportTemplate;

@SuppressWarnings("serial")
public class Opt_BondPricesReportTemplate extends BondReportTemplate {
	@Override
    public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_BondPricesReportStyle.DEFAULTS_COLUMNS);
    }
}

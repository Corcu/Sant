package calypsox.tk.report;

import com.calypso.tk.report.EquityReportTemplate;

@SuppressWarnings("rawtypes")
public class Opt_EquityPricesReportTemplate extends EquityReportTemplate {

	private static final long serialVersionUID = -3586111182897168193L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_EquityPricesReportStyle.DEFAULTS_COLUMNS);
	}
}

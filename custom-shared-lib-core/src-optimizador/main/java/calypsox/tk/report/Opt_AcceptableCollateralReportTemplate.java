package calypsox.tk.report;

import com.calypso.tk.report.ProductReportTemplate;

public class Opt_AcceptableCollateralReportTemplate extends ProductReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String OPT_ACCEPTABLE_COLLAT_ITEM = "OPT_ACCEPTABLE_COLLAT_ITEM";

	@Override
	public void setDefaults() {
		// super.setDefaults();
		setColumns(Opt_AcceptableCollateralReportStyle.DEFAULTS_COLUMNS);
	}
}

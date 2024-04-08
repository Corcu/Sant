package calypsox.tk.report;

import com.calypso.tk.report.ProductReportTemplate;

public class Opt_AcceptableCollateralMMOOReportTemplate extends ProductReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String OPT_ACCEPTABLE_COLLAT_MMOO_ITEM = "OPT_ACCEPTABLE_COLLAT_MMOO_ITEM";

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_AcceptableCollateralMMOOReportStyle.DEFAULTS_COLUMNS);
	}
}

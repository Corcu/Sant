package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class OptimSecurityFiltersReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 7319310272918398103L;

	public static final String OPTIM_CONFIG_IDS = "OPTIM_CONFIG_IDS";

	public static final String OPTIM_CONFIG = "OPTIM_CONFIG";
	public static final String TARGET_CATEGORY = "OPTIM_CONFIG_TARGET";
	public static final String FILTER_ELEMENT = "FILTER_ELEMENT";

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(OptimSecurityFiltersReportStyle.DEFAULTS_COLUMNS);
	}
}

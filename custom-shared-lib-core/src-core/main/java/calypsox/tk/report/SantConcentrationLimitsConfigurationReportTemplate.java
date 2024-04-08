package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class SantConcentrationLimitsConfigurationReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 12345L;

	public static final String CONC_LIMITS_ITEM = "CONC_LIMITS_ITEM";

	@Override
	public void setDefaults() {
		// super.setDefaults();
		setColumns(SantConcentrationLimitsConfigurationReportStyle.DEFAULTS_COLUMNS);
	}
}

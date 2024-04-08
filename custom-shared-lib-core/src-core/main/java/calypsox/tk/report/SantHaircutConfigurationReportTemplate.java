package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class SantHaircutConfigurationReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 1234L;

	public static final String HAIRCUT_CONF_ITEM = "HAIRCUT_CONF_ITEM";

	@Override
	public void setDefaults() {
		// super.setDefaults();
		setColumns(SantHaircutConfigurationReportStyle.DEFAULTS_COLUMNS);
	}
}

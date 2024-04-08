package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class Opt_HaircutDefinitionReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String OPT_HAIRCUT_DEF_ITEM = "OPT_HAIRCUT_DEF_ITEM";

	@Override
	public void setDefaults() {
		// super.setDefaults();
		setColumns(Opt_HaircutDefinitionReportStyle.DEFAULTS_COLUMNS);
	}
}

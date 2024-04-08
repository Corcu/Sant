package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericKPIMtmReportTemplate;

@SuppressWarnings("serial")
public class KGR_Export_MtMReportTemplate extends SantGenericKPIMtmReportTemplate {
	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(KGR_Export_MtMReportStyle.DEFAULTS_COLUMNS);
	}
}

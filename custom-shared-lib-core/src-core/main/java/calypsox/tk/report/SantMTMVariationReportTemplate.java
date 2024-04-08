package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericKPIMtmReportTemplate;

public class SantMTMVariationReportTemplate extends
	SantGenericKPIMtmReportTemplate {

    private static final long serialVersionUID = 123L;

    @Override
    public void setDefaults() {
	super.setDefaults();
	setColumns(SantMTMVariationReportStyle.DEFAULTS_COLUMNS);

    }
}

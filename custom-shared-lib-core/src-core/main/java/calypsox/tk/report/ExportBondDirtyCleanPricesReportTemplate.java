package calypsox.tk.report;

import com.calypso.tk.report.BondReportTemplate;

@SuppressWarnings("serial")
public class ExportBondDirtyCleanPricesReportTemplate extends
	BondReportTemplate {
    @Override
    public void setDefaults() {
	super.setDefaults();

	setColumns(ExportBondDirtyCleanPricesReportStyle.DEFAULTS_COLUMNS);
    }
}

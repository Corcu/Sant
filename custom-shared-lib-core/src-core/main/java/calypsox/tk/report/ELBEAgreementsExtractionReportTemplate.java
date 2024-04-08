package calypsox.tk.report;

import com.calypso.tk.report.CollateralConfigReportTemplate;

@SuppressWarnings("serial")
public class ELBEAgreementsExtractionReportTemplate extends CollateralConfigReportTemplate {
	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(ELBEAgreementsExtractionReportStyle.DEFAULTS_COLUMNS);
	}
}

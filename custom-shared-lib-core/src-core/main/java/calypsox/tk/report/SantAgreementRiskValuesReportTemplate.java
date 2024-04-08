package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class SantAgreementRiskValuesReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 346124763935135265L;

	@Override
	public void setDefaults() {
		super.setDefaults();

		setColumns(SantAgreementRiskValuesReportStyle.DEFAULTS_COLUMNS);
	}
}

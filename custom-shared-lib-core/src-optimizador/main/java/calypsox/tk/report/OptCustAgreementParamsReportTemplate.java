package calypsox.tk.report;

import com.calypso.tk.report.CollateralConfigReportTemplate;

public class OptCustAgreementParamsReportTemplate extends CollateralConfigReportTemplate {

	private static final long serialVersionUID = 291602201833179763L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(OptCustAgreementParamsReportStyle.DEFAULTS_COLUMNS);
	}
}

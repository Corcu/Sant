package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantHaircutByIssuerReportTemplate extends SantGenericTradeReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String ISSUER_ID = "ISSUER_ID";

	@Override
	public void setDefaults() {
		super.setDefaults();
		// setColumns(DatamartContractsExtractionReportStyle.DEFAULTS_COLUMNS);
	}
}

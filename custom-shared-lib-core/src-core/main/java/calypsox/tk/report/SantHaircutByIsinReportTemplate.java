package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantHaircutByIsinReportTemplate extends SantGenericTradeReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String ISIN_ID = "ISIN_ID";

	public static final String ISSUER = "ISSUER";

	public static final String PRODUCT = "PRODUCT";

	public static final String VAL_DATE = "VAL_DATE";

	@Override
	public void setDefaults() {
		super.setDefaults();
		// setColumns(DatamartContractsExtractionReportStyle.DEFAULTS_COLUMNS);
	}
}

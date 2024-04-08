package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class DatamartContractsExtractionReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String CONTRACT_WRAPPER = "ContractWrapper";
	public static final String PROCESS_DATE = "ProcessDate";

	@Override
	public void setDefaults() {
		super.setDefaults();
		// setColumns(DatamartContractsExtractionReportStyle.DEFAULTS_COLUMNS);
	}
}

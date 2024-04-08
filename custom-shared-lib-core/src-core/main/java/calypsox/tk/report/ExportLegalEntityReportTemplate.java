package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class ExportLegalEntityReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 8446173031056975440L;
	public final static String GENERATION_DATE = "Generation Date";
	public final static String CP_DESC = "Counterparty Description";
	public final static String PROCESSING_ORG = "Processing Org";
	public final static String COUNTERPARTY = "Counterparty";

	@Override
	public void setDefaults() {
		super.setDefaults();
		final String[] columnasDefecto = { GENERATION_DATE, PROCESSING_ORG, COUNTERPARTY, CP_DESC };
		setColumns(columnasDefecto);
	}
}
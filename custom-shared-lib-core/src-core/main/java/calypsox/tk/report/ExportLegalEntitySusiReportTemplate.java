package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class ExportLegalEntitySusiReportTemplate extends MarginCallReportTemplate {

	private static final long serialVersionUID = 8446173031056975440L;
	public final static String GENERATION_DATE = "Generation Date";
	public final static String CP_DESC = "Counterparty Description";
	public final static String PROCESSING_ORG = "Processing Org";
	public final static String COUNTERPARTY = "Counterparty";
	public final static String OVERNIGHT = "Overnight";

	@Override
	public void setDefaults() {
		super.setDefaults();
		// Default columns SUSI REPO.
		final String[] columnasSusi = { GENERATION_DATE, PROCESSING_ORG, COUNTERPARTY, CP_DESC, OVERNIGHT };
		setColumns(columnasSusi);
	}
}
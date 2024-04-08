package calypsox.tk.report;

import java.util.Hashtable;
import java.util.Vector;

import com.calypso.tk.report.CollateralConfigReportTemplate;

/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
public class SantMCConfigReportTemplate extends CollateralConfigReportTemplate {

	public static final Hashtable<String, String> DEFAULT_TEMPALTE_COL_NAMES = new Hashtable<String, String>();
	public static final Hashtable<String, String> DEFAULT_TEMPALTE_COL_FORMAT = new Hashtable<String, String>();

	public static final String EXTRACTION_DATE = "Extraction Date";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		// Set default columns
		Vector<String> columns = new Vector<String>();
		columns.add(SantMCConfigReportStyle.CONTRACT_ID);
		columns.add(SantMCConfigReportStyle.NAME);
		columns.add(SantMCConfigReportStyle.PROCESSING_ORG);
		columns.add(SantMCConfigReportStyle.START_DATE);
		columns.add(SantMCConfigReportStyle.END_DATE);
		columns.add(SantMCConfigReportStyle.EXTRACTION_DATE);

		setColumns((String[]) columns.toArray(new String[columns.size()]));
		// init default columns names
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.CONTRACT_ID, "CODE");
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.NAME, "CONTRACT");
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.PROCESSING_ORG, "OWNER");
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.START_DATE, "EFFECTIVE_DATE_MASTER_AGREEMENT");
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.END_DATE, "END_COLLATERAL_DATE AGREEMENT");
		DEFAULT_TEMPALTE_COL_NAMES.put(SantMCConfigReportStyle.EXTRACTION_DATE, "INFORM_GENERATION_DATE");
		// setColumnNamesHash(DEFAULT_TEMPALTE_COL_NAMES);
		DEFAULT_TEMPALTE_COL_FORMAT.put(SantMCConfigReportStyle.START_DATE, "DATE,DDMMYYYY,None,/");
		DEFAULT_TEMPALTE_COL_FORMAT.put(SantMCConfigReportStyle.END_DATE, "DATE,DDMMYYYY,None,/");
		// setColumnFormats(DEFAULT_TEMPALTE_COL_FORMAT);
	}
}

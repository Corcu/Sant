package calypsox.tk.report;

import com.calypso.tk.report.CollateralConfigReportTemplate;

/**
 * 
 * 
 * @author Guillermo
 * 
 */
public class SACCRStaticReportTemplate extends CollateralConfigReportTemplate {
	
	
	private static final long serialVersionUID = 3934849681876005L;
		
	public final static String ENTITY = "Branch";
	
	public final static String PRODUCT = "Product"; 
	
	public final static String REPORT_TYPE  = "Report_type";
	
	public static final String SOURCE_SYSTEM = "Processing Org.Attribute.SOURCE_SYSTEM_IRIS";
	
	/**
	 * Properties names of custom columns
	 */
	//same property as CollateralConfigReport
	public static final String COLLATERAL_CONFIG = "MarginCallConfig";
	
	/*
	 * Default Columns constants
	 */
	public static String[] DEFAULT_COLUMNS_COL_LE = {SOURCE_SYSTEM,CollateralConfigReportStyle.NAME,"Entity.Short Name", "Entity.Full Name", "ADDITIONAL_FIELD.HEAD_CLONE"};
	
	public static String[] DEFAULT_COLUMNS_COL_PRODUCT = {SOURCE_SYSTEM,CollateralConfigReportStyle.NAME, PRODUCT, "ADDITIONAL_FIELD.HEAD_CLONE"};
	
	@Override
	public void setDefaults() {
		//empty by default
		setColumns( DEFAULT_COLUMNS_COL_LE);

	}
}

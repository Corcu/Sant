/**
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.MarginCallPositionBaseReportStyle;
import com.calypso.tk.report.ReportTemplate;

/**
 * SA CCR Balances, template
 * 
 * @author Guillermo Solano
 * @version 1.0 
 * @Date 15/12/2016
 */	

public class SACCRCMPositionReportTemplate extends ReportTemplate {

	//Serial UID
	private static final long serialVersionUID = -1706339580668013592L;
	
	/*
	 * Attributes + columns names for custom columns
	 */
	public static final String COLLATERAL_CONFIG_TYPE = "Contract type";
	
	public static final String COLLATERAL_MOVEMENT_TYPE = "Movement type";
	
	public static final String COLLATERAL_PROCESS_DATE = "Collateral Process Date";
	
	public static final String COLLATERAL_VALUE_DATE = "Collateral Value Date";
	
	public static final String COLLATERAL_MATURITY_DATE = "Collateral Maturity Date";
	
	public static final String COLLATERAL_IN_TRANSIT = "Collateral In Transit";
	
	public static final String CCP_PLATFORM = "CCP Platform";
	//TODO: not define yet in Collateral
	public static final String SEGREGATED_COLLATERAL = "Segregated Collateral";
	
	public static final String MARGIN_CALL_ENTRY_DIRECTION = "MCEntry Direction custom";
	
	public static final String SOURCE_SYSTEM = "MarginCallConfig.Processing Org.Attribute.SOURCE_SYSTEM_IRIS";
	
	public static final String MATURITY_OFFSET = "MaturityOffset";
	
	public static final String MARGIN_TYPE  = "Margin Type";
	
	/**
	 * Properties names of custom columns
	 */
	//same property as CollateralConfigReport
	public static final String COLLATERAL_CONFIG = "MarginCallConfig";
	// idem for MarginCallEntryReport
	public static final String MARGIN_CALL_ENTRY = "MarginCallEntry";
	// mc position
	public static final String MC_POSITION = "Default";
	// products
	public static final String POSITION_PRODUCT = "Product";
	
	/*
	 *  Inventory postion type constants
	 */
	public final static String ACTUAL = "ACTUAL";
	
	public final static String THEORETICAL = "THEORETICAL";
	
	/**
	 * 
	 */
	public final static String CASH = "Cash";
	
	public final static String SECURITY = "Security";
	
	/*
	 * Default Columns constants
	 */
	public static String[] DEFAULT_COLUMNS = {SOURCE_SYSTEM,"MarginCallConfig.Name", "MarginCallConfig.Contract Type",
			"MarginCallConfig.Legal Entity.Short Name","MarginCallConfig.Book", COLLATERAL_CONFIG_TYPE,COLLATERAL_MOVEMENT_TYPE,
			MarginCallPositionBaseReportStyle.CURRENCY, "Nominal",MarginCallPositionBaseReportStyle.FX_RATE,
			MarginCallPositionBaseReportStyle.CONTRACT_VALUE, COLLATERAL_IN_TRANSIT, COLLATERAL_PROCESS_DATE,COLLATERAL_VALUE_DATE, COLLATERAL_MATURITY_DATE,
			MARGIN_CALL_ENTRY_DIRECTION,MarginCallPositionBaseReportStyle.HAIRCUT,"Product.PRODUCT_CODE.ISIN","Product.Issuer",
			MarginCallPositionReportStyle.SANT_DIRTY_PRICE, MarginCallPositionBaseReportStyle.CLEAN_PRICE,"Product.Maturity Date","Product.Product Type",CCP_PLATFORM,
			"MarginCallConfig.ADDITIONAL_FIELD.CCP", SEGREGATED_COLLATERAL, MARGIN_TYPE};

	/**
	 * Default columns, define in order based on DDR
	 */
	@Override
	public void setDefaults() {
		setColumns(DEFAULT_COLUMNS);
	}
}

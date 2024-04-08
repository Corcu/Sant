/**
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

/**
 * SA CCR Balances, template
 * 
 * @author Guillermo Solano
 * @version 1.0 
 * @Date 15/12/2016
 */	

public class SACCRBalancesReportTemplate extends ReportTemplate {

	//Serial UID
	private static final long serialVersionUID = -1706339580668013592L;
	
	/*
	 * Attributes + columns names for custom columns
	 */
	public static final String DIRTY_PRICE_QUOTE = "PE Price D-1";
	
	public static final String CLEAN_PRICE_QUOTE = "Clean Price D-1";
	
	public static final String PRICING_ENV_PROPERTY = "Pricing Environment";
	
	public static final String MARKET_VALUATION = "Market Value";
	
	public static final String NOMINAL = "Nominal";
	
	public static final String FX_RATE_NAME = "EUR Fixing";
	
	public static final String COLLATERAL_CONFIG_TYPE = "Contract type";
	
	public static final String COLLATERAL_MOVEMENT_TYPE = "Movement type";
	
	public static final String COLLATERAL_PROCESS_DATE = "Collateral Process Date";
	
	public static final String COLLATERAL_VALUE_DATE = "Collateral Value Date";
	
	public static final String COLLATERAL_MATURITY_DATE = "Collateral Maturity Date";
	
	public static final String COLLATERAL_IN_TRANSIT = "Collateral In Transit";
	
	public static final String HAIRCUT = "Product Haircut";
	
	public static final String CCP_PLATFORM = "CCP Platform";
	//TODO: not define yet in Collateral
	public static final String SEGREGATED_COLLATERAL = "Segregated Collateral";
	
	public static final String MARGIN_CALL_ENTRY_DIRECTION = "MCEntry Direction custom";
	
	public static final String SOURCE_SYSTEM = "MarginCallConfig.Processing Org.Attribute.SOURCE_SYSTEM_IRIS";
	
	public static final String MATURITY_OFFSET = "MaturityOffset";
	
	/**
	 * Properties names of custom columns
	 */
	//same property as CollateralConfigReport
	public static final String COLLATERAL_CONFIG = "MarginCallConfig";
	// idem for MarginCallEntryReport
	public static final String MARGIN_CALL_ENTRY = "Default";
	
	/*
	 * Default Columns constants
	 */
	public static String[] DEFAULT_COLUMNS = {SOURCE_SYSTEM,"MarginCallConfig.Name",
			"MarginCallConfig.Contract Type", "MarginCallConfig.Legal Entity.Short Name","MarginCallConfig.Book",
			COLLATERAL_CONFIG_TYPE,COLLATERAL_MOVEMENT_TYPE,"Currency",NOMINAL,MARKET_VALUATION, COLLATERAL_IN_TRANSIT,
			COLLATERAL_PROCESS_DATE,COLLATERAL_VALUE_DATE, COLLATERAL_MATURITY_DATE, MARGIN_CALL_ENTRY_DIRECTION,HAIRCUT,
			"PRODUCT_CODE.ISIN","Issuer",DIRTY_PRICE_QUOTE, CLEAN_PRICE_QUOTE,"Maturity Date",/*???*/"Product Type",CCP_PLATFORM,
			"MarginCallConfig.ADDITIONAL_FIELD.CCP"};

	/**
	 * Default columns, define in order based on DDR
	 */
	@Override
	public void setDefaults() {
		setColumns(DEFAULT_COLUMNS);
	}
}

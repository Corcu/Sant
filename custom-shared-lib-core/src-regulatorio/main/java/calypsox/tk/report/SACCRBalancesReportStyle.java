/**
 * 
 */
package calypsox.tk.report;

import static calypsox.tk.report.SACCRBalancesReportTemplate.CCP_PLATFORM;
import static calypsox.tk.report.SACCRBalancesReportTemplate.CLEAN_PRICE_QUOTE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_CONFIG_TYPE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_IN_TRANSIT;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_MATURITY_DATE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_MOVEMENT_TYPE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_PROCESS_DATE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.COLLATERAL_VALUE_DATE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.DIRTY_PRICE_QUOTE;
import static calypsox.tk.report.SACCRBalancesReportTemplate.FX_RATE_NAME;
import static calypsox.tk.report.SACCRBalancesReportTemplate.HAIRCUT;
import static calypsox.tk.report.SACCRBalancesReportTemplate.MARGIN_CALL_ENTRY_DIRECTION;
import static calypsox.tk.report.SACCRBalancesReportTemplate.MARKET_VALUATION;
import static calypsox.tk.report.SACCRBalancesReportTemplate.NOMINAL;
import static calypsox.tk.report.SACCRBalancesReportTemplate.PRICING_ENV_PROPERTY;
import static calypsox.tk.report.SACCRBalancesReportTemplate.SEGREGATED_COLLATERAL;
import static calypsox.tk.report.SACCRBalancesReportTemplate.SOURCE_SYSTEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.util.InstantiateUtil;

/**
 * Style SA CCR BALANCES. Extends all columns for positions (cash & securities), Collateral Configs and MarginCall Entries
 * 
 * @author Guillermo Solano
 *
 * @version 1.01
 * @Date 02/01/2017
 */
public class SACCRBalancesReportStyle extends ReportStyle {
		
	/**
	 * Seriel UID
	 */
	private static final long serialVersionUID = -7786686999420220098L;

	
	/**
	 * Collateral Config Style
	 */
	private CollateralConfigReportStyle collateralConfigReportStyle = null;
		
	/**
	 * Equity & Bond Position Style
	 */
	private final BOSecurityPositionReportStyle secReportStyle = new BOSecurityPositionReportStyle();
	
	/**
	 * Cash Position Style
	 */
	private final BOCashPositionReportStyle cashReportStyle = new BOCashPositionReportStyle();
	
	/**
	 * Margin Call entry Style
	 */
	
	private final  MarginCallEntryReportStyle mcEntryStyle = new MarginCallEntryReportStyle();
	
	/**
	 * Prefix to identify a MarginCallConfig column
	 */
	private static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";
	
	private static String MC_ENTRY_CONFIG_PREFIX = "MarginCallEntry.";
	
	/**
	 * decimals
	 */
	private static Integer DECIMALS = SACCRBalancesReport.decimalsPositions4Number();
	
	/**
	 * Override method to get columns values for the style
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		
		final Inventory pos = (Inventory) row.getProperty(ReportRow.INVENTORY);
		Object valueCol = null; 

		if (pos == null) {
			return null;
		}
		
		//custom columns names
		if (columnName.equals(NOMINAL)) {
			Double nom = (Double) row.getProperty(NOMINAL);
			if (nom != null)
				return new Amount (nom, DECIMALS);		
		
		} else if (columnName.equals(COLLATERAL_CONFIG_TYPE)){
			return row.getProperty(COLLATERAL_CONFIG_TYPE);
		
		} else if (columnName.equals(COLLATERAL_MOVEMENT_TYPE)){
			return row.getProperty(COLLATERAL_MOVEMENT_TYPE);
		
		} else if (columnName.equals(COLLATERAL_PROCESS_DATE)){
			return row.getProperty(COLLATERAL_PROCESS_DATE);
		
		} else if (columnName.equals(COLLATERAL_VALUE_DATE)){
			return row.getProperty(COLLATERAL_VALUE_DATE);
		
		} else if (columnName.equals(COLLATERAL_MATURITY_DATE)){
			return row.getProperty(COLLATERAL_MATURITY_DATE);
		
		} else if (columnName.equals(CCP_PLATFORM)){	
			return Util.isEmpty((String) getMarginCallConfigColumn(row, "MarginCallConfig.ADDITIONAL_FIELD.CCP", errors)) ? "N" : "Y";
			
		} else if (columnName.equals(SEGREGATED_COLLATERAL)){
			return ""; //TODO	
		
		} else if (columnName.equals(MARKET_VALUATION)) {
			Double mv = (Double) row.getProperty(MARKET_VALUATION);
			if (mv != null)
				return new Amount (mv, DECIMALS);	
		
		} else if (columnName.equals(MARGIN_CALL_ENTRY_DIRECTION)){
			
			final String directionLogic = (String) mcEntryStyle.getColumnValue(row, "Direction", errors);	
			if (!Util.isEmpty(directionLogic)){
				return directionLogic.equals("Pay") ? "1" : "0";
			}
		
		//Collateral Config columns names	
		} else if (getMarginCallConfigReportStyle().isMarginCallConfigColumn(MARGIN_CALL_CONFIG_PREFIX, columnName)) {
			
			if (columnName.equals(SOURCE_SYSTEM)){
				//control to ensure is included due to it's importance - mandatory
				final String sourceSystem = (String) getMarginCallConfigColumn(row, columnName, errors);
				if (!Util.isEmpty(sourceSystem))
					return sourceSystem;
				
				else{
					CollateralConfig config = (CollateralConfig)row.getProperty("MarginCallConfig");
					errors.add("ERROR: Source system attribute is not configured for PO: " + config.getProcessingOrg().getCode());
					return "";				
				}
			}
			//check is collateral Config
			return getMarginCallConfigColumn(row, columnName, errors);
		} 
		
		//cash positions style
		if (pos instanceof InventoryCashPosition) {
			
			if (columnName.equals(COLLATERAL_IN_TRANSIT)){
				MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty(SACCRBalancesReportTemplate.MARGIN_CALL_ENTRY);
				if (entry != null){
					Double transit= entry.getPreviousNotSettledCashMargin();
					return new Amount (round(transit, DECIMALS), DECIMALS);
				}				
			}
			//core call
			valueCol = this.cashReportStyle.getColumnValue(row, columnName, errors);
			if (valueCol != null)
				return valueCol;
		
		//security positions style
		} else if (pos instanceof InventorySecurityPosition) {
			
			if (columnName.equals(CLEAN_PRICE_QUOTE)){
				final Double cp = (Double) row.getProperty(CLEAN_PRICE_QUOTE);
				if (cp != null)
					return new Amount(cp, DECIMALS);
			
			} else if (columnName.equals(DIRTY_PRICE_QUOTE)){
				final Double dp = (Double) row.getProperty(DIRTY_PRICE_QUOTE);
				if (dp != null)
					return new Amount(dp, DECIMALS);
			
			} else if (columnName.equals(FX_RATE_NAME)){
				return row.getProperty(FX_RATE_NAME);
			
			} else if (columnName.equals(HAIRCUT)){
				return row.getProperty(HAIRCUT);
			
			} else if (columnName.equals(COLLATERAL_IN_TRANSIT)){
				
				MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty(SACCRBalancesReportTemplate.MARGIN_CALL_ENTRY);
				if (entry != null){
					Double transit= entry.getPreviousNotSettledSecurityMargin();
					return new Amount (round(transit, DECIMALS), DECIMALS);
				}
			}
			
			//core Securities
			valueCol = this.secReportStyle.getColumnValue(row, columnName, errors);
			if (valueCol != null)
				return valueCol;
		
		//finally, try MCEntry Style	
		} else {
				
			//no value return and try MC entry column
			final String newColumnName = ReportStyle.getColumnName(MC_ENTRY_CONFIG_PREFIX, columnName);
			if (!Util.isEmpty(newColumnName))
				return mcEntryStyle.getColumnValue(row, newColumnName, errors);			
		}
			
		return null;	
	}
	
	/**
	 * Recovers the tree list (columns for the style). Adds new columns and Collateral Config & MC Entry styles + custom columns
	 */
	@Override
	public TreeList getTreeList() {
		
		final TreeList treeList = secReportStyle.getTreeList();
		
		if (collateralConfigReportStyle == null){
			collateralConfigReportStyle = getMarginCallConfigReportStyle();	
		}
		
		//add CollateralConfig tree
		if (collateralConfigReportStyle != null){
			addSubTreeList(treeList, new Vector<String>(), MARGIN_CALL_CONFIG_PREFIX, collateralConfigReportStyle.getTreeList());	
		}
		
		
		if (mcEntryStyle != null) {
			addSubTreeList(treeList, new Vector<String>(), MC_ENTRY_CONFIG_PREFIX, mcEntryStyle.getTreeList());
		}
		
		//new columns	
		treeList.add(DIRTY_PRICE_QUOTE);
		treeList.add(CLEAN_PRICE_QUOTE);
		treeList.add(PRICING_ENV_PROPERTY);
		treeList.add(MARKET_VALUATION);
		treeList.add(NOMINAL);
		treeList.add(FX_RATE_NAME);
		treeList.add(COLLATERAL_CONFIG_TYPE);
		treeList.add(COLLATERAL_MOVEMENT_TYPE);
		treeList.add(COLLATERAL_PROCESS_DATE);
		treeList.add(COLLATERAL_VALUE_DATE);
		treeList.add(COLLATERAL_MATURITY_DATE);
		treeList.add(HAIRCUT);
		treeList.add(MARGIN_CALL_ENTRY_DIRECTION);
		treeList.add(Util.dateToMString(JDate.getNow()));
		
		return treeList;
	}

	public static Double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	/**
	 * 	
	 * @param row
	 * @param columnName
	 * @param errors
	 * @return value of Collateral Config if is a MarginCAllConfig Column
	 */
	private Object getMarginCallConfigColumn(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors) {
		
		//Somehow super method isMarginCallConfigColumn returns null. Implemented logic here
		String name = getMarginCallConfigReportStyle().getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);
		return getMarginCallConfigReportStyle().getColumnValue(row, name, errors);	
	}
	
	/**
	 * @return custom CollateralConfigReportStyle. If custom code is not found, it will retrieve the custom version
	 */
	private CollateralConfigReportStyle getMarginCallConfigReportStyle() {
		try {
			if (this.collateralConfigReportStyle == null) {
				String className = "calypsox.tk.report.CollateralConfigReportStyle";

				this.collateralConfigReportStyle =  (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
						true, true);

			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		return this.collateralConfigReportStyle;
	}

	
}

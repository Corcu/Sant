/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.LegalEntityReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.InstantiateUtil;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Vector;

/**
 * Custom MarginCallEntry style, adding columns & extending core class.
 * 
 * @authors OA, AT, GSM.
 * @date 03/03/2015. Fixed resolution of collateralConfig columns
 */
public class MarginCallEntryReportStyle extends com.calypso.tk.report.MarginCallEntryReportStyle {

	/**
	 * Autogenerated serial UID.
	 */
	private static final long serialVersionUID = 62018569970763152L;
	
	/**
	 * Prefix to identify a MarginCallConfig column
	 */
	private static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";
	
	/**
	 * CollateralConfigReportStyle core & custom
	 */
	//core 
	private com.calypso.tk.report.CollateralConfigReportStyle coreCollateralConfigReportStyle = null;
	//custom
	private calypsox.tk.report.CollateralConfigReportStyle customCollateralConfigReportStyle = null;
	
	private LegalEntityReportStyle _leReportStyle = null;

	/**
	 * MarginCallEntryReportStyle Custom columns
	 */
	public final static String SANT_MC_OF_OVERALLEXPOSURE = "Sant_ProportionMarginCall_Of_OverallExposure";
	public final static String SANT_MCC_NAME = "Sant_MCCName";
	public final static String SANT_MCC_TYPE = "Sant_MCCType";
	public final static String SANT_MCC_OWNER_NAME = "Sant_MCCOwnerName";
	public final static String EXCLUDE_FROM_OPTIMIZER = "Exclude From Optimizer";
	public final static String SANT_MCC_STATUS = "Sant_MCCOwnerStatus";
	//GSM 17/08/2017 - Emir RTS9 - new column for UK IM CSD TYPE
	public final static String SANT_MCC_IM_TYPE = "Sant_MCCImType";
	private static final String IM_CSD_TYPE = "IM_CSD_TYPE";
	private static final String CUSTOM_DISPUTE_AMOUNT = "Custom Dispute Amount";
	 
	protected static final HashSet<String> LEGAL_ENTITIES_PREFIXES = new HashSet<String>();

	
	    static { 
	    	LEGAL_ENTITIES_PREFIXES.add("Legal Entity.");
	    	LEGAL_ENTITIES_PREFIXES.add("Processing Org.");
	   }

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		
		if (row == null) {
			return null;
		}

		if (SANT_MC_OF_OVERALLEXPOSURE.equalsIgnoreCase(columnName)) {
			MarginCallEntryDTO entry = getEntryDTO(row);
			
			if (entry == null) {
				return null;
			}
			
			double globalRequiredMargin = entry.getGlobalRequiredMargin();
			double marginRequired = entry.getMarginRequired();
			double result = globalRequiredMargin / marginRequired;
			
			if (Double.isNaN(result)) {
				return "";
			}
			return new Amount(result * 100, 5);
			
		} else if (SANT_MCC_NAME.equals(columnName)) {
			
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null) {
				return null;
			}
			return collateralConfig.getName();
			
		} else if (SANT_MCC_OWNER_NAME.equals(columnName)) {
			
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null) {
				return null;
			}
			return collateralConfig.getProcessingOrg().getCode();
			
		}else if (SANT_MCC_TYPE.equals(columnName)) {
			
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null) {
				return null;
			}
			return collateralConfig.getContractType();

		} else if (SANT_MCC_STATUS.equals(columnName)) {
			
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null) {
				return null;
			}
			return collateralConfig.getAgreementStatus();

		} else if (EXCLUDE_FROM_OPTIMIZER.equals(columnName)) {
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null) {
				return null;
			}
			return collateralConfig.isExcludeFromOptimizer();
		
			//GSM 17/08/2017 - Emir RTS9 - new column for UK IM CSD TYPE
		} else if (SANT_MCC_IM_TYPE.equals(columnName)) {
			
			CollateralConfig collateralConfig = getCollateralConfig(row);
			if (collateralConfig == null || Util.isEmpty(collateralConfig.getAdditionalField(IM_CSD_TYPE))) {
				return null;
			}
			return collateralConfig.getAdditionalField(IM_CSD_TYPE);
			
		} else if (CUSTOM_DISPUTE_AMOUNT.equals(columnName)) {
			MarginCallEntryDTO entry = getEntryDTO(row);
			if (entry.getDisputeAmount() == 0D) {
				double cptyAmount = entry.getCptyAmount();
				
				double globalRequiredMargin = entry.getGlobalRequiredMarginCalc();
				double disputeAmount = globalRequiredMargin - cptyAmount;
				
				return new Amount(disputeAmount, entry.getContractCurrency()); 
				
			} else {
				return new Amount(entry.getDisputeAmount(), entry.getContractCurrency()); 
			}
			
		} else {
			//try MarginCallEntryReportStyle column as generic
			Object valueCol = super.getColumnValue(row, columnName, errors);
			
			//GSM: 09/02/2016 Fix v14.- check if is another column type
			//check if is a isMarginCallConfigColumn
			if (valueCol == null) {
				//check is legal entity
				valueCol = getMarginCallConfigColumn(row, columnName, errors);
			}

			if (valueCol == null) {
				//check is legal entity
				valueCol = getLegalEntityColumn(row, columnName, errors);
			}

			// If the return value is null try to get it from the ExtensionColumns
			if (valueCol == null) {
				valueCol = this.getColumnValueFromExtensionReportStyle(getType(this.getClass()), row, columnName, errors);
			}
			
			return valueCol;
		}
			
			
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
		String name = getMarginCallConfigColumn(MARGIN_CALL_CONFIG_PREFIX, columnName);
		
		//check if is a mcconfig column
		if (!Util.isEmpty(name)){
			
			CollateralConfig config = getCollateralConfig(row);
		    if (config == null) {
		    	return null;
		    }
		    
		    row.setProperty("MarginCallConfig", config);
		    return getMarginCallConfigReportStyle().getColumnValue(row, name, errors);
		}
		return null;
	}

	/**
	 * 
	 * @param mc_prefix
	 * @param columnName
	 * @return real name of the column if is a MarginCAllConfig Column
	 */
	private String getMarginCallConfigColumn(String mc_prefix, String columnName) {

		CollateralConfigReportStyle configReportStyle = getMarginCallConfigReportStyle();
		String n = configReportStyle.getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);
		
		if (Util.isEmpty(n)){
			return null;
		}
		return n;
	}
	
	
	/**
	 * 
	 * @return LegalEntityReportStyle instance
	 */
	private LegalEntityReportStyle getLegalEntityStyle()
	   {
	     if (_leReportStyle == null) {
	       _leReportStyle = ReportStyle.getLegalEntityReportStyle();
	    }
	     return _leReportStyle;
	
	   }
	
	/**
	 * 
	 * @param row
	 * @param columnName
	 * @param errors
	 * @return value of Legal Entity if is a Legal Entity column
	 */
	private Object getLegalEntityColumn(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors){
		
		CollateralConfig config = getCollateralConfig(row);
	    if (config == null) {
	    	return null;
	    }
		
		for (String lePrefix : LEGAL_ENTITIES_PREFIXES) {

			LegalEntityReportStyle leReportStyle = getLegalEntityStyle();
			String prefix = lePrefix;
			if (leReportStyle.isLegalEntityColumn(prefix, columnName)) {
			 
				LegalEntity le = getLegalEntity(prefix, config);
			    if (le == null) {
			         return "";
			    }
			         return leReportStyle.getColumnValue(le, prefix, row, columnName, errors);
			    }
			}
			     
		return null;
	}
	
	/**
	 * 
	 * @param prefix
	 * @param config
	 * @return the PO or LE in the cm config based on the prefix
	 */
	protected static LegalEntity getLegalEntity(String prefix, CollateralConfig config) {
		
		if (prefix.equals("Legal Entity."))
			return config.getLegalEntity();
		if (prefix.equals("Processing Org.")) {
			return config.getProcessingOrg();
	    }
		return null;
	 }
	

	/**
	 * @param row
	 * @return MCEntryDTO
	 */
	private MarginCallEntryDTO getEntryDTO(ReportRow row) {
		MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty("Default");
		return entry;
	}

	/**
	 * @param row
	 * @return CollateralConfig
	 */
	private CollateralConfig getCollateralConfig(ReportRow row) {
		
		if (row.getProperty("MarginCallConfig") != null)
			return (CollateralConfig) row.getProperty("MarginCallConfig");
		
		final MarginCallEntryDTO entry = getEntryDTO(row);
		if (entry == null) {
			return null;
		}
		CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
				entry.getCollateralConfigId());
		return collateralConfig;
	}


	/**
	 * Recovers the tree list (columns for the style)
	 */
	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}
		final TreeList treeList = super.getTreeList();
		
		if (customCollateralConfigReportStyle == null){
			customCollateralConfigReportStyle = getCustomCollateralConfigReportStyle();
		}
		
		if (customCollateralConfigReportStyle != null){
			treeList.add(customCollateralConfigReportStyle.getNonInheritedTreeList());
		
		} else{
		
			if (coreCollateralConfigReportStyle == null) {
				coreCollateralConfigReportStyle = getCoreCollateralConfigReportStyle();
			}
			if (coreCollateralConfigReportStyle != null) {
				treeList.add(coreCollateralConfigReportStyle.getNonInheritedTreeList());
			}
		}
		return treeList;
	}
	
	
	/**
	 * @return custom CollateralConfigReportStyle
	 */
	protected calypsox.tk.report.CollateralConfigReportStyle getCustomCollateralConfigReportStyle() {
		try {
			if (this.customCollateralConfigReportStyle == null) {
				String className = "calypsox.tk.report.CollateralConfigReportStyle";

				this.customCollateralConfigReportStyle =  (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
						true, true);

			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		return this.customCollateralConfigReportStyle;
	}

	/**
	 * @return calypso core CollateralConfigReportStyle
	 */
	protected com.calypso.tk.report.CollateralConfigReportStyle getCoreCollateralConfigReportStyle() {
		try {
			if (this.coreCollateralConfigReportStyle == null) {
				String className = "com.calypso.tk.report.CollateralConfigReportStyle";

				this.coreCollateralConfigReportStyle =  (com.calypso.tk.report.CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
						true, true);

			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		return this.coreCollateralConfigReportStyle;
	}
}

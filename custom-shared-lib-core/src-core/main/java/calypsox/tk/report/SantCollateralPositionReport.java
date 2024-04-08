/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.SecurityTemplateHelper;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

/**
 * Collateral Positions report, including cash and securities (bonds and equities). Uses SantPositionReport
 * 
 * @author Guillermo Solano
 * @version 1.0 
 */
public class SantCollateralPositionReport extends SantPositionReport {

	
	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -5084123036648016323L;
	
	/**
	 * ID of the valuation agent
	 */
	private Integer valAgentId;
	/**
	 * Type of the Collateral Agreement
	 */
	private String agrType;
	/**
	 * Base currency of the Collateral Agreement
	 */
	private String baseCurrency;
	
	/**
	 * @param errors
	 * @return rows for both cash & securities
	 */
	@Override
	ReportRow[] buildBOPositionRows(Vector<String> errors) {

		// cash & securities report retrieved each one in a thread
	    SecurityPositionThread secThread = new SecurityPositionThread(errors);
		CashPositionThread cashThread = new CashPositionThread(errors);
		// start threads
		secThread.start();
		cashThread.start();

		// join: wait all threads till last one finishes
		try {
			secThread.join();
			cashThread.join();

		} catch (InterruptedException e) {
			Log.error(this, e);
		}

		// recover rows
		ReportRow[] secRows = secThread.getSecuritiesPositionsRows();
		ReportRow[] cashRows = cashThread.getCashPositionsRows();

		// concat rows
		ReportRow[] reportRows = new ReportRow[secRows.length + cashRows.length];
		System.arraycopy(secRows, 0, reportRows, 0, secRows.length);
		System.arraycopy(cashRows, 0, reportRows, secRows.length, cashRows.length);
		
		return reportRows;
	}

	/**
	 * @return true valuation agent or agreement type or base currency filters are required
	 */
	@Override
	boolean checkFilterRows() {

		valAgentId = (Integer) getReportTemplate().get(SantGenericTradeReportTemplate.VALUATION_AGENT);
		agrType = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		baseCurrency = (String) getReportTemplate().get(SantGenericTradeReportTemplate.BASE_CURRENCY);
		
		return (valAgentId != null || !Util.isEmpty(agrType) || !Util.isEmpty(baseCurrency));
	}


	/**
	 * @return true if valuation agent or agreement type or base currency filters applied
	 */
	@Override
	boolean filterRow(ReportRow row) {

		final Inventory pos = (Inventory) row.getProperty(ReportRow.INVENTORY);
		final MarginCallConfig mcConfig = (pos.getMarginCallConfigId() == 0) ? null : BOCache.getMarginCallConfig(
				DSConnection.getDefault(), pos.getMarginCallConfigId());
		
		if (mcConfig != null){
			//valuation agent
			/*ojo, en la 12 est? mal - No debe comprobarse con el ID pero es el comportamiento en la v12
			 * El combo deber?a mostrar el rango PARTY A, B, BOTH, NONE y comprobarlo con el contrato
			 */
			if (valAgentId != null && mcConfig.getProcessingOrg().getId() != valAgentId)
				return true;
			
			if (!Util.isEmpty(agrType) && !mcConfig.getContractType().equals(agrType))
				return true;
			
			//base currency
			if (!Util.isEmpty(baseCurrency) && !mcConfig.getCurrency().equals(baseCurrency))
				return true;
		
		} else {
			Log.error(this, "No marginCallConfig found in for BO position ");
		}
		return false;
	}

	/**
	 * @param template attributes for BOPosition 
	 */
	@Override
	void updateTemplate(ReportTemplate template) {

		template.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");
    	template.put(BOPositionReportTemplate.POSITION_DATE, "Trade");
    	template.put(BOPositionReportTemplate.POSITION_CLASS, "Margin_Call");
    	template.put(BOPositionReportTemplate.POSITION_TYPE, "Not settled,Actual");
    	template.put(BOPositionReportTemplate.POSITION_VALUE, "Nominal");
    	
    	template.put(BOPositionReportTemplate.AGGREGATION, "ProcessingOrg");
    	template.put(BOPositionReportTemplate.FILTER_ZERO, "true");
    	template.put(BOPositionReportTemplate.MOVE, "Balance");	
    	
    	String posStatus = (String) getReportTemplate().get(ALLOCATION_STATUS);
    	if (!Util.isEmpty(posStatus)){
    		String postype = "";
    		if (ALLOCATION_STATUS_HELD.equals(posStatus))
    			postype = BOPositionReport.ACTUAL;
    		else if (ALLOCATION_STATUS_IN_TRANSIT.equals(posStatus))
    			postype = BOPositionReport.FAILED;
    		
    		if (!Util.isEmpty(postype)){
    			template.put(BOPositionReportTemplate.POSITION_TYPE, postype);
    		}
    	}	
	}

}

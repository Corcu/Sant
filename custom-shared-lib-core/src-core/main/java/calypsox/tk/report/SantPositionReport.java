/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOCashPositionReport;
import com.calypso.tk.report.BOCashPositionReportTemplate;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;

/**
 * Positions report, calls BOPosition for both cash and securities or either just one.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * 
 */
public abstract class SantPositionReport extends SantReport {

	
	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -5084123036648016323L;
	
	/**
	 * Constants
	 */
	public final static String ALLOCATION_STATUS = "ALLOCATION_STATUS";
	public static final String ALLOCATION_ENTRY_PROPERTY = "ALLOCATION_ENTRY";
	public static final String REPORT_DATE_PROPERTY = "REPORT_DATE";
	public static final String ALLOCATION_STATUS_IN_TRANSIT = "In Transit";
	public static final String ALLOCATION_STATUS_HELD = "Held";
	
	/**
	 * Overrides the process: both cash & securities or just one
	 * @param errors
	 * @return rows 
	 */
	abstract ReportRow[] buildBOPositionRows(final Vector<String> errors);
	
	/**
	 * @return true if there is a filter not included in BOPosition & must be done by coding
	 */
	abstract boolean checkFilterRows();
	
	/**
	 * if checkFilterRows() is true, it will check these conditions
	 * @param r
	 * @return true if row must be excluded
	 */
	abstract boolean filterRow(ReportRow r);
	
	/**
	 * @param template with specific attributes
	 */
	abstract void updateTemplate(ReportTemplate template);
	

	/**
	 * Main logic
	 */
	@Override
	public ReportOutput loadReport(final Vector<String> errors) {
		
		final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

		if (getReportPanel() != null) {
			ReportTemplatePanel tempPanel = getReportPanel().getReportTemplatePanel();
			if (tempPanel != null) {
				tempPanel.setTemplate(getReportTemplate());
			}
		}

		/*
		 * abstract override method to build rows
		 */
		ReportRow[] reportRows = buildBOPositionRows(errors);
		List<ReportRow> rowsList = new ArrayList<ReportRow> (0);

		if ((reportRows != null) && (reportRows.length > 0)) {	
			/*
			 * Overrides abstract to check if code filter must be done
			 */
			boolean checkFilter = checkFilterRows();			
			rowsList = new ArrayList<ReportRow> (reportRows.length);
			
			for (int i = 0; i < reportRows.length; i++) {
				/*
				 * Override coding filter
				 */
				if (checkFilter){
					
					if (filterRow(reportRows[i]))
						continue;
				}
				
				
				reportRows[i].setProperty("PRICING_ENV", getPricingEnv());
				rowsList.add(reportRows[i]);
			}
		}
		/*
		 * If post-process is required, override next method
		 */
		rowsList = postProcessPositionRows(rowsList, errors);
		reportOutput.setRows(rowsList.toArray(new ReportRow[0]));
		return reportOutput;
	}
	

	/**
	 * @param rowsList
	 * @param errors
	 * @return List of rows after post-processing
	 */
	protected List<ReportRow> postProcessPositionRows(List<ReportRow> rowsList, Vector<String> errors) {
		// Override if postProcess is required
		return rowsList;
	}


	/**
	 * 
	 * Thread to run the SecurityPosition Report of the BOPosition
	 * 
	 */
	protected class SecurityPositionThread extends Thread {

		private final Vector<String> errors;
		private ReportRow[] secPosRows;

		SecurityPositionThread(Vector<String> errorsL) {
			this.errors = errorsL;
			this.secPosRows = null;
		}
		
		/**
		 * Main BOSecurity thread method
		 */
		@Override
		public void run() {

			BOSecurityPositionReportTemplate secPositionTemplate = buildBOSecurityPosTemplate();
				
			final JDate startDate = getStartDate(getReportTemplate(), getValDate());
			secPositionTemplate.put("StartDate",startDate.toString());
			
			final JDate endDate = getEndDate(getReportTemplate(), getValDate());
			secPositionTemplate.put("EndDate",endDate.toString());

			BOPositionReport secPositionReport = new BOSecurityPositionReport();
			secPositionReport.setPricingEnv(getPricingEnv());
			secPositionReport.setStartDate(getStartDate(getReportTemplate(), getValDate()));
			secPositionReport.setEndDate(getEndDate(getReportTemplate(), getValDate()));
			secPositionReport.setReportTemplate(secPositionTemplate);

			this.secPosRows = (((DefaultReportOutput) secPositionReport.load(this.errors)).getRows());
		}

		private BOSecurityPositionReportTemplate buildBOSecurityPosTemplate() {
			
			final BOSecurityPositionReportTemplate secPositionTemplate = new BOSecurityPositionReportTemplate();
			/*
			 * Overrides abstract method to incluide specific attributes to the security template
			 */
			updateTemplate(secPositionTemplate);
			
			secPositionTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");

        	//Collateral Config agreement IDs
        	final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        	if (!Util.isEmpty(agreementIds)) {
        		secPositionTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementIds);
        	}
        	// POs owners names to filter
        	final String poNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        	if (!Util.isEmpty(poNames)) {
        		secPositionTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, poNames);
        	}
        	
        	return secPositionTemplate;
		}

		public ReportRow[] getSecuritiesPositionsRows() {
			return this.secPosRows.clone();
		}
	}

	/**
	 * 
	 * Thread to run the CashPosition Report of the BOPosition
	 * 
	 */
	protected class CashPositionThread extends Thread {

		private final Vector<String> errors;
		private ReportRow[] cashPosRows;

		CashPositionThread(Vector<String> errorsL) {
			this.errors = errorsL;
			this.cashPosRows = null;
		}

		/**
		 * Main BOCash thread method
		 */
		@Override
		public void run() {

			ReportTemplate cashTemplate = buildBOCashPosTemplate();

			final JDate startDate = getStartDate(getReportTemplate(), getValDate());
			cashTemplate.put("StartDate",startDate.toString());
			
			final JDate endDate = getEndDate(getReportTemplate(), getValDate());
			cashTemplate.put("EndDate",endDate.toString());

			BOPositionReport cashPositionReport = new BOCashPositionReport();
			cashPositionReport.setStartDate(startDate);
			cashPositionReport.setEndDate(getEndDate(getReportTemplate(), getValDate()));
			cashPositionReport.setPricingEnv(getPricingEnv());
			
			cashPositionReport.setReportTemplate(cashTemplate);

			this.cashPosRows = (((DefaultReportOutput) cashPositionReport.load(this.errors)).getRows());
		}

		private ReportTemplate buildBOCashPosTemplate() {
			
			ReportTemplate cashTemplate = new BOCashPositionReportTemplate();
			/*
			 * Overrides abstract method to incluide specific attributes to the cash template
			 */
			updateTemplate(cashTemplate);
        	cashTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Cash");
    		
        	//Collateral Config agreement IDs
        	final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        	if (!Util.isEmpty(agreementIds)) {
        		cashTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementIds);
        	}
        
        	// POs owners names to filter
        	final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        	if (!Util.isEmpty(ownersNames)) {
        		cashTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, ownersNames);
        	}
        	return cashTemplate;
		}

		public ReportRow[] getCashPositionsRows() {
			return this.cashPosRows.clone();
		}
	}
	/**
	 * @param template
	 * @param valDate
	 * @return start date from template
	 */
	protected JDate getStartDate(ReportTemplate template, JDate valDate) {
		return getDate(template, valDate, TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
	}

	/**
	 * 
	 * @param template
	 * @param valDate
	 * @return end date from template
	 */
	protected JDate getEndDate(ReportTemplate template, JDate valDate) {
		return getDate(template, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
	}





}

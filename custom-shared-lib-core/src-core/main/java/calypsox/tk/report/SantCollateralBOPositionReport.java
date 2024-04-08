/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import org.jfree.util.Log;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;

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
import com.calypso.tk.report.SecurityTemplateHelper;
import com.calypso.tk.report.TradeReportTemplate;

/**
 * Collateral Positions report, including cash and securities (bonds and equities). This report uses BOPosition for
 * MARGIN_CALL positions. As this report is intended for notifications and is heavily use, each cash and securities
 * reports are within independent threads.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 30/04/2015
 * 
 */
public class SantCollateralBOPositionReport extends SantReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 4157510370796075401L;


	/**
	 * Report main load
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ReportOutput loadReport(final Vector errors) {

		final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

		if (getReportPanel() != null) {
			ReportTemplatePanel tempPanel = getReportPanel().getReportTemplatePanel();
			if (tempPanel != null) {
				tempPanel.setTemplate(getReportTemplate());
			}
		}

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

		if ((reportRows != null) && (reportRows.length > 0)) {
			for (int i = 0; i < reportRows.length; i++) {
				reportRows[i].setProperty("PRICING_ENV", getPricingEnv());
			}
		}

		reportOutput.setRows(reportRows);
		return reportOutput;
	}

protected BOPositionReport buildSecurityTemplate(BOSecurityPositionReportTemplate secPositionTemplate){

	secPositionTemplate.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, "Trade");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, "Not settled,Actual");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
	secPositionTemplate.put(BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");
	secPositionTemplate.put(BOPositionReportTemplate.FILTER_ZERO, "true");
	secPositionTemplate.put(BOPositionReportTemplate.MOVE, "Balance");
	final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
	if (!Util.isEmpty(agreementId)) {
		secPositionTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
	}
	// GSM 30/07/15. SBNA Multi-PO filter
	final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
	// final String ownersNames = (String)
	// getReportTemplate().get(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
	if (!Util.isEmpty(ownersNames)) {
		secPositionTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, ownersNames);
	}

	BOPositionReport secPositionReport = new BOSecurityPositionReport();
	secPositionReport.setReportTemplate(secPositionTemplate);
	secPositionReport.setPricingEnv(getPricingEnv());
	secPositionReport.setStartDate(getStartDate(getReportTemplate(), getValDate()));
	secPositionReport.setEndDate(getEndDate(getReportTemplate(), getValDate()));

	return secPositionReport;
}

	protected BOPositionReport buildCashTemplate(BOCashPositionReportTemplate cashTemplate){

		cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, "Trade");
		cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");
		cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, "Not settled,Actual");
		cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
		cashTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Cash");
		cashTemplate.put(BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");
		cashTemplate.put(BOPositionReportTemplate.FILTER_ZERO, "true");
		cashTemplate.put(BOPositionReportTemplate.MOVE, "Balance");
		final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
		if (!Util.isEmpty(agreementId)) {
			cashTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
		}

		// GSM 20/07/15. SBNA Multi-PO filter
		final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
		// final String ownersNames = (String) getReportTemplate().get(
		// SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
		if (!Util.isEmpty(ownersNames)) {
			cashTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, ownersNames);
		}

		BOPositionReport cashPositionReport = new BOCashPositionReport();
		cashPositionReport.setPricingEnv(getPricingEnv());
		cashPositionReport.setStartDate(getStartDate(getReportTemplate(), getValDate()));
		cashPositionReport.setEndDate(getEndDate(getReportTemplate(), getValDate()));
		cashPositionReport.setReportTemplate(cashTemplate);

		return cashPositionReport;
	}

	/**
	 * 
	 * Thread to run the SecurityPosition Report of the BOPosition
	 * 
	 */
	private class SecurityPositionThread extends Thread {

		private final Vector<String> errors;
		private ReportRow[] secPosRows;
		BOSecurityPositionReportTemplate secPositionTemplate;
		SecurityPositionThread(Vector<String> errorsL) {
			this.errors = errorsL;
			this.secPosRows = null;
			secPositionTemplate = new BOSecurityPositionReportTemplate();

		}

		@Override
		public void run() {
			BOPositionReport secPositionReport = buildSecurityTemplate(secPositionTemplate);
			this.secPosRows = (((DefaultReportOutput) secPositionReport.load(this.errors)).getRows());
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
	private class CashPositionThread extends Thread {

		private final Vector<String> errors;
		private ReportRow[] cashPosRows;
		BOCashPositionReportTemplate cashPositionTemplate;

		CashPositionThread(Vector<String> errorsL) {
			this.errors = errorsL;
			this.cashPosRows = null;
			cashPositionTemplate = new BOCashPositionReportTemplate();
		}

		@Override
		public void run() {
			BOPositionReport cashPositionReport = buildCashTemplate(cashPositionTemplate);
			this.cashPosRows = (((DefaultReportOutput) cashPositionReport.load(this.errors)).getRows());
		}

		public ReportRow[] getCashPositionsRows() {
			return this.cashPosRows.clone();
		}
	}

	/**
	 * 
	 * @param template
	 * @param valDate
	 * @return start date
	 */
	protected JDate getStartDate(ReportTemplate template, JDate valDate) {
		return getDate(template, valDate, TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
	}

	/**
	 * 
	 * @param template
	 * @param valDate
	 * @return end date
	 */
	protected JDate getEndDate(ReportTemplate template, JDate valDate) {
		return getDate(template, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.TradeReportTemplate;

import java.util.TimeZone;
import java.util.Vector;

public abstract class SantReport extends Report {

	private static final long serialVersionUID = 5274140116352528587L;

	private JDate processStartDate = null;

	private JDate processEndDate = null;

	private JDate customProcessStartDate = null;

	private JDate customProcessEndDate = null;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		
		StringBuffer error = new StringBuffer();

		if (checkProcessStartDate()) {
			computeStartDate(errorMsgs);
		}
		if (checkProcessEndDate()) {
			computeEndDate(errorMsgs);
		}

		if (checkCustomProcessStartDate()) {
			computeCustomStartDate(errorMsgs);
		}
		if (checkCustomProcessEndDate()) {
			computeCustomEndDate(errorMsgs);
		}

		if (errorMsgs.isEmpty()) {
			
			try {
			return loadReport(errorMsgs);
			
			} catch (OutOfMemoryError e2) {
				error.append("Not enough memory to run this report.\n");
			 	Log.error(this, e2);//Sonar
			} catch (Exception e3){
				error.append("Error generating SantReport.\n");
				error.append(e3.getLocalizedMessage());	
				Log.error(this, e3);//Sonar
			}	
		}
		
		Log.error(this, error.toString());
		errorMsgs.add(error.toString());
		return null;
	}
	
	
	//to override
	protected abstract ReportOutput loadReport(Vector<String> errorMsgs);
	

	private void computeStartDate(Vector<String> errorMsgs) {
		this.processStartDate = null;
		this.processStartDate = getDate(getReportTemplate(), getValuationDatetime().getJDate(TimeZone.getDefault()),
				TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
		if (this.processStartDate == null) {
			errorMsgs.add("Start Date cannot be empty.");
		}
	}

	private void computeEndDate(Vector<String> errorMsgs) {
		this.processEndDate = null;
		this.processEndDate = getDate(getReportTemplate(), getValuationDatetime().getJDate(TimeZone.getDefault()),
				TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);

		if (this.processEndDate == null) {
			errorMsgs.add("End Date cannot be empty.");
		}
	}

	private void computeCustomStartDate(Vector<String> errorMsgs) {
		this.customProcessStartDate = null;
		this.customProcessStartDate = getDate(getReportTemplate(), getValuationDatetime().getJDate(TimeZone.getDefault()),
				TradeReportTemplate.PROCESS_START_DATE, TradeReportTemplate.PROCESS_START_PLUS,
				TradeReportTemplate.PROCESS_START_TENOR);
		if (this.customProcessStartDate == null) {
			errorMsgs.add(getCustomProcessDateName() + " Start Date cannot be empty.");
		}
	}

	private void computeCustomEndDate(Vector<String> errorMsgs) {
		this.customProcessEndDate = null;
		this.customProcessEndDate = getDate(getReportTemplate(), getValuationDatetime().getJDate(TimeZone.getDefault()),
				TradeReportTemplate.PROCESS_END_DATE, TradeReportTemplate.PROCESS_END_PLUS,
				TradeReportTemplate.PROCESS_END_TENOR);

		if (this.customProcessEndDate == null) {
			errorMsgs.add(getCustomProcessDateName() + " End Date cannot be empty.");
		}
	}

	protected boolean checkProcessStartDate() {
		return true;
	}

	protected boolean checkProcessEndDate() {
		return true;
	}

	protected JDate getProcessStartDate() {
		return this.processStartDate;
	}

	protected JDate getProcessEndDate() {
		return this.processEndDate;
	}

	protected boolean checkCustomProcessStartDate() {
		return false;
	}

	protected boolean checkCustomProcessEndDate() {
		return false;
	}

	protected JDate getCustomProcessStartDate() {
		return this.customProcessStartDate;
	}

	protected JDate getCustomProcessEndDate() {
		return this.customProcessEndDate;
	}

	/**
	 * To be overridden when used to match the name in the report
	 * 
	 * @return
	 */
	protected String getCustomProcessDateName() {
		return "Custom";
	}
}

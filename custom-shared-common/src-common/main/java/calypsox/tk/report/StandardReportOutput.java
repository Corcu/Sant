/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag-management.com) 
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportView;
import com.calypso.tk.report.ReportViewer;

public class StandardReportOutput extends DefaultReportOutput {

	private static final long serialVersionUID = 2101690035656999556L;

	private String delimiteur;
	/**
	 * True for show headings in first line
	 */
	private boolean showHeadings = false;

	public StandardReportOutput(Report report) {
		super(report);
	}

	public String getDelimiteur() {
		return this.delimiteur;
	}

	public void setDelimiteur(String delimiteur) {
		this.delimiteur = delimiteur;
	}

	public boolean getShowHeadings() {
		return this.showHeadings;
	}

	public void setShowHeadings(boolean showHeadings) {
		this.showHeadings = showHeadings;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void format(ReportViewer reportViewer, ReportView view, Vector errors, boolean forceInit,
			boolean skipDataExtraction) {
		if ((reportViewer instanceof CSVReportViewer) && (this.delimiteur != null) && !this.delimiteur.equals("")) {
			((CSVReportViewer) reportViewer).setDelimiter(this.delimiteur);
			this.delimiteur = null;
		}

		if (reportViewer instanceof CSVReportViewer) {
			((CSVReportViewer) reportViewer).setShowHeadings(this.showHeadings);
		}
		if (errors == null) {
			errors = new Vector<String>();
		}
		super.format(reportViewer, view, errors, forceInit, skipDataExtraction);
	}

	@Override
	public void format(ReportViewer viewer) {
		this.format(viewer, null, null, true, false);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void format(ReportViewer viewer, Vector errors) {
		this.format(viewer, null, errors, true, false);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void format(ReportViewer viewer, ReportView view, Vector errors, boolean forceInit) {
		this.format(viewer, view, errors, forceInit, false);
	}
}

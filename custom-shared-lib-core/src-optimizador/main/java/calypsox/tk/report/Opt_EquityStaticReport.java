/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.EquityReport;
import com.calypso.tk.report.ReportOutput;

public class Opt_EquityStaticReport extends EquityReport {

	private static final long serialVersionUID = 3343834695450789373L;

	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {

		StandardReportOutput output = new StandardReportOutput(this);
		output.setRows(((DefaultReportOutput) super.load(errorMsgs)).getRows());
		return output;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;

import java.util.HashMap;
import java.util.Vector;

public class MarginCallEntryReport extends com.calypso.tk.report.MarginCallEntryReport implements CheckRowsNumberReport {

	@Override
	public ReportOutput load(Vector errorMsgs) {

		DefaultReportOutput output= (DefaultReportOutput)super.load(errorMsgs);

		//Generate a task is the report size is out of a defined umbral
		HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
		if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")){
			checkAndGenerateTaskReport(output, value);
		}

		return output;
	}
}
/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantKPIDailyTaskReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -882951624027492640L;

	@Override
	public void setDefaults() {
		setColumns(SantKPIDailyTaskReportStyle.DEFAULT_COLUMNS);
	}

}

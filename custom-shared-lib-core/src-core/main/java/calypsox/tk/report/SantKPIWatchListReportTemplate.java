/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantKPIWatchListReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -1643310920869862073L;

	@Override
	public void setDefaults() {
		setColumns(SantKPIWatchListReportStyle.DEFAULT_COLUMNS);
	}
}

package calypsox.tk.report;

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

import com.calypso.tk.report.ReportTemplate;

public class SantAllDealsReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 123L;

	@Override
	public void setDefaults() {
		setColumns(SantAllDealsReportStyle.DEFAULT_COLUMNS);
	}

}

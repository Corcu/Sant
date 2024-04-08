package calypsox.tk.report;

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

import com.calypso.tk.report.ReportTemplate;

public class SantDelinquentMarginCallReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -1867993986993021436L;

	@Override
	public void setDefaults() {
		setColumns(SantDelinquentMarginCallReportStyle.DEFAULT_COLUMNS);
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class PortfolioReconciliationReportTemplate extends ReportTemplate {

	/* generated UID */
	private static final long serialVersionUID = 6146206276733775327L;

	/**
	 * Set de default columns
	 */
	@Override
	public void setDefaults() {
		super.setDefaults();
		super.setColumns(PortfolioReconciliationReportStyle.DEFAULTS_COLUMNS);

	}

}

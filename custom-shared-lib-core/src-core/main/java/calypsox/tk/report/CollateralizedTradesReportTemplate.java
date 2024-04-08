/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class CollateralizedTradesReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -267406287362116712L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		super.setColumns(CollateralizedTradesReportStyle.DEFAULTS_COLUMNS);

	}
}

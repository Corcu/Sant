/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.AccountReportTemplate;

public class Opt_ReferenceRateReportTemplate extends AccountReportTemplate {

	private static final long serialVersionUID = 123L;

	/**
	 * Default columns
	 */
	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_ReferenceRateReportStyle.DEFAULTS_COLUMNS);
	}

}

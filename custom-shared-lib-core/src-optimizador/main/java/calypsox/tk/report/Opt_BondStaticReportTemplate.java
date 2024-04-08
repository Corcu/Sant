/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.BondReportTemplate;

public class Opt_BondStaticReportTemplate extends BondReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_BondStaticReportStyle.DEFAULTS_COLUMNS);
	}

}

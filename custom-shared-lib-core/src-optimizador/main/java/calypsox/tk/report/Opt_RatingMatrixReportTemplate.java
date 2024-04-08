/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.CollateralConfigReportTemplate;


public class Opt_RatingMatrixReportTemplate extends CollateralConfigReportTemplate {

	private static final long serialVersionUID = 123L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_RatingMatrixReportStyle.DEFAULTS_COLUMNS);		
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

@SuppressWarnings("serial")
public class SantListFxOptionReportTemplate extends ReportTemplate {

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantListFxOptionReportStyle.DEFAULTS_COLUMNS);
	}
}

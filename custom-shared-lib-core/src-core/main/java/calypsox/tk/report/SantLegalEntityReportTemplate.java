/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

public class SantLegalEntityReportTemplate extends com.calypso.tk.report.LegalEntityReportTemplate {

	private static final long serialVersionUID = 1L;
	// columns
	public static final String ISO_COUNTRY = "ISO Country";

	private static final String[] DEFAULT_COLUMNS = { ISO_COUNTRY };

	@Override
	public void setDefaults() {
		super.setDefaults();
		super.setColumns(DEFAULT_COLUMNS);
	}
}

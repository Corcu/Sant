/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantAuditReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -227130654814283998L;

	public static String FROM_DATE = "FROM_DATE";
	public static String TO_DATE = "TO_DATE";
	public static String PO = "PO";
	public static String CPTY = "CPTY";
	public static String CONTRACT_TYPE = "CONTRACT_TYPE";
	public static String BASE_CCY = "BASE_CCY";
	public static String HEAD_CLONE = "HEAD_CLONE";
	public static String HEDGE_FUND = "HEDGE_FUND";
	public static String PO_COLLAT_TYPE = "PO_COLLAT_TYPE";
	public static String CPTY_COLLAT_TYPE = "CPTY_COLLAT_TYPE";
	public static String INSTRUMENT_TYPE = "INSTRUMENT_TYPE";

	@Override
	public void setDefaults() {
		setColumns(SantAuditReportStyle.DEFAULT_COLUMNS);
	}

}

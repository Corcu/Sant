/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantInterestNotificationReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 5840326598849285054L;

	public static final String ROW_DATA = "SantInterestNotificationEntry";

	@Override
	public void setDefaults() {
		setColumns(SantInterestNotificationReportStyle.DEFAULTS_COLUMNS);
	}

	public static final String CALL_ACCOUNT_ID = "CALL_ACCOUNT_ID";
	public static final String AGREEMENT_ID = "AGREEMENT_ID";
	public static final String AGREEMENT_TYPE = "AGREEMENT_TYPE";
	public static final String RATE_INDEX = "RATE_INDEX";
	public static final String CURRENCY = "CURRENCY";
	public static final String OWNER_AGR = "OWNER_AGR";

	public static final String CONTRACT_MAP = "CONTRACT_MAP";
	public static final String ACCOUNT_MAP = "ACCOUNT_MAP";

}

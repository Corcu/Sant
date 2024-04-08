/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantIntragroupPortfolioBreakdownReportTemplate extends SantGenericTradeReportTemplate {

	private static final long serialVersionUID = 123L;

	public static final String TRADE_WRAPPER = "PrincipalWrapper";

	public static final String MTM_WRAPPER_VAL_DATE = "MtmWrapperValDate";

	public static final String MTM_WRAPPER_PREVIOUS_DAY = "MtmWrapperValDatePreviousDay";

	// Specific to send notif by email
	public static final String TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF = "EMAILGatewayMCNOTIFDocumentSender";

	public static final String INTRAGROUP_LE_ID = "INTRAGROUP_LE_ID";

}

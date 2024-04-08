/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantPortfolioBreakdownReportTemplate extends SantGenericTradeReportTemplate {

	private static final long serialVersionUID = 4746515878730029177L;

	public static final String TRADE_WRAPPER = "PrincipalWrapper";

	public static final String MTM_WRAPPER_VAL_DATE = "MtmWrapperValDate";

	public static final String MTM_WRAPPER_PREVIOUS_DAY = "MtmWrapperValDatePreviousDay";

	// Specific to send notif by email
	public static final String TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF = "EMAILGatewayMCNOTIFDocumentSender";

}

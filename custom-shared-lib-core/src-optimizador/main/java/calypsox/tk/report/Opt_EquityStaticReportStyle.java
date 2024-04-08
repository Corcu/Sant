/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

public class Opt_EquityStaticReportStyle extends EquityReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { CURRENCY, COUNTRY, EXCHANGE, NAME, CORPORATE,
			PRODUCT_CODE_PREFIX + "ISIN", ISSUER, PRODUCT_SUBTYPE, QUOTE_TYPE, NEXT_DIVIDEND_RECORD_DATE,
			NEXT_DIVIDEND_DATE };

}

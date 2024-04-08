/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

/**
 * Enum fields for Collateral Value Message for EMIR report.
 * 
 * @author xIS16241
 * 
 */
public enum SantEmirCVMColumns {
	COMMENT,
	VERSION,
	MESSAGETYPE,
	MESSAGEID,
	ACTION,
	LEIPREFIX,
	LEIVALUE,
	TRADEPARTYPREF1,
	TRADEPARTYVAL1,
	EXECUTIONAGENTPARTY1PREFIX,
	EXECUTIONAGENTPARTYVALUE1,
	COLLATERALPORTFOLIOCODE,
	COLLATERALPORTFOLIOINDICATOR,
	VALUEOFTHECOLLATERAL,
	CURRENCYCOLLATERALVALUE,
	COLLATERALVALUATIONDATETIME,
	COLLATERALREPORTINGDATE,
	SENDTO,
	PARTYREPOBLIGATION1;

	/**
	 * Get columns method for SantEmirCVM report.
	 * 
	 * @return columns
	 */
	public static String[] getColumns() {
		final SantEmirCVMColumns[] values = SantEmirCVMColumns.values();
		final String[] result = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i].toString();
		}
		return result;
	}
}

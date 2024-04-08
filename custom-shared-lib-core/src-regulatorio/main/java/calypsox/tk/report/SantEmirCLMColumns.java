/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

/**
 * Enum fields for Collateral Linking Message for EMIR report.
 * 
 * @author xIS16241
 * 
 */
public enum SantEmirCLMColumns {
	COMMENT, VERSION, MESSAGETYPE, MESSAGEID, ACTION, LEIPREFIX, LEIVALUE, TRADEPARTYPREF1,
	// TRADEPARTYVALUE,
	TRADEPARTYVAL1,
	EXECUTIONAGENTPARTY1PREFIX,
	EXECUTIONAGENTPARTYVALUE1,
	UTIPREFIX,
	UTI,
	USIPREFIX,
	USIVALUE,
	TRADEPARTYTRANSACTIONID1,
	COLLATERALPORTFOLIOCODE,
	COLLATERALIZED,
	SENDTO,
	PARTYREPOBLIGATION1,
	ACTIVITY,
	LEIPREFIXnew,
	LEIVALUEnew,
	TRADEPARTYPREF1new,
	TRADEPARTYVAL1new;

	/**
	 * Get columns method for SantEmirCLM report.
	 * 
	 * @return columns
	 */
	public static String[] getColumns() {
		final SantEmirCLMColumns[] values = SantEmirCLMColumns.values();
		final String[] result = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i].toString();
		}
		return result;
	}
}

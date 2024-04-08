/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

/**
 * Enum class Collateral Value Message for EMIR report
 * 
 * @author xIS16241
 * 
 */
public enum SantEmirCVMReportTemplate {
	EXTERNAL_ID, SOURCE_SYSTEM, MESSAGE_TYPE, ACTIVITY, TRANSACTION_TYPE, PRODUCT, TAG, VALUE;

	/**
	 * Get columns method for emir reports
	 * 
	 * @return columns
	 */
	public static String[] getColumns() {
		final SantEmirCVMReportTemplate[] values = SantEmirCVMReportTemplate.values();
		final String[] result = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i].toString();
		}
		return result;
	}
}

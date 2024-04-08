/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

public class SantCashPositionReportTemplate extends SantCollateralPositionReportTemplate {

	private static final long serialVersionUID = -3747895919167950591L;

	private static final String COLUMN_PREFIX_CASH_POSITION = "SantCashPosition.";

	// Custom Cash Position Report columns
	public static final String COLUMN_NAME_DATE = COLUMN_PREFIX_CASH_POSITION + "Date";
	public static final String COLUMN_NAME_CALL_ACCOUNT = COLUMN_PREFIX_CASH_POSITION + "Call Account";
	public static final String COLUMN_NAME_AMOUNT = COLUMN_PREFIX_CASH_POSITION + "Amount";
	public static final String COLUMN_NAME_POSITION_TYPE = COLUMN_PREFIX_CASH_POSITION + "Position Type";
	public static final String COLUMN_NAME_AGENT = COLUMN_PREFIX_CASH_POSITION + "Agent";
	public static final String COLUMN_NAME_BOOK = COLUMN_PREFIX_CASH_POSITION + "Book";

	private static final String[] DEFAULT_COLUMNS = { COLUMN_NAME_DATE, COLUMN_NAME_BOOK,
			BOCashPositionReportStyle.CURRENCY, COLUMN_NAME_AGENT, BOCashPositionReportStyle.AGENT_NAME,
			COLUMN_NAME_CALL_ACCOUNT, SantCollateralPositionReportStyle.POS_STATUS, COLUMN_NAME_AMOUNT };

	@Override
	public void setDefaults() {
		setColumns(DEFAULT_COLUMNS);
	}
}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

@SuppressWarnings("serial")
public class SantListFxOptionReportStyle extends ReportStyle {

	public static final String BOOK = "Book";
	public static final String COUNTERPARTY = "Counterparty";
	public static final String EXTERNAL_REF = "External Reference";
	public static final String GIVE_RECEIVE = "Give/Receive";
	public static final String PRODUCT_TYPE = "Product type";
	public static final String UNDERLYING = "Underlying";
	public static final String VALUE_DATE = "Value date";
	public static final String PO = "Procesing Org";
	public static final String PRINCIPAL = "Principal";
	public static final String PRINCIPAL_CCY = "Principal Currency";
	public static final String MATURITY_DATE = "Maturity date";
	public static final String TRADE_DATE = "Trade date";
	public static final String SOURCE_SYSTEM = "Source System";
	public static final String EXTERNAL_REF_2 = "External Reference FX Option";
	public static final String SOURCE_SYSTEM_2 = "Source System 2";
	public static final String UNDERLYING_TYPE = "Underlying Type";
	public static final String CALL_PUT = "Call/Put";
	public static final String MTM_TRADE = "MtM trade";

	/*
	 * New fields for the report
	 * 
	 * 
	 * TRADE DATE SOURCESYSTEM EXTERNALREF2 SOURCESYSTEM2 UNDERLYING_TYPE CALL PUT MTM OF THE TRADE
	 */

	public static final String[] DEFAULTS_COLUMNS = { BOOK, COUNTERPARTY, EXTERNAL_REF, GIVE_RECEIVE, PRODUCT_TYPE,
			UNDERLYING, VALUE_DATE, PO, PRINCIPAL, PRINCIPAL_CCY, MATURITY_DATE, TRADE_DATE, SOURCE_SYSTEM,
			EXTERNAL_REF_2, SOURCE_SYSTEM_2, UNDERLYING_TYPE, CALL_PUT, MTM_TRADE };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		if (BOOK.equals(columnName)) {
			return row.getProperty(BOOK);
		}

		if (COUNTERPARTY.equals(columnName)) {
			return row.getProperty(COUNTERPARTY);
		}

		if (EXTERNAL_REF.equals(columnName)) {
			return row.getProperty(EXTERNAL_REF);
		}

		if (GIVE_RECEIVE.equals(columnName)) {
			return row.getProperty(GIVE_RECEIVE);
		}

		if (PRODUCT_TYPE.equals(columnName)) {
			return row.getProperty(PRODUCT_TYPE);
		}

		if (UNDERLYING.equals(columnName)) {
			return row.getProperty(UNDERLYING);
		}

		if (VALUE_DATE.equals(columnName)) {
			return row.getProperty(VALUE_DATE);
		}

		if (PO.equals(columnName)) {
			return row.getProperty(PO);
		}

		if (PRINCIPAL.equals(columnName)) {
			return row.getProperty(PRINCIPAL);
		}

		if (PRINCIPAL_CCY.equals(columnName)) {
			return row.getProperty(PRINCIPAL_CCY);
		}

		if (MATURITY_DATE.equals(columnName)) {
			return row.getProperty(MATURITY_DATE);
		}

		if (TRADE_DATE.equals(columnName)) {
			return row.getProperty(TRADE_DATE);
		}

		if (SOURCE_SYSTEM.equals(columnName)) {
			return row.getProperty(SOURCE_SYSTEM);
		}

		if (EXTERNAL_REF_2.equals(columnName)) {
			return row.getProperty(EXTERNAL_REF_2);
		}

		if (SOURCE_SYSTEM_2.equals(columnName)) {
			return row.getProperty(SOURCE_SYSTEM_2);
		}

		if (UNDERLYING_TYPE.equals(columnName)) {
			return row.getProperty(UNDERLYING_TYPE);
		}

		if (CALL_PUT.equals(columnName)) {
			return row.getProperty(CALL_PUT);
		}

		if (MTM_TRADE.equals(columnName)) {
			return row.getProperty(MTM_TRADE);
		}

		return null;
	}
}

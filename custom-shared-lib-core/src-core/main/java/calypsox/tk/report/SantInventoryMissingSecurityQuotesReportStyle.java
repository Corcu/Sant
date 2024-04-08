/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.ReportTemplate;

public class SantInventoryMissingSecurityQuotesReportStyle extends ReportStyle {

	private static final long serialVersionUID = 6881928559332741996L;

	public static final String PRODUCT_ID = "Product Id";
	public static final String ISIN = "Isin";
	public static final String CURRENCY = "Currency";
	public static final String PRODUCT_TYPE = "Product Type";
	public static final String PRICING_ENV = "Pricing Env";
	public static final String VALUE_DATE = "Value Date";
	public static final String QUOTE_NAME = "Quote Name";
	public static final String BALANCE = "Balance";

	public static final String[] DEFAULT_COLUMNS = { PRODUCT_ID, ISIN, CURRENCY, PRODUCT_TYPE, PRICING_ENV, VALUE_DATE,
			QUOTE_NAME, BALANCE };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

		InventorySecurityPosition pos = (InventorySecurityPosition) row
				.getProperty(SantInventoryMissingSecurityQuotesReportTemplate.INVENTORY_MISSING_SECURITY);

		if (pos == null) {
			throw new InvalidParameterException("Invalid row " + row + ". Cannot locate Inventory object");
		}

		if (PRODUCT_ID.equals(columnId)) {
			return pos.getSecurityId();
		} else if (ISIN.equals(columnId)) {
			return pos.getProduct().getSecCode("ISIN");
		} else if (CURRENCY.equals(columnId)) {
			return pos.getSettleCurrency();
		} else if (PRODUCT_TYPE.equals(columnId)) {
			return pos.getProduct().getType();
		} else if (PRICING_ENV.equals(columnId)) {
			return row.getProperty(SantInventoryMissingSecurityQuotesReportTemplate.PRICING_ENV);
		} else if (VALUE_DATE.equals(columnId)) {
			return row.getProperty(SantInventoryMissingSecurityQuotesReportStyle.VALUE_DATE);
		} else if (QUOTE_NAME.equals(columnId)) {
			return pos.getProduct().getQuoteName();
		} else if (BALANCE.equals(columnId)) {
			return pos.getTotal();
		}

		return null;
	}

	@Override
	public boolean containsPricingEnvDependentColumns(ReportTemplate template) {
		return true;
	}

}

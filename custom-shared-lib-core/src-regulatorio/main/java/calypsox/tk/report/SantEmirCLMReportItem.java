/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.calypso.tk.core.Trade;

/**
 * SantEmirCLMReportItem contains all values for one report's row.
 * 
 * @author xIS16241
 * 
 */
public class SantEmirCLMReportItem {

	/**
	 * trade
	 */
	private Trade trade;

	/** The values. */
	protected final Map<String, Object> values;

	/**
	 * ID_SantEmirCLMReportStyle key.
	 */
	public static final String ID_SantEmirCLMReportItem = "ID_SantEmirCLMReportItem";

	/**
	 * Instantiates a new generic report item.
	 */
	public SantEmirCLMReportItem(Trade trade) {
		this.values = new HashMap<String, Object>();
		this.trade = trade;
	}

	/**
	 * Gets the column value.
	 * 
	 * @param fieldName
	 *            the field name
	 * @return the field value
	 */
	public Object getFieldValue(final String fieldName) {
		return this.values.get(fieldName);
	}

	/**
	 * Sets the field value.
	 * 
	 * @param fieldName
	 *            the column name
	 * @param value
	 *            the value
	 */
	public void setFieldValue(final String fieldName, final Object value) {
		this.values.put(fieldName, value);
	}

	/**
	 * get the field names
	 * 
	 * @return field names
	 */
	public Set<String> getFieldNames() {
		return this.values.keySet();
	}

	/**
	 * getter for tradeId
	 * 
	 * @return trade id
	 */
	public Trade getTrade() {
		return this.trade;
	}

	/**
	 * setter for tradeId
	 * 
	 * @param tradeId
	 *            trade id
	 */
	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

}

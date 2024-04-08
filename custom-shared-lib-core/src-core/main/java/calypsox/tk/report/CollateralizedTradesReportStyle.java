/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_OUTPUT_FIELDS;

public class CollateralizedTradesReportStyle extends ReportStyle {

	/** serial uid */
	private static final long serialVersionUID = 8904353131394595195L;

	public static final String[] DEFAULTS_COLUMNS = { DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL.getFieldName(),
			DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE.getFieldName(), DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM.getFieldName(),
			DFA_OUTPUT_FIELDS.VALUE_DATE.getFieldName(), DFA_OUTPUT_FIELDS.COLLATERAL_PROCESSING_DATE.getFieldName(),
			DFA_OUTPUT_FIELDS.COLLATERAL_VALUATION_DATE.getFieldName(),
			DFA_OUTPUT_FIELDS.COLLATERAL_NAME.getFieldName(), DFA_OUTPUT_FIELDS.COLLATERAL_TYPE.getFieldName(),
			DFA_OUTPUT_FIELDS.COLLATERAL_BASE_CURRENCY.getFieldName(),
			DFA_OUTPUT_FIELDS.COLLATERAL_OWNER.getFieldName(), DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE.getFieldName(),
			DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE.getFieldName(), DFA_OUTPUT_FIELDS.PRODUCT_TYPE.getFieldName(),
			DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION.getFieldName() };

	/**
	 * Generates cell by cell, the report output
	 */
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {

		@SuppressWarnings("unchecked")
		final Map<DFA_OUTPUT_FIELDS, String> collateralTrade = (Map<DFA_OUTPUT_FIELDS, String>) row
				.getProperty(CollateralizedTradesReport.ROW_PROPERTY_ID);

		if (DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL);

		} else if (DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE);

		} else if (DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM);

		} else if (DFA_OUTPUT_FIELDS.VALUE_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.VALUE_DATE);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_PROCESSING_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_PROCESSING_DATE);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_VALUATION_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_VALUATION_DATE);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_NAME.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_NAME);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_TYPE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_TYPE);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_BASE_CURRENCY.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_BASE_CURRENCY);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_OWNER.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_OWNER);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE);

		} else if (DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE);

		} else if (DFA_OUTPUT_FIELDS.PRODUCT_TYPE.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.PRODUCT_TYPE);

		} else if (DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION.getFieldName().equals(columnName)) {
			return collateralTrade.get(DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION);
		}

		return null; // Should NOT happen
	}

}

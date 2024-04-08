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

import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS;

public class PortfolioReconciliationReportStyle extends ReportStyle {

	/* SERIAL UID */
	private static final long serialVersionUID = 922354110336600618L;

	// default columns
	public static final String[] DEFAULTS_COLUMNS = {
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_AGREEMENT.getFieldName(), // 31
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_TYPE.getFieldName(), // 2
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COUNTERPARTY.getFieldName(), // 3
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_ID.getFieldName(), // 4
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EXTERNAL_REFERENCE.getFieldName(), // 5 FO
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.CLOSE_OF_BUSINESS.getFieldName(), // 6 valuation
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.STRUCTURE.getFieldName(), // 7
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_DATE.getFieldName(), // 8
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_VALUE_DATE.getFieldName(), // 9
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_MATURITY_DATE.getFieldName(), // 10
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.VALUATION_AGENT.getFieldName(), // 11
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PORTFOLIO.getFieldName(), // 12
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.OWNER.getFieldName(), // 13
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DEAL_OWNER.getFieldName(), // 14
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.INSTRUMENT.getFieldName(), // 15
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.UNDERLYING.getFieldName(), // 16
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY.getFieldName(), // 17
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL.getFieldName(), // 18
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2_CCY.getFieldName(), // 19
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2.getFieldName(), // 20
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.IND_AMOUNT.getFieldName(), // 21
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE.getFieldName(), // 22
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE_2.getFieldName(), // 23
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BUY_SELL.getFieldName(), // 24
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BASE_CCY.getFieldName(), // 25
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.MTM_BASE_CCY.getFieldName(), // 26
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.USI.getFieldName(), // 27
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.SD_MSP.getFieldName(), // 28
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.US_PERSON.getFieldName(), // 29
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DFA.getFieldName(), // 30
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.FC_NFC_NFCPLUS.getFieldName(), // 31
			PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EMIR.getFieldName() // 32
	};

	/**
	 * Generates cell by cell, the report output
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getColumnValue(ReportRow reportRow, String columnName, @SuppressWarnings("rawtypes") Vector vector)
			throws InvalidParameterException {

		final Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> collateralTrade = (Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String>) reportRow
				.getProperty(PortfolioReconciliationReport.ROW_PROPERTY_ID);

		if ((reportRow == null) || (collateralTrade == null)) {
			vector.add("Row is empty, imposible to read data");
			return null;
		}

		if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_AGREEMENT.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_AGREEMENT);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_TYPE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_TYPE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COUNTERPARTY.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COUNTERPARTY);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_ID.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_ID);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EXTERNAL_REFERENCE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EXTERNAL_REFERENCE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.CLOSE_OF_BUSINESS.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.CLOSE_OF_BUSINESS);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.STRUCTURE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.STRUCTURE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_DATE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_VALUE_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_VALUE_DATE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_MATURITY_DATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_MATURITY_DATE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.VALUATION_AGENT.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.VALUATION_AGENT);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PORTFOLIO.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PORTFOLIO);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.OWNER.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.OWNER);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DEAL_OWNER.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DEAL_OWNER);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.INSTRUMENT.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.INSTRUMENT);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.UNDERLYING.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.UNDERLYING);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2_CCY.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2_CCY);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.IND_AMOUNT.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.IND_AMOUNT);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE_2.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE_2);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BUY_SELL.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BUY_SELL);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BASE_CCY.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BASE_CCY);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.MTM_BASE_CCY.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.MTM_BASE_CCY);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.USI.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.USI);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.SD_MSP.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.SD_MSP);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.US_PERSON.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.US_PERSON);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DFA.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DFA);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.FC_NFC_NFCPLUS.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.FC_NFC_NFCPLUS);

		} else if (PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EMIR.getFieldName().equals(columnName)) {
			return collateralTrade.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EMIR);
		}
		return null;
	}

}

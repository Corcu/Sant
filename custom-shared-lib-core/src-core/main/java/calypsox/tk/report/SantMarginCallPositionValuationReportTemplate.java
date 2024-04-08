/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.tk.report.MarginCallPositionValuationReportTemplate;

/**
 * Custom Margin Call Valuation report, originally requested for MMOO conciliation.
 * 
 * @author aela & Guillermo Solano
 * @date 27/03/2015
 * @version 2.0, added collateral config columns and custom columns (new filters: isin, agreement owner, agreements &
 *          process date)
 * 
 */
public class SantMarginCallPositionValuationReportTemplate extends MarginCallPositionValuationReportTemplate {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * report attributes names
	 */
	public static final String DEFAULT = "Default";
	public static final String VALUATION_DATE = "ValuationDatetime";
	public static final String DIRTY_PRICE_QUOTE = "DirtyPriceQuote";
	public static final String CONTRACT_VALUE_YESTERDAY_QUOTES = "ContractValueYest";
	public static final String MARGIN_CALL_CONFIG = "MarginCallConfig";
	public static final String VAL_DATE = "ValDate";

	/**
	 * Template Attributes names
	 */
	public static final String SECURITIES = "Securities_isins";
	public static final String CORE_TEMPLATE_MC_IDS = "MARGIN_CALL_CONFIG_IDS";
	public static final String OWNER_AGR = SantGenericTradeReportTemplate.PROCESSING_ORG_IDS;
	public static final String AGREEMENT_ID = SantGenericTradeReportTemplate.AGREEMENT_ID;
	public static final String AGREEMENT_TYPE = SantGenericTradeReportTemplate.AGREEMENT_TYPE;

	// contants
	public static final String ISIN = "ISIN";

	/**
	 * Default columns
	 */
	@Override
	public void setDefaults() {
		super.setDefaults();
		super.setColumns(SantMarginCallPositionValuationReportStyle.DEFAULTS_COLUMNS);
	}

}

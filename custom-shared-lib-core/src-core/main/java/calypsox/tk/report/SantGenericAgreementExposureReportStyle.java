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

public class SantGenericAgreementExposureReportStyle extends ReportStyle {

	public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";
	public static final String TRADE_ID = "Trade Id.";
	public static final String FRONT_ID = "Front Id.";
	public static final String PORTFOLIO = "Portfolio";
	public static final String REPORT_DATE = "Report date";
	public static final String CURRENT_DATE = "Current date";
	public static final String PREVIOUS_DATE = "Previous date";
	public static final String TRADE_DATE = "Trade date";
	public static final String COLLATERAL_AGREEMENT_TYPE = "Collateral Agreement Type";
	public static final String INSTRUMENT = "Instrument";
	public static final String STRUCTURE = "Structure";
	public static final String COLLATERAL_AGREEMENT_BASE_CCY = "Collateral Agreement Base ccy";
	public static final String CURRENT_MTM_CCY_AGREE = "Current MTM ccy agree";
	public static final String PREVIOUS_MTM_BASE_CCY = "Previous MTM base ccy";
	public static final String ECONOMIC_SECTOR = "Economic Sector";
	public static final String COUNTERPARTY = "Counterparty";
	public static final String MATURITY = "Maturity";

	public static final String[] DEFAULTS_COLUMNS = { COLLATERAL_AGREEMENT, TRADE_ID, FRONT_ID, PORTFOLIO, REPORT_DATE,
			CURRENT_DATE, PREVIOUS_DATE, TRADE_DATE, COLLATERAL_AGREEMENT_TYPE, INSTRUMENT, STRUCTURE,
			COLLATERAL_AGREEMENT_BASE_CCY, CURRENT_MTM_CCY_AGREE, PREVIOUS_MTM_BASE_CCY, ECONOMIC_SECTOR, COUNTERPARTY,
			MATURITY };

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		if (COLLATERAL_AGREEMENT.equals(columnName)) {
			return row.getProperty(COLLATERAL_AGREEMENT);
		}

		if (TRADE_ID.equals(columnName)) {
			return row.getProperty(TRADE_ID);
		}
		if (FRONT_ID.equals(columnName)) {
			return row.getProperty(FRONT_ID);
		}
		if (PORTFOLIO.equals(columnName)) {
			return row.getProperty(PORTFOLIO);
		}
		if (REPORT_DATE.equals(columnName)) {
			return row.getProperty(REPORT_DATE);
		}
		if (CURRENT_DATE.equals(columnName)) {
			return row.getProperty(CURRENT_DATE);
		}
		if (PREVIOUS_DATE.equals(columnName)) {
			return row.getProperty(PREVIOUS_DATE);
		}
		if (TRADE_DATE.equals(columnName)) {
			return row.getProperty(TRADE_DATE);
		}
		if (COLLATERAL_AGREEMENT_TYPE.equals(columnName)) {
			return row.getProperty(COLLATERAL_AGREEMENT_TYPE);
		}
		if (INSTRUMENT.equals(columnName)) {
			return row.getProperty(INSTRUMENT);
		}
		if (STRUCTURE.equals(columnName)) {
			return row.getProperty(STRUCTURE);
		}
		if (COLLATERAL_AGREEMENT_BASE_CCY.equals(columnName)) {
			return row.getProperty(COLLATERAL_AGREEMENT_BASE_CCY);
		}
		if (CURRENT_MTM_CCY_AGREE.equals(columnName)) {
			return row.getProperty(CURRENT_MTM_CCY_AGREE);
		}
		if (PREVIOUS_MTM_BASE_CCY.equals(columnName)) {
			return row.getProperty(PREVIOUS_MTM_BASE_CCY);
		}
		if (ECONOMIC_SECTOR.equals(columnName)) {
			return row.getProperty(ECONOMIC_SECTOR);
		}
		if (COUNTERPARTY.equals(columnName)) {
			return row.getProperty(COUNTERPARTY);
		}
		if (MATURITY.equals(columnName)) {
			return row.getProperty(MATURITY);
		}

		return null;
	}

}

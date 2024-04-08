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

public class SantDealAgreementExposureReportStyle extends ReportStyle {

	public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";
	public static final String FRONT_ID = "Front Id.";
	public static final String TRADE_ID = "Trade Id.";
	public static final String STRUCTURE = "Structure";
	public static final String PORTFOLIO = "Portfolio";
	public static final String INSTRUMENT = "Instrument";
	public static final String PREVIOUS_MTM_BASE_CCY = "Previous MTM base ccy";
	public static final String CURRENT_MTM_CCY_AGREE = "Current MTM ccy agree";
	public static final String MATURITY = "Maturity";

	public static final String[] DEFAULTS_COLUMNS = { COLLATERAL_AGREEMENT, FRONT_ID, TRADE_ID, STRUCTURE, PORTFOLIO,
			INSTRUMENT, PREVIOUS_MTM_BASE_CCY, CURRENT_MTM_CCY_AGREE, MATURITY };

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

		if (FRONT_ID.equals(columnName)) {
			return row.getProperty(FRONT_ID);
		}

		if (TRADE_ID.equals(columnName)) {
			return row.getProperty(TRADE_ID);
		}

		if (STRUCTURE.equals(columnName)) {
			return row.getProperty(STRUCTURE);
		}

		if (PORTFOLIO.equals(columnName)) {
			return row.getProperty(PORTFOLIO);
		}

		if (INSTRUMENT.equals(columnName)) {
			return row.getProperty(INSTRUMENT);
		}

		if (PREVIOUS_MTM_BASE_CCY.equals(columnName)) {
			return row.getProperty(PREVIOUS_MTM_BASE_CCY);
		}

		if (CURRENT_MTM_CCY_AGREE.equals(columnName)) {
			return row.getProperty(CURRENT_MTM_CCY_AGREE);
		}

		if (MATURITY.equals(columnName)) {
			return row.getProperty(MATURITY);
		}

		return null;
	}

}

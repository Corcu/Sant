/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantCounterpartyAgreementExposureReportStyle extends ReportStyle {

	public static final String COLLATERAL_TYPE = "Margin Call Contract Type";
	public static final String COLL_AGREE_CCY = "Collateral Agreement CCY";
	public static final String COUNTERPARTY = "Counterparty";
	public static final String PREVIOUS_TRADE_COUNT = "Previous trade count";
	public static final String CURRENT_TRADE_COUNT = "Current trade count";
	public static final String CURRENT_MTM = "Current MTM ccy agree";
	public static final String PREVIOUS_MTM = "Previous MTM base ccy";
	public static final String DIFFERENCE_MTM = "Difference MTM";

	public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";
	public static final String INSTRUMENT = "Instrument";
	public static final String DIFF_TRADE_COUNT = "Difference trade count";;

	public static final String[] DEFAULTS_COLUMNS = { COLLATERAL_TYPE, COLL_AGREE_CCY, COUNTERPARTY,
			PREVIOUS_TRADE_COUNT, CURRENT_TRADE_COUNT, CURRENT_MTM, PREVIOUS_MTM, DIFFERENCE_MTM };

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {
		AgrExposureReportItem item = (AgrExposureReportItem) row.getProperty(ReportRow.DEFAULT);

		if (COLLATERAL_TYPE.equals(columnName)) {
			return item.getContractType();

		} else if (COLLATERAL_AGREEMENT.equals(columnName)) {
			return item.getAgreementName();

		} else if (INSTRUMENT.equals(columnName)) {
			return item.getInstrument();

		} else if (COLL_AGREE_CCY.equals(columnName)) {
			return item.getAgreementName() + " - (" + item.getAgreementCurrency() + ")";

		} else if (COUNTERPARTY.equals(columnName)) {
			return item.getCounterParty();

		} else if (PREVIOUS_TRADE_COUNT.equals(columnName)) {
			return item.getTradeCountPrev();

		} else if (CURRENT_TRADE_COUNT.equals(columnName)) {
			return item.getTradeCountCurrent();

		} else if (DIFF_TRADE_COUNT.equals(columnName)) {
			return item.getTradeCountCurrent() - item.getTradeCountPrev();

		} else if (CURRENT_MTM.equals(columnName)) {
			return CollateralUtilities.formatAmount(item.getExposureCurrent(), item.getAgreementCurrency());

		} else if (PREVIOUS_MTM.equals(columnName)) {
			return CollateralUtilities.formatAmount(item.getExposurePrev(), item.getAgreementCurrency());

		} else if (DIFFERENCE_MTM.equals(columnName)) {
			double diff = item.getExposureCurrent() - item.getExposurePrev();
			return CollateralUtilities.formatAmount(diff, item.getAgreementCurrency());
		}

		return null;
	}
}

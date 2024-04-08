package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportStyle;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

public class SantMTMVariationReportStyle extends SantGenericTradeReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String MTM_BASE_VALUE_OLD = "MTM Base Old";
	public static final String MTM_BASE_VALUE_NEW = "MTM Base New";
	public static final String MTM_DIFF = "MTM Diff";
	public static final String INT_RATE = "Interest Rate";
	public static final String FX_RATE1 = "Rate";
	public static final String FX_RATE2 = "Rate2";

	public static String[] DEFAULTS_COLUMNS = { AGREEMENT_NAME, CLOSE_OF_BUSINESS, VAL_AGENT, INSTRUMENT,
			CONTRACT_PO_SHORT_NAME, MTM_BASE_VALUE_OLD, MTM_BASE_VALUE_NEW, MTM_DIFF };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final SantMTMAuditItem item = (SantMTMAuditItem) row.getProperty(SantMTMAuditItem.SANT_MTM_AUDIT_ITEM);

		if (columnName.equals(MTM_BASE_VALUE_OLD)) {
			return checkAndFormatNumer(item.getOldValue());
		} else if (columnName.equals(MTM_BASE_VALUE_NEW)) {
			return checkAndFormatNumer(item.getNewValue());
		} else if (columnName.equals(MTM_DIFF)) {
			return checkAndFormatNumer(item.getMTMDiff());
		} else if (columnName.equals(FX_RATE1)) {
			return checkAndFormatNumer(item.getRate1());
		} else if (columnName.equals(FX_RATE2)) {
			return checkAndFormatNumer(item.getRate2());
		} else if (columnName.equals(TradeReportStyle.TRADE_DATE)) {
			return item.getTradeDate();
		} else if (columnName.equals(INT_RATE)) {
			return super.getColumnValue(row, columnName, errors);
		}

		return super.getColumnValue(row, columnName, errors);

	}

	private String checkAndFormatNumer(final Double val) {
		if (val != null) {
			return formatNumber(val);
		} else {
			return null;
		}
	}
}

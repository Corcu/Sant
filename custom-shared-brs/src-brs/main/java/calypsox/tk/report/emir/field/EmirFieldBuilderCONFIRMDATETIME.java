package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderCONFIRMDATETIME implements EmirFieldBuilder {
	@Override
	public String getValue(Trade trade) {
		// Don't save CONFIRMDATETIME if CONFIRMTYPE is "NotConfirmed"
		String confirmDatetime = EmirSnapshotReduxConstants.EMPTY_SPACE;


		String confirmType = EmirFieldBuilderUtil.getInstance()
				.getLogicConfirmType(trade);
		if (!EmirSnapshotReduxConstants.NOT_CONFIRMED.equals(confirmType)) {
			confirmDatetime = trade
					.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_CONFIRMATION_DATE_TIME);
			if (Util.isEmpty(confirmDatetime)) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						EmirSnapshotReduxConstants.UTC_DATE_FORMAT,
						Locale.getDefault());
				dateFormat.setTimeZone(TimeZone
						.getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));
				String dateStr = dateFormat.format(trade.getEnteredDate());
				confirmDatetime = dateStr;
			}
		}
		return confirmDatetime;
	}
}

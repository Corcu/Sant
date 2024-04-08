package calypsox.tk.bo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.bo.notification.SantInterestNotificationCache;
import calypsox.tk.bo.notification.SantStatementHelper;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

/**
 * Formatter class for margin call notification template (interest statement template)
 * 
 */
@SuppressWarnings("rawtypes")
public class MC_INTERESTSimpleTransferMessageFormatter extends MarginCallMessageFormatter {

	private final SantInterestNotificationCache cache;

	public MC_INTERESTSimpleTransferMessageFormatter() {
		this.cache = new SantInterestNotificationCache();
	}

	public String parseSTATEMENT_DETAILS(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon)
			throws MessageFormatException {

		final Locale locale = Util.getLocale(message.getLanguage());
		final DateFormat df = new SimpleDateFormat("dd-MMM-yy", locale);

		return new SantStatementHelper(this.cache).getDetails(trade, df);
	}

	public String parseINTEREST_START_DATE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon)
			throws MessageFormatException {

		JDate startDate = this.cache.getStartDate(trade);

		final Locale locale = Util.getLocale(message.getLanguage());
		final DateFormat df = new SimpleDateFormat("dd-MMM-yy", locale);
		return df.format(startDate.getDate(TimeZone.getDefault())).toUpperCase();

	}

	public String parseAGREEMENT_NAME(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon) {

		CollateralConfig mcc = this.cache.getMarginCallConfig(trade);
		return mcc.getContractType() + "-" + mcc.getLegalEntity().getCode();
	}

	public String parseINTEREST_RATE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon) {

		return this.cache.getInterestRate(trade);

	}

	public String parseINTEREST_END_DATE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon)
			throws MessageFormatException {

		JDate endDate = this.cache.getEndDate(trade);

		final Locale locale = Util.getLocale(message.getLanguage());
		final DateFormat df = new SimpleDateFormat("dd-MMM-yy", locale);
		return df.format(endDate.getDate(TimeZone.getDefault())).toUpperCase();

	}

	@Override
	public String parseCONTRACT_AGREEMENT(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon) {

		return this.cache.getMarginCallConfig(trade).getContractType();
	}

	@Override
	public String parseCONTRACT_OPENDATE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon) {
		final CollateralConfig mcc = this.cache.getMarginCallConfig(trade);

		final Locale locale = Util.getLocale(message.getLanguage());
		final DateFormat df = new SimpleDateFormat("dd-MMM-yy", locale);
		return df.format((mcc.getStartingDate())).toUpperCase();

	}

	@Override
	protected int getLeIdForSDI(final BOMessage message, final Trade trade, final DSConnection dsCon) {

		final Account acc = this.cache.getAccount(trade);
		return (acc != null ? acc.getProcessingOrgId() : 0);
	}

}

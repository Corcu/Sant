/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

@SuppressWarnings({ "serial", "rawtypes" })
public class SantInterestPaymentRunnerOldReportStyle extends ReportStyle {

	public static final String ACCOUNT_ID = "Account Id";

	public static final String ACCOUNT_NAME = "Account Name";

	public static final String ACCOUNT_CURRENCY = "Currency";

	public static final String PROCESS_DATE = "Process Date";

	public static final String ADHOC_PAYMENT = "AdHoc Payment";

	public static final String WATCH_INTEREST = "Watch Interest";

	public static final String RATE_INDEX = "Index";

	public static final String SPREAD = "Spread";

	public static final String CONTRACT_NAME = "Contract Name";

	public static final String PO_OWNER = "PO Owner";
	
	//GSM 31/05/2017 - Chile requires cpty shortname in this report
	public static final String CPTY_SHORTNAME = "CPTY Short Name";

	public static final String AMOUNT_IB = "Amount IB";

	public static final String AMOUNT_CT = "Amount CT";

	public static final String SETTLE_DATE_CT = "Settle Date CT";

	public static final String SIMPLE_XFER_AMOUNT = "Simple Transfer Amount";

	public static final String HAS_SIMPLE_XFER = "Has SimpleXfer";

	public static final String SELECT = "Select";

	public static final String EMPTY = "";

	// BAU 6.1 - Add new column: ContractID
	public static final String CONTRACT_ID = "Contract ID";

	public static final String MESSAGE_STATUS = "Message Status";

	static String LABEL_BUY = "Pay";

	static String LABEL_SELL = "Receive";

	public static final String  PAYMENTMSG = "PAYMENTMSG";
	public static final String  MC_INTEREST = "MC_INTEREST";

	public static final String  SWIFT_STATUS = "SWIFT Status";
	public static final String  STATEMENT_STATUS = "Statement Status";



    @Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		if (row == null) {
			return null;
		}
		SantInterestPaymentRunnerEntry entry = (SantInterestPaymentRunnerEntry) row
				.getProperty(SantInterestPaymentRunnerReportTemplate.ROW_DATA);
		Boolean select = (Boolean) row.getProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA);

		if (ACCOUNT_ID.equals(columnName)) {
			return entry.getAccount().getId();
		}
		if (ACCOUNT_NAME.equals(columnName)) {
			// GSM: Call account name fix
			return entry.getAccount().getExternalName(); // getName(); old
		}
		if (ACCOUNT_CURRENCY.equals(columnName)) {
			return entry.getAccount().getCurrency();
		}
		if (PROCESS_DATE.equals(columnName)) {
			return entry.getProcessDate();
		}
		if (PO_OWNER.equals(columnName)) {
			return entry.getPoOwner();
		}
		//GSM 31/05/2017 - Chile requires cpty shortname in this report
		else if (CPTY_SHORTNAME.equals(columnName)){
			return entry.getCptyName();
		}

		if (MESSAGE_STATUS.equals(columnName)){
		    if(entry.getCtTrade()!=null){
				HashMap<String, BOMessage> latestMessages = getLatestMessages(entry.getCtTrade());
				if(!Util.isEmpty(latestMessages)){
					//Updating Messages for row
					entry.setPaymentMessage(latestMessages.get(PAYMENTMSG));
					entry.setInterest(latestMessages.get(MC_INTEREST));
					BOMessage boMessage = entry.getPaymentMessage();
					if(boMessage!=null){
						return boMessage.getStatus();
					}
				}
			}
			return null;
		}

		if (AMOUNT_CT.equals(columnName)) {
			if (entry.getCtTrade() == null) {
				return null;
			}
			if(LABEL_BUY.equalsIgnoreCase(getCtDirection(entry.getCtTrade()))){
				return new Amount(entry.getCtTrade().getProduct().getPrincipal()*-1, 2);
			}

			return new Amount(Math.abs(entry.getCtTrade().getProduct().getPrincipal()), 2);
		}

		if(SETTLE_DATE_CT.equalsIgnoreCase(columnName)){
			if (entry.getCtTrade() == null) {
				return null;
			}
            this.setEditableColumns(new String[]{SETTLE_DATE_CT});
			return entry.getCtTrade().getSettleDate();
		}
		
		if (AMOUNT_IB.equals(columnName)) {
			if (entry.getIbTrade() == null) {
				return null;
			}

			return new Amount(entry.getIbTrade().getProduct().getPrincipal(), 2);
		}
		// GSM: Simple Transfer amount added. 03/12/12
		if (SIMPLE_XFER_AMOUNT.equals(columnName)) {
			if (entry.getSimpleXferTrade() == null) {
				return null;
			}
			return new Amount(Math.abs(entry.getSimpleXferTrade().getProduct().getPrincipal()), 2);
		}

		if (ADHOC_PAYMENT.equals(columnName)) {
			String adHoc = entry.getAccount().getAccountProperty("PayInterestAdHoc");
			if (Util.isEmpty(adHoc)) {
				return false;
			}
			if (adHoc.equalsIgnoreCase("true")) {
				return true;
			}
			return false;
		}
		// GSM: Interest watch property added. 03/12/12
		if (WATCH_INTEREST.equals(columnName)) {
			String watch = entry.getAccount().getAccountProperty("WatchInterest");
			if (watch != null) {
				return watch;
			} else {
				return EMPTY;
			}
		}

		if (RATE_INDEX.equals(columnName)) {
			Account account = entry.getAccount();
			AccountInterestConfigRange accIntConfigRange = getAccIntConfigRange(account, entry.getProcessDate());
			if (accIntConfigRange != null) {
				return accIntConfigRange.getRateIndex();
			} else {
				return EMPTY;
			}

		} else if (SPREAD.equals(columnName)) {
			Account account = entry.getAccount();
			AccountInterestConfigRange accIntConfigRange = getAccIntConfigRange(account, entry.getProcessDate());
			if (accIntConfigRange != null) {
				if (accIntConfigRange.getSpread() != 0.0) {
					return accIntConfigRange.getSpread() * 100;
				} else {
					return 0;
				}
			} else {
				return "";
			}

			// BAU 6.1 - Add new column: ContractID
		} else if (CONTRACT_ID.equals(columnName)) {
			Account account = entry.getAccount();
			int mccId = 0;
			try {
				mccId = Integer.parseInt(account.getAccountProperty("MARGIN_CALL_CONTRACT"));
			} catch (Exception exc) {
				Log.error(this, exc); //sonar
			}
			return mccId;

		} else if (CONTRACT_NAME.equals(columnName)) {
			Account account = entry.getAccount();
			int mccId = 0;
			try {
				mccId = Integer.parseInt(account.getAccountProperty("MARGIN_CALL_CONTRACT"));
			} catch (Exception exc) {
				Log.error(this, exc); //sonar
			}
			MarginCallConfig marginCallConfig = BOCache.getMarginCallConfig(DSConnection.getDefault(), mccId);
			return marginCallConfig.getName();

		}else if (SWIFT_STATUS.equals(columnName)) {
			BOMessage paymentMessage = entry.getPaymentMessage();
			if(null!=paymentMessage){
				return paymentMessage.getStatus();
			}
			return "";
		}else if (STATEMENT_STATUS.equals(columnName)) {
			BOMessage interest = entry.getInterest();
			if(null!=interest){
				return interest.getStatus();
			}
			return "";
		}

		if (HAS_SIMPLE_XFER.equals(columnName)) {
            return null!=entry.getCtTrade() && "True".equalsIgnoreCase(entry.getCtTrade().getKeywordValue("SendStatement"));
		}
		if (SELECT.equals(columnName)) {
			return select;
		}

		return null;
	}

    @Override
	public boolean select(ReportRow row, String columnName, Boolean state) {
		if (row == null) {
			return false;
		}

		if (SELECT.equals(columnName)) {
			if (state == null) {
				// invert current state
				boolean currentState = ((Boolean) row
						.getProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA)).booleanValue();
				state = Boolean.valueOf(!currentState);
			}
			row.setProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA, state);
			return true;
		}
		return false;
	}

	private AccountInterestConfigRange getAccIntConfigRange(Account account, JDate processDate) {
		int interestConfigId = account.getAccountInterestConfigId(processDate, "Interest", false);
		if (interestConfigId == 0) {
			return null;
		}

		AccountInterestConfig accountInterestConfig = BOCache.getAccountInterestConfig(DSConnection.getDefault(),
				interestConfigId);
		if (accountInterestConfig != null) {
			Vector ranges = accountInterestConfig.getRanges();
			if (ranges == null) {
				return null;
			}

			for (int i = 0; i < ranges.size(); i++) {
				AccountInterestConfigRange ai = (AccountInterestConfigRange) ranges.get(i);
				if (ai.getActiveFrom().lte(processDate)
						&& ((ai.getActiveTo() == null) || (ai.getActiveTo().gte(processDate)))) {
					return ai;

				}
			}
		}
		return null;
	}

    private String getCtDirection(Trade trade){
		return trade!=null && trade.getQuantity() >= 0.0D ? LABEL_BUY : LABEL_SELL;
	}

	public AccountInterestConfigRange getRangeByDate(Vector ranges, JDate valDate) {
		if (ranges == null) {
			return null;
		}
		for (int i = 0; i < ranges.size(); i++) {
			AccountInterestConfigRange ai = (AccountInterestConfigRange) ranges.get(i);
			if (ai.getActiveFrom().lte(valDate)) {
				return ai;
			}
		}
		return null;
	}

	/**
	 * Return last SWIFT-> PAYMENTMSG message and last MC_INTEREST message for the trade.
	 *
	 * @param trade
	 * @return
	 */
	private HashMap<String,BOMessage> getLatestMessages(Trade trade){
		HashMap<String,BOMessage> latestMessages = new HashMap<>();

		if(null!=trade){
			StringBuilder where = new StringBuilder();
			where.append(" TRADE_ID = " + trade.getLongId());
			where.append(" AND message_type IN ('MC_INTEREST','PAYMENTMSG')");
			try {
				MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(where.toString(), null);

				BOMessage paymentMessage = Arrays.stream(messages.getMessages())
						.filter(message -> PAYMENTMSG.equalsIgnoreCase(message.getMessageType()))
						.max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

				latestMessages.put(PAYMENTMSG,paymentMessage);

				BOMessage mcInterestMessage = Arrays.stream(messages.getMessages())
						.filter(message -> MC_INTEREST.equalsIgnoreCase(message.getMessageType()))
						.max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

				latestMessages.put(MC_INTEREST,mcInterestMessage);

			} catch (CalypsoServiceException e) {
				Log.error(this,"Error loading messages: " + e );
			}

		}
    	return latestMessages;
	}

}

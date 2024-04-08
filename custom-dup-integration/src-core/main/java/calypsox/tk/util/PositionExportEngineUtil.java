package calypsox.tk.util;

import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.Pair;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import org.apache.commons.lang.time.DateUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

public class PositionExportEngineUtil {

	public static final String LOG_CATEGORY = "PositionExportEngine";

	public static final String DIRTY_PRICE_STR = "DirtyPrice";

	public static final String KW_MUREX_ID = "MxID";
	public static final String KW_SECURITY_TYPE = "SECURITY_TYPE";
	public static final String KW_TRIPARTY = "TRIPARTY";
	public static final String KW_KEY_REVERSE = "KEY_REVERSE";
	public static final String KW_DESTINATION_FOLDER = "DESTINATION_FOLDER";
	public static final String KW_TOPIC = "TOPIC";
	public static final String KW_FACE_VALUE = "FACE_VALUE";
	public static final String KW_MATURITY_DATE = "MATURITY_DATE";
	public static final String KW_REHYPOTHETICAL = "REHYPOTHETICAL";
	public static final String KW_COLLATERAL_TYPE = "COLLATERAL_TYPE";
	public static final String KW_NOMINAL_PRICE = "NOMINAL_PRICE";
	public static final String KW_OLD_POSITION = "OLD_POSITION";
	public static final String KW_NEW_POSITION = "NEW_POSITION";
	public static final String KW_POSITION_DELTA = "POSITION_DELTA";
	public static final String KW_POSITION_DATE = "POSITION_DATE";
	public static final String KW_POSITION_ID = "POSITION_ID";
	public static final String KW_TRIPARTY_EXTERNAL = "TRIPARTY_EXTERNAL";
	public static final String KW_TRIPARTY_RUN = "TRIPARTY_RUN";
	public static final String KW_PRICE = "PRICE";
	public static final String KW_TRADE_BOOK = "TRADE_BOOK";

	public static final String ALIAS_BOOK_K_NO_REHYP = "ALIAS_BOOK_K+_NO_REHYP";
	public static final String ALIAS_BOOK_K_REHYP = "ALIAS_BOOK_K+_REHYP";
	public static final String ALIAS_BOOK_EQUITY = "ALIAS_BOOK_EQUITY";
	public static final String ALIAS_BOOK_KONDOR = "ALIAS_BOOK_KONDOR";

	// Reuso Accounts
	public static final String TK_FROM_TRIPARTY = "FromTripartyAllocation";
	public static final String TK_REVERSED_ALLOC = "ReversedAllocationTrade";
	public static final String TK_COLLATERAL_GIVER = "Collateral Giver";
	public static final String DV_REUSE_ACCOUNTS = "MT569_REUSE_ACCOUNTS";
	public static final String AD_B_TRIPARTY_REUSE_BOOK = "BOND_TRIPARTY_REUSE_BOOK";
	public static final String AD_E_TRIPARTY_REUSE_BOOK = "EQUITY_TRIPARTY_REUSE_BOOK";

	public static final String TRANSFER_ATTR_BUSINESS_REASON = "BusinessReason";
	public static final String TRANSFER_SPLIT = "SPLIT";
	public static final String TRANSFER_PARTIAL_SETTLE = "PARTIAL_SETTLE";
	public static final String ECMS = "ECMS";
	public static final String ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_PLEDGE_ACCOUNTS";
	public static final String UNDO = "UNDO";
	public static final String UNDO_XFER_ATTRIBUTE = "IsECMSUndoSplit";

	private static final String DAIL = "DAIL";
	private static final String EOD = "EOD";

	public static final String TOPIC = "TOPIC";

	public static final String TRUE = "TRUE";
	public static final String FALSE = "FALSE";

	protected static final String PREFIX_BOND_ACC = "Bond_";
	protected static final String PREFIX_EQUITY_ACC = "Equity_";

	// MOVEMENT_TYPE
	public static final String IM = "IM";
	public static final String VM = "VM";
	public static final String CONTRACT_TYPE_CSD = "CSD";

	public static final String POSITION_EXPORT_BOND_DATE = "POSITION_EXPORT_BOND_DATE";
	public static final String POSITION_EXPORT_EQUITY_DATE = "POSITION_EXPORT_EQUITY_DATE";

	public static final String format = "dd/MM/yyyy";
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

	public static String WHERE_CLAUSE = "trade.PRODUCT_ID=simplexfer.PRODUCT_ID AND simplexfer.SECURITY_ID=sec_code.PRODUCT_ID AND sec_code.SEC_CODE='ISIN' "
			+ "AND keywords.trade_id = trade.trade_id AND keywords.KEYWORD_NAME=\'" + KW_MUREX_ID + "\'"
			+ "AND CODE_VALUE=? AND LINKED_ID=? AND trade.TRADE_CURRENCY=? AND trade.BOOK_ID=?";

	public static String FROM_CLAUSE = "PRODUCT_SIMPLEXFER simplexfer, PRODUCT_SEC_CODE sec_code, TRADE_KEYWORD keywords";

	public static boolean isExportTrade(PSEventInventorySecPosition event, Trade trade) {
		if (trade.getProduct() instanceof MarginCall) {
			MarginCall mc = (MarginCall) trade.getProduct();
			JDate positionDate = event.getPosition().getPositionDate();
			JDate exportStartDate = null;

			if (mc.getSecurity() instanceof Equity) {
				String dateField = mc.getMarginCallConfig().getAdditionalField(POSITION_EXPORT_EQUITY_DATE);
				if (Util.isEmpty(dateField))
					return false;
				exportStartDate = JDate.valueOf(dateField);
			} else if (mc.getSecurity() instanceof Bond) {
				String dateField = mc.getMarginCallConfig().getAdditionalField(POSITION_EXPORT_BOND_DATE);
				if (Util.isEmpty(dateField))
					return false;
				exportStartDate = JDate.valueOf(dateField);
			}

			if (exportStartDate == null)
				return false;

			return exportStartDate.lte(positionDate);
		}
		return true;
	}

	/**
	 * handle psevent in the past : get the current updated position.
	 * 
	 * @param event
	 * @return
	 */

	public static PSEventInventorySecPosition updatePSEventInventorySecPosition(PSEventInventorySecPosition event) {
		if (DateUtils.isSameDay(event.getPosition().getPositionDate().getDate(), JDate.getNow().getDate())) {
			return event;
		}

		PSEventInventorySecPosition updatedEvent;
		try {
			updatedEvent = (PSEventInventorySecPosition) event.clone();
			updatedEvent.setPosition((InventorySecurityPosition) event.getPosition().clone());
			try {
				/*
				 * uncomment this to handle past position update
				 * updatedEvent.getPosition().setPositionDate(JDate.getNow().addDays(1)); JDate
				 * lastInvPos =
				 * DSConnection.getDefault().getRemoteInventory().getLastSecurityPosition(
				 * updatedEvent.getPosition());
				 * if(!DateUtils.isSameDay(event.getPosition().getPositionDate().getDate(),
				 * lastInvPos.getDate())) {
				 * updatedEvent.getPosition().setPositionDate(lastInvPos);
				 * InventorySecurityPosition securityPosition =
				 * DSConnection.getDefault().getRemoteInventory().getInventorySecurityPosition(
				 * updatedEvent.getPosition()); updatedEvent.setPosition(securityPosition); }
				 * 
				 */
				updatedEvent.getPosition().setPositionDate(JDate.getNow());
				InventorySecurityPosition securityPosition = DSConnection.getDefault().getRemoteInventory()
						.getInventorySecurityPosition(updatedEvent.getPosition());
				if (securityPosition == null)
					return event;
				updatedEvent.setPosition(securityPosition);
				return updatedEvent;
			} catch (CalypsoServiceException e) {
				Log.error(LOG_CATEGORY, e);
			}
			// updatedEvent.getPosition().setPositionDate(JDate.getNow());

		} catch (CloneNotSupportedException e1) {
			Log.error(LOG_CATEGORY, e1);
		}
		return null;

	}

	public static boolean updateTrade(DSConnection ds, Trade trade, PSEventInventorySecPosition event,
			boolean performAccountClosing) throws Exception {

		// Update position related keywords
		InventorySecurityPosition pos = event.getPosition();

		trade.setAction(Action.NEW);
		if (!Util.isEmpty(trade.getKeywordValue("isECMSPledge")) && trade.getKeywordValue("isECMSPledge").equals("Y")) {
			addKeyword(trade, KW_OLD_POSITION, formatNumber(
					(pos.getTotal() - event.getDelta()) == 0 ? 0 : -1 * (pos.getTotal() - event.getDelta())));
			addKeyword(trade, KW_NEW_POSITION, formatNumber(pos.getTotal() == 0 ? 0 : -1 * pos.getTotal()));
			addKeyword(trade, KW_POSITION_DELTA, formatNumber(-1 * event.getDelta()));
		} else {
			addKeyword(trade, KW_OLD_POSITION, formatNumber(pos.getTotal() - event.getDelta()));
			addKeyword(trade, KW_NEW_POSITION, formatNumber(pos.getTotal()));
			addKeyword(trade, KW_POSITION_DELTA, formatNumber(event.getDelta()));
		}

		addKeyword(trade, KW_POSITION_DATE, formatDate(pos.getPositionDate()));
		addKeyword(trade, KW_POSITION_ID, event.toString());
		if (performAccountClosing) {
			addKeyword(trade, KW_KEY_REVERSE, getKeyReversed(ds, trade));
		}
		addKeyword(trade, KW_TRIPARTY_RUN, getTripartyRun(trade));
		addKeyword(trade, KW_TRADE_BOOK, getTradeBook(trade));

		// keywords
		if (trade.getProduct() instanceof MarginCall) {
			MarginCall mc = (MarginCall) trade.getProduct();
			addKeyword(trade, KW_SECURITY_TYPE, getSecurityType(mc));
			addKeyword(trade, KW_TRIPARTY, isTriparty(trade) ? TRUE : FALSE);
			addKeyword(trade, KW_DESTINATION_FOLDER, getDestinationFolder(trade));
			addKeyword(trade, KW_TOPIC, getTopicAttribute(trade));
			addKeyword(trade, KW_FACE_VALUE, getFaceValue(mc));
			addKeyword(trade, KW_MATURITY_DATE, getMaturityDate(mc));
			addKeyword(trade, KW_REHYPOTHETICAL, getRehypothecable(mc));
			addKeyword(trade, KW_COLLATERAL_TYPE, getMovementType(mc));
			addKeyword(trade, KW_NOMINAL_PRICE, getNominalPrice(trade));
			addKeyword(trade, KW_TRIPARTY_EXTERNAL, getTripartyExternal(mc));
			addKeyword(trade, KW_PRICE, getPrice(trade, event.getPosition().getPositionDate()));

		}

		return true;

	}

	public static String getSecurityType(Trade trade) {
		if (trade.getProduct() instanceof MarginCall) {
			MarginCall mc = (MarginCall) trade.getProduct();
			return getSecurityType(mc);
		}

		return "";
	}

	public static String getSecurityType(MarginCall mc) {
		if (mc.getSecurity() instanceof Equity) {
			return "Equity";
		} else if (mc.getSecurity() instanceof Bond) {
			return "Bond";
		}
		return "";
	}

	public static void addKeyword(Trade trade, String key, String value) {
		if (Util.isEmpty(trade.getKeywordValue(key)) && !Util.isEmpty(value)) {
			trade.addKeyword(key, value);
		}
	}

	public static String getKeyReversed(DSConnection ds, Trade trade) throws Exception {
		if (isTriparty(trade))
			return trade.getKeywordValue(SantTradeKeywordUtil.REVERSED_ALLOCATION_TRADE);

		// update murex reference.
		Trade lastMurexTrade = findExistingMurexTrade(ds, trade);
		if (lastMurexTrade != null)
			return lastMurexTrade.getKeywordValue(KW_MUREX_ID);

		return null;

	}

	public static Trade findExistingMurexTrade(DSConnection ds, Trade trade) throws Exception {
		TradeArray trades = getExistingTradesFromIsinContractCcy(ds, trade);
		if (trades.size() == 0) {
			Log.info(LOG_CATEGORY, "no trade found with KEYWORD " + KW_MUREX_ID);
			return null;
		}
		if (trades.size() > 1) {
			String errorMessage = KW_MUREX_ID + " not unique";
			if (trade.getProduct().getType().equals(Product.MARGINCALL)) {
				MarginCall marginCall = (MarginCall) trade.getProduct();
				int marginCallId = marginCall.getMarginCallId();
				String isin = marginCall.getUnderlyingProduct().getSecCode(SecCode.ISIN);
				String ccy = trade.getSettleCurrency();
				errorMessage += " for Contract:" + marginCallId + ", ISIN:" + isin + ", CCy:" + ccy;
			}
			errorMessage += " [";
			for (int i = 0; i < trades.size(); i++) {
				if (i != 0)
					errorMessage += ",";
				errorMessage += trades.get(i).getLongId();
			}
			errorMessage += "]";

			Log.error(LOG_CATEGORY, errorMessage);
			throw new Exception(errorMessage);
		} else
			// return reverseTrade(trades.get(0), event);
			return trades.get(0);

	}

	public static TradeArray getExistingTradesFromIsinContractCcy(DSConnection ds, Trade trade) {
		TradeArray trades = new TradeArray();

		if (trade.getProduct().getType().equals(Product.MARGINCALL)) {
			MarginCall marginCall = (MarginCall) trade.getProduct();
			int marginCallId = marginCall.getMarginCallId();
			String isin = marginCall.getUnderlyingProduct().getSecCode(SecCode.ISIN);
			String ccy = trade.getSettleCurrency();
			long bookId = trade.getBookId();
			ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
			String whereClause = getWhereClauseForIsinContractCcy(marginCallId, isin, ccy, bookId, bindVariables);

			try {
				trades = ds.getRemoteTrade().getTrades(FROM_CLAUSE, whereClause, "trade.trade_id desc", true,
						bindVariables);
			} catch (CalypsoServiceException e) {
				Log.error(LOG_CATEGORY, e);
			}
		}

		return trades;

	}

	public static String getWhereClauseForIsinContractCcy(long marginCallId, String isin, String ccy, long bookId,
			ArrayList<CalypsoBindVariable> bindVariables) {
		CalypsoBindVariable bindVariableMarginCallId = new CalypsoBindVariable(CalypsoBindVariable.LONG, marginCallId);
		CalypsoBindVariable bindVariableIsin = new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, isin);
		CalypsoBindVariable bindVariableCcy = new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, ccy);
		CalypsoBindVariable bindVariableBookId = new CalypsoBindVariable(CalypsoBindVariable.LONG, bookId);
		bindVariables.add(bindVariableIsin);
		bindVariables.add(bindVariableMarginCallId);
		bindVariables.add(bindVariableCcy);
		bindVariables.add(bindVariableBookId);
		return WHERE_CLAUSE;

	}

	/**
	 * @param trade
	 * @return true if the trade has keyword FromTripartyAllocation with value true,
	 *         false otherwise
	 */
	public static boolean isTriparty(final Trade trade) {
		String fromTripartyAllocation = trade.getKeywordValue(SantTradeKeywordUtil.FROM_TRIPARTY_ALLOCATION);
		if (!Util.isEmpty(fromTripartyAllocation)) {
			return Boolean.valueOf(fromTripartyAllocation);
		}
		return false;
	}

	public static String getDestinationFolder(final Trade trade) {
		List<String> domValues = LocalCache.getDomainValues(DSConnection.getDefault(),
				SantDomainValuesUtil.MT569_DESTINATION_FOLDER_MAPPING);
		String account = trade.getKeywordValue(SantTradeKeywordUtil.COLLATERAL_GIVER);

		if (!Util.isEmpty(account) && domValues.contains(account)) {
			return getAcBook(account);
		} else if (trade.getProduct() instanceof MarginCall
				&& (((MarginCall) trade.getProduct()).getSecurity() != null)) {

			if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond
					&& domValues.contains(PREFIX_BOND_ACC + account)) {
				return getAcBook(PREFIX_BOND_ACC + account);
			} else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity
					&& domValues.contains(PREFIX_EQUITY_ACC + account)) {
				return getAcBook(PREFIX_EQUITY_ACC + account);
			}
		}

		return "";
	}

	private static String getAcBook(String account) {
		String comment = LocalCache.getDomainValueComment(DSConnection.getDefault(),
				SantDomainValuesUtil.MT569_DESTINATION_FOLDER_MAPPING, account);
		return comment;
	}

	public static String getFaceValue(MarginCall mc) {

		if (mc.getSecurity() instanceof Bond) {
			double faceValue = ((Bond) mc.getSecurity()).getFaceValue();
			return formatNumber(faceValue);
		}
		return "";

	}

	public static String getMaturityDate(MarginCall mc) {

		if (mc.getSecurity() instanceof Bond) {
			return formatDate(((Bond) mc.getSecurity()).getMaturityDate());
		}
		return "";

	}

	public static String getNominalPrice(Trade trade) {
		MarginCall mc = (MarginCall) trade.getProduct();

		if (mc.getSecurity() instanceof Equity) {
			Equity e = (Equity) mc.getSecurity();
			double nominalPrice = e.getNominalDecimals() * trade.getNegociatedPrice();
			return formatNumber(nominalPrice);
		}
		return "";

	}

	public static Double getDirtyPrice(Trade trade) {

		String e = trade.getKeywordValue("DirtyPrice");
		String locale = trade.getKeywordValue("EnteredTradeLocale");
		if (Util.isEmpty(locale)) {
			locale = Util.getDisplayName(Locale.getDefault());
		}

		if (e != null) {
			return Util.stringToNumber(e, Util.getLocale(locale));
		} else {
			return null;
		}

	}

	public static String getPrice(Trade trade, JDate date) {
		MarginCall mc = (MarginCall) trade.getProduct();

		if (isTriparty(trade)) {
			Double dirtyPrice = getDirtyPrice(trade);
			if (dirtyPrice != null)
				return formatNumber(dirtyPrice);
		} else {
			if (mc.getSecurity() instanceof Equity) {
				double price = trade.getNegociatedPrice();
				return formatNumber(price);
			}
			if (mc.getSecurity() instanceof Bond) {
				Double dirtyPrice = getBilateralDirtyPrice(trade, date);
				if (dirtyPrice != null)
					return formatNumber(dirtyPrice);
			}
		}
		return "";
	}

	public static Double getBilateralDirtyPrice(Trade trade, JDate date) {

		MarginCall mc = (MarginCall) trade.getProduct();
		JDate quoteDate = date.addBusinessDays(-1, trade.getBook().getHolidays());
		PricingEnv dirtyPricePE = AppUtil.loadPE(DIRTY_PRICE_STR,
				JDatetime.currentTimeValueOf(date, TimeZone.getDefault()));
		QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
		int quoteBase = 1;
		if (mc.getSecurity() instanceof Bond) {
			quoteBase = ((Bond) mc.getSecurity()).getQuoteBase();
		}
		QuoteValue productQuote = quoteSet.getProductQuote(mc.getSecurity(), quoteDate, DIRTY_PRICE_STR);
		if (productQuote == null || productQuote.getClose() == 0.0d)
			return trade.getTradePrice() * quoteBase;
		return productQuote.getClose() * quoteBase;

	}

	public static String getRehypothecable(MarginCall mc) {
		MarginCallConfig marginCallConf = mc.getMarginCallConfig();
		if (null != marginCallConf && marginCallConf.isRehypothecable())
			return "1";
		return "0";
	}

	public static String getMovementType(MarginCall marginCall) {
		String movementType = VM;
		if (null != marginCall) {
			MarginCallConfig marginCallConf = marginCall.getMarginCallConfig();
			if (null != marginCallConf && CONTRACT_TYPE_CSD.equals(marginCallConf.getContractType())) {
				movementType = IM;
			}
		}
		return movementType;
	}

	public static String getTripartyExternal(MarginCall marginCall) {
		MarginCallConfig marginCallConf = marginCall.getMarginCallConfig();
		return marginCallConf.getAdditionalField(KW_TRIPARTY_EXTERNAL);
	}

	public static synchronized String formatNumber(double valueToFormat) {

		DecimalFormat df = new DecimalFormat("0.00");
		df.setGroupingUsed(false);
		DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
		newSymbols.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(newSymbols);
		return df.format(valueToFormat);

	}

	public static String getTripartyRun(final Trade trade) {
		String statementFrequency = trade.getKeywordValue(SantTradeKeywordUtil.STATEMENT_FREQUENCY_INDICATOR);
		if (!Util.isEmpty(statementFrequency) && DAIL.equalsIgnoreCase(statementFrequency)) {
			return EOD;
		} else {
			String statementNumber = trade.getKeywordValue(SantTradeKeywordUtil.STATEMENT_NUMBER);
			if (!Util.isEmpty(statementNumber)) {
				return statementNumber;
			}
		}

		return null;
	}

	/**
	 * Parse the JDate to String.
	 *
	 * @param valueToFormat Value to format.
	 * @return The date formatted (converted to String).
	 * @throws ParseException Exception if we cannot parse the date.
	 */
	public static synchronized String formatDate(JDate valueToFormat) {
		return simpleDateFormat.format(valueToFormat.getDate(TimeZone.getDefault()));
	}

	public static String getTradeBook(Trade trade) {
		return findBook(trade);
	}

	public static Pair<PSEventInventorySecPosition, Pair<Trade, Trade>> getObjectToExport(
			PSEventInventorySecPosition event, Pair<Trade, Trade> trades) {
		return new Pair<>(event, trades);
	}

	public static String getTopicAttribute(final Trade trade) {

		if (trade != null) {
			Vector<LegalEntityAttribute> attrs = BOCache.getLegalEntityAttributes(DSConnection.getDefault(),
					trade.getBook().getLegalEntity().getId());
			if (!Util.isEmpty(attrs)) {
				for (LegalEntityAttribute legalEntityAttribute : attrs) {
					if (TOPIC.equals(legalEntityAttribute.getAttributeType())) {
						return legalEntityAttribute.getAttributeValue();
					}
				}
			}
		}

		return "";
	}

	public static String getErrorMessage(Exception e) {
		String errorMessage = e.getMessage();

		if (Util.isEmpty(errorMessage) && e.getCause() != null && !Util.isEmpty(e.getCause().getMessage())) {
			errorMessage = e.getCause().getMessage() + " (check logs)";
		}

		if (Util.isEmpty(errorMessage))
			errorMessage = "check logs";

		return errorMessage;
	}

	public static boolean isSplit(BOTransfer transfer, PSEventInventorySecPosition event) {

		if (transfer.getStatus().equals(Status.valueOf(TRANSFER_SPLIT))
				|| transfer.getStatus().equals(Status.valueOf(TRANSFER_PARTIAL_SETTLE))) {
			return true;
		}

		// Get ECMS accounts
		ArrayList<String> filterAccounts = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(),
				ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		for (String account : pledgeAccounts) {
			filterAccounts.add(account.split(";")[1]);
		}
		Account acc;
		try {
			acc = DSConnection.getDefault().getRemoteAccounting().getAccount(transfer.getGLAccountNumber());
		} catch (Exception e) {
			return true;
		}

		if (!filterAccounts.isEmpty() && filterAccounts.contains(acc.getName())
				&& transfer.getAction().equals(Action.valueOf("UNDO"))) {
			return true;
		}

		String businessReason = transfer.getAttribute(TRANSFER_ATTR_BUSINESS_REASON);
		if (businessReason != null
				&& (businessReason.equals(TRANSFER_SPLIT) || businessReason.equals(TRANSFER_PARTIAL_SETTLE))) {
			return exportSplitedPositiuonPosition(transfer, filterAccounts, acc, event);
		}

		return false;
	}

	public static String findBook(Trade trade) {
		if (trade.getProduct() != null && isReusoAccount(trade)) {
			return getAliasBook(trade);
		} else {
			String bookInCalypso = processReversalTripartyTrade(trade);
			if (!Util.isEmpty(bookInCalypso)) {
				return bookInCalypso;
			}
			bookInCalypso = trade.getBook().getName();

			String aliasForSearch = null;

			if ((null != bookInCalypso) && !"".equals(bookInCalypso)) {
				MarginCall marginCall = (MarginCall) trade.getProduct();
				if (null != marginCall) {
					Product p = marginCall.getSecurity();
					if (p != null) {

						if (p instanceof Bond) {
							try {
								// We look at the rehypotecable mark in the
								// contract.
								if (isRehypotecableContract(trade).equals("1")) {
									aliasForSearch = ALIAS_BOOK_K_REHYP;
								} else {
									aliasForSearch = ALIAS_BOOK_K_NO_REHYP;
								}
							} catch (Exception e) {
								Log.error(PositionExportEngineUtil.class.getSimpleName(), e); // sonar
							}

						} else if (p instanceof Equity) {
							try {
								aliasForSearch = ALIAS_BOOK_EQUITY;
							} catch (Exception e) {
								Log.error(PositionExportEngineUtil.class.getSimpleName(), e); // sonar
							}
						}
					}
				}

				// We look for the book in the alias included for K+.
				String bookReturned = CollateralUtilities.getBookAliasMapped(bookInCalypso, aliasForSearch);
				if ((null != bookReturned) && !"".equals(bookReturned) && !bookReturned.startsWith("BOOK_WARNING")) {
					return bookReturned;
				} else {
					return bookInCalypso;
				}
			}
		}

		return null;
	}

	private static boolean isReusoAccount(Trade trade) {
		int direction = trade.getProduct().getBuySell(trade); // MarginCall direction (<0):Pay / (>=0):Receive
		boolean tripartyAllocation = Boolean.parseBoolean(trade.getKeywordValue(TK_FROM_TRIPARTY));
		boolean reversedAllocation = Util.isEmpty(trade.getKeywordValue(TK_REVERSED_ALLOC));

		if (direction < 0.0 && tripartyAllocation && reversedAllocation) {
			String collateralGiver = trade.getKeywordValue(TK_COLLATERAL_GIVER);
			if (!Util.isEmpty(collateralGiver)) {
				try {
					DomainValues.DomainValuesRow mt569ReuseAccounts = DSConnection.getDefault().getRemoteReferenceData()
							.getDomainValuesRow(DV_REUSE_ACCOUNTS, collateralGiver);
					if (null != mt569ReuseAccounts) {
						return true;
					}
				} catch (CalypsoServiceException e) {
					Log.error(PositionExportEngineUtil.class.getSimpleName(),
							"Cannot get DomainValue " + DV_REUSE_ACCOUNTS + " : " + collateralGiver + " Error: " + e);
				}
			}
		}
		return false;
	}

	private static String getAliasBook(Trade trade) {
		String bookAlias = "";
		String alias;
		Product product = ((MarginCall) trade.getProduct()).getSecurity();
		MarginCallConfig contract = ((MarginCall) trade.getProduct()).getMarginCallConfig();

		if (null != contract && null != product) {
			if (product instanceof Bond) {
				alias = contract.getAdditionalField(AD_B_TRIPARTY_REUSE_BOOK);
				if (!Util.isEmpty(alias)) {
					Book book = getBookFromAlias(alias, DSConnection.getDefault());
					if (null != book) {
						if (contract.isRehypothecable()) {
							bookAlias = book.getAttribute(ALIAS_BOOK_K_REHYP);
						} else {
							bookAlias = book.getAttribute(ALIAS_BOOK_K_NO_REHYP);
						}
					}
				}
			} else if (product instanceof Equity) {
				alias = contract.getAdditionalField(AD_E_TRIPARTY_REUSE_BOOK);
				if (!Util.isEmpty(alias)) {
					Book book = getBookFromAlias(alias, DSConnection.getDefault());
					bookAlias = Optional.ofNullable(book).map(bk -> bk.getAttribute(ALIAS_BOOK_EQUITY)).orElse("");
				}
			}
		}

		return bookAlias;
	}

	private static Book getBookFromAlias(String alias, DSConnection dsConn) {
		Book book = null;
		try {
			book = dsConn.getRemoteReferenceData().getBook(alias);
		} catch (CalypsoServiceException e) {
			Log.error(PositionExportEngineUtil.class.getSimpleName(), "Cannot load book: " + alias + " Error: " + e);
		}
		return book;
	}

	private static String processReversalTripartyTrade(Trade trade) {
		String book = "";
		if (isReversalTripartyTrade(trade)) {
			Trade originalAllocationTrade;
			try {
				originalAllocationTrade = DSConnection.getDefault().getRemoteTrade()
						.getTrade(Long.parseLong(trade.getKeywordValue(TK_REVERSED_ALLOC)));
			} catch (Exception e) {
				return book;
			}
			if (isReusoAccount(originalAllocationTrade)) {
				book = getAliasBook(originalAllocationTrade);
			}
		}
		return book;
	}

	private static boolean isReversalTripartyTrade(Trade trade) {
		java.util.Optional<Trade> tradeOpt = java.util.Optional.ofNullable(trade);
		return !Util.isEmpty(tradeOpt.map(t -> t.getKeywordValue(TK_REVERSED_ALLOC)).orElse(""));
	}

	public static String isRehypotecableContract(Trade trade) {
		MarginCall marginCall = (MarginCall) trade.getProduct();
		if (null != marginCall) {
			MarginCallConfig marginCallConf = marginCall.getMarginCallConfig();
			if (null != marginCallConf && marginCallConf.isRehypothecable()) {
				return "1";
			}
		}
		return "0";
	}

	private static Boolean exportSplitedPositiuonPosition(BOTransfer transfer, ArrayList<String> filterAccounts,
			Account acc, PSEventInventorySecPosition event) {
		String isECMSUndoSplit = transfer.getAttribute(UNDO_XFER_ATTRIBUTE);

		if ((isECMSUndoSplit != null && transfer.getStatus().equals(Status.S_CANCELED)
				&& isECMSUndoSplit.equals("Y"))) {
			return true;
		}

		// In ECMS accounts, only send message if the position has changed
		if (!filterAccounts.isEmpty() && filterAccounts.contains(acc.getName())) {
			try {

				Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
				MarginCall mc = (MarginCall) trade.getProduct();
				BOTransfer xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(transfer.getParentLongId());

				if (trade != null && xfer != null) {
					TransferArray transferArray = DSConnection.getDefault().getRemoteBO()
							.getBOTransfers(trade.getLongId());
					transferArray.remove(xfer);
					double nominalAmount = 0.0;
					for (BOTransfer xferArray : transferArray) {
						Account acc1;
						try {
							acc1 = DSConnection.getDefault().getRemoteAccounting()
									.getAccount(xferArray.getGLAccountNumber());
						} catch (Exception e) {
							return true;
						}
						if (filterAccounts.contains(acc1.getName())) {
							if (!xferArray.getStatus().equals(Status.CANCELED)
									&& xferArray.getParentLongId() == xfer.getLongId())
								nominalAmount += xferArray.getNominalAmount();
						}
					}
					if (Double.compare(xfer.getNominalAmount(), nominalAmount) == 0) {
						return true;
					} else
						return false;

				}
			} catch (Exception excep) {
				Log.error(PositionExportEngineUtil.class, "Error giving Parent Transfer.");
				return true;
			}

		} else {
			return true;
		}
		return false;
	}
}

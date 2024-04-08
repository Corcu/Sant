/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory;

import static calypsox.engine.inventory.SantPositionConstants.ACTUAL;
import static calypsox.engine.inventory.SantPositionConstants.ACTUAL_MAPPING;
import static calypsox.engine.inventory.SantPositionConstants.BLOQUEO;
import static calypsox.engine.inventory.SantPositionConstants.BLOQUEO_MAPPING;
import static calypsox.engine.inventory.SantPositionConstants.EMPTY;
import static calypsox.engine.inventory.SantPositionConstants.EOD_MODE;
import static calypsox.engine.inventory.SantPositionConstants.FIELDS_NUMBER;
import static calypsox.engine.inventory.SantPositionConstants.FIELDS_SEPARATOR;
import static calypsox.engine.inventory.SantPositionConstants.NORMAL_MODE;
import static calypsox.engine.inventory.SantPositionConstants.RESPONSE_SEPARATOR;
import static calypsox.engine.inventory.SantPositionConstants.THEORETICAL;
import static calypsox.engine.inventory.SantPositionConstants.THEORETICAL_MAPPING;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.engine.inventory.SantPositionConstants.RESPONSES_CODES;
import calypsox.engine.inventory.util.PositionLogHelper;

/**
 * Builds a position bean, checking format and static data (if possible). If any errors are produced, then the
 * linePositionBeanParserStatus will not be empty.
 * 
 * @author Patrice Guerrido & Guillermo Solano
 * @version 1.6, change in the record process, added value to store original DB position. Added more account logging
 * 
 */
public class SantPositionBean {

	// patterns to check the date format of field 9 - positionDate
	public final static Pattern datePattern = Pattern.compile("\\d{2}/\\d{2}/[1-3]\\d{3}");
	/**
	 * DS connection pointer
	 */
	private final static DSConnection ds = DSConnection.getDefault();

	// fields of the line: POSITION + FIELD_NAME
	/**
	 * 1 - isin
	 */
	private Product security;
	/**
	 * 2 - Ccy code
	 */
	private String currency;
	/**
	 * 3 - Portfolio
	 */
	private Book book;
	/**
	 * 4 - Custodio/agent (mnemonic code)
	 */
	private LegalEntity agent;
	/**
	 * 5 - Account code
	 */
	private Account account;
	/**
	 * 6 - Position type
	 */
	private String positionType;
	/**
	 * 7 - Number of titles
	 */
	private Double quantity;
	/**
	 * 8 - Position value date
	 */
	private JDate positionDate;
	/**
	 * 9 - Timestamp
	 */
	private String timestamp;
	/**
	 * 10 - Sent mode ("0" normal, "1" EOD mode)
	 */
	private String sentMode;

	// END INCOMING ROW FIELDS
	/**
	 * Keeps the original position value in Database
	 */
	private Double originalPosition;
	/**
	 * Allows generating the each row to be responded. key attribute position, value String incoming attribute
	 */
	private final SortedMap<Integer, String> rowResponse;

	/**
	 * Constructor
	 * 
	 * @param columns
	 *            that are going to be parsered
	 * @param rowLog
	 */
	public SantPositionBean(final String[] columns, final PositionLogHelper rowLog) {

		this.rowResponse = new TreeMap<Integer, String>();
		this.originalPosition = -1.0;
		buildPosition(columns, rowLog);
	}

	/**
	 * Constructor (for Unit Test purpose only)
	 * 
	 * @param security
	 * @param currency
	 * @param book
	 * @param agent
	 * @param account
	 * @param positionType
	 * @param quantity
	 * @param positionDate
	 */
	SantPositionBean(Product security, String currency, Book book, LegalEntity agent, Account account,
			String positionType, Double quantity, JDate positionDate, String time, String send) {

		this.security = security;
		this.currency = currency;
		this.book = book;
		this.agent = agent;
		this.account = account;
		this.positionType = positionType; // 6
		this.quantity = quantity;
		this.positionDate = positionDate;
		this.timestamp = time; // 9
		this.sentMode = send; // 10
		this.rowResponse = new TreeMap<Integer, String>();
		this.originalPosition = null;
	}

	/**
	 * Clone method, get a copy
	 */
	@Override
	public SantPositionBean clone() {
		return new SantPositionBean(this.security, this.currency, this.book, this.agent, this.account,
				this.positionType, this.quantity, this.positionDate, this.timestamp, this.sentMode);
	}

	/**
	 * @return a response row with the next format
	 *         XS0414704451|FO_BONOS|DUMMY_AGENT|1@DUMMY|30/07/2013|2013-06-20-18.02.26.155321|00 The last field is 00
	 *         if the row is OK, != from 00 in other case
	 */
	public String getRowResponse(final PositionLogHelper rowLog) {

		final StringBuffer sb = new StringBuffer();

		for (String f : this.rowResponse.values()) {
			sb.append(f).append(RESPONSE_SEPARATOR);
		}

		// at least one error, return whatever different from 00
		if (rowLog.requestResendMessage()) {

			sb.append(rowLog.getLinesParserStatuses().get(0).getType().getResponseValue());
		} else { // no errors, row is ok

			sb.append(RESPONSES_CODES.ACK_OK.getResponseValue());
		}

		return sb.toString();
	}

	/**
	 * Builds a position bean, checking format and static data (if possible). If any errors are produced, then the
	 * linePositionBeanParserStatus will not be empty
	 * 
	 * @param columns
	 *            to be parsered
	 * @param rowLog
	 */
	private void buildPosition(final String[] columns, final PositionLogHelper rowLog) {
		int i = 0;
		String isin = columns[i++];
		buildCurrency(columns[i++], rowLog);
		buildBook(columns[i++], rowLog);
		buildAgent(columns[i++], rowLog);
		buildAccount(columns[i++], rowLog);
		buildPositionType(columns[i++], rowLog);
		buildQuantity(columns[i++], rowLog);
		buildPositionDate(columns[i++], rowLog);
		buildTimestamp(columns[i++]);
		buildSentMode(columns[i++], rowLog);
		// Build at the end as I need the ccy in case we have several products for same ISIN
		buildSecurity(isin, rowLog);
	}

	/**
	 * @param isin
	 *            to be parsered
	 * @param rowLog
	 */
	@SuppressWarnings("unchecked")
	private void buildSecurity(final String isin, final PositionLogHelper rowLog) {

		// added to build the reponse line
		this.rowResponse.put(1, isin);
		Vector<Product> securities = null;

		try {

			securities = ds.getRemoteProduct().getProductsByCode("ISIN", isin);

		} catch (RemoteException e) {

			rowLog.addLineStatus("ISIN DB error", RESPONSES_CODES.ERR_DB, e.getLocalizedMessage());
			// DO NOTHING
			return;
		}
		// no securities for that isin
		if (securities == null) {
			rowLog.addLineStatus("ISIN", RESPONSES_CODES.ERR_ISIN, "Security ISIN = " + isin + ", Currency = "
					+ this.currency + " Not Found.");
			return;
		}

		// Found, clean others Products like BOAdjustment and check only bonds
		Vector<Product> bonds = new Vector<Product>(securities.size());

		for (Product p : securities) {

			// GSM: 03/12/2013 -> fix to allow BondAssetBacked
			if (p instanceof Bond) {

				bonds.add(p);
			}
			// if (p.getType().equals(Product.BOND)) {
			// bonds.add(p);
			// }
		}
		// no bonds
		if (Util.isEmpty(bonds)) {
			rowLog.addLineStatus("ISIN", RESPONSES_CODES.ERR_ISIN, "Security ISIN = " + isin + " Not Found.");
			return;

			// more than one
		} else if (bonds.size() > 1) {
			rowLog.addLineStatus("ISIN", RESPONSES_CODES.ERR_ISIN, "More than one Bond found for ISIN = " + isin + ".");
			return;
			// one found
		} else if (bonds.size() == 1) {

			final Product tempProd = bonds.get(0);
			// check ccy
			if (tempProd.getCurrency().equals(this.currency)) {
				this.security = bonds.get(0);
				return;
			} else {
				rowLog.addLineStatus("ISIN", RESPONSES_CODES.ERR_ISIN, "One bond found for the ISIN = " + isin
						+ ", but it has a different Currency = " + this.currency + ".");
			}
		}

		// no securities for that isin
		if (this.security == null) {
			rowLog.addLineStatus("ISIN", RESPONSES_CODES.ERR_ISIN, "Security ISIN = " + isin + ", Currency = "
					+ this.currency + " Not Found.");
			return;
		}
	}

	/**
	 * @param ccy
	 *            to be added to the bean
	 * @param rowLog
	 */
	private void buildCurrency(final String ccy, PositionLogHelper rowLog) {

		this.currency = ccy;

		if (LocalCache.getCurrencyDefault(ccy) == null) {

			rowLog.addLineStatus("CCY", RESPONSES_CODES.ERR_CCY, "Currency = " + this.currency + " Not Found.");
		}
	}

	/**
	 * @param bookName
	 *            to be found
	 * @param rowLog
	 */
	public void buildBook(final String bookName, PositionLogHelper rowLog) {

		// added to build the reponse line
		this.rowResponse.put(2, bookName);

		this.book = BOCache.getBook(ds, bookName);

		if (this.book == null) {
			rowLog.addLineStatus("BOOK", RESPONSES_CODES.ERR_BOOK, "Book = " + bookName + " Not Found.");
		}
	}

	/**
	 * @param agentName
	 * @param rowLog
	 */
	private void buildAgent(final String agentName, final PositionLogHelper rowLog) {

		// added to build the reponse line
		this.rowResponse.put(3, agentName);

		this.agent = BOCache.getLegalEntity(ds, agentName);

		if (this.agent == null) {

			rowLog.addLineStatus("AGENT", RESPONSES_CODES.ERR_CUSTODIO_AGENT, "Agent = " + agentName + " Not Found.");
		}
	}

	/**
	 * Builds de account
	 * 
	 * @param accountName
	 * @param rowLog
	 */
	private void buildAccount(final String accountName, final PositionLogHelper rowLog) {

		// added to build the reponse line
		this.rowResponse.put(4, accountName);
		this.account = null;

		// try to find any account from BOCache for CCY ANY
		if ((getBook() != null) && (getBook().getLegalEntity() != null)) {
			this.account = BOCache.getAccount(ds, accountName, getBook().getLegalEntity().getId(), Account.ANY);

		}

		// if not found: try to find for one currency
		if (this.account == null) {

			if ((getBook() != null) && (getBook().getLegalEntity() != null)) {
				this.account = BOCache.getAccount(ds, accountName, getBook().getLegalEntity().getId(), getCurrency());
			}
			// GSM fix: 02/12/13: allow to use one account with several POs and ANY CCY
			// try for any PO
			if (this.account == null) {
				this.account = BOCache.getAccount(ds, accountName, 0, Account.ANY);

				// Several POs - ONE CCY
				if (this.account == null) {
					this.account = BOCache.getAccount(ds, accountName, 0, getCurrency());
				}
			}
		}
		// try directly from DB
		if (this.account == null) {
			if ((getBook() != null) && (getBook().getLegalEntity() != null)) {
				try {
					this.account = ds.getRemoteAccounting().getAccount(accountName, getBook().getLegalEntity().getId(),
							Account.ANY);
				} catch (RemoteException e) {
					Log.error(this, "DB error accesssing remoteAccounting service. Looking for account name="
							+ accountName);
				}
			}
		}

		// NOT FOUND THE ACCOUNT
		if (this.account == null) {
			buildAccountDetailedLog(accountName, rowLog);

			// the account has to belong the the same custodian (only one account per custodian, but a custodian may
			// have more than 1 account
		} else if ((getAgent() != null) && (this.account.getLegalEntityId() != getAgent().getEntityId())) {
			rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
					+ " Belongs to another Custodian!");
		}
	}

	/**
	 * Detailed Log for account error
	 * 
	 * @param accountName
	 * @param rowLog
	 */
	// GSM: 30/12/2013. Solves the issue I_165_Inventario RF
	@SuppressWarnings("unchecked")
	private void buildAccountDetailedLog(String accountName, PositionLogHelper rowLog) {

		// look for any account for that matches its name

		Vector<Account> accountVector = null;
		try {
			accountVector = ds.getRemoteAccounting().getAccountsByName(accountName);
		} catch (RemoteException e) {
			Log.error(this, "DB error accesssing remoteAccounting service. Looking for account name=" + accountName);
		}

		// any account found for the proposed name
		if ((accountVector == null) || accountVector.isEmpty()) {
			rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
					+ " Name Not Found.");

			// at least it was found an account
		} else {
			// I_165_R, discrepancia de POs
			// ensure accounts belong same custodio and CCY
			accountVector = cleanAccounts(accountVector);

			// more than one account
			if (accountVector.size() > 1) {

				rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
						+ ". More than one account found for the custudio & CCY. heck account configuration.");

				// after clean up
			} else if ((accountVector == null) || accountVector.isEmpty()) {

				rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
						+ " CCY or AGENT doesn't match with the Account ");

				// only one account found
			} else {

				final Account accountTemp = accountVector.get(0);
				// if we have the book and found account (but was not setup properly)
				if (getBook() != null) {

					final Integer lePoBookId = getBook().getLegalEntity().getId();
					final Integer lePoAccId = accountTemp.getProcessingOrgId();

					if (lePoBookId != lePoAccId) {
						rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
								+ ". PO of the book does NOT match PO of the account. Check account configuration.");

					} else {
						rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
								+ ". Account name found but with configuration problems. Check account configuration.");
					}
					// some unknown configuration error
				} else {
					rowLog.addLineStatus("ACCOUNT", RESPONSES_CODES.ERR_ACCOUNT, "Account = " + accountName
							+ ". Account name found but with configuration problems. Check account configuration.");
				}
			}
		}
	}

	/**
	 * Ensure each account of the Vector has the same CCY and custodio than the position
	 * 
	 * @param accountVector
	 * @return
	 */
	private Vector<Account> cleanAccounts(final Vector<Account> accountVector) {

		final Vector<Account> aVec = new Vector<Account>(accountVector.size());
		for (Account a : accountVector) {
			if ((a.getCurrency()).equals(getCurrency()) && (a.getLegalEntityId() == getAgent().getId())) {
				aVec.add(a);
			}
		}
		return aVec;
	}

	/**
	 * finds the position type: THEO, ACT, BLOQUEO.
	 * 
	 * @param posType
	 *            to check
	 * @param rowLog
	 */
	private void buildPositionType(final String posType, final PositionLogHelper rowLog) {

		if (posType.startsWith(ACTUAL_MAPPING)) {
			this.positionType = ACTUAL;

		} else if (posType.startsWith(THEORETICAL_MAPPING)) {
			this.positionType = THEORETICAL;

		} else if (posType.startsWith(BLOQUEO_MAPPING)) {
			this.positionType = BLOQUEO;

		}
		// ANY ERROR
		if (this.positionType == null) {
			if (rowLog != null) {
				rowLog.addLineStatus("POSITION_TYPE", RESPONSES_CODES.ERR_POSITION_TYPE, "PositionType = " + posType
						+ " Not Found.");
			}
		}
	}

	public void setPositionType(final String newPositionType) {
		buildPositionType(newPositionType, null);
	}

	/**
	 * @param qty
	 *            position amount quantity to be retrieved
	 * @param rowLog
	 */
	private void buildQuantity(final String qty, final PositionLogHelper rowLog) {

		try {
			this.quantity = Double.valueOf(qty);

			if (BLOQUEO.equals(this.positionType)) {
				this.quantity = -this.quantity;
			}
		} catch (Exception e) {
			// error parsing quantity
			rowLog.addLineStatus("QTY", RESPONSES_CODES.ERR_QTY, "Quantity = " + qty + " Not Valid.");
		}
	}

	/**
	 * @param posDate
	 *            to parser
	 * @param rowLog
	 */
	private void buildPositionDate(final String posDate, final PositionLogHelper rowLog) {

		// added to build the reponse line
		this.rowResponse.put(5, posDate);

		// check date field has a correct format
		if (!datePattern.matcher(posDate).matches()) {
			rowLog.addLineStatus("POS_DATE", RESPONSES_CODES.ERR_POSITION_DATE, "PositionDate = " + posDate
					+ " Not Valid.");
			return;
		}
		//AAP TO BE OPTIMIZED
		SimpleDateFormat sf=new SimpleDateFormat("dd/MM/yyyy");
		try {
			this.positionDate = JDate.valueOf(sf.parse(posDate));
		} catch (ParseException e) {
			Log.error(this.getClass(), "Cannot parser Date " + posDate);
		}

		if (this.positionDate == null) {
			rowLog.addLineStatus("POS_DATE", RESPONSES_CODES.ERR_POSITION_DATE, "PositionDate = " + posDate
					+ " Not Valid.");
		}
	}

	/**
	 * @param rowLog
	 * @param sent
	 *            mode
	 */
	private void buildSentMode(final String mode, final PositionLogHelper rowLog) {

		this.sentMode = null;

		if (mode.startsWith(NORMAL_MODE)) {
			this.sentMode = NORMAL_MODE;

		} else if (mode.startsWith(EOD_MODE)) {
			this.sentMode = EOD_MODE;
		}
		// ANY ERROR
		if (this.sentMode == null) {

			rowLog.addLineStatus("SENT_MODE", RESPONSES_CODES.ERR_SEND_MODE, "sentMode = " + mode + " Not Found.");
		}
	}

	/**
	 * @param rowLog
	 * @param timestamp
	 *            sent by the Source System (time of the message generation)
	 */
	private void buildTimestamp(final String time) {

		// added to build the reponse line
		this.rowResponse.put(6, time);

		this.timestamp = time;
	}

	/**
	 * In case a row has an incorrect number of fields, this method will try to build the response as "best as possible"
	 * so the
	 * 
	 * @param rowLog
	 */
	public static SantPositionBean tryBuildPosBeanFromIncorrectLine(final String row, PositionLogHelper rowLog) {

		final String[] parts = row.split(FIELDS_SEPARATOR);
		final String[] chunks = new String[FIELDS_NUMBER];

		for (int i = 0; i < chunks.length; i++) {
			chunks[i] = EMPTY;
		}

		for (int i = 0; i < parts.length; i++) {
			chunks[i] = parts[i];
		}

		// if this method is called, we know we have a different number of fields
		rowLog.addLineStatus("ERROR_FIELDS_NUMBER", RESPONSES_CODES.ERR_INCORRECT_NUMBER_FIELDS, "Expected = "
				+ FIELDS_NUMBER + " Fields. Received = " + parts.length);

		return (new SantPositionBean(chunks, rowLog));
	}

	// GETTERS
	/**
	 * @return the isin
	 */
	public Product getSecurity() {
		return this.security;
	}

	/**
	 * @return the ccy
	 */
	public String getCurrency() {
		return this.currency;
	}

	/**
	 * @return the portfolio
	 */
	public Book getBook() {
		return this.book;
	}

	/**
	 * @return the custodian/agent
	 */
	public LegalEntity getAgent() {
		return this.agent;
	}

	/**
	 * @return the account
	 */
	public Account getAccount() {
		return this.account;
	}

	/**
	 * @return the pos type
	 */
	public String getPositionType() {
		return this.positionType;
	}

	/**
	 * @return the number of titles
	 */
	public Double getQuantity() {
		return this.quantity;
	}

	/**
	 * @return position value date
	 */
	public JDate getPositionDate() {
		return this.positionDate;
	}

	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return this.timestamp;
	}

	/**
	 * @return the sentMode
	 */
	public String getSentMode() {
		return this.sentMode;
	}

	/**
	 * @param newQty
	 *            number of titles
	 */
	public void updateQuantity(double newQty) {
		this.quantity = newQty;
	}

	/**
	 * @return unique key for this position bean
	 */
	public String getPositionKey() {
		return new StringBuilder().append(getSecurity().getId()).append(getCurrency()).append(getBook().getId())
				.append(getAgent().getId()).append(getAccount().getId()).append(getPositionType())
				.append(getTimestamp()).append(getSentMode()).toString();
	}

	// AT0000383872|EUR|DUMMY_BOOK|MGBE|00000000000000000000000000000091100|ACT|+100|22/10/2013|2013-10-22-09.58.33.713610|0
	/**
	 * @return unique key for this position bean
	 */
	public String getBeanKey() {
		return new StringBuilder().append(getSecurity().getId()).append(RESPONSE_SEPARATOR).append(getCurrency())
				.append(RESPONSE_SEPARATOR).append(getBook().getId()).append(RESPONSE_SEPARATOR)
				.append(getAgent().getId()).append(RESPONSE_SEPARATOR).append(getAccount().getId())
				.append(RESPONSE_SEPARATOR).append(getPositionType()).append(RESPONSE_SEPARATOR).append(getQuantity())
				.append(RESPONSE_SEPARATOR).append(getPositionDate()).append(RESPONSE_SEPARATOR).append(getTimestamp())
				.append(RESPONSE_SEPARATOR).append(getSentMode()).toString();
	}

	/**
	 * @param other
	 *            bean to be compared
	 * @return true if both positions are the almost the same but differing in position type, amount, timestamp and
	 *         sentMode
	 */
	public boolean equalWithDiffType(SantPositionBean other) {

		return (this.security.equals(other.getSecurity()) && this.currency.equals(other.getCurrency())
				&& this.book.equals(other.getBook()) && this.agent.equals(other.getAgent())
				&& this.account.equals(other.getAccount()) && this.positionDate.equals(other.getPositionDate()));
	}

	/**
	 * original position value in Database, initially -1.0 (not update it).
	 * 
	 * @param originalPos
	 */
	public void setOriginalPosition(final Double originalPos) {

		this.originalPosition = originalPos;
	}

	/**
	 * @return original position value in Database
	 */
	public Double getOriginalPosition() {

		return this.originalPosition;
	}

}

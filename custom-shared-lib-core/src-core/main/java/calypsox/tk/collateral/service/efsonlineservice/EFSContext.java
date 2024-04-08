/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.PricesContextInterface;

/**
 * This class acts as a synchronized communication between the different threads involved in the EFS online WS (to
 * retrive real-time prices for bonds and quotes).
 * 
 * @author Guillermo Solano
 * @version 1.1, 12/06/2013
 * 
 */
public class EFSContext implements PricesContextInterface {

	/**
	 * The only instance that is going to be used to ensure its unique.
	 */
	protected static EFSContext singletonInstanceEFSContext;

	// Variables
	/**
	 * Instrument subscription list
	 */
	protected List<QuoteBeanIN> bondsQuotes;
	protected List<QuoteBeanIN> equitiesQuotes;
	/**
	 * Last instrument quotes recieved
	 */
	protected List<QuoteBeanOUT> quotesResponse;
	/**
	 * String with the list of isins+ccy which have been received from EFS. This is used in the log information
	 */
	private String receivedISINList;
	/**
	 * String with the list of isins+ccy which have been received requested to EFS but has not been received in the WS
	 * call. This is used in the log information
	 */
	private String missingISINList;
	/**
	 * Sleep time for each thread. Set up in the context
	 */
	private static int SLEEP_READ_POS = 30;
	private static int SLEEP_WEBSERVICE = 4;
	private static int SLEEP_WRITE_DB = 20;

	/**
	 * Private constructor to prohibit instantiation of this class. Just can be called once.
	 */
	private EFSContext() {

		this.bondsQuotes = Collections.synchronizedList(new ArrayList<QuoteBeanIN>());
		this.equitiesQuotes = Collections.synchronizedList(new ArrayList<QuoteBeanIN>());
		this.quotesResponse = null;
		this.receivedISINList = this.missingISINList = "";
	}

	/**
	 * @return the unique-instance of this class.
	 */
	public static EFSContext getEFSInstance() {

		if (singletonInstanceEFSContext == null) {
			synchronized (EFSContext.class) {
				singletonInstanceEFSContext = new EFSContext();
			}
		}
		return singletonInstanceEFSContext;
	}

	/**
	 * Resets main data
	 */
	public void resetData() {

		this.bondsQuotes = Collections.synchronizedList(new ArrayList<QuoteBeanIN>());
		this.equitiesQuotes = Collections.synchronizedList(new ArrayList<QuoteBeanIN>());
		this.quotesResponse = null;
	}

	/**
	 * @return the bonds and equities subscription list
	 */
	@Override
	public List<QuoteBeanIN> getQuotesSubscription() {

		int size = 0;

		size += this.bondsQuotes.size() + this.equitiesQuotes.size() + 1;

		final List<QuoteBeanIN> quotesSubs = new ArrayList<QuoteBeanIN>(size);
		quotesSubs.addAll(this.bondsQuotes);
		quotesSubs.addAll(this.equitiesQuotes);

		return quotesSubs;

	}

	/**
	 * @return the quotesResponses (the last one on the context).
	 */
	@Override
	public List<QuoteBeanOUT> getQuotesResponse() {

		int size = 0;

		if ((this.quotesResponse == null) || this.quotesResponse.isEmpty()) {
			return null;
		}

		size += this.quotesResponse.size() + 1;
		final List<QuoteBeanOUT> l = new ArrayList<QuoteBeanOUT>(size);
		l.addAll(this.quotesResponse);

		return l;
	}

	/**
	 * @param the
	 *            bondsQuotes to set. These are the bonds subscriptions to be sent to the WS of EFS.
	 */
	@Override
	public synchronized void setBondsQuotes(final List<QuoteBeanIN> bondsQuotes) {

		this.bondsQuotes = Collections.synchronizedList(bondsQuotes);
	}

	/**
	 * @param equitiesQuotes
	 *            the equitiesQuotes to set. These are the equities subscriptions to be sent to the WS of EFS.
	 */
	@Override
	public synchronized void setEquitiesQuotes(final List<QuoteBeanIN> equitiesQuotes) {

		this.equitiesQuotes = Collections.synchronizedList(equitiesQuotes);
	}

	/**
	 * @param instrumentQuotes
	 *            to be subcribe. Internally makes the difference of bonds or equities subscriptions.
	 */
	@Override
	public synchronized void setInstrumentsQuotes(final List<QuoteBeanIN> instrumentQuotes) {

		final List<QuoteBeanIN> bondsList = new ArrayList<QuoteBeanIN>(instrumentQuotes.size());
		final List<QuoteBeanIN> equitiesList = new ArrayList<QuoteBeanIN>(instrumentQuotes.size());

		for (QuoteBeanIN quote : instrumentQuotes) {

			if (quote.getType().equals(QuoteBeanOUT.QUOTE_TYPE.BOND)) {
				bondsList.add(quote);
			} else {
				equitiesList.add(quote);
			}
		}
		setBondsQuotes(bondsList);
		setEquitiesQuotes(equitiesList);
		// GSM:
	}

	/**
	 * @param quotesResponse
	 *            the quotes to be set (last one read from EFS throught the WebService)
	 */
	@Override
	public synchronized void setQuotesResponse(List<QuoteBeanOUT> quotesResponse) {

		this.quotesResponse = Collections.synchronizedList(quotesResponse);
		// GSM: 27/10/2013: To avoid unwanted sync between thread, in this point is generated
		// the difference between requested prices and received, used in the log.
		buildListReceivedSubscriptions();
		buildListPendingSubscriptions();
	}

	/**
	 * @return the sLEEP_READ_POS
	 */
	public static int getReadPositionsSleepTime() {
		return SLEEP_READ_POS;
	}

	/**
	 * @param sLEEP_READ_POS
	 *            the sLEEP_READ_POS to set
	 */
	public static void setReadPositionsSleepTime(int sLEEP_READ_POS) {
		SLEEP_READ_POS = sLEEP_READ_POS;
	}

	/**
	 * @return the sLEEP_WEBSERVICE
	 */
	public static int getWebServiceSleepTime() {
		return SLEEP_WEBSERVICE;
	}

	/**
	 * @param sLEEP_WEBSERVICE
	 *            the sLEEP_WEBSERVICE to set
	 */
	public static void setWebServiceSleepTime(int sLEEP_WEBSERVICE) {
		SLEEP_WEBSERVICE = sLEEP_WEBSERVICE;
	}

	/**
	 * @return the sLEEP_WRITE_DB
	 */
	public static int getPersistanceQuotesSleepTime() {
		return SLEEP_WRITE_DB;
	}

	/**
	 * @param sLEEP_WRITE_DB
	 *            the sLEEP_WRITE_DB to set
	 */
	public static void setPersistanceQuotesSleepTime(int sLEEP_WRITE_DB) {
		SLEEP_WRITE_DB = sLEEP_WRITE_DB;
	}

	/**
	 * @returns number of quotes to subscribe, number of quotes prices received and list of subscriptions without a
	 *          response
	 */
	public String getStatusInfo() {

		StringBuffer sb = new StringBuffer();
		Integer send = getQuotesRequested();
		Integer read = getQuotesReceived();

		sb.append("<-------> PRICES SUBSCRIPTIONS STATUS <------>\n");
		sb.append("Number of quotes requested: ");
		sb.append(send).append("\n");
		sb.append("Number of quotes prices received: ");
		sb.append(read).append("\n");
		sb.append("Number of quotes without prices: ");
		sb.append(send - read).append("\n");
		sb.append("----------------------------------------------\n");
		sb.append("<------> PRODUCT PRICES LIST RECEIVED <------>\n");
		sb.append(getStringReceivedSubscriptions());
		sb.append("\n");
		sb.append("<----> PRODUCT PRICES LIST NOT RECEIVED <---->\n");
		sb.append(getStringPendingSubscriptions());

		return sb.toString();
	}

	/*
	 * generates a string with the list of received quotes
	 */
	private void buildListReceivedSubscriptions() {

		final List<QuoteBeanOUT> readList = getQuotesResponse();
		final StringBuffer sb = new StringBuffer();
		Integer num = 0;

		if (readList == null) {
			this.receivedISINList = "No WS response received yet";
		}

		for (QuoteBeanOUT res : readList) { // pending subscriptions

			num++;
			sb.append(res.toString()).append(", ");
			if ((num % 7) == 0) {
				sb.append("\n");
			}
		}

		this.receivedISINList = sb.toString();

	}

	/*
	 * returns the String list of received quotes.
	 */
	private String getStringReceivedSubscriptions() {

		return this.receivedISINList;
	}

	/*
	 * Compares subscriptions and receptions. Based on this data, generates the String list of pending quotes.
	 */
	private void buildListPendingSubscriptions() {

		final List<QuoteBeanIN> readList = getQuotesSubscription();
		final Map<String, QuoteBeanIN> map = new HashMap<String, QuoteBeanIN>();

		if ((readList == null) || readList.isEmpty()) {
			this.missingISINList = "Quotes Subscriptions has not been filled by read Positions thread 2";
		}

		for (QuoteBeanIN ask : readList) {

			final String code = ask.getISIN() + ask.getCurrency();
			map.put(code, ask);
		}

		final List<QuoteBeanOUT> recList = getQuotesResponse();

		if ((recList == null) || recList.isEmpty()) {
			this.missingISINList = "Quotes Prices has not been filled by the WS thread 1";
		}

		for (QuoteBeanOUT rec : recList) {

			final String code = rec.getISIN() + rec.getCurrency();
			map.remove(code);
		}

		if (map.isEmpty()) {
			this.missingISINList = "";
		}

		final StringBuffer sb = new StringBuffer();
		Integer num = 0;

		for (QuoteBeanIN res : map.values()) { // pending subscriptions

			num++;
			sb.append(res.toString()).append(", ");
			if ((num % 7) == 0) {
				sb.append("\n");
			}
		}

		this.missingISINList = sb.toString();

	}

	/*
	 * return the list of isins requested to EFS but not received through the WS.
	 */
	private String getStringPendingSubscriptions() {

		return this.missingISINList;
	}

	/*
	 * @return the number of quotes prices responses
	 */
	public Integer getQuotesReceived() {

		Integer i = 0;
		if (this.quotesResponse != null) {
			i = this.quotesResponse.size();
		}

		return i;
	}

	/*
	 * returns the number of quotes subscriptions
	 */
	public Integer getQuotesRequested() {

		Integer i = 0;
		if (this.bondsQuotes != null) {
			i += this.bondsQuotes.size();
		}

		if (this.quotesResponse != null) {
			i += this.equitiesQuotes.size();
		}

		return i;
	}

	/**
	 * Resets the singleton class (all stored data)
	 */
	public static void resetPreviousCachedData() {

		getEFSInstance().resetData();

	}

}

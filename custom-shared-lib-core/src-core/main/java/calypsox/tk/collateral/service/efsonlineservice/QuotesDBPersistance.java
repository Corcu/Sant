package calypsox.tk.collateral.service.efsonlineservice;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT.QUOTE_TYPE;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.QuotesDBPersistanceInterface;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ThreadPool;

/**
 * Reads the last quotes processed from EFS and stores the quotes in Calypso DB.
 * 
 * @author Guillermo Solano
 * @version 1.2, 02/01/2014, Distinguish Price source Reuters & Bloomberg
 * @see QuotesDBPersistanceInterface
 * 
 */
public class QuotesDBPersistance extends ThreadController<String> implements QuotesDBPersistanceInterface {

	/*
	 * private Constants
	 */
	private static final String QUOTE_SET_MIDPRICE = "MidPrice_Intraday";
	/*
	 * quote date (today)
	 */
	private final static JDate today = JDate.getNow();
	/*
	 * Source Name
	 */
	private static final String EFS = "EFS";
	/*
	 * Price Sources
	 */
	private static final String BLOOMBERG = "Bloomberg";
	private static final String REUTERS = "Reuters";
	/*
	 * Save quotes in a Multithread mode = true
	 */
	private static final boolean MULTITHREAD = true;

	/**
	 * Access to the context.
	 */
	private final EFSContext context;

	/**
	 * Generic Constructor, multithreading activated
	 */
	public QuotesDBPersistance() {

		this(true);
	}

	/**
	 * Main Constructor
	 * 
	 * @param true to be executed as an independent thread.
	 */
	public QuotesDBPersistance(boolean threadOriented) {

		super(threadOriented);
		this.context = EFSContext.getEFSInstance();
		super.setSleepTime(EFSContext.getReadPositionsSleepTime());
	}

	/**
	 * Reads from the cache (context) the last prices received from the webservice and stores the quotes in the
	 * quoteSet. Matches interface with ThreadController.
	 */
	@Override
	public void updateDatabase() {

		load();
	}

	/**
	 * Main method of ThreadController, contains the main logic to be executed under the thread.
	 */
	@Override
	public void runThreadMethod() {

		Log.info(QuotesDBPersistance.class, "writeDb thread started.");

		do {

			// store the quotes on DB if the context has received prices, if no goes to sleep

			try {

				// Reads the last quotes from context and stores the quotes in Calypso DB.
				makeQuotesPersistance();

				Log.info(QuotesDBPersistance.class, " Time to Process in sec writeDb iteration= " + getRunningTime());

				// increase executions counter
				super.increaseExecutionsCounter();

				// now wait till next iteration
				super.sleepThread();

			} catch (EFSException e) {

				// Exception control and Log
				super.processException(e);

			}

		} while (super.isAlive());

	}

	/**
	 * Stops the thread
	 */
	@Override
	public void killUpdateDataseThread() {

		super.enableThreading = false;
	}

	/**
	 * @return status of the thread. Number of executions and timers
	 */
	@Override
	public String getStatusInfo() {

		StringBuffer sb = new StringBuffer("Thread 3: Write into DB\n");
		sb.append("     ");
		sb.append("Processing time write DB = ");
		sb.append(super.getTimeHoursMinutesSecondsString(getRunningTime())).append("  elapsed time \n");
		// sb.append(String.format("%.2g", getRunningTime() / 60.0)).append(" min\n");
		sb.append("     ");
		sb.append("Number of executions write DB = ");
		sb.append(super.executionsCounter()).append(" times \n");
		sb.append("----------------------------------------------");

		return sb.toString();
	}

	/***************************************************************************************************************/

	/**
	 * @return from the context the last quotes read from the WS (received from EFS).
	 */
	private List<QuoteBeanOUT> getQuotesCache() {

		return (this.context != null ? this.context.getQuotesResponse() : null);
	}

	/**
	 * From the context, get the last quotes read from EFS, and makes this data persistance (save on Calypso's DB).
	 * 
	 * @throws EFSException
	 *             if there are no qoutes in the context or a DB remote error occurs
	 */
	private void makeQuotesPersistance() throws EFSException {

		final List<QuoteBeanOUT> toUpdate = getQuotesCache();

		if (toUpdate == null) {

			throw new EFSException("No quotes in the context yet. Process to sleep.", false, true);
		}

		final List<QuoteValue> quotesValuesList = new Vector<QuoteValue>(toUpdate.size());

		for (QuoteBeanOUT quote : toUpdate) {

			final QuoteValue newQuoteValue = createQuoteValues(quote); // no procesar si es nulo

			if (newQuoteValue != null) {
				quotesValuesList.add(newQuoteValue);
			}
		}
		// GSM: 27/10/2013. Add linear saving mode
		if (MULTITHREAD) {
			saveQuotesPool(quotesValuesList);
		} else {
			saveQuotes(quotesValuesList);
			// linear, inefficient
		}
	}

	/*
	 * saves the new quotes in a linear way.
	 */
	// for testing, very inefficient
	private void saveQuotes(final List<QuoteValue> quotesList) {

		for (QuoteValue quote : quotesList) {

			final Vector<QuoteValue> quotesVector = new Vector<QuoteValue>(1);
			quotesVector.add(quote);

			try {
				@SuppressWarnings("unused")
				boolean status = DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(quotesVector);

			} catch (RemoteException e) {
				Log.warn(QuotesDBPersistance.class,
						"Quote " + quote.toString() + " hasn't change: " + e.getLocalizedMessage());
				Log.warn(this, e); //sonar
			}
		}
	}

	/*
	 * saves the new quotes, using a thread pool to optimize DB insertion.
	 */
	// @SuppressWarnings("unused")
	private void saveQuotesPool(final List<QuoteValue> quotesList) {

		final ThreadPool pool = new ThreadPool(5, this.getClass().getSimpleName());
		// Save quotes by bunch of 100
		final int SQL_COUNT = 100;
		int start = 0;

		for (int i = 0; i <= (quotesList.size() / SQL_COUNT); i++) {

			int end = (i + 1) * SQL_COUNT;

			if (end > quotesList.size()) {

				end = quotesList.size();
			}
			final List<QuoteValue> subList = quotesList.subList(start, end);
			final Vector<QuoteValue> subVector = new Vector<QuoteValue>(subList.size());
			subVector.addAll(subList);

			start = end;
			// threaded pool
			pool.addJob(new Runnable() {

				@Override
				public void run() {

					try {

						DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(subVector);

					} catch (RemoteException e) {
						Log.warn(this, e); //sonar
					}
				}
			});
		} // end for
	}

	/**
	 * @param quote
	 *            read from the context
	 * @return a new quote value to be inserted on DB
	 * @throws EFSException
	 *             if a DB problem ocurred
	 */
	private QuoteValue createQuoteValues(QuoteBeanOUT quote) throws EFSException {

		final Product product = getProductDB(quote);

		if (quote.getType().equals(QUOTE_TYPE.BOND)) {

			if (!(product instanceof Bond)) {
				return null; // should not happen
			}

			final Bond bond = (Bond) product;
			final QuoteValue q = new QuoteValue(QUOTE_SET_MIDPRICE, bond.getQuoteName(), today, bond.getType());
			addPricesToQuote(q, quote);
			q.setSourceName(EFS);
			q.setPriceSourceName(BLOOMBERG);

			return q;

		} else if (quote.getType().equals(QUOTE_TYPE.EQUITY)) {

			if (!(product instanceof Equity)) {
				return null; // should not happen
			}

			final Equity equity = (Equity) product;
			final QuoteValue q = new QuoteValue(QUOTE_SET_MIDPRICE, equity.getQuoteName(), today, equity.getType());
			addPricesToQuote(q, quote);
			q.setSourceName(EFS);
			q.setPriceSourceName(REUTERS);

			return q;
		}

		Log.warn(QuotesDBPersistance.class, "No type match for: " + quote.toString());

		return null;
	}

	/**
	 * 
	 * Adds the ask, bid and last price to the quoteValue
	 * 
	 * @param q
	 *            quoteValue created
	 * @param quote
	 *            read from the context
	 */
	private void addPricesToQuote(QuoteValue q, QuoteBeanOUT quote) {

		if ((q == null) || (quote == null)) {
			return;
		}

		double midPrice = 0.0;
		double ask = quote.getAskPrice();
		double bid = quote.getBidPrice();

		if ((ask != 0.0) && (bid != 0.0)) {
			midPrice = (quote.getAskPrice() + quote.getBidPrice()) / 2.0;
		} else if ((ask == 0.0) && (bid != 00)) {
			midPrice = bid;
		} else if ((bid == 0.0) && (ask != 00)) {
			midPrice = ask;
		}

		q.setAsk(quote.getAskPrice());
		q.setBid(quote.getBidPrice());
		q.setLast(midPrice);
	}

	/**
	 * @param quote
	 *            from the context
	 * @return the product found
	 * @throws EFSException
	 *             if a DB error ocurred.
	 */
	@SuppressWarnings("unchecked")
	private Product getProductDB(QuoteBeanOUT quote) throws EFSException {

		Vector<Product> products = null;
		try {

			products = DSConnection.getDefault().getRemoteProduct().getProductsByCode("ISIN", quote.getISIN());

		} catch (RemoteException e) {
			Log.error(this, e);//sonar
			throw new EFSException("DB Error reading product by ISIN. Thread slept \n" + e.getLocalizedMessage(), false);
		}

		// should not happen, product was read from the Position so it should exist
		if ((products == null) || products.isEmpty()) {

			Log.warn(QuotesDBPersistance.class, "No products for the ISIN: " + quote.getISIN());
		}

		for (Product p : products) {// we might find several products

			if (p.getCurrency().equalsIgnoreCase(quote.getCurrency())) { // if we have same currency quote and
																		 // product...

				if (p.getType().equalsIgnoreCase(QUOTE_TYPE.BOND.name().toLowerCase()) // and is only bond/equity
																					   // product
						|| p.getType().equalsIgnoreCase(QUOTE_TYPE.EQUITY.name().toLowerCase())) {

					return p; // this is the product to return
				}
			}
		}

		Log.warn(QuotesDBPersistance.class,
				"No product currency Match for the ISIN, but was extracted from a position: " + quote.getISIN()
						+ " Currency: " + quote.getCurrency());

		return null;
	}
}

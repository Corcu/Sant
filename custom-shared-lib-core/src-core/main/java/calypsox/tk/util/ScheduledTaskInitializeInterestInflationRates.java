package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;

/**
 * Scheduled Task to initialize Interest & Inflation Rates.
 * 
 * @author David Porras Mart?nez
 */
public class ScheduledTaskInitializeInterestInflationRates extends AbstractProcessFeedScheduledTask {
	private static final long serialVersionUID = 123L;

	private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name";
	private static final String TASK_INFORMATION = "Inicialization of Interest & Inflation rates.";
	@SuppressWarnings("unused")
	private boolean processOK = true;
	protected final HashSet<String> interestInflationQuoteNames = new HashSet<String>();

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

		attributeList.addAll(super.buildAttributeDefinition());
		try {
			attributeList.add(attribute(QUOTENAME_DOMAIN_STRING)
					.domain(new ArrayList<String>(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames())));
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
		}
		return attributeList;
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(QUOTENAME_DOMAIN_STRING);
//		return attr;
//	}
//
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Override
//	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
//		Vector vector = new Vector();
//		if (attribute.equals(QUOTENAME_DOMAIN_STRING)) {
//			try {
//				vector.addAll(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames());
//			} catch (final RemoteException e) {
//				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
//			}
//		} else {
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//
//		return vector;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public boolean process(final DSConnection conn, final PSConnection connPS) {
//
//		JDate jdate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
//		Vector<QuoteValue> result = new Vector<QuoteValue>();
//		final String quoteSetName = getAttribute(QUOTENAME_DOMAIN_STRING);
//
//		// get all quoteNames related to Interest&Inflation rates
//		try {
//			getInterestInflationQuotes(conn.getRemoteMarketData().getAllFeedAddress());
//		} catch (final RemoteException e) {
//			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving Feed Addresses", e);
//			this.processOK = false;
//			return this.processOK;
//		}
//
//		// get quotes and process them. Update quotes if there's no value for
//		// them
//		try {
//			result = setClosePrices(jdate, quoteSetName);
//			if ((result != null) && (result.size() != 0)) {
//				// save quotes
//				DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(result);
//			}
//
//		} catch (Exception e) {
//			this.processOK = false;
//			Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString(), e);
//		}
//
//		return this.processOK;
//	}

	@SuppressWarnings("unchecked")
	public Vector<QuoteValue> setClosePrices(JDate date, String quoteSetName) {

		Vector<QuoteValue> newQuotes = new Vector<QuoteValue>();
		Vector<QuoteValue> checkQuotes = new Vector<QuoteValue>();
		QuoteValue newQuoteValue = null;
		Iterator<String> iterQuotes = this.interestInflationQuoteNames.iterator();
		Double price = 0.0;

		// procesar
		while (iterQuotes.hasNext()) {

			String quoteName = iterQuotes.next();
			StringBuilder query = new StringBuilder();
			query.append("TRUNC(QUOTE_DATE) = TO_DATE('");
			query.append(date);
			query.append("', 'dd/mm/yyyy')");
			query.append(" AND QUOTE_NAME = ");
			query.append(Util.string2SQLString(quoteName));
			query.append(" AND QUOTE_SET_NAME = '");
			query.append(quoteSetName);
			query.append("'");

			try {
				checkQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(query.toString());
				if (checkQuotes != null) {
					if (checkQuotes.isEmpty()) {
						try {
							price = new Double(0.00001 / 100);
						} catch (final NumberFormatException e) {
							Log.error(this, e); //sonar
							return null;
						}
						if (quoteName.startsWith("MM.")) {
							newQuoteValue = new QuoteValue(quoteSetName, quoteName, date, QuoteValue.YIELD);
							newQuoteValue.setClose(price);
						}
						if (quoteName.startsWith("Inflation.")) {
							newQuoteValue = new QuoteValue(quoteSetName, quoteName, date, QuoteValue.PRICE);
							newQuoteValue.setClose(price);
						}

						newQuotes.add(newQuoteValue);
					}
				}
			} catch (RemoteException e) {
				Log.error(this, e); //sonar
				this.processOK = false;
				return null;
			}
		}

		return newQuotes;
	}

	public void getInterestInflationQuotes(Vector<FeedAddress> feeds) {
		if ((null != feeds) && (feeds.size() > 0)) {
			for (int i = 0; i < feeds.size(); i++) {
				String feedQuoteName = feeds.get(i).getQuoteName();
				if ((feedQuoteName.startsWith("MM")) || (feedQuoteName.startsWith("Inflation"))) {
					this.interestInflationQuoteNames.add(feedQuoteName);
				}
			}
		}
		return;
	}

	@Override
	public String getFileName() {
		return "";
	}
}

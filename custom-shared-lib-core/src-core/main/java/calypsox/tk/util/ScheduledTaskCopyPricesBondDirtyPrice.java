package calypsox.tk.util;

import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

/**
 * To import Bond Dirty and Clean Prices from a CSV file.
 *
 * @author David Porras Mart?nez (david.porras@isban.es) & Guillermo Solano
 * @version 2.0, added check Product exists before copy the quote
 * @date 20/04/2015
 */
public class ScheduledTaskCopyPricesBondDirtyPrice extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 123L;

    private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name";
    private static final String TASK_INFORMATION = "Copies day before quotes into today";
    private static final String QUOTE_TYPE = "Quote Type";
    private static final String PRODUCT_TYPE = "Product Type";

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    //v14 Migration
    @SuppressWarnings("unchecked")
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributeList = new ArrayList<>();
        try {
            attributeList.add(attribute(QUOTENAME_DOMAIN_STRING).domain(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames()));
        } catch (CalypsoServiceException e) {
            Log.error(ScheduledTaskCopyPricesBondDirtyPrice.class, "Cannot recover QuoteSets from DS: " + e);
        }
        attributeList.add(attribute(QUOTE_TYPE).domain(Arrays.asList("DirtyPrice", "CleanPrice", "Price", "Yield")));
        attributeList.add(attribute(PRODUCT_TYPE).domain(Arrays.asList("MM", "Bond", "FX", "Inflation", "Equity")));

        return attributeList;
    }

    /**
     * Custom domain attributes
     */
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = new Vector<String>(3);// super.getDomainAttributes();
//		attr.add(QUOTENAME_DOMAIN_STRING);
//		attr.add(QUOTE_TYPE);
//		attr.add(PRODUCT_TYPE);
//
//		return attr;
//	}
//
//	/**
//	 * @param attribute
//	 *            name
//	 * @param hastable
//	 *            with the attributes declared
//	 * @return a vector with the values for the attribute name
//	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
//		Vector vector = new Vector();
//		if (attribute.equals(QUOTENAME_DOMAIN_STRING)) {
//			try {
//				vector.addAll(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames());
//			} catch (final RemoteException e) {
//				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
//			}
//		} else if (attribute.equals(QUOTE_TYPE)) {
//			Collection col = new ArrayList<String>();
//			col.add("DirtyPrice");
//			col.add("CleanPrice");
//			col.add("Price");
//			col.add("Yield");
//			vector.addAll(col);
//		} else if (attribute.equals(PRODUCT_TYPE)) {
//			Collection col = new ArrayList<String>();
//			col.add("MM");
//			col.add("Bond");
//			col.add("FX");
//			col.add("Inflation");
//			col.add("Equity");
//			vector.addAll(col);
//		} else {
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//
//		return vector;
//	}

    /**
     * Main process
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {

        JDate jdate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
        Vector holidays = getHolidays();
        Vector<QuoteValue> quote2copy = new Vector<QuoteValue>();

        Vector<QuoteValue> quotes2Save = new Vector<QuoteValue>();
        final String quoteSetName = getAttribute(QUOTENAME_DOMAIN_STRING);
        final String quoteType = getAttribute(QUOTE_TYPE);
        final String productType = getAttribute(PRODUCT_TYPE);

        String where = "trunc(quote_date) = to_date('" + jdate.addBusinessDays(-1, holidays) + "', 'dd/mm/yyyy') "
                + "AND quote_name like '" + (productType + ".%") + "' AND quote_set_name = '" + quoteSetName + "'";

        // BAU - GSM: 14/04/2015
        try {
            quote2copy = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(where);

        } catch (RemoteException e) {
            Log.error(this, "Error gathering quotes for query: " + where);
            Log.error(this, e); //sonar
            return false;
        }

        // clean quotes for unexistance products
        quote2copy = cleanDeprecatedQuotesNames(quote2copy);

        if (!Util.isEmpty(quote2copy)) {

            quotes2Save = setClosePrices(quote2copy, jdate, quoteSetName, quoteType, productType);

            try {
                if (!Util.isEmpty(quotes2Save)) {
                    DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(quotes2Save);
                }
            } catch (RemoteException e) {
                Log.error(this, "Error saving quotes for day " + jdate);
                Log.error(this, e);//sonar
                return false;
            }
        }
        return true;
    }

    // BAU - GSM: 14/04/2015

    /**
     * @param quotes
     * @return quotes which quoteName is linked to a product
     */
    private Vector<QuoteValue> cleanDeprecatedQuotesNames(final Vector<QuoteValue> quotesToClean) {

        Vector<QuoteValue> quotes = new Vector<QuoteValue>(quotesToClean.size());

        for (QuoteValue q : quotesToClean) {

//			if (productExistForQuote(q)) {
            quotes.add(q);
            //		}
        }
        return quotes;
    }

    /**
     * @param q
     * @return true is the product exists
     */
    @SuppressWarnings("unused")
    private boolean productExistForQuote(final QuoteValue q) {

        Product p = null;
        try {
            p = getProduct(q);
        } catch (RemoteException e) {
            Log.error(this, e.getLocalizedMessage());
            Log.error(this, e); //sonar
        }
        if (p != null) {
            return true;
        }
        return false;

    }

    /**
     * @param qv
     * @return Product corresponding to the bond
     * @throws RemoteException
     */
    @SuppressWarnings("unchecked")
    private Product getProduct(final QuoteValue qv) throws RemoteException {

        final String clausule = "quote_name = " + Util.string2SQLString(qv.getName());
        final Vector<ProductDesc> v = DSConnection.getDefault().getRemoteProduct().getAllProductDesc(clausule, null);

        if (!Util.isEmpty(v)) {

            Product p = DSConnection.getDefault().getRemoteProduct().getProduct(v.get(0).getId());
            if (p != null) {
                return p;
            }
        }

        return null; // should not happen

    }

    @SuppressWarnings("unchecked")
    public Vector<QuoteValue> setClosePrices(Vector<QuoteValue> oldQuotes, JDate date, String quoteSetName,
                                             String quoteType, String productType) {

        Vector<QuoteValue> newQuotes = new Vector<>();
        @SuppressWarnings("unused")
        Vector<QuoteValue> checkQuotes = new Vector<>();

        // procesar
        for (int i = 0; i < oldQuotes.size(); i++) {
            // check quote for today
            String clausule = "trunc(quote_date) = to_date('" + date + "', 'dd/mm/yyyy') " + "AND quote_name = "
                    + Util.string2SQLString(oldQuotes.get(i).getName()) + " AND quote_set_name = '" + quoteSetName
                    + "'";
            try {
                checkQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                //	if (checkQuotes != null) {
                //		if (checkQuotes.size()>0) {
                QuoteValue newQuoteValue = new QuoteValue(quoteSetName, oldQuotes.get(i).getName(), date,
                        oldQuotes.get(i).getQuoteType());
                newQuoteValue.setClose(oldQuotes.get(i).getClose());
                newQuotes.add(newQuoteValue);
                //		}
                //	}
            } catch (RemoteException e) {
                Log.error(this, "error gathering quote query: " + clausule);
                Log.error(this, e); //sonar
                return null;
            }

        }

        return newQuotes;
    }

    @Override
    public String getFileName() {
        return "no file";
    }

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.product.BondCustomData;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskCopy_CustomDataHC_To_Quoteset extends ScheduledTask {

    // START OA 27/11/2013
    // Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
    // InvalidClassExceptions.
    // Please refer to Serializable javadoc for more details
    private static final long serialVersionUID = 15447854258L;
    // END OA OA 27/11/2013

    private static final String CUSTOM_DATA_SOURCE = "CustomDataSource";
    private static final String PRICE_TYPE = "PriceType";

    private static final String EUREX_HAIRCUT = "EUREX_HAIRCUT";

//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = new Vector<String>();
//		attr.add(CUSTOM_DATA_SOURCE);
//		attr.add(PRICE_TYPE);
//		return attr;
//	}

    //v14.4 GSM
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(CUSTOM_DATA_SOURCE).domain(Arrays.asList(EUREX_HAIRCUT)));
        attributeList.add(attribute(PRICE_TYPE));

        return attributeList;
    }

//	@SuppressWarnings("rawtypes")
//	@Override
//	public Vector<String> getAttributeDomain(final String attribute, final Hashtable hashtable) {
//		Vector<String> vector = new Vector<String>();
//		if (CUSTOM_DATA_SOURCE.equals(attribute)) {
//			vector.add(EUREX_HAIRCUT);
//		}
//		return vector;
//	}

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean retVal = super.isValidInput(messages);
        return retVal;
    }

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        boolean result = false;

        try {
            result = saveHaircutAsQuoteValues();
        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private boolean saveHaircutAsQuoteValues() throws CalypsoServiceException {
        JDate valDate = getValuationDatetime(true).getJDate(TimeZone.getDefault());
        PricingEnv pricingEnv = getDSConnection().getRemoteMarketData().getPricingEnv(getPricingEnv(),
                getValuationDatetime(true));
        String quoteSetName = pricingEnv.getQuoteSetName();

        Map<String, QuoteValue> existingQuotes = getQuotesMap(valDate, quoteSetName);

        Vector<QuoteValue> quotevalues = new Vector<>();
        Collection<Bond> bonds = loadBondsWithCustomData();
        for (Bond bond : bonds) {
            Double haircutValue = getCustomDataSpecificValue(bond, getAttribute(CUSTOM_DATA_SOURCE));
            if (haircutValue != null) {
                // Need to devide by 100 as this is a percentage
                haircutValue = haircutValue / 100.0;

                QuoteValue qv = existingQuotes.get(bond.getQuoteName());
                if (qv == null) {
                    qv = new QuoteValue();
                    qv.setName(bond.getQuoteName());
                    qv.setDate(valDate);
                    qv.setQuoteSetName(quoteSetName);
                }

                qv.setQuoteType(getAttribute(PRICE_TYPE));
                qv.setAsk(haircutValue);
                qv.setBid(haircutValue);
                qv.setClose(haircutValue);
                qv.setOpen(haircutValue);
                qv.setHigh(haircutValue);
                qv.setLow(haircutValue);
                qv.setLast(haircutValue);
                quotevalues.add(qv);
            }
        }

        return getDSConnection().getRemoteMarketData().saveQuoteValues(quotevalues);
    }

    private Double getCustomDataSpecificValue(Bond bond, String customDataSource) {
        BondCustomData customData = (BondCustomData) bond.getCustomData();
        if (customData != null && customDataSource.equals(EUREX_HAIRCUT) && (customData.getHaircut_eurex() != null)) {
            return customData.getHaircut_eurex();
        }
        return null;
    }

    private Collection<Bond> loadBondsWithCustomData() throws CalypsoServiceException {
        Map<String, Bond> bondsMap = new HashMap<>();
        String where = " product_desc.product_family='Bond' "
                + "and exists (select 1 from bond_custom_data where bond_custom_data.product_id=product_desc.product_id)";
        Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(null, where, null);
        for (Bond bond : allBonds) {
            if (bond.getCustomData() != null) {
                bondsMap.put(bond.getSecCode("ISIN"), bond);
            }
        }

        return bondsMap.values();
    }

    @SuppressWarnings("unchecked")
    private Map<String, QuoteValue> getQuotesMap(JDate valDate, String quoteSetName) throws CalypsoServiceException {
        Map<String, QuoteValue> map = new HashMap<>();
        Vector<QuoteValue> quoteValues = getDSConnection().getRemoteMarketData().getQuoteValues(valDate, quoteSetName);

        for (QuoteValue quote : quoteValues) {
            map.put(quote.getName(), quote);
        }

        return map;
    }

    @Override
    public String getTaskInformation() {
        return "Imports Bond REF_INTERNA, CUSIP, SEDOL from an asset control.";
    }

}

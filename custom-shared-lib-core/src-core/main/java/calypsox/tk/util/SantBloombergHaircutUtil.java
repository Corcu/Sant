package calypsox.tk.util;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.factory.QuoteTypeEnum;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.service.DSConnection;

import java.util.*;

// Project: Bloomberg tagging
// Project: Bloomberg tagging. Release 2

/**
 * SantBloombergHaircutUtil
 *
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Jos? Luis F. Luque <joseluis.f.luque@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com>
 */
public class SantBloombergHaircutUtil {

    private static final String BLOOMBERG_HAIRCUT_SEC_CODES_DOMAIN_NAME = "BLOOMBERG_HAIRCUT_SEC_CODES";

    private static final int QUOTE_VALUES_SAVE_SIZE = 1000;

    private static SantBloombergHaircutUtil instance = null;
    private Map<String, String> secCodesAndQuoteSetsMap = null;

    private SantBloombergHaircutUtil() {
        secCodesAndQuoteSetsMap = getHaircutSecCodesAndQuotes();
    }

    public static synchronized SantBloombergHaircutUtil getInstance() {
        if (instance == null) {
            instance = new SantBloombergHaircutUtil();
        }

        return instance;
    }

    public static void setInstance(SantBloombergHaircutUtil instance) {
        SantBloombergHaircutUtil.instance = instance;
    }

    public void updateAllHaircutQuotes(String productType, JDate quoteDate) {
        List<QuoteValue> quoteValues = new LinkedList<>();

        // Delete quotes if they exist
        // deleteAllQuotes(quoteDate);

        List<Product> allProducts = getAllProducts(productType);
        for (Product product : allProducts) {
            List<QuoteValue> partialQuoteValues = buildHaircutQuotesValues(
                    product, quoteDate);
            quoteValues.addAll(partialQuoteValues);
        }

        List<QuoteValue> quoteValuesChunk = new LinkedList<QuoteValue>();
        for (QuoteValue quoteValue : quoteValues) {
            quoteValuesChunk.add(quoteValue);
            if (quoteValuesChunk.size() >= QUOTE_VALUES_SAVE_SIZE) {
                saveQuoteValues(quoteValuesChunk);
                quoteValuesChunk.clear();
            }
        }

        if (!quoteValuesChunk.isEmpty()) {
            saveQuoteValues(quoteValuesChunk);
        }

    }

    public void updateOneOrSeveralHaircutQuotes(String securityCodes,
                                                JDate quoteCode) {
        Collection<String> isinCodes = SantBloombergUtil
                .getIsinCodes(securityCodes);
        for (String isinCode : isinCodes) {
            updateHaircutQuotes(isinCode, quoteCode);
        }
    }

    public void updateHaircutQuotes(String isin, JDate quoteDate) {
        try {
            Product product = DSConnection.getDefault().getRemoteProduct()
                    .getProductByCode(SecCode.ISIN, isin);
            List<QuoteValue> quoteValues = buildHaircutQuotesValues(product,
                    quoteDate);
            saveQuoteValues(quoteValues);
        } catch (CalypsoServiceException e) {
            Log.error(this, String.format(
                    "Cannot retrieve product with ISIN \"%s\"", isin), e);
        }
    }

    List<QuoteValue> buildHaircutQuotesValues(Product product,
                                              JDate quoteDate) {
        Map<String, String> haircutValues = getHaircutValues(product);

        List<QuoteValue> quotes = new LinkedList<QuoteValue>();
        for (String secCode : haircutValues.keySet()) {
            String valueString = haircutValues.get(secCode);
            if (isDoubleValue(valueString)) {
                double value = getHaircutValue(valueString);
                String quoteSetName = getHaircutSecCodesAndQuotes()
                        .get(secCode);
                QuoteValue quoteValue = buildHaircutQuoteValue(quoteSetName,
                        quoteDate, product, value);
                quotes.add(quoteValue);
            }
        }

        return quotes;
    }

    private Map<String, String> getHaircutSecCodesAndQuotes() {
        if (secCodesAndQuoteSetsMap == null) {
            secCodesAndQuoteSetsMap = new HashMap<String, String>();

            try {
                List<DomainValuesRow> domainValuesRows = DSConnection
                        .getDefault().getRemoteReferenceData()
                        .getDomainValuesRows(
                                BLOOMBERG_HAIRCUT_SEC_CODES_DOMAIN_NAME);
                for (DomainValuesRow row : domainValuesRows) {
                    secCodesAndQuoteSetsMap.put(row.getValue(),
                            row.getComment());
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, e);// Sonar
            }
        }

        return secCodesAndQuoteSetsMap;
    }

    private Map<String, String> getHaircutValues(Product product) {
        Map<String, String> haircutValues = new HashMap<String, String>();

        Set<String> secCodes = secCodesAndQuoteSetsMap.keySet();
        for (String secCode : secCodes) {
            haircutValues.put(secCode, product.getSecCode(secCode));
        }

        return haircutValues;
    }

    private void saveQuoteSet(String quoteSetName) {
        try {
            QuoteSet quoteSet = new QuoteSet(quoteSetName);
            DSConnection.getDefault().getRemoteMarketData().saveQuoteSet(
                    quoteSet, DSConnection.getDefault().getUser());
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    String.format("Cannot save Quote Set \"%s\"", quoteSetName),
                    e);
        }
    }

    private boolean isDoubleValue(String secCodeValue) {
        boolean doubleValue = false;

        if (!Util.isEmpty(secCodeValue)) {
            try {
                Double.valueOf(secCodeValue);
                doubleValue = true;
            } catch (NumberFormatException e) {
                doubleValue = false;
            }
        }

        return doubleValue;
    }

    private double getHaircutValue(String secCodeValue) {
        double doubleValue = 0.0;

        if (!Util.isEmpty(secCodeValue)) {
            try {
                doubleValue = Double.valueOf(secCodeValue);
                // Don't divide haircut value by 100
                doubleValue = -doubleValue;
            } catch (NumberFormatException e) {
                Log.error(this, String.format("Cannot parse \"%s\" as double",
                        secCodeValue), e);
            }
        }

        return doubleValue;
    }

    private QuoteValue buildHaircutQuoteValue(String quoteSetName,
                                              JDate quoteDate, Product product, double closeValue) {
        // Use Quote Type "Price"
        QuoteValue quoteValue = new QuoteValue(quoteSetName,
                product.getQuoteName(), quoteDate,
                QuoteTypeEnum.PRICE.getName());

        try {
            QuoteValue existingQuoteValue = DSConnection.getDefault()
                    .getRemoteMarketData().getQuoteValue(quoteValue);
            if (existingQuoteValue != null) {
                DSConnection.getDefault().getRemoteMarketData()
                        .remove(existingQuoteValue);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    String.format(
                            "Could not retrieve quote value from database: %s",
                            quoteValue.toString()),
                    e);
        }

        // Set all attributes to the same value
        quoteValue.setAsk(closeValue);
        quoteValue.setBid(closeValue);
        quoteValue.setOpen(closeValue);
        quoteValue.setClose(closeValue);
        quoteValue.setHigh(closeValue);
        quoteValue.setLow(closeValue);
        quoteValue.setLast(closeValue);

        return quoteValue;
    }

    private List<Product> getAllProducts(String productType) {
        List<Product> allProducts = new LinkedList<>();

        String fromClause = "product_sec_code psc";
        StringBuilder whereBuilder = new StringBuilder();
        List<CalypsoBindVariable> bindVariableList = new ArrayList<>();
        whereBuilder.append(
                "psc.product_id = product_desc.product_id AND psc.sec_code IN (");
        // MIG V16
        //whereBuilder.append(
        //        Util.collectionToSQLString(secCodesAndQuoteSetsMap.keySet()));
        whereBuilder.append(CustomBindVariablesUtil.collectionToPreparedInString(secCodesAndQuoteSetsMap.keySet(), bindVariableList) + ")");
        whereBuilder.append(" AND psc.code_value <> 'N.A.'");
        whereBuilder.append(" AND product_desc.product_family = '");
        whereBuilder.append(productType);
        whereBuilder.append('\'');
        String whereClause = whereBuilder.toString();
        try {
            Vector<?> rawAllProducts = DSConnection.getDefault()
                    .getRemoteProduct().getAllProducts(fromClause, whereClause, bindVariableList);
            for (Object rawProduct : rawAllProducts) {
                if (rawProduct instanceof Product) {
                    allProducts.add((Product) rawProduct);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    String.format(
                            "Cannot retrieve all products using:\nFROM: %s\nWHERE: %s",
                            fromClause, whereClause),
                    e);
        }

        return allProducts;
    }

    private void saveQuoteValues(List<QuoteValue> quoteValues) {
        try {
            // Before trying to save any Quote Values, make sure every Quote Set
            // we need is present in the system.
            saveMissingQuoteSets(quoteValues);

            // Save quote values
            DSConnection.getDefault().getRemoteMarketData()
                    .saveQuoteValues(new Vector<QuoteValue>(quoteValues));

        } catch (CalypsoServiceException e) {
            saveQuotesOneByOne(quoteValues);
        }
    }

    private void saveQuotesOneByOne(Collection<QuoteValue> quoteValues) {
        for (QuoteValue quoteValue : quoteValues) {
            try {
                QuoteValue quoteValueFromDB = DSConnection.getDefault()
                        .getRemoteMarketData().getQuoteValue(quoteValue);
                if (quoteValueFromDB != null) {
                    DSConnection.getDefault().getRemoteMarketData()
                            .remove(quoteValueFromDB);
                }
                DSConnection.getDefault().getRemoteMarketData()
                        .save(quoteValue);
            } catch (CalypsoServiceException e) {
                String errorMessage = String.format(
                        "Could not save Quote Value \"%s\" [%s]",
                        quoteValue.getName(), quoteValue.getDate());
                Log.error(this, errorMessage, e);
            }
        }
    }

    private void saveMissingQuoteSets(List<QuoteValue> quoteValues) {
        try {
            // Get existing quote sets
            Vector<?> rawQuoteSets = DSConnection.getDefault()
                    .getRemoteMarketData().getQuoteSetNames();
            Map<String, Boolean> quoteSetNames = new HashMap<String, Boolean>();
            for (Object rawQuoteSet : rawQuoteSets) {
                if (rawQuoteSet instanceof String) {
                    quoteSetNames.put((String) rawQuoteSet, Boolean.TRUE);
                }
            }

            // Get quote sets from list of quotes
            Set<String> quoteSetsInList = new HashSet<String>();
            for (QuoteValue quoteValue : quoteValues) {
                quoteSetsInList.add(quoteValue.getQuoteSetName());
            }

            // Save not existing quote sets
            for (String quoteSet : quoteSetsInList) {
                if (!quoteSetNames.containsKey(quoteSet)) {
                    saveQuoteSet(quoteSet);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot retrieve Quote Set names", e);
        }
    }

    protected void deleteAllQuotes(JDate quoteDate) {
        Map<String, String> quoteSetsMap = getHaircutSecCodesAndQuotes();
        for (String quoteSet : quoteSetsMap.values()) {
            StringBuilder where = new StringBuilder();
            where.append("quote_set_name = '");
            where.append(quoteSet);
            where.append("' AND TRUNC(quote_date) = ");
            where.append(Util.date2SQLString(quoteDate));

            try {
                DSConnection.getDefault().getRemoteMarketData()
                        .deleteQuoteValues(null, where.toString(), false, null, null);
            } catch (CalypsoServiceException e) {
                Log.error(this,
                        String.format(
                                "Could not delete existing quote values. WHERE = %s",
                                where.toString()),
                        e);
            }
        }
    }

}

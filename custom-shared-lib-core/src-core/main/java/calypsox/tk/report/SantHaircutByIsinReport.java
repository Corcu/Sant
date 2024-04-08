package calypsox.tk.report;

import calypsox.tk.report.loader.AgreementLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class SantHaircutByIsinReport extends SantReport {

    private static final long serialVersionUID = 123L;

    protected HashMap<Integer, Product> productsMap = new HashMap<>();

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput loadReport(final Vector errorMsgsP) {

        try {
            return getReportOutput(errorMsgsP);
        } catch (RemoteException e) {
            String error = "Error generating Haircut By ISIN Report\n";
            Log.error(this, error, e);
            errorMsgsP.add(error + e.getMessage());
        }
        return null;

    }

    @SuppressWarnings("rawtypes")
    private DefaultReportOutput getReportOutput(final Vector errorMsgsP) throws RemoteException {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<>();

        final ReportTemplate template = getReportTemplate();


        // get list of agreements
        final String agreementIds = (String) template.get(SantHaircutByIssuerReportTemplate.AGREEMENT_ID);

        // get list of securities
        final String securityIds = (String) template.get(SantHaircutByIsinReportTemplate.ISIN_ID);

        // load haircut items
        List<SantHaircutByIsinItem> haircutItems = loadSantHaircutByIsinItems(agreementIds, securityIds, errorMsgsP);

        for (SantHaircutByIsinItem haircutItem : haircutItems) {
            ReportRow row = new ReportRow(haircutItem.getAgreement(), ReportRow.MARGIN_CALL_CONFIG);
            row.setProperty(SantHaircutByIsinReportTemplate.ISSUER, haircutItem.getIssuer());
            row.setProperty(SantHaircutByIsinReportTemplate.PRODUCT, haircutItem.getSecurity());
            row.setProperty(SantHaircutByIsinReportTemplate.VAL_DATE, getValDate());
            reportRows.add(row);
        }

        // set report rows on output
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;

    }

    // --- METHODS --- //

    /* load SantaHaircutByIsin items */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SantHaircutByIsinItem> loadSantHaircutByIsinItems(String agreementIds, String isinIds,
                                                                   Vector errorMsgsP) {

        List<SantHaircutByIsinItem> itemList = new ArrayList<>();

        // filter by contract
        if (!Util.isEmpty(agreementIds) && Util.isEmpty(isinIds)) {
            itemList = loadItemsbyAgreementFilter(agreementIds);
        }
        // filter by ISIN
        else if (!Util.isEmpty(isinIds) && Util.isEmpty(agreementIds)) {
            itemList = loadItemsByIsinFilter(isinIds);
        }
        // filter by both
        else if (!Util.isEmpty(agreementIds) && !Util.isEmpty(isinIds)) {
            itemList = loadItemsByBothFilter(agreementIds, isinIds);
        }
        // non filter - error
        else {
            errorMsgsP
                    .add("Because of data volume reasons, both filters cannot be empty, so please filter by Agreement, ISIN or both.");
        }

        return itemList;

    }

    /* load items using agreement filter */
    private List<SantHaircutByIsinItem> loadItemsbyAgreementFilter(String agreementIds) {

        List<SantHaircutByIsinItem> itemList = new ArrayList<>();

        // get contracts
        Map<Integer, CollateralConfig> agreementMap = loadAgreementsByIds(Util.string2IntVector(agreementIds));

        // 03/08/15. SBNA Multi-PO filter
        Set<String> posIdsAllowed = new HashSet<>(Util.string2Vector(CollateralUtilities
                .filterPoIdsByTemplate(getReportTemplate())));

        // for each contract get haircut sdfs
        for (CollateralConfig agreement : agreementMap.values()) {

            // 03/08/15. SBNA Multi-PO filter
            if (CollateralUtilities.filterOwners(posIdsAllowed, agreement)) {
                continue;
            }

            List<Integer> productList = new ArrayList<>();

            Vector<String> haircutFilters = getHaircutFiltersFromAgreement(agreement);

            // for each haircut sdf get product ids
            if (!Util.isEmpty(haircutFilters)) {
                for (String haircutFilter : haircutFilters) {
                    productList.addAll(getProductListByHaircutFilter(haircutFilter));
                }
            }
            // get product ids based on haircut from quote (if agreement is related to haircut quote set)
            productList.addAll(getProductListFromAgreementHaircutQuoteSet(agreement.getName()));

            // get products
            List<Product> products = getProductsByIds(productList);

            // for each product build SantHaircutByIsinItem & add to list
            for (Product product : products) {
                itemList.add(new SantHaircutByIsinItem(agreement, getIssuerFromProduct(product), product));
            }
        }

        return itemList;

    }

    /* load items using isin filter */
    private List<SantHaircutByIsinItem> loadItemsByIsinFilter(String productIsins) {

        List<SantHaircutByIsinItem> itemList = new ArrayList<>();

        // get products
        List<Product> products = loadProductsByISIN(Util.string2Vector(productIsins));

        // for each product get contracts
        for (Product product : products) {
            List<Integer> agreementIds = getAgreementListByProductId(product.getId());
            if (!Util.isEmpty(agreementIds)) {
                Map<Integer, CollateralConfig> agreementMap = loadAgreementsByIds(agreementIds);
                // for each contract build SantHaircutByIsin item & add to list
                for (CollateralConfig agreement : agreementMap.values()) {
                    itemList.add(new SantHaircutByIsinItem(agreement, getIssuerFromProduct(product), product));
                }
            }
        }

        return itemList;

    }

    /* load items without filter */
    @SuppressWarnings("unused")
    private List<SantHaircutByIsinItem> loadItemsByNonFilter() {

        List<SantHaircutByIsinItem> itemList = new ArrayList<>();

        // all contracts
        Map<Integer, String> allAgreements = new AgreementLoader().load();
        itemList = loadItemsbyAgreementFilter(Util.collectionToString(allAgreements.keySet()));

        return itemList;

    }

    /* load items using both filters */
    private List<SantHaircutByIsinItem> loadItemsByBothFilter(String agreementIds, String productIsins) {

        List<SantHaircutByIsinItem> itemList = new ArrayList<SantHaircutByIsinItem>();

        // get contracts
        Map<Integer, CollateralConfig> agreementMap = loadAgreementsByIds(Util.string2IntVector(agreementIds));
        // get products
        List<Product> products = loadProductsByISIN(Util.string2Vector(productIsins));
        // for each contract & for each product build SantHaircutByIsin item & add to list
        for (CollateralConfig agreement : agreementMap.values()) {
            for (Product product : products) {
                itemList.add(new SantHaircutByIsinItem(agreement, getIssuerFromProduct(product), product));
            }
        }

        return itemList;

    }

    /* get issuer le from product */
    @SuppressWarnings("rawtypes")
    private LegalEntity getIssuerFromProduct(Product product) {

        LegalEntity issuer = new LegalEntity();

        Vector issuers = product.getIssuerIds();
        if (!Util.isEmpty(issuers)) {
            issuer = BOCache.getLEFromCache((Integer) issuers.get(0));
        }

        return issuer;

    }

    /* get products from product ids list */
    private List<Product> getProductsByIds(List<Integer> productList) {

        List<Product> products = new ArrayList<>();

        for (Integer productId : productList) {
            Product product = this.productsMap.get(productId);
            if (product == null) {
                product = BOCache.getExchangedTradedProduct(getDSConnection(), productId);
                this.productsMap.put(productId, product);
            }
            products.add(product);
        }

        return products;

    }

    /* get product ids that match with static data filter */
    private List<Integer> getProductListByHaircutFilter(String haircutFilter) {

        List<Integer> productList = new ArrayList<Integer>();

        try {
            productList = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getProductIdsByIssuerFilter(haircutFilter);
        } catch (RemoteException e) {
            Log.error(this, "Error getting product list by haircut_filter = " + haircutFilter, e);
        }

        return productList;

    }

    /* get product ids with existing quote in haircut quote set linked to contract */
    private List<Integer> getProductListFromAgreementHaircutQuoteSet(String agreementName) {

        String haircutQuoteSetName = null;
        List<Integer> productList = new ArrayList<Integer>();

        try {
            haircutQuoteSetName = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getAgreementHaircutFromQuote(agreementName);
        } catch (RemoteException e) {
            Log.error(this, "Error checking if contract " + agreementName + " is related to haircut from quote", e);
        }
        if (!Util.isEmpty(haircutQuoteSetName)) {
            Vector<QuoteValue> quotes = getQuotesFromHaircutQuoteSet(haircutQuoteSetName, getValDate());
            if (!Util.isEmpty(quotes)) {
                productList = getProductIdsFromQuotes(quotes);
            }
        }

        return productList;

    }

    /* get valid quotes from haircut quote set */
    @SuppressWarnings("unchecked")
    private Vector<QuoteValue> getQuotesFromHaircutQuoteSet(String haircutQuoteSetName, JDate valDate) {

        final StringBuilder where = new StringBuilder();

        where.append("quote_set_name = ");
        where.append(Util.string2SQLString(haircutQuoteSetName));
        where.append(" AND (quote_name like 'Bond.ISIN%' OR quote_name like 'Equity.ISIN%')");
        where.append(" AND trunc(quote_date) = ");
        where.append(Util.date2SQLString(valDate));
        where.append(" AND close_quote is not null");

        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();

        try {
            vQuotes = getDSConnection().getRemoteMarketData().getQuoteValues(where.toString());
        } catch (RemoteException e) {
            Log.error(this, "Error getting quotes frome quote set = " + haircutQuoteSetName, e);
        }

        return vQuotes;

    }

    /* get product ids from quotes list */
    private List<Integer> getProductIdsFromQuotes(Vector<QuoteValue> quotes) {

        List<Integer> productIds = new ArrayList<Integer>();

        for (QuoteValue quote : quotes) {
            int productId = getProductIdFromQuote(quote);
            if (productId != -1) {
                productIds.add(productId);
            }
        }

        return productIds;

    }

    /* get product id from quote */
    @SuppressWarnings("unchecked")
    private int getProductIdFromQuote(QuoteValue quote) {

        int productId = -1;

        StringBuilder where = new StringBuilder();
        where.append("quote_name = ?");
        List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(quote.getName());
        Vector<ProductDesc> productDescVector = new Vector<>();

        try {
            productDescVector = getDSConnection().getRemoteProduct().getAllProductDesc(where.toString(), bindVariables);
        } catch (final RemoteException e) {
            Log.error(this, "Cannot get product desc object related to quote_name = " + quote.getName(), e);
        }

        if (!Util.isEmpty(productDescVector)) {
            productId = productDescVector.get(0).getId();
        }

        return productId;

    }

    /* get haircut static data filter linked to a contract */
    @SuppressWarnings("unchecked")
    private Vector<String> getHaircutFiltersFromAgreement(CollateralConfig agreement) {

        Vector<String> haircutFilterNames = new Vector<>();

        try {
            haircutFilterNames = getDSConnection().getRemoteReferenceData().getHaircutFilters(
                    agreement.getHaircutName());
        } catch (RemoteException e) {
            Log.error(this, "Cannot get haircut filters from contract " + agreement.getId(), e);
        }

        return haircutFilterNames;

    }

    /* get agreements that can be linked to a product through agreements haircut filters */
    private List<Integer> getAgreementListByProductId(int productId) {

        List<Integer> agreementList = new ArrayList<Integer>();

        try {
            agreementList = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getAgreementsByProductId(productId);
        } catch (RemoteException e) {
            Log.error(this, "Error getting contracts by product = " + productId, e);
        }

        return agreementList;

    }

    /* load products from product ids */
    private List<Product> loadProductsByISIN(List<String> productIsins) {

        List<Product> products = new ArrayList<Product>();

        for (String isin : productIsins) {
            Product product = BOCache.getExchangeTradedProductByKey(getDSConnection(), "ISIN", isin);
            if (product != null) {
                products.add(product);
            }
        }
        return products;

    }

    /* load agreements from agreement ids */
    private Map<Integer, CollateralConfig> loadAgreementsByIds(List<Integer> agreementIds) {

        Map<Integer, CollateralConfig> agreements = new HashMap<Integer, CollateralConfig>();

        try {
            agreements = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigByIds(
                    agreementIds);
        } catch (PersistenceException e) {
            Log.error(this, "Error getting contracts", e);
        }

        return agreements;

    }

    @Override
    protected JDate getValDate() {

        JDate valDate = getProcessStartDate();
        if (valDate == null) {
            return JDate.getNow();
        }

        return valDate;

    }

}

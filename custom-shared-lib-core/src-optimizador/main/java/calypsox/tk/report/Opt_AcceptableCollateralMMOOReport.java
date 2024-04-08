package calypsox.tk.report;

import calypsox.tk.util.SantOptimizationUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ProductReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.*;

public class Opt_AcceptableCollateralMMOOReport extends ProductReport {

    private static final long serialVersionUID = 123L;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        try {
            return getReportOutput();
        } catch (RemoteException e) {
            String error = "Error generating Opt_AcceptableCollateralMMOOReport.\n";
            Log.error(this, error, e);
            errorMsgsP.add(error + e.getMessage());
        }

        return null;

    }

    /**
     * Get report output
     *
     * @return
     * @throws RemoteException
     */
    public ReportOutput getReportOutput() throws RemoteException {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

        // load products
        List<Product> products = loadAllProducts();
        if (Util.isEmpty(products)) {
            Log.info(this, "No product found.\n");
            return null;
        }

        // get info for all MMOO camaras
        List<camaraDATA> camarasData = buildCamaraDataList("infoCamarasMMOO");

        // build data items for each camara
        List<Opt_AcceptableCollateralMMOOItem> dataItems = buildItems(products, camarasData, getValDate());
        for (Opt_AcceptableCollateralMMOOItem dataItem : dataItems) {

            ReportRow row = new ReportRow(dataItem.getProduct(), ReportRow.PRODUCT);
            row.setProperty(Opt_AcceptableCollateralMMOOReportTemplate.OPT_ACCEPTABLE_COLLAT_MMOO_ITEM, dataItem);
            reportRows.add(row);

        }

        // set report rows on output
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        return output;

    }

    /**
     * Load all bonds and equities
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Product> loadAllProducts() {

        List<Product> allProducts = new Vector<>();
        String from = null;
        String whereBond = " product_desc.product_family='Bond'";
        String whereEquity = " product_desc.product_family='Equity'";
        Vector<Product> bondProducts, equityProducts;
        try {
            bondProducts = DSConnection.getDefault().getRemoteProduct().getAllProducts(from, whereBond, null);
            equityProducts = DSConnection.getDefault().getRemoteProduct().getAllProducts(from, whereEquity, null);
            allProducts.addAll(bondProducts);
            allProducts.addAll(equityProducts);
        } catch (RemoteException e) {
            Log.error(this, "Cannot get products from DB.\n", e);
        }

        return allProducts;

    }

    /**
     * Build data items
     *
     * @param contracts
     * @param products
     * @return
     */
    private List<Opt_AcceptableCollateralMMOOItem> buildItems(List<Product> products, List<camaraDATA> camaraDataList,
                                                              JDate valDate) {

        List<Opt_AcceptableCollateralMMOOItem> items = new ArrayList<>();

        for (camaraDATA camaraData : camaraDataList) {

            // get product static data filter linked to camara
            StaticDataFilter sdf = getSDF(camaraData.getAcceptCollatSDF());
            if (sdf == null) {
                continue;
            }

            // get camara's acceptable products
            Map<Integer, Product> camaraAcceptedProducts = getAcceptedProductMap(products, sdf);

            // get product's haircut values from QS
            Vector<Integer> vector = new Vector<Integer>(camaraAcceptedProducts.keySet());
            Map<Integer, Double> productHaircutQuoteMap = buildProductHaircutQuoteMap(vector,
                    camaraData.getHaircutQS(), valDate);
            if (Util.isEmpty(productHaircutQuoteMap)) {
                continue;
            }

            // build items
            for (Integer productId : productHaircutQuoteMap.keySet()) {
                Opt_AcceptableCollateralMMOOItem item = new Opt_AcceptableCollateralMMOOItem(
                        camaraAcceptedProducts.get(productId), camaraData.getCamaraName(),
                        productHaircutQuoteMap.get(productId));
                items.add(item);
            }

        }

        return items;

    }

    /**
     * Get accepted products for a static data filter
     *
     * @param products
     * @param sdf
     * @return
     */
    private Map<Integer, Product> getAcceptedProductMap(List<Product> products, StaticDataFilter sdf) {

        Map<Integer, Product> acceptedProductMap = new HashMap<Integer, Product>();

        for (Product product : products) {
            if (sdf.accept(null, product)) {
                acceptedProductMap.put(product.getId(), product);
            }
        }

        return acceptedProductMap;

    }

    private StaticDataFilter getSDF(String sdfName) {

        StaticDataFilter sdf = null;

        try {
            sdf = getDSConnection().getRemoteReferenceData().getStaticDataFilter(sdfName);
        } catch (RemoteException e) {
            Log.error(this, "Cannot get staticDataFilter = " + sdfName + ".\n", e);
        }

        return sdf;

    }

    private Map<Integer, Double> buildProductHaircutQuoteMap(Vector<Integer> productIds, String quoteSetName,
                                                             JDate valDate) {

        Map<Integer, Double> productHaircutQuoteMap = new HashMap<Integer, Double>();

        // final String sqlQuery =
        // "select product_id, close_quote from quote_value, product_desc where quote_set_name = "
        // + Util.string2SQLString(quoteSetName)
        // + " and quote_value.quote_name = product_desc.quote_name and product_id IN "
        // + Util.collectionToSQLString(productIds) + " and trunc(quote_date) = " + Util.date2SQLString(valDate);

        try {
            productHaircutQuoteMap = SantOptimizationUtil.getSantOptimizationService(DSConnection.getDefault())
                    .getProductHaircutQuoteMap(productIds, quoteSetName, valDate);
        } catch (RemoteException e) {
            Log.error(this, "Cannot build product-haircutQuote map.\n", e);
        }

        return productHaircutQuoteMap;

    }

    private Vector<String> getDomainValues(String domainName) {
        return LocalCache.getDomainValues(getDSConnection(), domainName);
    }

    private String getDomainValueComment(String fatherDomainName, String domainName) {
        return LocalCache.getDomainValueComment(getDSConnection(), fatherDomainName, domainName);
    }

    private List<camaraDATA> buildCamaraDataList(String camaraListDomainName) {

        List<camaraDATA> camarasData = new ArrayList<camaraDATA>();

        // get camaras
        Vector<String> camaras = getDomainValues(camaraListDomainName);
        for (String camara : camaras) {
            // build data for camara
            camaraDATA camaraData = buildCamaraData(camaraListDomainName, camara);
            camarasData.add(camaraData);
        }

        return camarasData;

    }

    private camaraDATA buildCamaraData(String camarasDomName, String camaraDomValue) {

        String haircutQS = "", acceptCollatSDF = "";

        // get camara LE
        String camaraName = getDomainValueComment(camarasDomName, camaraDomValue);
        // get camara info
        Vector<String> infoCamara = getDomainValues(camaraDomValue);
        for (String infoElement : infoCamara) {
            // get haircut qs
            if (infoElement.startsWith("Haircut")) {
                haircutQS = getDomainValueComment(camaraDomValue, infoElement);
            }
            // get dirty_price qs
            else if (infoElement.startsWith("DirtyPrice")) {
                // dirtyPriceQS = getDomainValueComment(camaraName, infoElement);
            }
            // get acceptable collateral SDF
            else {
                acceptCollatSDF = getDomainValueComment(camaraDomValue, infoElement);
            }
        }

        // buil camara data object
        return new camaraDATA(camaraName, haircutQS, acceptCollatSDF);

    }

    private class camaraDATA {

        public camaraDATA(String camaraName, String haircutQS, String acceptCollatSDF) {
            this.camaraName = camaraName;
            // this.dirtyPriceQS = dirtyPriceQS;
            this.haircutQS = haircutQS;
            this.acceptCollatSDF = acceptCollatSDF;
        }

        protected String camaraName;
        // String dirtyPriceQS;
        protected String haircutQS;
        protected String acceptCollatSDF;

        public String getCamaraName() {
            return this.camaraName;
        }

        // public String getDirtyQS() {
        // return this.dirtyPriceQS;
        // }

        public String getHaircutQS() {
            return this.haircutQS;
        }

        public String getAcceptCollatSDF() {
            return this.acceptCollatSDF;
        }

    }

}

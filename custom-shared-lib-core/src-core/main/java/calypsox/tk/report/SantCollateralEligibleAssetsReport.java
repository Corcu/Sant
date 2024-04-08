package calypsox.tk.report;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 */
public class SantCollateralEligibleAssetsReport extends Report {

    /**
     *
     */
    private static final long serialVersionUID = -2342020766371894257L;
    private static final String ISIN = "ISIN";
    private static final String SANT_COL_ELIGIBLE_ASSETS_SDF = "T2S_ELIGIBLE_ASSETS_REPORT";
    private static final String VALUATION_DATE_HOLIDAY = "SYSTEM";

    public static final NumberFormat numberFormatter15_3 = new DecimalFormat("##############0.000",
            new DecimalFormatSymbols(Locale.ENGLISH));
    public static final NumberFormat numberFormatter3_3 = new DecimalFormat("##0.000",
            new DecimalFormatSymbols(Locale.ENGLISH));

    /*
     * Saves DV parameterization of used
     */
    private static Map<String, String> dvConfiguration = null;

    /*
     * Stores the clean Price Env. By default is "CleanPrice", but it can be
     * configured by DV.
     */
    private PricingEnv cleanPricePricingEnv = null, dirtyPricePricingEnv = null;

    // variables
    /*
     * Process Date, introduced by date in Panel
     */
    private JDate processDate;
    private JDate valueDate;

    /*
     * super.getReportTemplate, allows threads to read it.
     */
    private ReportTemplate reportTemplate = null;

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.report.Report#load(java.util.Vector)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(Vector errors) {
        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        this.reportTemplate = super.getReportTemplate();

        this.processDate = getValuationDatetime().getJDate(TimeZone.getDefault());

        if (this.processDate == null) {
            Log.error(this, "Process Date cannot be empty.");
            return null;
        }

        this.valueDate = this.reportTemplate.getValDate();

        if (this.valueDate == null) {
            Log.error(this, "Value Date cannot be empty.");
            return null;
        } else {
            // Subtract 1 day to valuation date
            Vector<String> holidays = new Vector<String>();
            holidays.add(VALUATION_DATE_HOLIDAY);
            this.valueDate = this.valueDate.addBusinessDays(-1, holidays);
        }

        List<ReportRow> reportRowList = new ArrayList<ReportRow>();

        StaticDataFilter sdf = null;

        try {
            sdf = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(SANT_COL_ELIGIBLE_ASSETS_SDF);
        } catch (CalypsoServiceException e2) {
            Log.error(this, "Can't load SDF" + "\n" + e2); //sonar
        }

        if (getReportPanel() != null) {
            ReportTemplatePanel tempPanel = getReportPanel().getReportTemplatePanel();
            if (tempPanel != null) {
                tempPanel.setTemplate(getReportTemplate());
            }
        }

        try {
            cleanPricePricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("CleanPrice");
            dirtyPricePricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("DirtyPrice");
        } catch (CalypsoServiceException e1) {
            Log.error(this, "Can't load Pricing Envs" + "\n" + e1); //sonar
        }

        try {
            List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable("Bond");
            bindVariables = CustomBindVariablesUtil.addNewBindVariableToList("BondAssetBacked", bindVariables);
            List<Product> allProducts = DSConnection.getDefault().getRemoteProduct().getAllProducts(null,
                    "product_type in ( ? , ? )", bindVariables);

            // QuotesNames to load
            List<String> quotesNames = new ArrayList<>();
            // List ISIN + QuoteName
            HashMap<String, String> isinQuoteList = new HashMap<>();
            // Lista final de bonos
            List<Product> finalProductList = new ArrayList<>();

            for (Product product : allProducts) {
                // Solo se tratan los productos que pasen los criterios
                // definidos en el filtro
                if (product instanceof Bond && null != sdf && sdf.accept(null, product)) {
                    finalProductList.add(product);
                    setData(product, quotesNames, isinQuoteList);
                }
            }
            allProducts.clear();
            HashMap<String, Double> dirtyPriceList = new HashMap<>();
            HashMap<String, Double> cleanPriceList = new HashMap<>();

            int idx = 0;
            if (!Util.isEmpty(quotesNames)) {
                while (idx <= quotesNames.size()) {
                    StringBuffer quotes = new StringBuffer();

                    quotes.append(Util.collectionToString(quotesNames.subList(idx,
                            (idx + 999) > quotesNames.size() ? quotesNames.size() : idx + 999)));
                    dirtyPriceList.putAll(getPrice(quotes.toString(), this.valueDate, dirtyPricePricingEnv));
                    cleanPriceList.putAll(getPrice(quotes.toString(), this.valueDate, cleanPricePricingEnv));
                    idx += 999;
                }
            }

            for (Product product : finalProductList) {
                Bond bond = (Bond) product;
                SantCollateralEligibleAssetsItem item = new SantCollateralEligibleAssetsItem();
                item.setIsin(bond.getSecCode(ISIN));
                item.setCurrency(bond.getCurrency());
                item.setFaceValue(numberFormatter15_3.format(bond.getFaceValue()));
                item.setDirtyPrice(
                        numberFormatter3_3.format(getPrice(bond.getSecCode(ISIN), dirtyPriceList, isinQuoteList)));
                item.setCleanPrice(
                        numberFormatter3_3.format(getPrice(bond.getSecCode(ISIN), cleanPriceList, isinQuoteList)));
                item.setPoolFactor(numberFormatter3_3.format(bond.getPoolFactor(getValDate())));
                item.setMaturityDate(new SimpleDateFormat("dd/MM/yyyy")
                        .format(bond.getMaturityDate().getDate(TimeZone.getDefault())));
                item.setProcessDate(
                        new SimpleDateFormat("dd/MM/yyyy").format(this.processDate.getDate(TimeZone.getDefault())));
                item.setProductType(bond.getType().toUpperCase());
                reportRowList.add(new ReportRow(item, SantCollateralEligibleAssetsReportTemplate.COL_ELIGIBLE_ASSETS));
            }
        } catch (

                CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }

        if (reportRowList.size() > 0) {
            ReportRow[] reportRowArray = new ReportRow[reportRowList.size()];
            reportRowArray = reportRowList.toArray(reportRowArray);
            reportOutput.setRows(reportRowArray);
        }

        return reportOutput;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public HashMap<String, Double> getPrice(String quotesNames, JDate valDate, PricingEnv pricingEnv) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        HashMap<String, Double> prices = new HashMap<>();
        String quoteSet = "";

        if (pricingEnv != null && !Util.isEmpty(pricingEnv.getQuoteSetName())) {
            quoteSet = pricingEnv.getQuoteSetName();
        }

        try {
            if (!Util.isEmpty(quotesNames) && !Util.isEmpty(quoteSet)) {
                String clausule = "quote_name IN " + "(" + quotesNames + ") AND trunc(quote_date) = to_date('" + valDate
                        + "', 'dd/mm/yy') AND quote_set_name = '" + quoteSet + "'";
                vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                if (!Util.isEmpty(vQuotes)) {
                    List<QuoteValue> quotes = new ArrayList<>(vQuotes);
                    for (QuoteValue quote : quotes) {
                        if ((quote != null) && quote.getClose() != 0) {
                            prices.put(quote.getName(), quote.getClose() * 100);
                        } else { // GSM: 18/10/16
                            Log.info(this,
                                    quotesNames + " date: " + valDate.toString() + " and PE " + quoteSet + " is EMPTY");
                        }
                    }
                }
            }

        } catch (RemoteException e1) {
            Log.error(this, e1 + "Can't retrieve price");
        }

        return prices;
    }

    private void setData(Product product, List<String> quotesNames, HashMap<String, String> isinQuoteList) {
        String quoteName = product.getQuoteName();
        isinQuoteList.put(product.getSecCode("ISIN"), quoteName);
        quotesNames.add("'" + quoteName + "'");
    }

    private double getPrice(String isin, HashMap<String, Double> prices, HashMap<String, String> isinQuoteList) {
        if (!Util.isEmpty(isin) && isinQuoteList.containsKey(isin) && prices.containsKey(isinQuoteList.get(isin))) {
            return prices.get(isinQuoteList.get(isin));
        }
        return 0.00;
    }

}

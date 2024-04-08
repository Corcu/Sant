package calypsox.tk.report;

import calypsox.apps.reporting.BalanzaDePagosReportTemplatePanel;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.*;

import java.util.*;

public class BalanzaDePagosReport extends Report {
    private static final long serialVersionUID = -3317102188001975597L;
    private static final String LOG_CAT = "BalanzaDePagosReport";
    public static final String INITIAL = "Initial Balance";
    public static final String FINAL = "Final Balance";
    public static final String MOV_IN = "Movements In";
    public static final String MOV_OUT = "Movements Out";
    public static final String DIRTY_PRICE_STR = "DirtyPrice";
    public static final String OFFICIAL = "OFFICIAL";
    public static final String PROPERTY_POSITION_VALUATION_DATE = "PROPERTY_POSITION_VALUATION_DATE";
    public static final String PROPERTY_START_PRICE = "PROPERTY_START_PRICE";
    public static final String PROPERTY_END_PRICE = "PROPERTY_END_PRICE";

    private HashMap<String, PricingEnv> pEnvs = new HashMap<String, PricingEnv>();

    protected PriceCache priceCache = new PriceCache();

    @Override
    public ReportOutput load(Vector errorMsgs) {
        return createReportRows(errorMsgs);
    }

    protected ReportOutput createReportRows(Vector errorMsgs){
        DefaultReportOutput dro = new DefaultReportOutput(this);
        try {
            loadPEnvs();

            String inventoryProductType = getReportTemplate().get(BalanzaDePagosReportTemplatePanel.SELECTION_PRODUCT_TYPE);
            if (Util.isEmpty(inventoryProductType)) {
                errorMsgs.add("Inventory product type was not informed.");
                return dro;
            }

            // initially MC & PDV where made together, too hard to change
            if (BalanzaDePagosReportTemplatePanel.MARGIN_CALL_PDV.equals(inventoryProductType)) {
                processInventory(BalanzaDePagosReportTemplatePanel.PDV, dro, errorMsgs);
                processInventory(BalanzaDePagosReportTemplatePanel.MARGIN_CALL, dro, errorMsgs);
            } else {
                processInventory(inventoryProductType, dro, errorMsgs);
            }

        } catch (Exception ex ) {
            Log.error(LOG_CAT, ex);
        }
        return dro;
    }

    /**
     * Process Inventory Results for selected inventory type
     * @param inventoryType
     * @param output
     * @param errorMsgs
     * @throws CalypsoServiceException
     */
    protected  void processInventory(String inventoryType, DefaultReportOutput output, Vector errorMsgs) throws CalypsoServiceException {
        Log.system(LOG_CAT, "### Start to process " + inventoryType);
        Date startDate = getStartDate(getValuationDatetime());
        Date endDate = getEndDate(getValuationDatetime());
        List<ReportRow> rows = loadData(inventoryType, JDate.valueOf(startDate), JDate.valueOf(endDate), errorMsgs);
        setBalanceRows(rows, output);
        Log.system(LOG_CAT, "### Process " + inventoryType + " Finished.");
    }

    protected List<ReportRow> loadData(String inventoryType, JDate startDate, JDate endDate, Vector errorMsgs) {
        List<BalanzaDePagosItem> positionEntries = loadFromInventoryLoader(inventoryType, startDate, endDate, errorMsgs);
        setPricesAndMarketValue(positionEntries,  startDate, endDate, errorMsgs);
        ArrayList<ReportRow> result = new ArrayList<>();
        biuldBalanceReportRows(startDate, endDate, positionEntries, result, errorMsgs);
        return result;
    }

    private void setPricesAndMarketValue(List<BalanzaDePagosItem> positionsEntries, JDate startDate, JDate endDate, Vector errorMsgs) {

        Vector holidays = getReportTemplate().getHolidays();
        positionsEntries.stream().forEach(
                item-> {
                    Double price;
                   if (item.getPositionDate().equals(startDate))     {
                       price = getPriceOnDate(item.getProduct(), startDate, holidays, errorMsgs);
                   } else {
                       price = getPriceOnDate(item.getProduct(), endDate, holidays, errorMsgs);
                   }
                   if (null != price
                       &&  null != item.getNominal()) {
                       Double marketValue = (item.getNominal()*price);
                       item.setMarketValue(marketValue);
                   }
        });

    }

    protected List<BalanzaDePagosItem> loadFromInventoryLoader(String inventoryProductType, JDate startDate, JDate endDate, Vector errorMsgs) {
        BalanceInventoryLoader loader = new BalanceInventoryLoader(inventoryProductType, getReportTemplate(),  startDate, endDate, getValuationDatetime(), getPricingEnv(), errorMsgs);
        try {
            List<BalanzaDePagosItem> items = loader.loadData();
            return items;
        } catch (Exception e) {
            Log.error(LOG_CAT, e);
        }
        return null;
    }

    private List<ReportRow> biuldBalanceReportRows(JDate startDate, JDate endDate, List<BalanzaDePagosItem> positionEnttries, List<ReportRow> rows, Vector<String> errorMsgs) {
        positionEnttries.stream().forEach(item -> {
            ReportRow row = new ReportRow(item);
            row.setProperty(ReportRow.DEFAULT, item);
            row.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
            row.setProperty(PROPERTY_POSITION_VALUATION_DATE, endDate);
            DisplayValue priceStart = getPriceDisplayValue(startDate, item.getProduct(), row, errorMsgs);
            if (priceStart != null) {
                row.setProperty(PROPERTY_START_PRICE, priceStart);
            }
            DisplayValue priceEnd = getPriceDisplayValue(startDate, item.getProduct(), row, errorMsgs);
            if (priceEnd != null) {
                row.setProperty(PROPERTY_END_PRICE, priceEnd);
            }
            rows.add(row);
        });
        return rows;
    }

    private DisplayValue getPriceDisplayValue(JDate paramDate, Product product, ReportRow row, Vector<String> errorMsgs) {
        Double price = getPriceOnDate(product, paramDate, getReportTemplate().getHolidays(), errorMsgs);
        if (price != null) {
            DisplayValue result = product.getPriceDisplayValue();
            result.set(price);
            return result;
        }
        return null;
    }


    private Double getPriceOnDate(Product product, JDate startDate, Vector<String> holidays, Vector errorMsgs) {
        String pEnvName = getPricingEnvName(product);
        PricingEnv pEnv = pEnvs.get(pEnvName);
        Double startPrice = priceCache.getPrice(product, startDate);
        if(startPrice==null) {
            QuoteSet quoteSet = pEnv.getQuoteSet();
            startPrice = getQuotePrice(product, quoteSet, startDate, pEnv, holidays, errorMsgs);
            if (null!=startPrice) {
                priceCache.addPrice(product, startDate, startPrice);
            }
        }
        return startPrice;
    }

    /**
     * @param product
     * @return
     */
    private String getPricingEnvName(Product product) {
        String pEnvName = "";
        if(product  instanceof Bond) {
            pEnvName = DIRTY_PRICE_STR;
        }
        else if(product  instanceof Equity) {
            pEnvName = OFFICIAL;
        }

        return pEnvName;
    }

    /**
     * @return close quotePrice for the product
     */
    private Double getQuotePrice(final Product product, final QuoteSet quoteSet, JDate startDate, PricingEnv pEnv, Collection<String> holidays, Vector<String> errors) {

        QuoteValue productQuote = quoteSet.getProductQuote(product, startDate, getPriceType(pEnv.getName()));

        if ((productQuote != null) && (!Double.isNaN(productQuote.getClose())))
            return productQuote.getClose();

        final String error = "Quote not available for Product ISIN: " + product.getSecCode("ISIN");
        Log.system(LOG_CAT, error);
        return null;
    }

    public static boolean isInCountryLE(LegalEntity le) {
        if (!Util.isEmpty(le.getCountry())
                && (le.getCountry().equalsIgnoreCase("SPAIN")
                || le.getCountry().startsWith("ES"))) {
            return true;
        }
        return false;
    }

    private Date getStartDate(JDatetime valDate) {
        Vector holidays = getReportTemplate().getHolidays();
        Calendar calMin = valDate.getJDate(TimeZone.getDefault()).addMonths(-2).asCalendar();
        calMin.set(Calendar.DAY_OF_MONTH, calMin.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date lastDt = calMin.getTime();
        lastDt = adjustDate(lastDt, holidays);
        return lastDt;
    }

    private Date getEndDate(JDatetime valDate) {
        Vector holidays = getReportTemplate().getHolidays();
        Calendar cal = valDate.getJDate(TimeZone.getDefault()).addMonths(-1).asCalendar();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        final Date lastDayOfCurrent = cal.getTime();
        Date lastDt = cal.getTime();
        lastDt = adjustDate(lastDt, holidays);
        return lastDt;

    }

    private Date adjustDate(Date date, Vector<String> holidays) {
        if (!CollateralUtilities.isBusinessDay(JDate.valueOf(date), Util.string2Vector("SYSTEM"))) {
            JDate dt  = CollateralUtilities.getPreviousBusinessDay(JDate.valueOf(date), Util.string2Vector("SYSTEM"));
            Calendar cal = dt.asCalendar();
            date = cal.getTime();
        }
        return date;
    }


    private void setBalanceRows(List<ReportRow> rows, DefaultReportOutput output) {
        List<ReportRow> result = new ArrayList<>();
        if (null != output.getRows()
                && output.getRows().length > 0)  {
            result.addAll(Arrays.asList(output.getRows()));
        }
        result.addAll(rows);
        ReportRow[] reportRows = result.stream().toArray(ReportRow[]::new);
        output.setRows(reportRows);
    }

    public String getPriceType(String penvName) {
        if(penvName.equals(OFFICIAL))
            return "Price";
        else
            return penvName;
    }

    protected void loadPEnvs() {
        pEnvs.put(OFFICIAL, AppUtil.loadPE(OFFICIAL, getValuationDatetime()));
        pEnvs.put(DIRTY_PRICE_STR, AppUtil.loadPE(DIRTY_PRICE_STR, getValuationDatetime()));
    }


    public class PriceCache {

        HashMap<Product,HashMap<JDate,Double>> priceCache = new HashMap<Product,HashMap<JDate,Double>>();

        public void addPrice(Product sec, JDate date, Double price) {
            HashMap<JDate,Double>  priceHash = priceCache.get(sec);
            if(priceHash==null) {
                priceHash = new HashMap<JDate, Double>();
            }
            priceHash.put(date, price);
            priceCache.put(sec, priceHash);
        }


        public Double getPrice(Product sec, JDate date) {
            HashMap<JDate,Double>  priceHash = priceCache.get(sec);
            if(priceHash!=null) {
                return priceHash.get(date);
            }
            return null;
        }

        public void clear() {
            priceCache.clear();
        }

    }

}

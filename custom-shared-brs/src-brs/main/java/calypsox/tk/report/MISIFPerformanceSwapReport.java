package calypsox.tk.report;

import calypsox.tk.bo.mis.PerSwapMisBean;
import calypsox.tk.bo.mis.PerSwapMisBeanBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class MISIFPerformanceSwapReport extends TradeReport {

    private static final long serialVersionUID = 1L;
    public static final String DIRTY_PRICE_STR = "DirtyPrice";
    public static final String DIRTY_PRICE_TODAY_STR = "DirtyPriceToday";
    public static final String DIRTY_PRICE_YESTERDAY_STR = "DirtyPriceYesterday";
    public static final String CLEAN_PRICE_STR = "CleanPrice";
    public static final String CLEAN_PRICE_TODAY_STR = "CleanPriceToday";
    public static final String CLEAN_PRICE_YESTERDAY_STR = "CleanPriceYesterday";


    public class PriceCache {
        HashMap<Product, HashMap<String, Double>> priceCache = new HashMap<>();

        public void addPrice(Product sec, String type, Double price) {
            HashMap<String, Double> priceHash = priceCache.get(sec);
            if (priceHash == null) {
                priceHash = new HashMap<>();
            }
            priceHash.put(type, price);
            priceCache.put(sec, priceHash);
        }

        public Double getPrice(Product sec, String type) {
            final HashMap<String, Double> priceHash = priceCache.get(sec);
            if (priceHash != null) {
                return priceHash.get(type);
            }
            return null;
        }
    }

    /**
     * @param errorMsgs
     * @return
     */
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
        if (null != output) {
            final ReportRow[] rows = output.getRows();
            List<ReportRow> rowsBondForward = new ArrayList<ReportRow>();
            List<ReportRow> rowsPerformanceSwap = new ArrayList<ReportRow>();

            PriceCache priceCache = new PriceCache();
            HashMap<LegalEntity, String> jMinoristas = new HashMap<LegalEntity, String>();

            for (int i = 0; i < rows.length; i++) {
                final ReportRow row = rows[i];
                if (row != null) {
                    final Trade trade = row.getProperty(ReportRow.TRADE);
                    final Product product = trade.getProduct();
                    if (product instanceof PerformanceSwap) {
                        rowsPerformanceSwap.add(row);
                    } else {
                        rowsBondForward.add(row);
                    }
                }
            }

            //Relleno las rows del PerSwap
            final List<PerSwapMisBean> newRows = createNewRows(rowsPerformanceSwap);
            for (PerSwapMisBean row : newRows) {
                if (null != row.getSpread() && !Util.isEmpty(row.getSpread())) {
                    double spread = Double.parseDouble(row.getSpread()) * 100;
                    row.setSpread(Double.toString(spread));
                }
            }

            final ReportRow[] reportRows = newRows.stream().map(ReportRow::new).toArray(ReportRow[]::new);
            output.clear();
            output.setRows(reportRows);


            //Relleno las rows del BondForward
            for (int i = 0; i < rowsBondForward.size(); i++) {
                ReportRow row = rowsBondForward.get(i);
                final Trade trade = row.getProperty(ReportRow.TRADE);
                final Product product = trade.getProduct();
                final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
                final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
                final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
                final JDate valDateD1 = valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));

                final LegalEntity cpty = trade.getCounterParty();

                String jMinoristaStr = jMinoristas.get(cpty);
                if (jMinoristaStr == null) {
                    LegalEntityAttribute jMinorista = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, cpty.getId(), "ALL", "J_MINORISTA");
                    jMinoristaStr = jMinorista != null ? jMinorista.getAttributeValue() : "";
                    jMinoristas.put(cpty, jMinoristaStr);
                }
                row.setProperty("J_MINORISTA", jMinoristaStr);

                Double dirtyPrice = priceCache.getPrice(product, DIRTY_PRICE_STR);
                Double yesterdayDirtyPrice = priceCache.getPrice(product, DIRTY_PRICE_YESTERDAY_STR);
                Double cleanPrice = priceCache.getPrice(product, CLEAN_PRICE_STR);
                Double yesterdayCleanPrice = priceCache.getPrice(product, CLEAN_PRICE_YESTERDAY_STR);

                if (dirtyPrice == null) {
                    dirtyPrice = getQuote(product, DIRTY_PRICE_STR, valDate) * 100;
                    priceCache.addPrice(product, DIRTY_PRICE_TODAY_STR, dirtyPrice);
                }

                if (yesterdayDirtyPrice == null) {
                    yesterdayDirtyPrice = getQuote(product, DIRTY_PRICE_STR, valDateD1) * 100;
                    priceCache.addPrice(product, DIRTY_PRICE_YESTERDAY_STR, yesterdayDirtyPrice);
                }

                if (cleanPrice == null) {
                    cleanPrice = getQuote(product, CLEAN_PRICE_STR, valDate) * 100;
                    priceCache.addPrice(product, CLEAN_PRICE_TODAY_STR, cleanPrice);
                }

                if (yesterdayCleanPrice == null) {
                    yesterdayCleanPrice = getQuote(product, CLEAN_PRICE_STR, valDateD1) * 100;
                    priceCache.addPrice(product, CLEAN_PRICE_YESTERDAY_STR, yesterdayCleanPrice);
                }

                row.setProperty(DIRTY_PRICE_TODAY_STR, dirtyPrice);
                row.setProperty(DIRTY_PRICE_YESTERDAY_STR, yesterdayDirtyPrice);
                row.setProperty(CLEAN_PRICE_TODAY_STR, cleanPrice);
                row.setProperty(CLEAN_PRICE_YESTERDAY_STR, yesterdayCleanPrice);
                output.addReportRow(null, row);
            }

            return output;
        } else return null;

    }

    /**
     * @param rows
     * @return
     */
    private List<PerSwapMisBean> createNewRows(List<ReportRow> rows) {
        List<PerSwapMisBean> newRows = new ArrayList<>();
        if (Optional.ofNullable(rows).isPresent()) {
            newRows = rows.stream().parallel().map(this::buildBean).filter(list -> !Util.isEmpty(list)).flatMap(List::stream).collect(Collectors.toList());
        }
        return newRows;
    }

    /**
     * @param row
     * @return
     */
    private List<PerSwapMisBean> buildBean(ReportRow row) {
        List<PerSwapMisBean> beans = new ArrayList<>();
        final Optional opTrade = row.getProperty("Default") instanceof Trade ? Optional.ofNullable(row.getProperty("Default")) : Optional.empty();
        if (opTrade.isPresent()) {
            PerSwapMisBeanBuilder builder = new PerSwapMisBeanBuilder((Trade) opTrade.get(), getValDate());
            beans = builder.build(getPricingEnv());
            if (!Util.isEmpty(beans)) {
                for (PerSwapMisBean b : beans) {
                    b.setTrade((Trade) opTrade.get());
                }
            }
        }
        return beans;
    }

    public Double getQuote(Product product, String QuoteSetName, JDate valDate) {
        String quoteName = product.getQuoteName();
        QuoteValue value = new QuoteValue();
        value.setQuoteSetName(QuoteSetName);
        value.setName(quoteName);
        value.setQuoteType("DirtyPrice");
        value.setDate(valDate);
        try {
            final QuoteValue quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);
            if (null != quoteValue) {
                return quoteValue.getClose();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Quote not available for Product ISIN: " + product.getSecCode("ISIN") + " for date " + valDate + " quote name " + quoteName);
        }
        return 0.0;
    }
}

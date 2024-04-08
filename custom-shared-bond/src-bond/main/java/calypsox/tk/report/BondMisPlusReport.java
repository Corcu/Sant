package calypsox.tk.report;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;
import java.util.*;
import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;


public class BondMisPlusReport extends TradeReport {
    private static final long serialVersionUID = 1L;
    public static final String DIRTY_PRICE_STR = "DirtyPrice";
    public static final String DIRTY_PRICE_TODAY_STR = "DirtyPriceToday";
    public static final String DIRTY_PRICE_YESTERDAY_STR = "DirtyPriceYesterday";
    public static final String CLEAN_PRICE_STR = "CleanPrice";
    public static final String CLEAN_PRICE_TODAY_STR = "CleanPriceToday";
    public static final String CLEAN_PRICE_YESTERDAY_STR = "CleanPriceYesterday";
    public static final String OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";
    public static final String IS_FX_MULTI_CCY = "IS_FX_MULTI_CCY";
    public static final String FX_TRADE = "FX_TRADE";
    private static final String WHERE_CLAUSE = "bo_transfer.transfer_status<>'CANCELLED' AND trade.trade_id = bo_transfer.trade_id AND bo_transfer.trade_id IN (";
    HashMap<String, PricingEnv> pEnvs = new HashMap<>();


    public class PriceCache {
        HashMap<Product,HashMap<String,Double>> priceCache = new HashMap<>();
        public void addPrice(Product sec, String type, Double price) {
            HashMap<String,Double>  priceHash = priceCache.get(sec);
            if(priceHash==null) {
                priceHash = new HashMap<>();
            }
            priceHash.put(type, price);
            priceCache.put(sec, priceHash);
        }

        public Double getPrice(Product sec, String type) {
            final HashMap<String,Double>  priceHash = priceCache.get(sec);
            if(priceHash!=null) {
                return priceHash.get(type);
            }
            return null;
        }
    }



    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput load(Vector errorMsgs) {

        final DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
        if(output==null) {
            return null;
        }
        final ReportRow[] rows = output.getRows();
            PriceCache priceCache = new PriceCache();
            HashMap <LegalEntity, String> jMinoristas = new HashMap <LegalEntity, String> ();
            final JDate valDate = getValDate(_pricingEnv.getTimeZone());
            final JDate valDateD1 = valDate.addBusinessDays(-1,Util.string2Vector("SYSTEM"));

        for(int i=0; i<rows.length; i++) {
                final ReportRow row = rows[i];
                if (row != null){
                    final Trade trade = row.getProperty(ReportRow.TRADE);
                    final Product product = trade.getProduct();
                    final LegalEntity cpty = trade.getCounterParty();

                    String jMinoristaStr = jMinoristas.get(cpty);
                    if (jMinoristaStr==null){
                        LegalEntityAttribute jMinorista = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, cpty.getId(), "ALL", "J_MINORISTA");
                        jMinoristaStr = jMinorista!=null ? jMinorista.getAttributeValue() : "";
                        jMinoristas.put(cpty, jMinoristaStr);
                    }
                    row.setProperty("J_MINORISTA", jMinoristaStr);

                    Double dirtyPrice = priceCache.getPrice(product, DIRTY_PRICE_STR);
                    Double yesterdayDirtyPrice = priceCache.getPrice(product, DIRTY_PRICE_YESTERDAY_STR);
                    Double cleanPrice = priceCache.getPrice(product, CLEAN_PRICE_STR);
                    Double yesterdayCleanPrice = priceCache.getPrice(product, CLEAN_PRICE_YESTERDAY_STR);

                    if (dirtyPrice == null){
                        dirtyPrice = getQuote(product, DIRTY_PRICE_STR , valDate) * 100;
                        priceCache.addPrice(product, DIRTY_PRICE_TODAY_STR, dirtyPrice);
                    }

                    if (yesterdayDirtyPrice == null){
                        yesterdayDirtyPrice = getQuote(product, DIRTY_PRICE_STR, valDateD1) * 100;
                        priceCache.addPrice(product, DIRTY_PRICE_YESTERDAY_STR, yesterdayDirtyPrice);
                    }

                    if (cleanPrice == null){
                        cleanPrice = getQuote(product, CLEAN_PRICE_STR, valDate) * 100;
                        priceCache.addPrice(product, CLEAN_PRICE_TODAY_STR, cleanPrice);
                    }

                    if (yesterdayCleanPrice == null){
                        yesterdayCleanPrice = getQuote(product, CLEAN_PRICE_STR, valDateD1) * 100;
                        priceCache.addPrice(product, CLEAN_PRICE_YESTERDAY_STR, yesterdayCleanPrice);
                    }

                    row.setProperty(DIRTY_PRICE_TODAY_STR, dirtyPrice);
                    row.setProperty(DIRTY_PRICE_YESTERDAY_STR, yesterdayDirtyPrice);
                    row.setProperty(CLEAN_PRICE_TODAY_STR, cleanPrice);
                    row.setProperty(CLEAN_PRICE_YESTERDAY_STR, yesterdayCleanPrice);

                }
            }

        return processBondMultiCcy(output);
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
            Log.error(this,"Quote not available for Product ISIN: " + product.getSecCode("ISIN") + " for date " + valDate + " quote name " + quoteName);
        }
        return 0.0;
    }

    private DefaultReportOutput processBondMultiCcy(DefaultReportOutput output){
        ArrayList<ReportRow> fxRows = new ArrayList<>();
        for (ReportRow row: output.getRows()){
            Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
            if (isDualCcy(trade)){
                ReportRow newRow = row.clone();
                newRow.setProperty(IS_FX_MULTI_CCY,"true");
                newRow.setProperty(FX_TRADE,getOtherMultiCcyTrade(trade));
                fxRows.add(newRow);
            }
        }
        output.setRows(mergeListIntoArray(output.getRows(), fxRows));
        return output;
    }

    private ReportRow  [] mergeListIntoArray (ReportRow [] array, List<ReportRow> list) {
        List<ReportRow> list1 = Arrays.asList(array);
        list.addAll(list1);
        return  list.toArray(new ReportRow[0]);
    }

    public static boolean isDualCcy(Trade trade) {
        return trade.getKeywordValue("Dual_CCY") != null &&
                trade.getKeywordValue("Dual_CCY").equals("true");
    }
}
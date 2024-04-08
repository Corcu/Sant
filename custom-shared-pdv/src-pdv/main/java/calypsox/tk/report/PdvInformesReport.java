package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.*;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.stream.Collectors;

public class PdvInformesReport extends TradeReport {

    public static final String LOG_CATEGORY = "PdvInformesReport";
    public static final String HOLIDAYS = "HOLIDAYS";
    public static final String STR_DIRTY_PRICE = "DirtyPrice";
    public static final String STR_OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";


    public ReportOutput load(Vector errorMsgs) {


        checkAndAdjustMaturity();

        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
        JDate valDate = getReportTemplate().getValDate();

        PdvPriceCache priceCache = new PdvPriceCache();
        PricingEnv dirtyPricePE = AppUtil.loadPE(STR_DIRTY_PRICE, getValuationDatetime());

        if (output != null) {

            ReportRow[] mergedRows = getMergedTrades(output, valDate);
            output.setRows(mergedRows);

            ReportRow[] rows = output.getRows();
            HashMap<Trade, CashFlowSet> calculatedFlows = new HashMap<Trade, CashFlowSet>();

            for (int i = 0; i < rows.length; i++) {
                ReportRow row = rows[i];
                Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
                if (!calculatedFlows.containsKey(trade)) {
                    try {
                        if (!trade.getProduct().getCustomFlowsB()) {
                            trade.getProduct().getFlows(trade, getValDate(), false, -1, true);
                        }
                    } catch (FlowGenerationException e) {
                        Log.error(LOG_CATEGORY, e);
                    }
                    calculatedFlows.put(trade, trade.getProduct().getFlows());
                } else {
                    trade.getProduct().setFlows(calculatedFlows.get(trade));
                }

                getUnderlyingPrices(getValuationDatetime(), trade, row, dirtyPricePE, priceCache);


            }
        }

        return output;
    }

    private TradeArray getMaturedTradesCurrentMonth(JDate valDate) {
        TradeArray trades = null;
        if (!Util.isEmpty(getReportTemplate().get("Include Matured").toString()) && Boolean.parseBoolean(getReportTemplate().get("Include Matured").toString())) {
            String from = "trade, product_desc";
            String where = "( ( trade.trade_id in (select trade_id from trade_keyword where ( trade_keyword.keyword_name = 'SecLendingTrade' ) " +
                    "AND ( trade_keyword.keyword_value <> 'FICTICIO' )) ) " +
                    "OR ( not( trade.trade_id in (select trade_id from trade_keyword where ( trade_keyword.keyword_name = 'SecLendingTrade' )) ) ) ) " +
                    "AND ( trade.trade_id in (select trade_id from trade_keyword where ( trade_keyword.keyword_name = 'MurexRootContract' )) ) " +
                    "AND trade.PRODUCT_ID = product_desc.PRODUCT_ID " +
                    "AND product_desc.product_type  = 'SecLending' " +
                    "AND trade.trade_status  = 'MATURED' " +
                    "AND  ( to_number(to_char( product_desc.maturity_date ,'MM')) = " + valDate.getMonth() +
                    "AND to_number(to_char( product_desc.maturity_date ,'YYYY')) = " + valDate.getYear() + " )";
            try {
                trades = getDSConnection().getRemoteTrade().getTrades(from, where, null, null);

            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getName() + " - " + "Cant retrieve any matured trades over current month", e);
            }
        }
        return trades;
    }

    private void checkAndAdjustMaturity() {

        if (getReportTemplate() != null
                && !Util.isEmpty(getReportTemplate().getTemplateName())
                && getReportTemplate().getTemplateName().contains("PdvInformesOperacionesVivas")) {
            Calendar cal = getValuationDatetime().asCalendar();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));

            String startDateStr = JDate.valueOf(cal).toString();

            getReportTemplate().put("MaturityStartDate", startDateStr);
            setReportTemplate(getReportTemplate());
            //getReportPanel().setTemplate(getReportTemplate());
            //getReportPanel().refresh();
            getReportTemplate().callBeforeLoad();

        }
    }

    private void getUnderlyingPrices(JDatetime valDatetime, Trade trade, ReportRow row, PricingEnv pricingEnv, PdvPriceCache priceCache) {

        try {

            if (trade.getProduct() instanceof SecLending) {
                SecLending secLending = (SecLending) trade.getProduct();
                Double dirtyPrice = priceCache.getPrice(secLending.getSecurity(), STR_DIRTY_PRICE);
                if (dirtyPrice == null) {
                    Vector holidays = row.getProperty(PdvInformesReport.HOLIDAYS);
                    dirtyPrice = CollateralUtilities.getDirtyPrice(secLending.getSecurity(), valDatetime.getJDate().addBusinessDays(1, holidays), pricingEnv, holidays);
                    priceCache.addPrice(secLending.getSecurity(), STR_DIRTY_PRICE, dirtyPrice);
                }
                row.setProperty(STR_DIRTY_PRICE, dirtyPrice);
            }
        } catch (Exception e) {
            Log.error("PdvInformesReport", e);
        }
    }

    private ReportRow[] getMergedTrades(DefaultReportOutput output, JDate valDate) {

        List<ReportRow> mergedRows = Arrays.stream(output.getRows()).collect(Collectors.toList());
        TradeArray maturedTrades = getMaturedTradesCurrentMonth(valDate);
        if (null != maturedTrades) {
            for (Object trade : maturedTrades) {
                ReportRow row = new ReportRow(trade, ReportRow.TRADE);
                row.setProperty(ReportRow.PRICING_ENV, getPricingEnv());
                row.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
                row.setProperty(ReportRow.REPORT_OUTPUT, output);
                mergedRows.add(row);
            }
        }
        return mergedRows.toArray(new ReportRow[0]);
    }


}

package calypsox.tk.report;

import calypsox.util.FormatUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.*;

public class PdvContingenciaReport extends TradeReport {

    public static final String CLEAN_PRICE = "CleanPrice";
    public static final String OFFICIAL_PRICE = "OFFICIAL";
    public static final String DIRTY_PRICE = "DirtyPrice";
    public static final String FX_EUR_CCY = "FX_EUR_CCY";
    public static final String EUR = "EUR";


    public ReportOutput load(Vector errorMsgs) {

        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
        PdvPriceCache priceCache = new PdvPriceCache();

        if (output != null) {
            ReportRow[] rows = output.getRows();

            PricingEnv cleanPricePE = AppUtil.loadPE(CLEAN_PRICE, getValuationDatetime());
            PricingEnv officialPricePE = AppUtil.loadPE(OFFICIAL_PRICE, getValuationDatetime());
            PricingEnv dirtyPricePE = AppUtil.loadPE(DIRTY_PRICE, getValuationDatetime());

            for (int i = 0; i < rows.length; i++) {
                ReportRow row = rows[i];
                Trade trade = row.getProperty(ReportRow.TRADE);
                PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
                JDatetime valDateTime = ReportRow.getValuationDateTime(row);
                JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
                Product product = trade.getProduct();

                if (product instanceof SecLending) {

                    if (null != dirtyPricePE) {
                        Double dirtyPrice = priceCache.getPrice(((SecLending) product).getSecurity(), DIRTY_PRICE);

                        if (dirtyPrice == null) {
                            dirtyPrice = getQuotePrice(((SecLending) product).getSecurity(), valDate, dirtyPricePE);
                            priceCache.addPrice(((SecLending) product).getSecurity(), DIRTY_PRICE, dirtyPrice);
                        }
                        row.setProperty(DIRTY_PRICE, dirtyPrice);
                    }

                    if (null != cleanPricePE) {
                        Double cleanPrice = priceCache.getPrice(((SecLending) product).getSecurity(), CLEAN_PRICE);

                        if (cleanPrice == null) {
                            cleanPrice = getQuotePrice(((SecLending) product).getSecurity(), valDate, cleanPricePE);
                            priceCache.addPrice(((SecLending) product).getSecurity(), CLEAN_PRICE, cleanPrice);
                        }
                        row.setProperty(CLEAN_PRICE, cleanPrice);
                    }

                    if (null != officialPricePE) {

                        if (!((SecLending) product).getSecurity().getCurrency().equalsIgnoreCase("EUR")) {
                            Double fxQuotePrice = getFXQuotePrice(EUR, ((SecLending) product).getSecurity().getCurrency(), valDate, officialPricePE);
                            row.setProperty(FX_EUR_CCY, fxQuotePrice);
                        }

                        Double officialPrice = priceCache.getPrice(((SecLending) product).getSecurity(), OFFICIAL_PRICE);
                        if (officialPrice == null) {
                            officialPrice = getQuotePrice(((SecLending) product).getSecurity(), valDate, officialPricePE);
                            priceCache.addPrice(((SecLending) product).getSecurity(), OFFICIAL_PRICE, officialPrice);
                        }
                        row.setProperty(OFFICIAL_PRICE, officialPrice);
                    }
                }
            }
        }
        return output;
    }

    private Double getQuotePrice(final Product product, JDate valDate, PricingEnv pEnv) {

        JDate quoteDate = valDate.addBusinessDays(0, getHolidays());
        if (product instanceof Equity) {
            String date = FormatUtil.formatDate(quoteDate, "dd/MM/yyyy");
            quoteDate = JDate.valueOf(date);
        }
        Double close = CollateralUtilities.getQuotePrice(product, quoteDate, pEnv.getName());
        if (close != null && close > 0.0)
            return close;

        return null;
    }

    private Double getFXQuotePrice(final String ccy1, String ccy2, JDate valDate, PricingEnv pEnv) {
        JDate quoteDate = valDate.addBusinessDays(0, getHolidays());
        return CollateralUtilities.getFXRatebyQuoteSet(quoteDate, ccy1, ccy2, pEnv);
    }

    protected Vector getHolidays() {
        Vector holidays = new Vector<>();
        if (getReportTemplate().getHolidays() != null) {
            holidays = getReportTemplate().getHolidays();
        } else {
            holidays.add("SYSTEM");
        }
        return holidays;
    }

}

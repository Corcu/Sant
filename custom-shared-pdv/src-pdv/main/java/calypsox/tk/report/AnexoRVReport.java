package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.Vector;

public class AnexoRVReport extends TradeReport {

    public static final String OFFICIAL_PRICE = "OFFICIAL";
    public static final String FX_EUR_CCY = "FX_EUR_CCY";


    public ReportOutput load(Vector errorMsgs) {

        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        if (output != null) {
            ReportRow[] rows = output.getRows();
            PricingEnv officialPricePE = AppUtil.loadPE(OFFICIAL_PRICE, getValuationDatetime());

            for (int i = 0; i < rows.length; i++) {
                ReportRow row = rows[i];
                Trade trade = row.getProperty(ReportRow.TRADE);
                PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
                JDatetime valDateTime = ReportRow.getValuationDateTime(row);
                JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
                Product product = trade.getProduct();

                if (product instanceof SecLending) {
                    if (null != officialPricePE) {
                        if (!trade.getTradeCurrency().equalsIgnoreCase("EUR")) {
                            JDate quoteDate = valDate.addBusinessDays(-1, getReportTemplate().getHolidays());
                            Double fxQuotePrice = CollateralUtilities.getFXRatebyQuoteSet(quoteDate, "EUR", trade.getTradeCurrency(), officialPricePE);
                            row.setProperty(FX_EUR_CCY, fxQuotePrice);
                        }
                    }
                }
            }

            output.setRows(rows);
        }
        return output;
    }


}

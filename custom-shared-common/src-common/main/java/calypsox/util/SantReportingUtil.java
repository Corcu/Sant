package calypsox.util;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.collateral.service.RemoteSantRiskParameterService;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.Report;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.TimeZone;

public class SantReportingUtil {

    public static RemoteSantReportingService getSantReportingService(final DSConnection ds) {
        return (RemoteSantReportingService) ds.getRMIService("baseSantReportingService",
                RemoteSantReportingService.class);
    }

    public static RemoteSantRiskParameterService getSantRiskParameterService(final DSConnection ds) {
        return (RemoteSantRiskParameterService) ds.getRMIService("baseSantRiskParameterService",
                RemoteSantRiskParameterService.class);
    }

    /**
     * Gets the FXRate value between two currencies
     *
     * @param toCcy
     * @param fromCcy
     * @param valDate
     * @param pricingEnv
     * @return The double value of the FXRate
     * @throws MarketDataException
     */
    public static double getFXRate(final String toCcy, final String fromCcy,
                                   final JDate valDate, final String pricingEnv)
            throws MarketDataException {

        final JDate today = JDate.getNow();
        final JDatetime todayJdt = JDatetime.currentTimeValueOf(today,
                TimeZone.getDefault());

        final PricingEnv pe = PricingEnv.loadPE(pricingEnv, todayJdt);

        QuoteValue quote;
        double fXRate = 0.0D;

        if (pe != null) {
            if (!toCcy.equals(fromCcy)) {
                quote = pe.getQuoteSet().getFXQuote(fromCcy, toCcy, valDate);

                if ((quote == null) || (quote.getClose() == 0.0D)) {
                    quote = pe.getQuoteSet()
                            .getFXQuote(toCcy, fromCcy, valDate);

                    if ((quote != null) && (quote.getClose() != 0.0D)) {
                        fXRate = 1 / quote.getClose();
                    }
                } else {
                    fXRate = quote.getClose();
                }
            } else {
                fXRate = 1.0D;
            }
        }
        return fXRate;

    }

    /**
     * Format the number parameter to mask ###0.00
     *
     * @param value
     * @return
     * @throws ParseException
     */
    public static String getFormattedAmount(final Number value)
            throws ParseException {
        final DecimalFormat myFormatter = new DecimalFormat("###0.00");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(value);
        } else {
            return "";
        }
    }

    public static HashMap<String ,String> getSchedTaskNameOrReportTemplate(Report report) {
        HashMap<String ,String> result = new HashMap<>();
        String extRef = report.getReportTemplate().get("ScheduledTaskExternalReference");
        if (!Util.isEmpty(extRef)) {
            result.put("ScheduledTask: ", extRef);
        } else if (!Util.isEmpty(report.getReportTemplate().getTemplateName())) {
            result.put("ReportTemplate: ", report.getReportTemplate().getTemplateName());
        }
        return result;
    }
}

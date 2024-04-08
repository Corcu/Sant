package calypsox.tk.anacredit.util;


import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.Arrays;
import java.util.List;

public class AnacreditUtilities {

    public  static Double calculatePM(JDatetime valDatetime, Trade trade, PricerMeasure pm, String pEnv) {
        try {
            PricingEnv pricingEnv = PricingEnv.loadPE(pEnv, valDatetime );
            Pricer pricer = pricingEnv.getPricerConfig().getPricerInstance(trade.getProduct());
            PricerMeasure[] pricerMeasures = new PricerMeasure[] {pm};
            pricer.price(trade, valDatetime, pricingEnv, pricerMeasures);
            Double value = pm.getValue();
            return value;
        } catch (Exception ex) {
            Log.error("AnacreditUtilities", ex);
        }
        return null;
    }

    public static List<ReportRow> geListOfReportRows(DefaultReportOutput reportOutput) {
        ReportRow[] rows = reportOutput.getRows();
        List<ReportRow> reportRows = Arrays.asList(rows);
        return reportRows;
    }

    public static void setRowsToDefaultOutput(String rowDataType, List<ReportRow> items, DefaultReportOutput output) {
        if (output != null) {
            ReportRow[] rows = new ReportRow[items.size()];
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) != null) {
                    ReportRow row = items.get(i);
                    row.setProperty(AnacreditConstants.ROW_DATA_TYPE, rowDataType);
                    rows[i] = row;
                }
            }
            output.setRows(rows);
        }
    }

    /**
     * Get Principal on EUR by default
     * @param value
     * @param origCurrency
     * @param valDate
     * @param env
     * @return
     */
    public static Double convertToEUR(Double value, String origCurrency, JDate valDate, PricingEnv env) {
        if ("EUR".equalsIgnoreCase(origCurrency)) {
            return value;
        }
        Double valueEUR = 0.0;
        try {
            double fxRate = CollateralUtilities.getFXRatebyQuoteSet(valDate, origCurrency, "EUR", env);
            valueEUR = value * fxRate;
            if(valueEUR == 0.0){
                return value;
            }
        } catch (Exception e)  {
            Log.error(Log.CALYPSOX,"Error getting FXQuotes.", e);
        }
        return valueEUR;
    }

}

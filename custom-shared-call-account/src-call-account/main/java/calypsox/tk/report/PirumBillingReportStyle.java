package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.flow.period.PeriodHasAmount;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecFinanceBillingReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;

public class PirumBillingReportStyle extends SecFinanceBillingReportStyle {

    public static final String SOURCE_SYSTEM = "Source System";
    public static final String COUNTRY_CODE = "Country Code";
    public static final String STATUS = "Status";
    public static final String DATA_DATE_PART = "Data Date Part";
    public static final String BUY_SELL = "Buy Sell";
    public static final String PAY_REC_2ND_LEG = "Pay Rec 2nd Leg";
    public static final String REPO_OPEN = "Repo Open";
    public static final String FIX_VAR_RATE_2ND_LEG = "Fix Var Rate 2nd Leg";
    public static final String MAIN_DISPLAY_INDEX_2ND_LEG = "Main Display Index 2nd Leg";
    public static final String RATE_FACTOR_2ND_LEG = "Rate Factor 2nd Leg";
    public static final String CALC_DETAIL_MARGIN_VAUE_0 = "Calc Detail Margin Value 0";
    public static final String CURR_AMT_2NG_LEG_2 = "Current Amount 2nd Leg_2";
    public static final String END_DATE_OPEN = "End Date Open";
    public static final String SECURITY_LOT_SIZE = "Security Lot Size";
    public static final String TRADE_TIPOLOGY = "Trade Tipology";
    public static final String SECURITY_DESCRIPTION = "Security Description";
    public static final String PERIOD_VALUATION_AMT = "Period Valuation Amount";


       List<String>  specialFormatColumns = getSpecialFormatColumns();

    private static List<String>  getSpecialFormatColumns() {
        List<String> specialColumns =   Arrays.asList("Period.Rate", "Period.Amount", "Period.Security nominal", "Period.Security quantity", "Security Lot Size", "Period.Notional" );
        return specialColumns;
    }

    public static final String PAYMENT_CONDITIONS = "Payment Conditions";

    public  final String EMPTY_STRING = "";

    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {

        Trade trade = (Trade)row.getProperty("Trade");

        if (!(trade.getProduct() instanceof SecLending)) {
            return EMPTY_STRING;
        }

        SecLending secLending = (SecLending) trade.getProduct();
        JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);

        if (SOURCE_SYSTEM.equals(columnId)) {
            return "CALYPSO";
        } else if (COUNTRY_CODE.equals(columnId)) {
            return "ESC";
        } else if (STATUS.equals(columnId)) {
            if (trade.getStatus().equals(Status.VERIFIED)
                    || trade.getStatus().equals(Status.TERMINATED)) {
                if (null != secLending.getEndDate()) {
                    if (secLending.getEndDate().lte(valDateTime.getJDate(TimeZone.getDefault()))) {
                        return "DEAD";
                    }
                }
                return "LIVE";
            }
            return "DEAD";
        } else if (DATA_DATE_PART.equals(columnId)) {
            Calendar calMin = valDateTime.getJDate(TimeZone.getDefault()).asCalendar();
            calMin.set(Calendar.DAY_OF_MONTH, calMin.getActualMaximum(Calendar.DAY_OF_MONTH));
            return JDate.valueOf(calMin);

        } else if (BUY_SELL.equals(columnId)) {
            return secLending.isLending() ? "S" : "B";

        } else if (PAY_REC_2ND_LEG.equals(columnId)) {
            if (trade.getQuantity() < 0) {
                return "R";
            }  else  {
                return "P";
            }

        } else if (REPO_OPEN.equals(columnId)) {
            if (secLending.isOpen()) {
                return "Y";
            } else {
                return "N";
            }

        } else if (END_DATE_OPEN.equals(columnId)) {
            if (secLending.isOpen()) {
                Vector  holidays = LocalCache.getCurrencyDefault(trade.getSettleCurrency()).getDefaultHolidays();
                if (holidays == null) {
                    holidays = new Vector();
                    holidays.add("SYSTEM");
                }
                return valDateTime.getJDate(TimeZone.getDefault()).addBusinessDays(1,
                    LocalCache.getCurrencyDefault(trade.getSettleCurrency()).getDefaultHolidays());
            }
            return secLending.getEndDate();

        } else if (FIX_VAR_RATE_2ND_LEG.equals(columnId)) {
            return "F";
        } else if (MAIN_DISPLAY_INDEX_2ND_LEG.equals(columnId)) {
            return EMPTY_STRING;
        } else if (RATE_FACTOR_2ND_LEG.equals(columnId)) {
            return 1;
        } else if (CALC_DETAIL_MARGIN_VAUE_0.equals(columnId)) {
            return 0;
        } else if (CURR_AMT_2NG_LEG_2.equals(columnId)) {
            return 0;

        } else if (SECURITY_LOT_SIZE.equals(columnId)) {

            if (secLending.getSecurity() instanceof Equity) {
                return 1;
            } else if (secLending.getSecurity() instanceof Bond) {
                double faceValue = ((Bond)secLending.getSecurity()).getFaceValue();
                Amount r = new Amount(faceValue,2);
                String s = r.toString();
                s = s.replace(".","");
                s = s.replace(",",".");
                return s;
            }
            return EMPTY_STRING;

        } else if (TRADE_TIPOLOGY.equals(columnId)) {
            return  trade.getProductType() + " " +secLending.getSecurity().getType();
        } else if (PAYMENT_CONDITIONS.equals(columnId)) {
            if ("DFP".equals(secLending.getDeliveryType()))   {
                return "FOP";
            }
            if ("DAP".equals(secLending.getDeliveryType()))   {
                return "DAP";
            }
            return EMPTY_STRING;

        } else if (SECURITY_DESCRIPTION.equals(columnId)) {
            if (secLending != null) {
               StringBuilder result = new StringBuilder();
               result.append(secLending.getSecurity().getType());
                LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(),
                        secLending.getIssuerId());
                if (le != null) {
                    result.append(".")
                            .append(le.getCode()).append("_")
                            .append(le.getName());
                }
                return result;
            }
            return EMPTY_STRING;
        } else if (PERIOD_VALUATION_AMT.equals(columnId)) {
            Amount result = null;
            if (!"AUTO".equals(secLending.getMarkProcedure())) {
                result = (Amount)  super.getColumnValue(row, "Period.Notional", errors);

            } else {
                PeriodHasAmount period = (PeriodHasAmount)row.getProperty("HistoryPeriod");
                if (period != null) {
                    JDate startDate = period.getStartDate();
                    if (!CollateralUtilities.isBusinessDay(period.getStartDate(), new Vector(Arrays.asList("SYSTEM")))) {
                        startDate = CollateralUtilities.getPreviousBusinessDay(startDate, new Vector(Arrays.asList("SYSTEM")));
                    }
                    result = calculatePM(new JDatetime(startDate.getDate()), row, trade, new PricerMeasure(PricerMeasure.NPV_COLLAT));
                }
            }

            if (null != result) {
                double d = Math.abs(result.get());
                result.set(d);
                return formatField(result);

            }

            return "";
        } else  {
            return checkSpecialFormatColumns(row, columnId, errors);
        }

    }

    private Object checkSpecialFormatColumns(ReportRow row, String columnId, Vector errors) {
        Object result = super.getColumnValue(row, columnId, errors);
        if (result != null
                && specialFormatColumns.contains(columnId)) {
            Object s = formatField(result);
            if (s != null) return s;
        }
        return result;
    }

    private Object formatField(Object result) {
        if (result instanceof Amount
                    || result instanceof Rate) {
            String s = result.toString();
            s = s.replace(".","");
            s = s.replace(",",".");
            return s;
        }
        return null;
    }


    public static Amount calculatePM(JDatetime valDatetime, ReportRow row, Trade trade, PricerMeasure pm) {
        try {
            PricingEnv pEnv = (PricingEnv) row.getProperty("PENV");
            Pricer pricer = (Pricer) row.getProperty("PRICER");
            if (pricer != null && pEnv != null) {
                PricerMeasure[] pricerMeasures = new PricerMeasure[] {pm};
                pricer.price(trade, valDatetime, pEnv, pricerMeasures);
                Double value = pm.getValue();
                return new Amount(value, 2);
            }
        } catch (Exception ex) {
            Log.error(Log.CALYPSOX, ex);
        }
        return null;
    }
}
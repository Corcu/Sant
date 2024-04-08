package calypsox.tk.report;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.flow.*;
import com.calypso.tk.product.flow.period.PeriodFactory;
import com.calypso.tk.product.flow.period.PeriodHasAmount;
import com.calypso.tk.product.flow.period.PeriodHasRate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecFinanceBillingReport;
import com.calypso.tk.secfinance.sbl.period.SBLRentPeriod;

import java.util.*;

public class PirumBillingReport extends SecFinanceBillingReport {

    @Override
    public ReportOutput load(Vector errorMsgs) {

        DefaultReportOutput dro = (DefaultReportOutput) super.load(errorMsgs);
        return dro;

    }

    private void addRow(List<ReportRow> derivedRows, ReportRow reportRow, PeriodHasAmount historyPeriod) {
        reportRow.setProperty("HistoryPeriod", historyPeriod);
        derivedRows.add(reportRow);
    }

    @Override
    protected ReportRow[] deriveRows(ReportRow[] rows) {
        List<ReportRow> result = new ArrayList();
        List<CashFlow> processedFlows = new ArrayList<>();
        PricingEnv officialAcc = PricingEnv.loadPE("OFFICIAL_ACCOUNTING", getValuationDatetime());

        for (int i = 0; i < rows.length; i++) {
            ReportRow reportRow = rows[i];
            CashFlow flow = (CashFlow) reportRow.getProperty("Default");

            if (flow.isPrincipal()) {
                continue;
            }

            Trade trade = (Trade)reportRow.getProperty("Trade");
            if (trade.getProduct() instanceof  SecLending) {
                if ("AUTO".equals(((SecLending) trade.getProduct()).getMarkProcedure())) {
                    Pricer pricer = officialAcc.getPricerConfig().getPricerInstance(trade.getProduct());
                    reportRow.setProperty("PRICER", pricer);
                    reportRow.setProperty("PENV", officialAcc);
                }
            }


            if (flow instanceof CashFlowSBLRent) {
                CashFlowSBLRent cfSBLRent = (CashFlowSBLRent)flow;
                Iterator cfIterator = cfSBLRent.getPeriods().iterator();
                while(cfIterator.hasNext()) {
                    SBLRentPeriod sblRentPeriod = (SBLRentPeriod) cfIterator.next();
                    ReportRow row = reportRow.clone();
                    PeriodHasAmount hasRate = PeriodFactory.getHasRate(cfSBLRent, sblRentPeriod);
                    this.addRow(result, row, hasRate);
                    row.setProperty("HistorySecurity", hasRate);
                }
            } else {
                this.addRow(result, reportRow, PeriodFactory.getHasAmount(flow));
            }
        }

        if (this.isDailyBreakdownEnabled()) {
            result = this.applyDailyBreakdown(result);
        }

        return result.toArray(new ReportRow[(result).size()]);
    }

    private List<ReportRow> applyDailyBreakdown(List<ReportRow> rows) {
        List<ReportRow> result = new ArrayList();
        Iterator var3 = rows.iterator();

        while(true) {
            while(var3.hasNext()) {
                ReportRow row = (ReportRow)var3.next();
                PeriodHasAmount period = (PeriodHasAmount)row.getProperty("HistoryPeriod");
                if (!this.isDailyBreakdownApplicable(period)) {
                    result.add(row);
                } else {
                    PeriodHasRate periodHasRate = (PeriodHasRate)period;

                    for(JDate date = periodHasRate.getStartDate(); date.before(periodHasRate.getEndDate()); date = date.addDays(1)) {
                        ReportRow dayRow = row.clone();
                        this.addRow(result, dayRow, PeriodFactory.getHasRate(periodHasRate, date));
                    }
                }
            }

            return result;
        }
    }
    private boolean isDailyBreakdownApplicable(PeriodHasAmount period) {
        if (period != null && period instanceof PeriodHasRate) {
            PeriodHasRate periodHasRate = (PeriodHasRate)period;
            return periodHasRate.isDailyBreakdownApplicable();
        } else {
            return false;
        }
    }


    private boolean isDailyBreakdownEnabled() {
        boolean dailyBreakdown = false;
        if (this._reportTemplate != null) {
            String s = (String)this._reportTemplate.get("DayBreakdown");
            if (!Util.isEmpty(s) && "true".equals(s)) {
                dailyBreakdown = true;
            }
        }

        return dailyBreakdown;
    }

    private void filterCashFlowPeriod(DefaultReportOutput dro) {
        ReportRow[] aux = dro.getRows();
        ArrayList<ReportRow> outputRows = new ArrayList<>();
        if (null != aux && aux.length>0) {
            Calendar calMin = getValuationDatetime().getJDate(TimeZone.getDefault()).asCalendar();
            calMin.set(Calendar.DAY_OF_MONTH, calMin.getActualMinimum(Calendar.DAY_OF_MONTH));

            Calendar calMax = getValuationDatetime().getJDate(TimeZone.getDefault()).asCalendar();
            calMax.set(Calendar.DAY_OF_MONTH, calMax.getActualMaximum(Calendar.DAY_OF_MONTH));

            JDate firstDayOfMonth =  JDate.valueOf(calMin);
            JDate lastDayOfMonth =  JDate.valueOf(calMax);

            for(int i = 0; i < aux.length; ++i) {
                ReportRow reportRow = aux[i];
                CashFlow flow = reportRow.getProperty("Default");

                /*
                // test concerning Period
                PeriodHasAmount period = (PeriodHasAmount)reportRow.getProperty("HistoryPeriod");
                if (period != null
                    && period.getStartDate().before(firstDayOfMonth)
                    && period.getEndDate().before(firstDayOfMonth)) {
                    continue;
                }
                //System.out.println(flow.getStartDate() + " " + flow.getEndDate());
                 */

                // test concerning CF dates
                if (flow.getStartDate().before(firstDayOfMonth)
                            && flow.getEndDate().before(firstDayOfMonth))  {
                    continue;
                }

                if (flow.getStartDate().after(lastDayOfMonth)) {
                    continue;
                }

                if (flow.getEndDate().before(firstDayOfMonth)) {
                    continue;
                }

                if (flow.getEndDate().after(lastDayOfMonth)) {
                    continue;
                }

                outputRows.add(reportRow);
            }
        }
        ReportRow[] out = outputRows.toArray(new ReportRow[outputRows.size()]);
        dro.setRows(out);
    }

}
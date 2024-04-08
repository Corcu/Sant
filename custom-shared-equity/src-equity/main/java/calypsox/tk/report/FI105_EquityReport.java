package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FI105_EquityReport extends TradeReport {

    public ReportOutput load(Vector errorMsgs) {


        if(_countOnly)  {
           return  null;
        }
        setSettlementStart();
        DefaultReportOutput output = (DefaultReportOutput)  super.load(errorMsgs);

        filterTrades(output);
        buildReportBeans(output);
        return output;

    }

    private void filterTrades(DefaultReportOutput output) {
        List<ReportRow> result = new ArrayList<>();
        List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
        final JDate lastBusDayOfPreviousMonth = getLastBusDayOfPreviousMonth();
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");

        rows.stream().forEach(row ->  {
            Trade trade = row.getProperty(ReportRow.TRADE);
            if (!(trade.getProduct() instanceof Equity)) {
                return;
            }

            if (trade.getTradeDate().after(getValuationDatetime())) {
                return;
            }

            if (trade.getStatus().equals(Status.S_CANCELED)
                    && trade.getUpdatedTime().asCalendar().before(lastBusDayOfPreviousMonth)) {
                return;
            }

            if (compareDates(trade.getTradeDate().getJDate(TimeZone.getDefault()), trade.getSettleDate())) {
                return;
            }

            result.add(row);
        });
        output.setRows(result.toArray(new ReportRow[result.size()]));

    }

    private boolean compareDates(JDate date1, JDate date2) {
        if (date1.getMonth() == date2.getMonth() && date1.getYear() == date2.getYear()
                && date2.getDayOfMonth() == date1.getDayOfMonth()) {
            return true;
        }
        return false;
    }

    private void buildReportBeans(DefaultReportOutput output) {
        List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
        JDate lastBusDayOfPreviousMonth = getLastBusDayOfPreviousMonth();
        rows.parallelStream().forEach(row -> {
            Trade trade = row.getProperty(ReportRow.TRADE);
            FI105EquityBean bean = new FI105EquityBean();
            bean.build(trade, getValuationDatetime(), lastBusDayOfPreviousMonth, getPricingEnv());
            row.setProperty(FI105EquityBean.class.getSimpleName(), bean);
        });

    }

    private JDate getLastBusDayOfPreviousMonth() {
        if (getReportTemplate() != null
                && !Util.isEmpty(getReportTemplate().getTemplateName())) {
            Calendar cal = getValuationDatetime().asCalendar();
            cal.add(Calendar.MONTH, -1);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            JDate dateParam = JDate.valueOf(cal.getTime());

            if (!CollateralUtilities.isBusinessDay(dateParam, Util.string2Vector("SYSTEM"))){
                dateParam = CollateralUtilities.getPreviousBusinessDay(dateParam, Util.string2Vector("SYSTEM"));
            };
            return dateParam;
        }
        return null;
    }

    private void setSettlementStart() {
        if (getReportTemplate() != null
                && !Util.isEmpty(getReportTemplate().getTemplateName())) {
            JDate date = getLastBusDayOfPreviousMonth().addBusinessDays(1, getReportTemplate().getHolidays());
            getReportTemplate().put("SettleStartDate",  date.toString());
            setReportTemplate(getReportTemplate());
            getReportTemplate().callBeforeLoad();

        }
    }

}

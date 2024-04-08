package calypsox.tk.report;

import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.util.FdnUtilProvider;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class ABCTradeReport extends TradeReport {


    protected static final String IS_SETTLED_TRADE_PROP="IsSettledTrade";
    private static final long serialVersionUID = -1655127533046540816L;
    int holidaysTenor = 0;
    boolean isEnteredDateLoad = false;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        if (!_countOnly) {
            this.holidaysTenor = getPreviousBusinessDayTenor(this.getValDate());
            List<Long> currentTradeIdList = new ArrayList<>();

            ReportRow[] outputTradeDate = Optional.ofNullable(loadByTradeDate(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);
            ReportRow[] outputEnteredDate = Optional.ofNullable(loadByEnteredDate(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);

            ReportRow[] rows = Stream.concat(Arrays.stream(outputTradeDate),
                    Arrays.stream(outputEnteredDate)).toArray(ReportRow[]::new);

            List<ReportRow> filteredList = filterTradeList(rows, currentTradeIdList);
            return initReportOutput(filteredList);
        } else {
            return super.load(errorMsgs);
        }
    }

    private List<ReportRow> filterTradeList(ReportRow[] rows, List<Long> currentTradeIdList) {
        List<ReportRow> filteredList = new ArrayList<>();
        //BETA FOR DUPLICATES REMOVAL
        for (ReportRow row : rows) {
            Trade trade = row.getProperty(Trade.class.getSimpleName());
            if (!currentTradeIdList.contains(trade.getLongId())) {
                currentTradeIdList.add(trade.getLongId());
                setDealStatusMark(row,trade);
                filteredList.add(row);
            }
        }
        return filteredList;
    }

    public ReportOutput loadByTradeDate(Vector<String> errorMsgs) {
        this.getReportTemplate().put("StartTenor", this.holidaysTenor + "D");
        return super.load(errorMsgs);
    }

    /**
     * @param errorMsgs
     * @return
     */
    public ReportOutput loadByEnteredDate(Vector<String> errorMsgs) {
        setEnteredDateFilteringMode();
        ReportOutput output = super.load(errorMsgs);
        unsetEnteredDateFilteringMode();
        return output;
    }


    private void setEnteredDateFilteringMode() {
        this.isEnteredDateLoad = true;
    }

    private void unsetEnteredDateFilteringMode() {
        this.isEnteredDateLoad = false;
    }

    @Override
    protected String buildQuery(boolean buildQueryForRepoOrSecLending, List<CalypsoBindVariable> bindVariables) {
        String where = super.buildQuery(false, bindVariables);
        String tradeDateSqlId = "trade.trade_date_time";
        if (isEnteredDateLoad) {
            where = where.replaceAll(tradeDateSqlId, "trade.entered_date");
        }
        return where;
    }

    private StandardReportOutput initReportOutput(List<ReportRow> rows) {
        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        ReportRow[] rowArray = Optional.ofNullable(rows).map(r -> rows.toArray(new ReportRow[0])).orElse(new ReportRow[0]);
        standardReportOutput.setRows(rowArray);
        return standardReportOutput;
    }

    private int getPreviousBusinessDayTenor(JDate valDate) {
        Vector<String> holidays = new Vector<>();
        holidays.add("TARGET");
        JDate previousDate = FdnUtilProvider.getDateUtil().previousBusinessDay(valDate, holidays);
        return JDate.getTenor(previousDate, valDate);
    }

    private void setDealStatusMark(ReportRow row,Trade trade){
        row.setProperty(IS_SETTLED_TRADE_PROP,isSettledTrade(trade));
    }
    private boolean isSettledTrade(Trade trade){
        return Optional.ofNullable(trade)
                .map(Trade::getSettleDate)
                .map(sd->sd.before(this.getValDate()))
                .orElse(false);
    }

}

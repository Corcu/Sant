package calypsox.tk.report;

import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.CashFlowReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.Arrays;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RepoFOBOFlowsReport extends CashFlowReport {

    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput defaultReportOutput = null;
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();
        ReportOutput load = super.load(errorMsgs);


        if (load instanceof DefaultReportOutput) {
            defaultReportOutput = (DefaultReportOutput) load;
            ReportRow[] rows = defaultReportOutput.getRows();
            if(!Util.isEmpty(rows)){
                Arrays.stream(rows).parallel().forEach(row -> {
                    Trade trade = (Trade)row.getProperty("Trade");
                    CashFlow cashFlow = (CashFlow) row.getProperty("CashFlow");
                    if(acceptCashFlow(cashFlow)){
                        RepoFOBOFlowsBean bean = new RepoFOBOFlowsBean();
                        bean.init(trade);
                        row.setProperty(RepoFOBOFlowsBean.class.getName(),bean);
                        finalRows.add(row);
                    }
                });
                final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
                defaultReportOutput.setRows(finalReportRows);
            }
        }
        return defaultReportOutput;
    }

    private boolean acceptCashFlow(CashFlow cashFlow){
        JDate valDate = getValDate(TimeZone.getDefault());
        return null!=cashFlow && cashFlow.getStartDate().lte(valDate) && cashFlow.getEndDate().gte(valDate);
    }

}

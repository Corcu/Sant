package calypsox.tk.report;

import calypsox.apps.reporting.AnacreditRepoReportTemplatePanel;
import calypsox.tk.anacredit.processor.AnacreditProcessorRepo;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AnacreditRepoReport extends AnacreditTradeReport
            implements IAnacreditReport {

    public AnacreditRepoReport() {
        super();
    }

    private AnacreditProcessorRepo _processor = new AnacreditProcessorRepo();

    protected void processRows(DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {
        Boolean filterCtas = (Boolean) getReportTemplate().get(AnacreditRepoReportTemplatePanel.CTAS_FILTER);
        if (!Util.isEmpty(output.getRows())) {
            if(filterCtas!=null ? filterCtas : true ) {
                ctasFilter(output);
                reportRows = Arrays.asList(output.getRows());
            }
        }
        _processor.processReportRows(this, output, reportRows, errorMsgs);
    }

    /**
     * Filter trades whit RepoDirection = Repo && tradeDate = reportValDate && tradeDate != repoStartDate
     *
     * @param output
     * @return
     */
    private void ctasFilter(DefaultReportOutput output){
        if(Optional.ofNullable(output).isPresent()){
            ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
            ReportRow[] rows = output.getRows();
            if(!Util.isEmpty(rows)){
                Arrays.stream(rows).parallel().forEach(reportRow -> {
                    Trade trade = (Trade) reportRow.getProperty("Trade");
                    SecFinance secFinance = Optional.ofNullable(trade).map(Trade::getProduct).filter(SecFinance.class::isInstance).map(SecFinance.class::cast).orElse(null);
                    Optional.ofNullable(secFinance).ifPresent(repo ->{
                        if(trade.getProduct() instanceof Repo){
                            String repoDirection = ((Repo)trade.getProduct()).getDirection(Repo.REPO,repo.getSign());
                            JDate tradeDate = trade.getTradeDate().getJDate(TimeZone.getDefault());
                            JDate startDate = repo.getStartDate();
                            JDate valDate = getValDate(TimeZone.getDefault());
                            if("Repo".equalsIgnoreCase(repoDirection)){
                                if(startDate.lte(valDate) || startDate.equals(tradeDate)){
                                    syncList.add(reportRow);
                                }
                            }else{
                                syncList.add(reportRow);
                            }
                        }
                    });
                });
                output.setRows(syncList.toArray(new ReportRow[syncList.size()]));
            }
        }
    }
}

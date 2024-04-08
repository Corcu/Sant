package calypsox.tk.report;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RepoFOBOOperReport extends TradeReport {
    PricingEnv pricingEnv = getPricingEnv();

    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput defaultReportOutput = null;
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();
        ReportOutput load = super.load(errorMsgs);
        HashMap<String,Long> internals = new HashMap<>();
        JDate valDate = getValDate();
        loadPricingEnv();

        if (load instanceof DefaultReportOutput) {
            defaultReportOutput = (DefaultReportOutput) load;
            ReportRow[] rows = defaultReportOutput.getRows();
            if(!Util.isEmpty(rows)){
                Arrays.stream(rows).parallel().forEach(row -> {
                    Trade trade = (Trade)row.getProperty("Trade");
                    RepoFOBOOperBean bean = new RepoFOBOOperBean();
                    bean.init(row,trade,valDate,pricingEnv);
                    row.setProperty(RepoFOBOOperBean.class.getName(),bean);
                    finalRows.add(row);
                });
            }
            final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
            defaultReportOutput.setRows(finalReportRows);
        }
        return defaultReportOutput;
    }


    private boolean isInternal(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }

    private void loadPricingEnv(){
        try {
            if(null==pricingEnv){
                pricingEnv = getDSConnection().getRemoteMarketData().getPricingEnv("DirtyPrice");
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Err: " + e);
        }
    }


}

package calypsox.tk.report;

import calypsox.tk.pledge.util.TripartyPledgeProrateCalculator;
import calypsox.tk.pledge.util.TripartyPledgeProrateCalculatorBean;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RepoTripartyPledgeReport extends TradeReport {
    PricingEnv pricingEnv = new PricingEnv();
    TripartyPledgeProrateCalculator calculator = null;
    HashMap<Long, TripartyPledgeProrateCalculatorBean> repoTripartyPledges = new HashMap<>();

    @Override
    public ReportOutput load(Vector errorMsgs) {
        final DefaultReportOutput load = (DefaultReportOutput)super.load(errorMsgs);
        if(Optional.ofNullable(load).isPresent()){
            init();
            final List<ReportRow> reportRows = Arrays.stream((load).getRows()).collect(Collectors.toList());
            reportRows.forEach(this::groupPledgesByRepoId);

            List<ReportRow> finalReportRows = filterMarginPledges(reportRows);

            repoTripartyPledges.forEach((key, value) -> calculator.calculate(value));
            setRowIds(reportRows);

            final ReportRow[] newReportRows = finalReportRows.toArray(new ReportRow[finalReportRows.size()]);
            load.setRows(newReportRows);
        }


        return load;
    }

    private List<ReportRow> filterMarginPledges(List<ReportRow> reportRows){
        List<ReportRow> finalRows = new ArrayList<>();
        for (ReportRow row : reportRows){
            if(!Optional.ofNullable(row.getProperty("remove")).isPresent()){
                finalRows.add(row);
            }
        }
        return finalRows;
    }


    /**
     * Group pledges by Father Repo Trade ID
     * @param row
     */
    private void groupPledgesByRepoId(ReportRow row){
        final Trade pledgeTrade = Optional.ofNullable(row).map(r -> r.getProperty("Trade")).filter(Trade.class::isInstance).map(Trade.class::cast).orElse(new Trade());
        final Long fatherRepoTradeID = Optional.ofNullable(pledgeTrade.getInternalReference()).map(Long::parseLong).orElse(0L);
        if(repoTripartyPledges.containsKey(fatherRepoTradeID)){
                this.repoTripartyPledges.get(fatherRepoTradeID).addRow(row);
        }else {
            try {
                final Trade repoTrade = DSConnection.getDefault().getRemoteTrade().getTrade(fatherRepoTradeID);
                if(null!=repoTrade && repoTrade.getProduct() instanceof Repo){
                    TripartyPledgeProrateCalculatorBean repoTriparty = new TripartyPledgeProrateCalculatorBean();
                    repoTriparty.setFatherRepo(repoTrade);
                    repoTriparty.addRow(row);
                    repoTripartyPledges.put(repoTrade.getLongId(),repoTriparty);
                }else if(null!=row){
                    row.setProperty("remove",true);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getSimpleName(),e);
            }
        }
    }

    /**
     * @param reportRows
     */
    private void setRowIds(List<ReportRow> reportRows){
        if(!reportRows.isEmpty()){
            AtomicInteger count = new AtomicInteger(1);
            reportRows.forEach(row -> {
                row.setProperty(RepoTripartyPledgeReportTemplate.ROW_ID, count.getAndIncrement());
            });
        }
    }

    private void loadPricingEnv(){
        pricingEnv = getPricingEnv();
        if(!Optional.ofNullable(pricingEnv).isPresent()){
            try {
                pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL");
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading PricingEnv: " + e.getCause());
            }

        }
    }

    /**
     * Init Report
     */
    private void init(){
        loadPricingEnv();
        repoTripartyPledges.clear();
        calculator = new TripartyPledgeProrateCalculator();
    }

}

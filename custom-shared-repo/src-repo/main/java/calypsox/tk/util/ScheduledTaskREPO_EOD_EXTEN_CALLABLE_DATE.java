package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.report.CallExerciseActionEntryReport;
import com.calypso.tk.report.CallExerciseActionEntryReportTemplate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.secfinance.actions.cbl.CBLActionsEntry;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskREPO_EOD_EXTEN_CALLABLE_DATE extends ScheduledTask {

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    @Override
    public String getTaskInformation() {
        return "Apply Call Exercise on EXTENDABLE Repo";
    }


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        ConcurrentLinkedQueue<Trade> tradeToSave = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Trade> errorTrades = new ConcurrentLinkedQueue<>();
        TradeArray tradeArrayToUpdate = loadTrades();

        if(!Util.isEmpty(tradeArrayToUpdate)){

            CallExerciseActionEntryReport report = new CallExerciseActionEntryReport();
            CallExerciseActionEntryReportTemplate template = new CallExerciseActionEntryReportTemplate();
            template.setTrades(tradeArrayToUpdate);
            report.setReportTemplate(template);

            Vector errores = new Vector();
            DefaultReportOutput load = report.load(errores);

            Arrays.stream(tradeArrayToUpdate.getTrades()).parallel().forEach(trade -> {
                SecFinance secFinance = Optional.ofNullable(trade.getProduct()).filter(SecFinance.class::isInstance).map(SecFinance.class::cast).orElse(null);
                if(null!=secFinance && secFinance.isCallExerciseApplicable()){
                    CBLActionsEntry entry1 = new CBLActionsEntry(null);

                    //ValuationInfo info = new ValuationInfo();

                    entry1.setTrade(trade);
                    JDate newEndDate = entry1.getNewEndDate();
                    JDate callableDate = secFinance.getCallableDate();
                    JDate endDate = secFinance.getEndDate();
                    JDate today = JDate.getNow();
                    boolean succes = secFinance.applyExercise(callableDate, today, endDate, trade, today);
                    if(succes){
                        tradeToSave.add(trade);
                    }else {
                        errorTrades.add(trade);
                    }
                }
            });


           List<Trade> trades = new ArrayList(tradeToSave);
            try {
                saveTades(trades);
            } catch (InterruptedException e) {
                Log.error(this,"Error saving trades: " + e.getCause());
            }
        }
        return super.process(ds, ps);
    }


    private TradeArray loadTrades(){
        TradeArray tradeArrayToUpdate = new TradeArray();
        String tradeFilterName = getTradeFilter();
        if(!Util.isEmpty(tradeFilterName)){
            tradeArrayToUpdate = loadTradesByTradeFilter(tradeFilterName);
        }else {
            tradeArrayToUpdate = loadTradesByQuery();
        }
        return tradeArrayToUpdate;
    }


    private TradeArray loadTradesByTradeFilter(String tradeFilterName){
        TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrades2(tradeFilter);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading trades: " + e.getCause());
        }
        return new TradeArray();
    }

    private TradeArray loadTradesByQuery(){
        StringBuilder where = new StringBuilder();
        where.append("trade.product_id = product_repo.product_id ");
        where.append(" AND trade.trade_status IN ('PENDING', 'VERIFIED')");
        where.append(" AND product_repo.MATURITY_TYPE LIKE 'EXTENDABLE' ");
        where.append(" AND TRUNC(CALLABLE_DATE) = "+Util.date2SQLString(JDate.getNow()));
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrades("product_repo", where.toString(), "", null);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading trades by query: " +e.getCause());
        }
        return new TradeArray();
    }


    /**
     * @param tradesToSave
     * @throws InterruptedException
     */
    private void saveTades(List<Trade> tradesToSave) throws InterruptedException {
        Log.system(this.getClass().getSimpleName(), "Saving " + tradesToSave.size() + " trades.");

        if (!Util.isEmpty(tradesToSave)) {
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                int size = 999;
                for (int start = 0; start < tradesToSave.size(); start += size) {
                    int end = Math.min(start + size, tradesToSave.size());
                    final List<Trade> trades = tradesToSave.subList(start, end);
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
                                    } catch (CalypsoServiceException e) {
                                        Log.error(this, "Cannot save trades: " + e);
                                    } catch (InvalidClassException e) {
                                        Log.error(this, "Cannot convert to ExternalArray: " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(20, TimeUnit.MINUTES);
            }
            Log.system(this.getClass().getSimpleName(), "All trades updated.");
        }
    }
}

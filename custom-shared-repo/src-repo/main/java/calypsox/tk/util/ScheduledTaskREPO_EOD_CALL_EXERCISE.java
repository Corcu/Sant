package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.secfinance.actions.CallExerciseActionEntry;
import com.calypso.tk.secfinance.actions.SecFinanceActionEntry;
import com.calypso.tk.secfinance.bulkactions.tradeaction.TradeActionInOut;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.tradeentry.factory.ValuationInfo;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.util.Arrays;
import java.util.TimeZone;

public class ScheduledTaskREPO_EOD_CALL_EXERCISE extends ScheduledTask {

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    @Override
    public String getTaskInformation() {
        return "Apply Call Exercise Action on EXTENDABLE Repo whit Callable Date = Today (by default)";
    }


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        applyCallExerciseAction(loadTrades());
        return super.process(ds, ps);
    }

    /**
     *  Apply Action Call Exercised on trades.
     * @param tradesToExercise
     */
    private void applyCallExerciseAction(TradeArray tradesToExercise){
        final JDate effectiveDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        if(!Util.isEmpty(tradesToExercise)){
            //Apply CallExerciseAction and Save
            Arrays.stream(tradesToExercise.getTrades()).parallel().forEach(trade -> {
                ValuationInfo info = new ValuationInfo(null,getValuationDatetime());
                CallExerciseActionEntry tradeApplicable = SecFinanceActionEntry.newActionEntry("CallExerciseActionEntry", trade, info);
                tradeApplicable.getFieldEntrySet().getAutoDefaultManager().resetAll();
                tradeApplicable.setEffectiveDate(effectiveDate);
                TradeActionInOut tradeActionInOut = new TradeActionInOut();
                tradeActionInOut.setTradeApplicable(tradeApplicable);
                tradeActionInOut.apply(false,true);
            });
        }
    }

    /**
     * @return
     */
    private TradeArray loadTrades(){
        TradeArray tradeArrayToUpdate = new TradeArray();
        String tradeFilterName = getTradeFilter();
        if(!Util.isEmpty(tradeFilterName)){
            tradeArrayToUpdate = loadTradesByTradeFilter();
        }else {
            tradeArrayToUpdate = loadTradesByQuery();
        }
        return tradeArrayToUpdate;
    }

    /**
     * @return Trades by TradeFilter defined on ScheduledTask
     */
    private TradeArray loadTradesByTradeFilter(){
        TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
        if(null!=tradeFilter){
            try {
                return DSConnection.getDefault().getRemoteTrade().getTrades2(tradeFilter);
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading trades: " + e.getCause());
            }
        }
        return new TradeArray();
    }

    /**
     * @return query to load EXTENDABLE Repo on Callable Date = today and Status in ( PENDING and VERIFIED)
     */
    private TradeArray loadTradesByQuery(){
        StringBuilder where = new StringBuilder();
        where.append("trade.product_id = product_repo.product_id ");
        where.append(" AND trade.trade_status IN ('PENDING', 'VERIFIED')");
        where.append(" AND product_repo.MATURITY_TYPE LIKE 'EXTENDABLE' ");
        where.append(" AND TRUNC(CALLABLE_DATE) = "+Util.date2SQLString(getValuationDatetime().getJDate(TimeZone.getDefault())));
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrades("product_repo", where.toString(), "", null);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading trades by query: " +e.getCause());
        }
        return new TradeArray();
    }
}

package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScheduledTaskREPO_RFR_CALENDAR extends ScheduledTask {

    protected static final String  ACTION_TO_APPLY = "Action To Apply";
    protected static final String  INVERT_HOLIDAYS = "Invert Holidays";
    protected static final String  PAYMENT_HOLIDAYS = "Payment Holidays";
    protected static final String  RESET_HOLIDAYS = "Reset Holidays";
    @Override
    public String getTaskInformation() {
        return "Update Cash";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        List<Trade> trades = loadTrades(ds);
        updateTrades(trades);
        saveTrades(trades);
        return super.process(ds, ps);
    }

    private List<Trade> loadTrades(DSConnection ds){
        TradeFilter filter = BOCache.getTradeFilter(ds, getTradeFilter());
        TradeArray trades = getTrades(ds, filter, getDatetime());
        return Arrays.stream(Optional.ofNullable(trades).orElse(new TradeArray()).getTrades()).collect(Collectors.toList());
    }

    private void updateTrades(List<Trade> trades) {
        trades.forEach( trade -> Optional.ofNullable(trade.getProduct())
                .filter(Repo.class::isInstance)
                .map(Repo.class::cast)
                .ifPresent(this::updateCash));
    }

    /**
     * Update Cash object
     * @param repo Repo
     */
    void updateCash(Repo repo){
        final RateIndex rateIndex = Optional.ofNullable(repo).map(Repo::getCash).map(Cash::getRateIndex).orElse(null);
        Cash cash = Optional.ofNullable(repo).map(Repo::getCash).orElse(null);

        if(Optional.ofNullable(cash).isPresent() && Optional.ofNullable(rateIndex).isPresent()){
            //invert holidays
            if(getBooleanAttribute(INVERT_HOLIDAYS)){
                Vector paymentHolidays = cash.getPaymentHolidays();
                cash.setPaymentHolidays(cash.getResetHolidays());
                cash.setResetHolidays(paymentHolidays);
            }else {
                Optional.ofNullable(getAttribute(PAYMENT_HOLIDAYS)).ifPresent(pmt -> cash.setPaymentHolidays(Util.string2Vector(pmt)));
                Optional.ofNullable(getAttribute(RESET_HOLIDAYS)).ifPresent(rst -> cash.setResetHolidays(Util.string2Vector(rst)));
            }

            cash.setUseCustomResetOffsetB(false);
            cash.setPmtOffsetBusDayB(rateIndex.getResetBusLagB());
            cash.setResetOffset(rateIndex.getResetDays());

            cash.setCompoundingMethod("SimpleSpread");
            cash.setResetTiming("BEG_PER");
        }

    }

    private void saveTrades(List<Trade> tradesToSave){
        Action action = Action.valueOf(Optional.ofNullable(getAttribute(ACTION_TO_APPLY)).orElse("UPDATE"));
        if(!Util.isEmpty(action.toString())){
            for(Trade trade : tradesToSave){
                trade.setAction(action);
            }
        }
        if (!Util.isEmpty(tradesToSave)) {
            ExecutorService exec = Executors.newFixedThreadPool(5);
            try {
                int size = 500;
                for (int start = 0; start < tradesToSave.size(); start += size) {
                    int end = Math.min(start + size, tradesToSave.size());
                    final List<Trade> trades = tradesToSave.subList(start, end).stream().collect(Collectors.toList());
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
                try {
                    exec.awaitTermination(60, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Log.error(this,"Error: " + e.getMessage());
                }
            }
        }
        /*
        TradeSaverManager manager = new TradeSaverManager(5);
        manager.saveTrades(tradesToSave, action);
        manager.waitForCompletion();*/
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(ACTION_TO_APPLY).description("Action for apply in trades."));
        attributeList.add(attribute(INVERT_HOLIDAYS).booleanType().description("Invert Holidays Calendars."));
        attributeList.add(attribute(PAYMENT_HOLIDAYS).description(PAYMENT_HOLIDAYS));
        attributeList.add(attribute(RESET_HOLIDAYS).description(RESET_HOLIDAYS));

        return attributeList;
    }

}

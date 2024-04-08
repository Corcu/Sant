package calypsox.tk.util;

import calypsox.tk.pricer.PricerMeasureMTM_NET_MUREX;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;

import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMark;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author x983373
 */

public class ScheduledTaskAllocateMtM extends ScheduledTask {

    private static final int SQL_GET_SIZE = 999;

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    @Override
    public String getTaskInformation() {
        return "Allocate MtM from mother trade";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        JDatetime valDateTime = getValuationDatetime();
        TradeArray tradeArray = new TradeArray();
        if (!Util.isEmpty(getTradeFilter())){
            Log.info(this, "Getting trades based on Trade Filter " + getTradeFilter());
            TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
            if (tradeFilter != null){
                try {
                    tradeArray.addAll(ds.getRemoteTrade().getTrades(tradeFilter, valDateTime));
                }catch (CalypsoServiceException e){
                    Log.info("Cannot get trades for ", e);
                }
            }
        }else {
            tradeArray = getTradesAlloc(this.getDSConnection(), valDateTime);
        }

        double mother_quantity, daughter_quantity;
        double motherNetMurex, daughterNetMurex;
        Vector v;

        List<PLMark> plMarks = new ArrayList<>();

        for (Trade trade : tradeArray.asList()) {

            mother_quantity = trade.getAllocatedQuantity();
            motherNetMurex = getPLValue(trade, valDateTime, this._pricingEnv);

            if (motherNetMurex != 0L){
                v = trade.getRoleAllocations();

                for (int i = 0; i < v.size(); i++){
                    TradeRoleAllocation tr = (TradeRoleAllocation) v.get(i);
                    Trade relatedTrade = tr.getRelatedTrade();

                    if (!Util.isEqualStrings(relatedTrade.getStatus().toString(), Status.CANCELED.toString())){
                        daughter_quantity = tr.getAmount();
                        daughterNetMurex = (Double.parseDouble(formatNumber((daughter_quantity/mother_quantity))) * motherNetMurex);

                        addPLMarkForTrade(plMarks, relatedTrade, daughterNetMurex, valDateTime);
                    }
                }
            }
        }

        try {
            savePLMarks(plMarks);
        }catch (InterruptedException e){
            Log.error(this, "Error saving PLMarks. " + e);
        }
        return true;
    }

    private TradeArray getTradesAlloc(DSConnection ds, JDatetime valDate){
        TradeArray trades = new TradeArray();
        JDate valDat = valDate.getJDate(TimeZone.getDefault());

        int size = SQL_GET_SIZE;
        Log.info(this, "Loading trades in ALLOCATED status");

        StringBuilder where = new StringBuilder();
        try{
            where.append(" TRADE.TRADE_STATUS = ");
            where.append("'" + Status.ALLOCATED.toString() + "'");
            where.append(" AND TRADE.TRADE_DATE_TIME <= ");
            where.append(Util.date2SQLString(valDat));
            where.append(" AND TRADE.SETTLEMENT_DATE >= ");
            where.append(Util.date2SQLString(valDat));

            trades.addAll(ds.getRemoteTrade().getTrades("trade", where.toString(), "", null));
        } catch (CalypsoServiceException e){
            Log.info("Cannot get trades for ", e);
        }

        Log.info(this, trades.size() + " Trades loaded");

        return trades;
    }

    private double getPLValue(Trade trade, JDatetime valDatetime, String env){
        PLMarkValue mtmNetMurexPLValue = Optional.ofNullable(getPLMark(trade, valDatetime, env))
                .map(mark-> CollateralUtilities.retrievePLMarkValue(mark, PricerMeasureMTM_NET_MUREX.MTM_NET_MUREX))
                .orElse(null);

        return mtmNetMurexPLValue != null ? mtmNetMurexPLValue.getMarkValue() : 0L;
    }

    private PLMark getPLMark(Trade trade, JDatetime valDatetime, String env){
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        RemoteMark remoteMark = DSConnection.getDefault().getRemoteMark();
        PLMark plMark = null;
        try {
            plMark = RemoteAPI.getMark(remoteMark, "PL", trade.getLongId(), null, env, valDate);
            if (plMark == null){
                Log.info(this, "No PLMarks for trade " + trade.getLongId());
            }
        }catch (PersistenceException e) {
            Log.error(this, e.getCause());
        }
        return plMark;
    }

    private void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, double netMurexValue, JDatetime valDateTime){
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), trade.getBookId());
        if (book != null){
            plMark.setBookId(book.getId());
        }
        plMark.setValDate(valDateTime.getJDate(TimeZone.getDefault()));
        plMark.setPricingEnvName(this._pricingEnv);
        plMark.setType("PL");

        PLMarkValue markValue = new PLMarkValue();
        markValue.setMarkName("MTM_NET_MUREX");
        markValue.setMarkValue(netMurexValue);
        markValue.setCurrency(trade.getTradeCurrency());
        markValue.setOriginalCurrency(trade.getTradeCurrency());

        plMark.addPLMarkValue(markValue);
        plMarks.add(plMark);
    }

    private void savePLMarks(List<PLMark> plMarks) throws InterruptedException{
        if(!Util.isEmpty(plMarks)){
            int size = SQL_GET_SIZE;
            Log.info(this, "Saving " + plMarks.size() + "PLMarks.");
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try{
                for (int start = 0; start < plMarks.size(); start+=size){
                    int end = Math.min(start + size, plMarks.size());
                    List<PLMark> plMarksToSave = new ArrayList<>(plMarks.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(plMarksToSave, true);
                                    }catch (PersistenceException e){
                                        Log.error(this, "Cannot save PLMarks. " + e);
                                    }
                                }
                            });
                }
            }finally {
                exec.shutdown();
                exec.awaitTermination(40, TimeUnit.MINUTES);
            }
        }
    }

    private String formatNumber(Double number){
        return String.format(Locale.ENGLISH, "%.6f", number);
    }
}

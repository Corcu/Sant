package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;
import java.util.Vector;

public class ScheduledTaskDummyAllocGeneration extends ScheduledTask {

    private static final Action ALLOCATE_DUMMY = Action.valueOf("ALLOCATE_DUMMY");

    @Override
    public String getTaskInformation() {
        return "Creation of dummy trade for PARTIAL_ALLOC trades";
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
                    Log.info(this, "Cannot get trades for ", e);
                }
            }
        }else {
            tradeArray = getTradesPartialAlloc(ds, valDateTime);
        }

        TradeRoleAllocation dummyTrade;
        if (!tradeArray.isEmpty()){
            for (Trade trade: tradeArray.asList()) {
                dummyTrade = createDummyTrade(trade);
                trade.add(dummyTrade);
                trade.addKeyword("HasRoleAllocationDummy", "true");
                trade.setAction(ALLOCATE_DUMMY);
                doAllocation(trade, ds);
            }
        }
        return true;
    }

    private TradeArray getTradesPartialAlloc(DSConnection ds, JDatetime valDateTime) {
        TradeArray trades = new TradeArray();
        JDate valDat = valDateTime.getJDate(TimeZone.getDefault());

        Log.info(this, "Loading trades in PARTIAL_ALLOC status");

        StringBuilder where = new StringBuilder();
        try {
            where.append(" TRADE.TRADE_STATUS = ");
            where.append("'" + Status.PARTIAL_ALLOC.toString() + "'");

            trades.addAll(ds.getRemoteTrade().getTrades("trade", where.toString(), "", null));
        }catch (CalypsoServiceException e){
            Log.info(this, "Cannot get trades for ", e);
        }

        Log.info(this, "Loaded " + trades.size() + " trades");
        return trades;
    }

    private TradeRoleAllocation createDummyTrade (Trade trade){
        TradeRoleAllocation roleAllocation = new TradeRoleAllocation();

        roleAllocation.setRole(trade.getRole());
        roleAllocation.setLegalEntityId(trade.getCounterParty().getId());
        roleAllocation.setTradeLongId(trade.getLongId());
        roleAllocation.setBookId(trade.getBookId());
        roleAllocation.setInitialAndCurrentAmount(trade.computeNominal());
        roleAllocation.setSettleCcy(trade.getSettleCurrency());
        roleAllocation.setFxRate(trade.getSplitBasePrice());
        roleAllocation.addRelatedTradeKeywords("DummyAllocation", "true");
        roleAllocation.addRelatedTradeKeywords("HasRoleAllocationDummy", "");

        return roleAllocation;
    }

    private void doAllocation(Trade trade, DSConnection ds){
        Connection con = null;
        try {
            ds.getRemoteTrade().save(trade);
        }catch (RemoteException e){
            Log.error(this, e);
        }
    }
}

package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;

import java.sql.Connection;
import java.util.Vector;

public class RemoveAllocatedTradeTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Remove CANCELED allocation from blocktrade";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        long blockId = 0L;
        Trade blockTrade;
        if (!Util.isEmpty(trade.getKeywordValue("AllocatedFrom"))){
            blockId = Long.parseLong(trade.getKeywordValue("AllocatedFrom"));
        }

        if (blockId > 0L){
            try {
                blockTrade = DSConnection.getDefault().getRemoteTrade().getTrade(blockId).clone();

                if (blockTrade != null){
                    return deleteThisAllocation(trade, blockTrade);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return true;
    }

    private boolean deleteThisAllocation(Trade trade, Trade blockTrade){
        boolean blockTradeNeedsToBeUpdated = false;
        Vector v = blockTrade.getRoleAllocations();
        Vector<TradeRoleAllocation> finalAllocs = new Vector<>();

        for (Object o : v){
            TradeRoleAllocation roleAllocation = (TradeRoleAllocation) o;
            Trade relatedTrade = roleAllocation.getRelatedTrade();

            if (trade.getLongId() != relatedTrade.getLongId()){
                finalAllocs.add(roleAllocation);
            } else {
                blockTradeNeedsToBeUpdated = true;
            }
        }

        if (blockTradeNeedsToBeUpdated){
            blockTrade.setRoleAllocations(finalAllocs);
            blockTrade.setAction(Action.ALLOCATE);
            blockTrade.setQuantity(blockTrade.getQuantity() + trade.getQuantity());
            try {
                DSConnection.getDefault().getRemoteTrade().save(blockTrade, false);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }

        return true;
    }
}

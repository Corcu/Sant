package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;

import java.sql.Connection;
import java.util.Vector;

/**
 * @author x983373
 */

public class CancelAllocatedDummyTradeTradeRule implements WfTradeRule {

    @Override
    public String getDescription() {
        return "Cancel the allocated dummy trade";
    }

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (dbCon == null){
            return true;
        } else {
            boolean result = true;

            if(Boolean.parseBoolean(trade.getKeywordValue("HasRoleAllocationDummy"))){
                Vector v = trade.getRoleAllocations();
                Trade dummyTrade = null;
                String user = "";
                Vector<TradeRoleAllocation> finalAlloc = new Vector<>();
                if (v != null) {
                    for (Object o : v) {
                        TradeRoleAllocation tr = (TradeRoleAllocation) o;
                        Trade relatedTrade = tr.getRelatedTrade();

                        if (Boolean.parseBoolean(relatedTrade.getKeywordValue("DummyAllocation")) && !Status.S_CANCELED.equals(relatedTrade.getStatus())) {
                            dummyTrade = relatedTrade.clone();
                            user = relatedTrade.getEnteredUser();
                        } else {
                            finalAlloc.add(tr);
                        }
                    }

                    trade.setRoleAllocations(finalAlloc);
                    trade.removeKeyword("HasRoleAllocationDummy");

                    if (dummyTrade != null && TradeWorkflow.isTradeActionApplicable(dummyTrade, Action.CANCEL, dsCon, dbCon)) {
                        dummyTrade.setAction(Action.CANCEL);

                        if(Util.isEmpty(user)){
                            dummyTrade.setEnteredUser(user);
                        }
                        trade.setQuantity(trade.getQuantity() + dummyTrade.getQuantity());

                        try {
                            Connection con = (Connection)dbCon;
                            TradeServerImpl.saveTrade(dummyTrade, con, events, false, true);
                        } catch (CalypsoServiceException | WorkflowException | PersistenceException e) {
                            Log.error(this, e);
                        }
                    }
                }
            }
            return result;
        }
    }
}

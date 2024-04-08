package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SantCheckTransferAgentWaitingAcceptedTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return !Optional.ofNullable(trade).map(t -> t.getKeywordValue("TransferAgentWaitingAccepted")).filter("true"::equalsIgnoreCase).isPresent();
    }
    @Override
    public String getDescription() {
        return "Check TransferAgentWaitingAccepted:true";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}

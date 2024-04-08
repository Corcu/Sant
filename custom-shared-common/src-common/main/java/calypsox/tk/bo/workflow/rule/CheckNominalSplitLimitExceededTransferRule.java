package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class CheckNominalSplitLimitExceededTransferRule extends CheckSplitLimitTransferRule {
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return !super.check(wc, transfer, oldTransfer, trade, messages, dsCon, excps, task, dbCon, events);
    }

    public String getDescription() {
        return "This rule checks the transfer nominal exceeds the agent's or counterparty settlement size limits\n. It returns true if split is to be applied else false\n";
    }
}

package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class SantCheckNotPORefMessageRule extends SantCheckPORefMessageRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (Util.isEmpty(wc.getRuleParam(this.getClass().getSimpleName().replace("MessageRule", "")))) {
            messages.add("Regex pattern not configured.");
            return false;
        }

        boolean check = super.check(wc, message, oldMessage, trade, transfer, new Vector(), dsCon, excps, task, dbCon, events);
        if (check) {
            messages.add(String.format("PO reference %s matches system reference pattern.",  message.getAttribute("PORef")));
        }

        return !check;
    }

    @Override
    public String getDescription() {
        return "True if Message PORef is empty or does not match any of the patterns supplied in the rule parameters.";
    }
}

package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsUtil;

//Project: Eligibility control

public class SantIsEligibleInContractTradeRule implements WfTradeRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
            Vector messages, DSConnection dsCon, Vector excps, Task task,
            Object dbCon, Vector events) {
        // Is MarginCall trade eligible in its contract
        Vector<String> comments = new Vector<String>();
        boolean result = SantConcentrationLimitsUtil.isEligibleInContract(trade,
                comments);

        if (!result && comments != null && comments.size() > 0) {
            task.setComment(comments.get(0));
            for (String comment : comments) {
                messages.add(comment);
            }
        }

        return result;
        // Is MarginCall trade eligible in its contract - End
    }

    @Override
    public String getDescription() {
        return "Checks if this trade is eligible in its contract";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
            Vector messages, DSConnection dsCon, Vector excps, Task task,
            Object dbCon, Vector events) {
        // Do nothing
        return true;
    }

}

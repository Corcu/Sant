package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

/**
 * This Workflow Rule checks if a trade is NOT a reverse trade.
 * 
 * @author Carlos Cejudo
 *
 */
public class SantIsNotReverseTradeTradeRule implements WfTradeRule {

    private static final String DESCRIPTION = "Checks if a trade is NOT a reverse trade.";

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
            Vector messages, DSConnection dsCon, Vector excps, Task task,
            Object dbCon, Vector events) {
        // Return the contrary of whatever the SantIsReverseTrade rule would
        // return for the same parameters.
        boolean isReverse = new SantIsReverseTradeTradeRule().check(wc, trade,
                oldTrade, messages, dsCon, excps, task, dbCon, events);

        return !isReverse;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
            Vector messages, DSConnection dsCon, Vector excps, Task task,
            Object dbCon, Vector events) {
        return true;
    }

}

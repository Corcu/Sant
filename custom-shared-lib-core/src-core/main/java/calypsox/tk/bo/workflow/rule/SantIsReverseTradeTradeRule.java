package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import calypsox.apps.reporting.SantPolandSecurityPledgeUtil;

/**
 * This Workflow Rule checks if a trade is a reverse trade.
 * 
 * @author Carlos Cejudo
 *
 */
public class SantIsReverseTradeTradeRule implements WfTradeRule {

    private static final String DESCRIPTION = "Checks if a trade is a reverse trade.";

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
            Vector messages, DSConnection dsCon, Vector excps, Task task,
            Object dbCon, Vector events) {
        return SantPolandSecurityPledgeUtil.isReverseTrade(trade);
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

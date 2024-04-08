package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public abstract class SantApplyTradeAction implements WfMessageRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        return true;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean res=true;
        if(trade!=null) {
            res=applyTradeAction(trade,message,dsCon);
        }
        return res;
    }

    public abstract boolean applyTradeAction(Trade trade, BOMessage boMessage, DSConnection dsConnection);
}

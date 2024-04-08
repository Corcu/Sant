package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.SetAttributesMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;

import java.sql.Connection;
import java.util.Map;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class SetAttributesAndKeywordsMessageRule extends SetAttributesMessageRule {

    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean res = super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
        if (Util.isEmpty(map)) {
            map = this.getAttributeToValueMap(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        }
        if (!Util.isEmpty(map)) {
            Trade clonedTrade = trade.clone();
            for (String key : map.keySet()) {
                clonedTrade.addKeyword(key, map.get(key));
            }
            clonedTrade.setAction(Action.AMEND);
            try {
                TradeServerImpl.saveTrade(clonedTrade, (Connection) dbCon, events, false, true);
            } catch (CalypsoServiceException | WorkflowException | PersistenceException exc) {
                Log.error(this.getClass().getSimpleName(), "Error while updating Trade's matching status", exc.getCause());
            }
        }
        return res;
    }
}

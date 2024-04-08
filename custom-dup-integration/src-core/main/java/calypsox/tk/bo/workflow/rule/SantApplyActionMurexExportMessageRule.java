package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;

public class SantApplyActionMurexExportMessageRule implements WfMessageRule {
    @SuppressWarnings("unused")
    private static final String KW_ERROR_RAISED_BY_KONDOR = "ERROR_RAISED_BY_KONDOR";
    private static final String SENT = "SENT";
    private static final String CANCELED = "CANCELED";
    private static final String ACTION_EXPORT = "EXPORT";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Apply the action Export to the Trade related to the message.";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {
        // We apply the action EXPORTED to the trade and then we save it.
        try {
            // We check if the Trade is in status 'SENT'.
            if (SENT.equals(trade.getStatus().getStatus()) || CANCELED.equals(trade.getStatus().getStatus())) {
                Trade tradeCloned = (Trade) trade.clone();
                tradeCloned.setAction(Action.valueOf(ACTION_EXPORT));
                // tradeCloned.addKeyword(KW_ERROR_RAISED_BY_KONDOR, "");
                long tradeId = dsCon.getRemoteTrade().save(tradeCloned);
                if (tradeId > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            return false;
        }

        return true;
    }
}

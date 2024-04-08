package calypsox.tk.bo.workflow.rule;

import calypsox.tk.export.ack.TradeAckProcessor;
import com.calypso.infra.util.Util;
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

public class SantApplyActionMurexErrorMessageRule implements WfMessageRule {
    private static final String ERROR_RAISED_BY_MUREX = "MUREX_ERROR";
    private static final String SENT = "SENT";
    private static final String VERIFIED = "VERIFIED";
    private static final String ACTION_ERROR = "MC_ERROR";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Apply the action Error to the Trade related to the message.";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {
        // We apply the action ERROR to the trade and then we save it.
        try {
            // We check if the Trade is in status 'SENT'.
            if (SENT.equals(trade.getStatus().getStatus())||VERIFIED.equals(trade.getStatus().getStatus())) {
                Trade tradeCloned = (Trade) trade.clone();
                tradeCloned.setAction(Action.valueOf(ACTION_ERROR));
                String errorMsg = message.getAttribute(TradeAckProcessor.ERRORS_ATTRIBUTE);
                if ((null != message) && !Util.isEmpty(errorMsg)) {
                    tradeCloned.addKeyword(ERROR_RAISED_BY_MUREX, errorMsg);
                }
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

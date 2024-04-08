package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class SantSettleTradeTransferRule implements WfTransferRule {

    private static final String DEFAULT_ACTION_NAME = "SWIFT_SETTLE";
    private static final int RETRIES = 10;

    private static final String DESCRIPTION = "Applies action SWIFT_SETTLE to the trade that produced this transfer";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer,
                         BOTransfer oldTransfer, Trade trade, Vector messages,
                         DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        boolean result = false;

        long tradeId = transfer.getTradeLongId();
        try {
            if (tradeId > 0) {
                Trade tradeFromMessage = dsCon.getRemoteTrade()
                        .getTrade(tradeId);
                if (tradeFromMessage != null) {
                    result = true;
                }
            }
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format(
                    "Could not retrieve trade %d from transfer %d.", tradeId,
                    transfer.getLongId());
            Log.error(this, errorMessage, e);
        }

        return result;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer,
                          BOTransfer oldTransfer, Trade trade, Vector messages,
                          DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {
        boolean result = false;

        long tradeId = transfer.getTradeLongId();

        result = applyAction(dsCon, tradeId, DEFAULT_ACTION_NAME);

        return result;
    }

    private boolean applyAction(DSConnection dsCon, long tradeId,
                                String actionName) {
        boolean saveOk = false;

        int iTry = 0;
        while (!saveOk && iTry < RETRIES) {
            try {
                Trade trade = dsCon.getRemoteTrade().getTrade(tradeId);
                if (trade != null) {
                    Trade newTrade = (Trade) trade.clone();

                    Action action = Action.valueOf(actionName);
                    newTrade.setAction(action);

                    long newTradeId = dsCon.getRemoteTrade().save(newTrade);
                    if (newTradeId > 0) {
                        saveOk = true;
                    }
                } else {
                    String errorMessage = String.format(
                            "Could not retrieve trade %d from database",
                            tradeId);
                    Log.error(this, errorMessage);
                }
            } catch (CalypsoServiceException e) {
                String errorMessage = String.format(
                        "Could not save trade %d with action \"%s\"", tradeId,
                        actionName);
                Log.error(this, errorMessage, e);
            }

            iTry++;
        }

        return saveOk;
    }

}

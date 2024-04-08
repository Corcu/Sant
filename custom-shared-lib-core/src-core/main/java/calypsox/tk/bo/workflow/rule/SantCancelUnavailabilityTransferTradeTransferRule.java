package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;

import java.sql.Connection;
import java.util.Vector;

/**
 * @author acd
 */
public class SantCancelUnavailabilityTransferTradeTransferRule implements SantUnavailabilityTransferTradeTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Cancel UnavailabilityTransferTrade only form CANCEL or BO_CANCEL action";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (isUnavailabilityTransferTradeCancellationRequired(trade,transfer)) {
            return this.cancelUnavailabilityTrade(trade, transfer, events, excps, dsCon, dbCon);
        }
        return true;
    }

    protected boolean isUnavailabilityTransferTradeCancellationRequired(Trade trade, BOTransfer transfer) {
        return isUnavailabilityTransferTradeActionValid(trade,transfer) && Action.CANCEL.equals(transfer.getAction())
                || "BO_CANCEL".equalsIgnoreCase(transfer.getAction().toString());
    }

    protected boolean cancelUnavailabilityTrade(Trade trade, BOTransfer transfer, Vector events, Vector excps, DSConnection dsCon, Object dbCon) {
        try {
            long unavailabilityTransferTradeId = getUnavailabilityTransferTradeId(transfer);
            Trade unavailabilityTransferTrade = TradeSQL.getTrade(unavailabilityTransferTradeId);
            if (null!=unavailabilityTransferTrade && !Status.isCanceled(unavailabilityTransferTrade.getStatus())) {
                unavailabilityTransferTrade = (Trade) unavailabilityTransferTrade.cloneIfImmutable();
                unavailabilityTransferTrade.setAction(Action.CANCEL);
                TradeServerImpl.saveTrade(unavailabilityTransferTrade, (Connection) dbCon, events);
            }
            return true;
        } catch (Exception e) {
            Log.error(this, e);
            return false;
        }
    }

}

package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 */
public class SantSettleUnavailabilityTransferTransferRule implements SantUnavailabilityTransferTradeTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Move to SETTLED the Unavailability Transfer related whit the original Transfer Agent";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return moveToSettled(transfer,trade,dsCon);
    }

    protected boolean moveToSettled(BOTransfer transfer, Trade trade, DSConnection dsCon){
        Long unavailabilityTransferTradeId = getUnavailabilityTransferTradeId(transfer);

        if(unavailabilityTransferTradeId>0 && isUnavailabilityTransferTradeActionValid(trade,transfer)){

            TransferArray boTransfers = loadBOTransfers(unavailabilityTransferTradeId,dsCon);
            BOTransfer transferToSettle = Arrays.stream(Optional.ofNullable(boTransfers)
                            .map(TransferArray::getTransfers)
                            .orElse(new BOTransfer[0]))
                    .max(Comparator.comparing(BOTransfer::getLongId)).orElse(null);
            return this.applySettleAction(transferToSettle, trade,dsCon);
        }
        return true;
    }

    protected boolean applySettleAction(BOTransfer boTransfer, Trade trade, DSConnection dsCon){
        if(null!=boTransfer){
            try {
                BOTransfer clonedTransfer = (BOTransfer) boTransfer.cloneIfImmutable();
                clonedTransfer.setAction(Action.SETTLE);
                if(BOTransferWorkflow.isTransferActionApplicable(clonedTransfer,trade, Action.SETTLE,dsCon)){
                    long save = dsCon.getRemoteBO().save(clonedTransfer, 0L, "");
                    return save>0;
                }
            } catch (CloneNotSupportedException | CalypsoServiceException e) {
                Log.error(this,"Error: " + e.getMessage());
            }
        }
        return false;
    }

    protected TransferArray loadBOTransfers(long tradeId, DSConnection dsCon){
        try {
            return dsCon.getRemoteBO().getBOTransfers(tradeId);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading BOTrades fro trade: " + tradeId + " " + e.getCause());
        }
        return new TransferArray();

    }



}

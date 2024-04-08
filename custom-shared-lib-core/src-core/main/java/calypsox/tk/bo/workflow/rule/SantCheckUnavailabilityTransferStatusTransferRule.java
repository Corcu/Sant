package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Comparator;
import java.util.Vector;

/**
 * @author acd
 */
public class SantCheckUnavailabilityTransferStatusTransferRule implements SantUnavailabilityTransferTradeTransferRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if(isUnavailabilityTransferTradeActionValid(trade,transfer)){
            return checkUnavailabilityTransferSettledStatus(trade,transfer,dsCon);
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Check Status of the transfer for the Unavailability Transfer Trade";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    private boolean checkUnavailabilityTransferSettledStatus(Trade trade, BOTransfer transfer, DSConnection dsCon){
        Long unavailabilityTransferTradeId = getUnavailabilityTransferTradeId(transfer);
        if(unavailabilityTransferTradeId>0){
            try {
                TransferArray boTransfers = dsCon.getRemoteBO().getBOTransfers(unavailabilityTransferTradeId);
                if(!Util.isEmpty(boTransfers)){ //Get last created transfer for the UV
                    return boTransfers.stream().max(Comparator.comparing(BOTransfer::getLongId))
                            .filter(t -> t.getStatus().toString().equalsIgnoreCase(Status.SETTLED))
                            .isPresent();
                }
                return false;
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading BOTrades fro trade: " + unavailabilityTransferTradeId + " " + e.getCause());
            }
        }
        return true;
    }

}

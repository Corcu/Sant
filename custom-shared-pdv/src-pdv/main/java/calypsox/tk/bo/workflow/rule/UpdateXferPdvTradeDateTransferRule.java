package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.Vector;

public class UpdateXferPdvTradeDateTransferRule implements WfTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if(null!=transfer && transfer.getNettedTransfer() && transfer.getProductType().equalsIgnoreCase("SecLending")
                && transfer.getNettingType().contains("PairOff")
                && transfer.getTransferType().contains("SECURITY")){
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {
            TransferArray nettedTransfers = dsCon.getRemoteBO().getNettedTransfers(transfer.getLongId());
            double xferValue = 0.0;
            BOTransfer maxXfer = null;
            if(!Util.isEmpty(nettedTransfers)){
                boolean security = Arrays.stream(nettedTransfers.getTransfers()).anyMatch(xfer -> !xfer.getTransferType().equalsIgnoreCase("SECURITY"));
                if(!security){
                    for(BOTransfer trans : nettedTransfers.getTransfers()){
                        double abs = Math.abs(trans.getNominalAmount());
                        if(xferValue < abs){
                            xferValue = abs;
                            maxXfer = trans;
                        }
                    }
                    transfer.setTradeDate(maxXfer.getTradeDate());
                    return true;
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading Transfer for nettedTransfer: " + e.getCause());
        }
        return false;
    }
}

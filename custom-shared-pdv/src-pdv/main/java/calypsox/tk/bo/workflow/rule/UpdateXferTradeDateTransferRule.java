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
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.util.Vector;

/**
 * @author acd
 */
public class UpdateXferTradeDateTransferRule implements WfTransferRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Update Trade Date for Netted Transfer";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
       if( isTransferAccepted(transfer,trade)){
           try {
               TransferArray nettedTransfers = dsCon.getRemoteBO().getNettedTransfers(transfer.getLongId());
               double xferValue = 0.0;
               BOTransfer maxXfer = null;
               if (!Util.isEmpty(nettedTransfers)) {
                   for (BOTransfer trans : nettedTransfers.getTransfers()) {
                       double abs = Math.abs(trans.getSettlementAmount());
                       if (xferValue < abs) {
                           xferValue = abs;
                           maxXfer = trans;
                       }
                   }
                   if(null!=maxXfer){
                       transfer.setTradeDate(maxXfer.getTradeDate());
                   }
                   return true;
               }
           } catch (CalypsoServiceException e) {
               Log.error(this, "Error loading Transfer for nettedTransfer: " + e.getCause());
           }
       }
        return true;
    }


    private boolean isTransferAccepted(BOTransfer transfer, Trade trade){
        if(null!=transfer && !transfer.getNettingType().contains("NONE")) {
            String productType = trade.getProductType();
            Vector<String> updateXferTradeDateProducts = LocalCache.getDomainValues(DSConnection.getDefault(), "UpdateXferTradeDateProducts");
            if(!Util.isEmpty(updateXferTradeDateProducts)){
                for(String value : updateXferTradeDateProducts){
                    if(value.equalsIgnoreCase(productType)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

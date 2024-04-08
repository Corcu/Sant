package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.UpdateCASecurityLinkedXferTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;


public class SantUpdateCARFSecurityLinkedXferTransferRule extends UpdateCASecurityLinkedXferTransferRule {


    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (!this.acceptRF(transfer, trade)) {
            return true;
        } else if (transfer.getLongId() == 0L) {
            messages.add("Transfer not saved yet");
            return false;
        } else {
            return true;
        }
    }


    protected boolean acceptRF(BOTransfer transfer, Trade trade) {
        if (transfer.getTradeLongId() != 0L && !transfer.getNettedTransfer() && !transfer.isSecurity()) {
            if (trade != null && trade.getProduct() instanceof CA) {
                CA ca = (CA)trade.getProduct();
                return !(ca.getUnderlying() instanceof Bond);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}

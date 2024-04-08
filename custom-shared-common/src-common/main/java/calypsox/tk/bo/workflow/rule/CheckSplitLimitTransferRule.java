package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class CheckSplitLimitTransferRule extends com.calypso.tk.bo.workflow.rule.CheckSplitLimitTransferRule {

//42169501
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        Vector<BOTransfer> transfers = new Vector<>();
        BOTransfer newTransfer;
        try {
            newTransfer = (BOTransfer)transfer.clone();
        } catch (CloneNotSupportedException exc) {
            Log.error(this, exc);
            return false;
        }

        transfers.addElement(newTransfer);
        CustomSplitProductHandler.addNominalSplitTransfers(trade, transfers, excps, dsCon);
        return transfers.size() == 1;
    }
}

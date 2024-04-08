package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;
import org.jfree.util.Log;

import java.sql.Connection;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AuthorizeReclaimTaxTransferTransferRule implements WfTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {
            TransferArray boTransfers = dsCon.getRemoteBO().getBOTransfers(trade.getLongId());
            List<BOTransfer> reclaim_tax = boTransfers.stream().filter(a -> a.getTransferType().equalsIgnoreCase("RECLAIM_TAX"))
                    .filter(b -> Util.isSame(b.getStatus(), Status.PENDING)).collect(Collectors.toList());
            if (!reclaim_tax.isEmpty()) {
                String action = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "ReclaimTaxAction");
                if (action != null) {
                    for (BOTransfer transferObj : reclaim_tax) {
                            BOTransfer xfer = (BOTransfer) transferObj.cloneIfImmutable();
                            if (BOTransferWorkflow.isTransferActionApplicable(xfer, trade, Action.valueOf(action), dsCon, dbCon)) {
                                xfer.setAction(Action.valueOf(action));
                                BackOfficeServerImpl.save(xfer, 0L, (String) null, "Updated by message workflow rule " + this.getClass().getName(), (Connection) dbCon, events);
                            }
                        }
                }
            }
        } catch (Exception e) {
            Log.info("Could not apply action on transfer", e);
        }
        return true;
    }
}

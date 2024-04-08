package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.Optional;
import java.util.Vector;

public class SantCheckSplitTransferRule implements WfTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (transfer.getNettedTransfer() && "SECURITY".equals(transfer.getTransferType())) {
            try {
                TransferArray underlyings = Util.isEmpty(transfer.getUnderlyingTransfers()) ?
                        (dbCon == null ? dsCon.getRemoteBO().getNettedTransfers(transfer.getLongId()) : BOTransferSQL.getNettedTransfers(transfer.getLongId(), (Connection) dbCon)) : transfer.getUnderlyingTransfers();


                if (underlyings.stream().anyMatch(x -> !Status.S_CANCELED.equals(x.getStatus()) && "SECURITY".equals(x.getTransferType()))) {
                    Optional<BOTransfer> cashDap = underlyings.stream().filter(x -> !Status.S_CANCELED.equals(x.getStatus()) && !"SECURITY".equals(x.getTransferType())).findAny();
                    if (cashDap.isPresent()) {
                        messages.add(String.format("Calypso issues. Splitting of SECURITY transfers with SECURITY and CASH underlyings causes breaks in inventory positions, See DAP Cash Underlying Transfer %s.", cashDap.get()));
                        return false;
                    }
                }

            } catch (Exception e) {
                messages.add(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Workaround for a Calypso issue. Calypso incorrectly splits netted SECURITY transfers with DAP PAYMENT/RECEIPT underlings causing breaks in inventory.\n" +
                "FALSE is netting key allows cash and security mix and netted transfer has DAF cash underlyings, TRUE otherwise";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}

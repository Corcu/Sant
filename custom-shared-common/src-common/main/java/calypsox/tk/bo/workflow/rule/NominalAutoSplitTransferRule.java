package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.sql.SeedAllocSQL;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.Vector;


public class NominalAutoSplitTransferRule extends com.calypso.tk.bo.workflow.rule.AutoSplitTransferRule {


    @Override
    //BETA
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (transfer.getLongId() <= 0L) {
            if (dbCon == null) {
                return false;
            }

            try {
                if (transfer.getAllocatedLongSeed() <= 0L) {
                    transfer.setAllocatedLongSeed(SeedAllocSQL.nextSeedLong("transfer", 1));
                }
            } catch (Exception exc) {
                Log.error(this, "Failed to allocate transfer seed", exc);
                return false;
            }
        }

        Vector<BOTransfer> transfers = new Vector<>();
        transfers.addElement(transfer);
        CustomSplitProductHandler.addNominalSplitTransfers(trade, transfers, excps, dsCon);
        if (transfers.size() == 1) {
            return false;
        } else if (dbCon == null) {
            return true;
        } else {
            try {
                Vector splitEvents = BackOfficeServerImpl.splitTransfers(transfer, new TransferArray(transfers), false, (Connection)dbCon);

                for(int i = 0; i != splitEvents.size(); ++i) {
                    events.addElement(splitEvents.elementAt(i));
                }

                return true;
            } catch (Exception exc) {
                Log.error(this, exc);
                return false;
            }
        }
    }
}

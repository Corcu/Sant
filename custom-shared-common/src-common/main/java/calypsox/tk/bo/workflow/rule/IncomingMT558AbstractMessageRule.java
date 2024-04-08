package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * IncomingMT558AbstractMessageRule common methods for MT558
 *
 * @author Ruben Garcia
 */
public abstract class IncomingMT558AbstractMessageRule implements WfMessageRule {

    /**
     * Load trade transfer NOT canceled
     *
     * @param tradeId the outgoing trade id
     * @param dsCon   the Data Server connection
     * @param con     the SQL connection
     * @return the trade transfers
     * @throws Exception if error
     */
    protected TransferArray loadTradeTransfers(long tradeId, DSConnection dsCon, Connection con) throws Exception {
        if (tradeId > 0L) {
            String where = " trade_id = ? AND transfer_status <> 'CANCELED' ";
            List<CalypsoBindVariable> bindVariables = Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.LONG, tradeId));
            if (con != null) {
                return BOTransferSQL.load(where, con, bindVariables);
            } else {
                return dsCon.getRemoteBO().getBOTransfers(where, bindVariables);
            }
        }
        return null;
    }

    /**
     * Check if transfer is eligible for update like rule MatchIncomingTripartyRepoMessageRule
     *
     * @param xfer the current transfer to filter
     * @return true if is eligible
     */
    protected boolean isTransferEligibleForUpdate(BOTransfer xfer) {
        if (xfer.getNettedTransferLongId() > 0L) {
            return false;
        } else if (xfer.getNettedTransfer() && !this.isTradeNettingSettlement(xfer)) {
            return false;
        }
        return true;
    }

    /**
     * Check if transfer is trade netting settlement
     *
     * @param transfer the current transfer
     * @return true if transfer netting type contains key TradeId
     */
    protected boolean isTradeNettingSettlement(BOTransfer transfer) {
        if (!Util.isEmpty(transfer.getNettingType())) {
            Map<String, String> nettingKeys = BOCache.getNettingConfig(DSConnection.getDefault(), transfer.getNettingType());
            return !Util.isEmpty(nettingKeys) && nettingKeys.containsKey("TradeId");
        }
        return false;
    }

    /**
     * Save the transfer with new action
     *
     * @param transfer       the transfer to save
     * @param trade          the current trade
     * @param tradeTransfers the trade transfers
     * @param xferAction     the new action
     * @param events         the events vector
     * @param con            the SQL connection
     * @param dsCon          the Data Server connection
     */
    protected void saveTransfer(BOTransfer transfer, Trade trade, TransferArray tradeTransfers, Action xferAction,
                                Vector events, Connection con, DSConnection dsCon) {
        try {
            BOTransfer transferToSave = (BOTransfer) transfer.clone();
            transferToSave.setAction(xferAction);
            TransferArray transfers = new TransferArray();
            TransferArray payments = new TransferArray();
            TransferArray links = new TransferArray();

            if (transferToSave.getNettedTransfer()) {
                payments.add(transferToSave);
                for (BOTransfer bTransfer : tradeTransfers) {
                    if (bTransfer.getNettedTransferLongId() == transferToSave.getLongId() &&
                            !Status.isCanceled(bTransfer.getStatus())) {
                        bTransfer = (BOTransfer) bTransfer.clone();
                        bTransfer.setAction(transferToSave.getAction());
                        transfers.add(bTransfer);
                        links.add(transferToSave);
                        links.add(bTransfer);
                    }
                }
            } else {
                transfers.add(transferToSave);
            }

            if (con != null) {
                if (events == null) {
                    events = new Vector<>();
                }
                events.addAll(BackOfficeServerImpl.saveTransfers(0L, null, transfers, payments,
                        links, trade, new ExternalArray(), null, new Vector(), con));
            } else {
                dsCon.getRemoteBackOffice().saveTransfers(0L, null, transfers, payments, links,
                        new ExternalArray());
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
    }
}

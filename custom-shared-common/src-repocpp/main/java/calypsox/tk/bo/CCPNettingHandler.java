package calypsox.tk.bo;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.DefaultNettingHandler;
import com.calypso.tk.core.*;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.HashMap;

public class CCPNettingHandler extends DefaultNettingHandler {

    protected boolean[] acceptNettedTransferFromDB(BOTransfer nettedTransfer, Trade trade, TransferArray workingPayments, BOTransfer transfer, HashMap keys, Connection con) throws PersistenceException {
        boolean[] ret = new boolean[]{false, false};
        if (Status.isCanceled(nettedTransfer.getStatus())) {
            Log.debug(Log.NETTING, String.format("Rejected CANCELED netted xfer %s.", nettedTransfer));
            return ret;
        }
        if (workingPayments.indexOf(nettedTransfer) < 0) {
            long parentLongId = 0L;
            if (transfer.getLinkedLongId() > 0L) {
                parentLongId = getNettedParentTransferId(transfer, con);
            }

            if (parentLongId > 0L) {
                Log.debug(Log.NETTING, String.format("Rejected UNSPLIT netted xfer %s.", nettedTransfer));
                return ret;
            }

            if (!checkKey(nettedTransfer, transfer, keys)) {
                Log.debug(Log.NETTING, String.format("REJT - Check Key, transfer %s", nettedTransfer));
                return ret;
            }
            ret[1] = true;
            if (!isActionPossible(nettedTransfer, trade, Action.AMEND, transfer, con)) {
                Log.debug(Log.NETTING, String.format("REJT - Action AMEND not applicable to %s", nettedTransfer));
                return ret;
            }
            ret[0] = true;
            return ret;
        }

        Log.debug(Log.NETTING, String.format("REJT - Xfer is in working Payment list %s.", nettedTransfer));
        return ret;
    }

    protected boolean[] acceptNettedTransferFromPayments(BOTransfer nettedTransfer, Trade trade, BOTransfer transfer, HashMap keys, boolean engineProcessB, Connection con) throws PersistenceException {
        boolean[] ret = new boolean[]{false, false};
        if (!nettedTransfer.getAction().equals(Action.CANCEL)) {
            if (nettedTransfer.getLongId() > 0L && nettedTransfer.getAction().equals(Action.UNSPLIT)) {
                Log.debug(Log.NETTING, "REJT - Xfer is being UNSPLIT");
                return ret;
            }

            if (nettedTransfer.getLongId() > 0L && nettedTransfer.getAction().equals(Action.UNASSIGN)) {
                Log.debug(Log.NETTING, "REJT - Xfer is being UNASSIGN");
                return ret;
            }

            long parentLongId = 0L;
            if (transfer.getLinkedLongId() > 0L) {
                parentLongId = getNettedParentTransferId(transfer, con);
            }

            if (parentLongId > 0L && nettedTransfer.getParentLongId() != parentLongId && nettedTransfer.getLongId() != 0L) {
                Log.debug(Log.NETTING, "REJT - Not the xfer to UNSPLIT/UNASSIGN");
                return ret;
            }

            if (!checkKey(nettedTransfer, transfer, keys)) {
                Log.debug(Log.NETTING, "REJT - Check Key");
                return ret;
            }

            if (engineProcessB && keys.get("TradeId") != null && keys.get("NoTradeReversal") == null) {
                if (nettedTransfer.getLongId() > 0L) {
                    return ret;
                }
                return new boolean[]{true, true};
            }

            ret[1] = true;
            if (nettedTransfer.getLongId() > 0L && (keys.get("TradeId") == null || keys.get("NoTradeReversal") != null || !engineProcessB) && !isActionPossible(nettedTransfer, trade, Action.AMEND, transfer, con)) {
                Log.debug(Log.NETTING, "REJT - Action AMEND not applicable");
                return ret;
            }
            return new boolean[]{true, true};

        }
        return ret;
    }


    private boolean checkKey(BOTransfer nettedTransfer, BOTransfer transfer, HashMap<?, ?> keys) throws
            PersistenceException {
        boolean allowCashSecurityMixDiffCcy = keys.get("AllowCashSecurityMixDiffCcy") != null;
        boolean allowCashSecurityMix = keys.get("AllowCashSecurityMix") != null || allowCashSecurityMixDiffCcy;
        boolean allowCrossSecurityNettingZeroQty = Util.isTrue(transfer.getAttribute("CrossSecNettingZeroQtyAllowed"));
        boolean allowCrossSecurityNettingZeroQtyNetted = Util.isTrue(nettedTransfer.getAttribute("CrossSecNettingZeroQtyAllowed"));


        if (allowCrossSecurityNettingZeroQty && !allowCrossSecurityNettingZeroQtyNetted)
            return false;

        if (allowCashSecurityMix && allowCrossSecurityNettingZeroQty
                && transfer.getTransferType().equals("SECURITY")
                && nettedTransfer.getProductId() != transfer.getProductId()) {
            BOTransfer transferClone;
            try {
                transferClone = (BOTransfer) transfer.clone();
            } catch (CloneNotSupportedException e) {
                throw new PersistenceException(String.format("Cannot clone transfer %s", transfer), e);
            }
            transferClone.setProductId(nettedTransfer.getProductId());
            return nettedTransfer.checkKey(transferClone, keys);
        }
        return nettedTransfer.checkKey(transfer, keys);
    }

}

package calypsox.tk.bo;

import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeRoleFinder;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.Vector;

/**
 * RepoRoleFinder modify the SDI to a custom SDI if the transfer has custom SDI and it is a message to PaymentsHub.
 *
 * @author Ruben Garcia
 */
public class RepoRoleFinder extends TradeRoleFinder {

    @Override
    public Vector getRolesInTransfer(String role, BOTransfer transfer, Trade trade, Vector exceptions, AdviceConfig config, DSConnection dsCon) {
        if (transfer != null && PaymentsHubUtil.hasCustomSDIs(transfer) && config != null &&
                !Util.isEmpty(config.getMessageType()) && ("PAYMENTHUB_PAYMENTMSG".equals(config.getMessageType()) ||
                "PAYMENTHUB_RECEIPTMSG".equals(config.getMessageType()))) {
            BOTransfer clonedTransfer = null;
            try {
                clonedTransfer = (BOTransfer) transfer.clone();
            } catch (CloneNotSupportedException e) {
                Log.error(this, e);
            }
            PaymentsHubUtil.buildPHCustomSDIs(clonedTransfer);
            return super.getRolesInTransfer(role, clonedTransfer, trade, exceptions, config, dsCon);
        }
        return super.getRolesInTransfer(role, transfer, trade, exceptions, config, dsCon);
    }
}

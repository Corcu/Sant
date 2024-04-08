/**
 *
 */
package calypsox.tk.bo;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * Custom SDISelector for margin call trades
 *
 * @author aela
 *
 */
@SuppressWarnings("rawtypes")
public class MarginCallSDISelector extends com.calypso.tk.bo.SimpleTransferSDISelector {

    @Override
    public Vector getValidSDIList(Trade trade, TradeTransferRule transfer, JDate settleDate, String legalEntity,
                                  String legalEntityRole, Vector exceptions, boolean includeNotPreferred, DSConnection dsCon) {

        Vector validSDI = super.getValidSDIList(trade, transfer, settleDate, legalEntity, legalEntityRole, exceptions,
                includeNotPreferred, dsCon);
        //
        Product product = trade.getProduct();
        if (validSDI == null) {
            Log.error(this, "getValidSDIList returned null");
            return null;
        }
        Vector finalSDI = (Vector) validSDI.clone();
        if (!Util.isEmpty(validSDI) && (product instanceof MarginCall)) {
            MarginCall mrgCall = (MarginCall) product;
            long tradeMCCIdValue = mrgCall.getLinkedLongId();
            for (int i = 0; i < validSDI.size(); i++) {
                SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) validSDI.get(i);

                if ((sdi != null) && sdi.getDirectRelationship() && transfer.getCounterPartyRole().equals("Client")) {
                    // limit the use of this selector to margin call trades
                    int sdiMCCIdValue = -1;
                    Account account = BOCache.getAccount(dsCon, sdi.getGeneralLedgerAccount());
                    if (account != null) {
                        String sdiMCCId = account.getAccountProperty("MARGIN_CALL_CONTRACT");
                        try {
                            sdiMCCIdValue = Integer.parseInt(sdiMCCId);

                        } catch (NumberFormatException e) {
                            sdiMCCIdValue = 0;
                            Log.error(this, e);
                        }
                    }
                    if (tradeMCCIdValue != sdiMCCIdValue) {
                        finalSDI.remove(sdi);
                    }
                }
            }
        }
        return finalSDI;
    }
}

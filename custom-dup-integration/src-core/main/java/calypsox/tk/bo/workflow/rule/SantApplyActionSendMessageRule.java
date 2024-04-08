package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.Vector;

public class SantApplyActionSendMessageRule extends SantApplyTradeAction {

    private static final String VERIFIED = "VERIFIED";
    private static final String ACTION_SEND = "SEND";
    private static final String DV_ADDRESS_TYPES = "SendTradeRuleAddressTypes";

    @Override
    public String getDescription() {
        return "Apply the action Send to the Trade related to the message.";
    }

    @Override
    public boolean applyTradeAction(Trade trade,BOMessage message,DSConnection dsCon) {
        // We apply the action SEND to the trade and then we save it.
        try {
            Vector<String> sendTradeRuleAddressTypes = LocalCache.getDomainValues(dsCon,DV_ADDRESS_TYPES);
            if (!Util.isEmpty(sendTradeRuleAddressTypes) &&
                    sendTradeRuleAddressTypes.contains(message.getAddressMethod())) {
                if (VERIFIED.equals(trade.getStatus().getStatus())) {
                    Trade tradeCloned = trade.clone();
                    tradeCloned.setAction(Action.valueOf(ACTION_SEND));
                    long tradeId = dsCon.getRemoteTrade().save(tradeCloned);
                    return (tradeId > 0);
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            return false;
        }

        return true;
    }
}

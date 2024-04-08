package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;

public class SantApplyActionCancelMessageRule extends SantApplyTradeAction {

    private static final String WAITING_CANCEL = "WAITING_CANCEL";
    private static final String ACTION_CANCEL = "CANCEL";

    @Override
    public String getDescription() {
        return "Apply the action Error to the Trade related to the message.";
    }

    @Override
    public boolean applyTradeAction(Trade trade,BOMessage message,DSConnection dsCon){
    // We apply the action CANCEL to the trade and then we save it.
        try {
            // We check if the Trade is in status 'WAITING_CANCEL'.
            if (WAITING_CANCEL.equals(trade.getStatus().getStatus())) {
                Trade tradeCloned = trade.clone();
                tradeCloned.setAction(Action.valueOf(ACTION_CANCEL));
                long tradeId = dsCon.getRemoteTrade().save(tradeCloned);
                if (tradeId > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            return false;
        }

        return true;
    }
}

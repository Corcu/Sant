package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;

public class SantApplyActionExportMessageRule extends SantApplyTradeAction {
    @SuppressWarnings("unused")
    private static final String KW_ERROR_RAISED_BY_KONDOR = "ERROR_RAISED_BY_KONDOR";
    private static final String SENT = "SENT";
    private static final String ACTION_EXPORT = "EXPORT";

    @Override
    public String getDescription() {
        return "Apply the action Export to the Trade related to the message.";
    }

    @Override
    public boolean applyTradeAction(Trade trade,BOMessage message,DSConnection dsCon){
    // We apply the action EXPORTED to the trade and then we save it.
        try {
            // We check if the Trade is in status 'SENT'.
            if (SENT.equals(trade.getStatus().getStatus())) {
                Trade tradeCloned = trade.clone();
                tradeCloned.setAction(Action.valueOf(ACTION_EXPORT));
                // tradeCloned.addKeyword(KW_ERROR_RAISED_BY_KONDOR, "");
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

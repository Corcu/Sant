package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class ECMSMarginCall extends EmailDataBuilder {


    public ECMSMarginCall(BOMessage message) {
        super(message);
        setFileAttached(false);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    @Override
    public String getBody() {
        return null;
    }

    private String buildSubject() {
        Trade trade = new Trade();
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(message.getTradeLongId());
        } catch (CalypsoServiceException e) {
            Log.error(this, e.getCause());
        }
        return "Traspaso de valores a la cuenta " + trade.getKeywordValue("CSDCustodianPledge") + " y pignoración a favor del BCE (BackOffice id: " + trade.getLongId() + ")";
    }
}

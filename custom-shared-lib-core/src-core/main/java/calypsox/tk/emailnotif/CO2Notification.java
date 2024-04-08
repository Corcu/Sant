package calypsox.tk.emailnotif;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class CO2Notification extends EmailDataBuilder {


    private static final String TRADE_KEYWORD_MUREX_ROOT_CONTRACT = "MurexRootContract";


    public CO2Notification(BOMessage message) {
        super(message);
        setFileAttached(false);
    }


    @Override
    public String getToAddress(final BOMessage message) {
        return message.getSenderAddressCode();
    }


    @Override
    public String getSubject() {
        String murexRootContract = "";
        Long tradeId = message.getTradeLongId();
        Trade trade = null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
            if(trade!=null){
                murexRootContract = trade.getKeywordValue(TRADE_KEYWORD_MUREX_ROOT_CONTRACT);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve Trade with id: '" + tradeId + "'. " + e.toString());
        }
        return "Venta derechos CO2 - " + murexRootContract;
    }


    @Override
    public String getBody() {
        return null;
    }


}

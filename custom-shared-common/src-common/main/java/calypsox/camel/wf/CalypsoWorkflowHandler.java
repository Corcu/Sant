package calypsox.camel.wf;

import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.util.Hashtable;

/**
 * @author aalonsop
 */
public class CalypsoWorkflowHandler {

    private static final int RETRY_ATTEMPTS=3;

    public static void addKeywordsAndSaveTrade(Hashtable<String,String> newKwds, Trade trade, Action action){
        boolean successfulSave=false;
        Trade lastVersionTrade=trade;
        int i=0;
        while (!successfulSave&&i<RETRY_ATTEMPTS){
            trade.setAction(action);
            setKeywordMap(newKwds,lastVersionTrade);
            try {
                DSConnection.getDefault().getRemoteTrade().save(trade);
                successfulSave=true;
            } catch (CalypsoServiceException exc) {
                lastVersionTrade=handleTradeMismatchException(exc,lastVersionTrade);
            }
            i++;
        }
    }

    private static Trade handleTradeMismatchException(CalypsoServiceException exc, Trade tradeToBeUpdated) {
        Trade lastVersionTrade=tradeToBeUpdated;
        if (exc.getCause() instanceof PersistenceException) {
            if (exc.getCause().getCause() instanceof ObjectVersionMismatchedException) {
                try {
                    Log.debug(CalypsoWorkflowHandler.class.getSimpleName(), "Version mismatch while saving trade, refreshing object..." + lastVersionTrade.getLongId());
                    lastVersionTrade=DSConnection.getDefault().getRemoteTrade().getTrade(tradeToBeUpdated.getLongId());
                } catch (CalypsoServiceException exc1) {
                    Log.error(CalypsoWorkflowHandler.class.getSimpleName(), exc1);
                }
            }
        }
        return lastVersionTrade;
    }
    private static void setKeywordMap(Hashtable<String,String> newKwds, Trade trade){
        Hashtable currentKwds=trade.getKeywords();
        currentKwds.putAll(newKwds);
        trade.setKeywords(currentKwds);
    }
}

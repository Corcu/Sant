package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import java.util.Vector;

/**
 * @author acd
 */
public class CreProductTypesEventFilter implements EventFilter {
    private static final String ACCOUNTING_PRODUCT_TYPES = "ACCOUNTING_PRODUCT_TYPES";

    @Override
    public boolean accept(PSEvent event) {
        boolean res = true;
        if (event instanceof PSEventTrade) {
            res = acceptPSEvent(((PSEventTrade) event).getTrade());
        }else if(event instanceof PSEventTransfer){
            res = acceptPSEvent(((PSEventTransfer) event).getTrade());
        }
        return res;
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptPSEvent(Trade trade) {
        Vector<String> productTypes = getProdutTypes();
        if(trade == null){
            return true;
        }
        return !Util.isEmpty(productTypes) && productTypes.contains(trade.getProductType());
    }

    /**
     * @return List of product types
     */
    private Vector<String> getProdutTypes(){
        return LocalCache.getDomainValues(DSConnection.getDefault(), ACCOUNTING_PRODUCT_TYPES);
    }
}

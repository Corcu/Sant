package calypsox.tk.event;

import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 */
public class CreProductTypesEnginesEventFilter implements EventFilter {
    private static final String PRODUCT_TYPE_FILTER = "CreProductTypeFilter";

    @Override
    public boolean accept(PSEvent event) {
        String engineName = event.getEngineName();

        boolean res = true;
        if (event instanceof PSEventTrade) {
            res = acceptPSEvent(((PSEventTrade) event).getTrade(), engineName);
        }else if(event instanceof PSEventTransfer){
            Trade trade = ((PSEventTransfer) event).getTrade();
            if(null!=trade){
                res = acceptPSEvent(((PSEventTransfer) event).getTrade(),engineName);
            }else {
                int productId = ((PSEventTransfer) event).getBoTransfer().getProductId();
                res = acceptPSEvent(getProductTypeById(productId),engineName);
            }
        }
        return res;
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptPSEvent(Trade trade,String engineName) {
        Vector<String> productTypes = getProductTypes(engineName);
        if(trade == null){
            return true;
        }
        return !Util.isEmpty(productTypes) && productTypes.contains(trade.getProductType());
    }

    /**
     * @param productType
     * @return
     */
    private boolean acceptPSEvent(String productType,String engineName) {
        Vector<String> productTypes = getProductTypes(engineName);
        if(Util.isEmpty(productType)){
            return true;
        }
        return !Util.isEmpty(productTypes) && productTypes.contains(productType);
    }

    /**
     * @return List of product types
     */
    private Vector<String> getProductTypes(String engineName){
        return LocalCache.getDomainValues(DSConnection.getDefault(), engineName+"."+PRODUCT_TYPE_FILTER);
    }

    private String getProductTypeById(int productId){
        try {
            Product product = DSConnection.getDefault().getRemoteProduct().getProduct(productId);
            return Optional.ofNullable(product).map(Product::getType).orElse("");
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading product: " + productId + " error: " + e);
        }
        return "";
    }
}

package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.InterestBearing;
/**
 * @author acd
 */
public class CSTVerifiedEventFilter implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        if(event instanceof PSEventTransfer ){
            final Trade trade = ((PSEventTransfer) event).getTrade();
            if(null!=trade && isInterestBearing(trade) && isCSTEvent()){
                return isCreEventAccepted(trade);
            }
        }
        return true;
    }

    private boolean isCSTEvent(){
        return true;
    }

    private boolean isInterestBearing(Trade trade){
        return trade.getProduct() instanceof InterestBearing;
    }

    private boolean isCreEventAccepted(Trade trade){
        return !Util.isEmpty(trade.getKeywordValue("INTEREST_TRANSFER_TO"));
    }
}

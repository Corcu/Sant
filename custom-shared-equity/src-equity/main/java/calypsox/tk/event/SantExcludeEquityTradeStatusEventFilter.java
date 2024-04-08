package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.product.Equity;


public class SantExcludeEquityTradeStatusEventFilter implements EventFilter {


    @Override
    public boolean accept(PSEvent event) {

        if (event instanceof PSEventTrade) {
            Trade trade = ((PSEventTrade) event).getTrade();
            if ((trade.getProduct() instanceof Equity) && (isPendingBackDatedStatusOrPendingSdi(trade))) {
                return false;
            }
        }
        return true;
    }


    private boolean isPendingBackDatedStatusOrPendingSdi(Trade trade) {
        if ("PENDING_BACKDATE".equalsIgnoreCase(trade.getStatus().getStatus()) ||
                "PENDING_SDI".equalsIgnoreCase(trade.getStatus().getStatus())) {
            return true;
        }
        return false;
    }


}

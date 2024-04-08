package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;

public class SDIChangeEventFilter implements EventFilter {
    @Override
    public boolean accept(PSEvent event) {
        return !(event instanceof PSEventDomainChange) || ((PSEventDomainChange) event).getType() == PSEventDomainChange.SETTLE_DELIVERY;
    }
}

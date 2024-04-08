package calypsox.tk.event;

import com.calypso.tk.event.PSEventDomainChange;

/**
 * @author aalonsop
 */
public class RateIndexExportEventFilter extends DomainChangeExportEventFilter {

    public RateIndexExportEventFilter() {
        //Needed by Calypso
    }

    @Override
    boolean acceptDomainChangeEvent(PSEventDomainChange domainChangeEvent) {
        return PSEventDomainChange.RATE_INDEX==domainChangeEvent.getType();
    }


}

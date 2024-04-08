package calypsox.tk.event;

import calypsox.tk.log.IntegrationLogUtil;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;

/**
 * @author aalonsop
 */
public abstract class DomainChangeExportEventFilter implements EventFilter {

    protected DomainChangeExportEventFilter() {
        //Needed by Calypso
    }

    @Override
    public boolean accept(PSEvent event) {
        boolean isAccepted = true;
        if (event instanceof PSEventDomainChange) {
            isAccepted = acceptDomainChangeEvent((PSEventDomainChange) event);
        }
        new IntegrationLogUtil().logEventFilterProcessing(isAccepted,event,this.getClass().getSimpleName());

        return isAccepted;
    }

    abstract boolean acceptDomainChangeEvent(PSEventDomainChange domainChangeEvent);
}

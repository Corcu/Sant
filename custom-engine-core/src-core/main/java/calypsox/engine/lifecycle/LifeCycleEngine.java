package calypsox.engine.lifecycle;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.service.DSConnection;

public class LifeCycleEngine extends com.calypso.engine.lifecycle.LifeCycleEngine {
    public LifeCycleEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    public void processDomainChange(PSEventDomainChange dc) {
        super.processDomainChange(dc);
        if (getPersistentClasses().contains(dc.getClass().getSimpleName()) && dc.getLongId() > 0) {
            try {

                if (process(dc))
                    countProcessedEvent();
                else
                    countBadEvent(dc);

            } catch (Throwable t) {
                this.countBadEvent(dc);
                Log.error(this, t);
            }
        }
    }
}

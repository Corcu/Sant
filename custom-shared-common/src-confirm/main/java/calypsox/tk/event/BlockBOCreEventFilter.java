package calypsox.tk.event;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

public class BlockBOCreEventFilter implements EventFilter {
    /**
     * Block BOCre Events by Event Type
     * @param event
     * @return
     */
    @Override
    public boolean accept(PSEvent event) {
        if (event instanceof PSEventCre) {
            Vector<String> boCreEventTypesToBlock = LocalCache.getDomainValues(DSConnection.getDefault(), "CreEventTypesToBlock");
            BOCre boCre = ((PSEventCre) event).getBOCre();
            String eventType = Optional.ofNullable(boCre).map(BOCre::getEventType).orElse("");
            if(!Util.isEmpty(boCreEventTypesToBlock)){
                for(String eve : boCreEventTypesToBlock){
                    if(eve.equalsIgnoreCase(eventType)){
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

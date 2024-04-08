package calypsox.tk.event;

import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;

import java.util.Vector;

/**
 * @author dmenendd
 */
public class CreEventTypesEnginesEventFilter implements EventFilter {
    private static final String EVENT_TYPE_FILTER = "CreEventTypeFilter";

    @Override
    public boolean accept(PSEvent event) {
        String engineName = event.getEngineName();
        boolean res = true;
        CreArray creArray = new CreArray();

        try {
            creArray = DSConnection.getDefault().getRemoteBackOffice().getBOTransferCres(((PSEventTransfer) event).getBoTransfer().getLongId());
        } catch (CalypsoServiceException e) {
            e.printStackTrace();
        }

        if(event instanceof PSEventTransfer){
            if(null!=creArray){
                res = acceptPSEvent(creArray, event, engineName);
            }
        }
        return res;
    }

    /**
     * @param creArray
     * @return
     */
    private boolean acceptPSEvent(CreArray creArray, PSEvent event, String engineName) {
        Vector<String> eventTypes = getEventTypes(engineName);
        if(creArray == null){
            return true;
        }
        for(int i = 0; i < creArray.size() ; i++){
            String eventTypeCre = creArray.get(i).getEventType() + creArray.get(i).getCreType();
            if (((PSEventTransfer)event).getStatus().equals("CANCELED") && ((PSEventTransfer)event).getOldStatus().equals("SETTLED")) {
                return !Util.isEmpty(eventTypes) && eventTypes.contains(eventTypeCre);
            }
        }
        return true;
    }

    /**
     * @return List of Event types
     */
    private Vector<String> getEventTypes(String engineName){
        return LocalCache.getDomainValues(DSConnection.getDefault(), engineName+"."+ EVENT_TYPE_FILTER);
    }

}

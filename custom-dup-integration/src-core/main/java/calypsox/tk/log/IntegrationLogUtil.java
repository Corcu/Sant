package calypsox.tk.log;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;

/**
 * @author aalonsop
 */
public class IntegrationLogUtil {

    public void logEventFilterProcessing(boolean isAccepted, PSEvent event, String eventFilterName){
        if(event!=null) {
            String exporterStr="EXPORTER";
            Log.debug(exporterStr, eventFilterName + " finished processing " + event.toString());
            Log.debug(exporterStr, "Event " + event.getEventType() + " isaccepted = " + isAccepted);
        }
    }
}

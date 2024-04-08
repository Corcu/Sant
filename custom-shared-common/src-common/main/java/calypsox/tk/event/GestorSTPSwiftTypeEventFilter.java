package calypsox.tk.event;

import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.Vector;

/**
 *
 * Filtra por Swift MessageType - MT
 *
 * @author acd
 */
public class GestorSTPSwiftTypeEventFilter implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        if(event instanceof PSEventMTSwiftMessage) {
            return acceptMessage((PSEventMTSwiftMessage)event) ;
        }
        return false;
    }

    /**
     * @param event
     * @return
     */
    private boolean acceptMessage(PSEventMTSwiftMessage event){
        return acceptMsgType(event.getEngineName(),event.getMtType());
    }

    /**
     * Filter MSG Types
     * @param engineName
     * @param msgType
     * @return
     */
    private boolean acceptMsgType(String engineName, String msgType){
        boolean accepted = true;
        if(!Util.isEmpty(msgType)){
            Vector<String> typesToAccept = LocalCache.getDomainValues(DSConnection.getDefault(), engineName + "FilterAccept");
            Vector<String> typesToRefuse = LocalCache.getDomainValues(DSConnection.getDefault(), engineName + "FilterRefuse");
            if (!Util.isEmpty(typesToAccept)) {
                accepted = Arrays.stream(typesToAccept.toArray()).map(String.class::cast).anyMatch(msgType::equalsIgnoreCase);
            } else if (!Util.isEmpty(typesToRefuse)) {
                accepted = Arrays.stream(typesToRefuse.toArray()).map(String.class::cast).noneMatch(msgType::equalsIgnoreCase);
            }
        }else {
            return false;
        }

        return accepted;
    }
}

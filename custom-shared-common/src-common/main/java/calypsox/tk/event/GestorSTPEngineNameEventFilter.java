package calypsox.tk.event;

import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;


/**
 * @author acd
 *
 * Acepta unicamente eventos que vengan de un Engine Name específico
 * Se debe crear un DV con el nombre del engine + NameAccept or NameRefuse donde se indican de que engines acepta el evento.
 *
 */
public class GestorSTPEngineNameEventFilter implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        if(event instanceof PSEventMTSwiftMessage){
            return acceptEngineName(event.getEngineName(), (PSEventMTSwiftMessage)event);
        }
        return false;
    }

    /**
     * Filter MSG Types
     * @param
     * @return
     */
    private boolean acceptEngineName(String engineName, PSEventMTSwiftMessage event){
        boolean accepted = true;
        String creatorEngineName = Optional.ofNullable(event).map(PSEventMTSwiftMessage::getCreatorEngineName).orElse("");
        Vector<String> typesToAccept = LocalCache.getDomainValues(DSConnection.getDefault(), engineName + "NameAccept");
        Vector<String> typesToRefuse = LocalCache.getDomainValues(DSConnection.getDefault(), engineName + "NameRefuse");
        if (!Util.isEmpty(typesToAccept)) {
            accepted = Arrays.stream(typesToAccept.toArray()).map(String.class::cast).anyMatch(creatorEngineName::equalsIgnoreCase);
        } else if (!Util.isEmpty(typesToRefuse)) {
            accepted = Arrays.stream(typesToRefuse.toArray()).map(String.class::cast).noneMatch(creatorEngineName::equalsIgnoreCase);
        }
        return accepted;
    }

}

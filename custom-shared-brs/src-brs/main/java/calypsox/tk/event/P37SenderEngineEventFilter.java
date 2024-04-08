package calypsox.tk.event;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

import java.util.Optional;

public class P37SenderEngineEventFilter extends SenderEngineEventFilter {
    public static final String GATEWAY = "P37";

    public P37SenderEngineEventFilter() {
    }

    @Override
    public boolean accept(PSEvent psevent) {
        if (psevent instanceof PSEventMessage) {
            PSEventMessage pseventmessage = (PSEventMessage) psevent;
            BOMessage boMessage = pseventmessage.getBoMessage();

            if (isP37Message(boMessage)) {
                return super.accept(psevent);
            }
        }
        return false;
    }

    public boolean isP37Message(BOMessage boMessage) {
        return Optional.ofNullable(boMessage).map(BOMessage::getGateway).filter(GATEWAY::equalsIgnoreCase).isPresent();
    }

}

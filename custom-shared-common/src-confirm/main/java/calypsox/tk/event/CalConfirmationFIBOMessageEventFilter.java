package calypsox.tk.event;

import calypsox.tk.confirmation.model.CalConfirmMsgTypes;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalConfirmationFIBOMessageEventFilter implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        boolean res = false;
        if (event instanceof PSEventMessage) {
            PSEventMessage eventMessage = (PSEventMessage) event;
            res = acceptMessageType(eventMessage) && acceptEventType(eventMessage);
        }
        return res;
    }

    /**
     * @param event
     * @return true if is a CalypsoConfirmation boMessage type
     * @see CalConfirmMsgTypes
     */
     boolean acceptMessageType(PSEventMessage event) {
        return Optional.ofNullable(event.getBoMessage())
                .map(boMess -> Arrays.stream(CalConfirmMsgTypes.values())
                        .anyMatch(msgTypeEnum-> msgTypeEnum.toString().equals(boMess.getMessageType())))
                .orElse(false);
    }

    /**
     * @param event
     * @return true if is a TO_BE_SENT msg event
     */
    private boolean acceptEventType(PSEventMessage event) {
        return Optional.ofNullable(event.getEventType())
                .map(eventType -> eventType.contains("TO_BE_SENT"))
                .orElse(false);
    }

}

package calypsox.tk.confirmation.handler;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class BondCalConfirmationEventHandler extends CalypsoConfirmationEventHandler {

    public BondCalConfirmationEventHandler(BOMessage boMessage) {
        super(boMessage);
    }

    @Override
    void setEventType(BOMessage boMessage) {
        this.eventType = Optional.ofNullable(boMessage)
                .filter(this::isNotCancelMsg)
                .map(BOMessage::getLinkedLongId)
                .filter(id -> id > 0L)
                .map(id -> EventType.MODIFICATION)
                .orElse(EventType.REGISTRY);
    }

    boolean isNotCancelMsg(BOMessage boMessage) {
        return !isCancelMsg(boMessage);
    }

    @Override
    boolean isCancelMsg(BOMessage boMessage) {
        boolean isCancelSubtype = Optional.ofNullable(boMessage).map(BOMessage::getSubAction)
                .map(act -> act.equals(Action.CANCEL)).orElse(false);
        return super.isCancelMsg(boMessage) || isCancelSubtype;
    }

}

package calypsox.tk.confirmation.builder;

import calypsox.tk.confirmation.handler.CalypsoConfirmationEventHandler;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CalConfirmationEventDataBuilder extends CalypsoConfirmationConcreteBuilder{

    private static final String STP_IND_DV="CalypsoConfirmSTPIndicator";
    //private static final String INSTR_SUB_TYPE = "Standard";
    protected CalypsoConfirmationEventHandler confirmationEventInfo;

    public CalConfirmationEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.confirmationEventInfo=getEventTypeHandler(boMessage);
    }

    protected CalypsoConfirmationEventHandler getEventTypeHandler(BOMessage boMessage){
        return new CalypsoConfirmationEventHandler(boMessage);
    }

    public String buildAction() {
        return confirmationEventInfo.getEventAction();
    }

    public String buildOperId() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("MurexRootContract")).orElse("");
    }

    public String buildStructInd() {
        return String.valueOf(0);
    }

    public String buildEventId() {
        return Optional.ofNullable(boMessage)
                .map(BOMessage::getLongId).map(String::valueOf).orElse("");
    }

    public String buildEventType() {
        return confirmationEventInfo.getEventType();
    }

    public String buildEventDate() {
        return Optional.ofNullable(boMessage)
                .map(BOMessage::getCreationDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }

    public String buildSTPInd() {
        Vector<String> dvs= LocalCache.getDomainValues(DSConnection.getDefault(),STP_IND_DV);
        String stpIndicator=String.valueOf(0);
        if(!Util.isEmpty(dvs)){
            stpIndicator=dvs.get(0);
        }
        return stpIndicator;
    }

    public String buildWaitConfirm() {
        return String.valueOf(0);
    }

    public String buildParentEventId() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("ParentEventId")).orElse("");
    }

    public String buildManualChasing() {
        return String.valueOf(0);
    }

    public String buildMustBeSigned() {
        return String.valueOf(confirmationEventInfo.mustBeSigned());
    }

    public String buildUTI() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("UTI_REFERENCE")).orElse("");
    }

    public String buildUSI() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("USI_REFERENCE")).orElse("");
    }

    public String buildExternalId() {
        return Optional.ofNullable(trade).map(Trade::getExternalReference).orElse("");
    }

    public String buildInstrumentSubType() {
        return confirmationEventInfo.getInstrumentSubType();
    }
}

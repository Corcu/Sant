package calypsox.tk.event;


import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;


public class PaymentsHubCallbackEventFilter implements EventFilter {


    private static final String PAYMENTHUB_INPUT_MSG_TYPE = "PAYMENTHUB_INPUT";
    private static final String SAVED_STATUS = "SAVED";


    @Override
    public boolean accept(PSEvent paramPSEvent) {
        boolean accept = false;
        if (paramPSEvent instanceof PSEventMessage) {
            final PSEventMessage event = (PSEventMessage) paramPSEvent;
            // Get the message type
            final String msgType = event.getMessageType();
            // Get the message status
            final String msgStatus = event.getMessageStatus().getStatus();
            // Check if MessageType is PAYMENTHUB_INPUT and Status SAVED
            if (!Util.isEmpty(msgType) && !Util.isEmpty(msgStatus) && PAYMENTHUB_INPUT_MSG_TYPE.equals(msgType) && SAVED_STATUS.equals(msgStatus)) {
                accept = true;
            }
        } else if (paramPSEvent instanceof PSEventPaymentsHubImport) {
            accept = true;
        }
        return accept;
    }


}

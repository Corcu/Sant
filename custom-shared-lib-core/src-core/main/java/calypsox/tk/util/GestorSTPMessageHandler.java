package calypsox.tk.util;

import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;

/**
 * The Class GestorSTPMessageHandler.
 */
public class GestorSTPMessageHandler extends SwiftMessageHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * calypsox.tk.util.SwiftMessageHandler#getAction(com.calypso.tk.bo.swift
     * .SwiftMessage)
     */
    @Override
    protected Action getAction(final SwiftMessage externalMessage) {
        Action result;
        if (externalMessage.isAcked()) {
            result = Action.ACK;
        } else {
            result = Action.NACK;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * calypsox.tk.util.SwiftMessageHandler#getDescription(com.calypso.tk.bo
     * .swift.SwiftMessage)
     */
    @Override
    protected String getDescription(final SwiftMessage externalMessage) {
        String result = null;
        if (!externalMessage.isAcked()) {
            result = "Message status is NACK. Reason code:"
                    + externalMessage.getNackReasonCode();
        }
        return result;
    }

}

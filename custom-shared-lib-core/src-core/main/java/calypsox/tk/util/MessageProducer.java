package calypsox.tk.util;

import com.calypso.tk.bo.BOMessage;

/**
 * The Interface MessageProducer.
 */
public interface MessageProducer {

    /**
     * Serialize message.
     * 
     * @param message
     *            the Calypso {@link BOMessage} to be serialized
     * @param acknowledgement
     *            the String represantation for the acnowledgement
     * @param originalMessage
     *            The message that was received by the Engine
     * @return The string representation of the {@link BOMessage}
     */
    String serializeMessage(BOMessage message, String acknowledgement,
            String originalMessage);

}

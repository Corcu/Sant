package calypsox.tk.util;

import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.util.ProcessStatusException;

/**
 * The Class SwiftMessageHandler.
 */
public abstract class SwiftMessageHandler implements MessageHandler {

    /**
     * Instantiates a new swift message handler.
     */
    public SwiftMessageHandler() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.MessageHandler#getParsedMessage(java.lang.String)
     */
    @Override
    public BOMessage getParsedMessage(final String serializedMessage)
            throws ProcessStatusException {
        final SwiftMessage externalMessage = new SwiftMessage();
        externalMessage.parseSwiftText(serializedMessage, false);
        final int idValue = SantanderUtil.getInstance().getMessageIdFromMur(
                serializedMessage);
        final Action action = getAction(externalMessage);
        final String description = getDescription(externalMessage);
        final BOMessage result = new BOMessage();
        result.setLongId(idValue);
        result.setAction(action);
        result.setDescription(description);
        return result;
    }

    /**
     * Gets the action.
     *
     * @param externalMessage the external message
     * @return the action
     * @throws ProcessStatusException the process status exception
     */
    protected abstract Action getAction(SwiftMessage externalMessage)
            throws ProcessStatusException;

    /**
     * Gets the description.
     *
     * @param externalMessage the external message
     * @return The description of the message action or null if no description
     * should be applied
     */
    protected abstract String getDescription(SwiftMessage externalMessage);
}

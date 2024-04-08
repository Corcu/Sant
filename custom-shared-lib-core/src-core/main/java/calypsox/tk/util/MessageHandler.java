package calypsox.tk.util;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.util.ProcessStatusException;

/**
 * The Interface MessageHandler.
 */
public interface MessageHandler {

    /**
     * Gets the parsed message.
     * 
     * @param serializedMessage
     *            a String representing the {@link BOMessage} in a format
     *            compatible with this handler
     * @return A calypso {@link BOMessage} from a foreign system
     * @throws ProcessStatusException
     *             the process status exception
     */
    BOMessage getParsedMessage(String serializedMessage)
            throws ProcessStatusException;
}

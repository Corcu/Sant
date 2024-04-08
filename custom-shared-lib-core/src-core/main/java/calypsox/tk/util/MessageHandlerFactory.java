package calypsox.tk.util;

import java.util.HashMap;

import com.calypso.tk.core.Log;

/**
 * A factory for creating MessageHandler objects.
 */
public class MessageHandlerFactory {

    /** The Constant SWIFTACCORD_IMPORT_CONSTANT. */
    public static final String SWIFTACCORD_IMPORT_CONSTANT = "swiftaccord";

    /** The Constant PAYPLUS_IMPORT_CONSTANT. */
    public static final String PAYPLUS_IMPORT_CONSTANT = "payplus.in";

    /** The Constant GESTOR_STP_IMPORT_CONSTANT. */
    public static final String GESTOR_STP_IMPORT_CONSTANT = "gstp.in";

    private static HashMap<String, MessageHandler> messageHandlerMap;

    static {
        messageHandlerMap = new HashMap<String, MessageHandler>();

        messageHandlerMap.put(PAYPLUS_IMPORT_CONSTANT,
                new PayplusMessageHandler());
        messageHandlerMap.put(GESTOR_STP_IMPORT_CONSTANT,
                new GestorSTPMessageHandler());
        messageHandlerMap.put(SWIFTACCORD_IMPORT_CONSTANT,
                new SwiftAcordMessageHandler());
    }

    /**
     * Gets the message handler.
     * 
     * @param adapterType
     *            the adapter type
     * @return the message handler
     */
    public static MessageHandler getMessageHandler(final String adapterType) {
        final MessageHandler result = messageHandlerMap.get(adapterType);
        if (result == null) {
            Log.error("calypsox.tk.util.MessageHandlerFactory",
                    "MessageHandlerFactory::getMessageHandler::handler is null for adapterType ='"
                            + adapterType + "'");
        }
        return result;
    }
}

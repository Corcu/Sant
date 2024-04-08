package calypsox.engine.importer;

import calypsox.tk.core.retried.RetriedAction.RetriedActionException;
import calypsox.tk.core.retried.actions.AckMessageRetriedAction;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.util.MessageHandler;
import calypsox.tk.util.MessageProducer;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.ProcessStatusException;

import java.util.concurrent.Callable;

/**
 * The Class JMSMessageImporterCalleableInstance.
 */
public class JMSMessageImporterCalleableInstance implements Callable<Exception> {

    private ExternalMessage externalMessage;

    /**
     * The ds connection.
     */
    protected DSConnection dsConnection;

    /**
     * The engine name.
     */
    protected String engineName;
    private String adapterType;
    private IEAdapter adapter;
    private BOMessage parsedMessage;
    private MessageHandler messageHandler;

    /**
     * Instantiates a new jMS message importer calleable instance.
     *
     * @param externalMessage the external message
     * @param dsConnection    the ds connection
     * @param engineName      the engine name
     * @param adapter         the adapter
     * @param adapterType     the adapter type
     * @param parsedMessage   the parsed message
     * @param messageHandler  the message handler
     */
    public JMSMessageImporterCalleableInstance(
            final ExternalMessage externalMessage,
            final DSConnection dsConnection, final String engineName,
            final IEAdapter adapter, final String adapterType,
            final BOMessage parsedMessage, final MessageHandler messageHandler) {

        this.externalMessage = externalMessage;

        this.dsConnection = dsConnection;
        this.engineName = engineName;
        this.adapter = adapter;
        this.adapterType = adapterType;
        this.parsedMessage = parsedMessage;
        this.messageHandler = messageHandler;

    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public final Exception call() {
        final String originalMessage = this.externalMessage.getText();
        boolean result = false;

        if (this.parsedMessage.getAction() == null) {
            return new ProcessStatusException(
                    "No valid action was returned by the JMS queue");
        }
        BOMessage message = null;
        String acknowledgement = Action.S_NACK;

        final AckMessageRetriedAction ackMessage = new AckMessageRetriedAction(
                this.parsedMessage.getLongId(), this.parsedMessage.getAction(),
                this.parsedMessage.getAttributes(),
                this.parsedMessage.getDescription(), this.adapterType);
        try {
            ackMessage.execute(10, 5000);

            message = ackMessage.getMessage();
            if (message != null) {
                acknowledgement = Action.S_ACK;

                if (this.parsedMessage.getAction().equals(Action.NACK)) {
                    final SantExceptionType exType = getNackException(message
                            .getMessageType());

                    ImporterUtil.getInstance().publishJMSImportExceptionTask(
                            exType, message, message.getDescription(),
                            this.engineName);
                }
                result = true;
                if (this.messageHandler instanceof MessageProducer) {

                    final String serializedMessage = ((MessageProducer) this.messageHandler)
                            .serializeMessage(message, acknowledgement,
                                    originalMessage);
                    if (serializedMessage.length() > 0) {
                        result = writeResponse(serializedMessage);
                    } else {
                        result = true;
                    }
                }
            } else {
                final String logMessage = "Tried to apply an Action from "
                        + this.adapterType + " to the Message:"
                        + this.parsedMessage.getLongId()
                        + " that is not present in Calypso";
                Log.error(this,
                        "ImportJMSMessageEngine::handleIncomingMessage::Message of ID"
                                + logMessage);
                ImporterUtil.getInstance().publishJMSImportExceptionTask(
                        SantExceptionType.TECHNICAL_EXCEPTION,
                        this.parsedMessage, logMessage, this.engineName);

            }
        } catch (final RetriedActionException returnedException) {
            // something wrong happens when saving the message
            Log.error(this, "ImportJMSMessageEngine::handleIncomingMessage="
                    + returnedException, returnedException);
            ImporterUtil.getInstance().publishJMSImportExceptionTask(
                    SantExceptionType.TECHNICAL_EXCEPTION, this.parsedMessage,
                    returnedException.getMessage(), this.engineName);
        }
        freeMemory();
        if (!result) {
            return new Exception("Import Failure");
        } else {
            return null;
        }
    }

    private SantExceptionType getNackException(final String messageType) {
        SantExceptionType exType;
        try {
            exType = SantExceptionType.valueOf("EX_NACK_" + messageType);
        } catch (final java.lang.IllegalArgumentException ex) {
            exType = SantExceptionType.NACK;
        }
        return exType;
    }

    private void freeMemory() {
        this.externalMessage = null;
        this.dsConnection = null;
        this.engineName = null;
        this.adapter = null;
        this.adapterType = null;
        this.parsedMessage = null;
        this.messageHandler = null;

    }

    /**
     * Write response.
     *
     * @param response the response
     * @return true, if successful
     */
    public boolean writeResponse(final String response) {
        final boolean result = this.adapter.write(response);
        Log.debug(this, "ImportJMSMessageEngine::writeResponse::result= "
                + result + " & message=" + response);
        return result;
    }
}

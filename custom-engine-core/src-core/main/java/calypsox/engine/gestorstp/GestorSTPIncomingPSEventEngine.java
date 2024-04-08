package calypsox.engine.gestorstp;

import calypsox.engine.BaseIEEngine;
import calypsox.tk.event.PSEventMTSwiftMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.MessageParseException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author acd
 */
public class GestorSTPIncomingPSEventEngine extends BaseIEEngine {

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public GestorSTPIncomingPSEventEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * New message reception. Starts de Incoming handling message method
     *
     * @param adapter of the queue
     * @param message reception
     * @return if a new message has arrived
     */
    @Override
    public boolean newMessage(final IEAdapter adapter, final ExternalMessage message) {
        if (message == null) {
            return false;
        }
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }
        return handleIncomingMessage(message);
    }

    /**
     * Calls core to process the SWIFT messages received
     * the external message received and passed by the newMessage Method
     * @param externalMessage
     * @return if the incoming message has been handle
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {

        Log.info(this.getClass().getSimpleName(), "Received message:" + externalMessage.getText() + "\n");

        if (Util.isEmpty(externalMessage.getText())) {
            Log.info(this.getClass().getSimpleName(), "Message receive is null or empty");
            return false;
        }

        processExternalMessage(externalMessage);

        return true;
    }

    /**
     * @param externalMessage
     */
    private void processExternalMessage(ExternalMessage externalMessage) {
        try {
            generateAndPublishPSEvent(externalMessage);
        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName(), "Error process message: " + e.getCause());
        }
    }

    /**
     * Generate PSEventSwiftMessage
     *
     * @param externalMessage
     */
    private void generateAndPublishPSEvent(ExternalMessage externalMessage){
        try {
            final String messageType = getMessageType(externalMessage);
            //ValidateMessageTypes
            if(!Util.isEmpty(messageType)){
                PSEventMTSwiftMessage eventMessage = new PSEventMTSwiftMessage();
                eventMessage.setMtType(messageType);
                eventMessage.setMtSwiftMessage(externalMessage.getText());
                eventMessage.setCreatorEngineName(getEngineName());

                long eventId = DSConnection.getDefault().getRemoteTrade().saveAndPublish(eventMessage);
                Log.info(this.getClass().getSimpleName(),"PSEventMTSwiftEvent generated: " + eventId);
            }else {
                Log.error(this.getClass().getSimpleName(),"No Swift Message: " + externalMessage.getText());
            }
        } catch (Exception err) {
            Log.error(this.getClass().getSimpleName(),"Error publish event: " + err.getCause());
        }
    }

    /**
     * Get Swift MT Message
     *
     * @param externalMessage
     * @return
     */
    private String getMessageType(ExternalMessage externalMessage){
        try{
            final String swift = externalMessage.getText();
            String block2 = null;
            Pattern block2Pattern = Pattern.compile("\\{2:[^\\}]+\\}");
            Matcher block2Matcher = block2Pattern.matcher(swift);
            if (block2Matcher.find()) {
                block2 = swift.substring(block2Matcher.start() + 1, block2Matcher.end() - 1);
            }
            if (block2 == null) {
               Log.warn(this.getClass().getSimpleName(),"Block 2 is missing");
                return "Empty";
            }
            if (block2 != null) {
                return "MT" + block2.substring(3, 6);
            }
        }catch (Exception e){
            Log.error(this.getClass().getSimpleName(),"Error parsing swift message type: " + e.getCause());
        }
        return "";
    }

    /**
     * Parse and Generate Swift Message
     *
     * @param parser
     * @param externalMessage
     * @return
     */
    private ExternalMessage generateSwiftMessage(ExternalMessageParser parser, final ExternalMessage externalMessage) {
        String incomingMessageText = externalMessage.getText();
        incomingMessageText = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessageText);
        try {
            return parser.readExternal(incomingMessageText, "");
        } catch (MessageParseException e) {
            Log.error(this.getClass().getSimpleName(), e + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
        return false;
    }

    @Override
    public String handleOutgoingMessage(PSEvent event, List<Task> tasks) throws Exception {
        return null;
    }

}

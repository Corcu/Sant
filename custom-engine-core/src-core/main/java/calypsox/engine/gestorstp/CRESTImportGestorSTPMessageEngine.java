package calypsox.engine.gestorstp;


import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.ArrayList;

/**
 *
 * @author xIS16412
 *
 */
public class CRESTImportGestorSTPMessageEngine extends ImportGestorSTPMessageEngine {


    public static final String ENGINE_NAME_CREST = "SANT_CREST_ImportGestorSTPMessageEngine";

    private static ExternalMessageParser parser;

    private static final String TIME_OUT = "TIME_OUT";

    private static final String ERROR_TRADE_SWIFT = "ERROR_TRADE_SWIFT";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public CRESTImportGestorSTPMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Name of the engine that offers this service
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME_CREST;
    }

    @Override
    public boolean handleIncomingMessage(ExternalMessage externalMessage) {
        if(externalMessage instanceof JMSQueueMessage){
            JMSQueueMessage message = (JMSQueueMessage) externalMessage;
            Action action = getAction(message.getText());
            Log.info(this, "The message received from CREST is "+ message.getText());
            Log.info(this, "The correlation id is "+ message.getCorrelationId());
            BOMessage bomessage = null;
            try {
                bomessage = getMessageFromCorrelationId(message.getCorrelationId());
                if(bomessage!=null) {
                    if (action != Action.ACK) {
                        publishExceptionTask(getEngineName(), bomessage, getDescription(message.getText()), ERROR_TRADE_SWIFT);
                    }
                    bomessage.setAction(action);
                    bomessage = (BOMessage) bomessage.clone();
                    getDS().getRemoteBackOffice().save(bomessage, 0, getEngineName());
                }
            } catch (Exception e) {
                Log.error(this, "Error processing message with id=" + bomessage.getLongId(), e);
                throw new RuntimeException(e);
            }

        }
        return true;
    }

    protected String getDescription(final String externalMessage) {
        String result = null;
        if (getAction(externalMessage).equals(Action.NACK)) {
            int posNACK = externalMessage.indexOf(":procResultText:");
            String reasonNACK = externalMessage.substring(posNACK + 16, externalMessage.length());
            result = "Message status is NACK. Reason code:" + reasonNACK;
        }
        return result;
    }

    /**
     * @param msgString
     * @return
     */

    protected Action getAction(final String msgString) {
        Action result;
        int actionPos = msgString.indexOf(":procResultCode:");
        String msgAction = msgString.substring(actionPos + 16, actionPos + 19);
        if (msgAction.equals("ACK")) {
            result = Action.ACK;
        } else {
            result = Action.NACK;
        }
        return result;
    }

    private void publishExceptionTask(String configName, BOMessage message, String comment, String eventType) {
        Task task = buildTask(message != null ? message.getTradeLongId() : 0L, comment,
                message != null ? message.getLongId() : 0L, eventType, Task.MESSAGE_EVENT_CLASS);
        ArrayList<Task> taskList = new ArrayList<>();
        taskList.add(task);
        publishTask(taskList);
    }

    private BOMessage getMessageFromCorrelationId(String id){
        MessageArray messages=null;
        try {
            messages = DSConnection.getDefault().getRemoteBackOffice()
                    .getMessages("mess_attributes","mess_attributes.message_id = bo_message.message_id AND attr_name='JMSMessageID' AND attr_value ='" + id + "'",null,null);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error when try to obtain message with JMSMessageId value=" + id, e);
            return null;
        }
        if(!messages.isEmpty()){
            for(BOMessage message:messages){
                if(message.getStatus().toString().equals("SENT")){
                    return message;
                }
            }
        }
        return null;
    }

}

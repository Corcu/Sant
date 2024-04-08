package calypsox.engine.gestorstp;

import calypsox.engine.advice.ImportMessageEngine;
import calypsox.tk.util.ExtendedIEAdapterListener;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.ProcessStatusException;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xIS16412
 */
public class ImportGestorSTPMessageEngine extends ImportMessageEngine implements ExtendedIEAdapterListener {

    private static final String TIME_OUT = "TIME_OUT";
    private static final String ENGINE_NAME = "SANT_ImportGestorSTPMessageEngine";
    private static final String ERROR_TRADE_SWIFT = "ERROR_TRADE_SWIFT";
    private IEAdapter adapter;

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public ImportGestorSTPMessageEngine(DSConnection dsCon, String hostName, int port) {

        super(dsCon, hostName, port);
    }

    /*
     *
     *
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {

        super.init(engineContext);

        Log.info(this.getClass(), "Custom Log: Initializing ImportGestorSTPMessageEngine Engine...");
        // Initialize the adapter type in the Default. Used by so IEAdapter
        /*Properties properties = Defaults.getProperties();
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(SantanderIEAdapterConfig.ADAPTER_TYPE, type);
        Defaults.setProperties(properties);*/
        if (Util.isEmpty(this.getIeAdapterConfigName())) {
            throw new InvalidParameterException("Empty config name: cannot read Engine properties.");
        }

        setEngineName(ENGINE_NAME);
    }

    @Override
    public boolean newMessage(IEAdapter adapter, ExternalMessage message) {

        this.adapter = adapter.getIEAdapterConfig().getReceiverIEAdapter();
        return handleIncomingMessage(message);
    }

    /**
     *
     */
    @Override
    public boolean handleIncomingMessage(ExternalMessage externalMessage) {

        Log.info(this, "Received message:\n" + externalMessage.getText());

        BOMessage parsedMessage = null;
        try {

            // finds the BO message containing the SWIFT message
            parsedMessage = getParsedMessage(externalMessage.getText());

        } catch (Exception e) {
            Log.error(ImportGestorSTPMessageEngine.class, "Failed to process message: " + e.getMessage(), e);
        }

        BOMessage messageToSave = null;

        try {

            // recover the associated BO Message
            messageToSave = getDS().getRemoteBackOffice().getMessage(parsedMessage.getLongId());

            if (messageToSave == null) {
                Log.error(this, "Cannot find message with id=" + parsedMessage.getLongId());
                return true;
            }

            // update with NACK or ACK reponse
            messageToSave.setAction(parsedMessage.getAction());
            messageToSave.setDescription(parsedMessage.getDescription());
            messageToSave.setEnteredUser(getDS().getUser());

            // save in DS
            getDS().getRemoteBackOffice().save(messageToSave, 0, getEngineName());

            if (parsedMessage.getAction().equals(Action.NACK)) {
                // Raise a Task with the error message
                messageToSave.setAction(parsedMessage.getAction());
                publishExceptionTask(getEngineName(), messageToSave, messageToSave.getDescription(), ERROR_TRADE_SWIFT);

            } else if (parsedMessage.getAction().toString().equals(TIME_OUT)) {

                // Raise a Task with the error message
                messageToSave.setAction(parsedMessage.getAction());
                publishExceptionTask(getEngineName(), messageToSave, messageToSave.getDescription(), ERROR_TRADE_SWIFT);
            }

        } catch (RemoteException e) {
            Log.error(this, "Error processing message with id=" + parsedMessage.getLongId(), e);

        }
        return true;
    }

    /**
     * @param serializedMessage
     * @return Finds the BO message containing the SWIFT response, with the
     * apropiate NACK or ACK
     * @throws ProcessStatusException
     */
    // cortesy of FX
    public BOMessage getParsedMessage(final String serializedMessage) throws ProcessStatusException {

        final SwiftMessage externalMessage = new SwiftMessage();
        externalMessage.parseSwiftText(serializedMessage, false);
        final int idValue = getMessageIdFromMur(serializedMessage);
        final Action action = getAction(externalMessage);
        final String description = getDescription(externalMessage);

        final BOMessage result = new BOMessage();
        result.setLongId(idValue);
        result.setAction(action);
        result.setDescription(description);

        return result;
    }

    /*
     * Nack reason. cortesy of FX
     */
    protected String getDescription(final SwiftMessage externalMessage) {
        String result = null;
        if (!externalMessage.isAcked()) {
            result = "Message status is NACK. Reason code:" + externalMessage.getNackReasonCode();
        }
        return result;
    }

    /**
     * @param externalMessage
     * @return
     */
    protected Action getAction(final SwiftMessage externalMessage) {
        Action result;
        if (externalMessage.isAcked()) {
            result = Action.ACK;
        } else {
            result = Action.NACK;
        }
        return result;
    }

    /**
     * Retrieve the Message ID from a received message MUR from GestorSTP. The
     * ID is located in the field tag 108 within the block 3 of Message Header
     * It is the number after the MUR id code GT + Message Version number mapped
     * into two chars code. Example is: {108:GTAA000000010052} GT -> Global
     * Trading; AA -> Version number = 0; 000000010052 -> Message Id;
     *
     * @param serializedMessage the serialized message
     * @return the message id from mur
     */
    public int getMessageIdFromMur(final String serializedMessage) {

        final String block = "{108:";
        int id = -1;
        boolean found = false;
        int pos = serializedMessage.indexOf(block) + block.length();
        if (pos >= 0) {
            while (!found && (pos < serializedMessage.length())) {
                if (Character.isDigit(serializedMessage.charAt(pos))) {
                    found = true;
                } else {
                    pos = pos + 1;
                }
            }
            final int tagEnd = serializedMessage.indexOf('}', pos);
            if (tagEnd >= 0) {
                id = Integer.parseInt(serializedMessage.substring(pos, tagEnd));
            }
        }
        return id;
    }

    private void publishExceptionTask(String configName, BOMessage message, String comment, String eventType) {
        Task task = buildTask(message != null ? message.getTradeLongId() : 0L, comment,
                message != null ? message.getLongId() : 0L, eventType, Task.MESSAGE_EVENT_CLASS);
        ArrayList<Task> taskList = new ArrayList<>();
        taskList.add(task);
        publishTask(taskList);
    }

    protected void publishTask(List<Task> tasks) {
        try {
            getDS().getRemoteBO().saveAndPublishTasks(new TaskArray(tasks), 0, getEngineName());
        } catch (RemoteException e) {
            Log.error(this, "Failed to saveAndPublishTasks: " + tasks, e);
        }
    }

    protected Task buildTask(long tradeId, String comment, long messageId, String eventType, String eventClass) {
        Task task = new Task();
        task.setObjectLongId(messageId);
        task.setTradeLongId(tradeId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(getEngineName());
        task.setComment(comment);
        return task;
    }

    @Override
    protected void poststop(boolean willTerminate) {
        try {
            if (this.adapter != null)
                this.adapter.stop();
            super.poststop(willTerminate);
        } catch (Exception e) {
            Log.error(this, "Exception while closing connection");
        }
    }

    public boolean writeResponse(String response) {

        boolean result = this.adapter.write(response);
        Log.debug(this, "WriteResponse::result= " + result + " & message=" + response);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#setIEAdapter(com.calypso.tk.
     * util.IEAdapter)
     */
    @Override
    public void setIEAdapter(IEAdapter adapter) {
        this.adapter = adapter;

    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.ExtendedIEAdapterListener#getIEAdapter()
     */
    @Override
    public IEAdapter getIEAdapter() {
        return this.adapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#writeMessage(com.calypso.tk.
     * util.IEAdapter, java.lang.String)
     */
    @Override
    public boolean writeMessage(IEAdapter adapter, String message) {
        if (this.adapter == null) {
            this.adapter = adapter;
        }

        boolean ret = false;
        if (Util.isEmpty(message)) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            ret = adapter.getIEAdapterConfig().getSenderIEAdapter().write(message);
        }
        return ret;
    }

    /**
     * Method overridden so this engine can be restarted when it fails to
     * connect.
     */
    public boolean start(boolean batch) throws ConnectException {
        try {
            return super.start(batch);
        } catch (ConnectException e) {
            stop();
            throw e;
        }
    }

}

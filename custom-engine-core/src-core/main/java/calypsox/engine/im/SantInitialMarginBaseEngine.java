package calypsox.engine.im;

import calypsox.engine.BaseIEEngine;
import calypsox.engine.im.export.QEFJMSMessageWrapper;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.JMSQueueIEAdapter;
import calypsox.tk.util.SantanderJMSQueueIEAdapter;
import calypsox.tk.util.TibcoQueueIEAdapter;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.BilateralEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public abstract class SantInitialMarginBaseEngine extends BaseIEEngine implements SantInitialMarginEngineConstants {

    public static final String SENT_TO_QEF = "SENT_TO_QEF";

    @Override
    public abstract String getEngineName();

    /**
     * IE Config name
     */
    protected String configName = null;

    /**
     * Activates testing mode (file watcher simulating queue)
     */
    public static final String TESTING_MODE = "TESTING_MODE";

    public SantInitialMarginBaseEngine(final DSConnection dsCon, final String hostName,
                                       final int esPort) {
        super(dsCon, hostName, esPort);
    }

    /**
     * Start of the engine
     */
    @Override
    public boolean start(final boolean batch) throws ConnectException {
        return super.start(batch);
    }

    /**
     * Engine tells that it consumed the Event
     *
     * @param engine
     * @param id
     */
    protected boolean eventProcessed(String engine, int id) {
        try {
            this._ds.getRemoteTrade().eventProcessed(id, engine);
            return true;
        } catch (Exception e) {
            Log.error(getEngineName(), e);
            return false;
        }
    }

    /**
     * Processing for Incoming External messages :
     * <p>
     * <li>Get The Message from the adapater</li>
     * <li>Parse the Message if necessary</li>
     * <li>Process the received message in Calypso</li>
     * <li>Acknowledge the message</li>
     * <p>
     * called from IEAdapter.callBackListener if if a parser was found
     */
    @Override
    public boolean newMessage(final IEAdapter adapter, final ExternalMessage msg) {
        if (msg == null) {
            return false;
        }
        boolean proc = false;

        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }
        proc = handleIncomingMessage(msg);
        return proc;
    }

    /**
     * Handler incoming message. This is the main method that is called when a new message come from the IEAdapter.
     *
     * @param externalMessage message to handle
     * @return true if everything is find.
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
        JMSQueueAnswer answer = null;
        final List<Task> tasks = new ArrayList<>();

        // Display the message
        Log.info(this, externalMessage.getText());

        // Try to import the object
        try {
            if (externalMessage instanceof JMSQueueMessage) {
                final JMSQueueMessage jmsMessage = (JMSQueueMessage) externalMessage;
                // process the incoming message and then send the ack/nack
                // message
                answer = importMessage(jmsMessage, tasks);
                // set the correlation id on the message to send out
                answer.setCorrelationId(jmsMessage.getCorrelationId());
                answer.setReference(jmsMessage.getReference());

                sendAnswer(answer);
                // publish any prepared task
                if (!Util.isEmpty(tasks)) {
                    publishTask(tasks);
                }
                return true;
            }
        } catch (final Exception e) {
            tasks.add(buildTask(getExceptionMessage(e), answer));
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        if (!Util.isEmpty(tasks)) {
            publishTask(tasks);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.engine.BaseIEEngine#handleIncomingMessage(java.lang.String, java.util.List)
     */
    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
        JMSQueueAnswer answer = null;
        try {

            // process the incoming message and then send the ack/nack message
            answer = importMessage(message, tasks);
            sendAnswer(answer);
            return true;
        } catch (final Exception e) {
            tasks.add(buildTask(getExceptionMessage(e), answer));
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return false;
    }

    protected JMSQueueAnswer importMessage(JMSQueueMessage jmsMessage, List<Task> tasks) throws Exception {
        return null;
    }

    protected abstract JMSQueueAnswer importMessage(String message, List<Task> tasks) throws Exception;

    /**
     * generate a task from an specific message
     *
     * @param exception source of the task
     * @param answer    anwser if possible
     */
    protected Task buildTask(final String message, final JMSQueueAnswer answer) {
        Task task = new Task();
        if (answer != null) {
            task = buildTask(message, 0, answer.getETTEventType(), Task.EXCEPTION_EVENT_CLASS);
            if (answer.getTransactionKey() != null) {
                task.setUserComment(answer.getTransactionKey());
            }
        }

        return task;
    }

    /**
     * We only publish in Task Station if answer is not OK
     *
     * @param answer
     * @return
     */
    public boolean needTask(final JMSQueueAnswer answer) {
        if ((answer != null) && !JMSQueueAnswer.OK.equals(answer.getCode())) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.engine.BaseIEEngine#handleOutgoingMessage(com.calypso.tk.event .PSEvent, java.util.List)
     */
    @Override
    public String handleOutgoingMessage(PSEvent event, List<Task> tasks) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.engine.BaseIEEngine#handleOutgoingMessage(com.calypso.tk.event .PSEvent, java.util.List)
     */
    public List<QEFJMSMessageWrapper> handleOutgoingJMSMessage(PSEvent event, List<Task> tasks) throws Exception {
        return null;
    }

    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          object imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return
     */
    protected JMSQueueAnswer generateAnswer(final Object object, final Exception exception,
                                            final ExternalMessage externalMessage) {
        return new JMSQueueAnswer();
    }

    /**
     * send the answer to the MiddleWare
     *
     * @param answer message to send back
     * @throws Exception if there is a sending problem
     */
    protected void sendAnswer(final JMSQueueAnswer answer) throws Exception {
        if (Log.isDebug()) {
            Log.debug(this, answer.toString());
        }
        Log.info(SantInitialMarginBaseEngine.class.getName(), "Sending answer --------------> " + answer.toString());
        writeMessage(getIEAdapter(), answer);
    }

    /**
     * Process an event
     *
     * @param event incoming event
     * @return true if everything is fine.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean process(PSEvent event) {
        boolean ret = true;
        List<Task> tasks = new ArrayList<>();
        try {
            // call method accept of child engines
            if (!isAcceptedEvent(event)) {
                return true;
            }
            // call outgoingJMSMessage of child engines
            List<QEFJMSMessageWrapper> listMessages = handleOutgoingJMSMessage(event, tasks);
            if (!Util.isEmpty(listMessages)) {
                for (QEFJMSMessageWrapper messageWrapper : listMessages) {
                    if (getIEAdapter() != null) {
                        if (getIEAdapter() instanceof JMSQueueIEAdapter) {
                            Log.info(SantInitialMarginBaseEngine.class.getName(), "Es instancia de JMSQueueIEAdapter --------------> " + messageWrapper.getQefJmsMessage().toString());
                            ret &= writeJMSMessage((JMSQueueIEAdapter) getIEAdapter(), messageWrapper.getQefJmsMessage());
                        } else if (getIEAdapter() instanceof TibcoQueueIEAdapter) {
                            Log.info(SantInitialMarginBaseEngine.class.getName(), "Es instancia de TibcoQueueIEAdapter --------------> " + messageWrapper.getQefJmsMessage().toString());
                            ret &= writeJMSMessageCorrelationId((TibcoQueueIEAdapter) getIEAdapter(), messageWrapper.getQefJmsMessage());
                        } else {
                            Log.info(SantInitialMarginBaseEngine.class.getName(), "No es instancia de TibcoQueueIEAdapter ni JMSQueueIEAdapter --------------> " + messageWrapper.getQefJmsMessage().getText());
                            ret &= writeMessage(getIEAdapter(), messageWrapper.getQefJmsMessage().getText());
                        }
                        //If sucessful sending the facade entry's SENT TO QEF attribute is set to true
                        setSentKeywordTrue(messageWrapper.getMarginCallEntryId(),messageWrapper.isEndOfMessage());
                    }
                }
                if (ret) {
                    getDS().getRemoteTrade().eventProcessed(event.getLongId(), getEngineName());
                }
            }
        } catch (Exception exception) {
            Log.error(this, getEngineName(), exception);
            tasks.add(buildTask(exception));
        }
        publishTask(tasks);
        return ret;
    }

    /**
     * @param psEvent
     * @return
     */
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        return false;
    }

    private boolean writeJMSMessage(JMSQueueIEAdapter adapter, JMSQueueMessage message) {
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }

        boolean ret = false;
        if ((message != null) && Util.isEmpty(message.getText())) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            ret = ((SantanderJMSQueueIEAdapter) (adapter.getIEAdapterConfig().getSenderIEAdapter())).write(message);
        }
        return ret;
    }


    /**
     * Write a message through the adapter
     *
     * @param adapter IEAdapter used to send the message
     * @param message message to write
     * @return true if everything is fine.
     */
    public boolean writeMessage(IEAdapter adapter, JMSQueueMessage message) {
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }

        boolean ret = false;
        if ((message != null) && Util.isEmpty(message.getText())) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            if (getIEAdapter() instanceof TibcoQueueIEAdapter) {
                ret = writeJMSMessageCorrelationId((TibcoQueueIEAdapter) adapter, message);
            } else {
                ret = ((SantanderJMSQueueIEAdapter) adapter.getIEAdapterConfig().getSenderIEAdapter()).write(message);
            }
        }
        return ret;
    }

    private boolean writeJMSMessageCorrelationId(TibcoQueueIEAdapter adapter, JMSQueueMessage message) {
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }

        boolean ret = false;
        if ((message != null) && Util.isEmpty(message.getText())) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            Log.info(SantInitialMarginBaseEngine.class.getName(), "entra en el m?todo writeJMSMessageCorrelationId --------------> " + message.toString());
            ret = ((TibcoQueueIEAdapter) (adapter.getIEAdapterConfig().getSenderIEAdapter()))
                    .writeMessageCorrelationId(message);
        }
        return ret;
    }

    /**
     * @param entryId
     * @param isEndOfMessage
     */
    private void setSentKeywordTrue(int entryId,boolean isEndOfMessage){
        if(!isEndOfMessage){
            setSentKeywordTrue(entryId);
        }
    }
    /**
     * @param entryId ids of all entries to extract
     * @author epalaobe
     * <p>Put the attribute SENT to True value
     */
    private void setSentKeywordTrue(int entryId) {
        MarginCallEntryDTO entryProv = new BilateralEntryDTO();
        try {

            entryProv =
                    ServiceRegistry.getDefault(DSConnection.getDefault())
                            .getCollateralServer()
                            .loadEntry(entryId);

        } catch (CollateralServiceException e) {
            Log.error(this, "Couldn't load MarginCallEntriesDTO: " + e.getMessage());
        }

        entryProv.addAttribute(SENT_TO_QEF, (Boolean) true);

        try {
            entryId =
                    ServiceRegistry.getDefault(DSConnection.getDefault())
                            .getCollateralServer()
                            .save(entryProv, Action.UPDATE.toString(), TimeZone.getDefault());
            Log.info(
                    this,
                    "Entry with id "
                            + entryId
                            + " successfully saved for the contract "
                            + entryProv.getCollateralConfigId());

        } catch (RemoteException e) {
            Log.error(this, e.getMessage());
        }
    }

}

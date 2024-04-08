package calypsox.tk.bo.document;

import calypsox.tk.core.retried.actions.ApplyActionToMessageRetriedAction;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import calypsox.tk.util.SantanderIEAdapterConfig;
import calypsox.util.StripedLock;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * A Document Sender which uses JMS to deliver the documents
 */
public abstract class GatewayMQDocumentSender implements DocumentSender {
    /**
     * time between retries
     */
    private static final int CHECK_RELATED_MSG_SENT_WAITING_TIME = 10000;
    /**
     * number of times while a cancellation message will wait until its related
     * message is sent
     */
    private static final int CHECK_RELATED_MSG_SENT_MAX_RETRIES = 1000;
    private static final int UPD_STATUS_RETRIES = 100;
    private static final int UPD_STATUS_WAITING_TIME = 1000;
    private static final int STRIPED_LOCK_CAPACITY = 6;
    protected static StripedLock msgLocker = new StripedLock(STRIPED_LOCK_CAPACITY);
    /**
     * adapter type, i.e.: gstp, payplus, etc
     */
    protected String adapterType;
    /**
     * class to configure the IEAdapter
     */
    protected IEAdapterConfig config;
    /**
     * IEAdapter which will take the communications responsibility
     */
    protected IEAdapter adapter;
    /**
     * store the message types which have an intermediary "SENDING" status
     */
    protected static Vector<String> msgTypesWithSendingAction;
    protected static Boolean isMsgTypesWithSendingActionEvaluated = Boolean.FALSE;
    /**
     * Helper for Task Station
     */
    protected SantExceptions helper;

    /**
     * concrete classes use this constructor to indicate the adapterType
     *
     * @param adapterType adapter type
     */
    public GatewayMQDocumentSender(final String adapterType) {
        super();
        this.adapterType = adapterType;

        // HD0000007929480: SenderEngine gets locked up when several
        // threads try to get their configuration at the same time.
        final Vector<String> errors = new Vector<>();
        getConfiguration(errors);
    }

    @Override
    public boolean send(final DSConnection ds, final SenderConfig config, final SenderCopyConfig copyConfig,
                        final long eventId, final AdviceDocument document, final Vector copies, final BOMessage message,
                        final Vector errors, final String engineName, final boolean[] saved) {

        this.helper = new SantExceptions();

        boolean result = false;

        // avoid other event for the same message was taken in other thread
        msgLocker.lock(message.getLongId());
        if (checkAdapterType(errors)) {
            // check if the message status is still usable to send the message
            if (checkMessageStatus(config, ds, message, eventId)) {
                if (checkRelatedMsgSent(message, ds, errors)) {
                    // retrieve the configuration of the adapter
                    if (getConfiguration(errors)) {
                        try {
                            synchronized (this.adapter) {
                                this.adapter.init();
                            }
                            final StringBuffer output = stripSwiftExtraInfo(document);
                            String outputString = validateAndConverEOLFormat(output.toString());
                            // write the message to the queue
                            if (writeMsgIntoQueue(this.adapter, outputString, message, errors)) {
                                // if we have wrote the message to the queue
                                // we need to consume the event event if the
                                // action SEND is not applied
                                result = true;
                                // try to apply action send
                                applyActionSend(ds, message);
                            }
                        } catch (final Exception e) {
                            addException(SantExceptionType.TECHNICAL_EXCEPTION, message.getTradeLongId(), message.getLongId(),
                                    "Message can not be Sent; Exception in gateway ", e, errors);
                        } finally {
                            try {
                                this.adapter.commit();
                                this.adapter.stop();
                            } catch (Exception e) {
                                Log.error(this, "Error while trying to stop adapter", e);
                            }
                        }
                    }
                }
            }

            // Save the tasks if any
            this.helper.publishTasks(ds, eventId, "SenderEngine");
        } else {
            // we wont process the event cause the message's status have been
            // changed
            result = true;
        }
        msgLocker.unlock(message.getLongId());
        return result;
    }

    /**
     * Remove the comments from the swift document
     *
     * @param document advice document
     * @return the swift documents without comments
     */
    protected StringBuffer stripSwiftExtraInfo(final AdviceDocument document) {
        final StringBuffer doc = document.getDocument();
        if ("SWIFT".equalsIgnoreCase(document.getAddressMethod())) {
            SwiftMessage.stripExtraInfo(doc);
        }
        return doc;
    }

    /**
     * Add an exception to the taskstation and write it to the log file
     *
     * @param exceptionType exception type
     * @param tradeId       trade id
     * @param msgId         message id
     * @param errorMsg      text describing the error
     * @param ex            optional exception which has caused the error
     * @param errors        list of errors. the exception will be added to this list
     */
    public final void addException(final SantExceptionType exceptionType, final long tradeId, final long msgId,
                                   final String errorMsg, final Exception ex, final Vector<String> errors) {
        int bookId = 0;
        try {
            final Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
            if (trade != null) {
                final Book book = trade.getBook();
                if (book != null) {
                    bookId = book.getId();
                }
            }
        } catch (final RemoteException e) {
            Log.error(this, e + "Could not get trade with TradeId: " + tradeId);
        }

        Log.error(this, this.adapterType + ": " + errorMsg, ex);
        this.helper.addException(exceptionType, this.adapter.getConfigName(), errorMsg, tradeId, 0, msgId, "BOMessage",
                bookId);
        errors.addElement(errorMsg);

    }

    protected boolean applyActionSend(final DSConnection ds, final BOMessage message) {
        boolean result = false;
        try {
            result = updateMessageStatus(ds, message, Action.SEND);
            if (!result) {
                Log.error(this,
                        "Unable to apply Action SEND to message because the message has chaned its status. Anyway, the message have been written in the queue");
            }
        } catch (final Exception ex) {
            Log.error(this, "Unable to apply Action SEND to message but the message have been written in the queue",
                    ex);
        }
        return result;
    }

    /**
     * Checks if the adapter type is not null and generates errors if this is
     * not the case
     *
     * @param errors to return the errors in case they happen
     * @return true if the adapter type is not null
     */
    protected final boolean checkAdapterType(final Vector<String> errors) {
        if (this.adapterType == null) {
            Log.error(this, "GatewayMQDocumentSender::send::Implementation must specify a valid adapter type");
            final String error = "Message can not be Sent; Exception in gateway: adapterType can not be null";
            this.helper.addException(SantExceptionType.TECHNICAL_EXCEPTION, "GatewayMQDocumentSender", error, 0, 0, 0,
                    null, 0);
            errors.addElement(error);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if the status of the message is still relevant to be send
     *
     * @param config       Sender config
     * @param dsConnection DataServer connection
     * @param msg          message to be checked
     * @param eventId      event which caused the document to be send
     * @param engineName   engineName (normally SenderEngine)
     * @return true if the message is relevant to be send, false in other case
     */
    public final boolean checkMessageStatus(final SenderConfig config, final DSConnection dsConnection,
                                            final BOMessage msg, final long eventId) {

        // Retrieve latest version of the message because the event contains the
        // original version
        BOMessage latestVersionMsg;
        try {
            latestVersionMsg = dsConnection.getRemoteBO().getMessage(msg.getLongId());
        } catch (final RemoteException e) {
            Log.info(this,
                    "Can not retrieve the latest version of the message due a remote exception. It will be processed when the SenderEngine restart \n" + e);
            return false;
        }
        if (!config.getStatus().equals(latestVersionMsg.getStatus())) {
            // the status of the message was changed after the creation of
            // the event we need to remove the event from the SenderEngine queue
            final StringBuilder sb = new StringBuilder("Event ");
            sb.append(eventId);
            sb.append(", Message ");
            sb.append(msg.getLongId());
            sb.append(" ignored cause message status is not up to date");
            /**
             * we do not need to process the message because it will be done
             * automatically when the method send finish
             */
            Log.info(this, sb.toString());
            return false;
        }
        return true;

    }

    /**
     * return the adapter type
     *
     * @return adapter type
     */
    public final String getAdapterType() {
        return this.adapterType;
    }

    /**
     * returns the config file name
     *
     * @return the config file name
     */
    public abstract String getConfigFileName();

    /**
     * returns the config name
     *
     * @return the config name
     */
    public abstract String getConfigName();


    private final void initIEAdapter() {
        if (this.config == null) {
            this.config = IEAdapter.getConfig(getConfigName());
            //Needed to set the right adapter type
            if (this.config instanceof SantanderIEAdapterConfig) {
                ((SantanderIEAdapterConfig) this.config).setAdapterType(getAdapterType());
            }
        }
    }

    /**
     * Get the configuration and the IEAdapter
     *
     * @param errors to return the errors in case they happen
     * @return true if the adapter can be properly configured, false in other
     * case
     */
    // HD0000007929480: SenderEngine gets locked up when several
    // threads try to get their configuration at the same time.
    protected final synchronized boolean getConfiguration(final Vector<String> errors) {

        initIEAdapter();

        if ((this.config == null) || !this.config.isConfigured(getConfigFileName())) {
            Log.error(this, "GatewayMQDocumentSender::send::" + getConfigName() + " not configured properly ");
            final String error = "Message can not be Sent; Exception in gateway: " + this.adapterType
                    + " not configured properly";
            this.helper.addException(SantExceptionType.CONFIGURATION_EXCEPTION, getConfigName(), error, 0, 0, 0, null,
                    0);

            errors.addElement(error);
            return false;
        } else {
            this.adapter = this.config.getSenderIEAdapter();
            return true;
        }
    }

    @Override
    public final boolean isOnline() {
        return true;
    }

    /**
     * returns true if the message type's workflow uses a intermediary "SENDING"
     * status
     *
     * @param ds      data server connection
     * @param msgType message type
     * @return true if the "SENDING" status is needed for this message type
     * @throws RemoteException when the dataserver answer with a RemoteException
     */
    public final synchronized boolean needToApplyActionSending(final DSConnection ds, final String msgType)
            throws RemoteException {
        // synchronized block so that only one message could update the
        // 'msgTypesWithSendingAction' object.
        // [CAL_SUP_125]
        if (!isMsgTypesWithSendingActionEvaluated) {
            GatewayMQDocumentSender.msgTypesWithSendingAction = new Vector<>();
            final Vector<?> result = ds.getRemoteAccess().executeSelectSQL(
                    "select distinct MSG_TYPE from  wfw_transition where event_class = 'PSEventMessage' and possible_action = 'SENDING'", null);
            if (result.size() > 2) {
                for (int i = 2; i < result.size(); i++) {
                    if (result.get(i) instanceof Vector) {
                        Vector<?> rawRow = (Vector<?>) result.get(i);
                        Vector<String> row = new Vector<String>();
                        for (Object value : rawRow) {
                            if (value instanceof String) {
                                row.add((String) value);
                            } else {
                                row.add(null);
                            }
                        }
                        GatewayMQDocumentSender.msgTypesWithSendingAction.add(row.get(0));
                    }

                }
                setIsMsgTypesWithSendingActionEvaluated(Boolean.TRUE);
            }
        }

        return GatewayMQDocumentSender.msgTypesWithSendingAction.contains(msgType);
    }

    /**
     * set the adapter type
     *
     * @param adapterType adapter type
     */
    public final void setAdapterType(final String adapterType) {
        this.adapterType = adapterType;
    }

    /**
     * applies an action to the message once it was sent
     *
     * @param ds      connection to the DataServer
     * @param message message to be send
     * @param action  message's action to be applied
     * @return true if the update was ok
     * @throws Exception in case of an unexpected exception
     */
    protected final boolean updateMessageStatus(final DSConnection ds, final BOMessage message, final Action action)
            throws Exception {
        boolean result = false;
        final ApplyActionToMessageRetriedAction applyAction = new ApplyActionToMessageRetriedAction(message.getLongId(),
                action, null);
        applyAction.execute(UPD_STATUS_RETRIES, UPD_STATUS_WAITING_TIME);
        result = applyAction.isSuccess();

        return result;
    }

    /**
     * Write the document into the JMS queue
     *
     * @param adapter  IEAdapter which take care of the communications
     * @param document AdviceDocument
     * @param message  BOMessage
     * @param errors   If there are any errors they will be added to this list
     * @return true if the write could be done; false in case of error
     */
    protected boolean writeMsgIntoQueue(final IEAdapter adapter, final String document, final BOMessage message,
                                        final Vector<String> errors) {
        final boolean result = this.adapter.write(document);

        if (!result) {
            addException(SantExceptionType.TECHNICAL_EXCEPTION, message.getTradeLongId(), message.getLongId(),
                    "Message can't be written in the MQ queue", null, errors);
        } else {
            final StringBuilder sb = new StringBuilder("Message ");
            sb.append(message.getLongId());
            sb.append(" sent to ");
            sb.append(this.adapterType);
            Log.info(this, sb.toString());
        }
        return result;
    }

    /**
     * check if the related message of this message was sent or is to be send
     *
     * @param msg BOMessage
     * @param ds  DSConnection
     * @return true if the message was sent, false if is still waiting to be
     * sent
     */
    protected final boolean isRelatedMessageSent(final BOMessage msg, final DSConnection ds) {
        final long linkedId = msg.getLinkedLongId();
        if (linkedId != 0) {
            BOMessage relatedMsg;
            try {
                relatedMsg = ds.getRemoteBO().getMessage(linkedId);
            } catch (final RemoteException e) {
                // Something wrong occurred with the ds, we'll wait a bit
                Log.warn(this, "isRElatedMessageSent: we can't not retrieve the related message: returning false \n" + e);
                return false;
            }
            if (relatedMsg != null) {
                final String status = relatedMsg.getStatus().getStatus();
                return !("SENDING".equals(status) || "TO-BE-SEND".equals(status));
            }
        }
        return true;
    }

    /**
     * if the message is linked to another message by the linkedId, we need to
     * wait until the linked message was sent.
     *
     * @param msg    message to be send
     * @param ds     ds connection
     * @param errors in case of error a new item will be added to the list
     * @return false the linked message was not send before reach the maximum
     * number of retries
     */
    protected boolean checkRelatedMsgSent(final BOMessage msg, final DSConnection ds, final Vector<String> errors) {
        boolean rst = false;
        int retries = CHECK_RELATED_MSG_SENT_MAX_RETRIES;
        final int wait = CHECK_RELATED_MSG_SENT_WAITING_TIME;

        while (!rst && (retries > 0)) {
            if (isRelatedMessageSent(msg, ds)) {
                rst = true;
            } else {
                try {
                    Log.warn(this, "message: " + msg + " is waiting for the message " + msg.getLinkedLongId()
                            + " to be send. Retries remaining " + retries);
                    Thread.sleep(wait);
                } catch (final InterruptedException e) {
                    // try to continue
                    Log.error(this, e.getMessage());
                }
            }
            --retries;
        }
        if (!rst) {
            addException(SantExceptionType.TECHNICAL_EXCEPTION, msg.getTradeLongId(), msg.getLongId(),
                    "Message can not be Sent; Related msg has not be sent ", null, errors);
        }
        return rst;
    }

    private static synchronized void setIsMsgTypesWithSendingActionEvaluated(
            final Boolean isMsgTypesWithSendingActionEvaluated) {
        GatewayMQDocumentSender.isMsgTypesWithSendingActionEvaluated = isMsgTypesWithSendingActionEvaluated;
    }

    /**
     * @param originalMessage
     * @return
     */
    protected String validateAndConverEOLFormat(String originalMessage) {
        // By default, don't convert EOL sequences
        return originalMessage;
    }
}

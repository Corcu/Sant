/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.paymentshub;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentHubImportUtil;
import calypsox.tk.bo.util.PaymentsHubCallback;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.core.retried.actions.ApplyActionToMessageAttributesRetriedAction;
import calypsox.tk.event.PSEventPaymentsHubImport;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import com.calypso.engine.Engine;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.ArrayList;
import java.util.List;


/**
 * SantPaymentsHubCallbackEngine is the engine to send PaymentsHub messages.
 *
 * @author
 *
 */
public class SantPaymentsHubCallbackEngine extends Engine {


    private static SantExceptions exceptions = null;
    public static final String ENGINE_NAME = "SANT_PaymentsHubCallbackEngine";
    // Comments
    private static final String PARSE_COMMENT = "The JSON message received from PaymentsHub could not be read and parsed correctly";
    private static final String ADVICE_DOCUMENT_NOT_FOUND_COMMENT = "The AdviceDocument does not exist: the AdviceDocument cannot be retrieved from the message received from PaymentsHub";
    private static final String MESSAGE_NOT_FOUND_COMMENT = "The BOMessage does not exist: a BOMessage cannot be found from the 'IdempotentReference' value [%s] of the message received from PaymentsHub";
    private static final String TRANSFER_NOT_FOUND_COMMENT = "The BOTransfer does not exist: a BOTransfer cannot be found from the 'IdempotentReference' value [%s] of the message received from PaymentsHub";
    private static final String ACTION_NOT_FOUND_COMMENT = "The Action to be applied to the BOMessage does not exist: an Action cannot be found from the 'Status' [%s] and the 'CommunicationStatus' [%s] values of the message received from PaymentsHub";
    private static final String ACTION_NOT_APPLIED_COMMENT = "The Action [%s] cannot be applied to the BOMessage";
    private static final int UPD_STATUS_RETRIES = 1;
    private static final long UPD_STATUS_WAITING_TIME = 1;
    private static final PaymentHubImportUtil util = PaymentHubImportUtil.getInstance();


    public SantExceptions getExceptions() {
        if (exceptions == null) {
            exceptions = new SantExceptions();
        }
        return exceptions;
    }


    /**
     * Creates a new SantPaymentsHubCallbackEngine.
     *
     * @param dsCon
     *            DSConnection to be used.
     * @param hostName
     *            Name of the host.
     * @param port
     *            Port to connect
     */
    public SantPaymentsHubCallbackEngine(final DSConnection dsCon, final String hostName, final int port) {
        super(dsCon, hostName, port);
    }


    /**
     * Gets the Engine name.
     *
     * @return SantPaymentsHubCallbackEngine.ENGINE_NAME always.
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }


    // -------------------------------------
    // ---- PROCESS INPUT - WebService ----
    // -------------------------------------


    /**
     * Process a single PSEvent.
     *
     * @param psEvent
     *            Event to be processed.
     *
     * @return True if properly processed false if not.
     */
    @Override
    public boolean process(final PSEvent psEvent) {
        boolean rst = false;
        Log.debug(this, "Received PSEvent : " + psEvent.toString());
        if (psEvent instanceof PSEventMessage) {
            final PSEventMessage event = (PSEventMessage) psEvent;
            rst = processBOMessage(event);
        } else if (psEvent instanceof PSEventPaymentsHubImport) {
            final PSEventPaymentsHubImport event = (PSEventPaymentsHubImport) psEvent;
            rst = processImportEvent(event);
        }
        // Publish Tasks if any
        publishTasks(getDS(), psEvent.getLongId(), getEngineName());
        // Mark event as processed
        rst = (rst) ? PaymentHubImportUtil.getInstance().consumeEvent(psEvent, getEngineName()) : rst;
        return rst;
    }


    /**
     * Process BOMessage.
     *
     * Parse JSON.
     *
     * @param event
     * @return
     */
    protected boolean processBOMessage(final PSEventMessage event) {
        String debug = "";
        boolean isProcessedOk = false;
        // Get BOMessage
        final BOMessage boMessageInput = event.getBoMessage();
        if (boMessageInput == null) {
            debug = String.format("The BOMessage from Event [%s] is null.", String.valueOf(event.getLongId()));
            Log.error(this, debug);
            return false;
        }
        // 1. Get the AdviceDocument
        final long idInput = boMessageInput.getLongId();
        final AdviceDocument document = getLatestAdviceDocument(idInput);
        if (document != null) {
            // 2. Parse JSON document to Java object
            final String text = document.getTextDocument().toString();
            final PaymentsHubCallback phRequest = PaymentsHubCallback.parseText(text);
            if (phRequest != null) {
                // 3. Process PaymentsHubRequest
                isProcessedOk = processPaymentsHubRequest(phRequest, boMessageInput);
            } else {
                debug = String.format("Error retrieving PaymentsHubCallback. Unable to parse received document [%s].", text);
                Log.debug(this, debug);
                // addException : The JSON text can not be parsed to PaymentsHubCallback
                addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageInput, idInput, boMessageInput.getClass().getSimpleName(), "", PARSE_COMMENT);
            }
        } else {
            // addException : The AdviceDocument does not exist
            addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageInput, idInput, boMessageInput.getClass().getSimpleName(), "", ADVICE_DOCUMENT_NOT_FOUND_COMMENT);
            debug = String.format("The BOMessage [%s] has not any AdviceDocument.", String.valueOf(idInput));
            Log.debug(this, debug);
        }
        return isProcessedOk;
    }


    /**
     * Process PaymentsHub Request.
     *
     * @param paymentsHubCallback
     * @param boMessageInput
     * @return
     */
    protected boolean processPaymentsHubRequest(final PaymentsHubCallback paymentsHubCallback, final BOMessage boMessageInput) {
        String debug = "";
        boolean isProcessedOk = false;
        // Get IdempotentReference - instrId <-> TransferLongId
        final String idRef = paymentsHubCallback.getIdempotentReference();
        // Get BOTransfer using IdempotentReference

        final BOMessage boMessage = PaymentHubImportUtil.getInstance().getBOMessageFromIdRef(idRef);
        if(null!=boMessage){
            final BOTransfer xfer = PaymentHubImportUtil.getInstance().getBOTransferFromBOMessage(boMessage);
            if (xfer != null) {
                    // Check if BOTransfer has been previously processed by PaymentsHub
                    if (checkPaymentsHubCallback(xfer, paymentsHubCallback)) {
                        // Update BOMessage
                        isProcessedOk = updateBOMessage(boMessage, boMessageInput, paymentsHubCallback);
                    } else {
                        debug = String.format("There was a previous PaymentsHub Callback. BOMessage [%s] and its related BOTransfer [%s] will not updated.",
                                        String.valueOf(boMessage.getLongId()), String.valueOf(boMessage.getTransferLongId()));
                        Log.debug(SantPaymentsHubCallbackEngine.class, debug);
                        isProcessedOk = true;
                    }
            } else {
                // addException : a BOMessage cannot found from the value of IdempotentReference
                final String taskComment = getTaskComment(MESSAGE_NOT_FOUND_COMMENT, idRef);
                addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageInput,
                        boMessageInput.getLongId(), boMessageInput.getClass().getSimpleName(), "", taskComment);
                debug = String.format("No BOMessage found using the IdempotentReference [%s].", idRef);
                Log.debug(SantPaymentsHubCallbackEngine.class, debug);
            }
        }else {
            // addException : Cannot retrieve BOTransfer from IdempotentReference
            final String taskComment = getTaskComment(TRANSFER_NOT_FOUND_COMMENT, idRef);
            addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageInput, boMessageInput.getLongId(), boMessageInput.getClass().getSimpleName(), "", taskComment);
            debug = String.format("Cannot retrieve BOTransfer from 'IdempotentReference' value [%s].", idRef);
            Log.debug(SantPaymentsHubCallbackEngine.class, debug);
        }
        return isProcessedOk;
    }


    /**
     * Update BOMessage.
     *
     * @param boMessageToUpdate
     * @param boMessageInput
     * @param paymentsHubCallback
     * @return
     */
    private boolean updateBOMessage(final BOMessage boMessageToUpdate, final BOMessage boMessageInput, final PaymentsHubCallback paymentsHubCallback) {
        String debug = "";
        boolean isBOMessageUpdated = false;
        // Get Status and CommunicationStatus
        final String status = paymentsHubCallback.getStatus();
        final String commStatus = paymentsHubCallback.getCommunicationStatus();
        // Update BOMessage attributes
        updateBOMessageAttributes(boMessageToUpdate, boMessageInput);
        // Get the action to apply.
        final Action action = getAction(status, commStatus);
        if (action != null) {
            // Apply action and save
            final boolean isActionApplyMsg = applyAction(boMessageToUpdate, action);
            if (!isActionApplyMsg) {
                // addException : Cannot apply the Action to the BOMessage
                final String taskComment = getTaskComment(ACTION_NOT_APPLIED_COMMENT, action.toString());
                addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageToUpdate, boMessageToUpdate.getLongId(), boMessageToUpdate.getClass().getSimpleName(), "", taskComment);
                debug = String.format("The Action [%s] could not apply to the BOMessage [%s].", action.toString(), String.valueOf(boMessageToUpdate.getLongId()));
                Log.debug(SantPaymentsHubCallbackEngine.class, debug);
            } else {
                // Action applied and BOMessage updated
                debug = String.format("The Action [%s] has been applied to the BOMessage [%s].", action.toString(), String.valueOf(boMessageToUpdate.getLongId()));
                Log.debug(SantPaymentsHubCallbackEngine.class, debug);
                isBOMessageUpdated = true;
            }
        } else {
            // addException : Cannot found an Action from the value of Status and the value of
            // Communications Status
            final String taskComment = getTaskComment(ACTION_NOT_FOUND_COMMENT, status, commStatus);
            addException(SantExceptionType.PH_CALLBACK_EXCEPTION, boMessageInput, boMessageInput.getLongId(), boMessageInput.getClass().getSimpleName(), "", taskComment);
            debug = String.format("Action not found using the Status [%s] and the Communications Status [%s].", status, commStatus);
            Log.debug(SantPaymentsHubCallbackEngine.class, debug);
        }
        return isBOMessageUpdated;
    }


    protected BOMessage getBOMessage(String idRef) {
        BOMessage message = null;
        try {
            long transferId = Long.parseLong(idRef);
            message = getBOMessageFromTransferId(transferId);
        } catch (NumberFormatException e) {
            String errorMessage = String.format("Could not parse \"%s\" as transfer id", idRef);
            Log.error(SantPaymentsHubCallbackEngine.class, errorMessage, e);
        }
        return message;
    }


    private static BOMessage getBOMessageFromTransferId(long transferId) {
        BOMessage message = null;
        String where = String.format("transfer_id = %d AND message_type IN ('%s','%s') AND template_name != '%s'", transferId, PHConstants.PAYMENTHUB_PAYMENTMSG_TYPE, PHConstants.PAYMENTHUB_RECEIPTMSG_TYPE, PHConstants.MESSAGE_PH_FICTCOV);
        String orderBy = "message_id DESC";
        try {
            MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(null, where, orderBy, null);
            if (messages != null && messages.size() > 0) {
                message = messages.get(0);
            }
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format("Could not retrieve messages from transfer %d", transferId);
            Log.error(SantPaymentsHubCallbackEngine.class, errorMessage, e);
        }
        return message;
    }


    /**
     * Check if BOTransfer has been updated by PaymentsHub:
     *
     * - If the BOTransfer has the 'PHSettlementStatus'.
     *
     * - If the BOTransfer received another PH callback.
     *
     * @param xfer
     * @param paymentsHubCallback
     * @return
     */
    private static boolean checkPaymentsHubCallback(final BOTransfer xfer, final PaymentsHubCallback paymentsHubCallback) {
        String debug = "";
        if (xfer == null) {
            debug = String.format("Check BOTransfer [%s] param.", (xfer == null) ? "KO" : "OK");
            Log.debug(SantPaymentsHubCallbackEngine.class, debug);
            return false;
        }
        // Check if the xfer has the 'PHSettlementStatus' attribute
        final String phSettStatusAttr = xfer.getAttribute(PHConstants.XFER_ATTR_PH_SETTLEMENT_STATUS);
        if (Util.isEmpty(phSettStatusAttr)) {
            // It is the first PaymentsHub callback received for this
            // BOTransfer.
            return true;
        } else {
            // Actual timestamp
            final JDatetime timestampActual = paymentsHubCallback.getTime();
            // Previously, the transfer received another PaymentsHub callback.
            final String timestampPrevStr = xfer.getAttribute(PHConstants.XFER_ATTR_PH_CALLBACK_TIMESTAMP);
            final JDatetime timestampPrev = PaymentsHubCallback.parseTimestamp(timestampPrevStr);
            // If the actual timestamp is before timestampPrev -> do nothing
            // If the actual timestamp is after timestampPrev -> do process
            return (timestampPrev != null) ? timestampActual.after(timestampPrev) : false;
        }
    }


    /**
     * Update BOMessage attributes.
     *
     * @param boMessageToUpdate
     * @param boMessageIncoming
     */
    private static void updateBOMessageAttributes(final BOMessage boMessageToUpdate, final BOMessage boMessageIncoming) {
        // Add MessageAttributes
        boMessageToUpdate.setAttribute(PHConstants.MSG_ATTR_PH_CALLBACK_MESSAGEID, String.valueOf(boMessageIncoming.getLongId()));
    }


    private boolean processImportEvent(PSEventPaymentsHubImport event) {
        boolean savedOK = false;
        StringBuffer inputJson = event.getInputJson();
        if (inputJson != null) {
            savedOK = PaymentHubImportUtil.getInstance().saveBOMessage(inputJson, ENGINE_NAME);
        } else {
            String errorMessage = String.format("[Event %d] Input JSON is null", event.getLongId());
            Log.error(this, errorMessage);
        }
        return savedOK;
    }


    /**
     * Get Latest AdviceDocument.
     *
     * @param messageId
     * @return
     */
    protected AdviceDocument getLatestAdviceDocument(final long messageId) {
        AdviceDocument doc = null;
        try {
            doc=DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(messageId, new JDatetime());
        } catch (CalypsoServiceException e) {
            Log.error(this, e.getCause());
        }
        return doc;
    }


    /**
     * Get BOTransfer by IdempotentReference.
     *
     * @param idempotentReference
     * @return
     */
    protected BOTransfer getBOTransfer(final String idempotentReference) {
        if (!Util.isEmpty(idempotentReference) && PaymentsHubUtil.isCreatable(idempotentReference)) {
            //PaymentHubImportUtil.getInstance().getTransfer(DSConnection.getDefault(),new StringBuffer(idempotentReference));

            final long transferId = Long.parseLong(idempotentReference);
            final BOTransfer xfer = PaymentsHubUtil.getBOTransfer(transferId);
            return xfer;
        }
        return null;
    }


    /**
     * Get Action from Status and CommunicationStatus
     *
     * @param status
     * @param commStatus
     * @return
     */
    protected Action getAction(final String status, final String commStatus) {
        if (!Util.isEmpty(status) && !Util.isEmpty(commStatus)) {
            return util.getAction(status, commStatus);
        }
        final String debug = String.format("The Action cannot be retrieved. Check the value of 'Status' [%s] and the value of 'Communication Status' [%s]. ", status, commStatus);
        Log.debug(this, debug);
        return null;
    }


    /**
     * Apply action to BOMessage and save it.
     *
     * @param boMessage
     * @param action
     * @return
     */
    protected boolean applyAction(final BOMessage boMessage, final Action action) {
        final BOTransfer xfer = PaymentsHubUtil.getBOTransfer(boMessage.getTransferLongId());
        final Trade trade = PaymentsHubUtil.getTrade(boMessage.getTradeLongId());
        return applyAction(boMessage, xfer, trade, action);
    }


    /**
     * Apply Action to BOMessage.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param action
     * @return
     */
    private static boolean applyAction(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final Action action) {
        String debug = "";
        final List<String> errors = new ArrayList<String>();
        final ApplyActionToMessageAttributesRetriedAction applyAction = new ApplyActionToMessageAttributesRetriedAction(boMessage.getLongId(), action, boMessage.getAttributes());
        try {
            applyAction.execute(UPD_STATUS_RETRIES, UPD_STATUS_WAITING_TIME);
        } catch (ApplyActionToMessageAttributesRetriedAction.RetriedActionException e) {
            Log.error(SantPaymentsHubCallbackEngine.class, "Unable to apply Action to message", e);
        }
        if (applyAction.isSuccess()) {
            debug = String.format("Action [%s] was successfully applied to BOMessage [%d].", action.toString(), boMessage.getLongId());
            Log.debug(SantPaymentsHubCallbackEngine.class, debug);
            return true;
        }
        final StringBuffer sb = new StringBuffer();
        errors.forEach(e -> sb.append(e).append(";"));

        debug = String.format("Action [%s] was not applied to BOMessage [%d]. ", action.toString(), boMessage.getLongId());
        if (sb.length() > 0) {
            debug = debug.concat("Reasons: ").concat(sb.toString());
        }
        Log.debug(SantPaymentsHubCallbackEngine.class, debug);
        return false;
    }


    /**
     * Get comment formatted with the parameters.
     *
     * @param commentWithParams
     * @param parameters
     * @return
     */
    private static String getTaskComment(final String commentWithParams, final Object... parameters) {
        return (!Util.isEmpty(commentWithParams) && !Util.isEmpty(parameters)) ? String.format(
                commentWithParams, parameters) : "";
    }


    /**
     * Add the new Exception generated.
     *
     * @param exceptionType
     * @param object
     * @param objectId
     * @param objectClassName
     * @param source
     * @param comment
     */
    public void addException(final SantExceptionType exceptionType, final Object object, final long objectId,
                             final String objectClassName, final String source, final String comment) {
        // Add Exception
        getExceptions().addException(exceptionType, object.toString(), source, comment, 0, 0, objectId, objectClassName, 0);
    }


    /**
     * Publish Tasks in TaskStation.
     *
     * @param ds
     * @param eventId
     * @param engineName
     */
    public void publishTasks(final DSConnection ds, final long eventId, final String engineName) {
        // Save the tasks if any
        getExceptions().publishTasks(ds, eventId, engineName);
        // Clean Helper
        getExceptions().clear();
    }


    // ----------------------------------------------------------------


/**
    @Override
    protected void prestop(boolean willTerminate) {
        super.prestop(willTerminate);
        // Shutdown EngineContext
        shutdown();
    }


    @Override
    public void stop() {
        Log.info(this, "Stopping engine at Engine.class");
        super.stop();
        Log.info(this, "Engine " + getEngineName() + " stopped at Engine.class");
    }
*/


}

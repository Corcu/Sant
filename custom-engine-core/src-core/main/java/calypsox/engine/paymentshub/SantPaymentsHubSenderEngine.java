/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.paymentshub;


import calypsox.tk.bo.PaymentsHubMessage;
import calypsox.tk.bo.PaymentsHubHelper;
import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.core.retried.RetriedAction;
import calypsox.tk.core.retried.actions.ApplyActionToMessageAttributesRetriedAction;
import com.calypso.engine.Engine;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.AdviceDocumentBuilder;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;
import com.santander.restservices.ApiRestAdapter;
import org.apache.commons.lang3.StringUtils;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SantPaymentsHubSenderEngine extends Engine {


    private static final String LOG_CATEGORY = SantPaymentsHubSenderEngine.class.getName();
    public static final String DV_PH_SERVICE_LAST_CALL_IN = "LastCallIn";
    private static final int UPD_STATUS_RETRIES = 1;
    private static final int UPD_STATUS_WAITING_TIME = 1;



    /**
     * Name of the engine.
     */
    public static final String ENGINE_NAME = "SANT_PaymentsHubSenderEngine";


    /**
     * Creates a new SantPaymentsHubSenderEngine.
     *
     * @param dsCon DSConnection to be used.
     * @param hostName Name of the host.
     * @param port Port to connect
     */
    public SantPaymentsHubSenderEngine(final DSConnection dsCon, final String hostName, final int port) {
        super(dsCon, hostName, port);
    }


    /**
     * Gets the Engine name.
     *
     * @return SantPaymentsHubSenderEngine.ENGINE_NAME always.
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }


    // -------------------------------------
    // ---- PROCESS OUTPUT - WebService ----
    // -------------------------------------


    /**
     * Process a single PSEvent.
     *
     * @param psEvent Event to be processed.
     *
     * @return True if properly processed false if not.
     */
    @Override
    public boolean process(final PSEvent psEvent) {
        boolean rst = false;
        Log.debug(LOG_CATEGORY, "Received PSEvent : " + psEvent.toString());
        if (psEvent instanceof PSEventMessage) {
            final PSEventMessage event = (PSEventMessage) psEvent;
            final BOMessage boMessage = event.getBoMessage();
            if (boMessage != null) {
                rst = processBOMessage(boMessage);
            } else {
                final String debug = String.format("BOMessage from PSEvent [%s] does not exists.", String.valueOf(event.getLongId()));
                Log.debug(LOG_CATEGORY, debug);
            }
        }
        // Mark event as processed
        //rst = (rst) ? SantEngineUtil.getInstance().markEventAsProcessed(psEvent, getEngineName()) : rst;
        if (rst){
            try {
                getDS().getRemoteTrade().eventProcessed(psEvent.getLongId(), getEngineName());
            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY, "Couldn't process the event: " + psEvent.getLongId() + " - " + e.getMessage());
            }
        }
        return rst;
    }


    /**
     * Process a single BOMessage. Creates a JSON message and send it by webservice.
     *
     * @param boMessage BOMessage to be processed.
     * @return True always.
     */
    protected boolean processBOMessage(final BOMessage boMessage) {
        String debug = "";
        boolean rst = false;
        try {
            // 1. Call Service
            final PaymentsHubHelper helper = new PaymentsHubHelper();
            helper.callPaymentsHubService(boMessage, getPricingEnv());
            if (!helper.isConnectionFail() && helper.isHttpOk()) {
                // 1.1. Generate AdviceDocument with the JSON text sent.
                final String requestJsonText = helper.getTextMessage();
                final AdviceDocument adviceDocument = generateAdviceDocument(boMessage, this.getPricingEnv(), requestJsonText);
                // 1.2. Save advice document sent.
                if (!saveAdviceDocument(adviceDocument)) {
                    debug = String.format("The Advice Document for the BOMessage [%s] has not been saved.", String.valueOf(boMessage.getLongId()));
                    Log.error(LOG_CATEGORY, debug);
                }
            }
            // 2. Update BOMessage
            if (!updateBOMessage(boMessage, helper)) {
                debug = String.format("The BOMessage [%s] has not been updated.", String.valueOf(boMessage.getLongId()));
                Log.error(LOG_CATEGORY, debug);
            } else {
                // BOMessage updated
                rst = true; // mark the event as processed
            }
        } catch (final Exception e) {
            final String error = "Error calling the PaymentsHub Service.";
            Log.error(LOG_CATEGORY, error, e);
        }
        return rst;
    }


    /**
     * Update BOMessage.
     *
     * @param boMessage
     * @param helper
     * @return
     */
    private static boolean updateBOMessage(final BOMessage boMessage, final PaymentsHubHelper helper) {
        // Update BOMessage
        updateBOMessageAttributes(helper, boMessage);
        // Get action to apply to BOMessage
        final Action action = getAction(boMessage, helper);
        final boolean isActionApplyMsg = applyAction(boMessage, action);
        if (!isActionApplyMsg && action != null) {
            final String debug = String.format("The Action [%s] could not apply to the BOMessage [%s].", action.toString(), String.valueOf(boMessage.getLongId()));
            Log.error(LOG_CATEGORY, debug);
        }
        return isActionApplyMsg;
    }


    /**
     * Apply action to BOMessage and save it.
     *
     * @param boMessage
     * @param isAck
     * @return
     */
    private static boolean applyAction(final BOMessage boMessage, final Action action) {
        String debug = "";
        boolean isApply = false;
        if (boMessage == null) {
            debug = "The action can not be applied to the BOMessage because the BOMessage is null. ";
            Log.error(LOG_CATEGORY, debug);
            return isApply;
        }
        if (action == null) {
            debug = String.format("The action to be applied on the BOMessage [%s] is null. ", String.valueOf(boMessage.getLongId()));
            Log.error(LOG_CATEGORY, debug);
            return isApply;
        }
        // Get Trade and BOTransfer from BOMessage
        final long xferId = boMessage.getTransferLongId();
        final long tradeId = boMessage.getTradeLongId();
        final BOTransfer xfer = PaymentsHubUtil.getBOTransfer(xferId);
        final Trade trade = (tradeId > 0) ? PaymentsHubUtil.getTrade(boMessage.getTradeLongId()) : null;
        // Apply Action to BOMessage
        final List<String> errors = new ArrayList<String>();
        final ApplyActionToMessageAttributesRetriedAction applyAction = new ApplyActionToMessageAttributesRetriedAction(boMessage.getLongId(), action, boMessage.getAttributes());
        try {
            applyAction.execute(UPD_STATUS_RETRIES, UPD_STATUS_WAITING_TIME);
            isApply = applyAction.isSuccess();
        } catch (RetriedAction.RetriedActionException e) {
            Log.error(LOG_CATEGORY, "Unable to apply Action to message", e);
        }
        if (!isApply || !errors.isEmpty()) {
            final String strErrors = !errors.isEmpty() ? StringUtils.join(errors, ";") : "";
            debug = String.format("Error applying action [%s] to BOMessage [%s]. Errors [%s] ", action.toString(), String.valueOf(boMessage.getLongId()), strErrors);
            Log.error(LOG_CATEGORY, debug);
        }
        return isApply;
    }


    /**
     * Save AdviceDocument to database.
     *
     * @param adviceDocument
     * @return
     */
    private boolean saveAdviceDocument(final AdviceDocument adviceDocument) {
        String debug = "";
        boolean isSaved = false;
        if (adviceDocument == null) {
            debug = "The Advice Document is Null. Nothing to save.";
            Log.error(LOG_CATEGORY, debug);
            return isSaved;
        }
        try {
            final RemoteBackOffice rbo = DSConnection.getDefault().getRemoteBO();
            final long adviceDocId = RemoteAPI.save(rbo, adviceDocument, 0, getEngineName());
            if (adviceDocId > 0) {
                debug = String.format("The Advice Document [%s] has been saved. Advice [%s]",
                        String.valueOf(adviceDocument.getLongId()), String.valueOf(adviceDocument.getAdviceLongId()));
                Log.debug(LOG_CATEGORY, debug);
                isSaved = true;
            }
        } catch (final RemoteException e) {
            debug = "Error saving the Advice Document.";
            Log.error(LOG_CATEGORY, debug, e);
        }
        return isSaved;
    }


    /**
     * Generate the AdviceDocument.
     *
     * @param boMessage
     * @param pricingEnv
     * @param text
     * @return
     */
    private AdviceDocument generateAdviceDocument(final BOMessage boMessage, final PricingEnv pricingEnv,
                                                  final String text) {
        // Create PaymentsHubMessage.
        final PaymentsHubMessage phMessage = new PaymentsHubMessage(boMessage);
        phMessage.setPricingEnv(pricingEnv);
        phMessage.parsePaymentsHubText(text);
        // Create AdviceDocumentBuilder
        final AdviceDocumentBuilder adviceDocBuilder = AdviceDocumentBuilder.create(boMessage);
        adviceDocBuilder.datetime(getDS().getServerCurrentDatetime());
        adviceDocBuilder.userData(phMessage);
        adviceDocBuilder.document(new StringBuffer(text));
        adviceDocBuilder.characterEncoding(StandardCharsets.UTF_8.toString());
        // Build AdviceDocument
        return adviceDocBuilder.build();
    }


    /**
     * Update BOMessage attributes.
     *
     * @param helper
     * @param boMessage
     */
    private static void updateBOMessageAttributes(final PaymentsHubHelper helper, final BOMessage boMessage) {
        // Add attr 'PH_ResponseStatus'
        boMessage.setAttribute(PHConstants.MSG_ATTR_PH_RESPONSE_STATUS, String.valueOf(helper.getStatus()));
        // Add attr 'PH_ResponseMessage'
        boMessage.setAttribute(PHConstants.MSG_ATTR_PH_RESPONSE_MESSAGE, PaymentsHubUtil.substringTextMessAttrValue(helper.getResponseMessage()));
        // Add attr 'PH_PaymentSystemResponseHeader'
        final String xPaymentSystem = getPaymentSystemResponseHeader(helper.getResponseHeaders());
        boMessage.setAttribute(PHConstants.MSG_ATTR_PH_PAY_SYST_RESP_HEADER, xPaymentSystem);
        // Add attr 'PH_CallServiceDatetime'
        String attr = boMessage.getAttribute(PHConstants.MSG_ATTR_PH_CALL_SERVICE_DATETIME);
        if (Util.isEmpty(attr) || !helper.isConnectionFail()) {
            attr = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            boMessage.setAttribute(PHConstants.MSG_ATTR_PH_CALL_SERVICE_DATETIME, attr);
        }
    }


    /**
     * Get 'X-Payment-System' value from Response Header.
     *
     * @param responseHeaders
     * @return
     */
    private static String getPaymentSystemResponseHeader(final Map<String, List<String>> responseHeaders) {
        String xPaymentSystem = "";
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            final List<String> headerValue = responseHeaders.get(ApiRestAdapter.HTTP_HEADER_X_PAYMENT_SYSTEM);
            xPaymentSystem = StringUtils.join(headerValue, ",");
        }
        return xPaymentSystem;
    }


    /**
     * Get Action to apply to BOMessage.
     *
     * @param boMessage
     * @param helper
     * @return
     */
    private static Action getAction(final BOMessage boMessage, final PaymentsHubHelper helper) {
        String action = "";
        if (helper.isConnectionFail()) {
            action = (isRetryAccepted(boMessage)) ? PHConstants.ACTION_PH_WAIT_RETRY : PHConstants.ACTION_PH_CONNECTION_FAIL;
        } else {
            action = (helper.isHttpOk()) ? PHConstants.ACTION_PH_ACK : PHConstants.ACTION_PH_NACK;
        }
        return Action.valueOf(action);
    }


    /**
     * Checks, when the connection has failed, if the process can be retried.
     *
     * @param boMessage
     * @return
     */
    private static boolean isRetryAccepted(final BOMessage boMessage) {
        // Get LastCallIn
        int lastCallInMillis = 0;
        final String strLastCallIn = PaymentsHubUtil.getPaymenstHubParameterValue(DV_PH_SERVICE_LAST_CALL_IN, "0");
        if (PaymentsHubUtil.isCreatable(strLastCallIn)) {
            lastCallInMillis = Integer.parseInt(strLastCallIn) * 1000; // milliseconds
        }
        // Check CallServiceDatetime
        final JDatetime now = new JDatetime(LocalDateTime.now());
        final String attr = boMessage.getAttribute(PHConstants.MSG_ATTR_PH_CALL_SERVICE_DATETIME);
        if (!Util.isEmpty(attr)) {
            final LocalDateTime ldt = LocalDateTime.parse(attr, DateTimeFormatter.ISO_DATE_TIME);
            // Check if Retry is accepted.
            JDatetime callServiceJDatetime = new JDatetime(ldt);
            callServiceJDatetime = callServiceJDatetime.add(lastCallInMillis);
            return (now.before(callServiceJDatetime));
        }
        return false;
    }


    /**
     * Apply an Action in a BOMessage
     *
     * @param boMessage
     * @param boTransfer
     * @param trade
     * @param action
     * @param errors
     * @return
     */
    public boolean applyAction(final BOMessage boMessage, final BOTransfer boTransfer, final Trade trade,
                               final Action action, final List<String> errors) {
        if (boMessage != null) {
            if (BOMessageWorkflow.isMessageActionApplicable(boMessage, boTransfer, trade, action, DSConnection.getDefault())) {
                try {
                    final BOMessage clone = (BOMessage) boMessage.clone();
                    clone.setAction(action);
                    return saveBOMessage(clone, errors) > 0;
                } catch (final CloneNotSupportedException e) {
                    final String error = "Could not clone BOMessage: " + boMessage.getLongId();
                    errors.add(error);
                    Log.error(LOG_CATEGORY, error);
                }
            } else {
                final String debug = "Action " + action.toString() + " is not applicable in BOMessage " + boMessage.getLongId();
                errors.add(debug);
                Log.debug(LOG_CATEGORY, debug);
            }
        }
        return false;
    }


    public long saveBOMessage(final BOMessage boMessage, final List<String> errors) {
        long messageId = 0;
        final String templateName = boMessage.getTemplateName();
        final String errorMessage = String.format("Could not save message with template name %s", templateName);
        try {
            messageId = DSConnection.getDefault().getRemoteBO().save(boMessage, 0, null);
            if (messageId > 0) {
                final String infoMessage = String.format("Message saved with id: %s, template name: %s", String.valueOf(messageId), templateName);
                Log.info(LOG_CATEGORY, infoMessage);
            } else {
                errors.add(errorMessage);
                Log.error(LOG_CATEGORY, errorMessage);
            }
        } catch (final RemoteException e) {
            errors.add(errorMessage);
            Log.error(LOG_CATEGORY, errorMessage, e);
        }
        return messageId;
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
        Log.info(LOG_CATEGORY, "Stopping engine at Engine.class");
        super.stop();
        Log.info(LOG_CATEGORY, "Engine " + getEngineName() + " stopped at Engine.class");
    }
*/


}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.gestorstp;

import calypsox.engine.BaseIEEngine;
import calypsox.engine.TradeCollateralizationServiceEngine;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.SantanderIEAdapter;
import calypsox.util.SantDomainValuesUtil;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import java.util.List;

/**
 * This class implements GestorSTPIncomingMessageEngine for processing MT568,
 * MT569, MT900/MT910
 *
 * @author Carlos Cejudo & Guillermo Solano
 * @version 1.0
 * @date 31/05/2016
 */
public class GestorSTPIncomingMessageEngine extends BaseIEEngine {

    public static String ENGINE_NAME = "SANT_GestorSTPIncomingMessageEngine";

    private static final String ROLLBACK_DV = "EnableGestorSTPRollback";

    private static ExternalMessageParser parser;

    private static final String SWIFT_PARSER_TYPE = "SWIFT_PARSER_TYPE";

    private boolean isRollbackEnabled = false;

    private PricingEnv env = null;

    /**
     * Default Constructor of the service
     *
     * parameter configName
     * @param dsCon
     * @param hostName
     * @param port
     */
    public GestorSTPIncomingMessageEngine(DSConnection dsCon, String hostName, int port) {

        super(dsCon, hostName, port);
        // swift parser can be set up by an engine param, if empty, it will use
        // default
        final String SwifthParser = getEngineParam(SWIFT_PARSER_TYPE, SWIFT_PARSER_TYPE, "SWIFT");
        parser = SwiftParserUtil.getParser(SwifthParser);
        isRollbackEnabled = isRollbackEnabled();
        if (!Util.isEmpty(getPricingEnvName())) {
            setPricingEnv(PricingEnv.loadPE(getPricingEnvName(), null));
        }
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

        boolean proc = false;
        proc = handleIncomingMessage(message);

        return proc;
    }

    /**
     * Calls core to process the SWIFT messages received
     *
     * @param externalMessage  received and passed by the newMessage Method
     * @return if the incoming message has been handle
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {

        Log.info(GestorSTPIncomingMessageEngine.class, "Received message:" + externalMessage.getText() + "\n");
        final ExternalMessage message = cleanSWIFTMessage(externalMessage);

        if (message == null || Util.isEmpty(externalMessage.getText())) {
            Log.info(GestorSTPIncomingMessageEngine.class, "Message receive is null or empty");
            return false;
        }

        boolean messageProcessedOk = false;
        ExternalMessageHandler handler = SwiftParserUtil.getHandler("SWIFT", message.getType());
        /*
         * core logic, for MT910 & MT900 messages, check MT9X0MessageProcessor
         * classes in package calypsox.tk.util.swiftparser
         */
        try {
            if (handler != null) {
                messageProcessedOk = handler.handleExternalMessage(message, getPricingEnv(), null, null, super.getDS(), null);
            } else {
                messageProcessedOk = SwiftParserUtil.processExternalMessage(message, getPricingEnv(), null, null, super.getDS(),
                        null);
            }
        } catch (MessageParseException e) {
            Log.info(GestorSTPIncomingMessageEngine.class, "Parsing error message = " + externalMessage.getText());
            rollbackMsg();
            return false;
        } catch (Exception exc) {
            rollbackMsg();
            return false;
        }

        Log.info(GestorSTPIncomingMessageEngine.class, messageProcessedOk ? "OK" : "ERROR");

        // As FX, response 1 for OK or 0 for KO
        final String output = messageProcessedOk ? "1" : "0";

        // send response to queue, JMSCorrelation ID included
        sendResponse(output, externalMessage);
        Log.info(TradeCollateralizationServiceEngine.class, "B) Message response:" + output + "\n");

        return true;
    }

    /**
     * Name of the engine that offers this service
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public void setPricingEnv(PricingEnv env) {
       this.env=env;
    }

    @Override
    public PricingEnv getPricingEnv() {
        return env;
    }

    // ///////////////////////////////////
    // //////PRIVATE METHODS /////////////
    // ///////////////////////////////////

    /**
     * Send message with response and the correlationId
     *
     * @param output
     * @param externalMessage
     */
    private void sendResponse(String output, ExternalMessage externalMessage) {

        JMSQueueMessage answer = null;

        try {
            if (externalMessage instanceof JMSQueueMessage) {

                final JMSQueueMessage jmsMessage = (JMSQueueMessage) externalMessage;
                answer = new JMSQueueMessage();
                // set the correlation id on the message to send out
                answer.setText(output);
                answer.setCorrelationId(jmsMessage.getCorrelationId());
                answer.setReference(jmsMessage.getReference());

                sendAnswer(answer);
            }

        } catch (final Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);

        }
    }

    /**
     * @param externalMessage
     * @return message with SWIFT end of line format
     */
    private ExternalMessage cleanSWIFTMessage(final ExternalMessage externalMessage) {

        String incomingMessageText = externalMessage.getText();
        incomingMessageText = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessageText);

        try {
            return parser.readExternal(incomingMessageText, "");

        } catch (MessageParseException e) {
            Log.error(this, e + ": " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * send the answer to the MiddleWare
     *
     * @param answer message to send back
     * @throws Exception if there is a sending problem
     */
    private void sendAnswer(final JMSQueueMessage answer) throws Exception {

        if (Log.isDebug()) {
            Log.debug(this, answer.getText().toString());
        }

        final IEAdapter sender = getIEAdapter().getIEAdapterConfig().getSenderIEAdapter();
        if (sender == null) {
            Log.error(this, "Sender is null. Answer = " + answer.toString());
            return;
        }
        if (sender instanceof SantanderIEAdapter) {

            ((SantanderIEAdapter) sender).write(answer);

        } else {
            writeMessage(getIEAdapter(), answer.toString());
        }
    }

    // not used
    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
        // Auto-generated method stub
        return false;
    }

    // not used
    @Override
    public String handleOutgoingMessage(PSEvent event, List<Task> tasks) throws Exception {
        // Auto-generated method stub
        return null;
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

    /**
     * Rollbacks msg when crash
     */
    private void rollbackMsg() {
        if (isRollbackEnabled) {
            try {
                this.getIEAdapter().rollback();
            } catch (ConnectException ex) {
                Log.error(this.getEngineName(), "Error during queue session rollback() ", ex);
            }
        }
    }

    private boolean isRollbackEnabled() {
        return SantDomainValuesUtil.getBooleanDV(ROLLBACK_DV);
    }
}

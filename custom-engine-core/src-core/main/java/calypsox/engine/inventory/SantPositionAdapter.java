/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory;

import calypsox.engine.inventory.util.PositionLogHelper;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.FileIEAdapter;
import calypsox.tk.util.FileIEAdapterConfig;
import calypsox.tk.util.SantanderJMSQueueIEAdapterConfig;
import calypsox.tk.util.gdisponible.GDisponibleUtil;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;
import com.calypso.tk.util.IEAdapterListener;

import java.nio.charset.Charset;
import java.util.List;

import static calypsox.engine.inventory.SantPositionConstants.LINE_SEPARATOR;

/**
 * Positions from Gestion Disponible will be imported into Calypso by the mean of this Engine, called
 * SantUpdatePositionEngine
 *
 * @author Patrice Guerrido & Guillermo Solano
 * @version 1.2, added adapter interface to the publisher and check incoming message
 */
public class SantPositionAdapter implements IEAdapterListener {

    // constant to define the queue type name
    // required to load the properties field
    /**
     * queue name propertie
     */
    public final static String QUEUE_TYPE = "gdisponible";
    /**
     * operation mode propertie
     */
    public final static String INPUT_QUEUE_OPMODE = "in.jms.opmode";

    /**
     * Configuration adapter
     */
    private IEAdapterConfig config;

    public IEAdapter getAdapter() {
        return this.adapter;
    }

    public void setAdapter(IEAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Queue adapter (interacts the queue)
     */
    private IEAdapter adapter;

    /**
     * Process the message
     */
    private SantIncomingMsgProcessor processor;

    /**
     * Logs the message (parser errors and positions trade creation and DB update)
     */
    private SantPositionLog log = new SantPositionLog();

    /**
     * Message reference use for internally build message
     **/
    public String messageReference = null;

    public String getMessageReference() {
        return this.messageReference;
    }

    /**
     * Starts this Adapter
     *
     * @param testing , true to activate test mode
     * @throws ConnectException
     */
    public void start(boolean testing) throws ConnectException {

        this.log = new SantPositionLog();

        if (testing) { // test mode: reads from a file
            initFileAdapterConfig();
        } else {
            initJMSQueueAdapterConfig();
        }
        this.processor = new SantIncomingMsgProcessor(testing);
    }

    /**
     * @param String  with the message to handle
     * @param adapter of the handler
     * @return true in the positions received have been processed correctly
     */
    @Override
    public boolean newMessage(final IEAdapter adapter, final String message) {

        // FIX BAU for "&" Character
        Charset.forName("UTF-8").encode(message);

        // log message reception - new online Position
        Log.info(SantIncomingMsgProcessor.class, " A. Incoming message = " + message);
        // start time, to recover total process time required
        final long start = System.currentTimeMillis();

        // process the incoming message
        final List<PositionLogHelper> messLogTrack = this.processor.handleIncomingMessage(message.trim());

        // generate the response for this message
        final String responseMessage = this.processor.buildResponseMessage(messLogTrack);

        // log the processing positions from this message
        this.log.appendProcessMessageLog(messLogTrack);

        // send to GD the anwer: NACK=resend, ACK=no_resend
        // do not send answer for internally built messages
        if (!GDisponibleUtil.SANT_GD_MATURE_SEC_POS_REFERENCE.equals(getMessageReference())) {
            this.adapter.write(responseMessage);
        }

        // Log the message response
        Log.info(this, "B. Message output:" + responseMessage + "\n");
        // capture end time
        final long end = System.currentTimeMillis();
        Log.info(SantIncomingMsgProcessor.class, "C. Time to Process in ms = " + Long.toString(end - start));

        return true;

    }

    /**
     * Initialization of the adapter using the adapterConfig.
     *
     * @throws ConnectException
     */
    private void initJMSQueueAdapterConfig() throws ConnectException {

        if (this.config == null) {
            this.config = new SantanderJMSQueueIEAdapterConfig(QUEUE_TYPE);
        }

        if (!this.config.isConfigured()) {
            Log.error(this, "JMS Queue not configured properly - check property file");
            throw new ConnectException("JMS Queue not configured properly - check property file");
        }

        this.adapter = this.config.getReceiverIEAdapter();
        if (this.adapter == null) {
            throw new ConnectException("No incoming adapter configured");
        }

        // GSM: the adapter was missing the configuration
        this.adapter.setIEAdapterConfig(this.config);

        // add the adapter interface to the publisher
        this.adapter.setListener(this);

        this.adapter.init();
        this.config.checkTimer();

    }

    // for testing purposes, can use a file for uploading positions
    private void initFileAdapterConfig() throws ConnectException {
        if (this.config == null) {
            this.config = new FileIEAdapterConfig() {

                @Override
                public String getConfigFileName() {
                    return "updatepositionfileadapter_config.properties";
                }
            };
        }
        if (!this.config.isConfigured()) {
            Log.error(this, "Incorrect File Adapter Config - check property file");
            throw new ConnectException("Incorrect File Adapter Config - check property file");
        }
        this.adapter = this.config.getReceiverIEAdapter();
        if (this.adapter == null) {
            throw new ConnectException("No incoming adapter configured");
        }

        this.adapter.setListener(this);
        ((FileIEAdapter) this.adapter).init();
    }

    /**
     * @param ExternalMessage to handle
     * @param adapter         of the handler
     * @return true in the positions received have been processed correctly
     */
    @Override
    public boolean newMessage(final IEAdapter adapter, final ExternalMessage externalMessage) {

        if (externalMessage == null) {
            return false;
        }
        // get the content of the message
        final JMSQueueMessage jmsQueueMessage = (JMSQueueMessage) externalMessage;
        final String message = jmsQueueMessage.getText();

        // init current reference
        this.messageReference = jmsQueueMessage.getReference();
        if ((this.processor != null) && !Util.isEmpty(this.messageReference)) {
            this.processor.setMessageReference(this.messageReference);
        }

        // check if message is empty
        if (Util.isEmpty(message)) {
            return true;
        }

        // check the message format is correct
        if (!correctMessageContent(message)) {
            return false;
        }

        if (Log.isDebug()) {
            Log.debug(this, message);
        }

        return newMessage(adapter, message);
    }

    /**
     * Verify that it has 10 fields, as defined on the DDS
     *
     * @param message to check
     * @return true if it has 10 fields as expected
     */
    private boolean correctMessageContent(final String message) {

        // check message received
        if ((message == null) || message.isEmpty()) {
            Log.warn(this, "Message input empty, processing discarded");
            return false;
        }

        // separate rows
        final String[] rows = message.split(LINE_SEPARATOR);

        if (rows.length < 1) {
            Log.warn(this, "Message input, no incoming positions lines/rows: " + message);
            return false;
        }

        return true;
    }

    // not used
    @Override
    public void onDisconnect(final IEAdapter adapter) {
        // unused
    }

}

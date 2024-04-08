/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.SantanderMQIEAdapterConfig.PROPERTY;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterConfig;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * SantanderMQIEAdapter implements a IEAdapter to read MQ queues. Using the
 * configuration given by IEAdapterConfig, gives the methods necessary methods
 * to read, write and play around with the configured queue.
 *
 * @author Guillermo Solano
 * @version 1.0
 * @date 12/12/2013
 */
@Stateless
public class SantanderMQIEAdapter extends SantantaderQueueIEAdapter
        implements MessageListener, ExceptionListener {

    /**
     * MQ initial Context
     */
    private Context initialContext;
    /**
     * Connection Factory class
     */
    @Resource(mappedName = "java:/calypso/jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;
    /**
     * Queue Connection
     */
    private QueueConnection queueConnection;
    /**
     * Session
     */
    private QueueSession queueSession;
    /**
     * Input queue.
     */
    private Queue inputQueue;
    /**
     * Output queue.
     */
    private Queue outputQueue;
    /**
     * Incoming messages queue. op.mode = 1 or 2
     */
    private QueueReceiver queueReceiver;
    /**
     * Output messages queue. op.mode = 0 or 2
     */
    private QueueSender queueSender;

    /**
     * Sender configuration code.
     */
    public static final String SENDER = "0";

    /**
     * Receiver configuration code.
     */
    public static final String RECEIVER = "1";

    /**
     * Sender and receiver configuration code.
     */
    public static final String SENDER_RECEIVER = "2";

    /**
     * Emtpy Constructor
     */
    public SantanderMQIEAdapter() {
    }

    /**
     * SantQueuerJMSIEAdapter Constructor
     *
     * @param santanderMQIEAdapterConfig
     */
    public SantanderMQIEAdapter(SantanderMQIEAdapterConfig santanderMQIEAdapterConfig) {

        super.setIEAdapterConfig(santanderMQIEAdapterConfig);
    }

    /**
     * starts the connection
     *
     * @throws ConnectException
     */
    @Override
    public void init() throws ConnectException {

        if (super.getIEAdapterConfig() == null) {
            throw new ConnectException("IEAdapterConfig not found. Please check it is linked to this IEAdapter");
        }
        // starts the adapter
        initIEAdapter();
    }

    /**
     * @param config to set to the adapter interface
     */
    @Override
    public void setIEAdapterConfig(final IEAdapterConfig config) {
        super.setIEAdapterConfig(config);

    }

    /**
     * Inits the adapter
     */
    protected void initIEAdapter() {

        createInitialContext();
        createConnectionFactory();
        createQueueConnection();

        try {
            if (SENDER.equalsIgnoreCase(getOpMode())) {
                initSender();
            } else if (RECEIVER.equalsIgnoreCase(getOpMode())) {
                initReceiver();
            } else if (SENDER_RECEIVER.equalsIgnoreCase(getOpMode())) {
                initSenderReceiver();
            }
        } catch (final ConnectException e) {
            Log.error(this, "ConnectException in initIEAdapter():" + e);
        }

    }

    /**
     * Sets the initial context and url properties and runs the initial Context
     */
    protected void createInitialContext() {
        final Hashtable<String, String> props = new Hashtable<>();

        props.put(Context.INITIAL_CONTEXT_FACTORY, getModeTypeClass());
        props.put(Context.PROVIDER_URL, getUrl());

        try {
            this.initialContext = new InitialContext(props);

        } catch (final NamingException e) {
            Log.error(this, "NamingException in createInitialContext():" + e);
        }
    }

    /**
     * Looks in the JDNI the factory connection queue
     */
    protected void createConnectionFactory() {

        try {

            this.connectionFactory = (QueueConnectionFactory) this.initialContext.lookup(getFactoryName());
            Log.info(this, "Connected to ConnectionFactory " + getFactoryName());
        } catch (final NamingException e) {
            Log.error(this, "NamingException in createConnectionFactory():" + e);
        }
    }

    /**
     * gets the queue from the factory pool
     */
    protected void createQueueConnection() {

        try {

            this.queueConnection = ((QueueConnectionFactory) this.connectionFactory).createQueueConnection();
        } catch (final JMSException e) {
            Log.error(this, "JMSException in createQueueConnection():" + e);
        }
    }

    /**
     * Initialize Receiver
     *
     * @throws ConnectException
     */
    protected void initReceiver() throws ConnectException {

        // *******************************************************
        // * Create sessions, for sending and receiving messages
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        locateInputQueue();

        // *******************************************************
        // * Create MessageConsumers to receive messages
        // *******************************************************
        createQueueReceiver();

        // *******************************************************
        // * Attachs the message and Exception Listener
        // *******************************************************
        initReceiverQueueListener();

        // *******************************************************
        // * Finally, starts the connection
        // *******************************************************
        startConnection();
    }

    /**
     * Attachs the message and Exception Listener to the incoming queue
     *
     * @throws ConnectException
     */
    public void initReceiverQueueListener() throws ConnectException {

        if (this.queueReceiver == null) {
            throw new ConnectException("Failed to set the message Listener: queueReceiver is null");
        }

        if (this.queueConnection == null) {
            throw new ConnectException("Failed to set the message Listener: queueConnection is null");
        }

        try {
            this.queueReceiver.setMessageListener(this);
            this.queueConnection.setExceptionListener(this);
        } catch (final JMSException e) {
            throw new ConnectException("Failed to set the message Listener: " + e.getMessage());
        } // end of try-catch
    }

    /**
     * Initialize Sender
     *
     * @throws ConnectException
     */
    protected void initSender() throws ConnectException {

        startConnection();
        // *******************************************************
        // * Create sessions, for sending and receiving messages
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        locateOutputQueue();

        // *******************************************************
        // * Create MessageProducer to send messages
        // *******************************************************
        createQueueSender();
    }

    /**
     * Initialize Receiver & Sender MQ queues
     *
     * @throws ConnectException
     */
    protected void initSenderReceiver() throws ConnectException {
        startConnection();
        createSession();
        locateOutputQueue();
        createQueueSender();
        locateInputQueue();
        createQueueReceiver();
        initReceiverQueueListener();
    }

    /**
     * During initialization steps, message flow is inhibited to prevent
     * unpredictable behavior during initialization. Once initialization is
     * complete, the connection must be told to initiate the message flow.
     */
    protected void startConnection() throws ConnectException {
        try {
            this.queueConnection.start();
            Log.info(this, "Connection to Queue established!");
        } catch (final JMSException e) {
            throw new ConnectException("Unable to start Connection (" + e.getMessage() + ")");
        }
    }

    /**
     * Sessions are lightweight JMS objects which provide a context for
     * producing and consuming messages. Sessions are used to build message
     * producers and message consumers, as well as to build the messages
     * themselves.
     */
    protected void createSession() throws ConnectException {

        try {
            this.queueSession = this.queueConnection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
        } catch (final JMSException e) {
            throw new ConnectException("Unable to create Session (" + e.getMessage() + ")");
        }
    }

    /**
     * A queue is located through JNDI. The queue identifies the messages being
     * sent or received.
     */
    protected void locateInputQueue() throws ConnectException {

        final String inputQueueName = getInputQueueName();
        try {
            this.inputQueue = (Queue) this.initialContext.lookup(inputQueueName);
        } catch (final Exception e) {
            Log.warn(this, "Not possible to bind input queue MQ " + inputQueueName + ". Lets try to ask session .\n"
                    + e.getLocalizedMessage());
        }

        if (this.inputQueue == null) {
            try {
                this.inputQueue = this.queueSession.createQueue(inputQueueName);
            } catch (final JMSException e) {
                throw new ConnectException("Unable to create Queue (" + e.getMessage() + ")");
            }
        }
        Log.info(this, "Queue " + inputQueueName + " ready");
    }

    /**
     * Locates the output queue
     *
     * @throws ConnectException
     */
    protected void locateOutputQueue() throws ConnectException {

        final String outputQueueName = getOutputQueueName();
        try {
            this.outputQueue = (Queue) this.initialContext.lookup(outputQueueName);
        } catch (final Exception e) {
            Log.warn(this, "Not possible to bind input output MQ " + outputQueueName + ". Lets try to ask session .\n"
                    + e.getLocalizedMessage());
        }

        if (this.outputQueue == null) {
            try {
                this.outputQueue = this.queueSession.createQueue(outputQueueName);
            } catch (final JMSException e) {
                throw new ConnectException("Unable to create Queue (" + e.getMessage() + ")");
            }
        }
        Log.info(this, "Queue " + outputQueueName + " ready");
    }

    /**
     * Create a queue receiver. In the queueing domain, a receiver receives
     * messages from a given queue. This code shows the creation of a receiver.
     */
    protected void createQueueReceiver() throws ConnectException {
        try {
            this.queueReceiver = this.queueSession.createReceiver(this.inputQueue);
        } catch (final JMSException e) {
            throw new ConnectException("Unable to create receiver (" + e.getMessage() + ")");
        }
    }

    /**
     * Create a queue sender. In the queueing domain, a sender sends messages to
     * a given queue. This code shows the creation of a sender.
     */
    protected void createQueueSender() throws ConnectException {
        try {
            this.queueSender = this.queueSession.createSender(this.outputQueue);
        } catch (final JMSException e) {
            throw new ConnectException("unable to create sender (" + e.getMessage() + ")");
        } // end of try-catch
    }

    // 5º Juan haz...
    public boolean write(final String paramString, String IDCorrelation) {

        boolean rst = false;

        try {
            this.queueSession = this.queueConnection.createQueueSession(isTransacted(), Session.AUTO_ACKNOWLEDGE);
            this.queueSender = this.queueSession.createSender(this.outputQueue);
            final TextMessage msg = this.queueSession.createTextMessage();
            // JAT 09/05/14 set the correlation id. Incidence with Integration
            msg.setJMSCorrelationID(IDCorrelation);

            msg.setText(paramString);

            Log.info(this, "MQ QUEUE: Sending msg Text:" + paramString);
            // GSM 22/01/2014: added log in case we cannot write into the queue
            if (this.queueSender != null) {

                this.queueSender.send(msg);
                Log.info(this, "MQ QUEUE: Sent msg Text:" + paramString);

            } else {
                Log.error(this, "ERR: Not possible to write into the queue: " + msg
                        + "\n Check queue properties configuration");
            }

            this.queueSession.close();
            rst = true;
        } catch (final Exception e) {
            Log.error(this, "Sending msg Text:" + paramString, e);
            return false;
        }

        return rst;

    }

    /**
     * @param String message to write into the queue (if is a two-ways queue)
     * @return true is sended
     */
    @Override
    public boolean write(final String paramString) {

        boolean rst = false;

        try {
            this.queueSession = this.queueConnection.createQueueSession(isTransacted(), Session.AUTO_ACKNOWLEDGE);
            this.queueSender = this.queueSession.createSender(this.outputQueue);
            final TextMessage msg = this.queueSession.createTextMessage();

            msg.setText(paramString);

            Log.info(this, "MQ QUEUE: Sending msg Text:" + paramString);
            // GSM 22/01/2014: added log in case we cannot write into the queue
            if (this.queueSender != null) {

                this.queueSender.send(msg);
                Log.info(this, "MQ QUEUE: Sent msg Text:" + paramString);

            } else {
                Log.error(this, "ERR: Not possible to write into the queue: " + msg
                        + "\n Check queue properties configuration");
            }

            this.queueSession.close();
            rst = true;
        } catch (final Exception e) {
            Log.error(this, "Sending msg Text:" + paramString, e);
            return false;
        }

        return rst;
    }

    /**
     * @param String message to write into the queue (if is a two-ways queue)
     * @return true is sended
     */
    @Override
    public boolean write(final JMSQueueMessage message) {

        return this.write(message.getText());
    }

    /**
     * Captures exceptions on the incoming queue
     */
    @Override
    public void onException(final JMSException jmsexception) {

        Log.error(this, "Queue connection exception occurred: " + jmsexception.getLocalizedMessage());
    }

    /**
     * This is called by the onMessageListener of the receive queue. When a
     * message has arrived, this method will call all the suscriber listeners.
     */
    // Juan, 1º a retocar
    @SuppressWarnings("deprecation")
    @Override
    public void onMessage(final Message mess) {

        boolean proc = false;
        // check DS is online

        if (DSConnection.getDefault().isClosed()) {
            Log.fatal(this, "DSConnection is closed, stopping engine.");
            System.exit(-2);
        }

        JMSQueueMessage jmsMessage;
        try {
            // 2 JAT: en el build pasale el correlation id
            jmsMessage = buildJMSMessage(mess);

        } catch (JMSException e) {
            Log.error(this, "Message could not be build. It wont be processed");
            Log.error(this, e.getLocalizedMessage());
            return;
        }

        // Commit/rollback will be done inside this method, in the engine
        proc = callBackListener(jmsMessage);

        if (!proc) {
            Log.error(this, "Income Msg Not Processed.");
            return;
        }
        return;
    }

    /**
     * Transforms the incoming message into a JMSQueueMessage type
     *
     * @param mess received
     * @return a valid JMSQueueMessage with the text
     * @throws JMSException if it wasn't recognized the message
     */
    private JMSQueueMessage buildJMSMessage(final Message mess) throws JMSException {

        final JMSQueueMessage jmsMessage = new JMSQueueMessage();

        if (mess instanceof TextMessage) {

            final TextMessage msg = (TextMessage) mess;
            jmsMessage.setReference(mess.getJMSMessageID());
            jmsMessage.setText(msg.getText());
            jmsMessage.setCorrelationId(mess.getJMSCorrelationID());

        } else if (mess instanceof ExternalMessage) {

            jmsMessage.setText(((ExternalMessage) mess).getText());
            jmsMessage.setCorrelationId(mess.getJMSCorrelationID());
        } else {
            throw new JMSException("Message type not recognized!");
        }

        return jmsMessage;

    }

    /**
     * Is never used.
     *
     * @throws ConnectException ... never.
     */
    @Override
    public void reinit() throws ConnectException {
        return;
    }

    /**
     * @return factory name
     */
    private String getFactoryName() {

        return super.getProperty(PROPERTY.FACTORY_NAME.getName());
    }

    /**
     * @return MQ url
     */
    private String getUrl() {

        return super.getProperty(PROPERTY.URL.getName());
    }

    /**
     * @return Mode Type
     */
    private String getModeTypeClass() {
        return super.getProperty(PROPERTY.MODE_TYPE_CLASS.getName());
    }

    /**
     * @return operational mode
     */
    private String getOpMode() {

        return super.getProperty(PROPERTY.OP_MODE.getName());
    }

    /**
     * @return input queue name
     */
    private String getInputQueueName() {

        return super.getProperty(PROPERTY.INPUT_QUEUE_NAME.getName());
    }

    /**
     * @return output queue name
     */
    private String getOutputQueueName() {

        return super.getProperty(PROPERTY.OUTPUT_QUEUE_NAME.getName());
    }

    /**
     * @return is transactional
     */
    private boolean isTransacted() {

        final String trans = super.getProperty(PROPERTY.TRANSACTED.getName());
        return trans != null ? trans.trim().equalsIgnoreCase("true") || trans.trim().equalsIgnoreCase("1") : false;
    }

    /**
     * @return is the queue is transacted. Note, if true, the service will have
     * to call commit every transaction
     * @throws ConnectException ... never.
     */
    @Override
    public boolean getTransactionEnabled() {
        return isTransacted();
    }

    /**
     * Returns the session used.
     *
     * @return session used.
     */
    @Override
    public Session getSession() {
        return this.queueSession;
    }

    /**
     * Commit the session. Will delete all processed messages.
     *
     * @throws ConnectException if problems found.
     */
    @Override
    public void commit() throws ConnectException {
        try {
            this.queueSession.commit();
        } catch (final JMSException e) {
            Log.error(this, "JMSException in commit():" + e);
        }
    }

    /**
     * closes the connection. Should not be used.
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {

        this.queueConnection.close();
        this.queueSession.close();
    }

    /**
     * Rollsback the connection in transactional connections
     *
     * @throws ConnectException ... never.
     */
    @Override
    public void rollback() throws ConnectException {
        try {
            this.queueSession.rollback();

        } catch (JMSException e) {

            throw new ConnectException(e.getMessage());
        }

    }

    // For junit test purpose
    public void setInitialContext(final Context context) {
        this.initialContext = context;
        throw new UnsupportedOperationException("This method should be used just for testing purposes!");
    }

    public void setConnectionFactory(final QueueConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        throw new UnsupportedOperationException("This method should be used just for testing purposes!");
    }

    public void setQueueConnection(final QueueConnection queueConnection) {
        this.queueConnection = queueConnection;
        throw new UnsupportedOperationException("This method should be used just for testing purposes!");
    }

    public void setQueueSession(final QueueSession queueSession) {
        this.queueSession = queueSession;
        throw new UnsupportedOperationException("This method should be used just for testing purposes!");
    }

    public void setQueueSender(final QueueSender queueSender) {
        this.queueSender = queueSender;
        throw new UnsupportedOperationException("This method should be used just for testing purposes!");
    }

    @Override
    public boolean write(String message, BOMessage boMessage) {
        return false;
    }

    @Override
    public boolean createQueueDynamicallyAndWrite(JMSQueueMessage message) {
        return false;
    }

    // end of try-catch

} // end class

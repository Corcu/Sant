/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */

package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Properties;

/**
 * This class contains all the functionality required to manage a sending or
 * receiving a JMS queue. It will do all the set required. It expects the
 * setQueueName and setProperties() method to be called, before initialization,
 * with the parameters required to set
 * it up.
 *
 * @since 5.0, 10/12/01
 */
public class GenericJMSQueueIEAdapter extends SantanderIEAdapter implements MessageListener, ExceptionListener {

    public static final String TRADE_ID = "TradeId";
    public static final String MSG_ID = "MsgId";

    public static final String INPUT_QUEUE_NAME = "input.queue.name";
    public static final String OUTPUT_QUEUE_NAME = "output.queue.name";
    public static final String ENGINE_NAME_PRO = "ENGINENAME";

    static public int RETRY = 10;
    final static public int TIMEOUT = 5 * 1000; // 5 secs
    static public final int RECEIVER = 1;
    static public final int SENDER = 0;
    // *********************************
    // * JMS Queue Connectivity
    // *********************************
    protected Connection _queueConnection = null;
    protected Session _session = null;
    protected Context _context = null;
    // protected Queue _queue = null;

    protected String _receiverQueueName = null;
    protected String _senderQueueName = null;
    protected Queue _receiverQueue = null;
    protected Queue _senderQueue = null;


    protected MessageConsumer _receiver = null;
    protected javax.jms.MessageProducer _sender = null;

    protected String _queueName = null;
    protected String _factoryName = null;
    protected String _connectionUserName = null;
    protected String _connectionPassword = null;
    protected String _jmsUrl = null;
    protected String _modeTypeClass = null;

    // *********************************
    // Messaging
    // *********************************
    protected int _opMode = RECEIVER;
    protected int _ackType = Session.AUTO_ACKNOWLEDGE;
    protected boolean _transacted = false;
    protected int _persistency = DeliveryMode.NON_PERSISTENT;
    protected int _backoutThreshold = 0;

    //
    private static final String CONFIG = "config";
    private static final String JMS = "JMS";
    private static final String TIBCO = "Tibco";


    /**
     * Set whether this is a sender or receiver to a queue.
     *
     * @param opMode value that identified the kind of the queue (sender of receiver)
     */
    public GenericJMSQueueIEAdapter(final int opMode) {
        this._opMode = opMode;
    }

    @Override
    public void commit() throws ConnectException {
        try {
            this._session.commit();
        } catch (final JMSException e) {
            Log.error("JMS", "Commit failed", e);
            throw new ConnectException("Commit failed" + e.getMessage());
        }
    }

    protected void configureFactory(final QueueConnectionFactory factory) {
        // To be overridden
    }

    /**
     * A Connection provides the client access to the underlying messaging
     * system, and performs resource allocation and management. Connections are
     * created using a ConnectionFactory, which is typically located using JNDI.
     */
    private void createProviderConnection(boolean JNDILookup) throws ConnectException {
        try {
            final ConnectionFactory factory;
            if (JNDILookup && this._context != null) {
                factory = (QueueConnectionFactory) this._context.lookup(this._factoryName);
            } else {
                factory = new com.tibco.tibjms.TibjmsConnectionFactory(this._jmsUrl);
            }
            //factory = new com.tibco.tibjms.TibjmsQueueConnectionFactory(this._jmsUrl);

            if ((getConnectionUserName() == null) && (getConnectionPassword() == null)) {

                this._queueConnection = factory.createConnection();
            } else {
                this._queueConnection = factory.createConnection(getConnectionUserName(), getConnectionPassword());
            }
        } catch (final JMSException e) {
            Log.error("JMS", e);
            throw new ConnectException("Failed to Create Queue Connection: " + e.getMessage());
        } catch (final NamingException e) {
            Log.error("JMS", e);
            if (JNDILookup) {
                createProviderConnection(false);
            } else
                throw new ConnectException("Failed to locate connection factory " + this._factoryName);
        } catch (final Exception e) {
            Log.error("JMS", e);
            throw new ConnectException("Other Excpetion");
        } // end of try-catch

    }

    /**
     * Create a queue receiver. In the queueing domain, a receiver receives
     * messages from a given queue. This code shows the creation of a receiver.
     */
    private void createQueueReceiver() throws ConnectException {
        try {
            this._receiver = this._session.createConsumer(this._receiverQueue);
        } catch (final JMSException e) {
            Log.error(this, e); //sonar 03/11/2017
            throw new ConnectException("Unable to create receiver (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Create a queue sender. In the queueing domain, a sender sends messages to
     * a given queue. This code shows the creation of a sender.
     */
    private void createQueueSender() throws ConnectException {
        try {
            this._sender = this._session.createProducer(this._senderQueue);
        } catch (final JMSException e) {
            Log.error(this, e); //sonar 03/11/2017
            throw new ConnectException("unable to create sender (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Sessions are lightweight JMS objects which provide a context for
     * producing and consuming messages. Sessions are used to build message
     * producers and message consumers, as well as to build the messages
     * themselves.
     */
    private void createSession() throws ConnectException {
        try {
            this._session = this._queueConnection.createSession(this._transacted, this._ackType);
        } catch (final JMSException e) {
            Log.error(this, e); //sonar 03/11/2017
            throw new ConnectException("Unable to create Session (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Get the type of acknowledgment
     *
     * @return int ackType = [Session.AUTO_ACKNOWLEDGE |
     * Session.DUPS_OK_ACKNOWLEDGE | Session.CLIENT_ACKNOWLEDGE]
     */
    public int getAckType() {
        return this._ackType;
    }

    public int getBackoutThreshold() {
        return this._backoutThreshold;
    }

    /**
     * Get the connection factory name.
     */
    public String getConnectionFactory() {
        return this._factoryName;
    }

    /**
     * Get the password used when connecting to the queue
     */
    public String getConnectionPassword() {
        return this._connectionPassword;
    }

    /**
     * Get the user name used when connecting to the queue
     */
    public String getConnectionUserName() {
        return this._connectionUserName;
    }

    /**
     * Get the modus operandus of the JNDI server be that RMI or IPC
     */
    public String getModeType() {
        return this._modeTypeClass;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC
     */
    public void getModeType(final String mode) {
        this._modeTypeClass = mode;
    }

    /**
     * Get the name of the JMS queue to subscribe to
     */
    public String getQueueName() {
        return this._queueName;
    }

    /**
     * Return a queue receiver.
     */
    public MessageConsumer getQueueReceiver() {
        return this._receiver;
    }

    /**
     * Return a queue sender.
     */
    public javax.jms.MessageProducer getQueueSender() {
        return this._sender;
    }

    /**
     * Return the session
     */
    public Session getQueueSession() {
        return this._session;
    }

    /**
     * return the sender persistancy
     */
    public int getSenderPersisted() {
        return this._persistency;
    }

    @Override
    public boolean getTransactionEnabled() {
        return this._transacted;
    }

    /**
     * Get the URL to the JMS server
     */
    public String getUrl() {
        return this._jmsUrl;
    }

    @Override
    public void init() throws ConnectException {
        initProperties();
        init(false);
    }

    private static void setRETRY(int value) {
        RETRY = value;
    }

    /**
     * Connect to the JNDI Server to get a reference to the administered
     * objects. and start the ball rolling.
     */
    public void init(final boolean wait) throws ConnectException {
        boolean done = false;
        stop(); //***
        ConnectException error = new ConnectException("init IE Adapter failed");
        final String sdef = Defaults.getProperty(DSConnection.MAX_RETRY_CONNECT);
        Properties pro = this.getIEAdapterConfig().getProperties();
        if (sdef != null) {
            try {
                GenericJMSQueueIEAdapter.setRETRY(Integer.parseInt(sdef));
            } catch (final Exception exxx) {
                Log.error(this, exxx);
            }
            if (RETRY < 1) {
                GenericJMSQueueIEAdapter.setRETRY(1);
            }
        }
        final int retry = wait ? RETRY : 1;
        for (int i = 0; i < retry; i++) {
            try {
                final Hashtable<String, String> props = new Hashtable<String, String>();
                if (this._jmsUrl != null) {
                    props.put(Context.INITIAL_CONTEXT_FACTORY, this._modeTypeClass);
                    props.put(Context.PROVIDER_URL, this._jmsUrl);
                }

                String type = pro.getProperty(CONFIG);
                if (type.equalsIgnoreCase(TIBCO)) {
                    createProviderConnection(false);
                } else if (type.equalsIgnoreCase(JMS)) {
                    this._context = new InitialContext(props);

                    createProviderConnection(true);
                } else {
                    Log.error(this, "Error. Incorrect type of config, failed to create provider connection.");
                    throw new ConnectException("Error. Incorrect type of config, failed to create provider connection.");
                }

                // Hay que mirar el caso BOTH
                if ((this._opMode == SantanderIEAdapterConfig.SENDER)
                        || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                    initSender();
                }

                if ((this._opMode == SantanderIEAdapterConfig.RECEIVER)
                        || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                    initReceiver();
                }
                done = true;
            } catch (final ConnectException e) {
                Log.error("START", e);
                error = e;
            } catch (final Exception e) {
                Log.error("START", e);
                error = new ConnectException("Connection to Message Adapter failed (" + e.getMessage() + ")");
            } // end of try-catch
            if (done == false) {
                Log.info("JMS", "Could not connect to Message Adapter, Retrying " + (i + 1));
                try {
                    // Thread.currentThread().sleep(TIMEOUT);
                    Thread.sleep(TIMEOUT);
                } catch (final Exception ee) {
                    Log.error(this, ee); //sonar 03/11/2017
                }
            } else {
                break;
            }
        }
        if (done == false) {
            throw error;
        }
        this._isOnline = true;
        Log.info("JMS", "Message Adapter Started ...");
    }

    /**
     * Set the properties for the Sender/Receiver jms.queue.connectionFactory =
     * JMS topic connection factory class name jms.queue.connectionUserName =
     * JMS topic connection user name jms.queue.connectionPassword = JMS topic
     * connection password jms.url = URL to JMS service jms.modetypeclass =
     * initial context class name <queueName>.queue.transacted = [true | false]
     * to transact messages <queueName>.queue.persist = [true | false] to
     * persist messages <queueName>.queue.ackType = [auto | dups | client]
     * <queueName>.queue.backoutThreshold
     *
     * @param Properties for the adapter
     */
    protected void initProperties() {
        String propVal = null;

        if ((propVal = getProperty("jms.queue.connectionFactory")) != null) {
            setConnectionFactory(propVal);
        }

        if ((propVal = getProperty("jms.queue.connectionUserName")) != null) {
            setConnectionUserName(propVal);
        }

        if ((propVal = getProperty("jms.queue.connectionPassword")) != null) {
            setConnectionPassword(propVal);
        }

        if ((propVal = getProperty("jms.url")) != null) {
            setUrl(propVal);
        }

        if ((propVal = getProperty("jms.modetypeclass")) != null) {
            setModeType(propVal);
        }

        if ((this._opMode == SantanderIEAdapterConfig.RECEIVER) || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
            initProperties(SantanderIEAdapterConfig.RECEIVER);
        }

        if ((this._opMode == SantanderIEAdapterConfig.SENDER) || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
            initProperties(SantanderIEAdapterConfig.SENDER);
        }

        Log.info("TEMPO",
                " queueName = " + getQueueName() + " connectionFactory = " + getConnectionFactory()
                        + " connectionUserName = " + getConnectionUserName() + " jmsUrl = " + getUrl() + " modeType = "
                        + getModeType() + " ackType " + getAckType() + " transacted " + getSenderPersisted()
                        + " persisted = " + getTransactionEnabled() + " backoutThreshold " + getBackoutThreshold());
    }

    protected void initProperties(int mode) {
        String queueName = null;

        if (mode == SantanderIEAdapterConfig.RECEIVER) {
            queueName = getProperty("input.queue.name");
            setReceiverQueueName(queueName);
        } else if (mode == SantanderIEAdapterConfig.SENDER) {
            queueName = getProperty("output.queue.name");
            setSenderQueueName(queueName);
        } else {
            return;
        }
        String propVal = null;
        if ((propVal = getProperty(queueName + ".queue.ackType")) != null) {
            setAckType(propVal);
        }

        if ((propVal = getProperty(queueName + ".queue.persist")) != null) {
            setSenderPersisted(propVal);
        }

        if ((propVal = getProperty(queueName + ".queue.transacted")) != null) {
            setTransactionEnabled(propVal);
        }

        if ((propVal = getProperty(queueName + ".queue.backoutThreshold")) != null) {
            setBackoutThreshold(propVal);
        }
    }

    /**
     * Initialize the adapter as a queue receiver
     */
    private void initReceiver() throws ConnectException {

        // *******************************************************
        // * Create sessions, for sending and receiving messages
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        this._receiverQueue = locateQueue(this._receiverQueue, getReceiverQueueName());

        // *******************************************************
        // * Create MessageConsumers to receive messages
        // *******************************************************
        createQueueReceiver();

        try {
            this._receiver.setMessageListener(this);
            this._queueConnection.setExceptionListener(this);
        } catch (final JMSException e) {
            Log.error(this, e); //sonar 03/11/2017
            throw new ConnectException("Failed to set the message Listener: " + e.getMessage());
        } // end of try-catch

        startConnection();
    }

    /**
     * Initialize the adapter as a queue sender
     */
    private void initSender() throws ConnectException {
        startConnection();
        // *******************************************************
        // * Create sessions, for sending and receiving messages
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        this._senderQueue = locateQueue(this._senderQueue, getSenderQueueName());

        // *******************************************************
        // * Create MessageProducer to send messages
        // *******************************************************
        createQueueSender();
    }

    @Override
    public boolean isOnline() {
        if (this._isOnline) {
            return this._isOnline;
        }
        try {
            init(false);
        } catch (final Exception e) {
            Log.error("GSCCSender", " unable to re Init Adapter ", e);
        } // end of try-catch
        return this._isOnline;
    }

    /**
     * A queue is located through JNDI. The queue identifies the messages being
     * sent or received.
     */
    private Queue locateQueue(Queue queue, String queueName) throws ConnectException {
        if (queue != null) {
            return queue;
        }
        try {
            if (this._context != null) {
                queue = (Queue) this._context.lookup(queueName);
            }
        } catch (final Exception e) {
            Log.error(this, e); //sonar 03/11/2017
        }
        if (queue == null && this._session != null) {
            try {
                queue = this._session.createQueue(queueName);
            } catch (final JMSException e) {
                Log.error(this, e); //sonar 03/11/2017
                throw new ConnectException("Unable to create Queue (" + e.getMessage() + ")");
            } // end of try-catch
        }
        Log.info(this, "Queue " + queueName + " ready");
        return queue;

    }

    public void messageConnectionClosed() {
        Log.debug(Log.START, "Connection Closed with Message Adapter");
        // stop the Event processing
        startMessageTimer();
    }

    @Override
    public void onException(final JMSException exception) {
        Log.error(this, "JMS onException notification", exception);
        if (this._listener != null) {
            this._listener.onDisconnect(this);
        }
        messageConnectionClosed();
        // if
        // (exception.getMessage().indexOf("Connection to the server has been
        // terminated")
        // >= 0) {
        // messageConnectionClosed();
        // } // end of if ()
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMessage(final Message mess) {

        boolean proc = false;
        try {
            if (mess instanceof TextMessage) {
                final TextMessage msg = (TextMessage) mess;

                final JMSQueueMessage jmsMessage = new JMSQueueMessage();
                jmsMessage.setReference(mess.getJMSMessageID());
                jmsMessage.setText(msg.getText());
                // GSM y DPM 11/11/14: Missing correlation id
                jmsMessage.setCorrelationId(mess.getJMSCorrelationID());

                final String propertyName = "JMSXDeliveryCount";
                int deliveryCount = 0;
                if (mess.propertyExists(propertyName)) {
                    deliveryCount = mess.getIntProperty(propertyName);
                }
                if ((getBackoutThreshold() != 0) && (deliveryCount > getBackoutThreshold())
                        && DSConnection.getDefault().isClosed()) {
                    Log.fatal("JMS", "Queue connection rolled back " + deliveryCount
                            + " times and DSConnection is closed, stopping engine.");
                    rollback();
                    System.exit(-1);
                } else {
                    // Commit /rollback will be done inside this method, in the
                    // engine
                    proc = callBackListener(jmsMessage);
                    if (getTransactionEnabled()) {
                        commit();
                    }
                }
            } else {
                // ObjectMessage msg = (ObjectMessage) mess;
                // byte[] bytes = (byte[])msg.getObject();
                // Object message = SerialUtil.bytes2object(bytes);
                // callBackListener(message);
                Log.error("JMS", "Non txt msg are not handled by this engine");
                if (getTransactionEnabled()) {
                    commit();
                }
            }
        } catch (final Exception e) {
            Log.error("JMS", e);
            try {
                if (getTransactionEnabled()) {
                    rollback();
                }
            } catch (final ConnectException e1) {
                Log.error(this, e1); //sonar 03/11/2017
                Log.error(getConfigName(), "Error during queue session rollback() for config " + getConfigName(), e);
            }
            Log.error(getConfigName(), "Error while parsing ExternalMessage ", e);
            return;
        } // end of try-catch

        if (getTransactionEnabled()) {
            return; // commit / rollback has already been done.
        }

        if (!proc) {
            Log.error("JMS", "Msg Not Acknoledge");
            return;
        }
        // // Message processed.
        try {
            if (getAckType() != Session.AUTO_ACKNOWLEDGE) {
                mess.acknowledge();
            }
        } catch (final JMSException e) {
            Log.error("JMS", "Error Acknowledging Incoming Message", e);

        } // end of try-catch
    }

    @Override
    public void reinit() throws ConnectException {
        init(false);
    }

    @Override
    public void rollback() throws ConnectException {
        try {
            this._session.rollback();
        } catch (final JMSException e) {
            Log.error("JMS", "Rollback failed", e);
            throw new ConnectException("Commit failed" + e.getMessage());
        }
    }

    /**
     * Set the type of acknowledgment
     *
     * @param String ackType= [auto | dups | client]
     */
    public void setAckType(final String ackType) {

        // ignore all ack modes, to test no acking
        this._ackType = -1;

        if (ackType == null) {
            return;
        }

        if (ackType.equals("auto")) {
            this._ackType = Session.AUTO_ACKNOWLEDGE;
        } else if (ackType.equals("dups")) {
            this._ackType = Session.DUPS_OK_ACKNOWLEDGE;
        } else if (ackType.equals("client")) {
            this._ackType = Session.CLIENT_ACKNOWLEDGE;
        } else {
            // ignore all ack modes, to test no acking
            this._ackType = -1;
        }

    }

    public void setBackoutThreshold(final String threshold) {
        try {
            this._backoutThreshold = Integer.parseInt(threshold);
        } catch (final NumberFormatException e) {
            Log.error("JMS", "could not parse backoutLimit property, value " + threshold);
        }
    }

    /**
     * Set the connection factory name.
     */
    public void setConnectionFactory(final String factory) {
        this._factoryName = factory;
    }

    /**
     * Set the password to use when connecting to the queue
     */
    public void setConnectionPassword(final String password) {
        this._connectionPassword = password;
    }

    /**
     * Set the user name to use when connecting to the queue
     */
    public void setConnectionUserName(final String userName) {
        this._connectionUserName = userName;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC
     */
    public void setModeType(final String mode) {
        this._modeTypeClass = mode;
    }

    /**
     * Set the name of the JMS queue to subscribe to
     */
    public void setReceiverQueueName(String queueName) {
        this._receiverQueueName = queueName;
    }

    /**
     * Get the name of the JMS queue to subscribe to
     */
    public String getReceiverQueueName() {
        return this._receiverQueueName;
    }

    /**
     * Set the name of the JMS queue to subscribe to
     */
    public void setSenderQueueName(String queueName) {
        this._senderQueueName = queueName;
    }

    /**
     * Get the name of the JMS queue to subscribe to
     */
    public String getSenderQueueName() {
        return this._senderQueueName;
    }

    /**
     * Set the sender persistence mode
     */
    public void setSenderPersisted(final String persist) {
        if (persist.equals("true")) {
            this._persistency = DeliveryMode.PERSISTENT;
        } else {
            this._persistency = DeliveryMode.NON_PERSISTENT;
        }
    }

    /**
     * Set whether or not a transaction should be used.
     */
    public void setTransactionEnabled(final String transacted) {
        this._transacted = Util.isTrue(transacted);
    }

    /**
     * Set the URL to the JMS server
     */
    public void setUrl(final String url) {
        this._jmsUrl = url;
    }

    /**
     * During initialization steps, message flow is inhibited to prevent
     * unpredictable behavior during initialization. Once initialization is
     * complete, the connection must be told to initiate the message flow.
     */
    private void startConnection() throws ConnectException {
        try {
            this._queueConnection.start();
        } catch (final JMSException e) {
            Log.error(this, e); //sonar 03/11/2017
            throw new ConnectException("Unable to start Connection (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Closing communication objects
     */
    @Override
    public void stop() {
        try {
            stopMessageTimer();
            if (this._opMode == SantanderIEAdapterConfig.SENDER || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                if (this._sender != null) {
                    this._sender.close();
                    this._sender = null;
                }
            }
            if (this._opMode == SantanderIEAdapterConfig.RECEIVER || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                if (this._receiver != null) {
                    this._receiver.close();
                    this._receiver = null;
                }
            }
            if (this._session != null) {
                this._session.close();
                this._session = null;
            }
            if (this._queueConnection != null) {
                this._queueConnection.close();
                this._queueConnection = null;
            }
            if (this._context != null) {
                this._context.close();
                this._context = null;
            }
        } catch (final JMSException e) {
            Log.error("JMS", e);
        } catch (NamingException e) {
            Log.error("JMS", e);
        }

    }

    @Override
    public boolean write(final JMSQueueMessage message) {
        try {
            final TextMessage msg = this._session.createTextMessage();
            msg.setText(message.getText());
            msg.setJMSCorrelationID(message.getCorrelationId());
            // GSM &DPM: set the reference to routing message id
            msg.setJMSMessageID(message.getReference());

            Log.debug("QueueAdapter", "Sending msg Text:" + message);
            this._sender.send(msg);
        } catch (final Exception e) {
            Log.error("QueueAdapter", "Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    @Override
    public boolean write(final String message) {
        try {
            final TextMessage msg = this._session.createTextMessage();
            msg.setText(message);
            Log.debug("QueueAdapter", "Sending msg Text:" + message);
            this._sender.send(msg);
        } catch (final Exception e) {
            Log.error("QueueAdapter", "Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#createQueueDynamicallyAndWrite(
     * calypsox .tk.bo.JMSQueueMessage)
     */
    @Override
    public boolean createQueueDynamicallyAndWrite(final JMSQueueMessage message) {
        // No implementation @ see TibocQueueIEAdapter
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#write(java.lang.String,
     * com.calypso.tk.bo.BOMessage)
     */
    @Override
    public boolean write(final String message, final BOMessage boMessage) {
        // CAL_DODD_112
        // not needed, will be override
        return false;
    }
}

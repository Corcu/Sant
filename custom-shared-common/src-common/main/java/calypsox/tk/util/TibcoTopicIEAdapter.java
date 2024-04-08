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
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterListener;

import javax.jms.MessageProducer;
import javax.jms.*;
import javax.naming.Context;
import java.util.Hashtable;

/**
 * This class contains all the functionality required to manage a sending or
 * receiving a JMS queue. It will do all the set required. It expects the
 * setQueueName and setProperties() method to be called, before initialization,
 * with the parameters required to set it up.
 *
 * @author aalonsop
 * <p>
 * This is a beta version, in future releases the idea it's to create an
 * IEAdapter that allows to connect to both Queues and Topics.
 * @since 1.0, 30/03/2016
 */
public class TibcoTopicIEAdapter extends SantanderIEAdapter implements MessageListener, ExceptionListener {

    final static public int RETRY = 2;
    final static public int TIMEOUT = 5 * 1000; // 5 secs
    private static final String INITIAL_FACTORY = "com.tibco.tibjms.naming.TibjmsInitialContextFactory";
    // *********************************
    // * JMS Queue Connectivity
    // *********************************
    protected Connection _queueConnection = null;
    protected TopicConnection topicConnection = null;
    protected Session _session = null;
    protected TopicSession topicSession = null;
    // Context _context = null;
    protected Queue _receiver = null;
    protected Queue _sender = null;
    protected Topic topicReceive = null;
    protected Topic topicSend = null;
    protected MessageConsumer _receiverQueue = null;
    protected MessageProducer _senderQueue = null;
    protected TopicSubscriber subscriber = null;
    protected TopicPublisher publisher = null;

    protected String _receiverQueueName = null;
    protected String _senderQueueName = null;
    // AAP FIX
    protected String _factoryName = INITIAL_FACTORY;
    protected String _connectionUserName = null;
    protected String _connectionPassword = null;
    protected String _jmsUrl = null;
    protected String _modeTypeClass = INITIAL_FACTORY;

    // *********************************
    // Messaging
    // *********************************
    protected int _opMode = SantanderIEAdapterConfig.RECEIVER;
    protected int _ackType = Session.AUTO_ACKNOWLEDGE;
    protected boolean _transacted = false;
    protected int _persistency = DeliveryMode.NON_PERSISTENT;
    protected int _backoutThreshold = 0;

    public TibcoTopicIEAdapter() {
    }

    /**
     * Set whether this is a sender or receiver to a queue.
     *
     * @param true = receiver
     */
    public TibcoTopicIEAdapter(int opMode) {
        this._opMode = opMode;
    }

    @Override
    public void init() throws ConnectException {
        initProperties();
        init(false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMessage(Message mess) {
        boolean proc = false;
        try {
            if (mess instanceof TextMessage) {
                TextMessage msg = (TextMessage) mess;
                Log.debug(this, "TibcoTopicIEAdapter.onMessage with message=" + mess.toString() + " and JMSMessageId="
                        + mess.getJMSMessageID());

                JMSQueueMessage jmsMessage = new JMSQueueMessage();
                jmsMessage.setReference(mess.getJMSMessageID());
                jmsMessage.setCorrelationId(msg.getJMSCorrelationID());
                jmsMessage.setText(msg.getText());

                String propertyName = "JMSXDeliveryCount";
                int deliveryCount = 0;
                if (mess.propertyExists(propertyName)) {
                    deliveryCount = mess.getIntProperty(propertyName);
                }
                if ((getBackoutThreshold() != 0) && (deliveryCount > getBackoutThreshold())
                        && DSConnection.getDefault().isClosed()) {
                    Log.fatal(this, "Queue connection rolled back " + deliveryCount
                            + " times and DSConnection is closed, stopping engine.");
                    rollback();
                    System.exit(-1);
                } else {
                    // Commit /rollback will be done inside this method, in the
                    // engine
                    // proc = callBackListener(message);
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
                Log.error(this, "Non txt msg are not handled by this engine");
                if (getTransactionEnabled()) {
                    commit();
                }
            }
        } catch (Exception e) {
            Log.error(this, e);
            try {
                if (getTransactionEnabled()) {
                    rollback();
                }
            } catch (ConnectException e1) {
                Log.error(this, "Error during queue session rollback() for config " + getConfigName(), e);
            }
            Log.error(this, getConfigName() + ": Error while parsing ExternalMessage in ", e);
            return;
        } // end of try-catch

        if (getTransactionEnabled()) {
            return; // commit / rollback has already been done.
        }

        if (!proc) {
            Log.error(this, getConfigName() + ": Msg Not Acknoledge");
            return;
        }
        // // Message processed.
        try {
            if (getAckType() != Session.AUTO_ACKNOWLEDGE) {
                mess.acknowledge();
            }
        } catch (JMSException e) {
            Log.error(this, getConfigName() + ": Error Acknowledging Incoming Message", e);

        } // end of try-catch
    }

    /**
     * Connect to the JNDI Server to get a reference to the administered
     * objects. and start the ball rolling.
     */
    public synchronized void init(boolean alwaysRetry) throws ConnectException {
        boolean done = false;
        stop();
        ConnectException error = new ConnectException("init IE Adapter failed");
        String sdef = Defaults.getProperty(DSConnection.MAX_RETRY_CONNECT);
        int retry = RETRY;
        if (sdef != null) {
            try {
                retry = Integer.parseInt(sdef);
            } catch (Exception exxx) {
            }
        }
        for (int i = 0; (i < retry) || (alwaysRetry); i++) {
            try {
                Hashtable<String, String> props = new Hashtable<String, String>();
                if (this._jmsUrl != null) {
                    props.put(Context.INITIAL_CONTEXT_FACTORY, this._modeTypeClass);
                    props.put(Context.PROVIDER_URL, this._jmsUrl);
                }

                // *******************************************************
                // * Create a connection to the messaging system provider
                // * and start the connection.
                // *******************************************************
                createProviderConnection(props);
                // Creates topic Connection
                createTopicProviderConnection();

                if ((this._opMode == SantanderIEAdapterConfig.SENDER)
                        || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                    initSender();
                }

                if ((this._opMode == SantanderIEAdapterConfig.RECEIVER)
                        || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                    initReceiver();
                }

                done = true;
            } catch (ConnectException e) {
                Log.error(this, e);
                error = e;
            } catch (Exception e) {
                Log.error(this, e);
                error = new ConnectException("Connection to Message Adapter failed (" + e.getMessage() + ")");
            } // end of try-catch
            if (done == false) {
                Log.info(this, "Could not connect to Message Adapter, Retrying " + (i + 1));
                try {
                    // Thread.currentThread().sleep(TIMEOUT);
                    Thread.sleep(TIMEOUT);
                } catch (Exception ee) {
                }
            } else {
                break;
            }
        }
        if (done == false) {
            throw error;
        }
        this._isOnline = true;
        Log.info(this, "Message Adapter Started ...");
    }

    @Override
    public void reinit() throws ConnectException {
        init(true);
    }

    @Override
    public boolean isOnline() {
        // if (this._isOnline) {
        // return this._isOnline;
        // }
        // try {
        // init(false);
        // } catch (Exception e) {
        // Log.error(this, getConfigName() + ": unable to re Init Adapter ", e);
        // } // end of try-catch
        return this._isOnline;
    }

    /**
     * Initialize the adapter as a queue sender
     */
    private void initSender() throws ConnectException {
        startConnections();
        // *******************************************************
        // * Create sessions, for sending and receiving messages
        // *******************************************************
        createSession();
        createTopicSession();

        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        this.topicSend = locateTopic(topicSend, getSenderQueueName());
        this._sender = locateQueue(this._sender, getSenderQueueName());

        // *******************************************************
        // * Create MessageProducer to send messages
        // *******************************************************
        try {
            createTopicPublisher();
        } catch (ConnectException e) {
            createQueueSender();
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
        createTopicSession();
        // *******************************************************
        // * finish initialization by locating/creating a queue and
        // * starting the connection.
        // *******************************************************
        this.topicReceive = locateTopic(topicReceive, getReceiverQueueName());
        this._receiver = locateQueue(this._receiver, getReceiverQueueName());

        // *******************************************************
        // * Create MessageConsumers to receive messages
        // *******************************************************
        try {
            createTopicSubscriber();
            try {
                this.subscriber.setMessageListener(this);
                this.topicConnection.setExceptionListener(this);
            } catch (JMSException e) {
                throw new ConnectException(
                        "Failed to set the message Listener: " + e.getClass() + ": " + e.getMessage());
            }
        } catch (ConnectException e) {
            createQueueReceiver();
            try {
                this._receiverQueue.setMessageListener(this);
                this._queueConnection.setExceptionListener(this);
            } catch (JMSException e1) {
                throw new ConnectException(
                        "Failed to set the message Listener: " + e.getClass() + ": " + e1.getMessage());
            } // end of try-catch
        }
        startConnections();
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
        Log.info(this,
                " receiverQueueName = " + getReceiverQueueName() + " senderQueueName = " + getSenderQueueName()
                        + " connectionFactory = " + getConnectionFactory() + " connectionUserName = "
                        + getConnectionUserName() + " jmsUrl = " + getUrl() + " modeType = " + getModeType()
                        + " ackType " + getAckType() + " transacted " + getSenderPersisted() + " persisted = "
                        + getTransactionEnabled() + " backoutThreshold " + getBackoutThreshold());
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
     * Set the connection factory name.
     */
    public void setConnectionFactory(String factory) {
        this._factoryName = factory;
    }

    /**
     * Get the connection factory name.
     */
    public String getConnectionFactory() {
        return this._factoryName;
    }

    /**
     * Set the user name to use when connecting to the queue
     */
    public void setConnectionUserName(String userName) {
        this._connectionUserName = userName;
    }

    /**
     * Get the user name used when connecting to the queue
     */
    public String getConnectionUserName() {
        return this._connectionUserName;
    }

    /**
     * Set the password to use when connecting to the queue
     */
    public void setConnectionPassword(String password) {
        this._connectionPassword = password;
    }

    /**
     * Get the password used when connecting to the queue
     */
    public String getConnectionPassword() {
        return this._connectionPassword;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC
     */
    // AAP FIX
    public void setModeType(String mode) {
        // this._modeTypeClass = mode;
    }

    /**
     * Get the modus operandus of the JNDI server be that RMI or IPC
     */
    public String getModeType() {
        return this._modeTypeClass;
    }

    /**
     * Set the URL to the JMS server
     */
    public void setUrl(String url) {
        this._jmsUrl = url;
    }

    /**
     * Get the URL to the JMS server
     */
    public String getUrl() {

        return this._jmsUrl;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC
     */
    public void getModeType(String mode) {
        this._modeTypeClass = mode;
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

    /**
     * Set the type of acknowledgment
     *
     * @param String ackType= [auto | dups | client]
     */
    public void setAckType(String ackType) {

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

    /**
     * Set the sender persistence mode
     */
    public void setSenderPersisted(String persist) {
        if (persist.equals("true")) {
            this._persistency = DeliveryMode.PERSISTENT;
        } else {
            this._persistency = DeliveryMode.NON_PERSISTENT;
        }
    }

    /**
     * return the sender persistancy
     */
    public int getSenderPersisted() {
        return this._persistency;
    }

    /**
     * Set whether or not a transaction should be used.
     */
    public void setTransactionEnabled(String transacted) {
        this._transacted = Util.isTrue(transacted);
    }

    @Override
    public boolean getTransactionEnabled() {
        return this._transacted;
    }

    public void setBackoutThreshold(String threshold) {
        try {
            this._backoutThreshold = Integer.parseInt(threshold);
        } catch (NumberFormatException e) {
            Log.error(this, "could not parse backoutLimit property, value " + threshold);
        }
    }

    public int getBackoutThreshold() {
        return this._backoutThreshold;
    }

    /**
     * A Connection provides the client access to the underlying messaging
     * system, and performs resource allocation and management. Connections are
     * created using a ConnectionFactory, which is typically located using JNDI.
     *
     * @throws ConnectException
     */
    // AAP MIG 14.4 Some class definition updates
    private void createProviderConnection(Hashtable<String, String> props) throws ConnectException {

        try {
            ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(this._jmsUrl);
            // configureFactory(factory);
            // We test for null, not "" as "" is a valid value for both user
            // name and password
            if ((getConnectionUserName() == null) && (getConnectionPassword() == null)) {
                // This should use your OS user name and password by default
                this._queueConnection = factory.createConnection();
            } else {
                this._queueConnection = factory.createConnection(getConnectionUserName(), getConnectionPassword());
            }
        } catch (JMSException e) {
            Log.error(this, e);
            throw new ConnectException("Failed to Create Queue Connection: " + e.getMessage());
        } catch (Exception e) {
            Log.error(this, e);
            throw new ConnectException("Other Exception");
        } // end of try-catch
    }

    /**
     * AAP A Connection provides the client access to the underlying messaging
     * system, and performs resource allocation and management. Connections are
     * created using a ConnectionFactory, which is typically located using JNDI.
     */
    private void createTopicProviderConnection() throws ConnectException {
        try {
            ConnectionFactory factory = new com.tibco.tibjms.TibjmsTopicConnectionFactory(this._jmsUrl);

            if ((getConnectionUserName() == null) && (getConnectionPassword() == null)) {
                // This should use your OS user name and password by default
                this.topicConnection = ((TopicConnectionFactory) factory).createTopicConnection();
            } else {
                this.topicConnection = ((TopicConnectionFactory) factory).createTopicConnection(getConnectionUserName(),
                        getConnectionPassword());
            }
        } catch (final JMSException e) {
            Log.error(this, e);
            throw new ConnectException(new CalypsoException(e), "Failed to Create Topic Connection: " + e.getMessage());
        } catch (final Exception e) {
            Log.error(this, e);
            throw new ConnectException(new CalypsoException(e), "Exception: " + e.getClass() + ": " + e.getMessage());
        } // end of try-catch

    }

    protected void configureFactory(QueueConnectionFactory factory) {
        // To be overridden
    }

    /**
     * Sessions are lightweight JMS objects which provide a context for
     * producing and consuming messages. Sessions are used to build message
     * producers and message consumers, as well as to build the messages
     * themselves.
     */
    private void createSession() throws ConnectException {
        if (this._session == null) {
            try {
                this._session = this._queueConnection.createSession(this._transacted, this._ackType);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create Session (" + e.getMessage() + ")");
            } // end of try-catch
        }
    }

    /**
     * Sessions are lightweight JMS objects which provide a context for
     * producing and consuming messages. Sessions are used to build message
     * producers and message consumers, as well as to build the messages
     * themselves.
     */
    private void createTopicSession() throws ConnectException {
        if (this._session == null) {
            try {
                this._session = this.topicConnection.createTopicSession(this._transacted, this._ackType);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create Session (" + e.getMessage() + ")");
            } // end of try-catch
        }
    }

    /**
     * Return the session
     */
    public Session getQueueSession() {
        return this._session;
    }

    /**
     * A queue is located through JNDI. The queue identifies the messages being
     * sent or received.
     */
    private Queue locateQueue(Queue queue, String queueName) throws ConnectException {
        /*
         * _queue = null; try { _queue = (Queue)_context.lookup(getQueueName());
         * } catch (Exception e) { }
         */
        if (queue == null) {
            try {
                queue = this._session.createQueue(queueName);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create Queue (" + e.getMessage() + ")");
            } // end of try-catch
            Log.info(this, "Queue " + queueName + " ready");
        }
        return queue;
    }

    /**
     * A Topic is located through JNDI. The queue identifies the messages being
     * sent or received.
     */
    private Topic locateTopic(Topic topic, String topicName) throws ConnectException {
        if (topic == null) {
            try {
                topic = this._session.createTopic(topicName);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create Queue (" + e.getMessage() + ")");
            } // end of try-catch
            Log.info(this, "Topic " + topicName + " ready");
        }
        return topic;
    }

    /**
     * During initialization steps, message flow is inhibited to prevent
     * unpredictable behavior during initialization. Once initialization is
     * complete, the connection must be told to initiate the message flow.
     */
    private void startConnections() throws ConnectException {
        try {
            this._queueConnection.start();
            this.topicConnection.start();
        } catch (JMSException e) {
            throw new ConnectException("Unable to start Connection (" + e.getMessage() + ")");
        }
    }

    /**
     * Create a queue receiver. In the queueing domain, a receiver receives
     * messages from a given queue. This code shows the creation of a receiver.
     */
    private void createQueueReceiver() throws ConnectException {
        if (this._receiverQueue == null) {
            try {
                this._receiverQueue = (this._session).createConsumer(this._receiver);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create receiver (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Create a queue receiver. In the queueing domain, a receiver receives
     * messages from a given queue. This code shows the creation of a receiver.
     */
    private void createTopicSubscriber() throws ConnectException {
        if (this.subscriber == null) {
            try {
                this.subscriber = (this.topicSession).createSubscriber(topicReceive);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create receiver (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Return a queue receiver.
     */
    public MessageConsumer getQueueReceiver() {
        return this._receiverQueue;
    }

    /**
     * Create a queue sender. In the queueing domain, a sender sends messages to
     * a given queue. This code shows the creation of a sender.
     */
    private void createQueueSender() throws ConnectException {
        if (this._senderQueue == null) {
            try {
                this._senderQueue = (this._session).createProducer(this._sender);
            } catch (JMSException e) {
                throw new ConnectException("unable to create sender (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Create a queue receiver. In the queueing domain, a receiver receives
     * messages from a given queue. This code shows the creation of a receiver.
     */
    private void createTopicPublisher() throws ConnectException {
        if (this.subscriber == null) {
            try {
                this.publisher = (this.topicSession).createPublisher(topicReceive);
            } catch (JMSException e) {
                throw new ConnectException("Unable to create receiver (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Return a queue sender.
     */
    public MessageProducer getQueueSender() {
        return this._senderQueue;
    }

    // /**
    // * For trades, when we have a mirror, we need to get the JmsReference from
    // * the trade master
    // *
    // * @param message
    // * @param trade
    // * trade Slave
    // * @return
    // */
    // public boolean write(String message, Trade trade) {
    // try {
    // TextMessage msg = _session.createTextMessage();
    // msg.setText(message);
    // String jmsReference = TradeUtil.getJMSReferenceMirror(trade);
    // msg.setJMSCorrelationID(jmsReference);
    // Log.debug(this, "TibcoQueueIEAdapter.write Sending msg Text:"
    // + message + " with JMSCorrelationID=" + jmsReference);
    // _senderQueue.send(msg);
    // } catch (Exception e) {
    // Log.error(this, getConfigName() + ": Unable to send Msg", e);
    // onException(new JMSException("Deconnected from Message Adapter"));
    // return false;
    // } // end of try-catch
    // return true;
    // }

    @Override
    public boolean write(String message) {
        try {
            TextMessage msg = this._session.createTextMessage();
            msg.setText(message);
            Log.debug(this, "TibcoQueueIEAdapter.write Sending msg Text:" + message);
            this._senderQueue.send(msg);
        } catch (Exception e) {
            Log.error(this, getConfigName() + ": Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    @Override
    public boolean write(JMSQueueMessage message) {
        try {
            TextMessage msg = this._session.createTextMessage();
            msg.setText(message.getText());
            msg.setJMSCorrelationID(message.getReference());
            Log.debug(this, "TibcoQueueIEAdapter.write Sending msg Text:" + message + " with JMSCorrelationID="
                    + message.getReference());
            this._senderQueue.send(msg);
        } catch (Exception e) {
            Log.error(this, getConfigName() + ": Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    public boolean writeMessageCorrelationId(JMSQueueMessage message) {
        try {
            TextMessage msg = this._session.createTextMessage();
            msg.setText(message.getText());
            msg.setJMSCorrelationID(message.getCorrelationId());
            Log.debug(this, "TibcoQueueIEAdapter.write Sending msg Text:" + message + " with JMSCorrelationID="
                    + message.getReference());
            this._senderQueue.send(msg);
        } catch (Exception e) {
            Log.error(this, getConfigName() + ": Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    @Override
    public void onException(JMSException exception) {
        Log.error(this, "JMS onException notification", exception);
        if (this._listener != null) {
            this._listener.onDisconnect(this);
        }
        messageConnectionClosed();
    }

    public void messageConnectionClosed() {
        Log.debug(this, getConfigName() + ": Connection Closed with Message Adapter");
        // stop the Event processing
        startMessageTimer();
    }

    /**
     * Closing communication objects
     */
    @Override
    public void stop() {
        try {
            stopMessageTimer();
            if (this._opMode == SantanderIEAdapterConfig.SENDER || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                if (this._senderQueue != null) {
                    this._senderQueue.close();
                    this._senderQueue = null;
                }
            }
            if (this._opMode == SantanderIEAdapterConfig.RECEIVER || (this._opMode == SantanderIEAdapterConfig.BOTH)) {
                if (this._receiverQueue != null) {
                    this._receiverQueue.close();
                    this._receiverQueue = null;
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

        } catch (final JMSException e) {
            Log.error("JMS", e);
        }
    }


    @Override
    public void commit() throws ConnectException {
        try {
            this._session.commit();
        } catch (JMSException e) {
            Log.error(this, "Commit failed", e);
            throw new ConnectException("Commit failed" + e.getMessage());
        }
    }

    @Override
    public void rollback() throws ConnectException {
        try {
            this._session.rollback();
        } catch (JMSException e) {
            Log.error(this, "Rollback failed", e);
            throw new ConnectException("Commit failed" + e.getMessage());
        }
    }

    /**
     * Purpose of this override is to give the IEAdapterListener visibility on
     * this Adapter. Mainly because ImportMessageEngine.IEAdapter is not visible
     * to extensions.
     */
    @Override
    public void setListener(IEAdapterListener listener) {
        this._listener = listener;
        if (listener instanceof ExtendedIEAdapterListener) {
            ((ExtendedIEAdapterListener) listener).setIEAdapter(this);
        }
    }

    @Override
    public boolean write(String message, BOMessage boMessage) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean createQueueDynamicallyAndWrite(JMSQueueMessage message) {
        // TODO Auto-generated method stub
        return false;
    }


}

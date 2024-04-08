/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
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

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class contains all the functionality required to manage a sending or
 * receiving a JMS topic. It will do all the set required. It expects the
 * setQueueName and setProperties() method to be called, before initialization,
 * with the parameters required to set it up.
 *
 * @since 5.0, 10/12/01
 */
// CAL_DODD_117
public class JMSTopicIEAdapter extends SantanderIEAdapter implements MessageListener, ExceptionListener {

    /**
     * The Constant INPUT_QUEUE_NAME.
     */
    public static final String INPUT_QUEUE_NAME = "input.queue.name";

    /**
     * The Constant OUTPUT_QUEUE_NAME.
     */
    public static final String OUTPUT_QUEUE_NAME = "output.queue.name";

    private static int numberOfRetries = 10;

    /**
     * The Constant TIMEOUT.
     */
    public static final int TIMEOUT = 5 * 1000; // 5 secs

    /**
     * The Constant RECEIVER.
     */
    public static final int RECEIVER = 1;

    /**
     * The Constant SENDER.
     */
    public static final int SENDER = 0;
    // *********************************
    // * JMS Topic Connectivity
    // *********************************
    protected TopicConnection topicConnection;
    protected TopicSession session;
    protected Context context;
    protected Topic topic;
    protected TopicSubscriber subscriber;
    protected TopicPublisher publisher;

    protected String topicName;
    protected String factoryName;
    protected String connectionUserName;
    protected String connectionPassword;
    protected String jmsUrl;
    protected String modeTypeClass;

    // *********************************
    // Messaging
    // *********************************
    int opMode = RECEIVER;
    int ackType = Session.AUTO_ACKNOWLEDGE;
    boolean transacted;
    int persistency = DeliveryMode.NON_PERSISTENT;
    int backoutThreshold;

    /**
     * Set whether this is a sender or receiver to a queue.
     *
     * @param isReceiver true = receiver
     */
    public JMSTopicIEAdapter(final int isReceiver) {
        this.opMode = isReceiver;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#commit()
     */
    @Override
    public void commit() throws ConnectException {
        try {
            this.session.commit();
        } catch (final JMSException e) {
            Log.error(this, "Commit failed", e);
            throw new ConnectException(new CalypsoException(e), "Commit failed" + e.getMessage());
        }
    }

    /**
     * Configure factory.
     *
     * @param factory the factory
     */
    protected void configureFactory(final TopicConnectionFactory factory) {
        // To be overridden
    }

    /**
     * @param jndiLookup
     */
    private synchronized void checkAndCreateProviderConnection(boolean jndiLookup) throws ConnectException {
        if (this.topicConnection == null) {
            createProviderConnection(jndiLookup);
        }
    }

    /**
     * A Connection provides the client access to the underlying messaging
     * system, and performs resource allocation and management. Connections are
     * created using a ConnectionFactory, which is typically located using JNDI.
     */
    private synchronized void createProviderConnection(boolean jndiLookup) throws ConnectException {
        try {
            final TopicConnectionFactory factory;
            if (jndiLookup) {
                factory = (TopicConnectionFactory) this.context.lookup(this.factoryName);
            } else
                factory = new com.tibco.tibjms.TibjmsTopicConnectionFactory(this.jmsUrl);
            // configureFactory(factory);
            // We test for null, not "" as "" is a valid value for both user
            // name and password
            if ((getConnectionUserName() == null) && (getConnectionPassword() == null)) {
                // This should use your OS user name and password by default
                this.topicConnection = factory.createTopicConnection();
            } else {
                this.topicConnection = factory.createTopicConnection(getConnectionUserName(), getConnectionPassword());
            }
        } catch (final JMSException e) {
            Log.error(this, e);
            throw new ConnectException(new CalypsoException(e), "Failed to Create Topic Connection: " + e.getMessage());
        } catch (final NamingException e) {
            Log.error(this, e);
            if (jndiLookup) {
                createProviderConnection(false);
            } else
                throw new ConnectException(new CalypsoException(e),
                        e.getMessage() + " using factory: " + this.factoryName);
        } catch (final Exception e) {
            Log.error(this, e);
            throw new ConnectException(new CalypsoException(e), "Exception: " + e.getClass() + ": " + e.getMessage());
        } // end of try-catch
    }

    /**
     * Create a topic subscriber.
     */
    private void createTopicSubscriber() throws ConnectException {
        try {
            this.subscriber = this.session.createSubscriber(this.topic);
        } catch (final JMSException e) {
            throw new ConnectException(new CalypsoException(e), "Unable to create subscriber (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Create a topic publisher.
     */
    private void createTopicPublisher() throws ConnectException {
        try {
            this.publisher = this.session.createPublisher(this.topic);
        } catch (final JMSException e) {
            throw new ConnectException(new CalypsoException(e), "unable to create publisher (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Sessions are lightweight JMS objects which provide a context for
     * producing and consuming topics.
     */
    private void createSession() throws ConnectException {
        try {
            // this.session = this.topicConnection.createTopicSession(
            // this.transacted, this.ackType);
            this.session = this.topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (final JMSException e) {
            throw new ConnectException(new CalypsoException(e), "Unable to create Session (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Get the type of acknowledgment.
     *
     * @return int ackType = [Session.AUTO_ACKNOWLEDGE |
     * Session.DUPS_OK_ACKNOWLEDGE | Session.CLIENT_ACKNOWLEDGE]
     */
    public int getAckType() {
        return this.ackType;
    }

    /**
     * Gets the backout threshold.
     *
     * @return the backout threshold
     */
    public int getBackoutThreshold() {
        return this.backoutThreshold;
    }

    /**
     * Get the connection factory name.
     *
     * @return the connection factory
     */
    public String getConnectionFactory() {
        return this.factoryName;
    }

    /**
     * Get the password used when connecting to the queue.
     *
     * @return the connection password
     */
    public String getConnectionPassword() {
        return this.connectionPassword;
    }

    /**
     * Get the user name used when connecting to the queue.
     *
     * @return the connection user name
     */
    public String getConnectionUserName() {
        return this.connectionUserName;
    }

    /**
     * Get the modus operandus of the JNDI server be that RMI or IPC.
     *
     * @return the mode type
     */
    public String getModeType() {
        return this.modeTypeClass;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC.
     *
     * @param mode the mode
     */
    public void getModeType(final String mode) {
        this.modeTypeClass = mode;
    }

    /**
     * Get the name of the JMS topic to subscribe to.
     *
     * @return the topic name
     */
    public String getTopicName() {
        return this.topicName;
    }

    /**
     * Return a topic subscriber.
     *
     * @return the topic subscriber.
     */
    public TopicSubscriber getTopicSubscriber() {
        return this.subscriber;
    }

    /**
     * Return a topic publisher.
     *
     * @return the topic publisher.
     */
    public TopicPublisher getTopicPublisher() {
        return this.publisher;
    }

    /**
     * Return the session.
     *
     * @return the queue session
     */
    public TopicSession getQueueSession() {
        return this.session;
    }

    /**
     * return the sender persistancy.
     *
     * @return the sender persisted
     */
    public int getSenderPersisted() {
        return this.persistency;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#getTransactionEnabled()
     */
    @Override
    public boolean getTransactionEnabled() {
        return this.transacted;
    }

    /**
     * Get the URL to the JMS server.
     *
     * @return the url
     */
    public String getUrl() {
        return this.jmsUrl;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#init()
     */
    @Override
    public void init() throws ConnectException {
        initProperties();
        init(false);
    }

    /**
     * Connect to the JNDI Server to get a reference to the administered
     * objects. and start the ball rolling.
     *
     * @param wait the wait
     * @throws ConnectException the connect exception
     */
    public void init(final boolean wait) throws ConnectException {
        boolean done = false;
        stop();
        ConnectException error = new ConnectException("init IE Adapter failed");
        final String sdef = Defaults.getProperty(DSConnection.MAX_RETRY_CONNECT);
        if (sdef != null) {
            try {
                numberOfRetries = Integer.parseInt(sdef);
            } catch (final Exception exxx) {
                Log.error(this, exxx);
            }
            if (numberOfRetries < 1) {
                numberOfRetries = 1;
            }
        }

        int retry;
        if (wait) {
            retry = numberOfRetries;
        } else {
            retry = 1;
        }
        for (int i = 0; i < retry; i++) {
            try {
                final Hashtable<String, String> props = new Hashtable<>();
                if (this.jmsUrl != null) {
                    props.put(Context.INITIAL_CONTEXT_FACTORY, this.modeTypeClass);
                    props.put(Context.PROVIDER_URL, this.jmsUrl);
                    if (this.connectionUserName != null) {
                        props.put(Context.SECURITY_PRINCIPAL, this.connectionUserName);
                    }
                    if (this.connectionPassword != null) {
                        props.put(Context.SECURITY_CREDENTIALS, this.connectionPassword);
                    }
                }
                this.context = new InitialContext(props);

                // *******************************************************
                // * Create a connection to the messaging system provider
                // * and start the connection.
                // * Now checks if an previous connection exists
                // *******************************************************
                checkAndCreateProviderConnection(true);

                if (this.opMode == RECEIVER) {
                    initReceiver();
                } else {
                    initSender();
                }
                done = true;
            } catch (final ConnectException e) {
                Log.error("START", e);
                error = e;
            } catch (final Exception e) {
                Log.error("START", e);
                error = new ConnectException("Connection to Message Adapter failed (" + e.getMessage() + ")");
            } // end of try-catch
            if (!done) {
                //Log.system("JMS", "Could not connect to Message Adapter, Retrying " + (i + 1));
                Log.info("JMS", "Could not connect to Message Adapter, Retrying " + (i + 1));
                try {
                    // Thread.currentThread().sleep(TIMEOUT);
                    Thread.sleep(TIMEOUT);
                } catch (final Exception ee) {
                    Log.error(this, ee);
                }
            } else {
                break;
            }
        }
        if (!done) {
            throw error;
        }
        this._isOnline = true;
        Log.system("JMS", "Message Adapter Started ...");
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
     */
    protected void initProperties() {
        String propVal = null;

        if ((propVal = getProperty("jms.topic.connectionFactory")) != null) {
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

        if (this.opMode == RECEIVER) {
            setTopicName(getProperty(INPUT_QUEUE_NAME));
        } else {
            setTopicName(getProperty(OUTPUT_QUEUE_NAME));
        }

        if (getTopicName() != null) {

            if ((propVal = getProperty(getTopicName() + ".queue.ackType")) != null) {
                setAckType(propVal);
            }

            if ((propVal = getProperty(getTopicName() + ".queue.persist")) != null) {
                setSenderPersisted(propVal);
            }

            if ((propVal = getProperty(getTopicName() + ".queue.transacted")) != null) {
                setTransactionEnabled(propVal);
            }

            if ((propVal = getProperty(getTopicName() + ".queue.backoutThreshold")) != null) {
                setBackoutThreshold(propVal);
            }

        }
        Log.system("TEMPO", " topicName = " + getTopicName() + " connectionFactory = " + getConnectionFactory()
                + " connectionUserName = " + getConnectionUserName() + " jmsUrl = " + getUrl() + " modeType = "
                + getModeType() + " ackType " + getAckType() + " transacted " + getSenderPersisted()
                + " persisted = " + getTransactionEnabled() + " backoutThreshold " + getBackoutThreshold());

		/*Log.info("TEMPO",
				" topicName = " + getTopicName() + " connectionFactory = " + getConnectionFactory()
						+ " connectionUserName = " + getConnectionUserName() + " jmsUrl = " + getUrl() + " modeType = "
						+ getModeType() + " ackType " + getAckType() + " transacted " + getSenderPersisted()
						+ " persisted = " + getTransactionEnabled() + " backoutThreshold " + getBackoutThreshold());*/
    }

    /**
     * Initialize the adapter as a queue receiver
     */
    private void initReceiver() throws ConnectException {

        // *******************************************************
        // * Create sessions, for publishing and subscribing topics
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a topic and
        // * starting the connection.
        // *******************************************************
        locateTopic();

        // *******************************************************
        // * Create TopicSubscribers to subscribe topics
        // *******************************************************
        createTopicSubscriber();

        try {
            this.subscriber.setMessageListener(this);
            this.topicConnection.setExceptionListener(this);
        } catch (final JMSException e) {
            throw new ConnectException(new CalypsoException(e),
                    "Failed to set the message Listener: " + e.getMessage());
        } // end of try-catch

        startConnection();
    }

    /**
     * Initialize the adapter as a queue sender
     */
    private void initSender() throws ConnectException {
        startConnection();
        // *******************************************************
        // * Create sessions, for publishing and subscribing topics
        // *******************************************************
        createSession();

        // *******************************************************
        // * finish initialization by locating/creating a topic and
        // * starting the connection.
        // *******************************************************
        locateTopic();

        // *******************************************************
        // * Create TopicPublisher to publish topics
        // *******************************************************
        createTopicPublisher();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#isOnline()
     */
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
    private void locateTopic() throws ConnectException {
        this.topic = null;
        try {
            this.topic = (Topic) this.context.lookup(getTopicName());
        } catch (final Exception e) {
            Log.error(this, e);
        }
        if (this.topic == null) {
            try {
                this.topic = this.session.createTopic(getTopicName());
            } catch (final JMSException e) {
                throw new ConnectException(new CalypsoException(e), "Unable to create Topic (" + e.getMessage() + ")");
            } // end of try-catch
        }
        Log.system(this.toString(), "Topic " + getTopicName() + " ready");
    }

    /**
     * Message connection closed.
     */
    public void messageConnectionClosed() {
        Log.debug(Log.START, "Connection Closed with Message Adapter");
        // stop the Event processing
        startMessageTimer();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
     */
    @Override
    public void onException(final JMSException exception) {
        Log.error(this, "JMS onException notification", exception);
        if (this._listener != null) {
            this._listener.onDisconnect(this);
        }
        messageConnectionClosed();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
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
                Log.error(this, "Non txt msg are not handled by this engine");
                if (getTransactionEnabled()) {
                    commit();
                }
            }
        } catch (final Exception e) {
            Log.error(this, e);
            try {
                if (getTransactionEnabled()) {
                    rollback();
                }
            } catch (final ConnectException e1) {
                Log.error(getConfigName(), "Error during queue session rollback() for config " + getConfigName(), e);
            }
            Log.error(getConfigName(), "Error while parsing ExternalMessage ", e);
            return;
        } // end of try-catch

        if (getTransactionEnabled()) {
            return; // commit / rollback has already been done.
        }

        if (!proc) {
            Log.error(this, "Msg Not Acknoledge");
            return;
        }
        // Message processed.
        try {
            if (getAckType() != Session.AUTO_ACKNOWLEDGE) {
                mess.acknowledge();
            }
        } catch (final JMSException e) {
            Log.error(this, "Error Acknowledging Incoming Message", e);

        } // end of try-catch
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#reinit()
     */
    @Override
    public void reinit() throws ConnectException {
        stop();
        init(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#rollback()
     */
    @Override
    public void rollback() throws ConnectException {
        try {
            this.session.rollback();
        } catch (final JMSException e) {
            Log.error(this, "Rollback failed", e);
            throw new ConnectException(new CalypsoException(e), "Commit failed" + e.getMessage());
        }
    }

    /**
     * Set the type of acknowledgment.
     *
     * @param ackType ackType= [auto | dups | client]
     */
    public void setAckType(final String ackType) {

        // ignore all ack modes, to test no acking
        this.ackType = -1;

        if (ackType == null) {
            return;
        }

        if ("auto".equals(ackType)) {
            this.ackType = Session.AUTO_ACKNOWLEDGE;
        } else if ("dups".equals(ackType)) {
            this.ackType = Session.DUPS_OK_ACKNOWLEDGE;
        } else if ("client".equals(ackType)) {
            this.ackType = Session.CLIENT_ACKNOWLEDGE;
        } else {
            // ignore all ack modes, to test no acking
            this.ackType = -1;
        }

    }

    /**
     * Sets the backout threshold.
     *
     * @param threshold the new backout threshold
     */
    public void setBackoutThreshold(final String threshold) {
        try {
            this.backoutThreshold = Integer.parseInt(threshold);
        } catch (final NumberFormatException e) {
            Log.error(this, "could not parse backoutLimit property, value " + threshold);
        }
    }

    /**
     * Set the connection factory name.
     *
     * @param factory the new connection factory
     */
    public void setConnectionFactory(final String factory) {
        this.factoryName = factory;
    }


    /**
     * @param connection
     */
    public void setTopicConnection(final TopicConnection connection) {
        this.topicConnection = connection;
    }

    public TopicConnection getTopicConnection() {
        return this.topicConnection;
    }

    /**
     * Set the password to use when connecting to the queue.
     *
     * @param password the new connection password
     */
    public void setConnectionPassword(final String password) {
        this.connectionPassword = password;
    }

    /**
     * Set the user name to use when connecting to the queue.
     *
     * @param userName the new connection user name
     */
    public void setConnectionUserName(final String userName) {
        this.connectionUserName = userName;
    }

    /**
     * Set the modus operandus of the JNDI server be that RMI or IPC.
     *
     * @param mode the new mode type
     */
    public void setModeType(final String mode) {
        this.modeTypeClass = mode;
    }

    /**
     * Set the name of the JMS queue to subscribe to.
     *
     * @param topicName the new queue name
     */
    public void setTopicName(final String topicName) {
        this.topicName = topicName;
    }

    /**
     * Set the sender persistence mode.
     *
     * @param persist the new sender persisted
     */
    public void setSenderPersisted(final String persist) {
        if ("true".equals(persist)) {
            this.persistency = DeliveryMode.PERSISTENT;
        } else {
            this.persistency = DeliveryMode.NON_PERSISTENT;
        }
    }

    /**
     * Set whether or not a transaction should be used.
     *
     * @param transacted the new transaction enabled
     */
    public void setTransactionEnabled(final String transacted) {
        this.transacted = Util.isTrue(transacted);
    }

    /**
     * Set the URL to the JMS server.
     *
     * @param url the new url
     */
    public void setUrl(final String url) {
        this.jmsUrl = url;
    }

    /**
     * During initialization steps, message flow is inhibited to prevent
     * unpredictable behavior during initialization. Once initialization is
     * complete, the connection must be told to initiate the message flow.
     */
    private void startConnection() throws ConnectException {
        try {
            this.topicConnection.start();
        } catch (final JMSException e) {
            throw new ConnectException(new CalypsoException(e), "Unable to start Connection (" + e.getMessage() + ")");
        } // end of try-catch
    }

    /**
     * Closing communication objects.
     */
    @Override
    public void stop() {
        try {
            stopMessageTimer();
            if (this.opMode == SENDER) {
                if (this.publisher != null) {
                    this.publisher.close();
                    this.publisher = null;
                }
            } else {
                if (this.subscriber != null) {
                    this.subscriber.close();
                    this.subscriber = null;
                }
            }
            if (this.session != null) {
                this.session.close();
                this.session = null;
            }
            if (this.topicConnection != null) {
                this.topicConnection.close();
                this.topicConnection = null;
            }
            if (this.context != null) {
                this.context.close();
                this.context = null;
            }
        } catch (final JMSException | NamingException e) {
            Log.error(this, e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.SantanderIEAdapter#write(calypsox.tk.bo.JMSQueueMessage)
     */
    @Override
    public synchronized boolean write(final JMSQueueMessage message) {
        try {
            final TextMessage msg = this.session.createTextMessage();
            msg.setText(message.getText());
            msg.setJMSCorrelationID(message.getReference());
            //Log.system(this, "Sending msg Text:" + message);
            Log.info(this, "Sending msg Text:" + message);
            this.publisher.send(msg);
            Log.info(this, "Message sent");
            //Log.system(this, "Message sent"));
        } catch (final Exception e) {
            Log.error(this, "Unable to send Msg", e);
            onException(new JMSException("Disconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#write(java.lang.String)
     */

    @Override
    public synchronized boolean write(final String message) {
        try {
            final TextMessage msg = this.session.createTextMessage();
            msg.setText(message);
            Log.debug("QueueAdapter", "Sending msg Text:" + message);
            this.publisher.send(msg);
        } catch (final Exception e) {
            Log.error("QueueAdapter", "Unable to send Msg", e);
            onException(new JMSException("Disconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }
    
    
    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#write(java.lang.String)
     */

    public synchronized boolean write(final String message, final Map<String, String> contextMap) {
        try {
            final TextMessage msg = this.session.createTextMessage();
            msg.setText(message);
            
            msg.setJMSCorrelationID((String) contextMap.get("ObjectId"));
			Iterator<Entry<String,String>> it = contextMap.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String,String> entry = (Entry<String,String>) it.next();
				msg.setStringProperty((String) entry.getKey(), (String) entry.getValue());
			}
            
            Log.debug("QueueAdapter", "Sending msg Text:" + message);
            this.publisher.send(msg);
        } catch (final Exception e) {
            Log.error("QueueAdapter", "Unable to send Msg", e);
            onException(new JMSException("Disconnected from Message Adapter"));
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

    @Override
    public synchronized boolean write(final String message, final BOMessage boMessage) {
        return write(message);
    }

    public synchronized JMSTopicIEAdapter cloneAdapterNoSession() {
        JMSTopicIEAdapter clonedAdapter = new JMSTopicIEAdapter(this.opMode);
        clonedAdapter.ackType = this.ackType;
        clonedAdapter.topicConnection = this.topicConnection;
        clonedAdapter.topic = this.topic;
        clonedAdapter.backoutThreshold = this.backoutThreshold;
        clonedAdapter.connectionPassword = this.connectionPassword;
        clonedAdapter.connectionUserName = this.connectionUserName;
        clonedAdapter.context = this.context;
        clonedAdapter.factoryName = this.factoryName;
        clonedAdapter.jmsUrl = this.jmsUrl;
        clonedAdapter.modeTypeClass = this.modeTypeClass;
        clonedAdapter.opMode = this.opMode;
        clonedAdapter.topicName = this.topicName;
        return clonedAdapter;
    }
}

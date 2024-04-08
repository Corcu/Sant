/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterConfig;
import com.calypso.tk.util.IEAdapterListener;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import java.util.Map;
import java.util.Properties;

/**
 * The Class SantanderJMSQueueIEAdapterConfig.
 */
// CAL_DODD_117
public class SantanderJMSQueueTopicIEAdapter extends SantanderIEAdapter implements MessageListener, ExceptionListener {

    protected IEAdapterMode mode; //sonar 02/11/2017
    protected JMSQueueIEAdapter inputAdapter; //sonar 02/11/2017
    protected JMSTopicIEAdapter outputAdapter; //sonar 02/11/2017

    /**
     * Instantiates a new santander routing queue ie adapter.
     *
     * @param mode the mode
     */
    public SantanderJMSQueueTopicIEAdapter(final IEAdapterMode mode) {
        this.mode = mode;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#setIEAdapterConfig(com.calypso.tk.util. IEAdapterConfig)
     */
    @Override
    public void setIEAdapterConfig(final IEAdapterConfig config) {
        super.setIEAdapterConfig(config);
        try {
            if (isSender()) {
                JMSTopicIEAdapter topicIEAdapter = new JMSTopicIEAdapter(JMSTopicIEAdapter.SENDER);
                topicIEAdapter.setIEAdapterConfig(config);
            }
            if (isReceiver()) {
                this.inputAdapter = new JMSQueueIEAdapter(JMSQueueIEAdapter.RECEIVER);
                this.inputAdapter.setIEAdapterConfig(config);
            }
        } catch (final ConnectException exception) {
            Log.error(Log.ERR, exception);
        }
    }

    public void setOutputAdapter(JMSTopicIEAdapter ieAdapter) {
        this.outputAdapter = ieAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#setListener(com.calypso.tk.util. IEAdapterListener)
     */
    @Override
    public void setListener(final IEAdapterListener listener) {
        super.setListener(listener);

        try {
            if (isSender() && (this.outputAdapter != null)) {
                this.outputAdapter.setListener(listener);
            }
            if (isReceiver() && (this.inputAdapter != null)) {
                this.inputAdapter.setListener(listener);
            }
        } catch (final ConnectException exception) {
            Log.error(Log.ERR, exception);
        }
    }

    public JMSTopicIEAdapter getOutputAdapter() {
        return this.outputAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
     */
    @Override
    public void onException(final JMSException arg0) {
        if (this.inputAdapter != null) {
            this.inputAdapter.onException(arg0);
        }
        if (this.outputAdapter != null) {
            this.outputAdapter.onException(arg0);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(final Message arg0) {
        if (this.inputAdapter != null) {
            this.inputAdapter.onMessage(arg0);
        }
    }

    /**
     * Gets the way.
     *
     * @return the way
     * @throws ConnectException the connect exception
     */
    public int getWay() throws ConnectException {
        final IEAdapterConfig config = getIEAdapterConfig();
        if (config == null) {
            throw new ConnectException("IEAdapter config is null");
        }
        final Properties properties = config.getProperties();
        if (properties == null) {
            throw new ConnectException("IEAdapter config Properties is null");
        }
        final Object opMode = properties.get("jms.opmode");
        if (opMode == null) {
            throw new ConnectException("IEAdapter config Properties jms.opmode is null");
        }
        return Integer.parseInt((String) opMode);

    }

    /**
     * Checks if is sender.
     *
     * @return true, if is sender
     * @throws ConnectException the connect exception
     */
    public boolean isSender() throws ConnectException {
        if (IEAdapterMode.WRITE.equals(this.mode)) {
            final int way = getWay();
            return (way == SantanderIEAdapterConfig.SENDER) || (way == SantanderIEAdapterConfig.BOTH);
        }
        return false;
    }

    /**
     * Checks if is receiver.
     *
     * @return true, if is receiver
     * @throws ConnectException the connect exception
     */
    public boolean isReceiver() throws ConnectException {
        if (IEAdapterMode.READ.equals(this.mode)) {
            final int way = getWay();
            return (way == SantanderIEAdapterConfig.RECEIVER) || (way == SantanderIEAdapterConfig.BOTH);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#init()
     */
    @Override
    public void init() throws ConnectException {
        if ((this.inputAdapter != null) && isReceiver()) {
            this.inputAdapter.init();
        }
        if ((this.outputAdapter != null) && isSender()) {
            this.outputAdapter.init();
        }
    }

    private void initAdapterPool() {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#reinit()
     */
    @Override
    public void reinit() throws ConnectException {
        if ((this.inputAdapter != null) && isReceiver()) {
            this.inputAdapter.reinit();
        }
        if ((this.outputAdapter != null) && isSender()) {
            this.outputAdapter.reinit();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#write(java.lang.String)
     */
    @Override
    public boolean write(final String message) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#write(calypsox.tk.bo.JMSQueueMessage)
     */
    @Override
    public boolean write(final JMSQueueMessage message) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#stop()
     */
    @Override
    public void stop() throws Exception {
        if (this.inputAdapter != null) {
            this.inputAdapter.stop();
        }
        if (this.outputAdapter != null) {
            this.outputAdapter.stop();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#commit()
     */
    @Override
    public void commit() throws ConnectException {
        if (this.inputAdapter != null) {
            this.inputAdapter.commit();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#rollback()
     */
    @Override
    public void rollback() throws ConnectException {
        if (this.inputAdapter != null) {
            this.inputAdapter.rollback();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapter#getTransactionEnabled()
     */
    @Override
    public boolean getTransactionEnabled() throws ConnectException {
        if (this.inputAdapter != null) {
            return this.inputAdapter.getTransactionEnabled();
        }
        if (this.outputAdapter != null) {
            return this.outputAdapter.getTransactionEnabled();
        }
        return false;
    }

    /**
     * @return
     */
    public IEAdapterMode getMode() {
        return this.mode;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#createQueueDynamicallyAndWrite(calypsox .tk.bo.JMSQueueMessage)
     */
    public boolean createQueueDynamicallyAndWrite(final JMSQueueMessage message) {
        // No implementation @ see TibocQueueIEAdapter
        return false;
    }

    public boolean write(final String message, final BOMessage boMessage) {
        // CAL_DODD_112
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message, boMessage);
        }
        return false;
    }
    
    public boolean write(final String message, final Map<String, String> contextMap) {
        // CAL_DODD_112
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message, contextMap);
        }
        return false;
    }
    
}

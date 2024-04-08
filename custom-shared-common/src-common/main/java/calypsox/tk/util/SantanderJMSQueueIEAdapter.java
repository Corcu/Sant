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
import java.util.Properties;

/**
 * Using the configuration given by IEAdapterConfig, gives the methods necessary methods to read, write and play around
 * with the configured queue.
 * <p>
 * The SantUpdatePositionEngine engine will interact the JMS Queue using this class.
 *
 * @author Guillermo Solano & Patrice Guerrido
 * @version 2.0, 29/08/13
 */
public class SantanderJMSQueueIEAdapter extends SantanderIEAdapter implements MessageListener, ExceptionListener {

    protected IEAdapterMode mode;

    /**
     * output queue
     */
    protected JMSQueueIEAdapter inputAdapter = null;
    /**
     * input queue
     */
    protected JMSQueueIEAdapter outputAdapter = null;

    /**
     * Constructor of the adapter
     */
    public SantanderJMSQueueIEAdapter() {
    }

    /**
     * Instantiates a new santander routing queue ie adapter.
     *
     * @param mode the mode
     */
    public SantanderJMSQueueIEAdapter(final IEAdapterMode mode) {
        this.mode = mode;
    }

    /**
     * @param config to set to the adapter interface
     */
    @Override
    public void setIEAdapterConfig(final IEAdapterConfig config) {
        super.setIEAdapterConfig(config);

        try {
            if (isSender()) {
                this.outputAdapter = new JMSQueueIEAdapter(JMSQueueIEAdapter.SENDER);
                this.outputAdapter.setIEAdapterConfig(config);
            }
            if (isReceiver()) {
                this.inputAdapter = new JMSQueueIEAdapter(JMSQueueIEAdapter.RECEIVER);
                this.inputAdapter.setIEAdapterConfig(config);
            }
        } catch (final ConnectException exception) {
            Log.error(Log.ERR, exception);
        }
    }

    /**
     * Sets the adapter interface to be listened
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

    /**
     * Inits the adapters for incoming and response
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

    /**
     * Exceptions on JMS
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

    /**
     * process on message
     */
    @Override
    public void onMessage(final Message arg0) {
        if (this.inputAdapter != null) {
            this.inputAdapter.onMessage(arg0);
        }
    }

    /**
     * @return the direction (operation mode integer)
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
     * @return if adapter is sender
     * @throws ConnectException
     */
    public boolean isSender() throws ConnectException {
        final int way = getWay();

        return (way == SantanderIEAdapterConfig.SENDER) || (way == SantanderIEAdapterConfig.BOTH);
    }

    /**
     * @return if adapter is receiver
     * @throws ConnectException
     */
    public boolean isReceiver() throws ConnectException {
        final int way = getWay();

        return (way == SantanderIEAdapterConfig.RECEIVER) || (way == SantanderIEAdapterConfig.BOTH);
    }

    /**
     * Reinits both adapters
     */
    @Override
    public void reinit() throws ConnectException {
        @SuppressWarnings("unused") final int way = getWay();

        if ((this.inputAdapter != null) && isReceiver()) {
            this.inputAdapter.reinit();
        }
        if ((this.outputAdapter != null) && isSender()) {
            this.outputAdapter.reinit();
        }
    }

    /**
     * Writes the message through the output adapter
     *
     * @param message
     * @return success or not
     */
    @Override
    public boolean write(final String message) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message);
        } else {
            Log.error(this, "ERR: Not possible to write into the queue: " + message + "\n Check queue configuration");
        }
        return false;
    }

    /**
     * Writes the message through the output adapter
     *
     * @param message
     * @return success or not
     */
    @Override
    public boolean write(final JMSQueueMessage message) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message);
        } else {
            Log.error(this, "ERR: Not possible to write into the queue message: " + message
                    + "\n Check queue configuration");
        }
        return false;
    }

    /**
     * Stops all adapters
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

    /**
     * Commits
     */
    @Override
    public void commit() throws ConnectException {
        if (this.inputAdapter != null) {
            this.inputAdapter.commit();
        }
    }

    /**
     * Rollback
     */
    @Override
    public void rollback() throws ConnectException {
        if (this.inputAdapter != null) {
            this.inputAdapter.rollback();
        }
    }

    /**
     * @return transaction mode
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

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#createQueueDynamicallyAndWrite(calypsox .tk.bo.JMSQueueMessage)
     */
    @Override
    public boolean createQueueDynamicallyAndWrite(final JMSQueueMessage message) {
        // No implementation @ see TibocQueueIEAdapter
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.SantanderIEAdapter#write(java.lang.String, com.calypso.tk.bo.BOMessage)
     */
    @Override
    // CAL_DODD_112
    public boolean write(final String message, final BOMessage boMessage) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message, boMessage);
        }
        return false;
    }
}

package calypsox.tk.util;

import com.calypso.tk.core.Log;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;

/**
 * The Class SantanderJMSQueueIEAdapterConfig.
 */
// CAL_DODD_117
public class SantanderJMSQueueTopicIEAdapterConfig extends SantanderIEAdapterConfig {

    /**
     * Instantiates a new santander routing queue ie adapter config.
     */
    public SantanderJMSQueueTopicIEAdapterConfig() {
        // nothing to do
    }

    /**
     * Instantiates a new santander routing queue ie adapter config.
     *
     * @param type the type
     */
    public SantanderJMSQueueTopicIEAdapterConfig(final String type) {
        super(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapterConfig#getReceiverIEAdapter()
     */
    @Override
    public synchronized SantanderIEAdapter getSantReceiverIEAdapter() {
        if (!isReceiver()) {
            return null;
        }
        if (this.santIEAdapter == null) {
            if ("file".equals(this.properties.getProperty("adapter"))) {
                this.santIEAdapter = new FileIEAdapter(IEAdapterMode.READ);
                try {
                    this.santIEAdapter.init();
                } catch (final ConnectException e) {
                    Log.error(this, e); //sonar
                }

            } else {
                this.santIEAdapter = new SantanderJMSQueueIEAdapter(IEAdapterMode.READ);
            }
            this.santIEAdapter.setIEAdapterConfig(this);
        }
        return this.santIEAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.IEAdapterConfig#getSenderIEAdapter()
     */
    @Override
    public IEAdapter getSenderIEAdapter() {
        if (!isSender()) {
            return null;
        }
        if (this.santIEAdapter == null) {
            if ("file".equals(this.properties.getProperty("adapter"))) {
                this.santIEAdapter = new FileIEAdapter(IEAdapterMode.WRITE);
            } else {
                this.santIEAdapter = new SantanderJMSQueueTopicIEAdapter(IEAdapterMode.WRITE);
            }
            this.santIEAdapter.setIEAdapterConfig(this);
        }
        return this.santIEAdapter;
    }

    /**
     * Gets the way.
     *
     * @return the way
     * @throws ConnectException the connect exception
     */
    public int getWay() throws ConnectException {
        if (this.properties == null) {
            throw new ConnectException("IEAdapter config Properties is null");
        }
        final Object opMode = this.properties.get("jms.opmode");
        if (opMode == null) {
            throw new ConnectException("IEAdapter config Properties jms.opmode is null");
        }
        return Integer.parseInt((String) opMode);
    }

    /**
     * Checks if is sender.
     *
     * @return true, if is sender
     */
    public boolean isSender() {
        int way;
        try {
            way = getWay();
        } catch (final ConnectException e) {
            Log.error(this, e); //sonar
            return false;
        }

        return (way == SENDER) || (way == BOTH);
    }

    /**
     * Checks if is receiver.
     *
     * @return true, if is receiver
     */
    public boolean isReceiver() {
        int way;
        try {
            way = getWay();
        } catch (final ConnectException e) {
            Log.error(this, e); //sonar
            return false;
        }

        return (way == RECEIVER) || (way == BOTH);
    }
}

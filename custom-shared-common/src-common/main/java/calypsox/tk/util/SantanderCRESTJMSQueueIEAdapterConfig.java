package calypsox.tk.util;

import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import org.jfree.util.Log;

/**
 * This class contains all the functionality required to manage a sending or receiving a JMS messages from Gestion
 * Disponible. It will do all the configuration needed. It expects the setQueueName and setProperties() method to be
 * override, before initialization, with the parameters required to set it up.
 * <p>
 * The SantUpdatePositionEngine engine will connect to a JMS Queue by the mean of this adapter.
 *
 * @author Guillermo Solano & Patrice Guerrido
 * @version 2.0, 29/05/13
 */
public class SantanderCRESTJMSQueueIEAdapterConfig extends SantanderIEAdapterConfig {

    protected SantanderCRESTJMSQueueIEAdapter santanderCRESTJMSQueueIEAdapter = null; //sonar 02/07/2017

    /**
     * Constructor
     */
    public SantanderCRESTJMSQueueIEAdapterConfig() {
    }

    /**
     * Constructor
     *
     * @param type of the Adapter (expected
     */
    public SantanderCRESTJMSQueueIEAdapterConfig(final String type) {
        super(type);
    }

    /**
     * @return the receiver adapter
     */
    @Override
    public synchronized SantanderIEAdapter getSantReceiverIEAdapter() {
        if (!isReceiver()) {
            return null;
        }
        if (this.santanderCRESTJMSQueueIEAdapter == null) {
            this.santanderCRESTJMSQueueIEAdapter = new SantanderCRESTJMSQueueIEAdapter(JMSQueueIEAdapter.RECEIVER);
            this.santanderCRESTJMSQueueIEAdapter.setIEAdapterConfig(this);
        }
        return this.santanderCRESTJMSQueueIEAdapter;
    }

    /**
     * @return the IEAdapter of the sender
     */
    @Override
    public IEAdapter getSenderIEAdapter() {
        if (!isSender()) {
            return null;
        }
        if (this.santanderCRESTJMSQueueIEAdapter == null) {
            this.santanderCRESTJMSQueueIEAdapter = new SantanderCRESTJMSQueueIEAdapter(JMSQueueIEAdapter.SENDER);
            this.santanderCRESTJMSQueueIEAdapter.setIEAdapterConfig(this);
        }
        return this.santanderCRESTJMSQueueIEAdapter;
    }

    /**
     * @return 0 if SENDER, 1 if RECEIVER
     * @throws ConnectException
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
     * @return true if the adapterConfig belongs to the sender
     */
    public boolean isSender() {
        int way;
        try {
            way = getWay();
        } catch (final ConnectException e) {
            Log.error(this, e); //sonar 02/11/2017
            return false;
        }

        return (way == SENDER) || (way == BOTH);
    }

    /**
     * @return true if the adapterConfig belongs to the receiver
     */
    public boolean isReceiver() {
        int way;
        try {
            way = getWay();
        } catch (final ConnectException e) {
            Log.error(this, e); //sonar 02/11/2017
            return false;
        }

        return (way == RECEIVER) || (way == BOTH);
    }
}

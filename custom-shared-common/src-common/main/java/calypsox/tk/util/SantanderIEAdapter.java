package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

public abstract class SantanderIEAdapter extends IEAdapter {

    /**
     * Write.
     *
     * @param message the message
     * @return true, if successful
     */
    public abstract boolean write(JMSQueueMessage message);

    /**
     * Write.
     *
     * @param message   the message
     * @param boMessage the BOMessage
     * @return true, if successful
     */
    // CAL_DODD_112
    public abstract boolean write(String message, BOMessage boMessage);

    /**
     * Creates the queue dynamically and write.
     *
     * @param message the message
     * @return true, if successful
     */
    abstract boolean createQueueDynamicallyAndWrite(JMSQueueMessage message);


    public static SantanderIEAdapterConfig getConfig(String configName) {
        IEAdapterConfig config = IEAdapter.getConfig(configName);
        if (config instanceof SantanderIEAdapterConfig) {
            return (SantanderIEAdapterConfig) config;
        } else {
            Log.error(SantanderIEAdapter.class.getSimpleName(), "Couldn't instanciate IEAdapterConfig, " +
                    "it must extend SantanderIEAdapterConfig");
            return null;
        }
    }
}

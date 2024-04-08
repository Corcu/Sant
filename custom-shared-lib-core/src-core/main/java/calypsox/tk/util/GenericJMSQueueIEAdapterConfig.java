package calypsox.tk.util;

import com.calypso.tk.util.IEAdapter;

public class GenericJMSQueueIEAdapterConfig extends SantanderIEAdapterConfig {
    protected GenericJMSQueueIEAdapter iEAdapter = null;

    @Override
    public synchronized SantanderIEAdapter getSantReceiverIEAdapter() {
        int mode = getOpMode();

        if (mode == RECEIVER || mode == BOTH) {
            if (iEAdapter != null) {
                return iEAdapter;
            }

            iEAdapter = new GenericJMSQueueIEAdapter(mode);
            iEAdapter.setIEAdapterConfig(this);
            return iEAdapter;
        } else {
            return null;
        }
    }

    @Override
    public IEAdapter getSenderIEAdapter() {
        /*
         * if (_mqSender != null) return _mqSender;
         *
         * _mqSender = new JMSQueueIEAdapter(JMSQueueIEAdapter.SENDER);
         * _mqSender.setIEAdapterConfig(this); return _mqSender;
         */
        int mode = getOpMode();

        if (mode == SENDER || mode == BOTH) {
            if (iEAdapter != null) {
                return iEAdapter;
            }

            iEAdapter = new GenericJMSQueueIEAdapter(mode);
            iEAdapter.setIEAdapterConfig(this);
            return iEAdapter;
        } else {
            return null;
        }

    }

    private int getOpMode() {
        Object oValue = properties.get(OPMODE_PROP_NAME);
        if (oValue == null || !(oValue instanceof String)) {
            return -1;
        }
        String sValue = (String) oValue;
        return Integer.parseInt(sValue);
    }
    /*
     * public String getPropertyFileName() { return
     * "calypso_tibco_config.properties"; }
     */
}

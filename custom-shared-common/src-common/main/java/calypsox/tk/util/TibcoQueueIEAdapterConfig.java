package calypsox.tk.util;

import com.calypso.tk.util.IEAdapter;

public class TibcoQueueIEAdapterConfig extends SantanderIEAdapterConfig {

    @Override
    public synchronized SantanderIEAdapter getSantReceiverIEAdapter() {
        int mode = getOpMode();

        if (mode == RECEIVER || mode == BOTH) {
            if (santIEAdapter != null) {
                return santIEAdapter;
            }

            santIEAdapter = new TibcoQueueIEAdapter(mode);
            santIEAdapter.setIEAdapterConfig(this);
            return santIEAdapter;
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
            if (santIEAdapter != null) {
                return santIEAdapter;
            }

            santIEAdapter = new TibcoQueueIEAdapter(mode);
            santIEAdapter.setIEAdapterConfig(this);
            return santIEAdapter;
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

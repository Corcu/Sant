/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterConfig;

import javax.jms.ExceptionListener;
import javax.jms.MessageListener;

public class SantanderKondorPlusIEAdapter extends SantanderJMSQueueTopicIEAdapter implements MessageListener,
        ExceptionListener {

    public SantanderKondorPlusIEAdapter(final IEAdapterMode mode) {
        super(mode);
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
                this.outputAdapter = new KondorPlusJMSQueueIEAdapter(JMSTopicIEAdapter.SENDER);
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
     * @see calypsox.tk.util.SantanderJMSQueueIEAdapter#write(java.lang.String, com.calypso.tk.bo.BOMessage)
     */
    @Override
    public boolean write(final String message, final BOMessage boMessage) {
        if (this.outputAdapter != null) {
            return this.outputAdapter.write(message, boMessage);
        }
        return false;
    }

}

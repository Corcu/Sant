/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.bo.document.JMSKPlusDocumentSender;
import com.calypso.tk.util.IEAdapter;

public class SantanderKondorPlusIEAdapterConfig extends SantanderJMSQueueTopicIEAdapterConfig {

    /**
     * Default constructor.
     */
    public SantanderKondorPlusIEAdapterConfig() {
        setAdapterType(JMSKPlusDocumentSender.ADAPTER);
    }

    @Override
    public boolean isReceiver() {
        return true;
    }

    @Override
    public boolean isSender() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return isConfigured(JMSKPlusDocumentSender.CONFIG_FILE_NAME);
    }

    @Override
    public IEAdapter getSenderIEAdapter() {
        if (!isSender()) {
            return null;
        }
        if ("file".equals(this.properties.getProperty("adapter"))) {
            this.santIEAdapter = new FileIEAdapter(IEAdapterMode.WRITE);
        } else {
            this.santIEAdapter = new SantanderKondorPlusIEAdapter(IEAdapterMode.WRITE);
        }
        this.santIEAdapter.setIEAdapterConfig(this);

        return this.santIEAdapter;
    }
}

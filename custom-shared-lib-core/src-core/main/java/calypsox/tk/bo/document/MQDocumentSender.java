/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag-management.com)
 * All rights reserved.
 *
 */
package calypsox.tk.bo.document;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import java.util.Properties;
import java.util.Vector;

public abstract class MQDocumentSender implements DocumentSender {
    public static String ADAPTER_TYPE = "ADAPTER_TYPE";
    public static String ADAPTER_CONFIG = "SantanderJMSQueue";

    protected String adapterType = null;

    /**
     * @param adapterType
     */
    public MQDocumentSender(String adapterType) {
        super();
        this.adapterType = adapterType;
    }

    public abstract String getConfigFileName();

    @Override
    public boolean isOnline() {
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId,
                        AdviceDocument document, Vector copies, BOMessage message, Vector errors, java.lang.String engineName,
                        boolean[] saved) {

        Log.debug(this + this.adapterType, "MQDocumentSender.send() - entry");

        boolean result = false;
        Properties p = Defaults.getProperties();
        if (p == null) {
            p = new Properties();
        }
        if (this.adapterType == null) {
            Log.error(this, "MQDocumentSender.send() - Implementation must specify a valid adapter type");
            String error = ("Message can not be Sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                    : "" + " " + "adapterType cannot be null";

            errors.addElement(error);
        } else {
            p.put(ADAPTER_TYPE, this.adapterType);
            Defaults.setProperties(p);
            IEAdapterConfig _config = null;
            IEAdapter adapter = null;
            String _configName = ADAPTER_CONFIG;
            StringBuffer output = null;

            if (_config == null) {
                _config = IEAdapter.getConfig(_configName);
            }
            if ((_config == null) || !_config.isConfigured(getConfigFileName())) {
                Log.error(this + this.adapterType, "MQDocumentSender.send() - " + _configName
                        + " not configured properly ");
                String error = ("Message can not be Sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                        : "" + " " + " not configured properly";

                errors.addElement(error);
            } else {
                adapter = _config.getSenderIEAdapter();
                try {
                    adapter.init();
                    output = document.getDocument();
                    adapter.write(output.toString());
                    BOMessage msg = (BOMessage) message.clone();
                    msg.setAction(Action.SEND);
                    msg.setEnteredUser(DSConnection.getDefault().getUser());
                    DSConnection.getDefault().getRemoteBO().save(msg, 0, null);
                    result = true;
                } catch (Exception e) {
                    Log.error(this + this.adapterType, "MQDocumentSender.send() - Exception:\n" + e);
                    String error = ("Message can not be Sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                            : "" + ": " + e.getMessage();

                    errors.addElement(error);
                }
            }
        }
        Log.debug(this + this.adapterType, "MQDocumentSender.send() - finished with result=" + result + " MessageId: "
                + message.getAllocatedLongSeed());
        // Save the task

        return result;
    }

    public String getAdapterType() {
        return this.adapterType;
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

}

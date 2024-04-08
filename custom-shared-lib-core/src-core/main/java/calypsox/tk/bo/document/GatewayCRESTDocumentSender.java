/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag-management.com)
 * All rights reserved.
 *
 */
package calypsox.tk.bo.document;

import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import calypsox.tk.util.SantanderCRESTJMSQueueIEAdapter;
import calypsox.tk.util.SantanderIEAdapterConfig;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;

import java.util.Vector;

/**
 * Document sender to Gestor STP via JMS
 */
public class GatewayCRESTDocumentSender extends GatewayAbstractGestorSTPDocumentSender {
    /**
     * Adapter type
     */
    public static final String ADAPTER_TYPE = "gstpcrest.out";
    private static final String CONFIG_FILE_NAME = "gstpcrest.connection.properties";
    private static final String CONFIG_NAME_GESTOR_STP = "SantanderCRESTJMSQueue";
    private static final String QUERY_MSG_ATTRIBBUTE = "SELECT MAX(ATTR_VALUE) FROM mess_attributes WHERE ATTR_NAME='Crest_UFTQ'";

    protected SantanderCRESTJMSQueueIEAdapter adapter;
    /**
     * Default Constructor
     */
    public GatewayCRESTDocumentSender() {
        super(ADAPTER_TYPE);
    }

    @Override
    public String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    public String getConfigName() {
        return CONFIG_NAME_GESTOR_STP;
    }

    //It is necessary to remove the @@ swift description
    @Override
    protected StringBuffer stripSwiftExtraInfo(final AdviceDocument document) {
        final StringBuffer doc = document.getDocument();
        SwiftMessage.stripExtraInfo(doc);
        return doc;
    }

    private String getCrestUftq(StringBuffer doc, BOMessage message, DSConnection dsCon) {
        String docSetUftq = doc.toString();

        docSetUftq = docSetUftq.replace("UFTQ/NULL", "UFTQ/" + getCrestUFTQAttributeValue(dsCon, message));

        return docSetUftq;
    }


    @Override
    public boolean send(final DSConnection ds, final SenderConfig config, final SenderCopyConfig copyConfig,
                        final long eventId, final AdviceDocument document, final Vector copies, final BOMessage message,
                        final Vector errors, final String engineName, final boolean[] saved) {

        this.helper = new SantExceptions();

        boolean result = false;

        // avoid other event for the same message was taken in other thread
        msgLocker.lock(message.getLongId());
        if (checkAdapterType(errors)) {
            // check if the message status is still usable to send the message
            if (checkMessageStatus(config, ds, message, eventId)) {
                if (checkRelatedMsgSent(message, ds, errors)) {
                    // retrieve the configuration of the adapter
                    if (getCrestConfiguration(errors)) {
                        try {
                            synchronized (this.adapter) {
                                this.adapter.init();
                            }
                            final StringBuffer outputStripSwiftExtraInfo = stripSwiftExtraInfo(document);
                            String output = getCrestUftq(outputStripSwiftExtraInfo, message, ds);
                            String uftqNumber = getCrestUFTQAttributeValue(ds, message);
                            String outputString = validateAndConverEOLFormat(output);
                            // write the message to the queue
                            if (writeMsgIntoQueue(this.adapter, outputString, message, errors)) {
                                // if we have wrote the message to the queue
                                // we need to consume the event event if the
                                // action SEND is not applied
                                result = true;
                                // try to apply action send
                                saveMessage(ds, message, uftqNumber);
                            }
                        } catch (final Exception e) {
                            addException(SantExceptionType.TECHNICAL_EXCEPTION, message.getTradeLongId(), message.getLongId(),
                                    "Message can not be Sent; Exception in gateway ", e, errors);
                        } finally {
                            try {
                                this.adapter.commit();
                                this.adapter.stop();
                            } catch (Exception e) {
                                Log.error(this, "Error while trying to stop adapter", e);
                            }
                        }
                    }
                }
            }

            // Save the tasks if any
            this.helper.publishTasks(ds, eventId, "SenderEngine");
        } else {
            // we wont process the event cause the message's status have been
            // changed
            result = true;
        }
        msgLocker.unlock(message.getLongId());
        return result;
    }

    protected boolean writeMsgIntoQueue(final IEAdapter adapter, final String document, final BOMessage message,
                                        final Vector<String> errors) {
        final boolean result = this.adapter.write(document, message);


        if (!result) {
            addException(SantExceptionType.TECHNICAL_EXCEPTION, message.getTradeLongId(), message.getLongId(),
                    "Message can't be written in the MQ queue", null, errors);
        } else {
            final StringBuilder sb = new StringBuilder("Message ");
            sb.append(message.getLongId());
            sb.append(" sent to ");
            sb.append(this.adapterType);
            Log.info(this, sb.toString());
        }
        return result;
    }

    public synchronized boolean getCrestConfiguration(final Vector<String> errors) {

        initIEAdapter();

        if ((this.config == null) || !this.config.isConfigured(getConfigFileName())) {
            Log.error(this, "GatewayMQDocumentSender::send::" + getConfigName() + " not configured properly ");
            final String error = "Message can not be Sent; Exception in gateway: " + this.adapterType
                    + " not configured properly";
            this.helper.addException(SantExceptionType.CONFIGURATION_EXCEPTION, getConfigName(), error, 0, 0, 0, null,
                    0);

            errors.addElement(error);
            return false;
        } else {
            this.adapter = (SantanderCRESTJMSQueueIEAdapter) this.config.getSenderIEAdapter();
            return true;
        }
    }

    private final void initIEAdapter() {
        if (this.config == null) {
            this.config = IEAdapter.getConfig(getConfigName());
            //Needed to set the right adapter type
            if (this.config instanceof SantanderIEAdapterConfig) {
                ((SantanderIEAdapterConfig) this.config).setAdapterType(getAdapterType());
            }
        }
    }

    public static String getCrestUFTQAttributeValue(DSConnection dsCon, BOMessage message){
        int addOneUftqNumber = 1;
        try {
            Vector<Vector<String>> selectUFTQAttribute = (Vector<Vector<String>>) dsCon.getRemoteAccess().executeSelectSQL(QUERY_MSG_ATTRIBBUTE, null);
            if (selectUFTQAttribute.get(selectUFTQAttribute.size() - 1).toArray()[0] != null) {
                addOneUftqNumber = Integer.parseInt(String.valueOf(selectUFTQAttribute.get(selectUFTQAttribute.size() - 1).toArray()[0])) + 1;
                if(addOneUftqNumber>9999999){
                    addOneUftqNumber = addOneUftqNumber - 9999999;
                }

            }
        } catch (CalypsoServiceException e) {
            org.jfree.util.Log.error(e);
        }
        String uftqNumber = String.format("%07d", addOneUftqNumber);
        return uftqNumber;
    }

    private boolean saveMessage(DSConnection ds, BOMessage message, String uftqNumber){
        message.setAction(Action.SEND);
        try {
            message = (BOMessage) message.clone();
            message.setAttribute("Crest_UFTQ", uftqNumber);
            ds.getRemoteBackOffice().save(message,0,this.getClass().getName());
            return true;
        } catch (Exception e) {
            Log.error(this, "Unable to apply Action SEND to message but the message have been written in the queue",
                    e);
        }
        return false;
    }

}

/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */
package calypsox.engine.dataimport;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.calypso.apps.common.CalypsoMLConfiguration;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.apps.importer.adapter.DefaultImporterAdapter;
import com.calypso.apps.importer.adapter.ImporterAdapter;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventQuote;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.CalypsoSession;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DefaultCalypsoConnection;
import com.calypso.tk.service.DefaultCalypsoSession;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import calypsox.apps.importer.adapter.SantanderDefaultImporterAdapter;
import calypsox.engine.BaseIEEngine;
import calypsox.engine.dataimport.BondElements.ImportBondElements;
import calypsox.engine.importer.ImporterUtil;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventProduct;
import calypsox.tk.util.GenericJMSQueueIEAdapter;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.SantanderIEAdapter;
import calypsox.util.DOMUtility;

/**
 * The Class CalypsoMLIEEngine.
 * <p>
 * This engine handle the CML importing process
 *
 * @author Bruno P.
 * @version 1.0
 * @since 03/18/2011
 */
public class CalypsoMLIEEngine extends BaseIEEngine {

    /**
     * CalypsoML ImporterAdapter
     */
    private ImporterAdapter importerAdapter = null;
    /**
     * CalypsoML CalypsoSession
     */
    private CalypsoSession calypsoSession = null;

    private LinkedList<Object> messageQueue;
    public static final String DOMAIN_VALUE_MSG_TEST = "engineTechnicalTest";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public CalypsoMLIEEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        messageQueue = new LinkedList<>();
    }

    /**
     * Method that initialize the engine
     *
     * @param engineContext context of engine
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {
        setEngineName(engineContext.getEngineName());
        super.init(engineContext);
        initCalypsoMLAdapters();
        logImporterAdapter();
    }

    @Override
    public synchronized boolean start(boolean batch) throws ConnectException {
        boolean started = super.start(batch);
        logStartQueueTrace("Connected to ");
        return started;
    }

    /**
     * Initialize the CalypsoML adapter
     *
     * @return true if everything is correct
     */
    protected boolean initCalypsoMLAdapters() {
        boolean ret = false;
        try {
            final DSConnection dsCon = DSConnection.getDefault();
            final DefaultCalypsoConnection defaultCalypsoConnection = new DefaultCalypsoConnection(dsCon);
            final DefaultCalypsoSession defaultCalypsoSession = new DefaultCalypsoSession(defaultCalypsoConnection);
            final CalypsoMLConfiguration calypsoMLConfiguration = getCalypsoMLConfiguration();

            this.importerAdapter = calypsoMLConfiguration.buildImporterAdapter(defaultCalypsoSession);

            ret = true;
        } catch (final Exception e) {
            Log.error(this, ": Failed to init CalypsoML Importer and Exporter Adapters.", e);
        }
        return ret;
    }

    /**
     * Get the CalypsoML configuration from the property file
     *
     * @return the CalypsoMLConfiguration
     * @throws Exception throw if there is a problem with the property file.
     */
    private synchronized CalypsoMLConfiguration getCalypsoMLConfiguration() throws AdapterException {
        final CalypsoMLConfiguration config = new CalypsoMLConfiguration();
        final String translatorConfigFile = System
                .getProperty(CalypsoMLConfiguration.CALYPSO_XML_TRANSLATORS_CONFIG_FILE);
        final String identifiersConfigFile = System
                .getProperty(CalypsoMLConfiguration.CALYPSO_XML_IDENTIFIERS_CONFIG_FILE);

        if (translatorConfigFile != null) {
            final URL configFileURL = CalypsoMLConfiguration.getFileURL(translatorConfigFile);
            config.addTranslatorsConfigurationFile(configFileURL);
            config.addPersistenceConfigurationFile(configFileURL);
            config.addSelectorTypeConfigurationFile(configFileURL);
            config.addProcessorConfigurationFile(configFileURL);
        }
        if (identifiersConfigFile != null) {
            config.addIdentifiersConfigurationFile(CalypsoMLConfiguration.getFileURL(identifiersConfigFile));
        }
        config.setImporterAdapterClass(getImportAdapterClass());

        return config;
    }

    /**
     * Get the correct ImporterAdapter class, depending on the type of object
     * the Engine is suppose to import (event if it can import whatever CML)
     *
     * @return ImporterAdapter class
     */
    protected Class<? extends DefaultImporterAdapter> getImportAdapterClass() {
        return SantanderDefaultImporterAdapter.class;
    }

    /**
     * Handler incoming message. This is the main method that is called when a
     * new message come from the IEAdapter.
     *
     * @param externalMessage message to handle
     * @return true if everything is find.
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {

        if (externalMessage == null) {

            return true;
        }

        if (Util.isEmpty(externalMessage.getText())) {

            return false;
        } else {
            boolean ret = false;
            JMSQueueAnswer answer = null;
            final List<Task> tasks = new ArrayList<>();
            Object importedObject = null;
            Exception exception = null;
            // Display the message
            logStartQueueTrace("New message from");
            Log.system(this.getClass().getName(), "Received new " + externalMessage.getType() + " message");
            Log.info(this.getClass().getName(), "Message contents  " + externalMessage.getText());


            //PRODUBAN Technical test begins if messages receives TECHNICAL_TEST word at the beginning of the message
            //if (EnginesUtils.technicalProbe(this, getEngineName(),externalMessage.getText()))
            //	return true;

            // Try to import the object
            try {
                if ((externalMessage instanceof JMSQueueMessage)
                        && (this.importerAdapter instanceof SantanderDefaultImporterAdapter)) {
                    final JMSQueueMessage jmsMessage = (JMSQueueMessage) externalMessage;

                    final SantanderDefaultImporterAdapter santanderAdapter = (SantanderDefaultImporterAdapter) this.importerAdapter;
                    // We pass the externalReference to the Adapter because it will
                    // need for a mirror
                    santanderAdapter.setJMSReference(jmsMessage.getReference());
                    santanderAdapter.setTasks(tasks);
                }
                
                // Manage Pool Factor and Call Schedules  
                if (externalMessage.getText().contains("RDFlowXML") || externalMessage.getText().contains("RDFlowTransaction")) {
                	exception = new ImportBondElements().importBondElements(externalMessage.getText());
                	if (exception == null) {
                		Log.system(this.getClass().getName(), "Message import was a success");
                		ret = true;
                	}
                }
                else {
                	// AAP HOT HOT FIX
                	this.importerAdapter.importXML(convertMessageVersion(externalMessage));
                	ret = true;
                	Log.system(this.getClass().getName(), "Message import was a success");
                	if (this.importerAdapter instanceof SantanderDefaultImporterAdapter) {
                		importedObject = ((SantanderDefaultImporterAdapter) this.importerAdapter).getImporterObject();
                		Log.system(this.getClass().getName(), "The following " + importedObject.getClass() + " was imported");
                		
                		// If Bond, always export it when receiving through Queues
                		if (importedObject instanceof Bond) {
                			try {
                				PSEventProduct event = new PSEventProduct();
                				event.setProduct((Bond)importedObject);
                				DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
                			} catch (CalypsoServiceException exc) {
                				Log.error(this.getClass().getSimpleName(), "Couldn't publish event - " + exc.toString());
                			}
                		}
                	}
                }
            } catch (final AdapterException e) {
                exception = e;
                Log.error(this, e + ": " + e.getMessage(), e);
            }

            // Try to send a response to the MiddleWare
            try {
                // TODO for ETT, amend generateAnswer to add on it the UniqueID kw
                answer = generateAnswer(importedObject, exception, externalMessage);

                if (needTask(answer)) {
                    tasks.add(buildTask(answer.getDescription(), answer));
                }

                sendAnswer(answer);
            } catch (final Exception e) {
                tasks.add(buildTask(getExceptionMessage(e), answer));
                Log.error(this, e + ": " + e.getMessage(), e);
            }

            if (!Util.isEmpty(tasks)) {
                publishTask(tasks);
            }
            return ret;
        }

    }

    /**
     * AAP HOTFIX
     */
    private String convertMessageVersion(ExternalMessage message) {
        String version12tag = "version=\"12-0\"";
        String version14tag = "version=\"14-0\"";
        String bond12tag = "<calypso:definition>";
        String bond14tag = "<calypso:bondDefinition>";
        String bond12CloseTag = "</calypso:definition>";
        String bond14CloseTag = "</calypso:bondDefinition>";
        String bondType = "xsi:type=\"calypso:Bond\"";
        String text = message.getText();
        text = text.replaceAll(version12tag, version14tag);
        if (text.contains(bondType)) {
            text = text.replaceAll(bond12tag, bond14tag);
            text = text.replace(bond12CloseTag, bond14CloseTag);
        }
        
        // Fix until Calypso delivers the new release with these fields.
        text = text.replaceAll("<calypso:redemptionAmount>.*?</calypso:redemptionAmount>", "");
        text = text.replaceAll("<calypso:redemptionAmount/>", "");
        text = text.replaceAll("<calypso:interestCleanupB>.*?</calypso:interestCleanupB>", "");
        text = text.replaceAll("<calypso:interestCleanupB/>", "");

        return text;

    }

    /**
     * Do not use
     *
     * @param message message
     * @param tasks
     */
    @Override
    public boolean handleIncomingMessage(final String message, final List<Task> tasks) throws Exception {
        throw new UnsupportedOperationException("Method handleIncomingMessage(String message, List<Task> tasks) should not be used");
    }

    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          object imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return
     */
    protected JMSQueueAnswer generateAnswer(final Object object, final Exception exception,
                                            final ExternalMessage externalMessage) {
        return new JMSQueueAnswer();
    }

    /**
     * send the answer to the MiddleWare
     *
     * @param answer message to send back
     * @throws Exception if there is a sending problem
     */
    private void sendAnswer(final JMSQueueAnswer answer) {
        JMSQueueMessage msg;
        boolean result;
        Log.info(this, answer.toString());
        if (this.getIEAdapterConfig() == null) {
            return;
        }

        final IEAdapter sender = this.getIEAdapterConfig().getSenderIEAdapter();
        if (sender == null) {
            Log.error(this, "Sender is null. Answer = " + answer.toString());
            return;
        }

        messageQueue.addLast(answer);

        while (!messageQueue.isEmpty()) {
            msg = (JMSQueueMessage) messageQueue.getFirst();
            if (sender instanceof SantanderIEAdapter) {
                result = ((SantanderIEAdapter) sender).write(msg);
            } else {

                result = sender.write(msg.toString());
            }

            if (result) {
                messageQueue.removeFirst();
            }
        }
    }

    /**
     * Handle outgoing message. Not used
     *
     * @param event event
     * @param tasks tasks
     * @throw Exception exception
     */
    @Override
    public String handleOutgoingMessage(final PSEvent event, final List<Task> tasks) throws Exception {
        return null;
    }

    /**
     * get the CML importing adaptor
     *
     * @return the CML adapter
     */
    public ImporterAdapter getImporterAdapter() {
        return this.importerAdapter;
    }

    /**
     * set the CML importing adapter
     *
     * @param adapter CML importing adapter
     */
    public void setImporterAdapter(final ImporterAdapter adapter) {
        this.importerAdapter = adapter;
    }

    /**
     * get the session and init it if needed
     *
     * @return the CML importing session
     */
    public CalypsoSession getCalypsoSession() {
        if (this.calypsoSession == null) {
            try {
                this.calypsoSession = new DefaultCalypsoSession(new DefaultCalypsoConnection(getDS()));
            } catch (final Exception e) {
                Log.error(this, e);
            }
        }
        return this.calypsoSession;
    }

    /**
     * Catch PSEventQuote while there is a specific processing in
     * Engine.newEvent().
     */
    @Override
    public void processMarketDataChange(final PSEvent event) {
        super.processMarketDataChange(event);
        if (event instanceof PSEventQuote) {
            process(event);
        }
    }

    /**
     * Processing for Incoming External messages :
     * <p>
     * <li>Get The Message from the adapater</li>
     * <li>Parse the Message if necessary</li>
     * <li>Process the received message in Calypso</li>
     * <li>Acknowledge the message</li>
     * <p>
     * called from IEAdapter.callBackListener if if a parser was found
     */
    @Override
    public boolean newMessage(final IEAdapter adapter, final ExternalMessage msg) {
        if (msg == null) {
            return false;
        }
        boolean proc = false;

        if (this.getIEAdapterConfig() == null) {
            this.setIEAdapterConfig(adapter.getIEAdapterConfig());
        }
        proc = handleIncomingMessage(msg);
        return proc;
    }

    /**
     * generate a task from an specific message
     *
     * @param exception source of the task
     * @param answer    anwser if possible
     */
    protected Task buildTask(final String message, final JMSQueueAnswer answer) {
        Task task = new Task();
        if (answer != null) {
            task = buildTask(message, 0, answer.getETTEventType(), Task.EXCEPTION_EVENT_CLASS);
            if (answer.getTransactionKey() != null) {
                task.setUserComment(answer.getTransactionKey());
            }
        }

        return task;
    }

    /**
     * This method try to get the Murex reference in the CML if the importer
     * failed.
     *
     * @param message original message
     * @return the Murex reference.
     */
    protected String getDirtyReference(final ExternalMessage message) {
        return ImporterUtil.getInstance().getXMLTagValue(message, getDirtyReferenceTag());
    }

    /**
     * This method gets the ETTEventType from the message in case of a NACK.
     *
     * @param text
     * @return
     * @throws Exception
     */
    protected String getETTEventTypeFromMesg(final String text) {
        try {
            final ByteArrayInputStream xmlStream = new ByteArrayInputStream(text.getBytes());
            final Document document = DOMUtility.createDOMDocument(xmlStream);
            final Node node1 = document.getFirstChild().getFirstChild();
            String prefix = node1.getPrefix();
            prefix += ":";

            final NodeList childNodesKW = document.getElementsByTagName(prefix + "keyword");
            for (int j = 0; j < childNodesKW.getLength(); j++) {
                final Node subnode1 = childNodesKW.item(j).getFirstChild();
                if ((subnode1.getNodeType() == Node.ELEMENT_NODE) && subnode1.getNodeName().equals(prefix + "name")) {
                    if (subnode1.getFirstChild().getTextContent().equals("ETTEventType")) {
                        final Node value = subnode1.getNextSibling().getFirstChild();
                        final String str = value.getTextContent();
                        return str;
                    }
                }
            }
        } catch (final Exception exc) {
            Log.system(this.getClass().getName(), "Error getting ETTEventType From Message.", exc);
        }
        return "";
    }

    /**
     * get the tag used for the dirty reference
     *
     * @return the tag value
     */
    protected String getDirtyReferenceTag() {
        return null;
    }

    /**
     * We only publish in Task Station if answer is not OK
     *
     * @param answer
     * @return
     */
    public boolean needTask(final JMSQueueAnswer answer) {
        return ((answer != null) && !JMSQueueAnswer.OK.equals(answer.getCode()));
    }

    /**
     * @param logMessage
     */
    public void logStartQueueTrace(String logMessage) {
        Optional<IEAdapter> ieAdapterOptional = Optional.of(this.getIEAdapter());
        IEAdapter receiverIeAdapter = ieAdapterOptional.map(IEAdapter::getIEAdapterConfig).map(IEAdapterConfig::getReceiverIEAdapter).orElse(null);
        if (receiverIeAdapter instanceof GenericJMSQueueIEAdapter && ((GenericJMSQueueIEAdapter) receiverIeAdapter).getReceiverQueueName() != null) {
            Log.system(this.getClass().getSimpleName(), this.getClass().getSimpleName() + " --> " + logMessage + " " + ((GenericJMSQueueIEAdapter) receiverIeAdapter).getQueueReceiver().toString());
        }
    }

    /**
     *
     */
    public void logImporterAdapter() {
        if (this.getImporterAdapter() != null) {
            Log.system(this.getClass().getSimpleName(), "[" + System.currentTimeMillis() + "] " + this.getClass().getSimpleName() + " --> Importer Adapter: " + this.getImporterAdapter().getClass().getSimpleName());
        }
    }
    
    

}

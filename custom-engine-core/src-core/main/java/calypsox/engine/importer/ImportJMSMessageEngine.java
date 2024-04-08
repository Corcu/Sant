package calypsox.engine.importer;

import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.util.FutureTaskThreadPool;
import calypsox.tk.util.MessageHandler;
import calypsox.tk.util.MessageHandlerFactory;
import com.calypso.engine.advice.ImportMessageEngine;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;

import java.util.List;

/**
 * the ImportJMSMessageEngine engine which is used to implement several
 * interfaces like gstp, payplus, doddFrank, etc.
 */
public class ImportJMSMessageEngine extends ImportMessageEngine {

    private final String adapterType;
    private IEAdapter adapter;

    /**
     * constant ADAPTER_TYPE.
     */
    public static final String ADAPTER_TYPE = "ADAPTER_TYPE";

    private final int nbThreads;

    /**
     * Thread pool to process the incoming messages.
     */
    protected FutureTaskThreadPool<Exception>[] threadPools;

    /**
     * Creates a new ImportJMSMessageEngine.
     *
     * @param adapterType adapter type (i.e. gstp.in)
     * @param configName  config name: the class which will be used to manage the
     *                    configuration is selected by reflection using this name
     * @param dsCon       data server connection
     * @param hostName    event server's hostname
     * @param esPort      event server's port
     */
    @SuppressWarnings("unchecked")
    public ImportJMSMessageEngine(final String adapterType,
                                  final String configName, final DSConnection dsCon,
                                  final String hostName, final int esPort) {
        super(configName, dsCon, hostName, esPort);
        this.adapterType = adapterType;
        this.nbThreads = getEngineParam(THREAD_COUNT, null, 1);

        this.threadPools = new FutureTaskThreadPool[this.nbThreads];
        for (int i = 0; i < this.nbThreads; i++) {
            this.threadPools[i] = new FutureTaskThreadPool<Exception>(1);
        }
    }

    public ImportJMSMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        this.adapterType = getEngineParam("type", null, "gstp.in");
        this.nbThreads = getEngineParam(THREAD_COUNT, null, 1);

        this.threadPools = new FutureTaskThreadPool[this.nbThreads];
        for (int i = 0; i < this.nbThreads; i++) {
            this.threadPools[i] = new FutureTaskThreadPool<Exception>(1);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.engine.advice.ImportMessageEngine#handleIncomingMessage(com
     * .calypso.tk.bo.ExternalMessage)
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
        Log.info(this, "ImportJMSMessageEngine::entered with message:\n"
                + externalMessage.getText());

        MessageHandler messageHandler;
        BOMessage parsedMessage;
        try {
            messageHandler = MessageHandlerFactory
                    .getMessageHandler(this.adapterType);
            parsedMessage = messageHandler.getParsedMessage(externalMessage
                    .getText());
        } catch (final Exception e) {
            Log.error(this,
                    "ImportJMSMessageEngine::handleIncomingMessage::Exception="
                            + e, e);
            final String msg = "The message handler for the adapter type \""
                    + this.adapterType
                    + "\" failed to parse the incoming message";
            Log.error(this, msg, e);
            ImporterUtil.getInstance().publishJMSImportExceptionTask(
                    SantExceptionType.JMS_IMPORTER_PARSE, null, msg,
                    getEngineName());

            return false;
        }

        final int threadPoolNumber = getThreadPoolNumber(parsedMessage.getLongId());

        final JMSMessageImporterCalleableInstance importerCalleableInstance = new JMSMessageImporterCalleableInstance(
                externalMessage, getDS(), getEngineName(), this.adapter,
                this.adapterType, parsedMessage, messageHandler);
        Log.info(this, "Message will be processed in thread nb: "
                + threadPoolNumber);

        this.threadPools[threadPoolNumber].addTask(importerCalleableInstance);

        return true;

    }

    /**
     * Get the number of the thread to use in order to prevent multi acces to
     * the same object. Object are identified by id
     *
     * @param id
     * @return
     */
    private int getThreadPoolNumber(final long id) {
        return Math.toIntExact(id % this.nbThreads);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.engine.advice.ImportMessageEngine#newMessage(com.calypso.
     * tk.util.IEAdapter, com.calypso.tk.bo.ExternalMessage)
     */
    @Override
    public boolean newMessage(final IEAdapter adapter,
                              final ExternalMessage message) {
        this.adapter = adapter.getIEAdapterConfig().getReceiverIEAdapter();
        return handleIncomingMessage(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.engine.advice.ImportMessageEngine#getEngineName()
     */
    @Override
    public String getEngineName() {
        return "SANT_ImportJMSMessageEngine_" + this.adapterType;
    }

    /**
     * returns the adapter used to establish the communications with the JMS
     * system.
     *
     * @return IEAdapter
     */
    public IEAdapter getIEAdapter() {
        return this.adapter;
    }

    /**
     * set the adapter used to establish the communications with the JMS system.
     * Used mainly for testing purposes
     *
     * @param simulatorIEAdapter an IEAdapter
     */
    public void setIEAdapter(final IEAdapter simulatorIEAdapter) {
        this.adapter = simulatorIEAdapter;

    }

    /**
     * wait until all messages have been processed.
     */
    public void waitAllMessages() {
        for (int i = 0; i < this.threadPools.length; i++) {
            this.threadPools[i].waitForAllTasksToBeCompleted();

            final List<Exception> list = this.threadPools[i].getExceptions();
            for (int j = 0; j < list.size(); j++) {
                Log.info(this, list.get(j));
            }
        }
    }

}

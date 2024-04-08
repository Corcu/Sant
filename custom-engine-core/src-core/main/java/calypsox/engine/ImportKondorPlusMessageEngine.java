package calypsox.engine;

import calypsox.engine.advice.ImportMessageEngine;
import calypsox.tk.bo.handler.KondorPlusIncomingMessageHandler;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.ExtendedIEAdapterListener;
import calypsox.util.KondorPlusUtilities;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.TaskArray;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImportKondorPlusMessageEngine extends ImportMessageEngine implements ExtendedIEAdapterListener {

    private static final String TIME_OUT = "TIME_OUT";
    private static final String ENGINE_NAME = "SANT_ImportMessageEngine_Murex";

    //Processing delay stuff
    private static final String PROCESSING_DELAYTIME_DV_NAME = "KondorPlusIncomingProcessingDelay";
    private static final Integer DEFAULT_DELAY_TIME_MS = 5000;
    private static final String DELAY_ACTIVATOR = "EnableKondorPlusIncomingProcessingDelay";
    boolean needsDelay = true;

    private IEAdapter adapter;
    //Constant
    private static final String MUREX_ID = "MxID";
    private static final String DEAL_ID = "DealID";
    private static final String LOT_SIZE = "LotSize";
    //default
    private Integer incomingMsgProcessingDelay = DEFAULT_DELAY_TIME_MS;
    //Trade Action
    private static final String MC_ERROR = "MC_ERROR";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public ImportKondorPlusMessageEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    /*
     *
     *
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {
        super.init(engineContext);

        Log.info(this.getClass(), "Custom Log: Initializing MCLiquidation Engine...");
        // Initialize the adapter type in the Default. Used by so IEAdapter

        if (Util.isEmpty(getIeAdapterConfigName())) {
            throw new InvalidParameterException("Empty config name: cannot read Engine properties.");
        }
        initDelayTimeValue();
        initNeedsDelayFlag();
        // Migracion V14 22/01/2015
        setEngineName(ENGINE_NAME);
    }

    private void initDelayTimeValue() {
        List<String> delayTimeDVs = LocalCache.getDomainValues(getDS(), PROCESSING_DELAYTIME_DV_NAME);
        if (!Util.isEmpty(delayTimeDVs)) {
            incomingMsgProcessingDelay = Integer.parseInt(delayTimeDVs.get(0));
        }
    }

    private void initNeedsDelayFlag() {
        List<String> delayTimeFlagDVs = LocalCache.getDomainValues(getDS(), DELAY_ACTIVATOR);
        if (!Util.isEmpty(delayTimeFlagDVs)) {
            needsDelay = Boolean.parseBoolean(delayTimeFlagDVs.get(0));
        }
    }


    @Override
    public boolean handleIncomingMessage(ExternalMessage externalMessage) {
        Log.system(this.toString(), "Received message:\n" + externalMessage.getText());
        if (needsDelay) {
            processIncomingMsg(externalMessage);
        } else {
            processIncomingMsgNoDelay(externalMessage);
        }
        return true;
    }

    private void processIncomingMsgNoDelay(ExternalMessage externalMessage) {
        //Original code
        BOMessage parsedMessage = getParsedMessage(externalMessage);
        saveMessage(parsedMessage);
        //V6.2 refactor
        handleMrxMarginCallTrade(parsedMessage, externalMessage);
    }

    /**
     * RXJava timer, for incoming processing delay
     *
     * @param externalMessage
     */
    private void processIncomingMsg(ExternalMessage externalMessage) {
        Completable.timer(incomingMsgProcessingDelay, TimeUnit.MILLISECONDS, Schedulers.computation()).subscribe(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                //Original code
                BOMessage parsedMessage = getParsedMessage(externalMessage);
                saveMessage(parsedMessage);
                //V6.2 refactor
                handleMrxMarginCallTrade(parsedMessage, externalMessage);
            }
        });
    }

    private void publishExceptionTask(String configName, BOMessage message, String comment, String eventType) {
        Task task = buildTask(message != null ? message.getTradeLongId() : 0L, comment,
                message != null ? message.getLongId() : 0L, eventType, Task.TRADE_EVENT_CLASS);
        ArrayList<Task> taskList = new ArrayList<Task>();
        taskList.add(task);
        publishTask(taskList);
    }

    protected void publishTask(List<Task> tasks) {
        try {
            getDS().getRemoteBO().saveAndPublishTasks(new TaskArray(tasks), 0, getEngineName());
        } catch (RemoteException e) {
            Log.error(this, "Failed to saveAndPublishTasks: " + tasks, e);
        }
    }

    protected Task buildTask(long tradeId, String comment, long messageId, String eventType, String eventClass) {
        Task task = new Task();
        task.setObjectLongId(messageId);
        task.setTradeLongId(tradeId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(getEngineName());
        task.setComment(comment);
        return task;
    }

    @Override
    protected void poststop(boolean willTerminate) {
        try {
            if (this.adapter != null)
                this.adapter.stop();
            super.poststop(willTerminate);
        } catch (Exception e) {
            Log.error(this, e); //sonar 02/11/2017
            Log.error(this, "Exception while closing connection");
        }
    }

    @Override
    public boolean newMessage(IEAdapter adapter, ExternalMessage message) {
        this.adapter = adapter.getIEAdapterConfig().getReceiverIEAdapter();
        return handleIncomingMessage(message);
    }

    public boolean writeResponse(String response) {
        boolean result = this.adapter.write(response);
        Log.debug(this, "WriteResponse::result= " + result + " & message=" + response);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#setIEAdapter(com.calypso.tk.
     * util.IEAdapter)
     */
    @Override
    public void setIEAdapter(IEAdapter adapter) {
        this.adapter = adapter;

    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.ExtendedIEAdapterListener#getIEAdapter()
     */
    @Override
    public IEAdapter getIEAdapter() {
        return this.adapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#writeMessage(com.calypso.tk.
     * util.IEAdapter, java.lang.String)
     */
    @Override
    public boolean writeMessage(IEAdapter adapter, String message) {
        if (this.adapter == null) {
            this.adapter = adapter;
        }

        boolean ret = false;
        if (Util.isEmpty(message)) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            ret = adapter.getIEAdapterConfig().getSenderIEAdapter().write(message);
        }
        return ret;
    }

    /**
     * @param externalMessage
     * @return parsed message from external message
     */
    private BOMessage getParsedMessage(final ExternalMessage externalMessage) {
        BOMessage parsedMessage = null;
        KondorPlusIncomingMessageHandler handler = new KondorPlusIncomingMessageHandler();
        try {
            parsedMessage = handler.parseMessage(externalMessage.getText());

        } catch (Exception e) {
            Log.error(DelayedImportKondorPlusMessageEngine.class, "Failed to process message: " + e.getMessage(), e);
        }
        return parsedMessage;
    }

    /**
     * Retrieve BOMessage from DataServer, sets the description and entered
     * user, and try to save the message
     *
     * @param parsedMessage
     */
    private void saveMessage(BOMessage parsedMessage) {
        BOMessage messageToSave = null;
        try {
            messageToSave = getDS().getRemoteBackOffice().getMessage(parsedMessage.getLongId());

            if (messageToSave != null) {

                messageToSave.setDescription(parsedMessage.getDescription());
                messageToSave.setEnteredUser(getDS().getUser());

                if (isBOMessageActionApplicable(messageToSave, parsedMessage.getAction())) {
                    messageToSave.setAction(parsedMessage.getAction());
                    saveBOMessage(messageToSave, DelayedImportKondorPlusMessageEngine.ENGINE_NAME);
                } else {
                    Log.error(this, "Could not apply action " + parsedMessage.getAction().toString() + " to message with id: " + parsedMessage.getLongId());
                }

                if (parsedMessage.getAction().equals(Action.NACK)
                        || parsedMessage.getAction().toString().equals(TIME_OUT)) {
                    // Raise a Task with the error message
                    publishExceptionTask(getEngineName(), messageToSave, messageToSave.getDescription(), "ERROR_TRADE");
                }
            } else {
                Log.error(this, "Cannot find message with id=" + parsedMessage.getLongId());
            }

        } catch (RemoteException e) {
            Log.error(this, "Error processing message with id=" + parsedMessage.getLongId(), e);
        }
    }

    /**
     * Checks if the BO message action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, getDS(), null);
    }

    /**
     * Saves the BO message.
     *
     * @param message    the bo message
     * @param engineName the engine name
     * @return the bo message id
     */
    protected void saveBOMessage(final BOMessage message, final String engineName) {
        long id = 0;
        try {
            id = DSConnection.getDefault().getRemoteBO().save(message, 0, engineName);
        } catch (CalypsoServiceException e) {
            Log.error(this, e); //sonar 02/11/2017
            Log.error(this, e.getMessage());
        }
        if (id > 0) {
            Log.info(this, "Message saved with id=" + id);
        }
    }

    /**
     * Create and fill MurexID keyword in trade
     *
     * @param externalMessage
     * @return the contract id
     * @throws CalypsoServiceException
     * @throws Exception
     */
    private void setAllMurexId(Trade trade, ExternalMessage externalMessage) {
        try {
            KondorPlusIncomingMessageHandler handler = new KondorPlusIncomingMessageHandler();
            String status = handler.parseStatus(externalMessage.getText());
            String contractId = handler.parseContractId(externalMessage.getText());
            String dealId = handler.parseDealId(externalMessage.getText());
            String lotSize = handler.parseLotSize(externalMessage.getText());

            trade.addKeyword(MUREX_ID, contractId);
            trade.addKeyword(DEAL_ID, dealId);
            trade.addKeyword(LOT_SIZE, lotSize);

            Trade cloneTrade = trade.clone();
            if (CollateralStaticAttributes.ERROR_AC.equals(status)
                    && Status.SENT.equals(trade.getStatus().toString())) {
                cloneTrade.setAction(Action.valueOf(MC_ERROR));
            } else {
                cloneTrade.setAction(Action.AMEND);
            }

            DSConnection.getDefault().getRemoteTrade().save(cloneTrade);
        } catch (Exception exc) {
            Log.error(KondorPlusIncomingMessageHandler.class.getName(), "Murex Values have not been correctly parsed", exc.getCause());
        }

    }


    /**
     * V6.2 refactor
     *
     * @param parsedMessage
     * @param externalMessage
     */
    private void handleMrxMarginCallTrade(BOMessage parsedMessage, ExternalMessage externalMessage) {
        try {
            Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(parsedMessage.getTradeLongId());
            if (isMrxMarginCallTrade(trade)) {
                MarginCall mc = (MarginCall) trade.getProduct();
                if (mc.getSecurity() != null) {
                    setAllMurexId(trade, externalMessage);
                }
            }
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass(), "Unable to load Trade", exc.getCause());
        }
    }

    /**
     * V6.2 trade NullPointer Fix
     *
     * @param trade
     * @return true if matches conditions
     */
    private boolean isMrxMarginCallTrade(Trade trade) {
        boolean res = false;
        if (trade != null) {
            res = KondorPlusUtilities.isSLB(trade) && trade.getProduct() instanceof MarginCall;
        }
        return res;
    }
}

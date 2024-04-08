package calypsox.engine.scheduling;

import calypsox.engine.DelayedImportKondorPlusMessageEngine;
import calypsox.tk.bo.handler.KondorPlusIncomingMessageHandler;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.KondorPlusUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author aalonsop
 * Same code as before :S, different execution approach
 */
public class IncomingKondorPlusProcessingJob implements Callable<Boolean> {

    //Constant
    private static final String MUREX_ID = "MxID";
    private static final String DEAL_ID = "DealID";
    private static final String LOT_SIZE = "LotSize";

    //Trade Action
    private static final String MC_ERROR = "MC_ERROR";
    private static final String TIME_OUT = "TIME_OUT";

    private ExternalMessage externalMessage;


    /**
     * @param externalMessage
     */
    public IncomingKondorPlusProcessingJob(ExternalMessage externalMessage) {
        this.externalMessage = externalMessage;
    }

    @Override
    public Boolean call() throws Exception {
        boolean res = true;
        Log.system(this.toString(), "Received message:\n" + externalMessage.getText());
        BOMessage parsedMessage = getParsedMessage(externalMessage);
        if (parsedMessage != null) {
            saveMessage(parsedMessage);
            //V6.2 refactor
            handleMrxMarginCallTrade(parsedMessage, externalMessage);
        }
        return res;
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
     * Retrieve BOMessage from DataServer, sets the description and entered
     * user, and try to save the message
     *
     * @param parsedMessage
     */
    private void saveMessage(BOMessage parsedMessage) {
        BOMessage messageToSave = null;
        try {
            messageToSave = DSConnection.getDefault().getRemoteBackOffice().getMessage(parsedMessage.getLongId());

            if (messageToSave != null) {

                messageToSave.setDescription(parsedMessage.getDescription());
                messageToSave.setEnteredUser(DSConnection.getDefault().getUser());

                if (isBOMessageActionApplicable(messageToSave, parsedMessage.getAction())) {
                    messageToSave.setAction(parsedMessage.getAction());
                    saveBOMessage(messageToSave, DelayedImportKondorPlusMessageEngine.ENGINE_NAME);
                } else {
                    Log.error(this, "Could not apply action " + parsedMessage.getAction().toString() + " to message with id: " + parsedMessage.getLongId());
                }

                if (parsedMessage.getAction().equals(Action.NACK)
                        || parsedMessage.getAction().toString().equals(TIME_OUT)) {
                    // Raise a Task with the error message
                    publishExceptionTask(messageToSave, messageToSave.getDescription(), "ERROR_TRADE");
                }
            } else {
                Log.error(this, "Cannot find message with id=" + parsedMessage.getLongId());
            }

        } catch (RemoteException e) {
            Log.error(this, "Error processing message with id=" + parsedMessage.getLongId(), e);
        }
    }

    /**
     * Saves the BO message.
     *
     * @param message    the bo message
     * @param engineName the engine name
     * @return the bo message id
     */
    protected void saveBOMessage(final BOMessage message, final String engineName) {
        long id = 0L;
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
     * Checks if the BO message action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }

    private void publishExceptionTask(BOMessage message, String comment, String eventType) {
        Task task = buildTask(message != null ? message.getTradeLongId() : 0L, comment,
                message != null ? message.getLongId() : 0L, eventType, Task.TRADE_EVENT_CLASS);
        ArrayList<Task> taskList = new ArrayList<>();
        taskList.add(task);
        publishTask(taskList);
    }

    /**
     * @param tasks
     */
    protected void publishTask(List<Task> tasks) {
        try {
            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(new TaskArray(tasks), 0, DelayedImportKondorPlusMessageEngine.ENGINE_NAME);
        } catch (RemoteException e) {
            Log.error(this, "Failed to saveAndPublishTasks: " + tasks, e);
        }
    }

    /**
     * @param tradeId
     * @param comment
     * @param messageId
     * @param eventType
     * @param eventClass
     * @return
     */
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
        task.setSource(DelayedImportKondorPlusMessageEngine.ENGINE_NAME);
        task.setComment(comment);
        return task;
    }


}

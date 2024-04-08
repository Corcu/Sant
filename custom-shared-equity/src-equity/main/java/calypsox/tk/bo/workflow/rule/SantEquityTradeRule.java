package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SantEquityTradeRule implements WfTradeRule {

    /** WHERE clause for not COMPLETE tasks */
    private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");

    /** Code format */
    private static final String FORMAT_EXCEPTION_MODIFICATION_AMEND = "Trade %s, does not pass the validation for being modified (AMEND) and trade already settled.";
    private static final String FORMAT_EXCEPTION_MODIFICATION_CANCELED = "Trade %s, does not pass the validation for being modified (CANCEL) and trade already settled.";
    private static final String FORMAT_EXCEPTION_DEFAULT= "Trade %s, does not pass the validation, without identifying case.";

    private static final String EXCEPTION_TYPE_MODIFICATION_AMEND = "EX_MODIFICATION_AMEND_LIQUIDATED";
    private static final String EXCEPTION_TYPE_MODIFICATION_CANCEL = "EX_MODIFICATION_CANCEL_LIQUIDATED";

    /** Task messages */
    private static final String COMMENT_TASK_COMPLETED = "Task completed.";

    /** Text Messages */
    private static final String EMPTY_SPACE = "";

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean rst = true;

        if (trade!=null) {
            Log.debug(this, "Trade obtained for validation: " + trade.getLongId());
            // Validation trade
            final ExceptionItem exception = validateTrade(trade, wc);

            if (exception!=null){
                //Generate Tasks to Task Station with type of exception
                createTasks(dsCon, exception);
            }

        }
        return rst;
    }

    @Override
    public String getDescription() {
        return "Validate Equity at status VERIFIED, if is modification (AMEND or CANCEL)of settled trade." +
                "If dont pass the validation, generate a Task Station, type EX_VALIDATION_EQUITY.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    /**
     * Search for trade with Exception or Valitation.
     *
     * @return
     */
    protected ExceptionItem validateTrade(final Trade trade, final TaskWorkflowConfig wc) {

        Log.debug(this, "Start to check the validation for trades.");
        ExceptionItem result = new ExceptionItem();

        // Exception: Trades Predated
        if (("PENDING_CANCELED").equalsIgnoreCase(String.valueOf(wc.getStatus()))) {
            result.setTradeIdException(trade.getLongId());
            result.setDescriptionException(EXCEPTION_TYPE_MODIFICATION_CANCEL);
        } else if(("PENDING_AMEND").equalsIgnoreCase(String.valueOf(wc.getStatus()))) {
            result.setTradeIdException(trade.getLongId());
            result.setDescriptionException(EXCEPTION_TYPE_MODIFICATION_AMEND);
        }

        return result;
    }

    /**
     * createTasks if is necessary
     *
     * @param ds
     * @param exception
     */
    protected void createTasks(final DSConnection ds, final ExceptionItem exception) {
        final Long tradeId = exception.getTradeIdException();
        final String descriptionException = exception.getDescriptionException();
        // Check if exists active Tasks state != COMPLETED
        final TaskArray tasksNotCompleted = getActiveTasksEquityException(ds, tradeId, descriptionException, WHERE_TASKS_NOT_COMPLETED.toString());
        if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
            completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
        }
        // Create new task exception
        createTaskException(ds, tradeId, exception);
    }

    /**
     * Create Task Exception.
     *
     * @param tradeId
     */
    protected void createTaskException(final DSConnection ds, final long tradeId, final ExceptionItem item) {

        Trade trade = null;
        try {
            trade = ds.getRemoteTrade().getTrade(tradeId);
        }
        catch (final RemoteException e) {
            Log.error(this, String.format("Could not get the trade \"%s\"", tradeId), e);
        }

        if(trade==null) {
            Log.error(this, "Trade is null");
            return;
        }

        String message = getTaskComment(tradeId, item);
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType(item.getDescriptionException());
        taskException.setComment(message);
        taskException.setTradeId(trade.getLongId());
        taskException.setBookId(trade.getBookId());
        taskException.setPriority(Task.PRIORITY_HIGH);

        TaskArray task = new TaskArray();
        task.add(taskException);
        try {
            ds.getRemoteBackOffice().saveAndPublishTasks(task,0L,null);
        }
        catch (CalypsoServiceException e) {
            Log.error(this, "Could not save the exception task.");
        }

    }

    /**
     * Get the message for the Task Comment.
     *
     * @param tradeId
     * @param item
     * @return
     */
    protected String getTaskComment(final long tradeId, final ExceptionItem item) {

        String taskComment = EMPTY_SPACE;

        int i = getCase(item);

        switch (i) {
            case 1:  taskComment = String.format(FORMAT_EXCEPTION_MODIFICATION_AMEND, tradeId);
                break;
            case 2:  taskComment = String.format(FORMAT_EXCEPTION_MODIFICATION_CANCELED, tradeId);
                break;
            case 3:  taskComment = String.format(FORMAT_EXCEPTION_DEFAULT, tradeId);
                break;
        }

        return taskComment;
    }

    /**
     * Get the type of Exception
     *
     * @param item
     * @return
     */
    private int getCase(ExceptionItem item) {
        // Default case
        int out = 3;
        String description = item .getDescriptionException();

        if(EXCEPTION_TYPE_MODIFICATION_AMEND.equals(description)){
            out = 1;
        }
        if(EXCEPTION_TYPE_MODIFICATION_CANCEL.equals(description)){
            out = 2;
        }

        return out;
    }

    /**
     * Get tasks the trade have just created a Task previously
     *
     * @param tradeId
     * @return
     */
    protected TaskArray getActiveTasksEquityException(final DSConnection ds, final long tradeId, final String descriptionException, final String addClause) {
        TaskArray tasks = new TaskArray();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade_id = ").append(tradeId);
        whereClause.append(" AND ");
        whereClause.append("event_class = 'Exception'");
        whereClause.append(" AND ");
        whereClause.append("event_type = '").append(descriptionException).append("'");

        if (addClause != null && !addClause.isEmpty()) {
            whereClause.append(addClause);
        }

        try {
            tasks = ds.getRemoteBO().getTasks(whereClause.toString(), null);
        }
        catch (final RemoteException e) {
            Log.error(this, String.format("Error retrieving tasks from BBDD."), e);
        }

        return tasks;
    }

    /**
     * complete tasks and save it
     *
     * @param tasks
     *            task to complete
     * @param comment
     *            comment
     * @throws RemoteException
     */
    public void completeAndSaveTask(final DSConnection ds, final TaskArray tasks, final String comment) {
        if (tasks != null) {
            for (int i = 0; i < tasks.size(); i++) {
                final Task task1 = tasks.get(i);
                if(task1!=null) {
                    Task task2 = null;
                    try {
                        task2 = (Task) task1.cloneIfImmutable();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }

                    if (task2 != null) {
                        task2.setOwner(ds.getUser());
                        task2.setCompletedDatetime(new JDatetime());
                        task2.setStatus(Task.COMPLETED);
                        task2.setUserComment(comment);
                        saveTask(ds, task2);
                    }
                }
            }
        }
    }

    /**
     * Save one task
     *
     * @param task
     */
    protected void saveTask(final DSConnection ds, final Task task) {
        try {
            ds.getDefault().getRemoteBO().save(task);
        } catch (RemoteException e) {
            Log.error("Error saving task for exception = " + task.getId() + "\n", e);
        }

    }


    class ExceptionItem {

        long tradeIdException;
        String descriptionException;

        public ExceptionItem() {
            tradeIdException = 0;
            descriptionException = "";
        }

        public long getTradeIdException() {
            return tradeIdException;
        }

        public String getDescriptionException() {
            return descriptionException;
        }

        public void setTradeIdException(long tradeIdException) {
            this.tradeIdException = tradeIdException;
        }

        public void setDescriptionException(String descriptionException) {
            this.descriptionException = descriptionException;
        }

    }

}

package calypsox.tk.util.exception;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.List;

public class SantExceptionUtil {
    protected DSConnection ds;
    protected long objectId;
    protected JDatetime valuationDateTime;
    protected JDatetime datetime;
    protected JDatetime undoDatetime;
    protected TaskArray exceptionTasks;

    public SantExceptionUtil(DSConnection ds, long objectId,
                             JDatetime valuationDateTime, JDatetime datetime,
                             JDatetime undoDatetime) {

        this.exceptionTasks = new TaskArray();
        this.ds = ds;
        this.objectId = objectId;
        this.valuationDateTime = valuationDateTime;
        this.datetime = datetime;
        this.undoDatetime = undoDatetime;
    }

    public void addExceptionToTask(SantExceptionType exceptionType,
                                   String source, long tradeId, String message) {

        SantException ex = new SantException(exceptionType, source, message);
        addExceptionToTask(ex, tradeId);
    }

    public void addExceptionToTask(SantExceptionType exceptionType,
                                   long tradeId, String message, Throwable t) {

        SantException ex = new SantException(exceptionType, message, t);
        addExceptionToTask(ex, tradeId);
    }

    public void addExceptionToTask(SantExceptionType exceptionType,
                                   String source, long tradeId, Throwable t) {

        SantException ex = new SantException(exceptionType, source, t);
        addExceptionToTask(ex, tradeId);
    }

    public void addExceptionToTask(SantException santException, long tradeId) {
        Task task = new Task();
        task.setObjectLongId(objectId);
        task.setEventClass(santException.getTaskEventClass());
        task.setNewDatetime(valuationDateTime);
        task.setUnderProcessingDatetime(datetime);
        task.setUndoTradeDatetime(undoDatetime);
        task.setDatetime(datetime);
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setTradeLongId(tradeId);
        task.setStatus(Task.NEW);
        task.setSource(santException.getSource());
        task.setComment(santException.getMessage());
        task.setEventType(santException.getTaskEventType());
        exceptionTasks.add(task);

        // This is used on Control-M console
        System.out.println("Exception sent to the task station: "
                + task.getEventType() + ": " + task.getComment());
    }

    public void publishTasks() {
        try {
            if (exceptionTasks.size() > 0) {
                Log.info(this, "Sending " + exceptionTasks.size()
                        + " exceptions to the task station");
                ds.getRemoteBO().saveAndPublishTasks(exceptionTasks, 0, null);
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
    }

    public boolean hasExceptions() {
        return (exceptionTasks.size() > 0);
    }

    public void addExceptionsToTaskArray(List<SantException> exceptions) {
        for (SantException ex : exceptions)
            addExceptionToTask(ex, 0);
    }
}

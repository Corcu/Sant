package calypsox.tk.collateral.marginCall.logger;

import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportConstants;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class MarginCallLoggerHelper {
    public static final String EVENT_CLASS = "Collateral";

    /**
     * @param comment
     * @param objectId
     * @param executionId
     * @param eventType
     * @param eventClass
     * @param source
     * @return
     */
    protected static Task buildTask(final String comment, final long objectId,
                                    int bookId, Long executionId, final String eventType,
                                    final String eventClass, String source, String internalReference) {
        final Task task = new Task();
        task.setObjectLongId(objectId);
        // task.setTradeLongId(tradeId);
        task.setBookId(bookId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setLinkId((executionId == null ? 0 : executionId));
        task.setSource(source);
        task.setInternalReference(internalReference);
        task.setComment(comment);
        return task;
    }

    /**
     * @param mc
     * @param errors
     */
    public static List<Task> getLogsAsTasks(MarginCall mc, String eventType,
                                            Long executionId, List<String> errors) {
        List<Task> tasks = new ArrayList<Task>();
        if (Util.isEmpty(errors)) {
            return null;
        }
        int objectId = (mc != null ? mc.getId() : 0);
        for (String error : errors) {
            Log.error(MarginCallLoggerHelper.class, error);
            final Task task = buildTask(error, objectId, 0, executionId,
                    eventType, EVENT_CLASS,
                    ExternalMarginCallImportConstants.TASK_SOURCE_OPTIMIZER,
                    null);
            tasks.add(task);
        }

        return tasks;
    }

    /**
     * @param mc
     * @param errors
     */
    public static Task getLogAsTask(MarginCall mc, String eventType,
                                    Long executionId, String internalReference, String error) {
        if (Util.isEmpty(error)) {
            return null;
        }
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        return getLogsAsTasks(mc, eventType, executionId, errors).get(0);
    }

    /**
     * @param source
     * @param tasks
     */
    public static void publishTask(String source, List<Task> tasks) {

        if (Util.isEmpty(tasks)) {
            return;
        }
        try {
            DSConnection.getDefault().getRemoteBO()
                    .saveAndPublishTasks(new TaskArray(tasks), 0, source);
            Log.info(Log.CALYPSOX, tasks.size()
                    + " task(s) have been saved and published");
        } catch (RemoteException e) {
            Log.error(Log.CALYPSOX, "Failed to save and publish task");
            Log.error(Log.CALYPSOX, e);
        }

    }

    /**
     * @param source
     * @param task
     */
    public void publishTask(String source, Task task) {
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(task);
        publishTask(source, tasks);
    }
}

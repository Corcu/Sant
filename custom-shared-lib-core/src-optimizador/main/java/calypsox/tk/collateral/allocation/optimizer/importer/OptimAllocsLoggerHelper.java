/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.collateral.allocation.optimizer.importer;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aela
 */
public class OptimAllocsLoggerHelper {

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
    protected static Task buildTask(final String comment, final long objectId, int bookId,
                                    Long executionId, final String eventType, final String eventClass,
                                    String source, String internalReference) {
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
     * @param entry
     * @param errors
     */
    public static List<Task> getLogsAsTasks(MarginCallEntry entry, String eventType,
                                            Long executionId, List<String> errors) {
        int entryBookId = (entry.getBook() == null ? 0 : entry.getBook().getId());
        List<Task> tasks = new ArrayList<Task>();
        if (Util.isEmpty(errors)) {
            return null;
        }
        int objectId = (entry != null ? entry.getCollateralConfigId() : 0);
        for (String error : errors) {
            Log.error(OptimAllocsLoggerHelper.class, error);
            tasks.add(buildTask(error, objectId, entryBookId, executionId, eventType,
                    EVENT_CLASS,
                    OptimAllocsImportConstants.TASK_SOURCE_OPTIMIZER,
                    null));
        }

        //publishTask(OptimAllocsImportConstants.TASK_SOURCE_OPTIMIZER, tasks);

        return tasks;
    }

    /**
     * @param entry
     * @param errors
     */
    public static Task getLogAsTask(MarginCallEntry entry, String eventType,
                                    Long executionId, String internalReference, String error) {
        if (Util.isEmpty(error)) {
            return null;
        }
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        return getLogsAsTasks(entry, eventType, executionId,
                errors).get(0);
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

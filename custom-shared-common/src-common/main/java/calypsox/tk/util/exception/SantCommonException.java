package calypsox.tk.util.exception;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.TaskInternalRef;
import com.calypso.tk.service.DSConnection;
import java.util.List;


/**
 * Represent an exception which can be published in the task station.
 */
public class SantCommonException extends Exception {


    private static final int MAX_SOURCE_LENGTH = 64;
    private static final int MAX_COMMENT_LENGTH = 256;
    private static final long serialVersionUID = 5109839191574115964L;
    private final SantCommonExceptionType santExceptionType;
    private final Task task;


    /**
     * constructor.
     *
     * @param exceptionType   nature of the exception
     * @param source          component which raised the exception, should be the
     *                        externalRef for Scheduled tasks
     * @param comment         comment
     * @param tradeId         trade id
     * @param productId       product Id
     * @param objectId        if the exception can be related with another object (like a
     *                        BOTransfer) this field indicate its id
     * @param objectClassName this is the class' short name of the object indicated in
     *                        objectId (i.e. "BOTransfer") or null if objectId == 0
     * @param Book            Id book id
     */
    public SantCommonException(final SantCommonExceptionType exceptionType,
                         final String source, final String comment, final long tradeId,
                         final int productId, final long objectId,
                         final String objectClassName, final int bookId) {
        super(comment);
        this.santExceptionType = exceptionType;
        this.task = createTask(exceptionType, null, source, comment, tradeId, productId, objectId, objectClassName, bookId);
    }


    /**
     * constructor.
     *
     * @param exceptionType   nature of the exception
     * @param source          component which raised the exception, should be the
     *                        externalRef for Scheduled tasks
     * @param comment         comment
     * @param tradeId         trade id
     * @param objectId        if the exception can be related with another object (like a
     *                        BOTransfer) this field indicate its id
     * @param objectClassName this is the class' short name of the object indicated in
     *                        objectId (i.e. "BOTransfer") or null if objectId == 0
     * @param bookId          Book id
     * @param cause           Exception
     */
    public SantCommonException(final SantCommonExceptionType exceptionType,
                         final String source, final String comment, final long tradeId,
                         final long objectId, final String objectClassName,
                         final int bookId, final Throwable cause) {
        super(comment, cause);
        this.santExceptionType = exceptionType;
        this.task = createTask(exceptionType, null, source, comment, tradeId, 0, objectId, objectClassName, bookId);
    }


    /**
     * constructor with an specific eventClass
     *
     * @param exceptionType
     * @param eventClass
     * @param source
     * @param comment
     * @param tradeId
     * @param productId
     * @param objectId
     * @param objectClassName
     * @param bookId
     */
    public SantCommonException(final SantCommonExceptionType exceptionType,
                         final String eventClass, final String source, final String comment,
                         final long tradeId, final int productId, final long objectId,
                         final String objectClassName, final int bookId) {
        super(comment);
        this.santExceptionType = exceptionType;
        this.task = createTask(exceptionType, eventClass, source, comment, tradeId, productId, objectId, objectClassName, bookId);
    }


    /**
     * creates a new Exception with priority Task.PRIORITY_NORMAL and status NEW
     *
     * @param exceptionType   nature of the exception
     * @param eventClass      eventClass of the exception
     * @param source          component which raised the exception, should be the
     *                        externalRef for Scheduled tasks
     * @param comment         comment
     * @param tradeId         trade id
     * @param objectId        if the exception can be related with another object (like a
     *                        BOTransfer) this field indicate its id
     * @param bookId          Book id
     * @param objectClassName this is the class' short name of the object indicated in
     *                        objectId (i.e. "BOTransfer") or null if objectId == 0
     * @return
     */
    Task createTask(final SantCommonExceptionType exceptionType,
                    final String eventClass, final String source, final String comment,
                    final long tradeId, final int productId, final long objectId,
                    final String objectClassName, final int bookId) {
        final Task newTask = new Task();
        newTask.setTradeLongId(tradeId);
        newTask.setProductId(productId);
        newTask.setObjectLongId(objectId);
        newTask.setObjectClassName(objectClassName);

        // customize eventClass of the exception
        if (eventClass != null) {
            newTask.setEventClass(eventClass);
        } else {
            newTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        }
        final JDatetime time = new JDatetime();
        newTask.setNewDatetime(time);
        newTask.setDatetime(time);
        newTask.setPriority(Task.PRIORITY_NORMAL);
        newTask.setStatus(Task.NEW);
        if (source != null) {
            if (source.length() > MAX_SOURCE_LENGTH) {
                newTask.setSource(source.substring(0, MAX_SOURCE_LENGTH));
            } else {
                newTask.setSource(source);
            }
        }
        if (comment != null) {
            if (comment.length() > MAX_COMMENT_LENGTH) {
                newTask.setComment(comment.substring(0, MAX_COMMENT_LENGTH));
            } else {
                newTask.setComment(comment);
            }
        }
        newTask.setEventType(exceptionType.type);
        if (bookId != 0) {
            newTask.setBookId(bookId);
        }
        if (exceptionType.equals(SantCommonExceptionType.STOP_PAYMENT_THRESHOLD)) {
            newTask.setInternalReference(findInternalRef(newTask));
        }
        return newTask;
    }


    /**
     * find the Task Station InternalRef that accept this task
     *
     * @param task task
     * @return internal Ref
     */
    @SuppressWarnings("rawtypes")
    private static String findInternalRef(final Task task) {
        final List list = BOCache.getTaskInternalRefs(DSConnection.getDefault());
        int i = -1;
        String internalRef = "";
        for (int j = 0; j < list.size(); j++) {
            final TaskInternalRef taskIntRef = (TaskInternalRef) list.get(j);
            if (!taskIntRef.getEventClass().equals("Transfer") || (taskIntRef.getLevel() <= i)) {
                continue;
            }
            if (taskIntRef.getSDFilter().equals("NONE")) {
                i = taskIntRef.getLevel();
                internalRef = (new StringBuilder()).append(internalRef).append(taskIntRef.getValue()).toString();
                continue;
            }
            final StaticDataFilter staticdatafilter = BOCache.getStaticDataFilter(DSConnection.getDefault(), taskIntRef.getSDFilter());
            if ((staticdatafilter != null) && staticdatafilter.accept(null, null, null, task)) {
                i = taskIntRef.getLevel();
                internalRef = (new StringBuilder()).append(internalRef).append(taskIntRef.getValue()).toString();
            }
        }
        return internalRef;
    }


    /**
     * Returns the Exception type.
     *
     * @return the santExceptionType
     */
    public SantCommonExceptionType getSantExceptionType() {
        return this.santExceptionType;
    }


    /**
     * returns the task.
     *
     * @return the task
     */
    public Task getTask() {
        return this.task;
    }


}

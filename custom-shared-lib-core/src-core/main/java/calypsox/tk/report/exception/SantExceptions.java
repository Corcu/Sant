package calypsox.tk.report.exception;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a list of exceptions and add methods to publish them to the task
 * station.
 */
public class SantExceptions {

    private List<SantException> exceptions = new ArrayList<SantException>();//Sonar

    /**
     * Adds the exception.
     *
     * @param exceptionType   the exception type
     * @param eventClass      event class of the exception type
     * @param source          the source
     * @param comment         the comment
     * @param tradeId         the trade id
     * @param productId       the product id
     * @param objectId        the object id
     * @param objectClassName the object class name
     * @param bookId          Book id
     */
    // CAL_BO_138
    public void addException(final SantExceptionType exceptionType,
                             final String eventClass, final String source, final String comment,
                             final long tradeId, final int productId, final long objectId,
                             final String objectClassName, final int bookId) {
        final SantException ex = new SantException(exceptionType, eventClass,
                source, comment, tradeId, productId, objectId, objectClassName,
                bookId);
        this.exceptions.add(ex);
    }

    /**
     * Adds the exception.
     *
     * @param exceptionType   the exception type
     * @param source          the source
     * @param comment         the comment
     * @param tradeId         the trade id
     * @param productId       the product id
     * @param objectId        the object id
     * @param objectClassName the object class name
     * @param bookId          Book id
     */
    public void addException(final SantExceptionType exceptionType,
                             final String source, final String comment, final long tradeId,
                             final int productId, final long objectId,
                             final String objectClassName, final int bookId) {
        final SantException ex = new SantException(exceptionType, source,
                comment, tradeId, productId, objectId, objectClassName, bookId);
        this.exceptions.add(ex);
    }

    /**
     * Adds the exception.
     *
     * @param exceptionType the exception type
     * @param source        the source
     * @param cause         the cause
     */
    public void addException(final SantExceptionType exceptionType,
                             final String source, final Throwable cause) {
        // CAL_ACC_TRDFX_113
        String comment = cause.getMessage();
        if (Util.isEmpty(comment)) {
            comment = source;
        }
        final SantException ex = new SantException(exceptionType, source,
                comment, 0, 0, null, 0, cause);
        this.exceptions.add(ex);
    }

    private TaskArray getTaskArray() {
        final TaskArray taskArray = new TaskArray();
        for (final SantException exception : this.exceptions) {
            taskArray.add(exception.getTask());
        }
        return taskArray;
    }

    /**
     * publish all task in the list to the task station.
     *
     * @param ds         the ds
     * @param evId       an event id (long) that generated the TaskArray (may be 0)
     * @param engineName an engine name (String) that generated the TaskArray (may be
     *                   NULL), as defined in the engineName domain
     */
    public void publishTasks(final DSConnection ds, final long evId,
                             final String engineName) {
        try {
            if (this.exceptions.size() > 0) {
                Log.info(this, "Sending " + this.exceptions.size()
                        + " exceptions to the task station");
                ds.getRemoteBO().saveAndPublishTasks(getTaskArray(), evId,
                        engineName);
            }
        } catch (final RemoteException e) {
            Log.error(this, e);
        }
    }

    // CAL_ACC_1252

    /**
     * Clear.
     */
    public void clear() {
        this.exceptions.clear();
    }

    /**
     * Size.
     *
     * @return the int
     */
    public int size() {
        return this.exceptions.size();
    }

    /**
     * Gets the.
     *
     * @param i the i
     * @return the sant exception
     */
    public SantException get(final int i) {
        return this.exceptions.get(i);
    }

    /**
     * Gets the error msg.
     *
     * @param i the i
     * @return the error msg
     */
    public String getErrorMsg(final int i) {
        return get(i).getMessage();
    }

    /**
     * Adds the exceptions to task array.
     *
     * @param exceptions the exceptions
     */
    public void addExceptionsToTaskArray(final List<SantException> exceptions) {
        for (final SantException ex : exceptions) {
            this.exceptions.add(ex);
        }
    }

    /**
     * Adds the exception.
     *
     * @param santException the sant exception
     */
    public void addException(final SantException santException) {
        this.exceptions.add(santException);
    }

    /**
     * Adds the exception.
     *
     * @param exceptionType the exception type
     * @param source        the source
     * @param message       the message
     * @param cause         the cause
     */
    public void addException(final SantExceptionType exceptionType,
                             final String source, final String message, final Exception cause) {
        final SantException ex = new SantException(exceptionType, source,
                message, 0, 0, null, 0, cause);

        this.exceptions.add(ex);
    }

    /**
     * Sets the exception list.
     *
     * @param exceptionList the new exception list
     */
    public void setExceptionList(final List<SantException> exceptionList) {
        this.exceptions = exceptionList;
    }

}

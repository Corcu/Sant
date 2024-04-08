package calypsox.tk.core.retried;
/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */


import java.rmi.RemoteException;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.WorkflowException;

/**
 * The Class RetriedAction.
 */
public abstract class RetriedAction {

    /** The cause. */
    protected Throwable cause;

    /**
     * Gets the cause.
     * 
     * @return the cause
     */
    public Throwable getCause() {
        return this.cause;
    }

    /**
     * Execute.
     * 
     * @param times
     *            the times
     * @throws RetriedActionException
     *             the retried action exception
     */
    public void execute(final int times) throws RetriedActionException {
        execute(times, 0);
    }

    /**
     * Execute.
     * 
     * @param times
     *            the times
     * @param wait
     *            the wait
     * @throws RetriedActionException
     *             the retried action exception
     */
    public void execute(final int times, final long wait)
            throws RetriedActionException {
        int i = 0;
        do {
            i++;
            try {
                call();
                return;
            } catch (final Exception e) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Retrying action:\n");
                sb.append(getActionInfo());
                sb.append("\nBecause:");
                Log.info(this, sb.toString(), e);
            } catch (final Throwable e) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Retrying action:\n");
                sb.append(getActionInfo());
                sb.append("\nBecause:");
                Log.info(this, sb.toString(), e);
            }
            if (wait > 0) {
                Log.info(this, "Retrying: " + i + "/" + times);
                try {
                    // CAL_SUP_035
                    Thread.sleep(wait);
                } catch (final InterruptedException e) {
                    // TODO Auto-generated catch block
                    Log.error(this, e);
                }
            }
        } while (i < times);
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append(":The action reaches the max num of retries (");
        sb.append(i);
        sb.append(").\nAction Info:");
        sb.append(getActionInfo());
        throw new RetriedActionException(sb.toString());
    }

    /**
     * Call.
     * 
     * @throws Throwable
     *             the throwable
     */
    protected abstract void call() throws Throwable;

    /**
     * Gets the action info.
     * 
     * @return the action info
     */
    protected abstract String getActionInfo();

    /**
     * The Class RetriedActionException.
     */
    public class RetriedActionException extends Exception {
        private static final long serialVersionUID = 4433701658958609572L;

        /**
         * Instantiates a new retried action exception.
         * 
         * @param message
         *            the message
         */
        public RetriedActionException(final String message) {
            super(message);
        }

    }

    /**
     * get the cause of the exception.
     * 
     * @param ex
     *            the ex
     * @return the exception cause
     */
    protected Throwable getExceptionCause(final RemoteException ex) {
        if (ex.detail == null) {
            return ex;
        } else {
            if (ex.detail instanceof RemoteException) {
                return getExceptionCause((RemoteException) ex.detail);
            } else {
                return ex.detail;
            }
        }
    }

    /**
     * check if the exception have been caused by a WorkflowException and update
     * the field "cause".
     * 
     * @param ex
     *            the ex
     * @return true, if successful
     */
    protected boolean checkIsWorkFlowException(final RemoteException ex) {
        boolean rst = false;

        this.cause = getExceptionCause(ex);

        if (this.cause instanceof WorkflowException) {
            rst = true;
        } else {
            rst = false;
        }

        return rst;
    }
}

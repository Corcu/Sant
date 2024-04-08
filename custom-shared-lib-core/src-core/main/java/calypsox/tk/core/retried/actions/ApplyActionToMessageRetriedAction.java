/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.core.retried.actions;

import calypsox.tk.core.retried.RetriedAction;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * The Class ApplyActionToMessageRetriedAction.
 */
public class ApplyActionToMessageRetriedAction extends RetriedAction {

    /**
     * The message id.
     */
    protected final long messageId;

    /**
     * The action.
     */
    protected final Action action;

    /**
     * The attributes.
     */
    @SuppressWarnings("rawtypes")
    protected final Vector attributes;

    /**
     * The success.
     */
    protected boolean success;

    /**
     * Instantiates a new apply action to message retried action.
     *
     * @param messageId  the message id
     * @param action     the action
     * @param attributes the attributes, null if you don't need them
     */
    @SuppressWarnings("rawtypes")
    public ApplyActionToMessageRetriedAction(final long messageId,
                                             final Action action, final Vector attributes) {
        this.messageId = messageId;
        this.action = action;
        this.attributes = attributes;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.core.retried.RetriedAction#call()
     */
    @Override
    protected void call() throws Throwable {
        BOMessage message = null;
        final RemoteBackOffice rbo = DSConnection.getDefault().getRemoteBO();

        message = rbo.getMessage(this.messageId);

        // ATENCION: esta RetriedAction no repite la acci?n si se produce una
        // WorkflowException.
        // Quiz?s quieras utilizar la AckMessageRetriedAction
        if (message != null) {
            message = (BOMessage) message.clone();
            message.setAction(this.action);
            message.setEnteredUser(DSConnection.getDefault().getUser());

            try {
                rbo.save(message, 0, null);
            } catch (final RemoteException ex) {
                if (!checkIsWorkFlowException(ex)) {
                    throw ex;
                } else {
                    setSuccess(false);
                }
            }
        }
        setSuccess(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.core.retried.RetriedAction#getActionInfo()
     */
    @Override
    protected String getActionInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Action Name: ");
        sb.append(this.getClass().getName());
        sb.append('\n');
        sb.append("Message: ");
        sb.append(this.messageId);
        sb.append('\n');
        sb.append("Action: ");
        sb.append(this.action.toString());
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Gets the message id.
     *
     * @return the message id
     */
    public long getmessageId() {
        return this.messageId;
    }

    /**
     * Gets the action.
     *
     * @return the action
     */
    public Action getAction() {
        return this.action;
    }

    /**
     * Sets the success.
     *
     * @param success the new success
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * Checks if is success.
     *
     * @return true, if is success
     */
    public boolean isSuccess() {
        return this.success;
    }

}

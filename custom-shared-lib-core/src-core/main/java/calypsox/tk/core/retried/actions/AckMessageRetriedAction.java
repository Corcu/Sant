/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.core.retried.actions;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * The Class AckMessageRetriedAction.
 */
public class AckMessageRetriedAction extends ApplyActionToMessageRetriedAction {

    /**
     * The description.
     */
    protected String description;

    /**
     * The exception.
     */
    protected RemoteException exception;

    /**
     * The message.
     */
    protected BOMessage message = null;

    /**
     * The adapter type.
     */
    protected String adapterType;

    /**
     * Instantiates a new ack message retried action.
     *
     * @param messageId   the message id
     * @param action      the action
     * @param attributes  the attributes
     * @param description the description
     * @param adapterType the adapter type
     */
    @SuppressWarnings("rawtypes")
    public AckMessageRetriedAction(final long messageId, final Action action,
                                   final Vector attributes, final String description,
                                   final String adapterType) {
        super(messageId, action, attributes);
        this.description = description;
        this.adapterType = adapterType;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.core.retried.actions.ApplyActionToMessageRetriedAction#call()
     */
    @Override
    protected void call() throws Throwable {
        setSuccess(false);

        final RemoteBackOffice rbo = DSConnection.getDefault().getRemoteBO();

        this.message = rbo.getMessage(this.messageId);

        if (this.message != null) {
            if (!("CLSTCOPYCONFIRM".equals(this.message.getMessageType()) && "swiftaccord"
                    .equals(this.adapterType))) {

                this.message = (BOMessage) this.message.clone();
                this.message.setAction(this.action);
                this.message
                        .setEnteredUser(DSConnection.getDefault().getUser());
                this.message.setDescription(this.description);

                this.message.setAttributes(getBoMessageAtts());

                try {
                    rbo.save(this.message, 0, null);
                } catch (final RemoteException ex) {
                    this.exception = ex;
                    throw ex;
                }
            }
            setSuccess(true);
        }
    }

    /**
     * get the BOMessage attributes
     *
     * @return vector of atts
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Vector getBoMessageAtts() {
        final Vector<String> atts = new Vector<String>();
        final Vector boMsgAtts = this.message.getAttributes();

        if (!Util.isEmpty(this.attributes)) {
            if (!Util.isEmpty(boMsgAtts)) {
                // check if boMsgAtts contains attsToInsert, in order to replace
                // them with the value stored in attsToInsert
                for (int i = 0; i < (boMsgAtts.size() - 1); i = i + 2) {
                    if (!this.attributes.contains(boMsgAtts.get(i))) {
                        atts.add((String) boMsgAtts.get(i));
                        atts.add((String) boMsgAtts.get(i + 1));
                    }
                }
                atts.addAll(this.attributes);
            } else {
                atts.addAll(this.attributes);
            }
        } else if (!Util.isEmpty(boMsgAtts)) {
            atts.addAll(boMsgAtts);
        }

        return atts;
    }

    /**
     * Gets the exception.
     *
     * @return the exception
     */
    public RemoteException getException() {
        return this.exception;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public BOMessage getMessage() {
        return this.message;
    }
}

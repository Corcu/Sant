/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */

package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskACC_TRANSFER_INTEREST;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ScheduledTaskSant_ACC_XFER_INTEREST extends ScheduledTaskACC_TRANSFER_INTEREST {
    private static final long serialVersionUID = 123L;

    public static final String SDF_ACCOUNT_PO = "Accounts SDF";

    public ScheduledTaskSant_ACC_XFER_INTEREST() {
    }

    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {
        final TaskArray tasks = new TaskArray();
        boolean exec = false;
        if (this._executeB) {
            exec = handleAccounts(ds, ps, tasks);
        }

        return exec;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        try {
            attributeList.add(attribute(SDF_ACCOUNT_PO).domain(
                    new ArrayList<String>(DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterNames())));
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass(), "Error while retrieving quotes name", e);
        }
        return attributeList;
    }

    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // @Override
    // public Vector getDomainAttributes() {
    // final Vector v = super.getDomainAttributes();
    // v.add(SDF_ACCOUNT_PO);
    // return v;
    // }
    //
    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // @Override
    // public Vector getAttributeDomain(final String attr, final Hashtable
    // currentAttr) {
    // Vector vector = new Vector();
    // if (attr.equals(SDF_ACCOUNT_PO)) {
    // try {
    // vector.addAll(DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterNames());
    // } catch (final RemoteException e) {
    // Log.error(ScheduledTaskSant_ACC_XFER_INTEREST.class, "Error while
    // retrieving quotes name", e);
    // }
    // } else {
    // vector = super.getAttributeDomain(attr, currentAttr);
    // }
    // return vector;
    // }

    @Override
    public String getTaskInformation() {
        return "This Scheduled Task gerates payement transfers for the list of the given accounts  \n";
    }

    @SuppressWarnings("unchecked")
    protected boolean handleAccounts(final DSConnection ds, final PSConnection ps, final TaskArray tasks) {
        // get the list of accounts

        String sdfAttribute = getAttribute(SDF_ACCOUNT_PO);

        if (Util.isEmpty(sdfAttribute)) {
            // call the normal process
            super.process(ds, ps);

        }

        StaticDataFilter sdf = BOCache.getStaticDataFilter(ds, sdfAttribute);

        if (sdf == null) {
            // call the normal process
            super.process(ds, ps);
        }

        // get All accounts to handle
        String whereClause = "acc_type = 'SETTLE' AND automatic_b = 0 AND interest_bearing = 1";
        Vector<Account> accounts;
        try {
            accounts = ds.getRemoteAccounting().getAccounts(whereClause, null);
            if (!Util.isEmpty(accounts)) {
                for (Account acc : accounts) {
                    if (sdf.accept(null, null, null, null, null, acc)) {
                        //
                        setAttribute(ACCOUNT_ID, "" + acc.getId());
                        super.process(ds, ps);
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        return true;
    }

    /**
     * <code>createException</code> create an Exception in the TStation.
     *
     * @param comment       Exception comment
     * @param criticalError if true, the Task Status is NEW, otherwise its COMPLETED.
     * @return the Exception to be saved
     */
    protected Task createException(final String comment, final boolean criticalError) {
        final Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(new JDatetime());
        task.setDatetime(new JDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setObjectLongId(getId());
        task.setStatus(criticalError ? Task.NEW : Task.COMPLETED);
        task.setEventType("EX_" + BOException.EXCEPTION);
        task.setComment(comment);

        task.setUndoTradeDatetime(task.getDatetime());
        task.setCompletedDatetime(task.getDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setUnderProcessingDatetime(task.getDatetime());
        task.setSource(getType());
        return task;
    }
}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.RemoteCollateralServer;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ScheduledTaskSANT_CLEANUP_DUPLICATED_ENTRIES extends ScheduledTask {

    private static final String CONTRACT_ID = "CONTRACT ID";

    // START OA 27/11/2013
    // Oracle recommendation : declare a serialVersionUID for each Serializable
    // class in order to avoid
    // InvalidClassExceptions.
    // Please refer to Serializable javadoc for more details
    private static final long serialVersionUID = 954825074033L;

    // END OA OA 27/11/2013

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(CONTRACT_ID));
        return attributeList;
    }

    @Override
    public String getTaskInformation() {
        return "Cleanup duplicate entries for a given contract and a given date passed by valuation date";
    }

    // @Override
    // public Vector<String> getDomainAttributes() {
    // final Vector<String> v = Util.string2Vector(CONTRACT_ID);
    // return v;
    // }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        StringBuffer scheduledTaskExecLogs = new StringBuffer();
        Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(getValuationDatetime());
        task.setUnderProcessingDatetime(getDatetime());
        task.setUndoTradeDatetime(getUndoDatetime());
        task.setDatetime(getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setSource(getType());

        boolean handlingOk = false;
        try {
            deleteContract(scheduledTaskExecLogs);
            handlingOk = scheduledTaskExecLogs.toString().length() == 0;
            if (handlingOk) {
                task.setEventType("EX_" + BOException.INFORMATION);
            } else {
                task.setComment(scheduledTaskExecLogs.toString());
                task.setEventType("EX_" + BOException.EXCEPTION);
            }
            task.setCompletedDatetime(new JDatetime());
            task.setStatus(Task.NEW);

            TaskArray v = new TaskArray();
            v.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return handlingOk;
    }

    @SuppressWarnings({"unused"})
    private void deleteContract(StringBuffer scheduledTaskExecLogs) {
        RemoteCollateralServer service = ServiceRegistry.getDefault(getDSConnection()).getCollateralServer();

        int mccId = isValid();
        if (mccId <= 0) {
            return;
        }

        JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        List<MarginCallEntryDTO> entries = null;
        try {
            entries = CollateralManagerUtil.loadMarginCallEntriesDTO(Arrays.asList(mccId), processDate);
        } catch (RemoteException e) {
            Log.error(this, "Error loading entries", e);
            scheduledTaskExecLogs.append("Error loading entries");
        }

        if (Util.isEmpty(entries)) {
            return;
        }
        for (MarginCallEntryDTO entry : entries) {
            try {
                // AAP MIG14.4 Boolean added
                ServiceRegistry.getDefault(getDSConnection()).getCollateralServer().remove(getDSConnection().getUser(),
                        entry.getId(), true);
            } catch (RemoteException e) {
                Log.error(this, "Error deleting entry " + entry.getId(), e);
                scheduledTaskExecLogs.append("Error deleting entry " + entry.getId());
            }
        }
    }

    public int isValid() {
        String contractId = getAttribute(CONTRACT_ID);
        int id = 0;
        String message = null;
        try {
            id = Integer.valueOf(contractId);
            MarginCallConfig mcc = BOCache.getMarginCallConfig(getDSConnection(), id);
            if (mcc == null) {
                message = "Contract does not exist";
            }
        } catch (Exception e) {
            Log.error(this, e); //sonar
            message = "Contract id is not valid";
        }
        if (message.length() > 0) {
            JOptionPane.showMessageDialog(null, message);
        }

        return id;
    }

}

package calypsox.tk.util;

import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.report.SantMissingIsinUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ScheduledTaskSANT_MT569_ISIN_REPROCESS extends ScheduledTask {

    private static final long serialVersionUID = -1769293773087140861L;
    private static JDate valDate;
    private final static String stInformation = "This ST finds the tasks associated with a MT569 SWIFT MESSAGE and filter them by a rule";
    private static String triParty = "'%TRIPARTY_ALLOCATION: Undefined system property%'";
    private static String actionReprocess = "REPROCESS";
    private static SantMissingIsinUtil santMiss = SantMissingIsinUtil.getInstance();


    @Override
    public String getTaskInformation() {

        return stInformation;
    }

    @Override
    protected boolean process(DSConnection dsConn, PSConnection psConn) {
        //Get a list of failed tasks:
        TaskArray tList = null;
        tList = getTasks(tList, getValuationDatetime());

        for (Task task : tList) {
            boolean reprocessTaskIsin = !(lostIsinsMT(task, dsConn));
            long boID;
            if (reprocessTaskIsin) {
                boID = task.getObjectLongId();
                try {
                    BOMessage message = dsConn.getRemoteBO().getMessage(boID);
                    saveMessage(message, dsConn);
                } catch (CalypsoServiceException e) {
                    Log.error(ScheduledTaskSANT_MT569_ISIN_REPROCESS.class, e);
                }
            }
            //else {} What is it going to do if unless, one ISIN is LOST
        }

        return true;
    }
//SELECT col FROM db.tbl WHERE (col LIKE 'str1' OR col LIKE 'str2') AND col2 = num


    //: Undefined system property//+ PDVConstants.PDV_ALLOC_FUT_EXCEPTION_TYPE
    private static TaskArray getTasks(TaskArray list, JDatetime valDate) {//query for retrieving the tasks
        StringBuffer str = new StringBuffer();
        str.append("(bo_task.event_type LIKE 'PENDING_SUB_INC_ALLOCATION' OR bo_task.event_type LIKE 'PENDING_INC_ALLOCATION')");
        str.append(" AND (bo_task.object_status LIKE 'PENDING_SUB' OR bo_task.object_status LIKE 'PENDING')");
        str.append(" AND bo_task.task_status NOT LIKE '2'");
        str.append(" AND bo_task.comments LIKE " + triParty);
//	    str.append(" AND trunc(bo_task.TASK_DATETIME) >= {d '2018-07-17'}");
        str.append(" AND trunc(bo_task.TASK_DATETIME) > " + Util.date2SQLString(valDate));
        try {
            list = DSConnection.getDefault().getRemoteBO().getTasks(str.toString(), null, "task_datetime ASC", null);
        } catch (RemoteException e) {
            Log.error("ScheduledTaskSANT_MT569_ISIN_REPROCESS", e);
        }

        return list;

    }

    //Check the Isins from the failed tasks' messages
    private static boolean lostIsinsMT(Task task, DSConnection ds) { //Check for everyIN for a BOM from a task
        ArrayList<String> isins = null;
        BOMessage boM = null;
        long boID = task.getObjectLongId();//getting the message ID
        try {
            boM = ds.getRemoteBO().getMessage(boID);// getting the boMessage
            isins = (ArrayList<String>) santMiss.getIsins(boM); // and the ISINs from it
        } catch (CalypsoServiceException e) {
            Log.error(ScheduledTaskSANT_MT569_ISIN_REPROCESS.class, e);
        }

        return isinMissing(isins);
    }

    private static boolean isinMissing(ArrayList<String> isinsMT569) {//check if there are some missing Isin
        for (String Isin : isinsMT569) {
            boolean isinLost = santMiss.isMissing(Isin);
            if (isinLost) {
                return true;
            }
        }
        return false;
    }

    private void saveMessage(BOMessage msg, DSConnection ds) {
        if (msg != null) {
            Action action = Action.valueOf(actionReprocess);
            if (BOMessageWorkflow.isMessageActionApplicable(msg, null, null, action, ds, null)) {
                msg.setAction(action);
                try {
                    DSConnection.getDefault().getRemoteBO().save(msg, 0, null);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not save message with messageId=" + msg.getLongId());
                }
            } else {
                Log.error(this, "Could not apply action " + action + " to message with id: " + msg.getLongId());
            }
        }
    }

}

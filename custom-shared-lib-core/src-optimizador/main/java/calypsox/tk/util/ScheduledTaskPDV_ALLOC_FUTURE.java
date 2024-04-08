package calypsox.tk.util;

import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.event.PSEventPDVAllocationFut;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.TimeZone;
import java.util.Vector;

public class ScheduledTaskPDV_ALLOC_FUTURE extends ScheduledTask {

    protected static final String TASK_INFORMATION = "Publish PSEventPDVAllocationFut from TaskStation tasks on valDate in order to be imported by SantPDVCollatEngine.";
    private static final long serialVersionUID = 7002273897461066558L;

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    public boolean process(DSConnection dsCon, PSConnection connPS) {

        boolean proccesOK = true;

        // load tasks
        JDatetime valDatetime = getValuationDatetime();
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        TaskArray tasksToPublish = PDVUtil
                .getAllocFutureTasksToPublish(valDatetime.getJDate(TimeZone
                        .getDefault()));

        Vector<PSEvent> psEventsToPublish = new Vector<>();
        if (tasksToPublish != null && tasksToPublish.size() > 0) {
            Log.debug(ScheduledTaskPDV_ALLOC_FUTURE.class.getName(),
                    "Processing nb tasks " + tasksToPublish.size());

            for (int i = 0; i < tasksToPublish.size(); i++) {
                Task currentTask = tasksToPublish.get(i);
                Log.debug(ScheduledTaskPDV_ALLOC_FUTURE.class.getName(),
                        currentTask.getId() + ": " + currentTask.getComment());

                PSEventPDVAllocationFut pdvAllocFutEvent = new PSEventPDVAllocationFut(
                        currentTask.getId(), currentTask.getComment(), currentTask.getAttribute(), currentTask.getObjectDate(), currentTask.getObjectLongId());
                psEventsToPublish.add(pdvAllocFutEvent);

                Log.debug(
                        ScheduledTaskPDV_ALLOC_FUTURE.class.getName(),
                        "Publishing event: "
                                + pdvAllocFutEvent.getAllocMessage());
            }
            try {
                if (psEventsToPublish.size() > 0) {
                    DSConnection.getDefault().getRemoteTrade()
                            .saveAndPublish(psEventsToPublish);
                }
            } catch (RemoteException e) {
                Log.error(ScheduledTaskPDV_ALLOC_FUTURE.class.getName(), e);
                return false;
            }
        } else {
            Log.debug(ScheduledTaskPDV_ALLOC_FUTURE.class.getName(),
                    "No task to process on valDate " + valDate);
        }

        return proccesOK;
    }
}

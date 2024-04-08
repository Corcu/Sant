package calypsox.apps.reporting;

import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.event.PSEventPDVAllocation;
import calypsox.tk.event.PSEventPDVAllocationFut;
import calypsox.tk.event.PSEventSendOptimizerPosition;
import calypsox.tk.util.optimizer.TaskErrorUtil;
import com.calypso.apps.reporting.TaskStationJFrame;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import org.jfree.util.Log;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.Vector;

public class CustomTaskStationMenuException implements
        PDVConstants, com.calypso.apps.reporting.CustomTaskStationMenu {

    public static final String REPROCESS_OPT_POSITION = "Reprocess Optimizer Position...";

    public static final String PROCESS_PDV_ALLOCATION_CONTRACT = "Process PDV Allocation for given contrat...";

    public static final String REPROCESS_PDV_ALLOCATION = "Reprocess PDV Allocation...";

    public static final String REPROCESS_PDV_ALLOCATION_FUTURE = "Reprocess PDV Allocation Future...";

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.apps.reporting.CustomTaskStationMenu#getMenuItems()
     */
    @SuppressWarnings("rawtypes")
    public Vector getMenuItems() {
        Vector<JMenuItem> v = new Vector<>();
        JMenuItem itemOptPos = new JMenuItem(REPROCESS_OPT_POSITION);
        itemOptPos.setActionCommand(REPROCESS_OPT_POSITION);
        v.addElement(itemOptPos);
        JMenuItem itemAllocContract = new JMenuItem(PROCESS_PDV_ALLOCATION_CONTRACT);
        itemAllocContract.setActionCommand(PROCESS_PDV_ALLOCATION_CONTRACT);
        v.addElement(itemAllocContract);
        JMenuItem itemAllocFut = new JMenuItem(REPROCESS_PDV_ALLOCATION_FUTURE);
        itemAllocFut.setActionCommand(REPROCESS_PDV_ALLOCATION_FUTURE);
        v.addElement(itemAllocFut);
        JMenuItem itemAllocReprocess = new JMenuItem(REPROCESS_PDV_ALLOCATION);
        itemAllocReprocess.setActionCommand(REPROCESS_PDV_ALLOCATION);
        v.addElement(itemAllocReprocess);
        return v;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.apps.reporting.CustomTaskStationMenu#handleAction(java.lang
     * .String, com.calypso.apps.reporting.TaskStationJFrame, java.util.Vector)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handleAction(String action, TaskStationJFrame window,
                             Vector selectedTasks) {
        for (Task task : (Vector<Task>) selectedTasks) {
            if (TaskErrorUtil.EnumOptimProcessType.OPTIMIZER_POSITION
                    .equals(task.getSource())
                    && TaskErrorUtil.EnumOptimProcessType.OPTIMIZER_POSITION
                    .equals(task.getOwner())) {
                PSEventSendOptimizerPosition psEvent = new PSEventSendOptimizerPosition(
                        task.getProductId(), task.getBookId());
                try {
                    DSConnection.getDefault().getRemoteTrade()
                            .saveAndPublish(psEvent);
                } catch (RemoteException e) {
                    Log.error(CustomTaskStationMenuException.class.getName(), e);
                }
            } else if (PROCESS_PDV_ALLOCATION_CONTRACT.equals(action) && PDVUtil.isMoreThanOneEligibleContract(task)) {
                try {
                    if (!Util.isEmpty(task.getInternalReference())) {

                        PSEventPDVAllocation pdvAllocEvent = new PSEventPDVAllocation(task.getId(),
                                (int) task.getLinkId(), (int) task.getTradeLongId(), task.getInternalReference(),
                                (int) task.getObjectLongId(), false, true);

                        CollateralConfig mcc = CacheCollateralClient
                                .getCollateralConfig(DSConnection.getDefault(),
                                        pdvAllocEvent.getContractId());
                        if (mcc == null) {
                            AppUtil.displayError(window,
                                    "Cannot process allocation: corresponding contract does not exist");
                        } else if (AppUtil.displayQuestion(
                                "Do you really want to process allocation for contrat: " + "\n"
                                        + mcc.getName() + "("
                                        + pdvAllocEvent.getContractId() + ")?",
                                window)) {

                            DSConnection.getDefault().getRemoteTrade()
                                    .saveAndPublish(pdvAllocEvent);
                        }
                    }
                } catch (RemoteException e) {
                    Log.error(CustomTaskStationMenuException.class.getName(), e);
                }
            } else if (REPROCESS_PDV_ALLOCATION_FUTURE.equals(action) && PDVConstants.PDV_ALLOC_FUT_EXCEPTION_TYPE.equals(task
                    .getEventType())) {
                try {
                    PSEventPDVAllocationFut pdvAllocFutEvent = new PSEventPDVAllocationFut(
                            task.getId(), task.getComment(), task.getAttribute(), task.getObjectDate(), task.getObjectLongId());
                    if (AppUtil.displayQuestion(
                            "Do you really want to reprocess allocation future: " + "\n"
                                    + task.getComment() + "?", window)) {

                        DSConnection.getDefault().getRemoteTrade()
                                .saveAndPublish(pdvAllocFutEvent);
                    }
                } catch (RemoteException e) {
                    Log.error(CustomTaskStationMenuException.class.getName(), e);
                }
            } else if (REPROCESS_PDV_ALLOCATION.equals(action) && PDVConstants.PDV_ALLOC_EXCEPTION_TYPE.equals(task
                    .getEventType()) && !Util.isEmpty(task.getInternalReference()) && !PDVUtil.isMoreThanOneEligibleContract(task)) {
                if (!Util.isEmpty(task.getComment())) {

                    PSEventPDVAllocation pdvAllocEvent = new PSEventPDVAllocation(task.getId(), (int) task.getLinkId(), 0, task.getInternalReference(), (int) task.getObjectLongId(), true, false);

                    if (AppUtil.displayQuestion(
                            "Do you really want to reprocess allocation: " + "\n"
                                    + task.getInternalReference() + "?", window)) {
                        try {
                            DSConnection.getDefault().getRemoteTrade()
                                    .saveAndPublish(pdvAllocEvent);
                        } catch (RemoteException e) {
                            Log.error(CustomTaskStationMenuException.class.getName(), e);
                        }
                    }
                }
            } else {
                AppUtil.displayError(window, "Could not apply such action on this task");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.apps.reporting.CustomTaskStationMenu#handleWorkflowAction
     * (com.calypso.tk.core.Action,
     * com.calypso.apps.reporting.TaskStationJFrame, com.calypso.tk.bo.Task,
     * java.util.Vector)
     */
    @SuppressWarnings("rawtypes")
    public boolean handleWorkflowAction(Action action,
                                        TaskStationJFrame taskstationjframe, Task task, Vector vector) {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.apps.reporting.CustomTaskStationMenu#isWorkflowActionImplemented
     * (com.calypso.tk.bo.Task, com.calypso.tk.core.Action)
     */
    public boolean isWorkflowActionImplemented(Task task, Action action) {
        return false;
    }
}

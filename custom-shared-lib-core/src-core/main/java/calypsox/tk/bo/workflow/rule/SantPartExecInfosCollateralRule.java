package calypsox.tk.bo.workflow.rule;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.Date;
import java.util.List;

/**
 * Add additional information on the margin call if the result of execution is Partially executed
 *
 * @author aela
 */
public class SantPartExecInfosCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Add additional information on the margin call if the result of execution is Partially executed";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        // add additional information on the entry about why the margin call is partially allocated.
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.IS_PART_EXEC, Boolean.TRUE);
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_REMAINING_MC,
                paramMarginCallEntry.getRemainingMargin());
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE,
                paramMarginCallEntry.getProcessDate());

        RemoteSantCollateralService remoteColService = DSConnection.getDefault().getRMIService("baseSantCollateralService",
                RemoteSantCollateralService.class);

        try {
            List<MarginCallEntryDTO> lastEntry = remoteColService.getMarginCallEntries("mcc_id= "
                    + paramMarginCallEntry.getCollateralConfigId()
                    + " and process_date = (select max(process_date) "
                    + "						from margin_call_entries me2 where me2.mcc_id=margin_call_entries.mcc_id and me2.mcc_id="
                    + paramMarginCallEntry.getCollateralConfigId() + " and me2.process_date < "
                    + Util.date2SQLString(paramMarginCallEntry.getProcessDate()) + ")");

            if (!Util.isEmpty(lastEntry)) {
                MarginCallEntryDTO lastMarginCallEntry = lastEntry.get(0);
                Object lastExecutionDate = lastMarginCallEntry
                        .getAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE);
                // if previous entry has PART_EXEC_STATUS_SINCE set, then we need to set the same value for the
                // current entry
                if (lastExecutionDate != null) {
                    JDate partExecSince = null;
                    if (lastExecutionDate instanceof Date) {
                        partExecSince = JDate.valueOf((Date) lastExecutionDate);
                    } else if (lastExecutionDate instanceof JDate) {
                        partExecSince = (JDate) lastExecutionDate;
                    }
                    paramMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE,
                            partExecSince);
                }
            }
        } catch (PersistenceException e) {
            paramList.add("Unable to add additional infos on margin call: " + e.getMessage());
            Log.error(this, e); //sonar
            return false;
        }


        return true;
    }
}

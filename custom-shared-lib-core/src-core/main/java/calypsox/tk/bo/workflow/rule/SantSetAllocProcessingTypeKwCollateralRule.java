package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.List;

public class SantSetAllocProcessingTypeKwCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Set the allocation's ProcessingType attribute from its entry.";
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

        List<MarginCallAllocation> allocs = paramMarginCallEntry.getAllocations();
        if ((allocs != null) && (allocs.size() > 0)) {
            for (MarginCallAllocation alloc : allocs) {
                alloc.addAttribute("ProcessingType", paramMarginCallEntry.getProcessingType());
            }
        }

        return true;
    }
}

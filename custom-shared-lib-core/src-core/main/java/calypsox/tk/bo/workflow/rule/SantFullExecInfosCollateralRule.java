package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.List;

/**
 * Add additional information on the margin call if the result of execution is Fully executed
 *
 * @author aela
 */
public class SantFullExecInfosCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Add additional information on the margin call if the result of execution is Fully executed";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry paramMarginCallEntry, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        // add additional information on the entry about why the margin call is partially allocated.
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.IS_PART_EXEC, Boolean.FALSE);
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_REMAINING_MC,
                paramMarginCallEntry.getRemainingMargin());
        paramMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE, null);
        wfr.success();
        return wfr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        return true;
    }
}

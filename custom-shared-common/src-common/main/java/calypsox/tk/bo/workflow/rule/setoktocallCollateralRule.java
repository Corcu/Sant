package calypsox.tk.bo.workflow.rule;

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
import java.util.Map;
import java.util.Optional;

public class setoktocallCollateralRule extends BaseCollateralWorkflowRule {

    public static final String OK_TO_CALL = "OK-TO-CALL";

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        return true;
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskWorkflowConfig, MarginCallEntry marginCallEntry, DSConnection dsConnection) {
        WorkflowResult wfr = new WorkflowResult();

        if (marginCallEntry != null) {
            marginCallEntry.addAttribute(OK_TO_CALL, true);
        }
        wfr.success();
        return wfr;
    }

    @Override
    public String getDescription() {
        return "This rule move to true the OK-TO-CALL MarginCallEntry keyword";
    }
}

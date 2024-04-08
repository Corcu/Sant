package calypsox.tk.bo.workflow.rule;

import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

/**
 * Save the last margin call entry status before a substitution process
 * 
 * @author aela
 * 
 */
public class SantLastStatusBeforeSubstitutionCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Save the last margin call entry status before a substitution process.";
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

		paramMarginCallEntry.addAttribute("LAST_STATUS_BEFORE_SUBSTITUTION", paramMarginCallEntry.getStatus());

		return true;
	}
}

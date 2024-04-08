package calypsox.tk.bo.workflow.rule;

import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.CheckNoCallCollateralRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

/**
 * Check if a call is required
 * 
 * @author aela
 * 
 */
public class SantCheckCallRequiredCollateralRule extends CheckNoCallCollateralRule {

	@Override
	public String getDescription() {
		return "Check if the margin call is partially allocated keywords.";
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
		boolean isNoCallRequired = super.isApplicable(paramTaskWorkflowConfig, paramMarginCallEntry, paramEntityState1,
				paramEntityState2, paramList, paramDSConnection, paramList1, paramTask, paramObject, paramList2);
		return !isNoCallRequired;
	}
}

package calypsox.tk.bo.workflow.rule;

import java.util.List;

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

public class SantResetWarningAlreadyCheckedCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Removes the attribute WarningExecutionShorton on MarginCall Entry ";
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
		WorkflowResult wfr = new WorkflowResult();
		wfr.success();
		return wfr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {

		if (entry.getAttribute(CollateralStaticAttributes.ENTRY_ATTR_WARNING_EXEC_SHORT) != null) {
			entry.addAttribute(CollateralStaticAttributes.ENTRY_ATTR_WARNING_EXEC_SHORT, null);
		}

		return true;
	}
}

package calypsox.tk.bo.workflow.rule;

import java.util.List;

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

/**
 * Set the allocation's attribute AllocationMode to Substitution
 * 
 * @author aela
 * 
 */
public class SantSetAllocAsSubstitutionCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Set the allocation's attribute AllocationMode to Substitution.";
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

		// get the entry pending allocations

		List<MarginCallAllocation> allocs = paramMarginCallEntry.getPendingMarginAllocations();
		if ((allocs != null) && (allocs.size() > 0)) {
			for (MarginCallAllocation alloc : allocs) {
				alloc.addAttribute("AllocationMode", "Substitution");
			}
		}

		return true;
	}
}

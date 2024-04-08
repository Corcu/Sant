package calypsox.tk.bo.workflow.rule;

import java.util.List;

import com.calypso.infra.util.Util;
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
 * 
 * @author aela
 * 
 */
public class SantIsOneLegSubstitutionCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Check if there is only subsitution allocations";
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
		boolean isOneLeg = false;
		List<MarginCallAllocation> pendingAllocation = paramMarginCallEntry.getPendingMarginAllocations();
		if (!Util.isEmpty(pendingAllocation)) {
			for (MarginCallAllocation alloc : pendingAllocation) {
				if (!MarginCallAllocation.ALLOCATION_SUBSTITUTION.equals(alloc.getType())) {
					return false;
				}
			}
			// all the pending allocations are substitution
			isOneLeg = true;
		}
		return isOneLeg;
	}
}

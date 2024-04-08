package calypsox.tk.bo.workflow.rule;

import java.util.List;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
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
public class SantIsTwoLegsSubstitutionCollateralRule extends SantIsOneLegSubstitutionCollateralRule {

	@Override
	public String getDescription() {
		return "Check if there is not only subsitution allocations";
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

		boolean isSubtBooked = false;
		boolean isMargeBooked = false;

		List<MarginCallAllocation> pendingAllocation = paramMarginCallEntry.getPendingMarginAllocations();
		if (!Util.isEmpty(pendingAllocation)) {
			for (MarginCallAllocation alloc : pendingAllocation) {
				if (MarginCallAllocation.ALLOCATION_SUBSTITUTION.equals(alloc.getType())) {
					isSubtBooked = true;
				} else {
					isMargeBooked = true;
				}
			}
		}
		if (!(isMargeBooked && isSubtBooked)) {
			paramList.add("At least one of the booked allocations must be of Substitution type.");
			return false;
		}
		return true;

		// return !super.isApplicable(paramTaskWorkflowConfig, paramMarginCallEntry, paramEntityState1,
		// paramEntityState2,
		// paramList, paramDSConnection, paramList1, paramTask, paramObject, paramList2);
	}
}

/**
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.List;

import calypsox.tk.util.SantCollateralOptimUtil;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.ReleaseCollateralRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

/**
 * Release only the reserved margin allocations, if any
 * 
 * @author aela
 * 
 */
public class SantReleaseMarginCollateralRule extends
		ReleaseCollateralRule {

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig,
			MarginCallEntry entry, DSConnection dsCon) {
		List<MarginCallAllocation> reservedAllocs = entry
				.getReservedAllocations();
		WorkflowResult wr = new WorkflowResult();
		if (Util.isEmpty(reservedAllocs)) {
			// nothing to do, skip this rule
			wr.success();
			return wr;
		}

		List<MarginCallAllocation> removedSubstitAllocs = SantCollateralOptimUtil
				.removeSubstitAllocations(entry);
		// apply the core release process
		wr = super.apply(taskConfig, entry, dsCon);
		// add back the Margin allocation
		if (!Util.isEmpty(removedSubstitAllocs)) {
			for (MarginCallAllocation alloc : removedSubstitAllocs) {
				entry.addAllocation(alloc);
			}
		}
		return wr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig wc,
			MarginCallEntry entry, EntityState state, EntityState oldState,
			List<String> messages, DSConnection dsConnection,
			List<BOException> excps, Task task, Object dbCon,
			List<PSEvent> events) {

		return true;

		// return super.isApplicable(wc, entry, state, oldState, messages,
		// dsConnection,
		// excps, task, dbCon, events);
	}

}

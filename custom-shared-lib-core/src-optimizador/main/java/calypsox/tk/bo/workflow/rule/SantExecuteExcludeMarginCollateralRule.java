/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.ArrayList;
import java.util.List;

import calypsox.tk.util.SantCollateralOptimUtil;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

public class SantExecuteExcludeMarginCollateralRule extends SantExecuteCollateralRule {

	protected List<MarginCallAllocation> removedMarginAllocs = new ArrayList<MarginCallAllocation>();
	private static final String RULE_NAME = "SantExecuteExcludeSubstitutionsCollateralRule";

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
			List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {

		// Remove allocations without Execute Alloc attribute set
		this.removedMarginAllocs = SantCollateralOptimUtil.removeMarginAllocations(entry);

		boolean isApplicalble = super.isApplicable(paramTaskWorkflowConfig, entry, paramEntityState1,
				paramEntityState2, paramList, dsCon, paramList1, paramTask, paramObject, paramList2);

		if (!isApplicalble) {
			addEntryAllocations(entry, this.removedMarginAllocs);
			this.removedMarginAllocs.clear();
		}

		return isApplicalble;
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		WorkflowResult wfr = null;

		// Remove allocations without Execute Alloc attribute set
		// this.removedAllocsWithNoAttr = removeAllocationsWithNoExecuteAttr(entry);
		wfr = super.apply(taskConfig, entry, dsCon);

		Log.info(RULE_NAME, "super.apply() returned ");
		Log.info(RULE_NAME, "removedAllocsWithNoAttr.size()=" + this.removedMarginAllocs.size());

		// Add the allocations with no Exec attribute set
		if (!Util.isEmpty(this.removedMarginAllocs)) {
			Log.info(RULE_NAME, "Now adding the deleted allocs back");
			Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) before adding back="
					+ entry.getAllocations().size());

			addEntryAllocations(entry, this.removedMarginAllocs);
			this.removedMarginAllocs.clear();
			Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) after adding back="
					+ entry.getAllocations().size());
		}

		return wfr;
	}

}

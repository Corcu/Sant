/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.ArrayList;
import java.util.List;

import calypsox.tk.core.CollateralStaticAttributes;
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

public class SantExecuteExcludeSubstitutionsCollateralRule extends SantExecuteCollateralRule {

	protected List<MarginCallAllocation> removedSubstitAllocs = new ArrayList<MarginCallAllocation>();
	private static final String RULE_NAME = "SantExecuteExcludeSubstitutionsCollateralRule";

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
			List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {
		
		// Remove allocations without Execute Alloc attribute set
		this.removedSubstitAllocs = SantCollateralOptimUtil.removeSubstitAllocations(entry);

		boolean isApplicalble = super.isApplicable(paramTaskWorkflowConfig, entry, paramEntityState1,
				paramEntityState2, paramList, dsCon, paramList1, paramTask, paramObject, paramList2);

		if (!isApplicalble) {
			addEntryAllocations(entry, this.removedSubstitAllocs);
			this.removedSubstitAllocs.clear();
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
		Log.info(RULE_NAME, "removedAllocsWithNoAttr.size()=" + this.removedSubstitAllocs.size());

		// Add the allocations with no Exec attribute set
		if (!Util.isEmpty(this.removedSubstitAllocs)) {
			Log.info(RULE_NAME, "Now adding the deleted allocs back");
			Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) before adding back="
					+ entry.getAllocations().size());

			addEntryAllocations(entry, this.removedSubstitAllocs);
			this.removedSubstitAllocs.clear();
			Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) after adding back="
					+ entry.getAllocations().size());
		}

		return wfr;
	}
	
	public static boolean isPlainAllocation(MarginCallAllocation alloc) {
		if ((alloc.getAttribute(CollateralStaticAttributes.ALLOC_ATTR_SUBST_AND_REPLACE) == null)
				|| Util.isEmpty((String) alloc.getAttribute(CollateralStaticAttributes.ALLOC_ATTR_SUBST_AND_REPLACE))
				|| ((String) alloc.getAttribute(CollateralStaticAttributes.ALLOC_ATTR_SUBST_AND_REPLACE))
						.equals("false")) {
			return true;
		}
		return false;
	}
}

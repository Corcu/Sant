/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

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

import java.util.ArrayList;
import java.util.List;

public class SantExecutePlainAllocationsOnlyCollateralRule extends SantExecuteCollateralRule {

    protected List<MarginCallAllocation> removedAllocsWithNoAttr = new ArrayList<MarginCallAllocation>();
    private static final String RULE_NAME = "SantExecutePlainAllocationsOnlyCollateralRule";

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
                                   List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {

        // Remove allocations without Execute Alloc attribute set
        this.removedAllocsWithNoAttr = removeAllocationsWithNoExecuteAttr(entry);

        boolean isApplicalble = super.isApplicable(paramTaskWorkflowConfig, entry, paramEntityState1,
                paramEntityState2, paramList, dsCon, paramList1, paramTask, paramObject, paramList2);

        if (!isApplicalble) {
            addEntryAllocations(entry, this.removedAllocsWithNoAttr);
            this.removedAllocsWithNoAttr.clear();
        }

        return isApplicalble;
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = null;

        // Remove allocations without Execute Alloc attribute set
        this.removedAllocsWithNoAttr = removeAllocationsWithNoExecuteAttr(entry);
        wfr = super.apply(taskConfig, entry, dsCon);

        Log.info(RULE_NAME, "super.apply() returned ");
        Log.info(RULE_NAME, "removedAllocsWithNoAttr.size()=" + this.removedAllocsWithNoAttr.size());

        // Add the allocations with no Exec attribute set
        if (!Util.isEmpty(this.removedAllocsWithNoAttr)) {
            Log.info(RULE_NAME, "Now adding the deleted allocs back");
            Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) before adding back="
                    + entry.getAllocations().size());

            addEntryAllocations(entry, this.removedAllocsWithNoAttr);
            this.removedAllocsWithNoAttr.clear();
            Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size()) after adding back="
                    + entry.getAllocations().size());
        }

        return wfr;
    }

    private List<MarginCallAllocation> removeAllocationsWithNoExecuteAttr(MarginCallEntry entry) {

        Log.info(RULE_NAME, "entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());

        List<MarginCallAllocation> deletedAllocations = new ArrayList<>();

        if (!Util.isEmpty(entry.getPendingMarginAllocations())) {
            for (MarginCallAllocation alloc : entry.getPendingMarginAllocations()) {
                boolean plainAllocation = SantExecuteExcludeSubstitutionsCollateralRule.isPlainAllocation(alloc);
                if (!plainAllocation) {
                    Log.info(RULE_NAME, "One Allocation to Delete... amount=" + alloc.getValue());
                    deletedAllocations.add(alloc);
                    entry.removeAllocation(alloc);
                    Log.info(RULE_NAME,
                            "One Allocation to Deleted. Now entry.getPendingMarginAllocations(true).size())="
                                    + entry.getPendingMarginAllocations().size());
                }

            }
        }
        Log.info(RULE_NAME, "deletedAllocations.size()=" + deletedAllocations.size());
        Log.info(RULE_NAME, "After Delete, entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());
        return deletedAllocations;
    }

}

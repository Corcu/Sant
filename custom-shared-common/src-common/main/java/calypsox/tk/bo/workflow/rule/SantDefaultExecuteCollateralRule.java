/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.ExecuteCollateralRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aela
 */
public class SantDefaultExecuteCollateralRule extends ExecuteCollateralRule {
    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.rule.ExecuteCollateralRule#apply(com.calypso.tk.bo.TaskWorkflowConfig,
     * com.calypso.tk.collateral.MarginCallEntry, com.calypso.tk.service.DSConnection)
     */
    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = null;
        // get the list of allocations to be excluded
        List<MarginCallAllocation> notExecutedAllocations = getExcludedAllocations(entry);
        // temporarily remove the excluded allocations from the entry
        removeEntryAllocations(entry, notExecutedAllocations);
        // execute the rest of the allocations
        wfr = executeAllocations(taskConfig, entry, dsCon);
        // add back the excluded allocation so they will remain as drafts
        addEntryAllocations(entry, notExecutedAllocations);
        return wfr;
    }

    protected WorkflowResult defaultApply(TaskWorkflowConfig taskConfig,
                                          MarginCallEntry entry, DSConnection dsCon) {
        return super.apply(taskConfig, entry, dsCon);
    }


    /**
     * @param taskConfig
     * @param entry
     * @param dsCon
     * @return
     */
    protected WorkflowResult executeAllocations(TaskWorkflowConfig taskConfig,
                                                MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = new WorkflowResult();
        // if this list is defined, the allocations will be executed in the defined order, ie list one will be executed then list two...
        List<List<MarginCallAllocation>> orderedAllocationsBlocks = getOrderedAllocationsBlocks(entry);
        if (Util.isEmpty(orderedAllocationsBlocks)) {
            wfr = super.apply(taskConfig, entry, dsCon);
        } else {
            //start by removing the allocation before they will be executed in the defined order
            for (List<MarginCallAllocation> listAllocs : orderedAllocationsBlocks) {
                removeEntryAllocations(entry, listAllocs);
            }
            // execute the allocations block by block
            for (List<MarginCallAllocation> listAllocs : orderedAllocationsBlocks) {
                addEntryAllocations(entry, listAllocs);
                WorkflowResult tmpWfr = super.apply(taskConfig, entry, dsCon);
                if (tmpWfr != null) {
                    wfr.add(tmpWfr);
                }
            }
        }
        return wfr;
    }


    protected List<List<MarginCallAllocation>> getOrderedAllocationsBlocks(
            MarginCallEntry entry) {
        return new ArrayList<List<MarginCallAllocation>>();
    }

    /**
     * Get the list of allocations to be excluded from the current execution.
     *
     * @param entry
     * @return list of allocations to be excluded from the current execution.
     * By default this list is empty.
     */
    protected List<MarginCallAllocation> getExcludedAllocations(MarginCallEntry entry) {
        return new ArrayList<MarginCallAllocation>();
    }

    /**
     * remove the list of allocations from the given entry
     *
     * @param entry
     * @param allocations
     */
    void removeEntryAllocations(MarginCallEntry entry, List<MarginCallAllocation> allocations) {
        if (!Util.isEmpty(allocations)) {
            for (MarginCallAllocation alloc : allocations) {
                entry.removeAllocation(alloc);
            }
        }
    }

    /**
     * add the list of allocations to the given entry
     *
     * @param entry
     * @param allocations
     */
    void addEntryAllocations(MarginCallEntry entry, List<MarginCallAllocation> allocations) {
        if (!Util.isEmpty(allocations)) {
            for (MarginCallAllocation alloc : allocations) {
                entry.addAllocation(alloc);
            }
        }
    }
}

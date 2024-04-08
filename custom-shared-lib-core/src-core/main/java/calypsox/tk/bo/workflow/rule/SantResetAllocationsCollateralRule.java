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
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.List;

public class SantResetAllocationsCollateralRule extends BaseCollateralWorkflowRule {

    private static final String RULE_NAME = "SantResetAllocationsCollateralRule";

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
                                   List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {
        return true;
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = new WorkflowResult();
        try {
            List<MarginCallAllocation> allocations = entry.getAllocations();
            for (MarginCallAllocation allocation : allocations) {
                if ((allocation.getTradeId() != 0) || allocation.isLocked()) {
                    continue;
                }

                entry.removeAllocation(allocation);

            }
        } catch (Exception exc) {
            Log.error(RULE_NAME, exc);
            return wfr;
        }

        wfr.success();
        return wfr;
    }

    @Override
    public String getDescription() {
        return "Removes all the Allocation associated with the MarginCall Entry.";
    }
}

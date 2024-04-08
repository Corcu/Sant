/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.ExecuteCollateralRule;
import com.calypso.tk.collateral.*;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aela
 */
public class SantExecuteCollateralRule extends ExecuteCollateralRule {

    private final Map<String, Double> currentCashPosition = new HashMap<String, Double>();
    private final Map<String, List<MarginCallAllocation>> pendingCashPosition = new HashMap<String, List<MarginCallAllocation>>();
    private final static String GRM_CONTROL = "GRM_CONTROL";
    private static final String CALL_ACCOUNT = "NEW_CALL_ACCOUNT_CIRCUIT";


    /* (non-Javadoc)
     * @see calypsox.tk.bo.workflow.rule.SantDefaultExecuteCollateralRule#executeAllocations(com.calypso.tk.bo.TaskWorkflowConfig, com.calypso.tk.collateral.MarginCallEntry, com.calypso.tk.service.DSConnection)
     */
    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = null;
        List<MarginCallAllocation> pendingCashAllocations = entry
                .getPendingMarginAllocations(CashAllocation.UNDERLYING_TYPE);

        wfr = super.apply(taskConfig, entry,dsCon);
        //add GRM Keywords on allocations
        if (!Util.isEmpty(pendingCashAllocations)) {
            if (validGRMCollateralConfig(entry.getCollateralConfig())) {
                addStopPaymentKeywords(pendingCashAllocations, entry);
            }
        }
        return wfr;
    }

    /**
     * add the list of allocations to the given entry
     *
     * @param entry
     * @param allocations
     */
    protected void addEntryAllocations(MarginCallEntry entry, List<MarginCallAllocation> allocations) {
        if (!Util.isEmpty(allocations)) {
            for (MarginCallAllocation alloc : allocations) {
                entry.addAllocation(alloc);
            }
        }
    }

    private void addStopPaymentKeywords(List<MarginCallAllocation> pendingCashAllocations, MarginCallEntry entry) {
        if (!Util.isEmpty(pendingCashAllocations) && null != entry) {
            final Double globalRequiredMargin = entry.getGlobalRequiredMargin();
            double sumAllocations = 0.0;

            for (MarginCallAllocation allocation : pendingCashAllocations) {
                Double value = allocation.getContractValue();
                sumAllocations += value;
                if (allocation.getDirection().equalsIgnoreCase("Pay")) {
                    allocation.addAttribute("GRM_Control", "true");
                    if (globalRequiredMargin > value) {
                        allocation.addAttribute("GRM_Control", "false");
                    }
                }
            }

            for (MarginCallAllocation allocation : pendingCashAllocations) {
                if (allocation.getDirection().equalsIgnoreCase("Pay")) {
                    if (globalRequiredMargin > sumAllocations) {
                        allocation.addAttribute("GRM_Control", "false");
                    } else {
                        allocation.addAttribute("GRM_Control", "true");
                    }
                }
            }
        }
    }

    private boolean validGRMCollateralConfig(CollateralConfig config) {
        return null != config
                && "True".equalsIgnoreCase(config.getAdditionalField(GRM_CONTROL));
    }

}

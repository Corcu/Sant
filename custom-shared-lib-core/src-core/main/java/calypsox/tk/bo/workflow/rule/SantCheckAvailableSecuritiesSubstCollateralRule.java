/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

public class SantCheckAvailableSecuritiesSubstCollateralRule extends SantCheckAvailableSecuritiesCollateralRule {

	protected static String RULE_NAME = "SantCheckAvailableSecuritiesSubstCollateralRule";

	public SantCheckAvailableSecuritiesSubstCollateralRule() {
		setCheckPlainAllocsOnly(false);
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
			List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {
		return super.isApplicable(paramTaskWorkflowConfig, entry, paramEntityState1, paramEntityState2, paramList,
				dsCon, paramList1, paramTask, paramObject, paramList2);
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		return super.apply(taskConfig, entry, dsCon);
	}

	@Override
	public String getDescription() {
		return "Checks if any of the Substitution security allocations make the current position short";
	}
}

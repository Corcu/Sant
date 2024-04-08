/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.List;

import calypsox.tk.core.CollateralStaticAttributes;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

public class SantWarnExecShortCollateralRule extends BaseCollateralWorkflowRule {

	@SuppressWarnings("unused")
	private static final String RULE_NAME = "SantWarnExecShortCollateralRule";

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
			List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {
		boolean success = true;
		String errorMsg = "";
		if (!Util.isEmpty((String) entry.getAttribute("WarningExecutionShortMsg"))
				&& (entry.getAttribute(CollateralStaticAttributes.ENTRY_ATTR_WARNING_EXEC_SHORT) != null)
				&& !((Boolean) entry.getAttribute(CollateralStaticAttributes.ENTRY_ATTR_WARNING_EXEC_SHORT))) {
			errorMsg = (String) entry.getAttribute("WarningExecutionShortMsg");
			paramList.add(errorMsg);
			success = false;
		}

		return success;
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		WorkflowResult wfr = new WorkflowResult();
		wfr.success();
		return wfr;
	}

	@Override
	public String getDescription() {
		return "Checks if any of the security allocations make the current position short";
	}
}

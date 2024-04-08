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
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

/**
 * @author xIS15793
 * 
 */
public class SantCheckContinueExecuteGlobalMTACollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "This rule checks if 'Continue ExecuteGlobalMTA' attribute is checked";
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		Log.info(SantCheckContinueExecuteGlobalMTACollateralRule.class,
				"SantCheckContinueExecuteGlobalMTACollateralRule Check - Start");
		boolean result = false;

		String attribute = (String) entry.getAttribute("Continue ExecuteGlobalMTA");

		if (!Util.isEmpty(attribute)) {
			result = Boolean.parseBoolean(attribute);
		}

		Log.info(SantCheckContinueExecuteGlobalMTACollateralRule.class,
				"SantCheckContinueExecuteGlobalMTACollateralRule Check - End");
		return result;

	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantCheckContinueExecuteGlobalMTACollateralRule.class,
				"SantCheckContinueExecuteGlobalMTACollateralRule Update - Start");
		WorkflowResult wfr = new WorkflowResult();

		Log.info(SantCheckContinueExecuteGlobalMTACollateralRule.class,
				"SantCheckContinueExecuteGlobalMTACollateralRule Update - End");
		wfr.success();
		return wfr;
	}

}

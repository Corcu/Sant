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
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

public class SantSBWOSetFilterRatingCollateralRule extends BaseCollateralWorkflowRule {

	private static final String TRUE_VALUE = "True";
	private static final String SBWO_FILTER_RATING = "SBWO_FILTER_RATING";

	@Override
	public String getDescription() {
		return "Add SBWO_FILTER_RATING entity attribute to True if mccAdditionalField.SBWO_FILTER_RATING is True";
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskworkflowconfig, MarginCallEntry margincallentry,
			DSConnection dsconnection) {
		WorkflowResult wfr = new WorkflowResult();
		wfr.success();
		return wfr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig taskworkflowconfig, MarginCallEntry margincallentry,
			EntityState entitystate, EntityState entitystate1, List<String> list, DSConnection dsconnection,
			List<BOException> list1, Task task, Object obj, List<PSEvent> list2) {
		if ((margincallentry != null) && (margincallentry.getCollateralConfig() != null)) {
			if (TRUE_VALUE.equals(margincallentry.getCollateralConfig().getAdditionalField(SBWO_FILTER_RATING))) {
				margincallentry.addAttribute(SBWO_FILTER_RATING, TRUE_VALUE);
			}
		}
		return true;
	}
}

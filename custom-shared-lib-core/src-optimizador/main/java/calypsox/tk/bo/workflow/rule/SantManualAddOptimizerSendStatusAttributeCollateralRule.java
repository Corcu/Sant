package calypsox.tk.bo.workflow.rule;

import java.util.List;

import calypsox.tk.util.SantCollateralOptimConstants;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class SantManualAddOptimizerSendStatusAttributeCollateralRule extends
		BaseCollateralWorkflowRule implements SantCollateralOptimConstants {

	@Override
	public String getDescription() {
		return "Add manually attribute status field in order to send entry to the optimizer";
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskWorkflowConfig,
			MarginCallEntry paramMarginCallEntry, DSConnection dsCon) {

		WorkflowResult wfr = new WorkflowResult();
		// get the contract
		CollateralConfig mcc = paramMarginCallEntry.getCollateralConfig();
		if (mcc == null) {
			mcc = CacheCollateralClient.getCollateralConfig(dsCon,
					paramMarginCallEntry.getCollateralConfigId());
			if (mcc == null) {
				Log.error(
						SantManualAddOptimizerSendStatusAttributeCollateralRule.class,
						"Unable to get the margin call contract.");
				return wfr;
			}
		}

		try {
			if (!mcc.isExcludeFromOptimizer()) {
				// add attribute to entry in order to send it to Optimizer
				paramMarginCallEntry.addAttribute(OPTIMIZER_SEND_STATUS,
						OPTIMIZER_TO_BE_SENT_STATUS_VALUE);
			}
		} catch (Exception e) {
			Log.error(
					SantManualAddOptimizerSendStatusAttributeCollateralRule.class
							.getName(), e);
		}
		wfr.success();
		return wfr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig,
			MarginCallEntry paramMarginCallEntry,
			EntityState paramEntityState1, EntityState paramEntityState2,
			List<String> paramList, DSConnection paramDSConnection,
			List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		return true;
	}

}

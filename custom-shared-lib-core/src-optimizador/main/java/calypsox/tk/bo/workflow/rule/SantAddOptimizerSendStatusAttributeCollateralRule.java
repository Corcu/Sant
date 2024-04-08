package calypsox.tk.bo.workflow.rule;

import java.util.List;

import calypsox.tk.util.SantCollateralOptimConstants;
import calypsox.tk.util.SantCollateralOptimUtil;

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
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class SantAddOptimizerSendStatusAttributeCollateralRule extends
		BaseCollateralWorkflowRule implements SantCollateralOptimConstants {

	private static final String FREQUENCY = "FREQUENCY";

	@Override
	public String getDescription() {
		return "Add attribute status field in order to send entry to the optimizer";
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
						SantAddOptimizerSendStatusAttributeCollateralRule.class,
						"Unable to get the margin call contract.");
				return wfr;
			}
		}
		String frequency = paramMarginCallEntry.getCollateralConfig()
				.getAdditionalField(FREQUENCY);
		
		try {
			if (!Util.isEmpty(frequency)
					&& SantCollateralOptimUtil.acceptDateRuleBusinessDay(frequency,
							paramMarginCallEntry.getProcessDate())) {
				if (!mcc.isExcludeFromOptimizer() && !EXCLUDE_FROM_SOD_OPTIMIZER_TRUE_VALUE
						.equalsIgnoreCase(mcc.getAdditionalField(EXCLUDE_FROM_SOD_OPTIMIZER))) {
					// add attribute to entry in order to send it to Optimizer
					paramMarginCallEntry.addAttribute(OPTIMIZER_SEND_STATUS,
							OPTIMIZER_TO_BE_SENT_STATUS_VALUE);
				}
			} else {
				String msg = "Entry with id=" + paramMarginCallEntry.getId()
						+ "; Contract id="
						+ paramMarginCallEntry.getCollateralConfigId()
						+ "; Contract Name="
						+ paramMarginCallEntry.getCollateralConfig().getName()
						+ " is excluded by the Frequency DateRule";
				Log.info(SantAddOptimizerSendStatusAttributeCollateralRule.class, msg);
			}
		} catch (Exception e) {
			Log.error(SantAddOptimizerSendStatusAttributeCollateralRule.class
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

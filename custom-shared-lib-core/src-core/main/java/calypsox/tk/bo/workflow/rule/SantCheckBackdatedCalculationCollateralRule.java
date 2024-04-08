package calypsox.tk.bo.workflow.rule;

import java.util.Calendar;
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

/**
 * Check and forbid a backdated pricing/calculation.
 * 
 * @author aela
 * 
 */
public class SantCheckBackdatedCalculationCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Check and forbid a backdated pricing/calculation.";
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
		WorkflowResult wfr = new WorkflowResult();
		wfr.success();
		return wfr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		Calendar calProcessDate = Calendar.getInstance();
		calProcessDate.setTime(paramMarginCallEntry.getProcessDate().getDate(
				paramMarginCallEntry.getValuationTimeZone()));
		calProcessDate.set(Calendar.AM_PM, Calendar.AM);
		calProcessDate.set(Calendar.HOUR, 00);
		calProcessDate.set(Calendar.MINUTE, 00);
		calProcessDate.set(Calendar.SECOND, 00);
		calProcessDate.set(Calendar.MILLISECOND, 00);

		Calendar calCurrentDate = Calendar.getInstance(paramMarginCallEntry.getValuationTimeZone());
		calCurrentDate.set(Calendar.AM_PM, Calendar.AM);
		calCurrentDate.set(Calendar.HOUR, 00);
		calCurrentDate.set(Calendar.MINUTE, 00);
		calCurrentDate.set(Calendar.SECOND, 00);
		calCurrentDate.set(Calendar.MILLISECOND, 00);

		if (calCurrentDate.after(calProcessDate)) {
			paramList.add("Backdated calculations are not permitted.");
			return false;
		}

		return true;
	}
}

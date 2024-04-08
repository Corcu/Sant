package calypsox.tk.bo.workflow.rule;

import java.util.List;

import calypsox.tk.collateral.util.SantMarginCallUtil;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.CheckNoCallCollateralRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.ContextAttribute;
import com.calypso.tk.service.DSConnection;

/**
 * Check if the margin call is partially allocated keywords.
 * 
 * @author aela
 * 
 */
public class SantCheckPartialAllocationCollateralRule extends CheckNoCallCollateralRule {

	@Override
	public String getDescription() {
		return "Check if the margin call is partially allocated keywords.";
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
		boolean isPartial = false;
		double allocationAmount = paramMarginCallEntry.getCashMargin() + paramMarginCallEntry.getSecurityMargin();

		ContextAttribute tolerenceAttr = ServiceRegistry.getDefaultContext().getAttribute("FULL_ALLOCATION_TOLERENCE");
		double tolerence = 0.0;
		if (tolerenceAttr != null) {
			try {
				tolerence = Double.parseDouble(tolerenceAttr.getValue());

			} catch (Exception e) {
				Log.info(this, "no full allocation tolerence set (FULL_ALLOCATION_TOLERENCE) + \n" + e); //sonar
				tolerence = 0;
			}

		}

		System.out.println("tolerence " + tolerence + " \n");

		// decide if its a MarginCall full or partial delivery
		// notice
		if (SantMarginCallUtil.isReceiveMarginCall(paramMarginCallEntry)) {
			System.out.println("we're in receive case\n");
			System.out.println("global required margin is " + paramMarginCallEntry.getGlobalRequiredMargin() + " \n");
			System.out.println("global allocated is " + allocationAmount + " \n");

			//
			if (Math.abs(Math.abs(paramMarginCallEntry.getGlobalRequiredMargin()) - Math.abs(allocationAmount)) > tolerence) {
				isPartial = true;
			}

		} else if (SantMarginCallUtil.isPayMarginCall(paramMarginCallEntry)) {// PAY
			System.out.println("we're in pay case\n");

			Double cptyMargin = null; // Case
			try {
				cptyMargin = (Double) paramMarginCallEntry.getAttribute("Cpty Margin");
			} catch (Exception e) {
				Log.error(this, e); //sonar
				cptyMargin = null;
			}

			if (cptyMargin == null) {
				cptyMargin = paramMarginCallEntry.getGlobalRequiredMargin();
			}

			System.out.println("cpty margin call " + cptyMargin + " \n");

			if (Math.abs(Math.abs(allocationAmount) - Math.abs(cptyMargin)) > tolerence) {
				isPartial = true;
			}
		} else {
			paramList.add("The allocation is not partial");
		}
		return isPartial;
	}
}

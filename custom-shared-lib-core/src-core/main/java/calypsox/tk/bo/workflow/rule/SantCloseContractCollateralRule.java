package calypsox.tk.bo.workflow.rule;

import java.rmi.RemoteException;
import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.ContextAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * Close the contract on this margin call entry
 * 
 * @author aela
 * 
 */
public class SantCloseContractCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "Try to automatically dispute the given entry.";
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

		ContextAttribute toleranceAttr = ServiceRegistry.getDefaultContext().getAttribute("BALANCE_CLOSE_TOLERANCE");
		double tolerance = 0.0;
		if (toleranceAttr != null) {
			try {
				tolerance = Double.parseDouble(toleranceAttr.getValue());

			} catch (Exception e) {
				Log.info(this, "no close balance tolerance set (BALANCE_CLOSE_TOLERANCE) + \n" + e); //sonar
				paramList.add("no close balance tolerance set (BALANCE_CLOSE_TOLERANCE)");
				return false;
			}

		}

		// check that the contract can be closed
		if (Math.abs(paramMarginCallEntry.getPreviousTotalMargin()) > tolerance) {
			paramList.add("Unable to close the contract since the previous total margin is above the close tolerance: "
					+ tolerance);
			return false;
		}

		// get the contract
		// CollateralConfig mcc = paramMarginCallEntry.getMarginCallConfig();
		// if (mcc == null) {

		CollateralConfig cachedMcc = CacheCollateralClient.getCollateralConfig(paramDSConnection,
				paramMarginCallEntry.getCollateralConfigId());

		if (cachedMcc == null) {
			paramList.add("Unable to get the margin call contract.");
			return false;
		}

		CollateralConfig mcc = null;
		try {
			mcc = (CollateralConfig) cachedMcc.clone();
		} catch (CloneNotSupportedException ce) {
			Log.error(this, ce);
			paramList.add("Unable to get the margin call contract.");
			return false;
		}

		// }
		mcc.setClosingDate(paramMarginCallEntry.getProcessDatetime());
		mcc.setAgreementStatus(CollateralConfig.CLOSED);
		try {
			ServiceRegistry.getDefault().getCollateralDataServer().save(mcc);
			// paramDSConnection.getRemoteReferenceData().save(mcc);
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
			paramList.add("Unable to close the contract: Error while saving the contract: " + e.getMessage());
			return false;
		}
		return true;
	}
}

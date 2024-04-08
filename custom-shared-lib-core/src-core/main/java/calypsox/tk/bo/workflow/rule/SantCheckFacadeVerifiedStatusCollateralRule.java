/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * @author xIS15793
 * 
 */
public class SantCheckFacadeVerifiedStatusCollateralRule extends BaseCollateralWorkflowRule {

	@Override
	public String getDescription() {
		return "This rule checks if CSA_FACADE contract related to the contract is in VERIFIED status";
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		Log.info(SantCheckFacadeVerifiedStatusCollateralRule.class,
				"SantCheckFacadeVerifiedStatusCollateralRule Check - Start");
		boolean result = true;

		CollateralConfig contract = entry.getCollateralConfig();

		// we need to check if contract is Subtype = Facade
		if (CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(contract.getSubtype())) {
			List<Integer> mccIds = new ArrayList<Integer>();
			mccIds.add(contract.getId());

			// get entry for today and check its status
			List<MarginCallEntryDTO> facadeEntries = null;
			try {
				facadeEntries = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
						.loadEntries(mccIds, JDate.getNow(), Integer.valueOf(entry.getCollateralContext().getId()));

				// if entries not null and status = VERIFIED
				if (!Util.isEmpty(facadeEntries)
						&& Status.VERIFIED.equalsIgnoreCase(facadeEntries.get(0).getStatus())) {
					result = true;
				} else {
					result = false;
				}
			} catch (RemoteException e) {
				result = false;
				Log.error(this, "Couldn't get today Entries for contracts " + mccIds.toString());
				Log.error(this, e); //sonar purpose
			}
		}

		Log.info(SantCheckFacadeVerifiedStatusCollateralRule.class,
				"SantCheckFacadeVerifiedStatusCollateralRule Check - End");
		return result;
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantCheckFacadeVerifiedStatusCollateralRule.class,
				"SantCheckFacadeVerifiedStatusCollateralRule Update - Start");
		WorkflowResult wfr = new WorkflowResult();

		Log.info(SantCheckFacadeVerifiedStatusCollateralRule.class,
				"SantCheckFacadeVerifiedStatusCollateralRule Update - End");
		wfr.success();
		return wfr;
	}
}

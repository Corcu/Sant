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
import java.util.TimeZone;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ACTION;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

public class SantPriceIMFacadeCollateralRule extends BaseCollateralWorkflowRule {

	private static final String IM = "IM";

	@Override
	public String getDescription() {
		return "Force Price on Facade";
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		return true;
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantPriceIMFacadeCollateralRule.class, "SantPriceIMFacadeCollateralRule - Start");
		WorkflowResult wfr = new WorkflowResult();

		CollateralConfig contract = entry.getCollateralConfig();

		if (!CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(contract.getContractType())
				&& IM.equalsIgnoreCase(contract.getContractGroup())) {

			// get Parent FACADE
			int parentId = contract.getParentId();

			Log.info(SantPriceIMFacadeCollateralRule.class, "parentId: " + parentId);

			if (parentId > 0) {
				try {
					List<Integer> mccIds = new ArrayList<Integer>();
					mccIds.add(parentId);

					List<MarginCallEntryDTO> parentEntry = ServiceRegistry.getDefault(DSConnection.getDefault())
							.getCollateralServer().loadEntries(mccIds, JDate.getNow(), Integer.valueOf(entry.getCollateralContext().getId()));

					Log.info(SantPriceIMFacadeCollateralRule.class, "parentEntry: " + parentEntry);

					if (!Util.isEmpty(parentEntry)) {

						Log.info(SantPriceIMFacadeCollateralRule.class, "parentEntry(0): " + parentEntry.get(0));

						int entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
								.save(parentEntry.get(0), ACTION.PRICE.toString(), TimeZone.getDefault());

						Log.info(this, "Action " + ACTION.PRICE.toString()
								+ "successfully applied on CSA_FACADE contract Entry with id " + entryId);
					}

				} catch (RemoteException e) {
					Log.error(this, "Couldn't get MarginCallEntry from MarginCallEntryDTO " + parentId);
					Log.error(this, e); //sonar
				}
			}
		}

		Log.info(SantPriceIMFacadeCollateralRule.class, "SantPriceIMFacadeCollateralRule - End");
		wfr.success();
		return wfr;
	}
}
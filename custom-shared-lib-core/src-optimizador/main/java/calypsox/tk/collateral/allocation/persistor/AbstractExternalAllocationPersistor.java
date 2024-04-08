/**
 * 
 */
package calypsox.tk.collateral.allocation.persistor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import calypsox.tk.bo.workflow.rule.SantAddOptimizerSendStatusAttributeCollateralRule;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;


/**
 * @author aela
 *
 */
 public class AbstractExternalAllocationPersistor implements ExternalAllocationPersistor {
	
	protected ExternalAllocationImportContext context;
	
	public AbstractExternalAllocationPersistor() {
	}
	
	/**
	 * @param context
	 */
	public AbstractExternalAllocationPersistor(ExternalAllocationImportContext context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.persistor.ExternalAllocationPersistorInterface#persistAllocation(com.calypso.tk.collateral.MarginCallAllocation, com.calypso.tk.collateral.MarginCallEntry, java.util.List)
	 */
	@Override
	public void persistAllocation(MarginCallAllocation allocation,
			MarginCallEntry entry, String action, List<String> messages)
			throws Exception {}
	
	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.persistor.ExternalAllocationPersistorInterface#persistAllocation(com.calypso.tk.collateral.MarginCallAllocation, com.calypso.tk.collateral.MarginCallEntry, java.util.List)
	 */
	@Override
	public void persistEntry(MarginCallEntry entry, String action, List<String> messages)
			throws Exception {
		
		ExecutionContext executionContext = context.getExecutionContext();
		prePersistenceProcessing(entry);
		if ((entry == null)) {
			return;
		}
		calculateAndSaveEntry(entry, executionContext,action, messages);
		postPersistenceProcessing(entry);
	}

	protected void postPersistenceProcessing(MarginCallEntry entry) {
	}

	protected void prePersistenceProcessing(MarginCallEntry entry) {
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.persistor.ExternalAllocationPersistorInterface#persistAllocations(java.util.List, com.calypso.tk.collateral.MarginCallEntry, java.util.List)
	 */
	@Override
	public void persistAllocations(List<MarginCallAllocation> allocations,
			MarginCallEntry entry, String action, List<String> messages)
			throws Exception {
		
	}
	
	/**
	 * Calculate and save the given margin call entries
	 * 
	 * @param entries
	 * @param execContext
	 * @param errors
	 */
	public static void calculateAndSaveEntry(final MarginCallEntry entry,
			final ExecutionContext execContext, String action, final List<String> errors) {

		if (entry == null) {
			return;
		}
		List<MarginCallEntry> entries = new ArrayList<MarginCallEntry>();
		entries.add(entry);
		try {
			CollateralManagerUtil.saveEntries(entries, action, errors);			
		}
		catch (Exception e) {
			Log.error(AbstractExternalAllocationPersistor.class, e); //sonar
			MarginCallEntryDTO reloadedEntryDTO = null;
			// TODO limit the second save just to the mismatch version
			// error
			try {
				reloadedEntryDTO = ServiceRegistry
						.getDefault(DSConnection.getDefault())
						.getCollateralServer()
						.loadEntry(entry.getId());
					int entryId = ServiceRegistry
							.getDefault(DSConnection.getDefault())
							.getCollateralServer()
							.save(reloadedEntryDTO, action,
									TimeZone.getDefault());
					Log.info(AbstractExternalAllocationPersistor.class,
							 "Entry with id " + entryId
							+ " successfully saved for the contract "
							+ entry.getCollateralConfigId());
			}
			catch (RemoteException re) {
				Log.error(
						SantAddOptimizerSendStatusAttributeCollateralRule.class
								.getName(), re);
			}
		}
	}
}

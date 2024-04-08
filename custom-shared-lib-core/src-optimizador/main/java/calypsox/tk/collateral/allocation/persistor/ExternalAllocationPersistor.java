/**
 * 
 */
package calypsox.tk.collateral.allocation.persistor;

import java.util.List;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 * 
 */
public interface ExternalAllocationPersistor {

	/**
	 * @param allocation
	 * @param entry
	 * @param action
	 * @param messages
	 * @throws Exception
	 */
	public void persistAllocation(MarginCallAllocation allocation,
			MarginCallEntry entry, String action, List<String> messages)
			throws Exception;

	/**
	 * @param allocations
	 * @param entry
	 * @param action
	 * @param messages
	 * @throws Exception
	 */
	public void persistAllocations(List<MarginCallAllocation> allocations,
			MarginCallEntry entry, String action, List<String> messages)
			throws Exception;

	/**
	 * @param entry
	 * @param action
	 * @param messages
	 * @throws Exception
	 */
	public void persistEntry(MarginCallEntry entry, String action,
			List<String> messages) throws Exception;
}

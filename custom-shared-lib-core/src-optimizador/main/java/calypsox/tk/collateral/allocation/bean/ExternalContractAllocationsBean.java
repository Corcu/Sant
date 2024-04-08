/**
 * 
 */
package calypsox.tk.collateral.allocation.bean;

import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 *
 */
public class ExternalContractAllocationsBean extends ExternalAllocationBean {
	
	protected MarginCallEntry marginCallEntry;
	protected List<ExternalAllocationBean> entryAllocations;
	/**
	 * @return the marginCallEntry
	 */
	public MarginCallEntry getMarginCallEntry() {
		return marginCallEntry;
	}
	/**
	 * @param marginCallEntry the marginCallEntry to set
	 */
	public void setMarginCallEntry(MarginCallEntry marginCallEntry) {
		this.marginCallEntry = marginCallEntry;
	}
	/**
	 * @return the entryAllocations
	 */
	public List<ExternalAllocationBean> getEntryAllocations() {
		return entryAllocations;
	}
	/**
	 * @param entryAllocations the entryAllocations to set
	 */
	public void setEntryAllocations(List<ExternalAllocationBean> entryAllocations) {
		this.entryAllocations = entryAllocations;
	}
	
	/**
	 * @param allocation
	 */
	public void addAllocation(ExternalAllocationBean allocation) {
		
		if(entryAllocations == null) {
			entryAllocations = new ArrayList<ExternalAllocationBean>();
		}
		entryAllocations.add(allocation);
		
	}
	
	/**
	 * @param allocations
	 */
	public void addAllocations(List<ExternalAllocationBean> allocations) {
		
		if(entryAllocations == null) {
			entryAllocations = new ArrayList<ExternalAllocationBean>();
		}
		entryAllocations.addAll(allocations);
		
	}
	
}

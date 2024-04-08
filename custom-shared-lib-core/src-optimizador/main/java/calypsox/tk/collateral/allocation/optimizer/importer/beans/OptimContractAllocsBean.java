/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.beans;

import java.util.ArrayList;
import java.util.List;

public class OptimContractAllocsBean extends OptimAllocationBean {

	private String contractName;
	private List<OptimAllocationBean> allocations;

	public OptimContractAllocsBean(OptimAllocationBean alloc) {
		this.contractName = alloc.getContractName();
		this.allocations = new ArrayList<OptimAllocationBean>();
		this.allocations.add(alloc);
	}

	/**
	 * @return the contractName
	 */
	@Override
	public String getContractName() {
		return this.contractName;
	}

	/**
	 * @param contractName
	 *            the contractName to set
	 */
	@Override
	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	/**
	 * @return the allocations
	 */
	public List<OptimAllocationBean> getAllocations() {
		return this.allocations;
	}

	/**
	 * @param allocations
	 *            the allocations to set
	 */
	public void setAllocations(List<OptimAllocationBean> allocations) {
		this.allocations = allocations;
	}
	
	public String getKey() {
		StringBuffer sb = new StringBuffer("");
		sb.append(nullIfEmpty(getContractName()));
		 
		return sb.toString();
	}

}

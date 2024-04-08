/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

/**
 * cache for knowing the information of clone collateral contracts.
 * 
 * @author xIS16241
 * 
 */
public class SantEmirCacheCloneContracts implements Serializable {

	/**
	 * serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/** clone contract id */
	protected int cloneContractId;

	/** head contract name */
	protected String headContractName;

	/** head contract id */
	protected int headContractId;

	/** value of the collateral */
	protected double valueCloneContract;


	/**
	 * constructor
	 */
	public SantEmirCacheCloneContracts() {

	}

	static SantEmirCacheCloneContracts[] initializeCacheCloneContracts(int length) {
		SantEmirCacheCloneContracts[] contracts = new SantEmirCacheCloneContracts[length + 1];
	    for(int i = 0; i < length + 1; i++) {
	        contracts[i] = new SantEmirCacheCloneContracts();
	    }
	    return contracts;
	}
	
	public static SantEmirCacheCloneContracts[] copy(final SantEmirCacheCloneContracts[] origin, final int length) {
		SantEmirCacheCloneContracts[] contracts = initializeCacheCloneContracts(length);
	    for(int i=0; origin[i].getHeadContractName()!=null; i++){
	    	contracts[i] = origin[i];
	    }
	    return contracts;
	}
	public int getCloneContractId() {
		return this.cloneContractId;
	}

	public void setCloneContractId(int cloneContractId) {
		this.cloneContractId = cloneContractId;
	}
	
	public int getHeadContractId() {
		return this.headContractId;
	}

	public void setHeadContractId(int headContractId) {
		this.headContractId = headContractId;
	}

	public String getHeadContractName() {
		return this.headContractName;
	}

	public void setHeadContractName(String headContractName) {
		this.headContractName = headContractName;
	}

	public double getValueCloneContract() {
		return this.valueCloneContract;
	}

	public void setValueCloneContract(double valueCloneContract) {
		this.valueCloneContract = valueCloneContract;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.reader;

import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;


/**
 * @author aela
 * 
 */
public interface ExternalAllocationReader {

	/**
	 * @param messages
	 * @return
	 * @throws Exception
	 */
	public List<? extends ExternalAllocationBean> readAllocations(List<AllocImportErrorBean> errors) throws Exception;
	
	/**
	 * @param message
	 * @param messages
	 * @return
	 * @throws Exception
	 */
	public ExternalAllocationBean readAllocation(String message, List<AllocImportErrorBean> errors) throws Exception;

}

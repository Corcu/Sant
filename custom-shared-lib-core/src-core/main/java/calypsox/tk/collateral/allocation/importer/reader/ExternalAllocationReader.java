/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.importer.reader;

import java.util.List;

import calypsox.tk.collateral.allocation.importer.ExternalAllocationBean;

/**
 * @author aela
 * 
 */
public interface ExternalAllocationReader {

	public List<? extends ExternalAllocationBean> readAllocations(List<String> messages) throws Exception;

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.importer.mapper;

import java.util.List;

import calypsox.tk.collateral.allocation.importer.ExternalAllocationBean;

import com.calypso.tk.collateral.MarginCallAllocation;

/**
 * @author aela
 * 
 */
public interface ExternalAllocationMapper {

	public MarginCallAllocation mapAllocation(ExternalAllocationBean allocBean, List<String> messages) throws Exception;

	public boolean isValidAllocation(ExternalAllocationBean allocBean, List<String> messages) throws Exception;

}

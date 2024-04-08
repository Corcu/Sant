/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.mapper;

import java.util.List;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;

import com.calypso.tk.collateral.MarginCallAllocation;

/**
 * @author aela
 * 
 */
public interface ExternalAllocationMapper {

	public MarginCallAllocation mapAllocation(OptimAllocationBean allocBean, List<AllocImportErrorBean> messages) throws Exception;

	public boolean isValidAllocation(OptimAllocationBean allocBean, List<AllocImportErrorBean> messages) throws Exception;

}

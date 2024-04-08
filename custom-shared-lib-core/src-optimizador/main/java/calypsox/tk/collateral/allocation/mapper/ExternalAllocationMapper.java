/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.mapper;

import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 * 
 */
public interface ExternalAllocationMapper {

	public MarginCallAllocation mapAllocation(ExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) throws Exception;

	public boolean isValidAllocation(ExternalAllocationBean allocBean,
			MarginCallEntry entry, List<AllocImportErrorBean> messages)
			throws Exception;

}

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

import java.io.InputStream;
import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;

/**
 * @author aela
 * 
 */
public class FileExternalAllocationReader extends
		AbstractExternalAllocationReader {

	
	/**
	 * @param is
	 * @param context
	 */
	public FileExternalAllocationReader(InputStream is,
			ExternalAllocationImportContext context) {
		super(is, context);
	}
	
	
	@Override
	public ExternalAllocationBean readAllocation(String message,
			List<AllocImportErrorBean> errors) throws Exception {
		return null;
	}}

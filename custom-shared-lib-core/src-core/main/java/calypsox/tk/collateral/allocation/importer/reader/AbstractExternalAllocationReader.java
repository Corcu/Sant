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
public abstract class AbstractExternalAllocationReader implements ExternalAllocationReader {

	protected final String fileToImportPath;

	public AbstractExternalAllocationReader(String fileToImport) {
		this.fileToImportPath = fileToImport;

	}

	@Override
	public List<ExternalAllocationBean> readAllocations(List<String> messages) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return the fileToImportPath
	 */
	public String getFileToImportPath() {
		return this.fileToImportPath;
	}
}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.apps.reporting.balanza;

import calypsox.tk.util.bean.ExternalBalanzaBean;

import java.util.List;

/**
 * @author aela
 * 
 */
public abstract class AbstractExternalBalanzaReader implements ExternalBalanzaReader {

	protected final String fileToImportPath;

	public AbstractExternalBalanzaReader(String fileToImport) {
		this.fileToImportPath = fileToImport;

	}

	@Override
	public List<ExternalBalanzaBean> readLines(List<String> messages) throws Exception {
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

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
public interface ExternalBalanzaReader {

	public List<? extends ExternalBalanzaBean> readLines(List<String> messages) throws Exception;

}

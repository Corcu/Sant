/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.importer;

import com.calypso.tk.core.Product;

/**
 * @author aela
 * 
 */
public class SecurityExternalAllocationBean extends ExternalAllocationBean {

	private String isin;
	private Product security;

	/**
	 * @return the isin
	 */
	public String getIsin() {
		return this.isin;
	}

	/**
	 * @param isin
	 *            the isin to set
	 */
	public void setIsin(String isin) {
		this.isin = isin;
	}

	/**
	 * @return the security
	 */
	public Product getSecurity() {
		return this.security;
	}

	/**
	 * @param security
	 *            the security to set
	 */
	public void setSecurity(Product security) {
		this.security = security;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.beans;

import com.calypso.tk.core.Product;


public class OptimSecurityAllocationBean extends OptimAllocationBean {
	private Product security;

	public Product getSecurity() {
		return security;
	}

	public void setSecurity(Product sec) {
		security = sec;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.importer;

public class CashExternalAllocationBean extends ExternalAllocationBean {

	private String currency;

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}

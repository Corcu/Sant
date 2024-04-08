/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.bean;

import java.io.Serializable;

public class BondCrossReferencesBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String IS = "IS";
	public static final String SE = "SE";
	public static final String CU = "CU";

	protected String internalRef;
	protected String isin;
	protected String cusip;
	protected String sedol;

	public boolean addReferences(String refCode, String value) {
		if (IS.equals(refCode)) {
			setIsin(value);
		} else if (SE.equals(refCode)) {
			setSedol(value);
		} else if (CU.equals(refCode)) {
			setCusip(value);
		} else {
			return false;
		}

		return true;
	}

	public String getInternalRef() {
		return this.internalRef;
	}

	public void setInternalRef(String internalRef) {
		this.internalRef = internalRef;
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getCusip() {
		return this.cusip;
	}

	public void setCusip(String cusip) {
		this.cusip = cusip;
	}

	public String getSedol() {
		return this.sedol;
	}

	public void setSedol(String sedol) {
		this.sedol = sedol;
	}
}

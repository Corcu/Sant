/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.bean;

import java.io.Serializable;

public class ECBLiquidityClassAssetTypeBean implements Serializable {

	private static final long serialVersionUID = 1L;

	protected String internalRef;
	protected String eCBLiquidityClass;
	protected String eCBAssetType;

	public ECBLiquidityClassAssetTypeBean(String[] fields) {
		this.internalRef = fields[0];
		this.eCBLiquidityClass = fields[1];
		this.eCBAssetType = fields[2];
	}

	public String getInternalRef() {
		return this.internalRef;
	}

	public void setInternalRef(String internalRef) {
		this.internalRef = internalRef;
	}

	public String getECBLiquidityClass() {
		return this.eCBLiquidityClass;
	}

	public void setECBLiquidityClass(String eCBLiquidityClass) {
		this.eCBLiquidityClass = eCBLiquidityClass;
	}

	public String getECBAssetType() {
		return this.eCBAssetType;
	}

	public void setECBAssetType(String eCBAssetType) {
		this.eCBAssetType = eCBAssetType;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.bean;

import java.io.Serializable;

public class ECBAttributesBean implements Serializable {

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = -3168154180896236130L;
	// END OA OA 27/11/2013

	protected String isin;
	protected String ccy;
	protected String eCBLiquidityClass;
	protected String eCBAssetType;
	protected String eCBHaircut;
	protected String date;

	public ECBAttributesBean(String[] fields) {
		this.isin = fields[0];
		this.ccy = fields[1];
		this.eCBLiquidityClass = fields[2];
		this.eCBAssetType = fields[3];
		this.eCBHaircut = fields[4];
		this.date = fields[5];
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getCcy() {
		return this.ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
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

	public String getECBHaircut() {
		return this.eCBHaircut;
	}

	public void setECBHaircut(String eCBHaircut) {
		this.eCBHaircut = eCBHaircut;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(String date) {
		this.date = date;
	}

}

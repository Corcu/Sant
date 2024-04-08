/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

/**
 * 
 * @author gsaiz
 * 
 */
public class SantMCTripartyItem implements Serializable {

	private static final long serialVersionUID = 123L;

	private String id;
	private String isin;
	private String currency;
	private String direction;
	private Double collateralValue;
	private Double collateralFather;
	private Double nominal;
	private Double collateralFatherAlloc;

	/**
	 * @return the collateralFatherAlloc
	 */
	public Double getCollateralFatherAlloc() {
		return this.collateralFatherAlloc;
	}

	/**
	 * @param collateralFatherAlloc
	 *            the collateralFatherAlloc to set
	 */
	public void setCollateralFatherAlloc(Double collateralFatherAlloc) {
		this.collateralFatherAlloc = collateralFatherAlloc;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

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
	 * @return the currency
	 */
	public String getCurrency() {
		return this.currency;
	}

	/**
	 * @param currency
	 *            the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * @return the collateralValue
	 */
	public Double getCollateralValue() {
		return this.collateralValue;
	}

	/**
	 * @param collateralValue
	 *            the collateralValue to set
	 */
	public void setCollateralValue(Double collateralValue) {
		this.collateralValue = collateralValue;
	}

	/**
	 * @return the collateralFather
	 */
	public Double getCollateralFather() {
		return this.collateralFather;
	}

	/**
	 * @param collateralFather
	 *            the collateralFather to set
	 */
	public void setCollateralFather(Double collateralFather) {
		this.collateralFather = collateralFather;
	}

	/**
	 * @return the nominal
	 */
	public Double getNominal() {
		return this.nominal;
	}

	/**
	 * @param nominal
	 *            the nominal to set
	 */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * @return direction
	 */
	public String getDirection() {
		return this.direction;
	}

	/**
	 * @param direcrion
	 * 
	 */
	public void setDirection(String direction) {
		this.direction = direction;
	}

}

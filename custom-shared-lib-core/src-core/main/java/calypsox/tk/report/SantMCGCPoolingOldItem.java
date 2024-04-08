/*
 *
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

/**
 * 
 * @author Juan Angel Torija
 * 
 */
public class SantMCGCPoolingOldItem implements Serializable {

	private static final long serialVersionUID = 123L;

	private String isinAlloc;
	private String currency;
	private Double collateralValue;
	private Double nominal;
	private String isinCesta;
	private Double collateralCesta;
	private Double nominalCesta;
	private Double collateralCestaAlloc;
	private String direction;

	/**
	 * @return the isinAlloc
	 */
	public String getIsinAlloc() {
		return this.isinAlloc;
	}

	/**
	 * @param isinAlloc
	 *            the isinAlloc to set
	 */
	public void setIsinAlloc(String isinAlloc) {
		this.isinAlloc = isinAlloc;
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
	 * @return the isinCesta
	 */
	public String getIsinCesta() {
		return this.isinCesta;
	}

	/**
	 * @param isinCesta
	 *            the isinCesta to set
	 */
	public void setIsinCesta(String isinCesta) {
		this.isinCesta = isinCesta;
	}

	/**
	 * @return the collateralCesta
	 */
	public Double getCollateralCesta() {
		return this.collateralCesta;
	}

	/**
	 * @param collateralCesta
	 *            the collateralCesta to set
	 */
	public void setCollateralCesta(Double collateralCesta) {
		this.collateralCesta = collateralCesta;
	}

	/**
	 * @return the nominalCesta
	 */
	public Double getNominalCesta() {
		return this.nominalCesta;
	}

	/**
	 * @param nominalCesta
	 *            the nominalCesta to set
	 */
	public void setNominalCesta(Double nominalCesta) {
		this.nominalCesta = nominalCesta;
	}

	/**
	 * @return the collateralCestaaAlloc
	 */
	public Double getCollateralCestaAlloc() {
		return this.collateralCestaAlloc;
	}

	/**
	 * @param collateralCestaAlloc
	 *            the collateralCestaaAlloc to set
	 */
	public void setCollateralCestaAlloc(Double collateralCestaAlloc) {
		this.collateralCestaAlloc = collateralCestaAlloc;
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}

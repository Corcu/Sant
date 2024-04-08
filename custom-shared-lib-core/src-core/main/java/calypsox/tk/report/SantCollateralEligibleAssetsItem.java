package calypsox.tk.report;

import com.calypso.tk.core.JDate;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 *
 */
public class SantCollateralEligibleAssetsItem {

	private String isin;
	private String currency;
	private String faceValue;
	private String dirtyPrice;
	private String cleanPrice;
	private String poolFactor;
	private String maturityDate;
	private String processDate;
	private String productType;

	/**
	 * @return ISIN
	 */
	public String getIsin() {
		return isin;
	}

	/**
	 * @param isin
	 *            - ISIN
	 */
	public void setIsin(String isin) {
		this.isin = isin;
	}

	/**
	 * @return Currency
	 */
	public String getCurrency() {
		return currency;
	}

	/**
	 * @param currency
	 *            - Currency
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * @return Face Value
	 */
	public String getFaceValue() {
		return faceValue;
	}

	/**
	 * @param faceValue
	 *            - Face Value
	 */
	public void setFaceValue(String faceValue) {
		this.faceValue = faceValue;
	}

	/**
	 * @return - Dirty Price
	 */
	public String getDirtyPrice() {
		return dirtyPrice;
	}

	/**
	 * @param dirtyPrice
	 *            - Dirty Price
	 */
	public void setDirtyPrice(String dirtyPrice) {
		this.dirtyPrice = dirtyPrice;
	}

	/**
	 * @return - Clean Price
	 */
	public String getCleanPrice() {
		return cleanPrice;
	}

	/**
	 * @param cleanPrice
	 *            - Clean Price
	 */
	public void setCleanPrice(String cleanPrice) {
		this.cleanPrice = cleanPrice;
	}

	/**
	 * @return - Pool Factor
	 */
	public String getPoolFactor() {
		return poolFactor;
	}

	/**
	 * @param poolFactor
	 *            - Pool Factor
	 */
	public void setPoolFactor(String poolFactor) {
		this.poolFactor = poolFactor;
	}

	/**
	 * @return - Maturity Date
	 */
	public String getMaturityDate() {
		return maturityDate;
	}

	/**
	 * @param maturityDate
	 *            - Maturity Date
	 */
	public void setMaturityDate(String maturityDate) {
		this.maturityDate = maturityDate;
	}

	/**
	 * @return - Process Date
	 */
	public String getProcessDate() {
		return processDate;
	}

	/**
	 * @param processDate
	 *            - Process Date
	 */
	public void setProcessDate(String processDate) {
		this.processDate = processDate;
	}

	/**
	 * @return - Product Type
	 */
	public String getProductType() {
		return productType;
	}

	/**
	 * @param productType
	 *            - Product Type
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}

}

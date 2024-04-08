package calypsox.tk.collateral.allocation.bean;

public class SecurityExternalAllocationBean extends ExternalAllocationBean {

	private String assetISIN;
	private Double assetPrice;
	private Double assetHaircut;
	
	
	/**
	 * @return the assetISIN
	 */
	public String getAssetISIN() {
		return assetISIN;
	}
	/**
	 * @param assetISIN the assetISIN to set
	 */
	public void setAssetISIN(String assetISIN) {
		this.assetISIN = assetISIN;
	}
	/**
	 * @return the assetPrice
	 */
	public Double getAssetPrice() {
		return assetPrice;
	}
	/**
	 * @param assetPrice the assetPrice to set
	 */
	public void setAssetPrice(Double assetPrice) {
		this.assetPrice = assetPrice;
	}
	/**
	 * @return the assetHaircut
	 */
	public Double getAssetHaircut() {
		return assetHaircut;
	}
	/**
	 * @param assetHaircut the assetHaircut to set
	 */
	public void setAssetHaircut(Double assetHaircut) {
		this.assetHaircut = assetHaircut;
	}


}

/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.beans;

/**
 * @author Guillermo Solano
 * 
 */
public class QuoteBeanOUT {

	public enum QUOTE_TYPE {
		BOND, EQUITY;
	}

	private String ISIN;
	private String currency;
	private Double bidPrice;
	private Double askPrice;
	private QUOTE_TYPE type;

	/**
	 * @return the iSIN
	 */
	public String getISIN() {
		return this.ISIN;
	}

	/**
	 * @param iSIN
	 *            the iSIN to set
	 */
	public void setISIN(String iSIN) {
		this.ISIN = iSIN;
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
	 * @return the bidPrice
	 */
	public Double getBidPrice() {
		return this.bidPrice;
	}

	/**
	 * @param price
	 *            the bid Price to set
	 */
	public void setBidPrice(double price) {
		this.bidPrice = price;
	}

	/**
	 * @return the ask Price
	 */
	public Double getAskPrice() {
		return this.askPrice;
	}

	/**
	 * @param price
	 *            the ask Price to set
	 */
	public void setAskPrice(double price) {
		this.askPrice = price;
	}

	/**
	 * @return the type
	 */
	public QUOTE_TYPE getType() {
		return this.type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(QUOTE_TYPE type) {
		this.type = type;
	}

	/**
	 * Override toString. Represented as Bond|Equity.ISIN_Currency
	 */
	@Override
	public String toString() {
		return ((getType().equals(QUOTE_TYPE.BOND) ? "Bond" : "Equity") + "." + getISIN() + "_" + getCurrency());
	}

}

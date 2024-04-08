/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.beans;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT.QUOTE_TYPE;

/**
 * @author Guillermo
 * 
 */
public class QuoteBeanIN {

	private String ISIN;
	private String currency;
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
	 * override to string
	 */
	@Override
	public String toString() {
		return (this.type.name() + ": " + this.ISIN + "_" + this.currency);
	}

}

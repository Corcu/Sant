package calypsox.tk.report;

/**
 * Class with the item definition for the EquityPrices report.
 * @author David Porras Mart?nez
 */
public class ExpTLM_EquityPricesItem {
	
	/**
     * Identify the object ExpTLM_EquityPricesItem.
     */
	public static final String EXPTLM_EQUITYPRICES_ITEM = "ExpTLM_EquityPricesItem";
	
	//Customized columns.
	private String fh_concilia;
	private String feed;
	private String lado;
	private String price;
	private String isin;
	private String divisa;
	
	public ExpTLM_EquityPricesItem(){}

	/**
	 * Retrieve the date.
	 * @param  
	 * @return String with the date.
	 */
	public String getFecha() {
		return fh_concilia;
	}

	/**
	 * Set the date.
	 * @param  fh_concilia String with the date.
	 * @return 
	 */
	public void setFecha(String fh_concilia) {
		this.fh_concilia = fh_concilia;
	}

	/**
	 * Retrieve the feed.
	 * @param  
	 * @return String with the feed.
	 */
	public String getFeed() {
		return feed;
	}
	
	/**
	 * Set the feed.
	 * @param  feed String with the feed.
	 * @return 
	 */
	public void setFeed(String feed) {
		this.feed = feed;
	}
	
	/**
	 * Retrieve the lado.
	 * @param  
	 * @return String with the lado.
	 */
	public String getLado() {
		return lado;
	}

	/**
	 * Set the lado.
	 * @param  lado String with the lado.
	 * @return 
	 */
	public void setLado(String lado) {
		this.lado = lado;
	}
	
	/**
	 * Retrieve the price.
	 * @param  
	 * @return String with the price.
	 */
	public String getPrice() {
		return price;
	}
	
	/**
	 * Set the price.
	 * @param  
	 * @return String with the price.
	 */
	public void setPrice(String price) {
		this.price = price;
	}
	
	/**
	 * Retrieve the ISIN.
	 * @param  
	 * @return String with the ISIN.
	 */
	public String getIsin() {
		return isin;
	}
	
	/**
	 * Set the ISIN.
	 * @param  isin String with the ISIN.
	 * @return 
	 */
	public void setIsin(String isin) {
		this.isin = isin;
	}
	
	/**
	 * Retrieve divisa.
	 * @param  
	 * @return String with the currency.
	 */
	public String getDivisa() {
		return divisa;
	}

	/**
	 * Set divisa
	 * @param  divisa String with the currency.
	 * @return 
	 */
	public void setDivisa(String divisa) {
		this.divisa = divisa;
	}
}

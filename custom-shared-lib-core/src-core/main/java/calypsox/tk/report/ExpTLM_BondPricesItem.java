package calypsox.tk.report;

/**
 * Class with the item definition for the BondPrices report.
 * @author David Porras Mart?nez
 */
public class ExpTLM_BondPricesItem {
	
	/**
     * Identify the object ExpTLM_BondPricesItem.
     */
	public static final String EXPTLM_BONDPRICES_ITEM = "ExpTLM_BondPricesItem";
	
	//Customized columns.
	private String fh_concilia;
	private String feed;
	private String lado;
	//private double price;
	private String price;
	private String isin;
	
	public ExpTLM_BondPricesItem(){}

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
}

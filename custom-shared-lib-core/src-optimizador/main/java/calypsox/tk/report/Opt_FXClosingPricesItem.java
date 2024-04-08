package calypsox.tk.report;

/**
 * Class with the item definition for the FXClosingPrices report.
 * @author David Porras Mart?nez
 */
public class Opt_FXClosingPricesItem {
	
	/**
     * Identify the object FXClosingPricesItem.
     */
	public static final String EXPTLM_FXCLOSINGPRICES_ITEM = "Opt_FXClosingPricesItem";
	
	//Customized columns.
	private String fh_concilia;
	private String feed;
	private String lado;
	private String price;
	private String pair;
	private String quoteset;
	
	public String getQuoteset() {
		return quoteset;
	}

	public void setQuoteset(String quoteset) {
		this.quoteset = quoteset;
	}

	public Opt_FXClosingPricesItem(){}

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
	 * Retrieve the pair.
	 * @param  
	 * @return String with the pair.
	 */
	public String getPair() {
		return pair;
	}
	
	/**
	 * Set the pair.
	 * @param  pair String with the pair.
	 * @return 
	 */
	public void setPair(String pair) {
		this.pair = pair;
	}
}


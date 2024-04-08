package calypsox.tk.report;

/**
 * Class with the item definition for the InfRates report.
 * @author David Porras Mart?nez
 */
public class Opt_InfRatesItem {
	
	/**
     * Identify the object InfRatesItem.
     */
	public static final String OPT_INFRATES_ITEM = "Opt_InfRatesItem";
	
	//Customized columns.
	private String fh_concilia;
	private String feed;
	private String lado;
	private String price;
	private String index;
	private String quotesetName;
	
	public String getQuoteSetName() {
		return quotesetName;
	}

	public void setQuoteSetName(String quoteName) {
		this.quotesetName = quoteName;
	}

	public Opt_InfRatesItem(){}

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
	 * Retrieve the index.
	 * @param  
	 * @return String with the index.
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * Set the index.
	 * @param  index String with the index.
	 * @return 
	 */
	public void setIndex(String index) {
		this.index = index;
	}
}

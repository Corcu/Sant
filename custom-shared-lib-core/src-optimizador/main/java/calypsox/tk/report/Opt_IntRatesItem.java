package calypsox.tk.report;

/**
 * Class with the item definition for the IntRates report.
 * 
 * @author David Porras Mart?nez
 */
public class Opt_IntRatesItem {

	/**
	 * Identify the object IntRatesItem.
	 */
	public static final String EXPTLM_INTRATES_ITEM = "Opt_IntRatesItem";

	// Customized columns.
	private String fh_concilia;
	private String feed;
	private String lado;
	private String price;
	private String index;
	private String quoteSet;

	public Opt_IntRatesItem() {
	}

	/**
	 * Retrieve the date.
	 * 
	 * @param
	 * @return String with the date.
	 */
	public String getFecha() {
		return this.fh_concilia;
	}

	/**
	 * Set the date.
	 * 
	 * @param fh_concilia
	 *            String with the date.
	 * @return
	 */
	public void setFecha(String fh_concilia) {
		this.fh_concilia = fh_concilia;
	}

	/**
	 * Retrieve the feed.
	 * 
	 * @param
	 * @return String with the feed.
	 */
	public String getFeed() {
		return this.feed;
	}

	/**
	 * Set the feed.
	 * 
	 * @param feed
	 *            String with the feed.
	 * @return
	 */
	public void setFeed(String feed) {
		this.feed = feed;
	}

	/**
	 * Retrieve the lado.
	 * 
	 * @param
	 * @return String with the lado.
	 */
	public String getLado() {
		return this.lado;
	}

	/**
	 * Set the lado.
	 * 
	 * @param lado
	 *            String with the lado.
	 * @return
	 */
	public void setLado(String lado) {
		this.lado = lado;
	}

	/**
	 * Retrieve the price.
	 * 
	 * @param
	 * @return String with the price.
	 */
	public String getPrice() {
		return this.price;
	}

	/**
	 * Set the price.
	 * 
	 * @param
	 * @return String with the price.
	 */
	public void setPrice(String price) {
		this.price = price;
	}

	/**
	 * Retrieve the index.
	 * 
	 * @param
	 * @return String with the index.
	 */
	public String getIndex() {
		return this.index;
	}

	/**
	 * Set the index.
	 * 
	 * @param index
	 *            String with the index.
	 * @return
	 */
	public void setIndex(String index) {
		this.index = index;
	}

	public void setQuoteSet(String quoteSetName) {
		this.quoteSet = quoteSetName;
	}

	/**
	 * @return the quoteSet
	 */
	public String getQuoteSet() {
		return this.quoteSet;
	}

}

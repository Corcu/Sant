package calypsox.tk.report;

/**
 * Class with the item definition for the BondPrices report.
 * 
 * @author David Porras Mart?nez
 */
public class ExportBondDirtyCleanPricesItem {

    /**
     * Identify the object ExpTLM_BondPricesItem.
     */
    public static final String EXP_BONDDCPRICES_ITEM = "ExportBondDirtyCleanPricesItem";

    // Customized columns.
    private String fh_concilia;
    private String feed;
    private String lado;
    private String clean_price;
    private String dirty_price;
    private String indicador;
    private String isin;

    public ExportBondDirtyCleanPricesItem() {
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
    public void setFecha(final String fh_concilia) {
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
    public void setFeed(final String feed) {
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
    public void setLado(final String lado) {
	this.lado = lado;
    }

    public String getFh_concilia() {
	return this.fh_concilia;
    }

    public void setFh_concilia(final String fh_concilia) {
	this.fh_concilia = fh_concilia;
    }

    public String getClean_price() {
	return this.clean_price;
    }

    public void setClean_price(final String clean_price) {
	this.clean_price = clean_price;
    }

    public String getDirty_price() {
	return this.dirty_price;
    }

    public void setDirty_price(final String dirty_price) {
	this.dirty_price = dirty_price;
    }

    public String getIndicador() {
	return this.indicador;
    }

    public void setIndicador(final String indicador) {
	this.indicador = indicador;
    }

    /**
     * Retrieve the ISIN.
     * 
     * @param
     * @return String with the ISIN.
     */
    public String getIsin() {
	return this.isin;
    }

    /**
     * Set the ISIN.
     * 
     * @param isin
     *            String with the ISIN.
     * @return
     */
    public void setIsin(final String isin) {
	this.isin = isin;
    }
}

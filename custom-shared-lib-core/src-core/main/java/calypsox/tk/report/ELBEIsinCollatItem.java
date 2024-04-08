package calypsox.tk.report;

public class ELBEIsinCollatItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;
	/**
	 * Identify the object ReposTradeItem.
	 */
	public static final String ELBE_ISIN_COLLAT_ITEM = "ELBEIsinCollatItem";

	// Customized columns.
	private String codLayout;
	private String extractDate;
	private String posTransDate;
	private String sourceApp;

	// COL_OUT_013
	private String frontId;
	private String claveColat;
	private String isinTitulo;
	private String senal;
	private String balanTitulosDivisa;
	private String grossExpoDivisa;
	private String monedaBase;
	private String divisa;
	// GSM: 04/07/14. Added Asset type for MMOO
	private String assetType;
	
	// Added May 2016
	private String marginType;

	private String legalEntityShortName;

	public ELBEIsinCollatItem() {
	}

	public String getCodLayout() {
		return this.codLayout;
	}

	public void setCodLayout(final String codLayout) {
		this.codLayout = codLayout;
	}

	public String getExtractDate() {
		return this.extractDate;
	}

	public void setExtractDate(final String extractDate) {
		this.extractDate = extractDate;
	}

	public String getPosTransDate() {
		return this.posTransDate;
	}

	public void setPosTransDate(final String posTransDate) {
		this.posTransDate = posTransDate;
	}

	public String getSourceApp() {
		return this.sourceApp;
	}

	public void setSourceApp(final String sourceApp) {
		this.sourceApp = sourceApp;
	}

	// COL_OUT_013
	public String getFrontId() {
		return this.frontId;
	}

	// COL_OUT_013
	public void setFrontId(String frontId) {
		this.frontId = frontId;
	}

	public String getClaveColat() {
		return this.claveColat;
	}

	public void setClaveColat(final String claveColat) {
		this.claveColat = claveColat;
	}

	public String getIsinTitulo() {
		return this.isinTitulo;
	}

	public void setIsinTitulo(final String isinTitulo) {
		this.isinTitulo = isinTitulo;
	}

	public String getSenal() {
		return this.senal;
	}

	public void setSenal(final String senal) {
		this.senal = senal;
	}

	public String getBalanTitulosDivisa() {
		return this.balanTitulosDivisa;
	}

	public void setBalanTitulosDivisa(final String balanTitulosDivisa) {
		this.balanTitulosDivisa = balanTitulosDivisa;
	}

	public String getMonedaBase() {
		return this.monedaBase;
	}

	public void setMonedaBase(final String monedaBase) {
		this.monedaBase = monedaBase;
	}

	public String getDivisa() {
		return this.divisa;
	}

	public void setDivisa(final String divisa) {
		this.divisa = divisa;
	}

	public String getGrossExpoDivisa() {
		return this.grossExpoDivisa;
	}

	public void setGrossExpoDivisa(final String divisa) {
		this.grossExpoDivisa = divisa;
	}

	/**
	 * Set Asset type B (BOND) or E (EQUITY)
	 * 
	 * @param assetTypeIN
	 */
	// GSM: 04/07/14. Added Asset type for MMOO
	public void setAssetType(String assetTypeIN) {
		this.assetType = assetTypeIN;
	}

	/**
	 * @return the assetType
	 */
	public String getAssetType() {
		return this.assetType;
	}

	/**
	 * @return the marginType
	 */
	public String getMarginType() {
		return marginType;
	}

	/**
	 * Set Margin type IM (if contract type is CSD) or VM (if contract type is CSD, OSLA or ISMA)
	 * @return
	 */
	public void setMarginType(String marginType) {
		this.marginType = marginType;
	}
	
	/**
	 * @return CounterParty (legalEntity) short name
	 */
	public String getLegalEntityShortName() {
		return this.legalEntityShortName;
	}

	/**
	 * Set CounterParty (legalEntity) short name
	 */
	public void setLegalEntityShortName(String legalEntityShortName) {
		this.legalEntityShortName = legalEntityShortName;
	}
	

}

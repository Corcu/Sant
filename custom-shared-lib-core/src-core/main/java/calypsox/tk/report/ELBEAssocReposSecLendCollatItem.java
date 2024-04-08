package calypsox.tk.report;

public class ELBEAssocReposSecLendCollatItem {
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
    public static final String ELBE_ASSOC_REPOS_SECLEN_COLLAT_ITEM = "ELBEAssocReposSecLendCollatItem";

    // Customized columns.
    private String frontID;
    private String collatID;
    private String codLayout;
    private String extractDate;
    private String posTransDate;
    private String sourceApp;
    private String frontOfficeReference;

    public ELBEAssocReposSecLendCollatItem() {
    }

    public String getFrontID() {
	return this.frontID;
    }

    public void setFrontID(final String frontID) {
	this.frontID = frontID;
    }

    public String getCollatID() {
	return this.collatID;
    }

    public void setCollatID(final String collatID) {
	this.collatID = collatID;
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

    public String getFrontOfficeReference() {
        return frontOfficeReference;
    }

    public void setFrontOfficeReference(String frontOfficeReference) {
        this.frontOfficeReference = frontOfficeReference;
    }
}

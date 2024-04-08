package calypsox.tk.report;

public class ELBEAssocDerivCollatItem {
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
    public static final String ELBE_ASSOC_DERIV_COLLAT_ITEM = "ELBEAssocDerivCollatItem";

    // Customized columns.
    private String frontID;
    private String collatID;
    private String codLayout;
    private String extractDate;
    private String posTransDate;
    private String sourceApp;
    private String boReferenceCustom;
    
    // MISAssocDerivCollat
    private String frontIDMIS;

    public ELBEAssocDerivCollatItem() {
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

    // MISAssocDerivCollat
    public String getFrontIDMIS() {
	return this.frontIDMIS;
    }

    public void setFrontIDMIS(final String frontIDMIS) {
	this.frontIDMIS = frontIDMIS;
    }

    public String getBoReferenceCustom() {
        return this.boReferenceCustom;
    }

    public void setBoReferenceCustom(String boReferenceCustom) {
        this.boReferenceCustom = boReferenceCustom;
    }
}

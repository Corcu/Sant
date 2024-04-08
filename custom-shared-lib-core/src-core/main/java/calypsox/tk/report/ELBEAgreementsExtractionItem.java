package calypsox.tk.report;

import com.calypso.tk.refdata.CollateralConfig;

public class ELBEAgreementsExtractionItem {
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
	public static final String ELBE_AGREE_EXT_ITEM = "ELBEAgreementsExtractionItem";

	// Customized columns.
	private String codLayout;
	private String extractDate;
	private String posTransDate;
	private String sourceApp;

	private String valAgent;
	private String owner;
	private String colAgreement;
	private String shortname;
	private String baseCCY;
	private String fixing;
	private String contractType;
	private String activeValues;
	private String activeRating;
	private String thresholdActiveCCY;
	private String thresholdActiveEUR;
	private String balanceCCY;
	private String balanceEUR;
	private String IAActiveCCY;
	private String IAActiveEUR;
	private String ratDown1notch;
	private String thresholdActiveDown1notchCCY;
	private String thresholdActiveDown1notchEUR;
	private String impactDown1notchCCY;
	private String impactDown1notchEUR;
	private String IADown1notchCCY;
	private String IADown1notchEUR;
	private String ratDown2notch;
	private String thresholdActiveDown2notchCCY;
	private String thresholdActiveDown2notchEUR;
	private String impactDown2notchCCY;
	private String impactDown2notchEUR;
	private String IADown2notchCCY;
	private String IADown2notchEUR;
	private String ratDown3notch;
	private String thresholdActiveDown3notchCCY;
	private String thresholdActiveDown3notchEUR;
	private String impactDown3notchCCY;
	private String impactDown3notchEUR;
	private String IADown3notchCCY;
	private String IADown3notchEUR;
	private String haircut;
	private String DSecurity;
	private String sign;
	private String balanceCashCCY;
	private String balanceCashEUR;
	private String balanceStockCCY;
	private String balanceStockEUR;
	private String status;
	private String event;
	private String grossExposureCCY;
	private String grossExposureEUR;
	private String marginCallCCY;
	private String iaActiveValues;

	// SBWO
	private CollateralConfig marginCallConfig;
	private double grossExposure;
	private double thresholdDown1notch;
	private double thresholdDown2notch;
	private double thresholdDown3notch;
	private double mtaDown1notch;
	private double mtaDown2notch;
	private double mtaDown3notch;

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

	public String getValAgent() {
		return this.valAgent;
	}

	public void setValAgent(final String valAgent) {
		this.valAgent = valAgent;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(final String owner) {
		this.owner = owner;
	}

	public String getColAgreement() {
		return this.colAgreement;
	}

	public void setColAgreement(final String colAgreement) {
		this.colAgreement = colAgreement;
	}

	public String getShortname() {
		return this.shortname;
	}

	public void setShortname(final String shortname) {
		this.shortname = shortname;
	}

	public String getBaseCCY() {
		return this.baseCCY;
	}

	public void setBaseCCY(final String baseCCY) {
		this.baseCCY = baseCCY;
	}

	public String getFixing() {
		return this.fixing;
	}

	public void setFixing(final String fixing) {
		this.fixing = fixing;
	}

	public String getContractType() {
		return this.contractType;
	}

	public void setContractType(final String contractType) {
		this.contractType = contractType;
	}

	public String getActiveValues() {
		return this.activeValues;
	}

	public void setActiveValues(final String activeValues) {
		this.activeValues = activeValues;
	}

	public String getActiveRating() {
		return this.activeRating;
	}

	public void setActiveRating(final String activeRating) {
		this.activeRating = activeRating;
	}

	public String getThresholdActiveCCY() {
		return this.thresholdActiveCCY;
	}

	public void setThresholdActiveCCY(final String thresholdActiveCCY) {
		this.thresholdActiveCCY = thresholdActiveCCY;
	}

	public String getThresholdActiveEUR() {
		return this.thresholdActiveEUR;
	}

	public void setThresholdActiveEUR(final String thresholdActiveEUR) {
		this.thresholdActiveEUR = thresholdActiveEUR;
	}

	public String getBalanceCCY() {
		return this.balanceCCY;
	}

	public void setBalanceCCY(final String balanceCCY) {
		this.balanceCCY = balanceCCY;
	}

	public String getBalanceEUR() {
		return this.balanceEUR;
	}

	public void setBalanceEUR(final String balanceEUR) {
		this.balanceEUR = balanceEUR;
	}

	public String getIAActiveCCY() {
		return this.IAActiveCCY;
	}

	public void setIAActiveCCY(final String iAActiveCCY) {
		this.IAActiveCCY = iAActiveCCY;
	}

	public String getIAActiveEUR() {
		return this.IAActiveEUR;
	}

	public void setIAActiveEUR(final String iAActiveEUR) {
		this.IAActiveEUR = iAActiveEUR;
	}

	public String getRatDown1notch() {
		return this.ratDown1notch;
	}

	public void setRatDown1notch(final String ratDown1notch) {
		this.ratDown1notch = ratDown1notch;
	}

	public String getThresholdActiveDown1notchCCY() {
		return this.thresholdActiveDown1notchCCY;
	}

	public void setThresholdActiveDown1notchCCY(final String thresholdActiveDown1notchCCY) {
		this.thresholdActiveDown1notchCCY = thresholdActiveDown1notchCCY;
	}

	public String getThresholdActiveDown1notchEUR() {
		return this.thresholdActiveDown1notchEUR;
	}

	public void setThresholdActiveDown1notchEUR(final String thresholdActiveDown1notchEUR) {
		this.thresholdActiveDown1notchEUR = thresholdActiveDown1notchEUR;
	}

	public String getImpactDown1notchCCY() {
		return this.impactDown1notchCCY;
	}

	public void setImpactDown1notchCCY(final String impactDown1notchCCY) {
		this.impactDown1notchCCY = impactDown1notchCCY;
	}

	public String getImpactDown1notchEUR() {
		return this.impactDown1notchEUR;
	}

	public void setImpactDown1notchEUR(final String impactDown1notchEUR) {
		this.impactDown1notchEUR = impactDown1notchEUR;
	}

	public String getIADown1notchCCY() {
		return this.IADown1notchCCY;
	}

	public void setIADown1notchCCY(final String iADown1notchCCY) {
		this.IADown1notchCCY = iADown1notchCCY;
	}

	public String getIADown1notchEUR() {
		return this.IADown1notchEUR;
	}

	public void setIADown1notchEUR(final String iADown1notchEUR) {
		this.IADown1notchEUR = iADown1notchEUR;
	}

	public String getRatDown2notch() {
		return this.ratDown2notch;
	}

	public void setRatDown2notch(final String ratDown2notch) {
		this.ratDown2notch = ratDown2notch;
	}

	public String getThresholdActiveDown2notchCCY() {
		return this.thresholdActiveDown2notchCCY;
	}

	public void setThresholdActiveDown2notchCCY(final String thresholdActiveDown2notchCCY) {
		this.thresholdActiveDown2notchCCY = thresholdActiveDown2notchCCY;
	}

	public String getThresholdActiveDown2notchEUR() {
		return this.thresholdActiveDown2notchEUR;
	}

	public void setThresholdActiveDown2notchEUR(final String thresholdActiveDown2notchEUR) {
		this.thresholdActiveDown2notchEUR = thresholdActiveDown2notchEUR;
	}

	public String getImpactDown2notchCCY() {
		return this.impactDown2notchCCY;
	}

	public void setImpactDown2notchCCY(final String impactDown2notchCCY) {
		this.impactDown2notchCCY = impactDown2notchCCY;
	}

	public String getImpactDown2notchEUR() {
		return this.impactDown2notchEUR;
	}

	public void setImpactDown2notchEUR(final String impactDown2notchEUR) {
		this.impactDown2notchEUR = impactDown2notchEUR;
	}

	public String getIADown2notchCCY() {
		return this.IADown2notchCCY;
	}

	public void setIADown2notchCCY(final String iADown2notchCCY) {
		this.IADown2notchCCY = iADown2notchCCY;
	}

	public String getIADown2notchEUR() {
		return this.IADown2notchEUR;
	}

	public void setIADown2notchEUR(final String iADown2notchEUR) {
		this.IADown2notchEUR = iADown2notchEUR;
	}

	public String getRatDown3notch() {
		return this.ratDown3notch;
	}

	public void setRatDown3notch(final String ratDown3notch) {
		this.ratDown3notch = ratDown3notch;
	}

	public String getThresholdActiveDown3notchCCY() {
		return this.thresholdActiveDown3notchCCY;
	}

	public void setThresholdActiveDown3notchCCY(final String thresholdActiveDown3notchCCY) {
		this.thresholdActiveDown3notchCCY = thresholdActiveDown3notchCCY;
	}

	public String getThresholdActiveDown3notchEUR() {
		return this.thresholdActiveDown3notchEUR;
	}

	public void setThresholdActiveDown3notchEUR(final String thresholdActiveDown3notchEUR) {
		this.thresholdActiveDown3notchEUR = thresholdActiveDown3notchEUR;
	}

	public String getImpactDown3notchCCY() {
		return this.impactDown3notchCCY;
	}

	public void setImpactDown3notchCCY(final String impactDown3notchCCY) {
		this.impactDown3notchCCY = impactDown3notchCCY;
	}

	public String getImpactDown3notchEUR() {
		return this.impactDown3notchEUR;
	}

	public void setImpactDown3notchEUR(final String impactDown3notchEUR) {
		this.impactDown3notchEUR = impactDown3notchEUR;
	}

	public String getIADown3notchCCY() {
		return this.IADown3notchCCY;
	}

	public void setIADown3notchCCY(final String iADown3notchCCY) {
		this.IADown3notchCCY = iADown3notchCCY;
	}

	public String getIADown3notchEUR() {
		return this.IADown3notchEUR;
	}

	public void setIADown3notchEUR(final String iADown3notchEUR) {
		this.IADown3notchEUR = iADown3notchEUR;
	}

	public String getHaircut() {
		return this.haircut;
	}

	public void setHaircut(final String haircut) {
		this.haircut = haircut;
	}

	public String getDSecurity() {
		return this.DSecurity;
	}

	public void setDSecurity(final String dSecurity) {
		this.DSecurity = dSecurity;
	}

	public String getSign() {
		return this.sign;
	}

	public void setSign(final String sign) {
		this.sign = sign;
	}

	public String getBalanceCashCCY() {
		return this.balanceCashCCY;
	}

	public void setBalanceCashCCY(final String balanceCashCCY) {
		this.balanceCashCCY = balanceCashCCY;
	}

	public String getBalanceCashEUR() {
		return this.balanceCashEUR;
	}

	public void setBalanceCashEUR(final String balanceCashEUR) {
		this.balanceCashEUR = balanceCashEUR;
	}

	public String getBalanceStockCCY() {
		return this.balanceStockCCY;
	}

	public void setBalanceStockCCY(final String balanceStockCCY) {
		this.balanceStockCCY = balanceStockCCY;
	}

	public String getBalanceStockEUR() {
		return this.balanceStockEUR;
	}

	public void setBalanceStockEUR(final String balanceStockEUR) {
		this.balanceStockEUR = balanceStockEUR;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public String getEvent() {
		return this.event;
	}

	public void setEvent(final String event) {
		this.event = event;
	}

	public String getGrossExposureCCY() {
		return this.grossExposureCCY;
	}

	public void setGrossExposureCCY(final String grossExposureCCY) {
		this.grossExposureCCY = grossExposureCCY;
	}

	public String getGrossExposureEUR() {
		return this.grossExposureEUR;
	}

	public void setGrossExposureEUR(final String grossExposureEUR) {
		this.grossExposureEUR = grossExposureEUR;
	}

	public String getMarginCallCCY() {
		return this.marginCallCCY;
	}

	public void setMarginCallCCY(final String marginCallCCY) {
		this.marginCallCCY = marginCallCCY;
	}

	public String getIaActiveValues() {
		return this.iaActiveValues;
	}

	public void setIaActiveValues(final String iaActiveValues) {
		this.iaActiveValues = iaActiveValues;
	}

	public void setMarginCallConfig(CollateralConfig collateralConfig) {
		this.marginCallConfig = collateralConfig;
	}

	public CollateralConfig getMarginCallConfig() {
		return this.marginCallConfig;
	}

	public void setGrossExposure(double grossExp) {
		this.grossExposure = grossExp;
	}

	public double getGrossExposure() {
		return this.grossExposure;
	}

	public double getThresholdDown1notch() {
		return this.thresholdDown1notch;
	}

	public void setThresholdDown1notch(double thresholdDown1notch) {
		this.thresholdDown1notch = thresholdDown1notch;
	}

	public double getThresholdDown2notch() {
		return this.thresholdDown2notch;
	}

	public void setThresholdDown2notch(double thresholdDown2notch) {
		this.thresholdDown2notch = thresholdDown2notch;
	}

	public double getThresholdDown3notch() {
		return this.thresholdDown3notch;
	}

	public void setThresholdDown3notch(double thresholdDown3notch) {
		this.thresholdDown3notch = thresholdDown3notch;
	}

	public double getMTADown1notch() {
		return this.mtaDown1notch;
	}

	public void setMTADown1notch(double mtaDown1notch) {
		this.mtaDown1notch = mtaDown1notch;
	}

	public double getMTADown2notch() {
		return this.mtaDown2notch;
	}

	public void setMTADown2notch(double mtaDown2notch) {
		this.mtaDown2notch = mtaDown2notch;
	}

	public double getMTADown3notch() {
		return this.mtaDown3notch;
	}

	public void setMTADown3notch(double mtaDown3notch) {
		this.mtaDown3notch = mtaDown3notch;
	}
}

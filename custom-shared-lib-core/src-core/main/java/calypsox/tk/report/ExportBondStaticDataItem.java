package calypsox.tk.report;

import com.calypso.tk.core.JDate;

/**
 * Class with the item definition for the Bonds Static Data report.
 * 
 * @author David Porras Mart?nez
 */
public class ExportBondStaticDataItem {

	/**
	 * Identify the object ExpTLM_BondPricesItem.
	 */
	public static final String EXPORT_BONDSTATICDATA_ITEM = "ExportBondStaticDataItem";

	// Customized columns.
	private String fecha;
	private String isin;
	private String bondType;
	private String bondSubType;
	private String couponCurrency;
	private String currency;
	private JDate datedDate;
	private String daycount;
	private int fixedB;
	private String couponFrequency;
	private String holidays;
	private String rateIndex;
	private JDate issueDate;
	private String issuer;
	private JDate maturityDate;
	private double coupon;
	private double spread;
	private double faceValue;
	private JDate firstCouponDate;
	private String notionalIndex;
	private String externalRef;

	public ExportBondStaticDataItem() {
	}

	// Getters and setters
	public String getFecha() {
		return this.fecha;
	}

	public void setFecha(final String fecha) {
		this.fecha = fecha;
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(final String isin) {
		this.isin = isin;
	}

	public String getBondType() {
		return this.bondType;
	}

	public void setBondType(final String bondType) {
		this.bondType = bondType;
	}

	public String getBondSubType() {
		return this.bondSubType;
	}

	public void setBondSubType(final String bondSubType) {
		this.bondSubType = bondSubType;
	}

	public String getCouponCurrency() {
		return this.couponCurrency;
	}

	public void setCouponCurrency(final String couponCurrency) {
		this.couponCurrency = couponCurrency;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(final String currency) {
		this.currency = currency;
	}

	public JDate getDatedDate() {
		return this.datedDate;
	}

	public void setDatedDate(final JDate datedDate) {
		this.datedDate = datedDate;
	}

	public String getDaycount() {
		return this.daycount;
	}

	public void setDaycount(final String daycount) {
		this.daycount = daycount;
	}

	public int getFixedB() {
		return this.fixedB;
	}

	public void setFixedB(final int fixedB) {
		this.fixedB = fixedB;
	}

	public String getCouponFrequency() {
		return this.couponFrequency;
	}

	public void setCouponFrequency(final String couponFrequency) {
		this.couponFrequency = couponFrequency;
	}

	public String getHolidays() {
		return this.holidays;
	}

	public void setHolidays(final String holidays) {
		this.holidays = holidays;
	}

	public String getRateIndex() {
		return this.rateIndex;
	}

	public void setRateIndex(final String rateIndex) {
		this.rateIndex = rateIndex;
	}

	public JDate getIssueDate() {
		return this.issueDate;
	}

	public void setIssueDate(final JDate issueDate) {
		this.issueDate = issueDate;
	}

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(final String issuer) {
		this.issuer = issuer;
	}

	public JDate getMaturityDate() {
		return this.maturityDate;
	}

	public void setMaturityDate(final JDate maturityDate) {
		this.maturityDate = maturityDate;
	}

	public double getCoupon() {
		return this.coupon;
	}

	public void setCoupon(final double coupon) {
		this.coupon = coupon;
	}

	public double getSpread() {
		return this.spread;
	}

	public void setSpread(final double spread) {
		this.spread = spread;
	}

	public double getFaceValue() {
		return this.faceValue;
	}

	public void setFaceValue(final double faceValue) {
		this.faceValue = faceValue;
	}

	public JDate getFirstCouponDate() {
		return this.firstCouponDate;
	}

	public void setFirstCouponDate(final JDate firstCouponDate) {
		this.firstCouponDate = firstCouponDate;
	}

	public String getNotionalIndex() {
		return this.notionalIndex;
	}

	public void setNotionalIndex(final String notionalIndex) {
		this.notionalIndex = notionalIndex;
	}

	public String getExternalRef() {
		return this.externalRef;
	}

	public void setExternalRef(final String externalRef) {
		this.externalRef = externalRef;
	}

}

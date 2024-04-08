package calypsox.tk.report;

/**
 * Class with the item definition for the Factor Schedule Conciliation report.
 * 
 * @author Juan Angel Torija D?az
 */
public class ExportCouponScheduleConciliationItem {

	/**
	 * Identify the object ExportCouponScheduleConciliationItem.
	 */
	public static final String EXPORT_COUPONSCHEDULECONCILIATION_ITEM = "ExportCouponScheduleConciliationItem";

	// Customized columns.

	private String ISIN;
	private String periodStartDate;
	private String periodEndDate;
	private String Coupon;
	private String Frq;

	// Getters & Setters

	public String getISIN() {
		return this.ISIN;
	}

	public void setISIN(String iSIN) {
		this.ISIN = iSIN;
	}

	public String getPeriodStartDate() {
		return this.periodStartDate;
	}

	public void setPeriodStartDate(String periodStartDate) {
		this.periodStartDate = periodStartDate;
	}

	public String getPeriodEndDate() {
		return this.periodEndDate;
	}

	public void setPeriodEndDate(String periodEndDate) {
		this.periodEndDate = periodEndDate;
	}

	public String getCoupon() {
		return this.Coupon;
	}

	public void setCoupon(String coupon) {
		this.Coupon = coupon;
	}

	public String getFrq() {
		return this.Frq;
	}

	public void setFrq(String frq) {
		this.Frq = frq;
	}

}

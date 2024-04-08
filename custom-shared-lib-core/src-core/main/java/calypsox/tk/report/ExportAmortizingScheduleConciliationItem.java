package calypsox.tk.report;

/**
 * Class with the item definition for the Amortizing Schedule Conciliation report.
 * 
 * @author Juan Angel Torija D?az
 */
public class ExportAmortizingScheduleConciliationItem {

	/**
	 * Identify the object ExportFactorScheduleConciliationItem.
	 */
	public static final String EXPORT_AMORTIZINGSCHEDULECONCILIATION_ITEM = "ExportAmortizingScheduleConciliationItem";

	// Customized columns.

	private String ISIN;
	private String date;
	private String notional;
	private String couponDateRule;
	private String frq;

	// Getters & Setters

	public ExportAmortizingScheduleConciliationItem() {
	}

	public String getISIN() {
		return this.ISIN;
	}

	public void setISIN(String iSIN) {
		this.ISIN = iSIN;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getNotional() {
		return this.notional;
	}

	public String getCouponDateRule() {
	    return this.couponDateRule;
	}

	public void setCouponDateRule(String couponDateRule) {
		this.couponDateRule = couponDateRule;
	}
	
	public void setNotional(String notional) {
		this.notional = notional;
	}

	public String getFrq() {
		return this.frq;
	}

	public void setFrq(String frq) {
		this.frq = frq;
	}

}

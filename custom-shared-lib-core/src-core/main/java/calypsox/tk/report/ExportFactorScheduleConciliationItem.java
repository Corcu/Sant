package calypsox.tk.report;

/**
 * Class with the item definition for the Factor Schedule Conciliation report.
 * 
 * @author Juan Angel Torija D?az
 */
public class ExportFactorScheduleConciliationItem {

	/**
	 * Identify the object ExportFactorScheduleConciliationItem.
	 */
	public static final String EXPORT_FACTORSCHEDULECONCILIATION_ITEM = "ExportFactorScheduleConciliationItem";

	// Customized columns.

	private String ISIN;
	private String effectiveDate;
	private String poolFactor;

	// Getters & Setters

	public ExportFactorScheduleConciliationItem() {
	}

	public String getISIN() {
		return this.ISIN;
	}

	public void setISIN(String iSIN) {
		this.ISIN = iSIN;
	}

	public String getEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public String getPoolFactor() {
		return this.poolFactor;
	}

	public void setPoolFactor(String poolFactor) {
		this.poolFactor = poolFactor;
	}

}

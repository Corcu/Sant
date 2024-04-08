package calypsox.tk.report;

public class ExportLegalEntityItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	public static final String EXPORT_LE_ITEM = "ExportLegalEntityItem";

	private String counterpartyDescription;
	private String generationDate;
	private String processingOrg;
	private String counterparty;
	// BAU 5.5 - New column - export cpty susi repo
	private String overnight;
	// BAU 6.1 - GSM New column - effective date trade or value
	private String effectiveDate;

	public ExportLegalEntityItem() {
	}

	public String getCounterpartyDescription() {
		return this.counterpartyDescription;
	}

	public void setCounterpartyDescription(String counterpartyDescription) {
		this.counterpartyDescription = counterpartyDescription;
	}

	public String getGenerationDate() {
		return this.generationDate;
	}

	public void setGenerationDate(String generationDate) {
		this.generationDate = generationDate;
	}

	public String getProcessingOrg() {
		return this.processingOrg;
	}

	public void setProcessingOrg(String processingOrg) {
		this.processingOrg = processingOrg;
	}

	public String getCounterparty() {
		return this.counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	// BAU 5.5 - New column - export cpty susi repo
	public String getOvernight() {
		return this.overnight;
	}

	public void setOvernight(String overnight) {
		this.overnight = overnight;
	}

	// BAU 6.1 - GSM New column - effective date trade or value
	public String getGetEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(String ed) {
		this.effectiveDate = ed;
	}
}

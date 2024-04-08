package calypsox.tk.report;

public class KGR_Export_MtMItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	public static final String KGR_EXPORT_MTM_ITEM = "KGR_Export_MtMItem";

	private String sourceSystem;
	private String transactionId;
	private String owner;
	private String counterparty;
	private String agreementId;
	private double mtm;
	private String mtmCurrency;

	public KGR_Export_MtMItem() {
	}

	/**
	 * @return the mtmMarginCall
	 */
	public double getMtm() {
		return this.mtm;
	}

	/**
	 * @param mtmMarginCall
	 *            the mtmMarginCall to set
	 */
	public void setMtm(double mtm) {
		this.mtm = mtm;
	}

	public String getSourceSystem() {
		return this.sourceSystem;
	}

	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public String getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCounterparty() {
		return this.counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public String getAgreementId() {
		return this.agreementId;
	}

	public void setAgreementId(String agreementId) {
		this.agreementId = agreementId;
	}

	public String getMtmCurrency() {
		return this.mtmCurrency;
	}

	public void setMtmCurrency(String mtmCurrency) {
		this.mtmCurrency = mtmCurrency;
	}
}

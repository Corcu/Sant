package calypsox.tk.util;

public class CalculationTracker {
	public static final int OK = 0;
	public static final int KO = 1;

	protected int contractId;//Sonar
	protected int calculationStatus;//Sonar
	protected String calculationComment;//Sonar

	public CalculationTracker(int contractId, int calculationStatus, String calculationComment) {
		this.contractId = contractId;
		this.calculationStatus = calculationStatus;
		this.calculationComment = calculationComment;
	}

	public String getStatusAsString() {
		return this.calculationStatus == OK ? "OK" : "KO";
	}

	/**
	 * @return the contractId
	 */
	public int getContractId() {
		return this.contractId;
	}

	/**
	 * @param contractId
	 *            the contractId to set
	 */
	public void setContractId(int contractId) {
		this.contractId = contractId;
	}

	/**
	 * @return the calculationStatus
	 */
	public int getCalculationStatus() {
		return this.calculationStatus;
	}

	/**
	 * @param calculationStatus
	 *            the calculationStatus to set
	 */
	public void setCalculationStatus(int calculationStatus) {
		this.calculationStatus = calculationStatus;
	}

	/**
	 * @return the calculationComment
	 */
	public String getCalculationComment() {
		return this.calculationComment;
	}

	/**
	 * @param calculationComment
	 *            the calculationComment to set
	 */
	public void setCalculationComment(String calculationComment) {
		this.calculationComment = calculationComment;
	}
}
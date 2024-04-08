package calypsox.tk.report;

import java.io.Serializable;

public class CumbreAccountingReportBean implements Serializable {

	public static final String CUMBRE_ACCOUNTING_REPORT_BEAN = "CumbreAccountingReportBean";
	
	/** UID */
	private static final long serialVersionUID = 1L;
	
	private String amount;
	private String processDate; //different format
	private String accountNumber;
	private String balanceBranch;
	private String cptyShortName;
	private String balanceReference;
	private String balanceCcy;
	private String balanceAmountWithSign;
	private String movementAmount;
	private String movementBranch;
	private String movementType;
	private String valueDate;
	private String movementValue;
	private String movementRef;
	private String movementProcessDate;
	private boolean isLode;
	
	
	public CumbreAccountingReportBean(){
		// comment to avoid sonar Critical error:  Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.
	}
	
	public String getProcessDate() {
		return processDate;
	}
	public void setProcessDate(String processDate) {
		this.processDate = processDate;
	}
	
	
	public String getBalanceBranch() {
		return balanceBranch;
	}

	public void setBalanceBranch(String balanceBranch) {
		this.balanceBranch = balanceBranch;
	}

	public String getMovementBranch() {
		return movementBranch;
	}

	public void setMovementBranch(String movementBranch) {
		this.movementBranch = movementBranch;
	}

	public String getCptyShortName() {
		return cptyShortName;
	}
	public void setCptyShortName(String cptyShortName) {
		this.cptyShortName = cptyShortName;
	}
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public String getBalanceReference() {
		return balanceReference;
	}
	public void setBalanceReference(String balanceReference) {
		this.balanceReference = balanceReference;
	}
	public String getBalanceCcy() {
		return balanceCcy;
	}
	public void setBalanceCcy(String balanceCcy) {
		this.balanceCcy = balanceCcy;
	}
	public String getBalanceAmount() {
		return balanceAmountWithSign;
	}
	public void setBalanceAmount(String balanceAmount) {
		this.balanceAmountWithSign = balanceAmount;
	}
	
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	
	public String getMovementType(){
		return this.movementType;
	}

	public void setMovementType(String movType){
		this.movementType = movType;
	}
	
	public String getValueDate(){
		return this.valueDate;
	}
	
	public void setValueDate(String valueDate){
		this.valueDate = valueDate;
	}
	
	public String getMovementAmount(){
		return this.movementAmount;
	}
	
	public void setMovementAmount(String amount){
		this.movementAmount = amount;
	}
	
	public String getMovementValue(){
		return this.movementValue;
	}
	
	public void setMovementValue(String amount){
		this.movementValue = amount;
	}

	public String getMovementRef() {
		return movementRef;
	}

	public void setMovementRef(String movementRef) {
		this.movementRef = movementRef;
	}

	public String getMovementProcessDate() {
		return movementProcessDate;
	}

	public void setMovementProcessDate(String movementProcessDate) {
		this.movementProcessDate = movementProcessDate;
	}

	public void setIsLode(boolean isLode) {
		this.isLode = isLode;
	}

	public boolean getIsLode() {
		return this.isLode;
	}
	
}

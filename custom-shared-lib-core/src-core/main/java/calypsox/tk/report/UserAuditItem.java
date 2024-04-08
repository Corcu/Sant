package calypsox.tk.report;

import com.calypso.tk.core.JDatetime;

public class UserAuditItem {
	public UserAuditItem() {

	}

	public static final String USER_AUDIT_ITEM = "UserAuditItem";

	private String USER_NAME;
	private String USER_CODE;
	private String USER_GROUP;
	private JDatetime USER_CREATION_DATE;
	private JDatetime PWD_EXPIRY_DATE;
	private JDatetime LAST_LOGIN_DATE;
	private JDatetime ACC_LOCKED_DATE;
	private JDatetime deletedDate;

	public JDatetime getDeletedDate() {
		return this.deletedDate;
	}

	public void setDeletedDate(JDatetime deletedDate) {
		this.deletedDate = deletedDate;
	}

	public String getUSER_NAME() {
		return this.USER_NAME;
	}

	public void setUSER_NAME(final String uSER_NAME) {
		this.USER_NAME = uSER_NAME;
	}

	public String getUSER_CODE() {
		return this.USER_CODE;
	}

	public void setUSER_CODE(final String uSER_CODE) {
		this.USER_CODE = uSER_CODE;
	}

	public String getUSER_GROUP() {
		return this.USER_GROUP;
	}

	public void setUSER_GROUP(final String uSER_GROUP) {
		this.USER_GROUP = uSER_GROUP;
	}

	public JDatetime getUSER_CREATION_DATE() {
		return this.USER_CREATION_DATE;
	}

	public void setUSER_CREATION_DATE(final JDatetime uSER_CREATION_DATE) {
		this.USER_CREATION_DATE = uSER_CREATION_DATE;
	}

	public JDatetime getPWD_EXPIRY_DATE() {
		return this.PWD_EXPIRY_DATE;
	}

	public void setPWD_EXPIRY_DATE(final JDatetime pWD_EXPIRY_DAYS) {
		this.PWD_EXPIRY_DATE = pWD_EXPIRY_DAYS;
	}

	public JDatetime getLAST_LOGIN_DATE() {
		return this.LAST_LOGIN_DATE;
	}

	public void setLAST_LOGIN_DATE(final JDatetime lAST_LOGIN) {
		this.LAST_LOGIN_DATE = lAST_LOGIN;
	}

	public void setACC_LOCKED_DATE(final JDatetime aCC_LOCKED_DATE) {
		this.ACC_LOCKED_DATE = aCC_LOCKED_DATE;
	}

	public JDatetime getACC_LOCKED_DATE() {
		return this.ACC_LOCKED_DATE;
	}

}

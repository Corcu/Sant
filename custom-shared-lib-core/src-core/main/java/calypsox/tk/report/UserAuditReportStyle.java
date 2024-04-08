package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class UserAuditReportStyle extends ReportStyle {

	private static final long serialVersionUID = 3033398173612307999L;

	public static final String USER_NAME = "USER_NAME";
	public static final String USER_CODE = "USER_CODE";
	public static final String USER_GROUP = "USER_GROUP";
	public static final String USER_CREATION_DATE = "USER_CREATION_DATE";
	public static final String PWD_EXPIRY_DATE = "PWD_EXPIRY_DATE";
	public static final String LAST_LOGIN_DATE = "LAST_LOGIN_DATE";
	public static final String ACC_LOCKED_DATE = "ACC_LOCKED_DATE";
	public static final String USER_DELETED_DATE = "USER_DELETED_DATE";

	public static final String[] DEFAULTS_COLUMNS = { USER_NAME, USER_CODE, USER_GROUP, USER_CREATION_DATE,
			PWD_EXPIRY_DATE, LAST_LOGIN_DATE, ACC_LOCKED_DATE, USER_DELETED_DATE };

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {
		final UserAuditItem userAuditItem = (UserAuditItem) row.getProperty(UserAuditItem.USER_AUDIT_ITEM);

		if (columnName.equals(USER_NAME)) {
			return String.valueOf(userAuditItem.getUSER_NAME());

		} else if (columnName.equals(USER_CODE)) {
			return userAuditItem.getUSER_CODE();

		} else if (columnName.equals(USER_GROUP)) {
			return userAuditItem.getUSER_GROUP();

		} else if (columnName.equals(USER_CREATION_DATE)) {
			return userAuditItem.getUSER_CREATION_DATE();

		} else if (columnName.equals(PWD_EXPIRY_DATE)) {
			return userAuditItem.getPWD_EXPIRY_DATE();

		} else if (columnName.equals(LAST_LOGIN_DATE)) {
			return userAuditItem.getLAST_LOGIN_DATE();

		} else if (columnName.equals(ACC_LOCKED_DATE)) {
			return userAuditItem.getACC_LOCKED_DATE();

		} else if (columnName.equals(USER_DELETED_DATE)) {
			return userAuditItem.getDeletedDate();

		} else {
			throw new InvalidParameterException("The column name:" + columnName + " is not expected");
		}

	}
}

/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Samuel Bartolome (samuel.bartolome@siag-management.com) 
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.Log;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

@SuppressWarnings("serial")
public class UserAuditReport extends Report {
	public static final String USER_AUDIT_REPORT = "UserAuditReport";

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {
		final Vector<String> errorMsgs = errorMsgsP;
		// initDates();
		final DefaultReportOutput output = new DefaultReportOutput(this);

		// CAL_COLLAT_REPORT_0114
		final JDate startDate = getDate(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
		final JDate endDate = getDate(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);

		final String enteredUserName = (String) this._reportTemplate.get(UserAuditReportTemplate.USER_NAME);
		final String selectedGroup = (String) this._reportTemplate.get(UserAuditReportTemplate.USER_GROUP);

		// Get All user Info and create UserAuditItem objects
		final ArrayList<UserAuditItem> userAuditItems = getUserAuditItems(errorMsgs, enteredUserName, selectedGroup,
				startDate, endDate);

		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		for (final UserAuditItem item : userAuditItems) {
			reportRows.add(new ReportRow(item));
		}

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

		return output;
	}

	/**
	 * This method retrieves all user info from Data Base
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<UserAuditItem> getUserAuditItems(@SuppressWarnings("rawtypes") final Vector errorMsgs,
			final String enteredUserName, final String selectedGroup, final JDate startDate, final JDate endDate) {

		final ArrayList<UserAuditItem> userAuditItems = new ArrayList<UserAuditItem>();

		final HashMap<String, JDatetime> creationDateMap = getUserCreationDateInfo(endDate, errorMsgs);
		// CAL_COLLAT_REPORT_0114
		final HashMap<String, JDatetime> deletionDateMap = getUserDeletionDateInfo(enteredUserName, startDate,
				errorMsgs);

		String SQL = "select usr.FULL_NAME, usr.user_name, usr.last_passwd_chg, usr.threshold_days, "
				+ "usr.acc_locked_date, lastlogin.SUCC_LOGIN_DATE, usr.use_threshold_days, grp.group_name ";
		final String sqlFrom = " from user_name usr, user_last_login lastlogin, group_access grp";
		String sqlWhere = " where usr.user_name=lastlogin.user_name(+) "
				+ "and usr.user_name=grp.access_value(+) and (grp.access_id is null or grp.access_id=0)";
		final String orderBy = "order by usr.user_name";

		if (!Util.isEmpty(enteredUserName)) {
			sqlWhere = sqlWhere + " AND usr.user_name like '" + enteredUserName + "%' ";
		}

		if (!Util.isEmpty(selectedGroup)) {
			sqlWhere = sqlWhere + " AND grp.group_name='" + selectedGroup + "' ";
		}

		SQL = SQL + sqlFrom + sqlWhere + orderBy;

		@SuppressWarnings("rawtypes")
		Vector rsVect = null;
		try {
			rsVect = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(SQL);
		} catch (final RemoteException re) {
			errorMsgs.add("An Exception ocurred when retreiving User Info:\n" + re);
			return userAuditItems;
		}

		if ((rsVect != null) && (rsVect.size() >= 3)) {
			for (int i = 2; i < rsVect.size(); i++) {
				@SuppressWarnings("rawtypes")
				final Vector userVect = (Vector) rsVect.get(i);

				final UserAuditItem user = new UserAuditItem();
				user.setUSER_NAME((String) userVect.get(0));
				user.setUSER_CODE((String) userVect.get(1));

				// CAL_COLLAT_REPORT_0114
				// Only if the creation are meet the start and end dates criteria, the user item is
				// added to the final report.
				final JDatetime creationDate = creationDateMap.get(user.getUSER_CODE());
				// final JDatetime deletionDate = deletionDateMap.get(user.getUSER_CODE());
				if ((creationDate == null) || (endDate == null) || creationDate.getJDate(TimeZone.getDefault()).lte(endDate)) {

					// calculate password expiry date
					if (Integer.parseInt((String) userVect.get(6)) == 1) {
						JDatetime lastPwdChg = null;
						int pwd_threshold = 0;
						if (userVect.get(2) != null) {
							lastPwdChg = (JDatetime) userVect.get(2);
						}
						try {
							pwd_threshold = Integer.parseInt((String) userVect.get(3));
						} catch (final Exception exc) {	
							Log.error(this,exc);//Sonar
						}
						JDatetime pwdExpiry = null;
						if (lastPwdChg != null) {
							pwdExpiry = lastPwdChg.add(pwd_threshold, 0, 0, 0, 0);
						}

						user.setPWD_EXPIRY_DATE(pwdExpiry);

					}

					// AccLocked Date
					JDatetime userAccLockDate = null;
					if (userVect.get(4) != null) {
						userAccLockDate = (JDatetime) userVect.get(4);
					}
					user.setACC_LOCKED_DATE(userAccLockDate);

					// Last Login
					JDatetime lastLoginDate = null;
					if (userVect.get(5) != null) {
						lastLoginDate = (JDatetime) userVect.get(5);
						lastLoginDate = lastLoginDate.cleanMillis();
					}
					user.setLAST_LOGIN_DATE(lastLoginDate);

					// Add Groups to the user
					if (!Util.isEmpty((String) userVect.get(7))) {
						user.setUSER_GROUP((String) userVect.get(7));
					}

					// Creation Date
					user.setUSER_CREATION_DATE(creationDate);

					// // Add Groups tot he user
					// String usrGrp = userGroupsMap.get(user.getUSER_CODE());
					// if (!Util.isEmpty(usrGrp))
					// user.setUSER_GROUP(usrGrp);

					userAuditItems.add(user);
				}

			}
		}

		// Add one item for each deleted user
		if (!Util.isEmpty(deletionDateMap)) {
			Iterator<String> iterator = deletionDateMap.keySet().iterator();
			while (iterator.hasNext()) {
				String userName = iterator.next();
				JDatetime userDeletionTime = deletionDateMap.get(userName);
				UserAuditItem user = new UserAuditItem();
				user.setUSER_NAME(userName);
				user.setDeletedDate(userDeletionTime);
				userAuditItems.add(user);
			}

		}

		return userAuditItems;
	}

	/**
	 * This method retrieves user creation date information from BO_Audit table and returns a hashmap of <user_name,
	 * creationDate>.
	 * 
	 * @param endDate
	 *            The last date to be taken in account for the report
	 * @param errorMsgs
	 *            Vector of errors to be shown in the report screen
	 * @return A HashMap with the creation date for each user name
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, JDatetime> getUserCreationDateInfo(final JDate endDate,
			@SuppressWarnings("rawtypes") final Vector errorMsgs) {

		final HashMap<String, JDatetime> map = new HashMap<String, JDatetime>();

		final StringBuilder SQL = new StringBuilder();
		SQL.append("select entity_name, modif_date from bo_audit ");
		SQL.append("WHERE entity_class_name LIKE 'UserAccessPermission' AND entity_name LIKE 'New User-%' ");
		SQL.append("ORDER BY modif_date ASC ");

		@SuppressWarnings("rawtypes")
		Vector rsVect = null;

		try {

			rsVect = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
					SQL.toString());
		} catch (final RemoteException re) {
			errorMsgs.add("An Exception ocurred when retreiving User creation Date Info:\n" + re);
			return map;
		}

		if ((rsVect != null) && (rsVect.size() >= 3)) {
			for (int i = 2; i < rsVect.size(); i++) {
				@SuppressWarnings("rawtypes")
				final Vector vect = (Vector) rsVect.get(i);

				// UserNameStr will be in the format of "New User-calypso_user"
				// so we need to get the user name out of it
				final String userNameStr = (String) vect.get(0);
				final String userName = userNameStr.substring(9);
				final JDatetime creationDate = (JDatetime) vect.get(1);

				JDatetime creationDateFromMap = map.get(userName);
				if ((creationDateFromMap == null) || (endDate == null) || creationDate.getJDate(TimeZone.getDefault()).lte(endDate)) {
					map.put(userName, creationDate);
				}
			}
		}

		return map;
	}

	// CAL_COLLAT_REPORT_0114
	/**
	 * Retrieves the deletion date for every user in the system in a map <user, deletion date>.
	 * 
	 * @param endDate
	 *            The first date to be taken in account for the report
	 * @param errorMsgs
	 *            Vector of errors to be shown in the report screen
	 * @return A HashMap with the deletion date for each user name
	 */
	public HashMap<String, JDatetime> getUserDeletionDateInfo(String enteredUserName, final JDate startDate,
			final Vector<String> errorMsgs) {

		final HashMap<String, JDatetime> map = new HashMap<String, JDatetime>();

		final StringBuilder SQL = new StringBuilder();
		SQL.append("select entity_name, modif_date from bo_audit ");
		if (Util.isEmpty(enteredUserName)) {
			SQL.append("WHERE entity_class_name LIKE 'UserAccessPermission' AND entity_name LIKE 'Deleted User-%' ");
		} else {
			SQL.append("WHERE entity_class_name LIKE 'UserAccessPermission' AND entity_name LIKE 'Deleted User-"
					+ enteredUserName + "%' ");
		}
		SQL.append("ORDER BY modif_date ASC ");

		@SuppressWarnings("rawtypes")
		Vector rsVect = null;

		try {

			rsVect = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
					SQL.toString());
		} catch (final RemoteException re) {
			errorMsgs.add("An Exception ocurred when retreiving User deletion Date Info:\n" + re);
			return map;
		}

		if ((rsVect != null) && (rsVect.size() >= 3)) {
			for (int i = 2; i < rsVect.size(); i++) {
				@SuppressWarnings("rawtypes")
				final Vector vect = (Vector) rsVect.get(i);

				// UserNameStr will be in the format of "Deleted User-calypso_user"
				// so we need to get the user name out of it
				final String userNameStr = (String) vect.get(0);
				final String userName = userNameStr.substring("Deleted User-".length());
				final JDatetime deletionDate = (JDatetime) vect.get(1);

				JDatetime deletionDateFromMap = map.get(userName);
				if ((deletionDateFromMap == null) || (startDate == null) || deletionDate.getJDate(TimeZone.getDefault()).gte(startDate)) {
					map.put(userName, deletionDate);
				}
			}
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, String> getUserGroups(@SuppressWarnings("rawtypes") final Vector errorMsgs) {
		final HashMap<String, String> map = new HashMap<String, String>();

		final String SQL = "select access_value, group_name from group_access "
				+ "where access_id=0 order by access_value Asc ";

		@SuppressWarnings("rawtypes")
		Vector rsVect = null;

		try {
			rsVect = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(SQL);
		} catch (final RemoteException re) {
			errorMsgs.add("An Exception ocurred when retreiving User creation Date Info:\n" + re);
			return map;
		}

		String prevUser = "", groupList = "";
		if ((rsVect != null) && (rsVect.size() >= 3)) {
			for (int i = 2; i < rsVect.size(); i++) {
				@SuppressWarnings("rawtypes")
				final Vector vect = (Vector) rsVect.get(i);

				// so we need to get the user name out of it
				final String userName = (String) vect.get(0);
				final String groupName = (String) vect.get(1);

				// if it is same user then add the group name to the groupList
				if ((Util.isEmpty(prevUser)) || userName.equals(prevUser)) {
					if (Util.isEmpty(groupList)) {
						groupList = groupList + groupName;
					} else {
						groupList = groupList + ", " + groupName;
					}
				}

				if ((!Util.isEmpty(prevUser) && !userName.equals(prevUser)) || ((rsVect.size() - 1) == i)) {
					map.put(prevUser, groupList);

					// If the last user has only one group then add it to the
					// map
					if (((rsVect.size() - 1) == i) && (!userName.equals(prevUser))) {
						map.put(userName, groupName);
					}

					groupList = groupName;
				}

				prevUser = userName;

			}
		}

		return map;
	}

	public String formatDate(final JDatetime dateTime) {
		if (dateTime == null) {
			return null;
		}

		final DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("CET"));
		return formatter.format(dateTime);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "Test");

		final UserAuditReport report = new UserAuditReport();
		@SuppressWarnings("rawtypes")
		final HashMap<String, String> userGroups = report.getUserGroups(new Vector());
		System.out.println(userGroups);

	}
}
/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import calypsox.tk.report.SantAuditReport.SantAuditValue;

public class SantAuditReportStyle extends ReportStyle {

	private static final long serialVersionUID = 656666276984750150L;

	public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";

	public static final String TYPE_MODIFIED_DATA = "Type Of Modified Data";

	public static final String PARAMETER = "Parameter";

	public static final String OLD_VALUE = "Old Value";

	public static final String NEW_VALUE = "New Value";

	public static final String DATE_TIME = "Date - Time Of Change";

	public static final String USER = "USER Id - Name";

	public static String[] DEFAULT_COLUMNS = { COLLATERAL_AGREEMENT, TYPE_MODIFIED_DATA, PARAMETER, OLD_VALUE,
			NEW_VALUE, DATE_TIME, USER };
	
	protected static final String dateFormat = "dd/MM/yyyy";

	// private final AuditReportStyle auditReportStyle = (AuditReportStyle) ReportStyle.getReportStyle("Audit");

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		SantAuditValue sav = (SantAuditValue) row.getProperty("SantAudit");

		if (COLLATERAL_AGREEMENT.equals(columnName)) {
			return sav.getAgreementName();
		} else if (TYPE_MODIFIED_DATA.equals(columnName)) {
			return sav.getTypeOfChange();
		} else if (PARAMETER.equals(columnName)) {
			return sav.getParameter();
		} else if (OLD_VALUE.equals(columnName)) {
			return sav.getOldValue();
		} else if (NEW_VALUE.equals(columnName)) {
			return sav.getNewValue();
		} else if (DATE_TIME.equals(columnName)) {
			return String.valueOf(sav.getModifiedDate());
		} else if (USER.equals(columnName)) {
			return sav.getUserName();
		}

		return null;
	}

	// @Override
	// public TreeList getTreeList() {
	// TreeList treeList = super.getTreeList();
	// treeList.add(this.auditReportStyle.getNonInheritedTreeList());
	// return treeList;
	// }

}

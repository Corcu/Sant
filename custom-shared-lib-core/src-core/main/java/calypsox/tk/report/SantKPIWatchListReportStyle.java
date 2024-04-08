/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.kpiwatchlist.SantKPIWatchListItem;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantKPIWatchListReportStyle extends ReportStyle {

	private static final long serialVersionUID = -8818964382810519621L;

	public static final String REPORT_DATE = "REPORT DATE";
	public static final String OWNER = "OWNER";
	public static final String ID = "ID";
	public static final String DESCRIPTION = "DESCRIPTION";
	public static final String TYPE = "TYPE";
	public static final String FREQUENCY = "FREQUENCY";
	public static final String MARGIN_CALL = "MARGIN CALL";
	public static final String BASE_CURRENCY = "BASE CURRENCY";
	public static final String MTM_EUR = "MTM EUR";
	public static final String INDEPENDENT = "INDEPENDENT";
	public static final String THRESHOLD = "THRESHOLD";
	public static final String MTA = "MTA";
	public static final String BALANCE = "BALANCE";
	public static final String UNPAID_DAYS = "UNPAID DAYS";
	public static final String STATUS = "STATUS";

	public static final String[] DEFAULT_COLUMNS = { REPORT_DATE, OWNER, ID, DESCRIPTION, TYPE, FREQUENCY, MARGIN_CALL,
			BASE_CURRENCY, MTM_EUR, INDEPENDENT, THRESHOLD, MTA, BALANCE, UNPAID_DAYS, STATUS };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		SantKPIWatchListItem item = (SantKPIWatchListItem) row.getProperty("SantKPIWatchListItem");
		return item.getColumnValue(columnName);

	}

}

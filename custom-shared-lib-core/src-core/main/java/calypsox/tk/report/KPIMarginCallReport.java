package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.restservices.WebServiceReport;
import calypsox.util.CheckRowsNumberReport;

public class KPIMarginCallReport extends Report implements CheckRowsNumberReport, WebServiceReport {
	private static final long serialVersionUID = 1L;

	@Override
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs) {
		return load(errorMsgs);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		String currency = null;
		String subtype = null;
		JDate from = null;
		JDate to = null;
		final DefaultReportOutput dro = new DefaultReportOutput(this);
		final ReportTemplate reportTemplate = getReportTemplate();
		JDatetime valDatetime = dro.getValDate();
		if (reportTemplate.getAttributes().get("KPICurrency") != null) {
			currency = reportTemplate.getAttributes().get("KPICurrency").toString();
		}

		if (reportTemplate.getAttributes().get("KPItype") != null) {
			subtype = reportTemplate.getAttributes().get("KPItype").toString();
		}
		if (reportTemplate.getAttributes().get("KPIfrom") != null) {
			from = Util.stringToJDate(reportTemplate.getAttributes().get("KPIfrom"));
		} else {
			from = valDatetime.getJDate(TimeZone.getDefault());
		}
		if (reportTemplate.getAttributes().get("KPIto") != null) {
			to = Util.stringToJDate(reportTemplate.getAttributes().get("KPIto"));
		} else {
			to = valDatetime.getJDate(TimeZone.getDefault());
		}

		ReportRow[] rowValues = null;
		if (reportTemplate.getAttributes().get("KPIquery").toString().contains("MC_STATUS")) {
			rowValues = getMarginCallKPIStatus(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("MC_CONTRACT_TYPE")) {
			rowValues = getMarginCallKPIContract(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("MC_METRICS")) {
			rowValues = getMarginCallKPIMetrics(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("MC_SETTLE_CASH")) {
			rowValues = getMarginCallKPISettleCash(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("MC_SETTLE_NON_CASH")) {
			rowValues = getMarginCallKPISettleNonCash(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("PDV_SETTLE_NON_CASH")) {
			rowValues = getPDVSettleNonCash(currency, subtype, from, to);
		} else if (reportTemplate.getAttributes().get("KPIquery").toString().contains("SETTLE_BALANCE")) {
			rowValues = getPDVSettleBalance(currency, subtype, from, to);
		}

		if (rowValues != null && rowValues.length > 0) {
			addPotentialSize("KPIMarginCall", rowValues.length);
			dro.setRows(rowValues);
		}

		return dro;
	}

	/**
	 * Set Custom row properties
	 * 
	 * @param row    ReportRow
	 * @param dsConn DSCOnnection object
	 * @return Added properties row
	 */
	public ReportRow setRowData(ReportRow row, DSConnection dsConn) {
		return row;
	}

	@Override
	public Map getPotentialSize() {
		addPotentialSize("KPIMarginCall", 0);
		return super.getPotentialSize();
	}

	protected ReportRow[] getMarginCallKPIStatus(String currency, String subtype, JDate from, JDate to) {

		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		StringBuilder query2 = new StringBuilder();
		query2.append(" (CASE WHEN MCE.STATUS IN (");
		query2.append("'TRIPARTY_AGREED','PART_EXECUTED','FULL_EXECUTED','EXECUTED') THEN 'COMPLETED' ");
		query2.append(" WHEN MCE.STATUS IN ('DEMAND_SENT','PRICED_RECEIVE') THEN 'MARGIN_CALL' ");
		query2.append(" WHEN MCE.STATUS IN ('FULLY_DISPUTED','FULLY_DISPUTED_REC') THEN 'DISPUTED' ");
		query2.append(" WHEN MCE.STATUS IN ('PRICING,PRICING_REC,RECEIVED') THEN 'PENDING' ");
		query2.append(" WHEN MCE.STATUS IN ('PRICED_NO_CALL') THEN 'CANCELED' ");
		query2.append(" ELSE 'CANCELED' END) ");
		query.append(" SELECT MCE.PROCESS_DATE, ");
		query.append(query2);
		query.append(" STATUS, COUNT(MCE.ID) VALUE FROM MARGIN_CALL_ENTRIES MCE ");
		query.append(" INNER JOIN COLLATERAL_CONFIG CC ON (MCE.MCC_ID=CC.MCC_ID OR MCE.MASTER_CONFIG_ID=CC.MCC_ID) ");
		query.append(" WHERE ");
		if (!Util.isEmpty(subtype)) {
			if(subtype!=null && subtype.equals("ACADIA")) {
				query.append(" CC.WF_SUBTYPE IN(?) AND ");
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
			}else if(subtype!=null && !subtype.equals("ALL")){
				query.append(" CC.WF_SUBTYPE NOT IN(?) AND ");
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
			}
		} 
		if (!Util.isEmpty(currency)) {
			query.append(" MCE.CONTRACT_CURRENCY IN(?) AND ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" MCE.PROCESS_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY MCE.PROCESS_DATE, ");
		query.append(query2);
		query.append(" ORDER BY MCE.PROCESS_DATE, ");
		query.append(query2);

		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);

		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}
		return mapStatus;

	}

	protected ReportRow[] getMarginCallKPIContract(String currency, String subtype, JDate from, JDate to) {

		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		query.append("SELECT MCE.PROCESS_DATE,MRC.CONTRACT_TYPE ,COUNT(MCE.ID) FROM MARGIN_CALL_ENTRIES MCE ");
		query.append(" INNER JOIN COLLATERAL_CONFIG CC ON (MCE.MCC_ID=CC.MCC_ID OR MCE.MASTER_CONFIG_ID=CC.MCC_ID) ");
		query.append(" INNER JOIN MRGCALL_CONFIG MRC ON (MCE.MCC_ID=MRC.MRG_CALL_DEF OR ");
		query.append(" MCE.MASTER_CONFIG_ID=CC.MCC_ID)");
		query.append(" WHERE ");
		if (!Util.isEmpty(subtype)) {
			if (subtype != null && subtype.equals("ACADIA")) {
				query.append(" CC.WF_SUBTYPE IN(?) AND ");
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
			} else if (subtype != null && !subtype.equals("ALL")) {
				query.append(" CC.WF_SUBTYPE NOT IN(?) AND ");
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
			}
		}
		if (!Util.isEmpty(currency)) {
			query.append(" MCE.CONTRACT_CURRENCY IN(?) AND ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}

		query.append(" MCE.PROCESS_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY MCE.PROCESS_DATE,MRC.CONTRACT_TYPE ");
		query.append(" ORDER BY MCE.PROCESS_DATE,MRC.CONTRACT_TYPE ");

		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);

		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}

		return mapStatus;
	}

	protected ReportRow[] getMarginCallKPIMetrics(String currency, String subtype, JDate from, JDate to) {

		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		String[] lst = new String[] { "NET_BALANCE", "NET_EXPOSURE", "MARGIN_REQUIRED" };
		StringBuilder queryUnion = new StringBuilder();
		for (String strField : lst) {
			StringBuilder query = new StringBuilder();
			query.append(" SELECT MCE.PROCESS_DATE, '");
			query.append(strField);
			query.append("' NAME, SUM(");
			query.append(strField);
			query.append(") FROM MARGIN_CALL_ENTRIES MCE  ");
			query.append(
					"  INNER JOIN COLLATERAL_CONFIG CC ON (MCE.MCC_ID=CC.MCC_ID OR MCE.MASTER_CONFIG_ID=CC.MCC_ID)  ");
			query.append("  WHERE  ");
			if (!Util.isEmpty(subtype)) {
				if (subtype != null && subtype.equals("ACADIA")) {
					query.append(" CC.WF_SUBTYPE IN(?) AND ");
					bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
				} else if (subtype != null && !subtype.equals("ALL")) {
					query.append(" CC.WF_SUBTYPE NOT IN(?) AND ");
					bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ACADIA"));
				}
			}
			if (!Util.isEmpty(currency)) {
				query.append(" MCE.CONTRACT_CURRENCY IN(?) AND ");
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
			}
			query.append(" MCE.PROCESS_DATE BETWEEN ? AND ? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
			query.append(" GROUP BY MCE.PROCESS_DATE ");
			if (queryUnion.length() > 0) {
				queryUnion.append(" UNION ");
			}
			queryUnion.append(query);
		}
		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(queryUnion, bindVariables);

		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}

		return mapStatus;
	}

	protected ReportRow[] getMarginCallKPISettleCash(String currency, String agreeType, JDate from, JDate to) {

		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" STATUS, COUNT(TRANSFER_ID) VALUE FROM BO_TRANSFER ");
		query.append(" INNER JOIN PRODUCT_SIMPLEXFER ON BO_TRANSFER.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON MRGCALL_CONFIG.MRG_CALL_DEF=PRODUCT_SIMPLEXFER.LINKED_ID ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='MarginCall' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE='COLLATERAL' ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");

		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);

		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}

		return mapStatus;
	}

	protected ReportRow[] getMarginCallKPISettleNonCash(String currency, String agreeType, JDate from, JDate to) {

		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" STATUS, COUNT(TRANSFER_ID) VALUE FROM BO_TRANSFER ");
		query.append(" INNER JOIN TRADE ON TRADE.TRADE_ID=BO_TRANSFER.TRADE_ID ");
		query.append(" INNER JOIN PRODUCT_SIMPLEXFER ON TRADE.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON MRGCALL_CONFIG.MRG_CALL_DEF=PRODUCT_SIMPLEXFER.LINKED_ID ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='MarginCall' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE='SECURITY' ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");

		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);

		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}

		return mapStatus;
	}
	private ReportRow[] orderMap(Vector<?> rawResultSet) {
		ReportRow[] mapStatus = new ReportRow[rawResultSet.size() - 2];

		for (int i = 2; i < rawResultSet.size(); i++) {
			Vector<?> data = (Vector<?>) rawResultSet.get(i);

			if (data.size() > 2) {
				JDate date = ((JDatetime) data.get(0)).getJDate(TimeZone.getDefault());
				String name = data.get(1).toString();
				Double value = (Double) data.get(2);
				ReportRow rr = new ReportRow(date.toString() + name);
				rr.setProperty(KPIMarginCallReportStyle.DATE, date);
				rr.setProperty(KPIMarginCallReportStyle.NAME, name);
				rr.setProperty(KPIMarginCallReportStyle.VALUE, value);
				mapStatus[i - 2] = rr;
			}
		}
		return mapStatus;

	}

	protected ReportRow[] getPDVSettleNonCash(String currency, String agreeType, JDate from, JDate to) {
		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END) ");
		query.append(" STATUS, COUNT(TRANSFER_ID) FROM BO_TRANSFER ");
		query.append(" INNER JOIN TRADE_KEYWORD ON BO_TRANSFER.TRADE_ID=TRADE_KEYWORD.TRADE_ID AND ");
		query.append(" TRADE_KEYWORD.KEYWORD_NAME='MARGIN_CALL_CONFIG_ID' ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON");
		query.append(" TO_NUMBER(TRADE_KEYWORD.KEYWORD_VALUE)=MRGCALL_CONFIG.MRG_CALL_DEF ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='SecLending' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE IN('SECURITY') ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);
		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}
		return mapStatus;
	}

	protected ReportRow[] getPDVSettleBalance(String currency, String agreeType, JDate from, JDate to) {
		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		query.append(" SELECT TABLE_UNION.VALUE_DATE,TABLE_UNION.STATUS,SUM(TABLE_UNION.VALUE) FROM (");
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" STATUS, SUM(AMOUNT) VALUE FROM BO_TRANSFER ");
		query.append(" INNER JOIN PRODUCT_SIMPLEXFER ON BO_TRANSFER.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON MRGCALL_CONFIG.MRG_CALL_DEF=PRODUCT_SIMPLEXFER.LINKED_ID ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='MarginCall' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE='COLLATERAL' ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" UNION ");
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" STATUS,  SUM(AMOUNT) VALUE FROM BO_TRANSFER ");
		query.append(" INNER JOIN TRADE ON TRADE.TRADE_ID=BO_TRANSFER.TRADE_ID ");
		query.append(" INNER JOIN PRODUCT_SIMPLEXFER ON TRADE.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON MRGCALL_CONFIG.MRG_CALL_DEF=PRODUCT_SIMPLEXFER.LINKED_ID ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='MarginCall' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE='SECURITY' ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" UNION ");
		query.append(" SELECT BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END) ");
		query.append(" STATUS,  SUM(AMOUNT) VALUE FROM BO_TRANSFER ");
		query.append(" INNER JOIN TRADE_KEYWORD ON BO_TRANSFER.TRADE_ID=TRADE_KEYWORD.TRADE_ID AND ");
		query.append(" TRADE_KEYWORD.KEYWORD_NAME='MARGIN_CALL_CONFIG_ID' ");
		query.append(" INNER JOIN MRGCALL_CONFIG ON");
		query.append(" TO_NUMBER(TRADE_KEYWORD.KEYWORD_VALUE)=MRGCALL_CONFIG.MRG_CALL_DEF ");
		query.append(" WHERE BO_TRANSFER.PRODUCT_TYPE='SecLending' ");
		query.append(" AND BO_TRANSFER.TRANSFER_STATUS NOT IN('CANCELED') ");
		query.append(" AND BO_TRANSFER.TRANSFER_TYPE IN('SECURITY') ");
		if (!Util.isEmpty(agreeType)) {
			query.append(" AND MRGCALL_CONFIG.CONTRACT_TYPE=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, agreeType));
		}
		if (!Util.isEmpty(currency)) {
			query.append(" AND BO_TRANSFER.AMOUNT_CCY=? ");
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, currency));
		}
		query.append(" AND BO_TRANSFER.VALUE_DATE BETWEEN ? AND ? ");
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, from));
		bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, to));
		query.append(" GROUP BY BO_TRANSFER.VALUE_DATE, ");
		query.append(" (CASE WHEN BO_TRANSFER.TRANSFER_STATUS='SETTLED' THEN 'SETTLED' ELSE 'NON_SETTLED' END)");
		query.append(" ) TABLE_UNION GROUP BY TABLE_UNION.VALUE_DATE,TABLE_UNION.STATUS");
		ReportRow[] mapStatus = null;
		Vector<?> rawResultSet = executeSQL(query, bindVariables);
		if (rawResultSet.size() > 2) {
			mapStatus = orderMap(rawResultSet);
		}
		return mapStatus;
	}
	private Vector<?> executeSQL(StringBuilder query, List<CalypsoBindVariable> bindVariables) {
		Vector<?> rawResultSet = null;
		try {

			rawResultSet = DSConnection.getDefault().getRemoteAccess().executeSelectSQL(query.toString(),
					bindVariables);

		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not execute query.", e);
		}

		return rawResultSet;
	}

}
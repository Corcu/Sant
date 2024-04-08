/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

import java.util.Vector;

public class SantInterestPaymentRunnerReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 6534316572857783805L;

	// for Report template
	public static final String CALL_ACCOUNT_ID = "CallAccountIds";
	public static final String AGREEMENT_ID = "AGREEMENT_ID";
	public static final String AGREEMENT_TYPE = "AGREEMENT_TYPE";
	public static final String RATE_INDEX = "RATE_INDEX";
	public static final String ACCOUNT_MAP = "AccountMap";
	public static final String CONTRACT_MAP = "ContractMap";

	public static final String LE_ROLE = "LegalEntityRole";
	public static final String SELECT_ALL = "SelectAll";
	public static final String CURRENCY = "Currency";
	// public static final String OWNER_AGR = "OWNER_AGR";

	// for Report rows
	public static final String ROW_DATA = "AccountRowData";
	public static final String SELECT_ALL_ROW_DATA = "SelectAllRowData";



	@Override
	public void setDefaults() {
		super.setDefaults();

		// Set default sorted columns
		Vector<String> columns = new Vector<String>();
		columns.add(SantInterestPaymentRunnerReportStyle.ACCOUNT_ID);
		columns.add(SantInterestPaymentRunnerReportStyle.ACCOUNT_NAME);
		columns.add(SantInterestPaymentRunnerReportStyle.ACCOUNT_CURRENCY);
		columns.add(SantInterestPaymentRunnerReportStyle.PO_OWNER);
		columns.add(SantInterestPaymentRunnerReportStyle.PROCESS_DATE);
		// BAU 6.1 - Add column Contract ID
		columns.add(SantInterestPaymentRunnerReportStyle.CONTRACT_ID);
		// GSM: Interest watch property added. 03/12/12
		columns.add(SantInterestPaymentRunnerReportStyle.WATCH_INTEREST);
		columns.add(SantInterestPaymentRunnerReportStyle.ADHOC_PAYMENT);
		columns.add(SantInterestPaymentRunnerReportStyle.AMOUNT_IB);
		columns.add(SantInterestPaymentRunnerReportStyle.AMOUNT_CT);
		columns.add(SantInterestPaymentRunnerReportStyle.SETTLE_DATE_CT);
		// GSM: Simple Transfer amount added. 03/12/
		columns.add(SantInterestPaymentRunnerReportStyle.SIMPLE_XFER_AMOUNT);
		columns.add(SantInterestPaymentRunnerReportStyle.HAS_SIMPLE_XFER);
		columns.add(SantInterestPaymentRunnerReportStyle.SELECT);
		//GSM 31/05/2017 - Chile requires cpty shortname in this report
		columns.add(SantInterestPaymentRunnerReportStyle.CPTY_SHORTNAME);
		columns.add(SantInterestPaymentRunnerReportStyle.MESSAGE_STATUS);

		super.setSortColumns(columns.toArray(new String[columns.size()]));
	}

}

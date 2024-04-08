package calypsox.tk.report.generic;

import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.MarginCallEntryDTOReportTemplate;
import com.calypso.tk.report.ReportTemplate;

public abstract class SantGenericTradeReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 7642594686568277431L;

	/*
	 * GSM 22/07/15. SBNA Multi-PO filter. Change the attribute value to match the Calypso Core name when filtering by
	 * processing Org. The idea is to try to match as max possible the attribute value when passed throught ST.
	 */
	public static final String PROCESSING_ORG_NAMES = BOSecurityPositionReportTemplate.PROCESSING_ORG;
	public static final String PROCESSING_ORG_IDS = "OWNER_AGR_IDS";// MarginCallEntryDTOReportTemplate.PROCESSING_ORG_IDS;
	public static final String PROCESSING_ORG_IDS_COL = MarginCallEntryDTOReportTemplate.PROCESSING_ORG_IDS;
	// public static final String OWNER_AGR = "OWNER_AGR";
	public static final String OWNER_DEALS = "OWNER_DEALS";
	public static final String COUNTERPARTY = "COUNTERPARTY";
	public static final String TRADE_STATUS = "TRADE_STATUS";
	public static final String INSTRUMENT_TYPE = "INSTRUMENT_TYPE";
	public static final String AGREEMENT_ID = "AGREEMENT_ID";
	public static final String AGREEMENT_TYPE = "AGREEMENT_TYPE";
	public static final String AGREEMENT_STATUS = "AGREEMENT_STATUS";
	public static final String AGREEMENT_DIRECTION = "AGREEMENT_DIRECTION";
	public static final String PO_ELIGIBLE_SEC_IND = "PO_ELIGIBLE_SEC_IND";
	public static final String VALUATION_AGENT = "VALUATION_AGENT";
	public static final String TRADE_ID = "TRADE_ID";
	public static final String ECONOMIC_SECTOR = "ECONOMIC_SECTOR";
	public static final String HEAD_CLONE_INDICATOR = "HEAD_CLONE_INDICATOR";
	public static final String MATURE_DEALS = "MATURE_DEALS";
	public static final String MTM_ZERO = "MTM_ZERO";
	public static final String MANUALLY_MODIFIED = "MTM_MANUALLY_MODIFIED";
	public static final String PORTFOLIO = "PORTFOLIO";
	public static final String STRUCTURE_ID = "STRUCTURE_ID";
	public static final String FUND_ONLY = "FUND_ONLY";
	public static final String IS_FUND = "IS_FUND";
	public static final String BASE_CURRENCY = "BASE_CURRENCY";
	public static final String LAST_ALLOCATION_CURRENCY = "LAST_ALLOCATION_CURRENCY";

	public static final String DELINQUENT_THRESHOLD = "DELINQUENT_THRESHOLD";

	@Override
	public void setDefaults() {
		setColumns(SantGenericTradeReportStyle.DEFAULTS_COLUMNS);
	}

}

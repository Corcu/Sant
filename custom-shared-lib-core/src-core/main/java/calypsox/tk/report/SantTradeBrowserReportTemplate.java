package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantTradeBrowserReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 7319310272918398103L;
	public static final String BO_REFERENCE = "BO_REFERENCE";
	public static final String CONTRACT_IDS = "CONTRACT_IDS";
	// public static final String MTM_CURRENCY="MTM_CURRENCY";
	// GSM: rigCode
	public static final String RIG_CODE = "RIG_CODE";
	public static final String AGR_TYPE = "AgrType";
	public static final String TRADE_STATUS = "TradeStatus";
	public static final String BUY_SELL = "BuySell";
	public static final String INSTRUMENT = "instrument";
	public static final String FRONT_ID = "FronId";

	public static final String SOURCE = "Source";
	public static final String STRUCTURE = "Structure";
	public static final String VAL_AGENT = "ValAgent";
	public static final String VAL_DATE = "VAL_DATE";
	public static final String VAL_DATE_FROM = "VAL_DATE_FROM";
	public static final String VAL_DATE_TO = "VAL_DATE_TO";

	public static final String PROCESSING_ORG_AGR = "ProcessingOrgAgr";
	public static final String PROCESSING_ORG_DEAL = "ProcessingOrgDeal";

	// ARCHIVE
	public static final String INCLUDE_ARCHIVE = "IncludeArchive";

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantMTMAuditReportStyle.DEFAULTS_COLUMNS);
	}
}

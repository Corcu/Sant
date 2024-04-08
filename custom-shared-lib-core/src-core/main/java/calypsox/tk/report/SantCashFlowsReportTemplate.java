package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

@SuppressWarnings("serial")
public class SantCashFlowsReportTemplate extends SantGenericTradeReportTemplate {

    public static final String TRADE_DATE = "Trade Date";
    public static final String SISTEMA_ORIGEN = "SISTEMA_ORIGEN";
    public static final String PORTFOLIO = "Book";
    public static final String BASE_CCY = "Base Currency";
    public static final String COUNTERPARTY = "CounterParty";
    public static final String COUNTERPARTY_NAME = "CounterParty Name";
    public static final String CALL_ACCOUNT = "Call Account";
    public static final String DEAL_TYPE = "Deal Type";
    public static final String PRINCIPAL_AMOUNT = "Principal";
    public static final String VALUE_DATE = "Value Date";
    public static final String GLOBAL_BALANCE = "Global Balance";
    public static final String VAL_CAP_REP = "VAL_CAP_REP";
    public static final String AGREEMENT_NAME = "Margin Call Agreement";

    @Override
    public void setDefaults() {
	super.setDefaults();
	setColumns(SantCashFlowsReportStyle.DEFAULTS_COLUMNS);

    }
}

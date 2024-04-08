package calypsox.tk.collateral.pdv.importer;

public interface PDVConstants {
	
	// PDV TYPE
	public static final String COLLAT_CASH_PDV_TYPE = "COLLAT_CASH";
	public static final String COLLAT_SECURITY_PDV_TYPE = "COLLAT_SECURITY";
	public static final String SEC_LENDING_PDV_TYPE = "SEC_LENDING";

	// EXCEPTION TYPE
	public static final String PDV_ALLOC_EXCEPTION_TYPE = "EX_PDV_ALLOC";
	public static final String PDV_ALLOC_FUT_EXCEPTION_TYPE = "EX_PDV_ALLOC_FUT";
	public static final String PDV_TRADE_EXCEPTION_TYPE = "EX_PDV_TRADE";
	public static final String PDV_LIQUIDATION_EXCEPTION_TYPE = "EX_PDV_LIQUID";
	
	// TRADE KEYWORDS
	public static final String MC_CONTRACT_NUMBER_TRADE_KEYWORD = "MC_CONTRACT_NUMBER";
	public static final String IS_FINANCEMENT_TRADE_KEYWORD = "IS_FINANCEMENT";
	public static final String DVP_FOP_TRADE_KEYWORD = "DVP-FOP";
	public static final String SETTLEMENT_STATUS_TRADE_KEYWORD = "SETTLEMENT_STATUS";
	public static final String COLLAT_ID_TRADE_KEYWORD = "COLLAT_ID";
	
	// PDV TRADE FIELDS
	public static final String TRADE_INSTRUMENT_FIELD = "INSTRUMENT";
	public static final String TRADE_NUM_FRONT_ID_FIELD = "NUM_FRONT_ID";
	
	// PDV COLLAT FIELDS
	public static final String COLLAT_ACTION_FIELD = "ACTION";
	public static final String COLLAT_FO_SYSTEM_FIELD = "BO_SYSTEM";
	public static final String COLLAT_NUM_FRONT_ID_FIELD = "NUM_FRONT_ID";
	public static final String COLLAT_COLLAT_ID_FIELD = "COLLAT_ID";
	public static final String COLLAT_OWNER_FIELD = "OWNER";
	public static final String COLLAT_COUNTERPARTY_FIELD = "COUNTERPARTY";
	public static final String COLLAT_INSTRUMENT_FIELD = "INSTRUMENT";
	public static final String COLLAT_PORTFOLIO_FIELD = "PORTFOLIO";
	public static final String COLLAT_VALUE_DATE_FIELD = "VALUE_DATE";
	public static final String COLLAT_TRADE_DATE_FIELD = "TRADE_DATE";
	public static final String COLLAT_DIRECTION_FIELD = "DIRECTION";
	public static final String COLLAT_AMOUNT_FIELD = "AMOUNT";
	public static final String COLLAT_AMOUNT_CCY_FIELD = "AMOUNT_CCY";
	public static final String COLLAT_UNDERLYING_TYPE_FIELD = "UNDERLYING_TYPE";
	public static final String COLLAT_UNDERLYING_FIELD = "UNDERLYING";
	public static final String COLLAT_CLOSING_PRICE_FIELD = "CLOSING_PRICE";
	public static final String SLB_BUNDLE_FIELD = "SLB_BUNDLE";
	
	// PDV LIQUIDATION FIELDS
	public static final String LIQUIDATION_ID_MESSAGE = "ID_MESSAGE";
	public static final String LIQUIDATION_FO_SYSTEM = "FO_SYSTEM";
	public static final String LIQUIDATION_ID_MUREX = "ID_MUREX";
	public static final String LIQUIDATION_SETTLEMENT_DATE = "SETTLEMENT_DATE";
	public static final String LIQUIDATION_SETTLEMENT_STATUS = "SETTLEMENT_STATUS";
	public static final String LIQUIDATION_COMMENT = "COMMENT";
	
	// ALLOC ATTRIBUTES
	public static final String SETTLEMENT_STATUS_ALLOC_ATTR = "SETTLEMENT_STATUS";
	public static final String MC_ALLOC_DELIVERY_TYPE = "MarginCallAllocation.DVP-FOP";
	
	public static final String MORE_THAN_ONE_ELIGIBLE_CONTRACT = "More than one eligible contract exist for this trade.";
	
	// list of valid collateral.allocation actions
	public static enum PDV_ACTION {
		NEW,AMEND,CANCEL,INCREASE,DECREASE,SLA,MATURE,ACCLOSING
	}
	// list of valid collateral type
	public static enum PDV_COLLAT_TYPE {
		COLLAT_SECURITY,COLLAT_CASH
	}
}

package calypsox.tk.core;

public class CollateralStaticAttributes {

    public static final String OK = "OK";
    public static final String FAIL = "FAIL";
    public static final String TIME_OUT = "TIME_OUT";
    public static final String ERROR_AC = "ErrAC";

    public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    public static final String CONTRACT_TYPE = "CONTRACT_TYPE";
    public static final String MCC_HAIRCUT = "MCC_HAIRCUT%";
    public static final String FO_HAIRCUT = "FO_HAIRCUT";
    public static final String BO_SYSTEM = "BO_SYSTEM";
    public static final String BO_REFERENCE = "BO_REFERENCE";

    public static final String MC_ENTRY_PO_MTM = "PO MtM";
    public static final String MC_ENTRY_CPTY_MTM = "Cpty MtM";
    public static final String MC_ENTRY_MTM_DIFF = "MtM difference";
    public static final String MC_ENTRY_AGREED_MTM = "MtM Agreed Amount";

    public static final String TK_DVP_ALLOCATION = "DVP_ALLOCATION";
    public static final String DVP = "DVP";
    public static final String RVP = "RVP";
    public static final String FOP = "FOP";
    public static final String ISMA = "ISMA";
    public static final String SI = "SI";
    public static final String MC_TRIPARTY = "MC_TRIPARTY";
    public static final String TK_FATHER_FRONT_ID = "FATHER_FRONT_ID";
    public static final String ALIAS_KPLUS = "ALIAS_KPLUS";

    public static final String INSTRUMENT_TYPE_REPO = "REPO";
    public static final String INSTRUMENT_TYPE_SEC_LENDING = "SEC_LENDING";
    public static final String INSTRUMENT_TYPE_PERFORMANCESWAP = "PerformanceSwap";

    public static final String MCC_ADD_FIELD_ECONOMIC_SECTOR = "ECONOMIC_SECTOR";
    public static final String MCC_ADD_FIELD_HEAD_CLONE = "HEAD_CLONE";
    public static final String MCC_ADD_FIELD_MCC_COUPON_RIGHTS = "MCC_COUPON_RIGHTS";

    public static final String FEE_TYPE_FAIL_SETTL_MAT = "FAIL_SETTL_MAT";

    // Collateral Exposure trade Context constants
    public static final String CTX_CONTRACT_IA = "CONTRACT_IA";
    public static final String CTX_CONTRACT_ID = "CONTRACT_ID";

    public static final String SEC_LENDING = "SecLending";
    public static final String REPO = "Repo";
    public static final String COLLATERAL_EXPOSURE = "CollateralExposure";

    public static final String FEE_TYPE_IND_AMOUNT = "IND_AMOUNT";

    public static final String FEE_TYPE_IND_AMOUNT_PO = "IND_AMOUNT_PO";

    public static final String STATUS_VERIFIED = "VERIFIED";

    public static final String DOMAIN_SANT_MTM_CHANGE_REASON = "markAdjustmentReasonOTC";

    public static final String PL_MARK_AMEND_DEFAULT_REASON = "Actualizando con MtM Valuations";
    public static final String PL_MARK_NPV_REASON = "MtM without haircut";
    public static final String DOMAIN_CURRENCY = "currency";

    public static final String TRADE_NOT_FOUND = "Trade Not Found";

    public static final String DOMAIN_SANT_CUSTOMREPORT_PROPS = "SantCustomReport.Props";
    public static final String TRADEBROWSER2_MAX_SELECTABLE_AGRS = "TRADEBROWSER2_MAX_SELECTABLE_AGRS";
    public static final String PART_EXEC_STATUS_SINCE = "PART_EXEC_STATUS_SINCE";// stored
                                                                                 // on
                                                                                 // the
                                                                                 // MCEntry
                                                                                 // as
                                                                                 // JDate
    public static final String PART_EXEC_REMAINING_MC = "PART_EXEC_REMAINING_MC";// stored
                                                                                 // on
                                                                                 // the
                                                                                 // MCEntry
                                                                                 // as
                                                                                 // Double
    public static final String IS_PART_EXEC = "IS_PART_EXEC";// stored on the
                                                             // MCEntry as
                                                             // Boolean
    public static final String DOMAIN_ADD_INFO = "mccAdditionalField";

    // Manage delinquent
    public static final String IS_DELINQUENT = "IS_DELINQUENT"; // stored on the
                                                                // MCEntry as
                                                                // Boolean
    public static final String DELINQUENT_AMOUNT = "DELINQUENT_AMOUNT";// stored
                                                                       // on the
                                                                       // MCEntry
                                                                       // as
                                                                       // Double
    public static final String DELINQUENT_SINCE = "DELINQUENT_SINCE"; // stored
                                                                      // on the
                                                                      // MCEntry
                                                                      // as
                                                                      // JDate

    // An additional field on contract
    public static final String MCC_ADD_FIELD_MIGRATION_DATE = "MIGRATION_DATE";
    public static final String MCC_ADD_FIELD_MIGRATED_DELINQUENT_DATE = "MIGRATED_DELINQUENT_DATE";
    public static final String MCC_ADD_FIELD_MIGRATED_PART_EXEC_DATE = "MIGRATED_PART_EXEC_DATE";

    public static final String MOODY = "Moody";
    public static final String SNP = "S&P";
    public static final String FITCH = "Fitch";
    public static final String SC = "SC";

    // Bond ECB Attributes
    public static final String ECB_HAIRCUR = "ECB_Haircut";
    public static final String ECB_DISCOUNTABLE = "ECB_Discountable";
    public static final String ECB_LIQUIDITY_CLASS = "ECB_LiquidityClass";
    public static final String ECB_ASSET_TYPE = "ECB_AssetType";

    public static final String ALLOC_ATTR_SUBST_AND_REPLACE = "Subst&Replace";
    public static final String ENTRY_ATTR_WARNING_EXEC_SHORT = "WarningExecutionShort";
    public static final String ENTRY_ATTR_CONTINUE_EXEC_SHORT = "Continue ExecuteShort";

    public static final String BOND_SEC_CODE_REF_INTERNA = "REF_INTERNA";
    public static final String BOND_SEC_CODE_ISIN = "ISIN";
    public static final String BOND_SEC_CODE_CUSIP = "CUSIP";
    public static final String BOND_SEC_CODE_SEDOL = "SEDOL";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_ECB = "Discountable_ECB";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_SWISS = "Discountable_Swiss";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_BOE = "Discountable_BOE";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_FED = "Discountable_FED";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_EUREX = "Discountable_EUREX";
    public static final String BOND_SEC_CODE_DISCOUNTABLE_MEFF = "Discountable_MEFF";

    public static final String KEYWORD_FOREX_CLEAR_TYPE = "ForexClearType";
    public static final String KEYWORD_FOREX_CLEAR_REPORT = "ForexClearReport";
    public static final String KEYWORD_FOREX_REVERTED_TRADE_ID = "ForexClearRevertedTradeId";

}

package calypsox.tk.core;

public class KeywordConstantsUtil {

    // Legal Entity Attributes
    public final static String LE_ATTRIBUTE_IDEMPR = "Partenon-IDEMPR";
    public final static String LE_ATTRIBUTE_IDPROD = "Partenon-IDPROD";
    public final static String LE_ATTRIBUTE_FINANCIAL_NATURE = "FINANCIAL_NATURE";
    public final static String LE_ATTRIBUTE_CSB = "CSB";
    public static final String LE_ATTRIBUTE_CIF = "CIF";
    public static final String LE_ATTRIBUTE_SANT_ACC_BUSINESS_DATE = "SANT_ACC_BUSINESS_DATE";
    public static final String LE_ATTRIBUTE_RSK = "RSK";
    public static final String LE_ATTRIBUTE_CARGABAL = "CARGABAL";
    public static final String LE_ATTRIBUTE_PRIORIDAD = "Prioridad";
    public static final String LE_ATTRIBUTE_INTRAGRUPO = "INTRAGRUPO";
    public static final String LE_ATTRIBUTE_FXF = "FXF";

    // Message Entity Attributes
    public final static String MSG_ATTRIBUTE_REMIND_NUMBER = "REMIND_NUMBER";
    public static final String MSG_ATTRIBUTE_LINKED_ID = "LINKED_ID";
    public static final String MSG_ATTRIBUTE_MESSAGE_REF = "MessageRef";
    // Book Attributes
    public static final String BOOK_ATTRIBUTE_CANALOPE = "CANALOPE";
    public static final String BOOK_ATTRIBUTE_CANALOPEPTN = "CANALOPEPTN";
    // post Attributes
    // public static final String POST_ATTRIBUTE_AGREGO = "AGREGO";
    // transfer Attributes
    public static final String TRANSFER_ATTRIBUTE_TRN = "TRN";
    public static final String TRANSFER_ATTRIBUTE_LAST_STATUS = "LastStatus";
    public static final String TRANSFER_ATTRIBUTE_MANUAL_SETTLE = "ManualSettle";

    // Cres Attributes
    public static final String CRE_ATTRIBUTE_DGO_ERROR_CODE = "DGOErrorCode";
    public static final String CRE_ATTRIBUTE_DGO_ERROR_COMMENT = "DGOErrorComment";
    public static final String CRE_ATTRIBUTE_FINALIZED = "Finalized";
    public static final String CRE_ATTRIBUTE_NEED_CONVERSION = "Need Conversion";
    public static final String CRE_ATTRIBUTE_DGO_ANSWER_CODE = "DGO Answer Code";
    public static final String CRE_ATTRIBUTE_DGO_ANSWER_COMMENT = "DGO Answer Comment";
    public static final String CRE_ATTRIBUTE_DGO_ANSWER_DATE = "DGO Answer Date";
    // sdi attributes
    public static final String SDI_ATTRIBUTE_TCCORSPO = "TCCORSPO";
    // Keywords
    public final static String KEYWORD_IDCENT = "Partenon-IDCENT";
    public final static String KEYWORD_IDCONTR = "Partenon-IDCONTR";
    public final static String KEYWORD_MX_PLCUR = "PLCurrency";
    public final static String KEYWORD_MX_BAXTER_ID = "eFXTradeID";
    public final static String KEYWORD_PLATAFORM = "Platform";
    public final static String KEYWORD_PLATAFORM_TRADE_ID = "PlatformTradeID";
    public final static String KEYWORD_MX_COMPONENT_ID = "MXComponentID";
    public final static String KEYWORD_CANCEL_REISSUE_TO = "CancelReissueTo";
    public final static String KEYWORD_CANCEL_REISSUE_FROM = "CancelReissueFrom";
    public final static String KEYWORD_IDCONTR_NEAR = "Partenon-IDCONTR_NEAR";
    public final static String KEYWORD_IDCONTR_FAR = "Partenon-IDCONTR_FAR";
    public final static String KEYWORD_BOOKINGDATE = "Partenon-BOOKINGDATE";
    public static final String KEYWORD_PL_CURRENCY = "PLCurrency";
    public static final String KEYWORD_IDCONTR_FAR_MIRROR = "Partenon-IDCONTR_FAR_MIRROR";
    public static final String KEYWORD_IDCONTR_NEAR_MIRROR = "Partenon-IDCONTR_NEAR_MIRROR";
    public static final String KEYWORD_IDCONTR_MIRROR = "Partenon-IDCONTR_MIRROR";
    public static final String KEYWORD_CANCELACKED = "Partenon-CANCELACKED";
    public static final String KEYWORD_PlatformTradeID = "PlatformTradeID";
    final public static String KEYWORD_BLOCK_TRADE = "BlockTradeId";
    final public static String KEYWORD_NOVATION_FROM = "NovationFrom";
    final public static String KEYWORD_NOVATION_TO = "NovationTo";
    final public static String KEYWORD_CUSTOM_FORWARD_RISK_TRANSFER_SETTING = "CustomForwardRiskTransferSetting";
    final public static String KEYWORD_CUSTOM_TRANSFER_SETTING = "CustomTransferSetting";
    final public static String KEYWORD_CUSTOM_SPLIT_SETTING = "CustomSplitSetting";
    final public static String KEYWORD_NEAR_LEG_PRECISION = "NearLegPrecision";
    final public static String KEYWORD_FAR_LEG_PRECISION = "FarLegPrecision";
    public static final String KEYWORD_MXNDF_TYPE = "MXNDFType";
    public static final String KEYWORD_CANCELLATION_DATE = "CancellationDate";
    public static final String KEYWORD_NDF_FIXED_DATE = "NDFFixedDate";
    public static final String KEYWORD_NDF_RERESET = "NDFReReset";
    public static final String KEYWORD_TRANSFER_DATE = "TransferDate";
    public static final String KEYWORD_TRANSFER_DATE_FORMAT = "MM-dd-yyyy";
    public static final String KEYWORD_MX_DEAL_ID = "MxDealID";
    public static final String KEYWORD_LATE_TRADE_MUREX = "LateTradeMurex";
    public static final String KEYWORD_MX_INTERNAL_MATCHED_TRADE = "MXInternalMatchedTrade";
    public static final String KEYWORD_FLOW_RATE_RESET_DATE = "FlowRateResetDate";
    public final static String KEYWORD_SANT_ROLLEDOVER_FROM = "SantRolledOverFrom";
    public static final String KEYWORD_SANT_ROLLEDOVER_TO = "SantRolledOverTo";
    public static final String KEYWORD_RIG = "RIG";
    public static final String KEYWORD_ROLLOVER = "RollOver";
    public static final String KEYWORD_TERMINATED_FROM = "TerminatedFrom";
    public static final String KEYWORD_CODIFIER_MUREX = "CODIFIER-Murex";
    public static final String KEYWORD_UNIQUE_ID = "UniqueID";
    public static final String KEYWORD_TRENSFER_FROM_UNIQUE_ID = "TransferFromUniqueID";
    /** The Constant TRANSFER_ATTRIBUTE_UETR.. */
    public static final String TRANSFER_ATTRIBUTE_UETR = "UETR";

    // Domains' names
    public static final String TRADE_DOMAIN = "tradeKeyword";
    public static String CRE_SENT_STATUS_DOMAIN = "creSentStatus";
    public static String ACC_EVENT_TYPE_DOMAIN = "accEventType";
    public static final String DGO_NETTING_DOMAIN = "DGONetting";
    public static final String TRANSFER_STATUS_DOMAIN = "transferStatus";
    public static final String CURRENCY_DOMAIN = "currency";
    public static final String PRODUCT_TYPE_DOMAIN = "productType";
    public static final String ACC_EVENT_TYPE = "accEventType";
    // Domain values
    public static final String EXTERNAL_REFERENCE_VALUE = "ExternalReference";
    public static final String LEGAL_ENTITY_VALUE = "LegalEntity";
    public static final String PORTFOL_VALUE = "PORKeywordConstantsUtilTFOL";
    public static final String MONEDA_VALUE = "MONEDA";
    public static final String MOCOMPRA_VALUE = "MOCOMPRA";
    public static final String MOVENTA_VALUE = "MOVENTA";
    public static final String IDCENT_DEST_VALUE = "IDCENT_DEST";
    public static final String IDCENT_VALUE = "IDCENT";
    public static final String TRADE_ID_VALUE = "TRADE_ID";
    public static final String SUCURSAL_DESTINO_VALUE = "SUCURSAL_DESTINO";

    public static final String ACC_MATURED_EVENT_TYPE = "MATURED";
    public static final String ACC_BALANCE_EVENT_TYPE = "BALANCE";
    public static final String ACC_OBBCST_EVENT_TYPE = "OBB_CST";
    public static final String ACC_OBB_EVENT_TYPE = "OBB_EOD";
    public static final String ACC_MM_EOD_TYPE = "MM_EOD";
    public static final String ACC_MM_BALANCE_TYPE = "MM_BALANCE";

    // Subproducts
    public static final String INTEREST_BEARING = "INTEREST_BEARING";
    public static final String COMMISSIONS = "COMMISSIONS";
    public static final String TRANSFER = "TRANSFER";
    public static final String CLAIM = "CLAIM";
    
    /** The Constant TRADE_KEYWORD_STATUS_MATCHED. */
    public static final String TRADE_KEYWORD_STATUS_MATCHED = "Matched";

    /** The Constant TRADE_KEYWORD_STATUS_UNMATCHED. */
    public static final String TRADE_KEYWORD_STATUS_UNMATCHED = "Unmatched";
    
    /** The Constant TRADE_KEYWORD_STATUS_MISMATCHED. */
    public static final String TRADE_KEYWORD_STATUS_MISMATCHED = "Mismatched";
    
    /** The Constant KEYWORD_BLOCK_TRADE_ID. */
    public static final String KEYWORD_BLOCK_TRADE_ID = "BlockTradeId";
    
    /** The Constant KEYWORD_USI_TRADE_ID. */
    public static final String KEYWORD_USI_TRADE_ID = "USITradeId";
    
    /** The Constant KEYWORD_UTI_TRADE_ID. */
    public static final String KEYWORD_UTI_TRADE_ID = "UTITradeId";

    // CAL_DODD 041
    /** The Constant KEYWORD_PRIOR_USI_TRADE_ID. */
    public static final String KEYWORD_PRIOR_USI_TRADE_ID = "PriorUSITradeId";

    /** The Constant KEYWORD_PRIOR_UTI_TRADE_ID. */
    public static final String KEYWORD_PRIOR_UTI_TRADE_ID = "PriorUTITradeId";

    //EMIR BRS
    /** The Constant TRADE_KEYWORD_NOVATION_FROM. */
    public static final String TRADE_KEYWORD_NOVATION_TO = "NovationTo";

    /** The Constant TRADE_KEYWORD_MATCHING_STATUS. */
    public static final String TRADE_KEYWORD_MATCHING_STATUS = "MatchingStatus";

    /** The Constant LE_ATTRIBUTE_LEI. */
    public static final String LE_ATTRIBUTE_LEI = "LEI";

    /** The Constant LE_ATTRIBUTE_EMIR_CPTY_CLASS. */
    public static final String LE_ATTRIBUTE_EMIR_CPTY_CLASS = "EMIR_CPTY_CLASS";
    /**
     * constant to specify if is trading or hedgin
     */
    public static final String LE_ATTRIBUTE_TRADING_HEDGING = "TRADING_HEDGING";

    /** The Constant TRADE_KEYWORD_PREVIOUSLEIVALUE. */
    public static final String TRADE_KEYWORD_PREVIOUSLEIVALUE = "PreviousLEIValue";

    /** The Constant LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR. */
    public static final String LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR = "EMIR_CORPORATE_SECTOR";

}

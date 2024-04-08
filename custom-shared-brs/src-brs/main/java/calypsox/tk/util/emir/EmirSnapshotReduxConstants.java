package calypsox.tk.util.emir;

import java.util.Arrays;
import java.util.List;

public class EmirSnapshotReduxConstants {

    /** Old value Column */
    public static final String SUBMITTEDVALUE_VAL = "SUBMITTEDVALUE_VAL";

    public static final String SCXTXX = "SCXTXX";
    public static final String NA = "NA";
    public static final String ISIN = "ISIN";
    public static final String BASKET = "Basket";
    public static final String NUMERIC_ONE = "1";


    public static final String PRICING_ENV_DIRTY_PRICE = "DirtyPrice";

    public static final String ATTR_MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    public static final String ATTR_EMIR_COLLATERAL_VALUE = "EMIR_COLLATERAL_VALUE";

    public static final String CREDIT_TOTAL_RETURN_SWAP  = "Credit:TotalReturnSwap";

    public static final String SNAPSHOT_TYPE = "SNP";
    public static final String DOT_CHAR_SPLIT = "\\.";
    public static final String DOT_CHAR = ".";
    public static final String DELIMITER = ",";
    public static final String REGEX_ONLY_NUMBERS = "\\d+";
    public static final String REGEX_WHITESPACES = "\\s+";
    public static final int MAX_SIZE = 1000;

    public static final String EMPTY_SPACE = "";
    public static final String CADENA_NULL = "null";

    public static final String ACTION_N = "N";
    public static final String ACTION_E = "E";
    public static final String ACTION_C = "C";
    public static final String ACTION_NEW = "New";

    public static final String YES = "Y";

    public static final String MAY_TRUE = "True";
    public static final String MAY_FALSE = "False";

    public static final String TIMEZONE_UTC = "UTC";
    public static final String UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String ONLY_DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String SYSTEM_CALENDAR = "SYSTEM";
    public static final String MADRID_TIMEZONE = "Europe/Madrid";

    public static final String AUDIT_STATUS = "_status";

    // Trade Keyword
    public static final String TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT = "EMIR_LIFECYCLEEVENT";
    public static final String TRADE_KEYWORD_EMIR_ACTION = "EMIR_ACTION";
    public static final String TRADE_KEYWORD_EMIR_ACTION_TYPE = "EMIR_ACTIONTYPE";
    public static final String TRADE_KEYWORD_EMIR_TRANSTYPE = "EMIR_TRANSTYPE";
    public static final String TRADE_KEYWORD_CONFIRMATION_DATE_TIME = "EMIR_CONFIRMATION_DATETIME";

    public static final String TRADE_KEYWORD_EMIR_MIC_CODE = "EMIR_MIC_CODE";
    public static final String TRADE_KEYWORD_EMIR_PRODUCT_TOTV = "EMIR_PRODUCT_TOTV";
    public static final String TRADE_KEYWORD_EMIR_IDENTIFICATION_ISIN = "EMIR_IDENTIFICATION_ISIN";
    public static final String TRADE_KEYWORD_UTI_VALUE = "UTIValue";
    public static final String TRADE_KEYWORD_MX_LAST_EVENT = "MxLastEvent";
    public static final String TRADE_KEYWORD_MUREX_TRANSFER_TO = "MurexTransferTo";


    // FO Events
    public static final String MX_EVENT_EARLY_TERMINATION_TOTAL_RETURN = "mxContractEventRatesIEARLY_TERMINATION_TOTAL_RETURN";
    public static final String MX_EVENT_ICANCEL = "mxContractEventICANCEL";
    public static final String MX_EVENT_ICANCEL_REISSUE = "mxContractEventICANCEL_REISSUE";
    public static final String MX_EVENT_IMODIFY_UDF = "mxContractEventIMODIFY_UDF";
    public static final String MX_EVENT_IFIXING = "mxContractEventIFIXING";
    public static final String MX_EVENT_IADDITIONAL_FLOW_AMENDMENT = "mxContractEventRatesIADDITIONAL_FLOW_AMENDMENT";
    public static final String MX_EVENT_ISHARES_MODIFICATION = "mxContractEventRatesISHARES_MODIFICATION";
    public static final String MX_EVENT_IRATE_AMENDMENT = "mxContractEventRatesIRATE_AMENDMENT";
    public static final String MX_EVENT_IRESTRUCTURE  =  "mxContractEventIRESTRUCTURE";
    public static final String MX_EVENT_IPORTFOLIO_MODIFICATION = "mxContractEventIPORTFOLIO_MODIFICATION";
    public static final String MX_EVENT_IPORTFOLIO_ASSIGNMENT = "mxContractEventIPORTFOLIO_ASSIGNMENT";
    public static final String MX_EVENT_ICOUNTERPART_AMENDMENT = "mxContractEventICOUNTERPART_AMENDMENT";
    public static final String MX_EVENT_IMATURIRY_EXTENSION = "mxContractEventIMATURITY_EXTENSION";



    // Trade keyword used for controlling changes in LEI
    public static final String TRADE_KEYWORD_EMIR_LEI_VALUE = "EMIR_LEI_VALUE";

    // Domain Values
    public static final String DV_LE_ATTRIBUTE_TYPE = "leAttributeType";
    public static final String DV_EMIR_SOVEREIGN_BOND = "EMIR_Sovereing_Bond";
    public static final String DV_EMIR_CLEARING_HOUSE = "EMIR_Clearing_House";



    // LE Attribute
    public static final String LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR_NF = "EMIR_CORPORATE_SECTOR_NF";
    public static final String LE_ATTRIBUTE_EMIR_FULL_DELEG = "EMIR_FULL_DELEG";
    public static final String LE_ATTRIBUTE_GESTORA = "GESTORA";
    public static final String LE_ATTRIBUTE_INTRAGRUPO = "INTRAGRUPO";
    public static final String LE_ATTRIBUTE_REPORTING_DELEGATION_MODEL = "REPORTING_DELEGATION_MODEL";
    public static final String LE_ATTRIBUTE_OWNERSHIP_COUNTRY = "Ownership_Country";
    public static final String LE_EMIR_THIPARTY =  "EMIR_ThirdParty";


    // Literal
    public static final String LITERAL_C = "C";
    public static final String LITERAL_F = "F";
    public static final String LITERAL_I = "I";
    public static final String LITERAL_L = "L";
    public static final String LITERAL_N = "N";
    public static final String LITERAL_S = "S";
    public static final String LITERAL_T = "T";
    public static final String LITERAL_U = "U";


    public static final String SELL = "S";
    public static final String BUY = "B";
    public static final String SELLER = "Seller";
    public static final String BUYER = "Buyer";

    public static final String CFI = "CFI";
    public static final String INDEPENDENT = "INDEPENDENT";
    public static final String INTERNAL = "INTERNAL";
    public static final String ES = "ES";
    public static final String ESMA = "ESMA";
    public static final String GB = "GB";
    public static final String FCA = "FCA";
    public static final String UNITED_KINGDOM = "United Kingdom";
    public static final String ESMA_N = "ESMA-N";
    public static final String ESMA_X = "ESMA-X";
    public static final String FW = "FW";
    public static final String CD = "CD";
    public static final String SW = "SW";

    public static final String OT = "OT";


    public static final String ISDA = "ISDA";
    public static final String LEI = "LEI";
    public static final String UNCOLLATERALIZED = "UNCOLLATERALIZED";

    public static final String XOFF = "XOFF";
    public static final String XXXX = "XXXX";

    public static final String NON_EEA = "nonEEA";
    public static final String EEA = "EEA";
    public static final String EEA_COUNTRIES = "EEACountries";

    public static final String ACTIVITY = "Activity";
    public static final String CREDIT = "Credit";
    public static final String TRADING = "Trading";
    public static final String HEDGING = "Hedging";
    public static final String EXECUTION_VENUE_PREFIX = "FREEFORMATTEXT";
    public static final String OFF_FACILITY = "Off-facility";
    public static final String SNAPSHOT_MESSAGE_TYPE = "Snapshot";
    public static final String PHYSICAL = "Physical";
    public static final String CASH = "Cash";
    public static final String UNITS = "Units";
    public static final String PERCENTAGE = "Percentage";

    public static final String TRADING_CAPACITY_PRINCIPAL = "Principal";
    public static final String TRADING_CAPACITY_AGENT = "Agent";
    public static final String CREDIT_INSTITUTION = "CreditInstitution";
    public static final String NOT_CONFIRMED = "NotConfirmed";
    public static final String NON_ELECTRONIC = "Non-Electronic";
    public static final String ELECTRONIC = "Electronic";
    public static final String NOT_APPLICABLE = "NotApplicable";


    // Audit fields
    public static final String ENDS_KEYWORD_UTI = "#UTITradeId";
    public static final String ENDS_KEYWORD_UTI_FAR = "#UTITradeIdFar";
    public static final String ENDS_KEYWORD_UTI_NEAR = "#UTITradeIdNear";
    public static final String ENDS_KEYWORD_UTI_REFERENCE = "#UTI_REFERENCE";


    public static final String ENDS_KEYWORD_TEMP_UTI = "#TempUTITradeId";
    public static final String ENDS_KEYWORD_TEMP_UTI_FAR = "#TempUTITradeIdFar";
    public static final String ENDS_KEYWORD_TEMP_UTI_NEAR = "#TempUTITradeIdNear";

    public static final String ENDS_KEYWORD_MATCHING_STATUS = "#MatchingStatus";

    public static final String ENDS_KEYWORD_CONFIRMATION_DATETIME = "#ConfirmationDateTime";

    public static final String ENDS_KEYWORD_EMIR_IDENTIFICATION_ISIN = "#EMIR_IDENTIFICATION_ISIN";
    public static final String ENDS_KEYWORD_EMIR_MIC_CODE = "#EMIR_MIC_CODE";
    public static final String ENDS_KEYWORD_EMIR_PRODUCT_TOTV = "#EMIR_PRODUCT_TOTV";

    public static final String ENDS_KEYWORD_MX_LAST_EVENT = "#MxLastEvent";

    public static final String AUDIT_FIELD_PRODUCT_SWAPLEG_AMORT_SCHEDULE = "Product.SWAPLEG.__amortSchedule";
    public static final String AUDIT_FIELD_PRODUCT_SWAPLEG_AMORT_SCHEDULE_SOURCE = "Product.SWAPLEG.__amortSchedule";

    public static final List AUDIT_FIELDS_AMORTIZATION_SCHEDULE = Arrays.asList(
            AUDIT_FIELD_PRODUCT_SWAPLEG_AMORT_SCHEDULE,
            AUDIT_FIELD_PRODUCT_SWAPLEG_AMORT_SCHEDULE_SOURCE);



    // New attribute Emir Cpty Class
    public static final String FINANCIAL_COUNTERPARTY = "FC";
    public static final String SMALL_FINANCIAL_COUNTERPARTY = "SFC";
    public static final String NON_FINANCIAL_COUNTERPARTY_PLUS = "NFC+";
    public static final String NON_FINANCIAL_COUNTERPARTY = "NFC";
    // New attribute Emir Cpty Class - End

    public static final String ENDS_KEYWORD_PREVIOUSLEIVALUE = "#PreviousLEIValue";
    /** The Constant TRADE_KEYWORD_PARTENON_ID */
    public static final String TRADE_KEYWORD_PARTENON_ID = "PartenonGDisponibleID";


    public static final String TRADE_KEYWORD_MUREX_TRADE_ID = "MurexTradeID";
    public static final String SWIFTBIC = "SWIFTBIC";
    public static final String LE_ATTR_EMIR_ISSUER_RED_CODE = "EMIR_ISSUER_RED_CODE";
    public static final String DTCCEU = "DTCCEU";
    public static final String APPLICABLE = "Applicable" ;
    public static final String DV_EMIR_FUND_PAYMENT_FREQUENCY = "EMIR_FundPaymentFrecuency";
    public static final String DV_EMIR_MASTER_DOCUMENT_DATE = "EMIR_MasterDocumentDate";
   public static final String TRADE_KEYWORD_PLATFORM = "Platform";
    public static final String TRADE_KEYWORD_UTI_REFERENCE = "UTI_REFERENCE" ;
    public static final String TRADE_KEYWORD_MUREX_TRANSFER_FROM = "MurexTransferFrom";

    public static final String SNDB = "SNDB";
    public static final String STANDARD_TERMS_SUPPLEMENT_TYPE = "StandardTermsSupplementType";
    public static final String CREDIT_DERIVATIVES_PHYSYCAL_SETTLEMENT_MATRIX = "CreditDerivativesPhysicalSettlementMatrix";


    public static final String LEG_SINGLE_ASSET = "SingleAsset";
    public static final String LEG_MANAGED = "Managed";
    public static final String FREEFORMATTEXT = "FREEFORMATTEXT";

    public static final String DV_EMIR_CALCULATION_BASIS = "EMIR_CalculationBasis";

    public static final String TRADE_KEYWORD_TEMP_UTI_TRADE_ID = "TempUTITradeId";
}

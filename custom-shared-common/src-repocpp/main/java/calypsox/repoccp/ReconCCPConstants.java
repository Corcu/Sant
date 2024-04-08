package calypsox.repoccp;

/**
 * Constant text strings for using in all Recon CCP related classes. <br>
 * Contains all task type names, trade keywords, transfer attributes and workflow actions
 *
 * @author x854118
 */
public class ReconCCPConstants {
    /**
     * Task station exceptionType, means that external id did not match with any of the calypso id
     */
    public static final String EXCEPTION_MISSING_TRADE_RECON_CCP = "EX_MISSING_TRADE_RECON_CCP";
    /**
     * Task station exceptionType, means that coupon identifier is Y
     */
    public static final String EXCEPTION_COUPON_IDENTIFIER_Y = "EX_COUPON_IDENTIFIER_Y";
    /**
     * Task station exceptionType, means that settlementReferenceInstructed from file did not match with any of the calypso transfers
     */
    public static final String EXCEPTION_MISSING_SETTLEMENT_RECON_CCP = "EX_MISSING_SETTLEMENT_RECON_CCP";
    /**
     * Task station exceptionType, means that external id matched with calypso id but other fields are not the same
     */
    public static final String EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP = "EX_CALYPSO_TRADE_UNMATCHED_RECON_CCP";
    /**
     * Task station exceptionType, Calypso trades susceptible to recon did not match with any of the external trades
     */
    public static final String EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP = "EX_FIELDS_NOT_MATCHING_RECON_CCP";
    /**
     * Task station exceptionType, means that no Calypso transfer was matched with any file transfer
     */
    public static final String EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP = "EX_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP";
    /**
     * Task station exceptionType, Calypso trades susceptible to recon did not match with any of the external trades
     */
    public static final String EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING = "EX_FIELDS_NOT_MATCHING_RECON_CCP_NETTING";
    /**
     * Task station exceptionType, Calypso settlement transfers susceptible to recon did not match fields
     */
    public static final String EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_SETTLEMENTS = "EX_FIELDS_NOT_MATCHING_RECON_CCP_SETTLEMENTS";
    /**
     * Trade keyword name OR Transfer attribute for storing the Recon OK or KO for trades or transfers
     */
    public static final String TRADE_KEYWORD_RECON = "Recon";
    /**
     * Transfer attribute for storing result of settlements recon
     */
    public static final String XFER_ATTR_RECON_SETTLEMENTS_EOD = "Recon_Settlements_EOD";
    /**
     * String value for recon OK
     */
    public static final String RECON_OK = "OK";
    /**
     * String value for recon KO
     */
    public static final String RECON_KO = "KO";
    /**
     * String value for Action that amends Trades saving recon attributes
     */
    public static final String WF_AMEND_RECON = "AMEND_RECON";
    /**
     * Trade Keyword BuyerSellerReference
     */
    public static final String TRADE_KWD_BUYER_SELLER_REF = "BuyerSellerReference";
    /**
     * Transfer Attribute SettlementReferenceInstructed
     */
    public static final String XFER_ATTR_SETTLEMENT_REF_INST = "SettlementReferenceInstructed";

    /**
     * Transfer Attribute SettlementReferenceInstructed
     */
    public static final String XFER_ATTR_SETTLEMENT_REF_INST_2 = "SettlementReferenceInstructed2";

    /**
     * Transfer Attribute TradeSourceName
     */
    public static final String XFER_ATTR_TRADE_SOURCE = "tradeSourceName";
    /**
     * Static Data Filter for filtering NETTING_GROSS transfers
     */
    public static final String NETTING_GROSS_SDF = "RECON_CCP_NETTING_SAMEDAY";
    /**
     * Static Data Filter for filtering NETTING_GROSS transfers
     */
    public static final String NETTING_MTINEXTDAY_SDF = "RECON_CCP_NETTING_NEXTDAY";
    /**
     * Workflow action for moving transfer to settled in recon
     */
    public static final String WF_XFER_SETTLE = "SETTLE_CCP";

    /**
     * Tolerance for Start Cash and End Cash
     */
    public static final String TOLERANCE_CCY = "EUR";

    public static final double TOLERANCE = 25.0;
    /**
     * Order by name asc
     */
    public static final String ORDER_BY_NAME = "Order by name";
    /**
     * Order by date
     */
    public static final String ORDER_BY_DATE = "Order by date";

    /**
     * Trade keyword Mx Electplatf
     */
    public static final String TRADE_KWD_MX_ELECTPLTF = "Mx Electplatf";

    /**
     * Transfer attribute save the origin platform to index MT54X
     */
    public static final String XFER_ATTR_SETTLEMENT_REF_INST_PLATFORM = "SRIPlatform";

    /**
     * The domain name UpdateSettlementReferenceInstructed contains pairs SDF/KW to paste SRI on transfer
     */
    public static final String UPDATE_XFER_SRI_DOMAIN_NAME = "UpdateSettlementReferenceInstructed";
    /**
     * Actual cash amount cleared by CCP
     */

    public static final String XFER_ATTR_CASH_AMOUNT_INSTRUCTED = "CashAmountInstructed";

    public static final String XFER_ATTR_RECON_RESULT = TRADE_KEYWORD_RECON;
}

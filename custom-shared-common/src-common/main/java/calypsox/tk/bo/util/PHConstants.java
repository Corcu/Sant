package calypsox.tk.bo.util;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SWIFTFormatter;
import com.calypso.tk.core.Action;

public class PHConstants {

    // NumberIndividualTransactions
    public static final int NUMBER_TXS = 1;

    // Provider
    public static final String PROVIDER_DEFAULT = "Calypso-STC";

    // EndToEnd default
    public static final String NOT_PROVIDED = "NOTPROVIDED";

    // Settlement Priority
    public static final String SETTLEMENT_PRIORITY_HIGH = "HIGH";
    public static final String SETTLEMENT_PRIORITY_NORM = "NORM";

    // Payment Info
    public static final String CRED = "CRED";
    public static final String SSTD = "SSTD";

    // Interbank Settle Date
    public static final String INTERBANK_SETT_DATE_FORMAT = "yyyy-MM-dd";

    // PaymentsHub
    public static final String PAYMENTS_HUB_SYSTEM = "PAYMENTS_HUB";

    // TransferType
    public static final String XFER_TYPE_RECEIVE = "RECEIVE";
    public static final String XFER_TYPE_PAY = "PAY";

    public static final String XFER_ATTR_PH_SETTLEMENT_STATUS = "PHSettlementStatus";
    public static final String XFER_ATTR_PH_CALLBACK_TIMESTAMP = "PHCallbackTimestamp";
    public static final String XFER_ATTR_PH_MESSAGE_TRN = "MessageTrn";


    public static final String PAYMENTHUB_PAYMENTMSG_TYPE = "PAYMENTHUB_PAYMENTMSG";
    public static final String PAYMENTHUB_RECEIPTMSG_TYPE = "PAYMENTHUB_RECEIPTMSG";
    public static final String PAYMENTHUB_INPUT_MSG_TYPE = "PAYMENTHUB_INPUT";

    // PaymentHub Message Templates
    public static final String MESSAGE_PH_FICCT = "PH-FICCT";
    public static final String MESSAGE_PH_FICT = "PH-FICT";
    public static final String MESSAGE_PH_FICTCOV = "PH-FICTCOV";
    public static final String MESSAGE_PH_NTR = "PH-NTR";
    public static final String MESSAGE_PH_NO_OUT_MESSAGE = "PH-NoOutMessage";

    // Legacy Message Templates
    public static final String LEGACY_TEMPLATE_MT103 = "MT103";
    public static final String LEGACY_TEMPLATE_MT202 = "MT202";
    public static final String LEGACY_TEMPLATE_MT202COV = "MT202COV";
    public static final String LEGACY_TEMPLATE_MT210 = "MT210";
    public static final String LEGACY_FORMAT_TYPE = SWIFTFormatter.SWIFT.toUpperCase();
    public static final String LEGACY_MESSAGE_TYPE = BOMessage.PAYMENTMSG;
    public static final String LEGACY_ADDRESS_METHOD = SWIFTFormatter.SWIFT.toUpperCase();
    public static final String LEGACY_GATEWAY = "GestorSTP";

    public static final String LEGACY_TEMPLATE_MT192 = "MT192PHLegacy";
    public static final String LEGACY_TEMPLATE_MT292 = "MT292PHLegacy";

    public static final String MSG_ATTR_PH_RESPONSE_STATUS = "PH_ResponseStatus";
    public static final String MSG_ATTR_PH_RESPONSE_MESSAGE = "PH_ResponseMessage";
    public static final String MSG_ATTR_PH_PAY_SYST_RESP_HEADER = "PH_PaymentSystemResponseHeader";
    public static final String MSG_ATTR_PH_CALL_SERVICE_DATETIME = "PH_CallServiceDatetime";
    public static final String MSG_ATTR_PH_CALLBACK_MESSAGEID = "PHCallbackMessageId";
    public static final String MSG_ATTR_PH_PAYMENT_SYSTEM_RESPONSE_HEADER = "PH_PaymentSystemResponseHeader";
    public static final String MSG_ATTRIBUTE_COVER_MESSAGE = "HasCoverMessage";

    public static final String ACTION_PH_WAIT_RETRY = "PH_WAIT_RETRY";
    public static final String ACTION_PH_CONNECTION_FAIL = "PH_CONN_FAIL";
    public static final String ACTION_PH_ACK = "PH_ACK";
    public static final String ACTION_PH_NACK = "PH_NACK";
    public static final Action MESSAGE_ACTION_SAVE = Action.valueOf("SAVE");

    /** The Constant TRADE or TRANSFER KEYWORD_13CTIME_INDICATION. */
    public static final String KEYWORD_13CTIME_INDICATION = "13CTimeIndication";

    /** The Constant SDI_ATTRIBUTE_BENEFICIARY. */
    public static final String SDI_ATTRIBUTE_BENEFICIARY = "Beneficiary";
    // Charge to Bear
    public static final String CHARGE_TO_BEAR = "ChargesToBear";
    public static final String CHARGE_TO_BEAR_DEFAULT = "DEBT";
    /** The Constant SDI_ATTRIBUTE_DETAILS_OF_CHARGES. */
    public static final String SDI_ATTRIBUTE_DETAILS_OF_CHARGES = "Details_Of_Charges";
    /** The Constant SDI_ATTRIBUTE_INTERMEDIARY. */
    public static final String SDI_ATTRIBUTE_INTERMEDIARY = "Intermediary";
    /** The Constant SDI_ATTRIBUTE_AGENT. */
    public static final String SDI_ATTRIBUTE_AGENT = "Agent";
    /** The Constant SDI_ATTRIBUTE_SEND_MSG_RECEIPT. */
    public static final String SDI_ATTRIBUTE_SEND_MSG_RECEIPT = "SendMsgForReceipt";
    public static final String SDI_ATTRIBUTE_IS_MUTUACTIVO = "IsMutuactivo";

    public static final String SDF_EXCEPTION_NAMES = "'SANT_PHEXCEPTION_%'";
    public static final String COV_SDF_EXCEPTION_NAMES = "'SANT_PHEXCEPTIONS_COV_%'";

    public static final String FORMAT_TYPE_PAYMENTHUB = "PAYMENTHUB";

    public static final String ADDRESS_METHOD_NONE = "NONE";
    public static final String FORMAT_TYPE_JSON = "JSON";
    public static final String TEMPLATE_NAME_PAYMENTHUB_INPUT = "PaymentHUBInput";
    public static final String GATEWAY_NONE = "NONE";

    public static final String TRADE_KEYWORD_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";

    public static final String SETTLEMENT_METHOD_DIRECT = "Direct";
    public static final String SETTLEMENT_METHOD_TARGET2 = "TARGET2";
    public static final String SETTLEMENT_METHOD_CHAPS = "CHAPS";

    // EventType
    public static final String EVENT_TYPE_N = "N";
    public static final String EVENT_TYPE_C = "C";

    // ConceptType
    public static final String CONCEPT_TYPE_P = "P";
    public static final String CONCEPT_TYPE_C = "C";

    public static final String SUFIX_STC = "_STC";
    public static final String PREFIX_CANCELLATION = "canc-";

    public static final String PATTERN_TO_CHANGE = "ZZZZZZZZZZ";


    /**
     * BOTransfer attribute internal PaymentHub SDI id
     */
    public static final String PH_INTERNAL_SDI_ID = "PH Internal SDI id";

    /**
     * BOTransfer attribute external PaymentHub SDI id
     */
    public static final String PH_EXTERNAL_SDI_ID = "PH External SDI id";

    /**
     * BOTransfer attribute PaymentHub Settlement Method
     */
    public static final String PH_SETTLEMENT_METHOD = "PH Settlement Method";

    /**
     * Attribute of the message that is reported during the generation
     * of the PH msg and indicates that the origin is PH in the Swift generation.
     */
    public static final String PH_ORIGIN = "PH Origin";
}

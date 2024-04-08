package calypsox.tk.bo;


import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.swift.SantanderSwiftUtil;
import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubFormatterUtil;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.AdviceDocumentBuilder;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.util.DisplayInBrowser;
import com.santander.restservices.paymentshub.model.submodel.*;
import com.santander.restservices.paymentshub.model.submodel.Account;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class PAYMENTHUBMSGMessageFormatter extends MessageFormatter {


    protected static final String FILENAME_DATE_FORMAT = "yyyyMMddHHmmss";
    protected static final String REGEX_LINE_SEPARATOR = "(?:\\n|\\r)";
    protected static final int NARRATIVES_SIZE = 10;
    protected static final String LE_ATTRIBUTE_IDCENT = "Partenon-IDCENT";
    protected static final String LE_ATTRIBUTE_IDEMPR = "Partenon-IDEMPR";
    protected static final String LE_ATTRIBUTE_IDCONTR = "Partenon-IDCONTR";
    protected static final String DV_NETTED_TRANSFER = "NettedTransfer";
    protected static final String SDI_ATTRIBUTE_PH_NO_MESSAGE_FOR_GSTP = "PH_NoMessageForGSTP";
    protected static final String LE_ATTRIBUTE_PH_BRANCH_IDENTIFIER = "PH_BranchIdentifier";
    protected static final String FORMAT_TYPE_PAYMENTHUB = "PAYMENTHUB";
    protected static final String XFER_TYPE_RECEIVE = "RECEIVE";
    protected static final String XFER_TYPE_PAY = "PAY";
    protected static final String CHAPS = "CHAPS";
    protected static final String MESSAGE_NONREF = "NONREF";
    protected static final String PO_BDSD = "BDSD";
    // Cancellation prefix
    public static final String PREFIX_CANCELLATION = "canc-";
    // The Constant TRADE or TRANSFER KEYWORD_13CTIME_INDICATION
    public static final String KEYWORD_13CTIME_INDICATION = "13CTimeIndication";
    // The Constant SDI_ATTRIBUTE_INTERMEDIARY
    public static final String SDI_ATTRIBUTE_INTERMEDIARY = "Intermediary";
    // The Constant SDI_ATTRIBUTE_AGENT
    public static final String SDI_ATTRIBUTE_AGENT = "Agent";


    public PAYMENTHUBMSGMessageFormatter() {
    }


    @Override
    public AdviceDocument generate(final PricingEnv env, final BOMessage boMessage, boolean newDocument, final DSConnection dsCon) throws MessageFormatException {
        // Check if the AdviceDocument exists.
        AdviceDocument doc = null;
        if (!newDocument || boMessage.getExternalB()) {
            try {
                doc = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(boMessage.getLongId(), new JDatetime());
            } catch (CalypsoServiceException exc) {
                Log.error(this,exc.getCause());
            }
        }
        PaymentsHubMessage phMessage;
        if (doc != null) {
            if (doc.getDocument() != null) {
                final StringBuffer docBuilder = doc.getDocument();
                phMessage = new PaymentsHubMessage(boMessage);
                if (!phMessage.parsePaymentsHubText(docBuilder.toString())) {
                    throw new MessageFormatException("Could not parse PaymentsHub message for BOMessage: " + boMessage.getLongId());
                } else {
                    doc.setUserData(phMessage);
                    return doc;
                }
            } else {
                throw new MessageFormatException("Could not parse database PaymentsHub message for BOMessage: " + boMessage.getLongId());
            }
        } else if (boMessage.getExternalB()) {
            return null;
        } else {
            AdviceDocumentBuilder adviceDocBuilder;
            if (DataServer._isDataServer) {
                adviceDocBuilder = AdviceDocumentBuilder.create(boMessage);
            } else {
                adviceDocBuilder = AdviceDocumentBuilder.create(boMessage).datetime(dsCon.getServerCurrentDatetime());
            }
            // Build PaymentsHubMessage
            phMessage = generatePaymentsHub(env, boMessage, newDocument, dsCon);
            if (phMessage != null) {
                final String phTextPretty = phMessage.getTexMessageWithDefaultPrettyPrinter(); // as Pretty format
                adviceDocBuilder.userData(phMessage);
                adviceDocBuilder.document(new StringBuffer(phTextPretty));
                adviceDocBuilder.characterEncoding(StandardCharsets.UTF_8.toString());
                adviceDocBuilder.userName(dsCon.getUser());
            } else {
                Log.error("PaymentsHub", new Throwable("No PaymentsHub message for BOMessage id = " + boMessage.getLongId()));
            }
            return adviceDocBuilder.build();
        }
    }


    @Override
    public final boolean display(final PricingEnv env, final BOMessage boMessage, final DSConnection dsCon, final AdviceDocument doc) throws MessageFormatException {
        PaymentsHubMessage phMessage = (PaymentsHubMessage) doc.getUserData();
        String messageText = null;
        String fileName = null;
        String ext = null;
        StringBuffer buff = null;
        // Get Message Text
        if (phMessage != null) {
            buff = PaymentsHubUtil.getAdviceDocumentAsStringBuffer(doc);
            // as Pretty format
            buff = (buff != null) ? buff : new StringBuffer(phMessage.getTexMessageWithDefaultPrettyPrinter());
        } else {
            buff = PaymentsHubUtil.getAdviceDocumentAsStringBuffer(doc);
            if (buff == null) {
                throw new MessageFormatException("Could not parse database PaymentsHub message for BOMessage: " + boMessage.getLongId());
            }
            // Get PaymentsHub Message
            phMessage = new PaymentsHubMessage(boMessage);
            if (!phMessage.parsePaymentsHubText(buff.toString())) {
                throw new MessageFormatException("Could not parse database PaymentsHub message for BOMessage: " + boMessage.getLongId());
            }
            doc.setUserData(phMessage);
        }
        if (Util.isEmpty(messageText) && buff != null) {
            messageText = buff.toString();
        }
        // Get FileName
        fileName = getTempFileName(boMessage);
        // Get Extension
        ext = getTempFileExtension(doc);
        // Display Temporary file
        if (messageText != null) {
            DisplayInBrowser.display(messageText, ext, fileName, StandardCharsets.UTF_8.toString());
        }
        return true;
    }


    /**
     * Get Temporary File Name.
     *
     * @param boMessage
     * @return
     */
    private String getTempFileName(final BOMessage boMessage) {
        final StringBuffer fileName = new StringBuffer();
        if (boMessage != null) {
            final String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(FILENAME_DATE_FORMAT));
            fileName.append(boMessage.getMessageType()).append("_");
            fileName.append(boMessage.getLongId()).append("_");
            fileName.append(now);
        }
        return fileName.toString();
    }


    /**
     * Get Temporary File Extension
     *
     * @param adviceDocument
     * @return
     */
    private String getTempFileExtension(final AdviceDocument adviceDocument) {
        String extension = EXTENSION_TXT.substring(1); // By default
        if (adviceDocument != null) {
            // Get Mime Type
            final MimeType mime = adviceDocument.getMimeType();
            extension = (mime != null) ? mime.getExtension() : extension;
        }
        return extension;
    }


    /**
     * Generate PaymentsHub Message
     *
     * @param env
     * @param boMessage
     * @param newDocument
     * @param dsCon
     * @return
     */
    private PaymentsHubMessage generatePaymentsHub(final PricingEnv env, final BOMessage boMessage, final boolean newDocument, final DSConnection dsCon) {
        PaymentsHubMessage phMessage = null;
        if (boMessage.getFormatType() != null && !boMessage.getFormatType().equals(FORMAT_TYPE_PAYMENTHUB)) {
            Log.error(LOG_CATEGORY, new Throwable("Not a PaymentsHub Message : (" + boMessage.getFormatType() + ") for message " + boMessage.getLongId()));
            return phMessage;
        } else {
            // Create PaymentsHub Message
            phMessage = new PaymentsHubMessage(boMessage);
            phMessage.setPricingEnv(env);
            phMessage.setFormatter(this);
            phMessage.build(newDocument);
        }
        return phMessage;
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Set of characteristics shared by all individual transactions included in the message.
     *
     * @return
     */
    public GroupHeader getGroupHeader(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // EventType
        final String eventType = parseSANT_EVENT_TYPE(boMessage, xfer, trade, dsCon);
        // ConceptType
        final String conceptType = parseSANT_CONCEPT_TYPE(boMessage, xfer, trade, dsCon);
        // MessageType
        final String messageType = parseSANT_MESSAGE_TYPE(boMessage, xfer, trade, dsCon);
        // NumberOfTransactions
        final String nbOfTxs = parseSANT_NUMBER_OF_TRANSACTIONS(boMessage, xfer, trade, dsCon);
        // Build GroupHeader
        return PaymentsHubFormatterUtil.buildGroupHeader(eventType, conceptType, messageType, nbOfTxs);
    }


    // ----------------------------------------------------------------- //
    //  // ------------------ CREDIT TRANSFER INSTRUCTION ------------------ //
    //  // ----------------------------------------------------------------- //


    /**
     * Get CreditTransferInstruction block.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public CreditTransferInstruction getCreditTransferInstruction(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Block Payment Identification - pmtId: instrId - pmtId: endToEndId - pmtId: uetr
        final PaymentIdentification paymentIdentification = getPaymentIdentification(boMessage, xfer, trade, dsCon);
        // Block Settlement Instruction - sttlmInf: clrSys: cd
        final SettlementInstruction settlementInstruction = getSettlementInstruction(boMessage, xfer, trade, dsCon);
        // Settlement Priority - sttlmPrty
        final String settlmPriority = parseSANT_SETTLEMENT_PRIORITY(boMessage, xfer, trade, dsCon);
        // Block ttlIntrBkSttlmAmt
        // AmountCurrency ttlIntrBkSttlmAmt = getTtlInterbankSettlementAmount(boMessage, xfer, trade,
        // dsCon);
        // Interbank Settle Date - intrBkSttlmDt
        final String intrBkSttlmDt = parseSANT_INTERBANK_SETTLE_DATE(boMessage, xfer, trade, dsCon);
        // Block Interbank Settle Amount Currency - intrBkSttlmAmt:value - intrBkSttlmAmt:ccy
        final AmountCurrency interBankSettleAmtCcy = getInterbankSettleAmount(boMessage, xfer, trade, dsCon);
        // Block Instructed Amount instdAmt
        // AmountCurrency instdAmt = getInstructedAmount(boMessage, xfer, trade, dsCon);
        // Exchange Rate xchgRate
        // Double xchgRate = parseSANT_EXCHANGE_RATE(boMessage, xfer, trade, dsCon);
        // Charge To Bear chrgBr
        final String chrgBr = parseSANT_CHARGE_TO_BEAR(boMessage, xfer, trade, dsCon);
        // Provides information on the charges to be paid by the charge bearer(s) related to the payment
        // transaction. chrgsInf
        // List<ChargesInformation> chrgsInf = getChargesInformation(boMessage, xfer, trade, dsCon);
        // Block Instructed For Next Agent
        final List<InstructedInfo> instrForNxtAgt = getInstructedForNextAgent(boMessage, xfer, trade, dsCon);
        // Remittance Info rmtInf
        final RemittanceInfo rmtInf = getRemittanceInfo(boMessage, xfer, trade, dsCon);
        // Block PaymentTypeInformation - pmtTpInf: lclInstrm: cd
        final PaymentTypeInformation pmtTpInf = getPaymentTypeInformation(boMessage, xfer, trade, dsCon);
        // Purpose purp
        // Purpose purp = getPurpose(boMessage, xfer, trade, dsCon);
        // Block Instruction For Creditor Agent instrForCdtrAgt
        // List<InstructionForCreditorAgent> instrForCdtrAgtList =
        // getInstructionForCreditorAgent(boMessage, xfer,
        // trade, dsCon);
        // Block RegulatoryReporting
        // List<RegulatoryReporting> rgltryRptg = getRegulatoryReporting(boMessage, xfer, trade, dsCon);
        // Block Settlement Time Request
        final SettlementTimeRequest sttlmTmReq = getSettlementTimeRequest(boMessage, xfer, trade, dsCon);
        // Block Instructed Agent - instdAgt: finInstnId :bicfi
        final FinancialInstitution instructedAgent = getInstructedAgent(boMessage, xfer, trade, dsCon);
        // Block Instructing Agent - instgAgt: finInstnId :bicfi
        final FinancialInstitution instructingAgent = getInstructingAgent(boMessage, xfer, trade, dsCon);
        // Block Debtor
        final Institution debtor = getDebtor(boMessage, xfer, trade, dsCon);
        // Block Debtor Account
        final PartyAccount debtorAcct = getDebtorAccount(boMessage, xfer, trade, dsCon);
        // Block Debtor Agent
        final FinancialInstitution debtorAgent = getDebtorAgent(boMessage, xfer, trade, dsCon);
        // Block Debtor Agent Account
        // PartyAccount dbtrAgtAcct = getDebtorAgentAccount(boMessage, xfer, trade, dsCon);
        // Block Creditor
        final Institution creditor = getCreditor(boMessage, xfer, trade, dsCon);
        // Block Creditor Account
        final PartyAccount creditorAcct = getCreditorAccount(boMessage, xfer, trade, dsCon);
        // Block Creditor Agent
        final FinancialInstitution creditorAgent = getCreditorAgent(boMessage, xfer, trade, dsCon);
        // Block Debtor Agent Account
        final PartyAccount cdtrAgtAcct = getCreditorAgentAccount(boMessage, xfer, trade, dsCon);
        // Block Senders Correspondent Agent
        final FinancialInstitution sndrsCorrespdntAgt = getSendersCorrespondentAgent(boMessage, xfer, trade, dsCon);
        // Block Senders Correspondent Agent Account
        final PartyAccount sndrsCorrespdntAgtAcct = getSendersCorrespondentAgentAccount(boMessage, xfer, trade, dsCon);
        // Block Receivers Correspondent Agent
        final FinancialInstitution rcvrsCorrespdntAgt = getReceiversCorrespondentAgent(boMessage, xfer, trade, dsCon);
        // Block Receivers Correspondent Agent Account
        final PartyAccount rcvrsCorrespdntAgtAcct = getReceiversCorrespondentAgentAccount(boMessage, xfer, trade, dsCon);
        // Block Third Reimbursement Agent
        // FinancialInstitution thrdRmbrsmntAgt = getThirdReimbursementAgent(boMessage, xfer, trade,
        // dsCon);
        // Block Third Reimbursement Agent Account
        // PartyAccount thrdRmbrsmntAgtAcct = getThirdReimbursementAgentAccount(boMessage, xfer, trade,
        // dsCon);
        // Block Intermediary Agent1 - intrmyAgt: finInstnId: bicfi
        final FinancialInstitution intermediaryAgent = getIntermediaryAgent(boMessage, xfer, trade, dsCon);
        // Block Intermediary Agent Account
        final PartyAccount intrmyAgtAcct = getIntermediaryAgentAccount(boMessage, xfer, trade, dsCon);
        // prvsInstgAgt1:
        // prvsInstgAgt1Acct:
        // prvsInstgAgt2:
        // prvsInstgAgt2Acct:
        // prvsInstgAgt3:
        // Block Intermediary Agent2
        // FinancialInstitution intrmyAgt2 = getIntermediaryAgent2(boMessage, xfer, trade, dsCon);
        // Block Intermediary Agent 2 Account
        // PartyAccount intrmyAgt2Acct = getIntermediaryAgent2Account(boMessage, xfer, trade, dsCon);
        // Block Intermediary Agent3
        // FinancialInstitution intrmyAgt3 = getIntermediaryAgent3(boMessage, xfer, trade, dsCon);
        // Block Intermediary Agent 3 Account
        // PartyAccount intrmyAgt3Acct = getIntermediaryAgent3Account(boMessage, xfer, trade, dsCon);
        // ultmtDbtr:
        // ultmtCdtr:
        // initgPty:
        // Build CreditTransferInstruction
        final CreditTransferInstruction creditXferIns = new CreditTransferInstruction();
        creditXferIns.setPmtId(paymentIdentification);
        creditXferIns.setSttlmInf(settlementInstruction);
        creditXferIns.setSttlmPrty(settlmPriority);
        creditXferIns.setIntrBkSttlmDt(intrBkSttlmDt);
        creditXferIns.setIntrBkSttlmAmt(interBankSettleAmtCcy);
        creditXferIns.setChrgBr(chrgBr);
        creditXferIns.setInstrForNxtAgt(instrForNxtAgt);
        creditXferIns.setRmtInf(rmtInf);
        creditXferIns.setPmtTpInf(pmtTpInf);
        creditXferIns.setSttlmTmReq(sttlmTmReq);
        creditXferIns.setInstdAgt(instructedAgent);
        creditXferIns.setInstgAgt(instructingAgent);
        creditXferIns.setDbtr(debtor);
        creditXferIns.setDbtrAcct(debtorAcct);
        creditXferIns.setDbtrAgt(debtorAgent);
        creditXferIns.setCdtr(creditor);
        creditXferIns.setCdtrAcct(creditorAcct);
        creditXferIns.setCdtrAgt(creditorAgent);
        creditXferIns.setCdtrAgtAcct(cdtrAgtAcct);
        creditXferIns.setSndrsCorrespdntAgt(sndrsCorrespdntAgt);
        creditXferIns.setSndrsCorrespdntAgtAcct(sndrsCorrespdntAgtAcct);
        creditXferIns.setRcvrsCorrespdntAgt(rcvrsCorrespdntAgt);
        creditXferIns.setRcvrsCorrespdntAgtAcct(rcvrsCorrespdntAgtAcct);
        creditXferIns.setIntrmyAgt(intermediaryAgent);
        creditXferIns.setIntrmyAgtAcct(intrmyAgtAcct);
        return creditXferIns;
    }


    /**
     * CreditTransfersInfo -> Get PaymentIdentification block. pmtId: instrId - pmtId: endToEndId -
     * pmtId: uetr
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PaymentIdentification getPaymentIdentification(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // pmtId: instrId
        final String paymentId = parseSANT_PAYMENT_UNIQUEID(boMessage, xfer, trade, dsCon);
        // pmtId: endToEndId
        final String endToEndId = parseSANT_END_TO_END_ID(boMessage, xfer, trade, dsCon);
        // pmtId: txId
        // string txId = parseSANT_TRANSACTION_ID(boMessage, xfer, trade, dsCon);
        // pmtId: uetr
        final String uetr = parseSANT_UETR(boMessage, xfer, trade, dsCon);
        // Build PaymentIdentification Block
        return PaymentsHubFormatterUtil.buildPaymentIdentification(paymentId, endToEndId, null, uetr);
    }


    /**
     * Get ClearingSystemIdentification block: cd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public ClearingSystemIdentification getClearingSystemIdentification(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Clearing System Code: cd
        final String clearingSystemCode = parseSANT_CLEARING_SYSTEM_CODE(boMessage, xfer, trade, dsCon);
        // ClearingSystemIdentification block
        return !Util.isEmpty(clearingSystemCode) ? PaymentsHubFormatterUtil.buildClearingSystemIdentification(clearingSystemCode) : null;
    }


    /**
     * Get Block Settlement Instruction - sttlmInf: clrSys: cd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public SettlementInstruction getSettlementInstruction(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get ClearingSystemIdentification block
        final ClearingSystemIdentification clrSysBlock = getClearingSystemIdentification(boMessage, xfer, trade, dsCon);
        // SettlementInstruction block
        return (clrSysBlock != null) ? PaymentsHubFormatterUtil.buildSettlementInstruction(clrSysBlock) : null;
    }


    /**
     * Get Interbank Settle Amount Currency block. intrBkSttlmAmt:value - intrBkSttlmAmt:ccy
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public AmountCurrency getInterbankSettleAmount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Interbank Settle Amount - intrBkSttlmAmt:value
        final String settleAmount = parseSANT_INTERBANK_SETTLE_AMOUNT(boMessage, xfer, trade, dsCon);
        // Interbank Settle Currency - intrBkSttlmAmt:ccy
        final String ccy = parseSANT_INTERBANK_SETTLE_AMOUNT_CCY(boMessage, xfer, trade, dsCon);
        return PaymentsHubFormatterUtil.buildAmountCurrency(settleAmount, ccy);
    }


    /**
     * Get Sender to Receiver Information block. instrForNxtAgt
     *
     * @param boMessage
     * @param xfer-
     * @param trade
     * @param dsCon
     * @return
     */
    public List<InstructedInfo> getInstructedForNextAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        List<InstructedInfo> instrInfoList = null;
        // Sender to Receiver Information
        final String instrForNxtAgt = parseSANT_INSTR_FOR_NEXT_AGENT(boMessage, xfer, trade, dsCon);
        // Sender to Receiver Information
        if (!Util.isEmpty(instrForNxtAgt)) {
            instrInfoList = new ArrayList<InstructedInfo>();
            final String[] split = instrForNxtAgt.split(SwiftMessage.END_OF_LINE);
            for (final String line : split) {
                final InstructedInfo instrInfo = PaymentsHubFormatterUtil.buildInstructedInfo(line);
                instrInfoList.add(instrInfo);
            }
        }
        return instrInfoList;
    }


    /**
     * Get RemittanceInfo block. rmtInf
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public RemittanceInfo getRemittanceInfo(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Remittance Info - rmtInf: ustrd
        final List<String> ustrd = parseSANT_REMITTANCE_INFO(boMessage, xfer, trade, dsCon);
        return PaymentsHubFormatterUtil.buildRemittanceInfo(ustrd);
    }


    /**
     * Get PaymentTypeInformation block. pmtTpInf: lclInstrm: cd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PaymentTypeInformation getPaymentTypeInformation(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Instruction Priority - pmtTpInf: instrPrty
        // String instrPrty = parseSANT_INSTRUCTION_PRIORITY(boMessage, xfer, trade, dsCon);
        // LocalInstrument Code - pmtTpInf: lclInstrm: cd
        final String pmTpInfCode = parseSANT_PAYMENT_TYPE_INFO_CODE(boMessage, xfer, trade, dsCon);
        // LocalInstrument Propietary - pmtTpInf: lclInstrm: prtry
        // String pmTpInfProp = parseSANT_PAYMENT_TYPE_INFO_PROPIETARY(boMessage, xfer, trade, dsCon);
        // Builds LocalInstrument block
        final LocalInstrument localInstrument = PaymentsHubFormatterUtil.buildLocalInstrument(pmTpInfCode, null);
        return PaymentsHubFormatterUtil.buildPaymentTypeInformation(localInstrument);
    }


    /**
     * Get SettlementTimeRequest block. SttlmTmReq
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public SettlementTimeRequest getSettlementTimeRequest(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Settlement Time Request - SttlmTmReq:CLSTm
        final String clsTm = parseSANT_CLEARING_TIME(boMessage, xfer, trade, dsCon);
        // Settlement Time Request - SttlmTmReq:tillTm
        // String tillTm = parseSANT_TILL_TIME(boMessage, xfer, trade, dsCon);
        // Settlement Time Request - SttlmTmReq:frTime
        // String frTm = parseSANT_FROM_TIME(boMessage, xfer, trade, dsCon);
        // Settlement Time Request - SttlmTmReq:CLSTm
        // String rjctTm = parseSANT_REJECTED_TIME(boMessage, xfer, trade, dsCon);
        return PaymentsHubFormatterUtil.buildSettlementTimeRequest(clsTm, null, null, null);
    }


    /**
     * Get Instructed Agent block. instdAgt: finInstnId :bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getInstructedAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Instructed Agent
        final String instdAgtBicfi = parseSANT_INSTD_AGT_BICFI(boMessage, xfer, trade, dsCon);
        final Party financialInstitutionId = PaymentsHubFormatterUtil.buildParty(instdAgtBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(financialInstitutionId);
    }


    /**
     * Get Instructing Agent block. instgAgt: finInstnId :bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getInstructingAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Instructing Agent
        final String instgAgtBicfi = parseSANT_INSTG_AGT_BICFI(boMessage, xfer, trade, dsCon);
        final Party financialInstitutionId = PaymentsHubFormatterUtil.buildParty(instgAgtBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(financialInstitutionId);
    }


    /**
     * Get Debtor block. dbtr: instId: bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public Institution getDebtor(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get dbtr/instId/pstlAdr/strtNm
        final String strtNm = parseSANT_DEBTOR_STREET_NAME(boMessage, xfer, trade, dsCon);

        // Get dbtr/instId/pstlAdr/pstCd
        final String pstCd = parseSANT_DEBTOR_POSTAL_CODE(boMessage, xfer, trade, dsCon);

        // Get dbtr/instId/pstlAdr/twnNm
        final String twnNm = parseSANT_DEBTOR_CITY(boMessage, xfer, trade, dsCon);

        // Get dbtr/instId/pstlAdr/ctry
        final String country = parseSANT_DEBTOR_COUNTRY(boMessage, xfer, trade, dsCon);

        // Builds Address block
        final Address debtorAddress = PaymentsHubFormatterUtil.buildAddress(country, twnNm, pstCd, strtNm, null, null);

        // Get dbtr/instId/ nm
        final String dbtrName = parseSANT_DEBTOR_FULL_NAME(boMessage, xfer, trade, dsCon);

        // Get dbtr/instId/bicfi
        final String dbtrBicfi = parseSANT_DEBTOR_BICFI(boMessage, xfer, trade, dsCon);

        final Party institutionId = PaymentsHubFormatterUtil.buildParty(dbtrBicfi, dbtrName, debtorAddress);

        return PaymentsHubFormatterUtil.buildInstitution(institutionId);
    }


    /**
     * Get Debtor Account block. dbtrAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getDebtorAccount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Account
        final String debtorAccount = parseSANT_DEBTOR_ACCOUNT_BICFI(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(debtorAccount);
        final Account account = (isIban) ? PaymentsHubFormatterUtil.buildAccount(debtorAccount, null) : PaymentsHubFormatterUtil.buildAccount(null, debtorAccount);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(account, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, account);
    }


    /**
     * Get Debtor Agent block. dbtrAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getDebtorAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get debtor agt bicfi
        final String debtorAgtBicfi = parseSANT_DEBTOR_AGENT_BICFI(boMessage, xfer, trade, dsCon);
        final Party debtorAgentParty = PaymentsHubFormatterUtil.buildParty(debtorAgtBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(debtorAgentParty);
    }


    /**
     * Get Creditor block. cdtr: instId: bicfi - cdtr: instId: nm - cdtr:instId:pstlAdr:twnNm - cdtr:
     * instId: pstlAdr: ctry
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public Institution getCreditor(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // cdtr:instId:pstlAdr:strtNm
        final String strtNm = parseSANT_CREDITOR_STREET_NAME(boMessage, xfer, trade, dsCon);

        // cdtr:instId:pstlAdr:pstCd
        final String pstCd = parseSANT_CREDITOR_POSTAL_CODE(boMessage, xfer, trade, dsCon);

        // cdtr: instId: pstlAdr: ctry
        final String country = parseSANT_CREDITOR_COUNTRY(boMessage, xfer, trade, dsCon);

        // cdtr:instId:pstlAdr:twnNm
        final String twnNm = parseSANT_CREDITOR_CITY(boMessage, xfer, trade, dsCon);

        // Builds Address block
        final Address creditorAddress = PaymentsHubFormatterUtil.buildAddress(country, twnNm, pstCd, strtNm, null, null);

        // cdtr: instId: bicfi
        final String creditorBicfi = parseSANT_CREDITOR_BICFI(boMessage, xfer, trade, dsCon);

        // cdtr: instId: nm
        final String creditorName = parseSANT_CREDITOR_FULL_NAME(boMessage, xfer, trade, dsCon);

        final Party institutionId = PaymentsHubFormatterUtil.buildParty(creditorBicfi, creditorName, creditorAddress);

        return PaymentsHubFormatterUtil.buildInstitution(institutionId);
    }


    /**
     * Get Creditor Account block. cdtrAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getCreditorAccount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Creditor Account bicfi
        final String creditorAccBicfi = parseSANT_CREDITOR_ACCOUNT_BICFI(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(creditorAccBicfi);
        final Account creditorAccount = (isIban) ? PaymentsHubFormatterUtil.buildAccount(creditorAccBicfi, null) : PaymentsHubFormatterUtil.buildAccount(null, creditorAccBicfi);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(creditorAccount, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, creditorAccount);
    }


    /**
     * Get Creditor Agent block. cdtrAgt: finInstnId: bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getCreditorAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Creditor Agent bicfi - cdtrAgt: finInstnId: bicfi
        final String creditorAgtBicfi = parseSANT_CREDITOR_AGENT_BICFI(boMessage, xfer, trade, dsCon);
        // Get Creditor Agent Party Name - cdtrAgt: finInstnId: nm
        final String creditorAgtNm = parseSANT_CREDITOR_AGENT_PARTY_NAME(boMessage, xfer, trade, dsCon);
        final Party creditorAgentParty = PaymentsHubFormatterUtil.buildParty(creditorAgtBicfi, creditorAgtNm, null);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(creditorAgentParty);
    }


    /**
     * Get Creditor Agent Account block. cdtrAgtAcc
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getCreditorAgentAccount(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        // Get Creditor Agent Account
        final String cdtrAgtAcc = parseSANT_CREDITOR_AGT_ACC(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(cdtrAgtAcc);
        final Account account = (isIban) ? PaymentsHubFormatterUtil.buildAccount(cdtrAgtAcc, null) : PaymentsHubFormatterUtil.buildAccount(null, cdtrAgtAcc);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(account, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, account);
    }


    /**
     * Get Senders Correspondent Agent block. sndrsCorrespdntAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getSendersCorrespondentAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Senders Corrspondent Agent
        final String sndrsCorrespdntAgtBicfi = parseSANT_SENDS_CORRESPDNT_AGT_BICFI(boMessage, xfer, trade, dsCon);
        final Party financialInstitutionId = PaymentsHubFormatterUtil.buildParty(sndrsCorrespdntAgtBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(financialInstitutionId);
    }


    /**
     * Get Senders Correspondent Agent Account block. sndrsCorrespdntAgtAcc
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getSendersCorrespondentAgentAccount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Senders Corrspondent Agent Account
        final String sndrsCorrespdntAgtAcc = parseSANT_SENDS_CORRESPDNT_AGT_ACC(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(sndrsCorrespdntAgtAcc);
        final Account account = (isIban) ? PaymentsHubFormatterUtil.buildAccount(sndrsCorrespdntAgtAcc, null) : PaymentsHubFormatterUtil.buildAccount(null, sndrsCorrespdntAgtAcc);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(account, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, account);
    }


    /**
     * Get Receivers Correspondent Agent block. rcvrsCorrespdntAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getReceiversCorrespondentAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Receivers Corrspondent Agent
        final String rcvrsCorrespdntAgtBicfi = parseSANT_RECVS_CORRESPDNT_AGT_BICFI(boMessage, xfer, trade, dsCon);
        final Party financialInstitutionId = PaymentsHubFormatterUtil.buildParty(rcvrsCorrespdntAgtBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(financialInstitutionId);
    }


    /**
     * Get Receivers Correspondent Agent Account block. rcvrsCorrespdntAgtAccount
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getReceiversCorrespondentAgentAccount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Receivers Corrspondent Agent
        final String rcvrsCorrespdntAgtAccount = parseSANT_RECVS_CORRESPDNT_AGT_ACC(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(rcvrsCorrespdntAgtAccount);
        final Account account = (isIban) ? PaymentsHubFormatterUtil.buildAccount(rcvrsCorrespdntAgtAccount, null) : PaymentsHubFormatterUtil.buildAccount(null, rcvrsCorrespdntAgtAccount);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(account, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, account);
    }


    /**
     * Get Intermediary Agent1 block. intrmyAgt: finInstnId: bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public FinancialInstitution getIntermediaryAgent(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Intermediary Agent1
        final String intermAgentBicfi = parseSANT_INTERMEDIARY_AGENT_BICFI(boMessage, xfer, trade, dsCon);
        final Party intermediaryAgentParty = PaymentsHubFormatterUtil.buildParty(intermAgentBicfi);
        return PaymentsHubFormatterUtil.buildFinancialInstitution(intermediaryAgentParty);
    }


    /**
     * Get Intermediary Agent Account block. Receiver.Intermediary.Account
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public PartyAccount getIntermediaryAgentAccount(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Intermediary Agent Account.
        final String intermediaryAgtAcc = parseSANT_INTERMEDIARY_AGENT_ACCOUNT(boMessage, xfer, trade, dsCon);
        final boolean isIban = PaymentsHubUtil.isIBAN(intermediaryAgtAcc);
        final Account account = (isIban) ? PaymentsHubFormatterUtil.buildAccount(intermediaryAgtAcc, null) : PaymentsHubFormatterUtil.buildAccount(null, intermediaryAgtAcc);
        return (isIban) ? PaymentsHubFormatterUtil.buildPartyAccount(account, null) : PaymentsHubFormatterUtil.buildPartyAccount(null, account);
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Get SupplementaryData block.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public SupplementaryData getSupplementaryData(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Block Envelope
        final Envelope envelope = getEnvelope(boMessage, xfer, trade, dsCon);
        // Build SupplementaryData
        final SupplementaryData supplementaryData = new SupplementaryData();
        supplementaryData.setEnvlp(envelope);
        return supplementaryData;
    }


    /**
     * Get Envelope block as part of SupplementaryData.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public Envelope getEnvelope(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Block BackOfficeInfo
        final BackOfficeInfo backOfficeInfo = getBackOfficeInfo(boMessage, xfer, trade, dsCon);
        // LegacyInfo splmtryData: envlp: legacyInfo
        final LegalEntity po = BOCache.getLegalEntity(dsCon, xfer.getInternalLegalEntityId());
        final String legacyInfo = (po == null || PO_BDSD.equals(po.getName())) ? null : parseSANT_LEGACY_INFO(boMessage, xfer, trade, dsCon);
        // Build Envelope
        return PaymentsHubFormatterUtil.buildEnvelope(legacyInfo, backOfficeInfo);
    }


    /**
     * Get BackOfficeInfo block as part of SupplementaryData.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public BackOfficeInfo getBackOfficeInfo(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Provider splmtryData: envlp: backOfficeInfo:provider
        final String provider = parseSANT_PROVIDER(boMessage, xfer, trade, dsCon);

        // EntityCode splmtryData:envlp: backOfficeInfo:entityCode
        final String entityCode = parseSANT_ENTITY_CODE(boMessage, xfer, trade, dsCon);

        // Branch splmtryData: envlp: backOfficeInfo:branch
        final String branch = parseSANT_BRANCH(boMessage, xfer, trade, dsCon);

        // Folder splmtryData: envlp: backOfficeInfo:folder
        // String folder = parseSANT_FOLDER(boMessage, xfer, trade, dsCon);

        // Contract splmtryData: envlp: backOfficeInfo:contract
        final String contract = parseSANT_CONTRACT(boMessage, xfer, trade, dsCon);

        // creditorResidence splmtryData: envlp: creditorResidence
        // String creditorResidence = parseSANT_CREDITOR_RESIDENCE(boMessage, xfer, trade, dsCon);

        // creditorNetting splmtryData: envlp: creditorNetting
        // String creditorNetting = parseSANT_CREDITOR_NETTING(boMessage, xfer, trade, dsCon);

        // Product Type splmtryData: envlp: productType
        // String productType = parseSANT_PRODUCT_TYPE(boMessage, xfer, trade, dsCon);

        // Product SubType splmtryData: envlp: productSubType
        // String productSubType = parseSANT_PRODUCT_SUBTYPE(boMessage, xfer, trade, dsCon);

        // productBunding splmtryData: envlp: productBunding
        // String productBunding = parseSANT_PRODUCT_BUNDING(boMessage, xfer, trade, dsCon);

        // Front reference splmtryData: envlp: frontRef
        // String frontRef = parseSANT_FRONT_REFERENCE(boMessage, xfer, trade, dsCon);

        // Back/Operation reference splmtryData: envlp: backRef
        // String backRef = parseSANT_BACK_REFERENCE(boMessage, xfer, trade, dsCon);

        // Get Accounting Block
        final Accounting accounting = getAccounting(boMessage, xfer, trade, dsCon);

        // DeliveryFlag splmtryData: envlp: backOfficeInfo:deliveryFlag
        final String deliveryFlag = parseSANT_DELIVERY_FLAG(boMessage, xfer, trade, dsCon);

        // Build BackOfficeInfo
        return PaymentsHubFormatterUtil.buildBackOfficeInfo(provider, entityCode, branch, contract, null, null, null, null, null, null, null, null, Boolean.valueOf(deliveryFlag), accounting);
    }


    /**
     * Get Accounting block as part of SupplementaryData.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public Accounting getAccounting(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Mirror Account splmtryData: envlp: backOfficeInfo:accounting: mirrorAccount
        final String mirrorAccount = parseSANT_MIRROR_ACCOUNT(boMessage, xfer, trade, dsCon);
        // Accounting Flag : splmtryData: envlp: backOfficeInfo:accounting: accountingFlag
        final String accountingFlag = parseSANT_ACCOUNTING_FLAG(boMessage, xfer, trade, dsCon);
        // Netted splmtryData: envlp: backOfficeInfo:accounting: netted
        final String netted = parseSANT_NETTED_TRANSFER(boMessage, xfer, trade, dsCon);
        // Accounting Type splmtryData: envlp: backOfficeInfo:accounting: accountingType
        final String accountingType = parseSANT_ACCOUNTING_TYPE(boMessage, xfer, trade, dsCon);
        // Narratives
        final List<String> narratives = parseSANT_ACCOUNTING_NARRATIVES(boMessage, xfer, trade, dsCon);
        // Build Accounting
        return PaymentsHubFormatterUtil.buildAccounting(Boolean.parseBoolean(accountingFlag),
                accountingType, null, null, null, null, mirrorAccount, narratives, Boolean.parseBoolean(netted));
    }


    // ----------------------------------------------------------------- //
    // ------------------------- PARSE METHODS ------------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Get GroupHeader -> EventType.
     *
     * If BOMessage.SubAction == NONE or NEW -> return N.
     *
     * If BOMessage.SubAction == CANCELED -> return C.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_EVENT_TYPE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOMessage SubAction
        final Action subAction = boMessage.getSubAction();
        if (subAction != null) {
            if (Action.NONE.equals(subAction) || Action.NEW.equals(subAction)) {
                return PHConstants.EVENT_TYPE_N;
            } else if (Action.CANCEL.equals(subAction)) {
                return PHConstants.EVENT_TYPE_C;
            }
        }
        return PHConstants.EVENT_TYPE_N;
    }


    /**
     * Get GroupHeader -> ConceptType.
     *
     * If BOTransfer is PAY -> return P.
     *
     * If BOTransfer is RECEIVE -> return C.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CONCEPT_TYPE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer PAY or RECEIVE
        final String type = xfer.getPayReceiveType();
        return (XFER_TYPE_PAY.equals(type)) ? PHConstants.CONCEPT_TYPE_P : (XFER_TYPE_RECEIVE.equals(type)) ? PHConstants.CONCEPT_TYPE_C : null;
    }


    /**
     * Get GroupHeader -> messageType
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_MESSAGE_TYPE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PaymentsHubUtil.MESSAGE_TYPE_DEFAULT;
    }


    /**
     * Get GroupHeader -> nbOfTxs.
     *
     * By default, 1.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_NUMBER_OF_TRANSACTIONS(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return String.valueOf(PHConstants.NUMBER_TXS);
    }


    /**
     * CreditTransfersInfo -> Get PaymentIdentification Id (instrId)
     *
     * If Cancellation Message: Prefix "canc-" plus the TRN. Else, only the TRN
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_PAYMENT_UNIQUEID(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PaymentsHubFormatterUtil.buildTRN(boMessage, trade, xfer);
    }


    /**
     * CreditTransfersInfo -> Get End To End Identification (endToEndId).
     *
     * If Cancellation Message: TRN. Else NOTPROVIDED
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_END_TO_END_ID(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if(isCancellationMessage(boMessage, xfer, trade, dsCon)){
            Long boLinkedMsgId = boMessage.getLinkedLongId();
            if(boLinkedMsgId > 0){
                try {
                    BOMessage linkedMsg = DSConnection.getDefault().getRemoteBO().getMessage(boLinkedMsgId);
                    if(linkedMsg!=null){
                        String id = PaymentsHubFormatterUtil.buildTRN(linkedMsg, trade, xfer);
                        return !Util.isEmpty(id) ? id : PHConstants.NOT_PROVIDED;
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(LOG_CATEGORY, "Could not get the linked boMessage with id " + boLinkedMsgId, e);
                }
            }
        }
        return PHConstants.NOT_PROVIDED;
        //return Optional.ofNullable(trade).map(Trade::getLongId).map(String::valueOf).orElse(PHConstants.NOT_PROVIDED);
    }


    /**
     * CreditTransfersInfo -> Get Unique End-to-end Transaction Reference
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_UETR(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PaymentsHubUtil.getUUID(xfer);
    }


    /**
     * Get Clearing System Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CLEARING_SYSTEM_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Settlement Method
        final String settMethod = (xfer != null) ? xfer.getSettlementMethod() : "";
        // Get the code from DomainValues
        return PaymentsHubUtil.getSettlementMethodCode(settMethod);
    }


    /**
     * CreditTransfersInfo -> Get Settlement Priority: sttlmPrty
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_SETTLEMENT_PRIORITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String settPr = PHConstants.SETTLEMENT_PRIORITY_NORM; // By default
        // Check SettlementMethod CHAPS
        if (CHAPS.equals(xfer.getSettlementMethod())) {
            settPr = PHConstants.SETTLEMENT_PRIORITY_HIGH;
        }
        return settPr;
    }


    /**
     * CreditTransfersInfo -> Get BOTransfer 'Settle Date'.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INTERBANK_SETTLE_DATE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Settle Date
        final JDatetime settleDatetime = xfer.getSettleDate().getJDatetime();
        final String settleDatetimeStr = Util.datetimeToString(settleDatetime, PHConstants.INTERBANK_SETT_DATE_FORMAT);
        return !Util.isEmpty(settleDatetimeStr) ? settleDatetimeStr : null;
    }


    /**
     * CreditTransfersInfo -> chrgBr
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CHARGE_TO_BEAR(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override, if necessary
    }


    /**
     * Get BOTransfer 'Settle Amount'.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INTERBANK_SETTLE_AMOUNT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return CollateralUtilities.formatAmount(Math.abs(xfer.getSettlementAmount()), xfer.getSettlementCurrency()).toString().replace(".","").replace(",",".");
    }


    /**
     * Get the value BOTransfer 'Settle Currency'.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INTERBANK_SETTLE_AMOUNT_CCY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer Currency
        return xfer.getSettlementCurrency();
    }


    /**
     * Get instrForNxtAgt: Additional Info - field 72
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INSTR_FOR_NEXT_AGENT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get RemittanceInfo: ustrd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public List<String> parseSANT_REMITTANCE_INFO(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get PaymentTypeInformation Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_PAYMENT_TYPE_INFO_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PHConstants.CRED;
    }


    /**
     * Get Clearing time
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CLEARING_TIME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get instdAgt bicfi -> BOMessage Receiver Address Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INSTD_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Receiver Address Code
        return boMessage.getReceiverAddressCode();
    }


    /**
     * Get instgAgt bicfi -> BOMessage Sender Address Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INSTG_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Sender Address Code
        return boMessage.getSenderAddressCode();
    }


    /**
     * Get cdtr:instId:pstlAdr:strtNm
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_STREET_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get cdtr:instId:pstlAdr:pstCd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_POSTAL_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get cdtr: country -> BOTransfer field 'Receiver.Country'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_COUNTRY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Receiver.Country'
        final String receiverCountry = PaymentsHubUtil.getBOTransferReceiverCountry(xfer);
        final Country country = BOCache.getCountry(dsCon, receiverCountry);
        return (country != null && !Util.isEmpty(country.getISOCode())) ? country.getISOCode() : null;
    }


    /**
     * Get cdtr: city -> BOTransfer field 'Receiver.Code' and get the City.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Receiver.Code'
        final LegalEntity le = PaymentsHubUtil.getLegalEntity(xfer, XFER_TYPE_RECEIVE);
        final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
        return contact != null && !Util.isEmpty(contact.getCityName()) ? contact.getCityName() : null;
    }


    /**
     * Get cdtr bicfi -> BOTransfer field 'Receiver.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Receiver.Swift'
        final String receiverSwift = PaymentsHubUtil.getBOTransferReceiverSwift(trade, xfer);
        return !Util.isEmpty(receiverSwift) ? receiverSwift : null;
    }


    /**
     * Get cdtr name -> Get BOTransfer SDI External, le beneficiary full name
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_FULL_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String receiverFullName = null;
        if (xfer != null) {
            // Get BOTransfer SDI External
            final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, xfer.getExternalSettleDeliveryId());
            if (sdi != null) {
                int leId = sdi.getLegalEntityId();
                if(leId>0) {
                    final LegalEntity legalEntity = BOCache.getLegalEntity(dsCon, leId);
                    if(legalEntity != null){
                        receiverFullName = legalEntity.getName();
                    }
                }
            }
        }
        return !Util.isEmpty(receiverFullName) ? receiverFullName : null;
    }


    /**
     * Get cdtrAcct bicfi -> BOTransfer field 'Receiver.Agent.Account'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Receiver.Agent.Account'
        final String receiverAgentAccount = PaymentsHubUtil.getBOTransferReceiverAgentAccount(trade, xfer);
        return !Util.isEmpty(receiverAgentAccount) ? receiverAgentAccount : null;
    }


    /**
     * Get cdtrAgt bicfi -> BOTransfer field 'Receiver.Agent.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Receiver.Agent.Swift'
        final String receiverAgentSwift = PaymentsHubUtil.getBOTransferReceiverAgentSwift(trade, xfer);
        return !Util.isEmpty(receiverAgentSwift) ? receiverAgentSwift : null;
    }


    /**
     * Get cdtrAgt Party Name -> External SDI Agent Name
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_AGENT_PARTY_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String cdtrAgtPartyName = null;
        if (xfer != null) {
            // Get External SDI
            final int sdiId = xfer.getExternalSettleDeliveryId();
            final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
            if (sdi != null) {
                // Get Agent Name
                cdtrAgtPartyName = sdi.getAgentName();
            }

        }
        return cdtrAgtPartyName;
    }


    /**
     * Get cdtrAgtAcc
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CREDITOR_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String cdtrAgtAcc = null;
        if (xfer != null) {
            cdtrAgtAcc = PaymentsHubUtil.getBOTransferReceiverIntermediaryAccount(xfer);
        }
        return cdtrAgtAcc;
    }


    /**
     * Get SndrsCorrespdntAgt -> Override
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_SENDS_CORRESPDNT_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get SndrsCorrespdntAgtAcc -> Override
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_SENDS_CORRESPDNT_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get RcvrsCorrespdntAgt -> Override
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_RECVS_CORRESPDNT_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get RcvrsCorrespdntAgtAcc -> Override
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_RECVS_CORRESPDNT_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get intrmyAgt -> BOTransfer field 'Receiver.Intermediary.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INTERMEDIARY_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get Receiver.Intermediary.Swift
        final String receiverIntermediarySwift = PaymentsHubUtil.getBOTransferReceiverIntermediarySwift(trade, xfer);
        return !Util.isEmpty(receiverIntermediarySwift) ? receiverIntermediarySwift : null;
    }


    /**
     * Get intrmyAgtAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_INTERMEDIARY_AGENT_ACCOUNT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String cdtrAgtAcc = null;
        if (xfer != null) {
            cdtrAgtAcc = PaymentsHubUtil.getBOTransferReceiverIntermediary2Account(xfer);
        }
        return cdtrAgtAcc;
    }


    /**
     * Get dbtr/instId/nm
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_FULL_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get dbtr -> BOTransfer field 'Payer.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Payer.Swift'
        final String payerSwift = PaymentsHubUtil.getBOTransferPayerSwift(trade, xfer);
        return !Util.isEmpty(payerSwift) ? payerSwift : null;
    }


    /**
     * Get dbtrAcct -> BOTransfer field 'Payer.Agent.Account'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Payer.Agent.Account'
        final String payerAgentAccount = PaymentsHubUtil.getBOTransferPayerAgentAccount(trade, xfer);
        return !Util.isEmpty(payerAgentAccount) ? payerAgentAccount : null;
    }


    /**
     * Get dbtr/instId/pstlAdr/strtNm
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_STREET_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get dbtr/instId/pstlAdr/pstCd
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_POSTAL_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get dbtr/instId/pstlAdr/twnNm
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get dbtr/instId/pstlAdr/ctry
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_COUNTRY(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null; // Override if necessary
    }


    /**
     * Get dbtrAgt bicfi -> BOTransfer field 'Payer.Agent.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DEBTOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Payer.Agent.Swift'
        final String payerAgentSwift = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
        return !Util.isEmpty(payerAgentSwift) ? payerAgentSwift : null;
    }


    /**
     * SupplementaryData: Get Provider -> 'Calypso-FX' by default.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_PROVIDER(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PHConstants.PROVIDER_DEFAULT;
    }


    /**
     * Get entityCode -> PO attribute Partenon-IDEMPR.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_ENTITY_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String entityCode = null;
        LegalEntity po = null;
        if (trade != null) {
            po = trade.getBook().getLegalEntity();
        } else if (xfer != null) {
            po = BOCache.getLegalEntity(dsCon, xfer.getInternalLegalEntityId());
        }
        if(po!=null){
            entityCode = BOCreUtils.getInstance().getEntityCod(po.getCode(), false);
        }
        return entityCode;
    }


    /**
     * Get Branch -> PO attribute Partenon-IDCENT, if the EntityCode is not empty.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_BRANCH(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String entityCode = null;
        String accCenter = null;
        String branch = null;
        LegalEntity po = null;
        if (trade != null) {
            po = trade.getBook().getLegalEntity();
        } else if (xfer != null) {
            po = BOCache.getLegalEntity(dsCon, xfer.getInternalLegalEntityId());
        }
        if(po!=null){
            if("BSTE".equalsIgnoreCase(po.getCode())){
                branch = "0049-5493";
            }
            else if("BDSD".equalsIgnoreCase(po.getCode())){
                branch = "0049-1100";
            }
        }
        return branch;
    }


    /**
     * Get Contract.
     *
     * If the transfer is not netted, return the tradeId + "_STC".
     *
     * If the transfer is netted, return 0
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_CONTRACT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if(PaymentsHubFormatterUtil.isBOTransferNetted(xfer)){
            return String.valueOf(xfer.getLongId()).concat(PHConstants.SUFIX_STC);
        }
        String partenon = PaymentsHubUtil.getPartenonIdContr(trade);
        if("BSTE".equalsIgnoreCase(trade.getBook().getLegalEntity().getCode()) && !Util.isEmpty(partenon)){
            return partenon.substring(0, 18);
        }
        else{
            return String.valueOf(trade.getLongId()).concat(PHConstants.SUFIX_STC);
        }
    }


    /**
     * SupplementaryData: Get Accounting Flag -> True by default.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_ACCOUNTING_FLAG(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return Boolean.TRUE.toString();
    }


    /**
     * SupplementaryData: Get Mirror Accounting
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_MIRROR_ACCOUNT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // override if necessary
    }


    /**
     * SupplementaryData: Get if transfer netting type is NONE.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_NETTED_TRANSFER(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get NettingType
        final String nettingType = xfer.getNettingType();
        if (!Util.isEmpty(nettingType)) {
            return ("None".equalsIgnoreCase(nettingType)) ? Boolean.FALSE.toString() : Boolean.TRUE.toString();
        }
        return Boolean.TRUE.toString();
    }


    /**
     * SupplementaryData: Get AccountingType -> By default, TRANSITORY_TO_NOSTRO
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_ACCOUNTING_TYPE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return Accounting.ACCOUNTING_TYPE_DEFAULT;
    }


    /**
     * SupplementaryData: Get Delivery Flag -> Check SDI Attribute PH_NoMessageForGSTP
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_DELIVERY_FLAG(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String deliveryFlag = Boolean.TRUE.toString(); // By default.
        // Get Internal SDI
        final SDI internalSdi = BOCache.getSettleDeliveryInstruction(dsCon, xfer.getInternalSettleDeliveryId());
        if (internalSdi != null) {
            // Agent Internal SDI
            final PartySDI agent = internalSdi.getAgent();
            if (agent != null) {
                // Check Msg checkbox is checked
                if (agent.getMessageToParty()) {
                    // Get Attribute SDI_ATTRIBUTE_PH_NO_MESSAGE_FOR_GSTP
                    final String attrPhNoMsgGstp = internalSdi.getAttribute(SDI_ATTRIBUTE_PH_NO_MESSAGE_FOR_GSTP);
                    if (!Util.isEmpty(attrPhNoMsgGstp)) {
                        // Get DeliverFlag by xfer type.
                        final boolean flag = PaymentsHubUtil.getDeliveryFlagByXferType(xfer, dsCon);
                        deliveryFlag = new Boolean(flag).toString();
                    }
                }
            }
        }
        return deliveryFlag;
    }


    /**
     * SupplementaryData: Get Narratives.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public List<String> parseSANT_ACCOUNTING_NARRATIVES(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        final Map<String, String> valuesDescMap = new HashMap<String, String>();
        final List<String> narratives = new ArrayList<String>();
        for (int i = 1; i <= NARRATIVES_SIZE; i++) {
            final String desc = "desc" + i;
            final String methodName = "SANT_ACCOUNTING_".concat(desc).toUpperCase();
            final String rst = invokeMethod(methodName, boMessage, xfer, trade, dsCon);
            valuesDescMap.put(desc, rst);
        }
        // Fill narratives list: adding nulls
        valuesDescMap.forEach((k, v) -> narratives.add(v));
        return narratives;
    }


    /**
         * Get Accounting Desc1.
         *
         * Transfer is not netted -> Partenon.
         *
         * Transfer is netted -> PO Attribute
         *
         * @param boMessage
         * @param xfer
         * @param trade
         * @param dsCon
         * @return
         */
        public String parseSANT_ACCOUNTING_DESC1(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
            if(trade!=null && !PaymentsHubFormatterUtil.isBOTransferNetted(xfer)) {
                return PaymentsHubUtil.getPartenonIdContr(trade);
            }
            return null;
        }


        /**
         * Get Accounting Desc2.
         *
         * Transfer is not netted -> Partenon.
         *
         * Transfer is netted -> PO Attribute
         *
         * @param boMessage
         * @param xfer
         * @param trade
         * @param dsCon
         * @return
         */
        public String parseSANT_ACCOUNTING_DESC2(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
            if(trade!=null && !PaymentsHubFormatterUtil.isBOTransferNetted(xfer)) {
                return trade.getExternalReference();
            }
            return null;
        }


    /**
     * Get Accounting Desc4.
     *
     * Transfer is not netted -> Partenon.
     *
     * Transfer is netted -> PO Attribute
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_ACCOUNTING_DESC4(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if(trade!=null && !PaymentsHubFormatterUtil.isBOTransferNetted(xfer)) {
            LegalEntity cpty = trade.getCounterParty();
            if(cpty != null){
                return cpty.getCode();
            }
        }
        final LegalEntity cpty = BOCache.getLegalEntity(dsCon, xfer.getExternalLegalEntityId());
        if(cpty!=null){
            return cpty.getCode();
        }
        return null;
    }


    /**
     * Get Accounting Desc5.
     *
     * Transfer is not netted -> Partenon Accounting Center.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_ACCOUNTING_DESC5(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if(trade!=null && !PaymentsHubFormatterUtil.isBOTransferNetted(xfer)) {
            String partenonKwd = PaymentsHubUtil.getPartenonIdContr(trade);
            if(!Util.isEmpty(partenonKwd) && 21==partenonKwd.length()){
                return partenonKwd.substring(4,8);
            }
        }
        return "";
    }


    /**
     * SupplementaryData: Get LegacyInfo -> must be override.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Must be override
        return null;
    }


    // ----------- Logic


    /**
     * If is a Cancellation Message checking EventType, return true.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    private boolean isCancellationMessage(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        final String eventType = parseSANT_EVENT_TYPE(boMessage, xfer, trade, dsCon);
        return PHConstants.EVENT_TYPE_C.equals(eventType);
    }


    /**
     * Invoke Method.
     *
     * @param name Method Name
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String invokeMethod(final String name, final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String res = null;
        final Object[] objs = new Object[] { boMessage, xfer, trade, dsCon };
        final PAYMENTHUBMSGMessageFormatter applyTo = this;
        final Method m = getParseMethod(this, name);
        if (m != null) {
            try {
                res = (String) m.invoke(applyTo, objs);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                final String msg = String.format("Error invoking method by reflecting [%s].", name);
                Log.error(this, msg, e);
            }
        }
        return res;
    }


    /**
     * Get Additional Info - Field 72
     *
     * Copy from TransferSWIFTFormatter.
     *
     * @param trade
     * @param transfer
     * @param message
     * @return
     */
    public String getAdditionalInfoField72(BOMessage message, BOTransfer transfer, Trade trade, final DSConnection con) {
        String senderInfoAttr = "";
        if (transfer == null) {
            message = getMessage(message);
            senderInfoAttr = message.getAttribute("Group");
            if (!Util.isEmpty(senderInfoAttr)) {
                return "";
            }
            trade = getTrade(message, con);
            transfer = getTransfer(message, con);
        }
        if (transfer != null) {
            senderInfoAttr = transfer.getAttribute("SenderInfo");
            if (!Util.isEmpty(senderInfoAttr)) {
                if (senderInfoAttr.equals("EMPTY VALUE")) {
                    return "";
                }
                senderInfoAttr = getInfo(senderInfoAttr, transfer, message);
                return SwiftUtil.formatTag72(senderInfoAttr, message.getTemplateName());
            }
        }
        TradeTransferRule rule1 = null;
        if (transfer != null) {
            rule1 = transfer.toTradeTransferRule();
        } else {
            try {
                message = getFirstUnderlying(message, con);
                transfer = getTransfer(message, con);
                rule1 = transfer.toTradeTransferRule();
            } catch (final Exception arg11) {
                Log.error("Swift", arg11);
            }
        }
        String info;
        if (trade != null && trade.getProduct() instanceof CustomerTransfer) {
            final CustomerTransfer sdi = (CustomerTransfer) trade.getProduct();
            if (!Util.isEmpty(sdi.getSenderToReceiverInfo())) {
                info = getInfo(sdi.getSenderToReceiverInfo(), transfer, message);
                return SwiftUtil.formatTag72(info, message.getTemplateName());
            }
            if ((rule1 == null || !rule1.isManualSDI()) && Defaults.getBooleanProperty("USE_CALL_ACCOUNT", false)) {
                return "";
            }
        }
        if (rule1 != null) {
            if (!rule1.isManualSDI()) {
                final SettleDeliveryInstruction sdi1 = BOCache.getSettleDeliveryInstruction(con, rule1.getCounterPartySDId());
                if (!Util.isEmpty(sdi1.getComments())) {
                    info = getInfo(sdi1.getComments(), transfer, message);
                    return SwiftUtil.formatTag72(info, message.getTemplateName());
                }
            } else {
                final ManualSDI sdi2 = BOCache.getManualSDI(con, rule1.getManualSDId());
                if (!Util.isEmpty(sdi2.getComments())) {
                    info = getInfo(sdi2.getComments(), transfer, message);
                    return SwiftUtil.formatTag72(info, message.getTemplateName());
                }
            }
        }
        return "";
    }


    /**
     * Copy from TransferSWIFTFormatter.
     *
     * @param info
     * @param transfer
     * @param message
     * @return
     */
    private static String getInfo(String info, BOTransfer transfer, BOMessage message) {
        if (!Util.isEmpty(info) && transfer != null && !Util.isEmpty(transfer.getSettlementMethod()) && message != null) {
            if (transfer.getSettlementMethod().equalsIgnoreCase("LBTR")
                    || transfer.getSettlementMethod().equalsIgnoreCase("Combanc")) {
                String ref = message.getAttribute("TAG20");
                if (Util.isEmpty(ref)) {
                    ref = String.valueOf(CoreAPI.getId(message));
                }
                if (info.endsWith("/")) {
                    info = info + ref;
                } else {
                    info = info + "/" + ref;
                }
            }
            return info;
        } else {
            return info;
        }
    }


}

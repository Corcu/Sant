package calypsox.tk.bo.util;


import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;
import com.calypso.tk.service.RemoteReferenceData;
import com.calypso.tk.util.MessageArray;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Optional;

import static calypsox.tk.bo.util.PHConstants.*;


public class PaymentsHubUtil {


    public static final String MESSAGE_TYPE_DEFAULT = "Single Customer Credit Transfer";
    private static final String DN_PAYMENTS_HUB_PARAMETERS = "PaymentsHub.parameters";
    public static final String DN_PAYMENTS_HUB_SETTL_METHOD = "PaymentsHub.acceptedSettMethods";
    private static final String LOG_PATTERN = "[PAYMENTS HUB - CALYPSO FX - API TECHNICAL ISSUE]";
    public static final String DN_PH_VALIDATION = "PaymentsHubValidation.";
    protected static final String LOG_CATEGORY = PaymentsHubUtil.class.getSimpleName();


    private static DSConnection getDSCon() {
        return DSConnection.getDefault();
    }


    /**
     * Get PaymentsHub parameter.
     *
     * @param domainValue
     * @param defaultValue
     * @return
     */
    public static String getPaymenstHubParameterValue(final String domainValue, final String defaultValue) {
        final String value = DomainValues.comment(DN_PAYMENTS_HUB_PARAMETERS, domainValue);
        return (!Util.isEmpty(value)) ? value : defaultValue;
    }


    /**
     * Check if the BOMessage has been sent to PaymentsHub
     *
     * @param message
     * @return
     */
    public static boolean isSentToPaymentsHub(final BOMessage message) {
        boolean sentToPH = false;
        if (message != null) {
            // Get 'PH_PaymentSystemResponseHeader' Attribute value
            final String xPaymentSystemValue = message.getAttribute(PHConstants.MSG_ATTR_PH_PAYMENT_SYSTEM_RESPONSE_HEADER);
            sentToPH = xPaymentSystemValue != null && PHConstants.PAYMENTS_HUB_SYSTEM.equals(xPaymentSystemValue.trim());
        }
        return sentToPH;
    }


    /**
     * Get LEContact by LEContactId
     *
     * @param leContactId
     * @param dsCon
     * @return
     */
    public static LEContact getContactById(final int leContactId, final DSConnection dsCon) {
        LEContact leContact = null;
        final RemoteReferenceData rrd = dsCon.getRemoteReferenceData();
        try {
            leContact = rrd.getContact(leContactId);
        } catch (final CalypsoServiceException e) {
            final String msg = "Error getting the LEContact with ID: " + leContactId;
            Log.error(PaymentsHubUtil.class, msg, e);
        }
        return leContact;
    }


    /**
     * Is SettlementMethod CHAPS
     *
     * @param xfer
     * @return
     */
    public static boolean isCHAPS(final BOTransfer xfer) {
        return xfer != null && PHConstants.SETTLEMENT_METHOD_CHAPS.equals(xfer.getSettlementMethod());
    }


    /**
     * Is SettlementMethod TARGET2
     *
     * @param xfer
     * @return
     */
    public static boolean isTARGET2(final BOTransfer xfer) {
        return xfer != null && PHConstants.SETTLEMENT_METHOD_TARGET2.equals(xfer.getSettlementMethod());
    }


    /**
     * Get BOTransfer field 'Receiver.Agent.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverAgentSwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverAgentSwift(trade, transfer, PHConstants.XFER_TYPE_RECEIVE);

    }


    /**
     * Get BOTransfer field 'Payer.Agent.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerAgentSwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverAgentSwift(trade, transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Payer.Agent.Swift' or 'Receiver.Agent.Swift'.
     *
     * @param trade
     * @param transfer
     * @param type
     * @return
     */
    public static String getBOTransferPayerReceiverAgentSwift(final Trade trade, final BOTransfer transfer, final String type) {
        String payRecAgentSwift = "";
        final Object objSdi = getSDI(transfer, type);
        if (objSdi != null) {
            if (objSdi instanceof ManualSDI) {
                final ManualSDI manualSdi = (ManualSDI) objSdi;
                final PartySDI agent = manualSdi.getAgent();
                if (agent == null) {
                    return "";
                }
                if (manualSdi.isAgentUnknown()) {
                    return agent.getCodeValue();
                }
                payRecAgentSwift = getSWIFT(agent.getPartyId(), agent.getPartyCode(), agent.getPartyContactType(),
                        agent.getPartyRole(), transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(),
                        trade, transfer);
            } else {
                final SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) objSdi;
                payRecAgentSwift = getSWIFT(sdi.getAgentId(), sdi.getAgentName(), sdi.getAgentContactType(), "Agent",
                        transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(), trade, transfer);
            }
        }
        return payRecAgentSwift;
    }


    /**
     * Get BOTransfer field 'Receiver.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverSwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverSwift(trade, transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Payer.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerSwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverSwift(trade, transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Payer.Swift' or 'Receiver.Swift'.
     *
     * @param trade
     * @param transfer
     * @param type
     * @return
     */
    public static String getBOTransferPayerReceiverSwift(final Trade trade, final BOTransfer transfer, final String type) {
        String payerReceiverSwift = "";
        LegalEntity legalEntity = null;
        String propertyName = null;
        if (transfer.getPayReceiveType().equals(type)) {
            legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getProcessingOrg());
            propertyName = LegalEntity.PROCESSINGORG;
        } else {
            legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getExternalLegalEntityId());
            propertyName = LegalEntity.COUNTERPARTY;
        }
        String contactType = null;
        final Object objSdi = getSDI(transfer, type);
        if (objSdi != null) {
            if (objSdi instanceof ManualSDI) {
                final ManualSDI manualSdi = (ManualSDI) objSdi;
                final PartySDI agent = manualSdi.getBeneficiary();
                if (manualSdi.isBeneficiaryUnknown()) {
                    return agent.getCodeValue();
                }
                contactType = agent.getPartyContactType();
            } else {
                final SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) objSdi;
                contactType = sdi.getBeneficiaryContactType();
            }
        }
        payerReceiverSwift = getSWIFT(legalEntity.getId(), legalEntity.getCode(), contactType, propertyName,
                transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(), trade, transfer);
        return payerReceiverSwift;
    }


    /**
     * Get BOTransfer field 'Receiver.Agent.Account'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverAgentAccount(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverAgentAccount(trade, transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Payer.Agent.Account'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerAgentAccount(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverAgentAccount(trade, transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Payer.Agent.Account' or 'Receiver.Agent.Account'.
     *
     * @param trade
     * @param transfer
     * @param type
     * @return
     */
    public static String getBOTransferPayerReceiverAgentAccount(final Trade trade, final BOTransfer transfer, final String type) {
        final Object objSdi = getSDI(transfer, type);
        if (objSdi == null || objSdi instanceof ManualSDI) {
            return "";
        }
        final SDI sdi = (SDI) objSdi;
        final PartySDI agent = sdi.getAgent();
        if (agent == null) {
            return "";
        }
        return agent.getPartyAccountName();
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverIntermediarySwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediarySwift(trade, transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Payer.Intermediary.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerIntermediarySwift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediarySwift(trade, transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Swift' or 'Payer.Intermediary.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerReceiverIntermediarySwift(final Trade trade, final BOTransfer transfer, final String type) {
        final Object objSdi = getSDI(transfer, type);
        if (objSdi == null) {
            return "";
        }
        if (objSdi instanceof ManualSDI) {
            final ManualSDI manualSdi = (ManualSDI) objSdi;
            final PartySDI intermediary = manualSdi.getIntermediary();
            if (intermediary == null) {
                return "";
            }
            if (manualSdi.isIntermediary1Unknown()) {
                return intermediary.getCodeValue();
            }
            return getSWIFT(intermediary.getPartyId(), intermediary.getPartyCode(), intermediary.getPartyContactType(),
                    intermediary.getPartyRole(), transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(),
                    trade, transfer);
        }
        final SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) objSdi;
        return getSWIFT(sdi.getIntermediaryId(), sdi.getIntermediaryName(), sdi.getIntermediaryContactType(), "Agent",
                transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(), trade, transfer);
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary2.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverIntermediary2Swift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediary2Swift(trade, transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Account'.
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverIntermediary2Account(final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediary2Account(transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Payer.Intermediary2.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerIntermediary2Swift(final Trade trade, final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediary2Swift(trade, transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary2.Swift' or 'Payer.Intermediary2.Swift'.
     *
     * @param trade
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerReceiverIntermediary2Swift(final Trade trade, final BOTransfer transfer, final String type) {
        final Object objSdi = getSDI(transfer, type);
        if (objSdi == null) {
            return "";
        }
        if (objSdi instanceof ManualSDI) {
            final ManualSDI manualSdi = (ManualSDI) objSdi;
            final PartySDI intermediary2 = manualSdi.getIntermediary2();
            if (intermediary2 == null) {
                return "";
            }
            if (manualSdi.isIntermediary2Unknown()) {
                return intermediary2.getCodeValue();
            }
            return getSWIFT(intermediary2.getPartyId(), intermediary2.getPartyCode(), intermediary2.getPartyContactType(),
                    intermediary2.getPartyRole(), transfer.getProductType(), transfer.getProcessingOrg(),
                    transfer.getValueDate(), trade, transfer);
        }
        final SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) objSdi;
        return getSWIFT(sdi.getIntermediary2Id(), null, sdi.getIntermediary2ContactType(), "Agent",
                transfer.getProductType(), transfer.getProcessingOrg(), transfer.getValueDate(), trade, transfer);
    }


    /**
     * Get BOTransfer field 'Payer.Inst'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerInst(final BOTransfer transfer) {
        return getBOTransferPayerReceiverInst(transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Receiver.Inst'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverInst(final BOTransfer transfer) {
        return getBOTransferPayerReceiverInst(transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Payer.Inst' or 'Receiver.Inst'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerReceiverInst(final BOTransfer transfer, final String type) {
        String payerReceiverInst = "";
        final Object objSdi = getSDI(transfer, type);
        if (objSdi instanceof ManualSDI) {
            final ManualSDI manualSdi = (ManualSDI) objSdi;
            payerReceiverInst = manualSdi.toString();
        } else {
            final SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) objSdi;
            if (sdi != null) {
                payerReceiverInst = sdi.getDescription();
            }
        }
        return payerReceiverInst;
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Account' or 'Receiver.Intermediary.Account'.
     *
     * @param transfer
     * @param type
     * @return
     */
    public static String getBOTransferPayerReceiverIntermediaryAccount(final BOTransfer transfer, final String type) {
        final Object objSdi = getSDI(transfer, type);
        if (objSdi == null || objSdi instanceof ManualSDI) {
            return "";
        }
        final SDI sdi = (SDI) objSdi;
        final PartySDI intermediary = sdi.getIntermediary();
        return (intermediary != null) ? intermediary.getPartyAccountName() : "";
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Account' or 'Receiver.Intermediary.Account'.
     *
     * @param transfer
     * @param type
     * @return
     */
    public static String getBOTransferPayerReceiverIntermediary2Account(final BOTransfer transfer, final String type) {
        final Object objSdi = getSDI(transfer, type);
        if (objSdi == null || objSdi instanceof ManualSDI) {
            return "";
        }
        final SDI sdi = (SDI) objSdi;
        final PartySDI intermediary2 = sdi.getIntermediary2();
        return (intermediary2 != null) ? intermediary2.getPartyAccountName() : "";
    }


    /**
     * Get BOTransfer field 'Payer.Intermediary.Account'.
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerIntermediaryAccount(final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediaryAccount(transfer, PHConstants.XFER_TYPE_PAY);
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Account'.
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverIntermediaryAccount(final BOTransfer transfer) {
        return getBOTransferPayerReceiverIntermediaryAccount(transfer, PHConstants.XFER_TYPE_RECEIVE);
    }


    /**
     * Get BOTransfer field 'Receiver.Full Name'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverFullName(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_RECEIVE);
        return le == null ? null : le.getName();
    }


    /**
     * Get BOTransfer field 'Payer.Full Name'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerFullName(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_PAY);
        return le == null ? null : le.getName();
    }


    /**
     * Get BOTransfer field 'Receiver.Code' or 'Receiver.Short Name'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverCode(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_RECEIVE);
        return le == null ? null : le.getCode();
    }


    /**
     * Get BOTransfer field 'Payer.Code' or 'Payer.Short Name'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerCode(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_PAY);
        return le == null ? null : le.getCode();
    }


    /**
     * Get BOTransfer field 'Receiver.Country'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferReceiverCountry(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_RECEIVE);
        return le == null ? null : le.getCountry();
    }


    /**
     * Get BOTransfer field 'Payer.Country'
     *
     * @param transfer
     * @return
     */
    public static String getBOTransferPayerCountry(final BOTransfer transfer) {
        final LegalEntity le = getLegalEntity(transfer, PHConstants.XFER_TYPE_PAY);
        return le == null ? null : le.getCountry();
    }


    /**
     * Get LegalEntity.
     *
     * @param xfer
     * @param payReceive
     * @return
     */
    public static LegalEntity getLegalEntity(final BOTransfer xfer, final String payReceive) {
        if (!Util.isEmpty(payReceive) && xfer != null) {
            final int legalEntityId = (payReceive.equals(xfer.getPayReceiveType())) ? xfer.getInternalLegalEntityId() : xfer.getExternalLegalEntityId();
            return BOCache.getLegalEntity(DSConnection.getDefault(), legalEntityId);
        } else {
            return null;
        }
    }


    /**
     * Get SDI.
     *
     * @param transfer
     * @param payReceive
     * @return
     */
    public static Object getSDI(final BOTransfer transfer, final String payReceive) {
        if (!transfer.getPayReceiveType().equals(payReceive) && transfer.isManualSDI()) {
            return BOCache.getManualSDI(DSConnection.getDefault(), transfer.getManualSDId());
        } else {
            int sdiId;
            int version;
            if (transfer.getPayReceiveType().equals(payReceive)) {
                sdiId = transfer.getInternalSettleDeliveryId();
                version = transfer.getIntSDIVersion();
            } else {
                sdiId = transfer.getExternalSettleDeliveryId();
                version = transfer.getExtSDIVersion();
            }
            return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId, version);
        }
    }


    /**
     * Get Swift.
     *
     * @param leId
     * @param name
     * @param contactType
     * @param role
     * @param productType
     * @param poId
     * @param valueDate
     * @param trade
     * @param transfer
     * @param dsCon
     * @return
     */
    public static String getSWIFT(final int leId, final String name, final String contactType, final String role, final String productType,
                                  final int poId, final JDate valueDate, final Trade trade, final BOTransfer transfer, final DSConnection dsCon) {
        final LegalEntity le = BOCache.getLegalEntity(dsCon, leId);
        if (le == null) {
            return name;
        } else {
            final LEContact leC = BOCache.getContact(dsCon, role, le, contactType, productType, poId, valueDate, trade, transfer);
            return leC != null ? leC.getSwift() : name;
        }
    }


    /**
     * Get Swift.
     *
     * @param leId
     * @param name
     * @param contactType
     * @param role
     * @param productType
     * @param poId
     * @param valueDate
     * @param trade
     * @param transfer
     * @return
     */
    public static String getSWIFT(final int leId, final String name, final String contactType, final String role, final String productType,
                                  final int poId, final JDate valueDate, final Trade trade, final BOTransfer transfer) {
        return getSWIFT(leId, name, contactType, role, productType, poId, valueDate, trade, transfer, getDSCon());
    }


    /**
     * Checks if is an IBAN.
     *
     * @param iban
     * @return
     */
    public static boolean isIBAN(final String iban) {
        if (iban == null || iban.length() < 5 || iban.length() > 34) {
            return false;
        }
        // Checks if the second two characters are numbers
        final String numbers = iban.substring(2, 4);
        try {
            Double.parseDouble(numbers);
        } catch (NumberFormatException nfe) {
            return false;
        }
        // Check the first two letters of the country
        final String countryCode = iban.substring(0, 2);
        return null != BOCache.getCountryByISO(getDSCon(), countryCode);
    }


    /**
     * Get PricingEnv.
     *
     * @param ds
     * @return
     */
    public static PricingEnv getPricingEnv(final DSConnection ds) {
        final String pricingEnvName = ds.getUserDefaults().getPricingEnvName();
        final JDatetime date = new JDatetime();
        final PricingEnv pricingEnv = PricingEnv.loadPE(pricingEnvName, date);
        return pricingEnv;
    }


    /**
     * Get the BOTransfer attribute 'UETR'.
     *
     * @param boTransfer
     * @return
     */
    public static String getUUID(final BOTransfer boTransfer) {
        return (boTransfer != null) ? boTransfer.getAttribute(KeywordConstantsUtil.TRANSFER_ATTRIBUTE_UETR) : null;
    }


    /**
     * Get AdviceDocument using BOMessage Legacy.
     *
     * @param legacyBOMessage
     * @param pricingEnv
     * @param dsCon
     * @return
     */
    public static AdviceDocument getAdviceDocumentLegacy(final BOMessage legacyBOMessage, final PricingEnv pricingEnv, final DSConnection dsCon) {
        AdviceDocument doc = null;
        final PricingEnv env = pricingEnv != null ? pricingEnv : PaymentsHubUtil.getPricingEnv(dsCon);
        try {
            // Generate AdviceDocument
            doc = FormatterUtil.generate(env, legacyBOMessage, true, dsCon);
        } catch (final MessageFormatException e) {
            final String msg = String.format("Error generating legacy BOMessage [%s]", legacyBOMessage.getTemplateName());
            Log.error(PaymentsHubUtil.class, msg, e);
        }
        return doc;
    }


    /**
     * Get Legacy Message
     *
     * @param originalBOMessage
     * @param template
     * @return
     * @throws CloneNotSupportedException
     */
    public static BOMessage getLegacyMessage(final BOMessage originalBOMessage, final String template) throws CloneNotSupportedException {
        if (originalBOMessage == null) {
            return null;
        }
        // BOMessage cloned
        final BOMessage legacyBOMessage = (BOMessage) originalBOMessage.clone();
        legacyBOMessage.setTemplateName(template);
        legacyBOMessage.setMessageType(PHConstants.LEGACY_MESSAGE_TYPE);
        legacyBOMessage.setFormatType(PHConstants.LEGACY_FORMAT_TYPE);
        legacyBOMessage.setGateway(PHConstants.LEGACY_GATEWAY);
        legacyBOMessage.setAddressMethod(PHConstants.LEGACY_ADDRESS_METHOD);
        //Report that the origin is PH for the SWIFT formatter.
        setPHOrigin(legacyBOMessage);
        return legacyBOMessage;
    }


    /**
     * Get Legacy Template. Legacy Template Selector
     *
     * @param boMessage
     * @param template
     * @return
     */
    public static String getLegacyTemplateSelector(final BOMessage boMessage, final String template) {
        String legacyTemplate = template;
        // Check subAction CANCEL
        if (boMessage.getSubAction().equals(Action.CANCEL)) {
            legacyTemplate = getLegacyTemplateSubActionCancel(boMessage);
        }
        return legacyTemplate;
    }


    /**
     * Gets the template sub action cancel.
     *
     * @param boMessage the bomessage
     * @return the template sub action cancel
     */
    public static String getLegacyTemplateSubActionCancel(final BOMessage boMessage) {
        String template = "";
        final BOMessage linkedMsg = getBOMessageById(boMessage.getLinkedLongId());
        if (linkedMsg != null) {
            template = linkedMsg.getTemplateName();
            switch (template) {
                case PHConstants.MESSAGE_PH_FICCT: // MT103
                    template = PHConstants.LEGACY_TEMPLATE_MT192; // MT192
                    break;
                case PHConstants.MESSAGE_PH_FICT: // MT202
                case PHConstants.MESSAGE_PH_FICTCOV: // MT202COV
                case PHConstants.MESSAGE_PH_NTR: // MT210
                    template = PHConstants.LEGACY_TEMPLATE_MT292;
                    break;
                default:
                    template = "MTx92";
                    break;
            }
        }
        return template;
    }


    /**
     * Get Legacy Info
     *
     * @param legacyBOMessage
     * @param pricingEnv
     * @param dsCon
     * @return
     */
    public static String getLegacyInfo(final BOMessage legacyBOMessage, final PricingEnv pricingEnv, final DSConnection dsCon) {
        String legacyInfo = "";
        if (legacyBOMessage != null) {
            // Generate AdviceDocument Legacy
            final AdviceDocument doc = getAdviceDocumentLegacy(legacyBOMessage, pricingEnv, dsCon);
            if (doc != null) {
                final StringBuffer docStr = doc.getDocument();
                if (docStr != null) {
                    SwiftMessage.stripExtraInfo(docStr);
                    legacyInfo = docStr.toString();
                }
            }
        }
        return legacyInfo;
    }


    /**
     * Get Template Name without '-'.
     *
     * @param templateName
     * @return
     */
    public static String getTransformedTemplateName(final String templateName) {
        return templateName.replaceAll("-", "").toUpperCase();
    }


    /**
     * substring if the text exceeds the maximum allowed in column ATTR_VALUE of table
     * MESS_ATTRIBUTES.
     *
     * @param text
     * @return
     */
    public static String substringTextMessAttrValue(final String text) {
        return (text != null && text.length() > 250) ? text.substring(0, 250) : text;
    }


    /**
     * Get Settlement Methods accepted and its code from DomainValues.
     *
     * @param settMethod
     * @return
     */
    public static String getSettlementMethodCode(final String settMethod) {
        final String rst = DomainValues.comment(DN_PAYMENTS_HUB_SETTL_METHOD, settMethod);
        return (!Util.isEmpty(rst)) ? rst : "";
    }


    /**
     * Get TradeKeyword Partenon-IDCONTR.
     *
     * @param trade Checks if the trade is the far leg of a swap
     * @return True if is the far leg, false if not.
     */
    public static String getPartenonIdContr(final Trade trade) {
        String partenonIdContr = trade.getKeywordValue(PHConstants.TRADE_KEYWORD_PARTENON_ACCOUNTING_ID);
        return Util.isEmpty(partenonIdContr) ? "" : partenonIdContr;
    }


    /**
     * Get Trade by Id
     *
     * @param tradeId
     * @return
     */
    public static Trade getTrade(long tradeId) {
        Trade trade = null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
        } catch (CalypsoServiceException e) {
            Log.error(PaymentsHubUtil.class, "Error loading Trade: " + e);
        }
        return trade;
    }


    /**
     * Get BOTransfer by Id. Build custom SDIs if transfer attributes
     * PH Internal SDI id and PH External SDI id is not empty.
     * These attributes will be reported with the rule SantUpdateSDIRepoTripartyTransferRule.
     *
     * @param transferId the current transfer id
     * @return the BOTransfer with custom SDI (if not empty)
     */
    public static BOTransfer getBOTransfer(final long transferId) {
        BOTransfer xfer = null;
        try {
            xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(transferId);
            buildPHCustomSDIs(xfer);
        } catch (final Exception e) {
            Log.error(PaymentsHubUtil.class, "Could not get transfer", e);
        }
        return xfer;
    }


    /**
     * Get DeliveryFlag by TransferType.
     *
     * @param xfer
     * @param dsCon
     * @return
     */
    public static boolean getDeliveryFlagByXferType(final BOTransfer xfer, final DSConnection dsCon) {
        // Get External SDI Id (Cpty)
        final int sdiExternalId = xfer.getExternalSettleDeliveryId();
        // Get BOTransfer Type
        final String xferType = xfer.getPayReceiveType();
        // Get DeliveryFlag value by TransferType
        return PHConstants.XFER_TYPE_RECEIVE.equals(xferType) ? getDeliveryFlagXferReceive(sdiExternalId, dsCon) : getDeliveryFlagXferPay(sdiExternalId, dsCon);
    }


    /**
     * Is DeliveryFlag when BOTransfer is RECEIVE.
     *
     * @param sdiId
     * @param dsCon
     * @return
     */
    public static boolean getDeliveryFlagXferReceive(final int sdiId, final DSConnection dsCon) {
        final String attrSendMsgForReceipt = getSDIAttribute(sdiId, PHConstants.SDI_ATTRIBUTE_SEND_MSG_RECEIPT);
        return Util.isTrue(attrSendMsgForReceipt);
    }


    /**
     * Is DeliveryFlag when BOTransfer is PAY.
     *
     * @param sdiId
     * @param dsCon
     * @return
     */
    public static boolean getDeliveryFlagXferPay(final int sdiId, final DSConnection dsCon) {
        boolean deliveryFlag = false;
        final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        if (sdi != null) {
            // check Agent
            final boolean agentMsgToParty = sdi.getAgent() != null && sdi.getAgent().getMessageToParty();
            // check Intermediary (1 & 2)
            boolean intermediaryMsgToParty = sdi.getIntermediary() != null && sdi.getIntermediary().getMessageToParty();
            if (!intermediaryMsgToParty) {
                intermediaryMsgToParty = sdi.getIntermediary2() != null && sdi.getIntermediary2().getMessageToParty();
            }
            deliveryFlag = agentMsgToParty || intermediaryMsgToParty;
        }
        return deliveryFlag;
    }


    /**
     * Get SDI Attribute.
     *
     * @param sdiId
     * @param attributeName
     * @return
     */
    public static String getSDIAttribute(final int sdiId, final String attributeName) {
        String debug = "";
        // Get SDI
        final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(getDSCon(), sdiId);
        if (sdi != null) {
            // Get SDI attribute
            return sdi.getAttribute(attributeName);
        } else {
            debug = String.format("The SDI [%d] does not exist.", sdiId);
            Log.error(PaymentsHubUtil.class, debug);
        }
        return null;
    }


    /**
     * Check if the BOMEssage has the "HasCoverMessage" attribute.
     *
     * @return
     */
    public static boolean hasCoverMessage(final BOMessage boMessage) {
        final String value = boMessage.getAttribute(PHConstants.MSG_ATTRIBUTE_COVER_MESSAGE);
        return (!Util.isEmpty(value)) && Util.isTrue(value.toUpperCase());
    }


    /**
     * Check if is Mutuactivo
     *
     * @param xfer
     * @return
     */
    public static boolean isMutuactivo(final BOTransfer xfer) {
        // Check if is Mutuactivo
        final String isMutuactivoAttr = getSDIAttribute(xfer.getReceiverSDId(), PHConstants.SDI_ATTRIBUTE_IS_MUTUACTIVO);
        return Util.isTrue(isMutuactivoAttr);
    }


    /**
     * Check if CounterParty is Financial
     *
     * @param trade
     * @param xfer
     * @param dsCon
     * @return
     */
    public static boolean isCounterPartyFinancial(final Trade trade, final BOTransfer xfer, final DSConnection dsCon) {
        // Get CounterParty
        LegalEntity cpty = null;
        if (trade != null) {
            cpty = trade.getCounterParty();
        } else {
            cpty = xfer != null ? BOCache.getLegalEntity(dsCon, xfer.getExternalLegalEntityId()) : null;
        }
        // Get if is Financial
        final boolean isFinancial = cpty != null ? cpty.getClassification() : false;
        return isFinancial;
    }


    /**
     * Check if has Intermediary
     *
     * @param sdiId
     * @param dsCon
     * @return
     */
    public static boolean hasIntermediary(final int sdiId, final DSConnection dsCon) {
        // Get INTERMEDIARY
        PartySDI intermediary = null;
        // Get SDI
        final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(getDSCon(), sdiId);
        if (sdi != null) {
            // Get Intermediary
            intermediary = sdi.getIntermediary();
        }
        return intermediary != null;
    }


    /**
     * Check if has Intermediary2
     *
     * @param sdiId
     * @param dsCon
     * @return
     */
    public static boolean hasIntermediary2(final int sdiId, final DSConnection dsCon) {
        // Get INTERMEDIARY2
        PartySDI intermediary2 = null;
        // Get SDI
        final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(getDSCon(), sdiId);
        if (sdi != null) {
            // Get Intermediary2
            intermediary2 = sdi.getIntermediary2();
        }
        return intermediary2 != null;
    }


    /**
     * Map original value using PHValidations
     *
     * @param originalValue
     * @param fieldNameValue
     * @return
     */
    public static String mapPaymentsHubValues(final String originalValue, final String fieldNameValue) {
        final Map<String, String> map = DomainValues.valuesComment(DN_PH_VALIDATION.concat(fieldNameValue));
        final StringBuilder domainValue = new StringBuilder();
        if (!Util.isEmpty(map)) {
            final Optional<String> comment = map.values().stream().filter(c -> c.contains(originalValue)).findFirst();
            if (comment.isPresent()) {
                map.forEach((k, v) -> {
                    if (domainValue.length() == 0 && v.equals(comment.get())) {
                        domainValue.append(k);
                    }
                });
            }
        }
        return !Util.isEmpty(domainValue.toString()) ? domainValue.toString() : originalValue;
    }


    /**
     * Get Pattern Log
     *
     * @return
     */
    public static String getLogPatternPrefix() {
        return LOG_PATTERN;
    }


    /**
     * Get BOMessage using linked TransferId.
     *
     * @param templateToSearch
     * @param boMessage
     * @param dsCon
     * @return
     */
    public static BOMessage getPaymentsHubLinkedMessageByTransfer(final String templateToSearch, final BOMessage boMessage, final DSConnection dsCon) {
        BOMessage boMessageFound = null;
        if (boMessage != null) {
            // Get 'MessageRef' attribute
            final String msgRefAttr = boMessage.getAttribute(KeywordConstantsUtil.MSG_ATTRIBUTE_MESSAGE_REF);
            if (!Util.isEmpty(msgRefAttr)) {
                // Get BOMessage directly
                boMessageFound = getBOMessageById(Long.parseLong(msgRefAttr));
            } else {
                // From the DataBase, retrieves the messages in the same transfer that the boMessage.
                final long xferId = boMessage.getTransferLongId();
                try {
                    final MessageArray messageArray = dsCon.getRemoteBO().getTransferMessages(xferId);
                    if ((null != messageArray) && (messageArray.size() > 0)) {
                        final BOMessage[] msgArray = messageArray.getMessages();
                        for (int i = 0; (i < msgArray.length) && (null != msgArray[i]); i++) {
                            // Checks if the template message = templateToSearch
                            final String templateNameMsg = msgArray[i].getTemplateName();
                            if (templateToSearch.equals(templateNameMsg)) {
                                // Get BOMessage
                                boMessageFound = (BOMessage) msgArray[i].clone();
                            }
                        }
                    }
                } catch (final CloneNotSupportedException e) {
                    Log.error(LOG_CATEGORY, "Error cloning BOMessage.", e);
                } catch (final RemoteException e) {
                    Log.error(LOG_CATEGORY, "Error retrieving messages from the DataBase with the transfer ID " + xferId, e);
                }
            }
        }
        return boMessageFound;
    }


    public static boolean isCreatable(String str) {
        if (Util.isEmpty(str)) {
            return false;
        } else {
            char[] chars = str.toCharArray();
            int sz = chars.length;
            boolean hasExp = false;
            boolean hasDecPoint = false;
            boolean allowSigns = false;
            boolean foundDigit = false;
            int start = chars[0] != '-' && chars[0] != '+' ? 0 : 1;
            int i;
            if (sz > start + 1 && chars[start] == '0') {
                if (chars[start + 1] == 'x' || chars[start + 1] == 'X') {
                    i = start + 2;
                    if (i == sz) {
                        return false;
                    }
                    while (i < chars.length) {
                        if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F')) {
                            return false;
                        }
                        ++i;
                    }
                    return true;
                }
                if (Character.isDigit(chars[start + 1])) {
                    for (i = start + 1; i < chars.length; ++i) {
                        if (chars[i] < '0' || chars[i] > '7') {
                            return false;
                        }
                    }
                    return true;
                }
            }
            --sz;
            for (i = start; i < sz || i < sz + 1 && allowSigns && !foundDigit; ++i) {
                if (chars[i] >= '0' && chars[i] <= '9') {
                    foundDigit = true;
                    allowSigns = false;
                } else if (chars[i] == '.') {
                    if (hasDecPoint || hasExp) {
                        return false;
                    }
                    hasDecPoint = true;
                } else if (chars[i] != 'e' && chars[i] != 'E') {
                    if (chars[i] != '+' && chars[i] != '-') {
                        return false;
                    }
                    if (!allowSigns) {
                        return false;
                    }
                    allowSigns = false;
                    foundDigit = false;
                } else {
                    if (hasExp) {
                        return false;
                    }
                    if (!foundDigit) {
                        return false;
                    }
                    hasExp = true;
                    allowSigns = true;
                }
            }
            if (i < chars.length) {
                if (chars[i] >= '0' && chars[i] <= '9') {
                    return true;
                } else if (chars[i] != 'e' && chars[i] != 'E') {
                    if (chars[i] == '.') {
                        return !hasDecPoint && !hasExp ? foundDigit : false;
                    } else if (!allowSigns && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
                        return foundDigit;
                    } else if (chars[i] != 'l' && chars[i] != 'L') {
                        return false;
                    } else {
                        return foundDigit && !hasExp && !hasDecPoint;
                    }
                } else {
                    return false;
                }
            } else {
                return !allowSigns && foundDigit;
            }
        }
    }


    /**
     * Get the BOMessage by Id.
     *
     * @param boMessageId
     * @return
     */
    public static BOMessage getBOMessageById(final long boMessageId) {
        BOMessage xfer = null;
        final RemoteBackOffice rbo = DSConnection.getDefault().getRemoteBackOffice();
        try {
            xfer = RemoteAPI.getMessage(rbo, boMessageId);
        } catch (final RemoteException e) {
            final String msg = String.format("Error retrieving the BOMessage [%s]", String.valueOf(boMessageId));
            Log.error(PaymentsHubUtil.class, msg, e);
        }
        return xfer;
    }


    /**
     * Get AdviceDocument as StringBuffer.
     *
     * @param doc
     * @return
     */
    public static StringBuffer getAdviceDocumentAsStringBuffer(final AdviceDocument doc) {
        StringBuffer buff = null;
        if (doc != null) {
            // Is not binary document
            if (!doc.getIsBinary()) {
                final StringBuffer docStr = doc.getDocument();
                buff = (docStr != null) ? new StringBuffer(docStr.toString()) : null;
            } else {
                // Is binary document
                final byte[] binaryDoc = doc.getBinaryDocument();
                final String binaryDocStr = (binaryDoc != null) ? new String(binaryDoc) : "";
                buff = (binaryDoc != null) ? new StringBuffer(binaryDocStr) : null;
            }
        }
        return buff;
    }


    /**
     * Get Latest AdviceDocument.
     *
     * @param messageId
     * @return
     */
    public static AdviceDocument getLatestAdviceDocument(final long messageId) {
        AdviceDocument doc = null;
        try {
            doc = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(messageId, new JDatetime());
        } catch (CalypsoServiceException e) {
            Log.error(PaymentsHubUtil.class, e.getCause());
        }
        return doc;
    }


    /**
     * Get Internal Settle Deilvery Instruction from BOTransfer.
     *
     * @param xfer
     * @param dsCon
     * @return
     */
    public static SettleDeliveryInstruction getInternalSettleDeliveryInstruction(final BOTransfer xfer, final DSConnection dsCon) {
        SettleDeliveryInstruction internalSdi = null;
        final int sdiId = xfer != null ? xfer.getInternalSettleDeliveryId() : 0;
        if (sdiId > 0) {
            internalSdi = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        }
        return internalSdi;
    }


    /**
     * Get External Settle Deilvery Instruction from BOTransfer.
     *
     * @param xfer
     * @param dsCon
     * @return
     */
    public static SettleDeliveryInstruction getExternalSettleDeliveryInstruction(final BOTransfer xfer, final DSConnection dsCon) {
        SettleDeliveryInstruction externalSdi = null;
        final int sdiId = xfer != null ? xfer.getExternalSettleDeliveryId() : 0;
        if (sdiId > 0) {
            externalSdi = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        }
        return externalSdi;
    }


    /**
     * Adds a new line to the given StringBuilder if it is not empty before
     * appending the new value.
     *
     * @param output   StringBuilder where the new value will be appended.
     * @param newValue Value to append to the output.
     */
    public static void appendNewLine(final StringBuilder output, final String newValue) {
        if ((output != null) && !Util.isEmpty(newValue)) {
            if (!Util.isEmpty(output.toString())) {
                output.append('\n');
            }
            output.append(newValue);
        }
    }


    /**
     * Modify the legacy info, change "\r\n" instead of "\n"
     *
     * @param legacyInfo String with the value of the legacy.
     */
    public static String adaptNewLineToLegacyInfo(String legacyInfo) {
        legacyInfo = legacyInfo.replaceAll(PHConstants.PATTERN_TO_CHANGE, "\r\n");
        return legacyInfo.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n").replaceAll("\r\n\r\n", "\r\n");
    }


    /**
     * Check if transfer has custom PH SDIs. If PH External SDI id and PH Internal SDI id.
     * These attributes are informed in the rule SantUpdateSDIRepoTripartyTransferRule.
     *
     * @param transfer the current transfer
     * @return true if has custom SDI PH
     */
    public static boolean hasCustomSDIs(BOTransfer transfer) {
        return transfer != null && !Util.isEmpty(transfer.getAttribute(PH_EXTERNAL_SDI_ID))
                && !Util.isEmpty(transfer.getAttribute(PH_INTERNAL_SDI_ID));
    }

    /**
     * Build custom SDI form PH message, if have custom SDI
     *
     * @param transfer the current transfer
     * @return true if build successfully
     */
    public static boolean buildPHCustomSDIs(BOTransfer transfer) {
        if (hasCustomSDIs(transfer)) {
            SettleDeliveryInstruction internalSDI = BOCache.getSettleDeliveryInstruction(getDSCon(),
                    Integer.parseInt(transfer.getAttribute(PH_INTERNAL_SDI_ID)));
            SettleDeliveryInstruction externalSDI = BOCache.getSettleDeliveryInstruction(getDSCon(),
                    Integer.parseInt(transfer.getAttribute(PH_EXTERNAL_SDI_ID)));
            if (internalSDI != null && externalSDI != null) {
                transfer.setInternalSettleDeliveryId(internalSDI.getId());
                transfer.setExternalSettleDeliveryId(externalSDI.getId());
                transfer.setSettlementMethod(transfer.getAttribute(PH_SETTLEMENT_METHOD));
                transfer.setInternalAgentId(internalSDI.getAgentId());
                transfer.setInternalLegalEntityId(internalSDI.getLegalEntityId());
                transfer.setExternalAgentId(externalSDI.getAgentId());
                transfer.setExternalLegalEntityId(externalSDI.getLegalEntityId());
                transfer.setIntSDIVersion(internalSDI.getVersion());
                transfer.setExtSDIVersion(externalSDI.getVersion());
                transfer.setSDIInfos(internalSDI, externalSDI, BOCache.getLegalEntity(getDSCon(), transfer.getProcessingOrg()), getDSCon());
                return true;
            }
        }
        return false;
    }

    /**
     * Check if message is PH for generate legacy info in SWIFT messages
     *
     * @param message the current message
     * @return true if is PH origin
     */
    public static boolean isPHOrigin(BOMessage message) {
        return message != null && !Util.isEmpty(message.getAttribute(PH_ORIGIN))
                && "true".equals(message.getAttribute(PH_ORIGIN));
    }

    /**
     * Se message origin PH
     *
     * @param message the current message
     */
    public static void setPHOrigin(BOMessage message) {
        if (message != null) {
            message.setAttribute(PH_ORIGIN, "true");
        }
    }

}

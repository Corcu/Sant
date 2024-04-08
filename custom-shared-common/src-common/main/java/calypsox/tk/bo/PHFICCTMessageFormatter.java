package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.swift.formatter.MT103SWIFTFormatter;
import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.ManualSDI;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;


public class PHFICCTMessageFormatter extends PAYMENTHUB_PAYMENTMSGMessageFormatter {


    private static final String MESSAGE_TYPE = "Single Customer Credit Transfer";
    /**
     * Fixed text
     */
    private static final String TEXT = "/BNF/IN COVER OF DIRECT SWIFT";
    /**
     * Separator between legacy MT103 and MT202COV
     */
    private static final Object SEPARATOR_LEGACY_MT103_MT202COV = "\r\n";


    public PHFICCTMessageFormatter() {
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_MESSAGE_TYPE(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return MESSAGE_TYPE;
    }


    // ----------------------------------------------------------------- //
    // ------------------ CREDIT TRANSFER INSTRUCTION ------------------ //
    // ----------------------------------------------------------------- //


    /**
     * Get instrForNxtAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INSTR_FOR_NEXT_AGENT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String instrForNxtAgt = null;
        if (xfer != null) {
            instrForNxtAgt = getField72(boMessage, xfer, trade, dsCon);
        }
        return !Util.isEmpty(instrForNxtAgt) ? instrForNxtAgt : null;
    }


    /**
     * Get Field 72 as MT103.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    private String getField72(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return new MT103SWIFTFormatter()
                .parseSANT_ADDITIONAL_INFO(boMessage,trade,null,null,null,xfer,null,dsCon);
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
    @Override
    public List<String> parseSANT_REMITTANCE_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        final List<String> result = new ArrayList<String>();
        final String defaultField70 = parseREMITTANCE_INFO(boMessage, xfer, trade, dsCon);
        final String customField70 = ""; //SantanderSwiftUtil.getInstance().parseADDITIONAL_INFO(boMessage, trade, null, null, null, xfer, null, dsCon);
        if (!Util.isEmpty(defaultField70)) {
            result.add(defaultField70);
        }
        if (!Util.isEmpty(customField70)) {
            result.add(customField70);
        }
        return result;
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
    @Override
    public String parseSANT_SENDS_CORRESPDNT_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (PaymentsHubUtil.isCHAPS(xfer)) {
            return null;
        }
        // Get Payer.Agent.Swift
        final String payerAgentSwift = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
        return !Util.isEmpty(payerAgentSwift) ? payerAgentSwift : null;
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
    @Override
    public String parseSANT_SENDS_CORRESPDNT_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String payerAgentAccount = null;
        if (!PaymentsHubUtil.isCHAPS(xfer) && !PaymentsHubUtil.isTARGET2(xfer)) {
            // Get Payer.Agent.Account
            payerAgentAccount = PaymentsHubUtil.getBOTransferPayerAgentAccount(trade, xfer);
        }
        return !Util.isEmpty(payerAgentAccount) ? payerAgentAccount : null;
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
    @Override
    public String parseSANT_RECVS_CORRESPDNT_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (PaymentsHubUtil.hasCoverMessage(boMessage)) {
            // Get Receiver.Intermediary.Swift
            final String receiverIntermSwift = PaymentsHubUtil.getBOTransferReceiverIntermediarySwift(trade, xfer);
            return !Util.isEmpty(receiverIntermSwift) ? receiverIntermSwift : null;
        }
        return null;
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
    @Override
    public String parseSANT_RECVS_CORRESPDNT_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // If has COV
        if (PaymentsHubUtil.hasCoverMessage(boMessage)) {
            // Get External SDI
            final int sdiId = xfer != null ? xfer.getExternalSettleDeliveryId() : 0;
            // Get SDI attribute "Intermediary"
            final String attrIntermediary = PaymentsHubUtil.getSDIAttribute(sdiId, PHConstants.SDI_ATTRIBUTE_INTERMEDIARY);
            if (!Util.isEmpty(attrIntermediary)) {
                return "/".concat(attrIntermediary);
            } else {
                // Get Receiver.Intermediary.Account
                final String receiverIntermAgtAcc = PaymentsHubUtil.getBOTransferReceiverIntermediaryAccount(xfer);
                return !Util.isEmpty(receiverIntermAgtAcc) ? receiverIntermAgtAcc : null;
            }
        }
        // If has not COV
        return null; // N/A
    }


    /**
     * Get dbtr -> From Internal SDI, the Beneficiary Name
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_FULL_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String senderFullName = null;
        if (xfer != null) {
            // Get BOTransfer SDI Internal
            final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, xfer.getInternalSettleDeliveryId());
            if (sdi != null) {
                int leId = sdi.getLegalEntityId();
                if(leId>0) {
                    final LegalEntity legalEntity = BOCache.getLegalEntity(dsCon, leId);
                    if(legalEntity != null){
                        senderFullName = legalEntity.getName();
                    }
                }
            }
        }
        return !Util.isEmpty(senderFullName) ? senderFullName : null;
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
    @Override
    public String parseSANT_DEBTOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (!PaymentsHubUtil.isTARGET2(xfer)) {
            return super.parseSANT_DEBTOR_ACCOUNT_BICFI(boMessage, xfer, trade, dsCon);
        } else {
            // Get SDI (PO) Attribute Beneficiary
            final int sdiId = xfer != null ? xfer.getInternalSettleDeliveryId() : 0;
            // Get Beneficiary attribute
            final String beneficiary = PaymentsHubUtil.getSDIAttribute(sdiId, PHConstants.SDI_ATTRIBUTE_BENEFICIARY);
            return !Util.isEmpty(beneficiary) ? beneficiary : null;
        }
    }


    /**
     * Get dbtr/instId/pstlAdr/strtNm: Internal SDI Beneficiary Contact has not Swift AddressCode,
     * then use BOTransfer 'Payer.Code' and get the MailingAddres from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_STREET_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get 'Payer.Code'
        final String payerCode = PaymentsHubUtil.getBOTransferPayerCode(xfer);
        // Get LegalEntity
        final LegalEntity le = BOCache.getLegalEntity(dsCon, payerCode);
        // Get LEContact 'Default'
        final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
        return contact != null && !Util.isEmpty(contact.getMailingAddress()) ? contact.getMailingAddress() : null;

    }


    /**
     * Get dbtr/instId/pstlAdr/pstCd: Internal SDI Beneficiary Contact has not Swift AddressCode, then
     * use BOTransfer 'Payer.Code' and get the ZipCode from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_POSTAL_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get 'Payer.Code'
        final String payerCode = PaymentsHubUtil.getBOTransferPayerCode(xfer);
        // Get LegalEntity
        final LegalEntity le = BOCache.getLegalEntity(dsCon, payerCode);
        // Get LEContact 'Default'
        final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
        return contact != null && !Util.isEmpty(contact.getZipCode()) ? contact.getZipCode() : null;

    }


    /**
     * Get dbtr/instId/pstlAdr/twnNm: Internal SDI Beneficiary Contact has not Swift AddressCode, then
     * use BOTransfer 'Payer.Code' and get the CityName from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get SWIFT AddressCode in the Internal SDI
        // Get 'Payer.Code'
        final String payerCode = PaymentsHubUtil.getBOTransferPayerCode(xfer);
        // Get LegalEntity
        final LegalEntity le = BOCache.getLegalEntity(dsCon, payerCode);
        // Get LEContact 'Default'
        final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
        return contact != null && !Util.isEmpty(contact.getCityName()) ? contact.getCityName() : null;
    }


    /**
     * Get dbtr/instId/pstlAdr/ctry: Internal SDI Beneficiary Contact has not Swift AddressCode, then
     * use BOTransfer 'Payer.Country' and get the Country ISO Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_COUNTRY(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        // Get Country
        final String payerCountry = PaymentsHubUtil.getBOTransferPayerCountry(xfer);
        final Country country = BOCache.getCountry(dsCon, payerCountry);
        return (country != null && !Util.isEmpty(country.getISOCode())) ? country.getISOCode() : null;
    }


    /**
     * Get Debtor Agent Bicfi. dbtrAgt: bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (!PaymentsHubUtil.hasCoverMessage(boMessage) && PaymentsHubUtil.isCHAPS(xfer)) {
            // Get Payer.Swift
            final String payerSwift = PaymentsHubUtil.getBOTransferPayerSwift(trade, xfer);
            return !Util.isEmpty(payerSwift) ? payerSwift : null;
        }
        return null;
    }


    /**
     * Get cdtr:instId:pstlAdr:strtNm: External SDI Beneficiary Contact has not Swift AddressCode,
     * then use BOTransfer 'Receiver.Code' and get the MailingAddres from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_STREET_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String streetName = null;
        // Get SWIFT AddressCode in the External SDI
        final String swift = getSwiftAddressCodeExternalSdi(xfer, trade, dsCon);
        if (Util.isEmpty(swift)) {
            // Get 'Receiver.Code'
            final String receiverCode = PaymentsHubUtil.getBOTransferReceiverCode(xfer);
            // Get LegalEntity
            final LegalEntity le = BOCache.getLegalEntity(dsCon, receiverCode);
            // Get LEContact 'Default'
            final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
            streetName = contact != null && !Util.isEmpty(contact.getMailingAddress()) ? contact.getMailingAddress() : null;
        }
        return streetName;
    }


    /**
     * Get cdtr:instId:pstlAdr:pstCd: External SDI Beneficiary Contact has not Swift AddressCode, then
     * use BOTransfer 'Receiver.Code' and get the ZipCode from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_POSTAL_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String zipCode = null;
        // Get SWIFT AddressCode in the External SDI
        final String swift = getSwiftAddressCodeExternalSdi(xfer, trade, dsCon);
        if (Util.isEmpty(swift)) {
            // Get 'Receiver.Code'
            final String receiverCode = PaymentsHubUtil.getBOTransferReceiverCode(xfer);
            // Get LegalEntity
            final LegalEntity le = BOCache.getLegalEntity(dsCon, receiverCode);
            // Get LEContact 'Default'
            final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
            zipCode = contact != null && !Util.isEmpty(contact.getZipCode()) ? contact.getZipCode() : null;
        }
        return zipCode;
    }


    /**
     * Get cdtr: city -> : External SDI Beneficiary Contact has not Swift AddressCode, then use
     * BOTransfer 'Receiver.Code' and get the CityName from LEContact 'Default'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String cityName = null;
        // Get SWIFT AddressCode in the External SDI
        final String swift = getSwiftAddressCodeExternalSdi(xfer, trade, dsCon);
        if (Util.isEmpty(swift)) {
            // Get 'Receiver.Code'
            final String receiverCode = PaymentsHubUtil.getBOTransferReceiverCode(xfer);
            // Get LegalEntity
            final LegalEntity le = BOCache.getLegalEntity(dsCon, receiverCode);
            // Get LEContact 'Default'
            final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
            cityName = contact != null && !Util.isEmpty(contact.getCityName()) ? contact.getCityName() : null;
        }
        return cityName;
    }


    /**
     * Get cdtr: country -> External SDI Beneficiary Contact has not Swift AddressCode, then use
     * BOTransfer 'Receiver.Country' and get the Country ISO Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_COUNTRY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String countryCode = null;
        // Get SWIFT AddressCode in the External SDI
        final String swift = getSwiftAddressCodeExternalSdi(xfer, trade, dsCon);
        if (Util.isEmpty(swift)) {
            // Get 'Receiver.Country'
            final String receiverCountry = PaymentsHubUtil.getBOTransferReceiverCountry(xfer);
            // Get Country
            final Country country = BOCache.getCountry(dsCon, receiverCountry);
            countryCode = (country != null && !Util.isEmpty(country.getISOCode())) ? country.getISOCode() : null;
        }
        return countryCode;
    }


    /**
     * Get cdtr bicfi -> External SDI Beneficiary Contact has Swift AddressCode, then use BOTransfer
     * 'Receiver.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get SWIFT AddressCode in the External SDI
        final String swift = getSwiftAddressCodeExternalSdi(xfer, trade, dsCon);
        // Check Swift. // Get 'Receiver.Swift'
        return !Util.isEmpty(swift) ? PaymentsHubUtil.getBOTransferReceiverSwift(trade, xfer) : null;
    }


    /**
     * Get cdtr name -> External SDI Beneficiary Contact has not Swift AddressCode, then use SDI
     * Beneficiary Name
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
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
     * Get Mirror Account -> Check 'Receiver.Inst' and get from the last /.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_MIRROR_ACCOUNT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String mirrorAccount = null;
        // Get Receiver.Inst
        final String receverInst = PaymentsHubUtil.getBOTransferReceiverInst(xfer);
        if (!Util.isEmpty(receverInst)) {
            final int lastIndexOf = receverInst.lastIndexOf("/");
            mirrorAccount = receverInst.substring(lastIndexOf);
        }
        return mirrorAccount;
    }


    /**
     * Get intrmyAgt:bicfi
     * <p>
     * If it hasn't Cov: BOTransfer field 'Receiver.Intermediary.Swift' Otherwise, empty.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INTERMEDIARY_AGENT_BICFI(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return !PaymentsHubUtil.hasCoverMessage(boMessage) ? super.parseSANT_INTERMEDIARY_AGENT_BICFI(boMessage, xfer, trade, dsCon) : null;
    }


    /**
     * CreditTransfersInfo -> chrgBr: Get SDI External Attribute 'Details_of_Charge'. By Default, SHA.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CHARGE_TO_BEAR(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get SDI External Id
        final int sdiId = xfer.getExternalSettleDeliveryId();
        // Get SDI Attribute Details_of_Charges
        final String attr = PaymentsHubUtil.getSDIAttribute(sdiId, PHConstants.SDI_ATTRIBUTE_DETAILS_OF_CHARGES);
        if (!Util.isEmpty(attr)) {
            // Map value using PaymentsHubValidation DV
            return PaymentsHubUtil.mapPaymentsHubValues(attr, PHConstants.CHARGE_TO_BEAR);
        } else {
            // By Default
            return PHConstants.CHARGE_TO_BEAR_DEFAULT;
        }
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // MT103 or MT103 and MT202COV
        final StringBuilder legacyInfo = new StringBuilder();
        final PricingEnv env = getPricingEnv() != null ? getPricingEnv() : PaymentsHubUtil.getPricingEnv(dsCon);
        try {
            // BOMessage legacy MT103
            // Get legacyInfo
            final String msgLegacyMt103 = getLegacyInfoMT103(boMessage, env, dsCon);
            legacyInfo.append(msgLegacyMt103);
            // BOMessage legacy MT202COV
            if (PaymentsHubUtil.hasCoverMessage(boMessage)) {
                // Get LegacyInfo MT202COV
                final String msgLegacyMt202Cov = getLegacyInfoMT202COV(boMessage, env, dsCon);
                // Add separator between legacy MT103 and legacy MT202COV
                if (!Util.isEmpty(msgLegacyMt202Cov)) {
                    legacyInfo.append(SEPARATOR_LEGACY_MT103_MT202COV).append(msgLegacyMt202Cov);
                }
            }
        } catch (final CloneNotSupportedException e) {
            final String msg = String.format("Error cloning BOMessage [%s]", String.valueOf(boMessage.getLongId()));
            Log.error(this, msg, e);
        }
        return PaymentsHubUtil.adaptNewLineToLegacyInfo(legacyInfo.toString());
    }


    /**
     * Get Legacy Info for PHFICCT (MT103)
     *
     * @param boMessage
     * @param env
     * @param dsCon
     * @return
     * @throws CloneNotSupportedException
     */
    private static String getLegacyInfoMT103(final BOMessage boMessage, final PricingEnv env, final DSConnection dsCon) throws CloneNotSupportedException {
        // Template Selector
        final String template = PaymentsHubUtil.getLegacyTemplateSelector(boMessage, PHConstants.LEGACY_TEMPLATE_MT103);
        // Builds the legacyBOMessage with the correct template
        final BOMessage legacyBOMessage = PaymentsHubUtil.getLegacyMessage(boMessage, template);
        // Get legacyInfo
        return PaymentsHubUtil.getLegacyInfo(legacyBOMessage, env, dsCon);
    }


    /**
     * Get LegacyInfo MT202COV.
     *
     * @param boMessage
     * @param env
     * @param dsCon
     * @return
     * @throws CloneNotSupportedException
     */
    private static String getLegacyInfoMT202COV(final BOMessage boMessage, final PricingEnv env, final DSConnection dsCon) throws CloneNotSupportedException {
        String msgLegacyMt202Cov = "";
        // Get PH-FICTCOV Message using linked TransferId
        final BOMessage boMessagePHFICTCOV = PaymentsHubUtil.getPaymentsHubLinkedMessageByTransfer(PHConstants.MESSAGE_PH_FICTCOV, boMessage, dsCon);
        // Get LegacyMessage MT202COV
        if (boMessagePHFICTCOV != null) {
            final PHFICTCOVMessageFormatter fictCovFormatter = new PHFICTCOVMessageFormatter();
            msgLegacyMt202Cov = fictCovFormatter.parseSANT_LEGACY_INFO(boMessagePHFICTCOV, null, null, dsCon);
        }
        return msgLegacyMt202Cov;
    }


    /**
     * Get parseREMITTANCE_INFO.
     * <p>
     * Copy from MT103SWIFTFormatter.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    public String parseREMITTANCE_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (xfer != null) {
            final String customferTransferTrade = xfer.getAttribute("RemittanceInfo");
            if (!Util.isEmpty(customferTransferTrade)) {
                if (customferTransferTrade.equals("EMPTY VALUE")) {
                    return "";
                }
                return SwiftUtil.formatTag72(customferTransferTrade, true);
            }
        }
        boolean customferTransferTrade1 = false;
        if (trade != null) {
            String rule = trade.getKeywordValue("REMITTANCE_INFO");
            if (rule != null) {
                return SwiftUtil.formatTag72(rule, true);
            }
            if (trade.getProduct() instanceof CustomerTransfer) {
                customferTransferTrade1 = true;
                final CustomerTransfer si = (CustomerTransfer) trade.getProduct();
                rule = si.getRemittanceInfo();
                if (rule != null) {
                    return SwiftUtil.formatTag72(rule, true);
                }
            }
        }
        if (xfer != null && !customferTransferTrade1) {
            final TradeTransferRule rule1 = xfer.toTradeTransferRule();
            final SettleDeliveryInstruction si1 = BOCache.getSettleDeliveryInstruction(dsCon, rule1.getCounterPartySDId());
            String doc = null;
            if (si1 != null) {
                doc = si1.getAttribute("Remittance_Information");
            }
            if (!Util.isEmpty(doc)) {
                return SwiftUtil.formatTag72(doc, true);
            }
        }
        return "";
    }


    /**
     * Get SWIFT AddressCode in the Internal SDI Contact.
     *
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    private static String getSwiftAddressCodeInternalSdi(final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String swift = "";
        // Get Internal SDI
        final SettleDeliveryInstruction sdi = PaymentsHubUtil.getInternalSettleDeliveryInstruction(xfer, dsCon);
        if (sdi != null) {
            final LegalEntity processingOrg = sdi.getProcessingOrg();
            final String name = processingOrg != null ? processingOrg.getCode() : "";
            final int leId = processingOrg != null ? processingOrg.getId() : -1;
            final String contactType = sdi.getBeneficiaryContactType();
            // Get LEContact Swift
            swift = PaymentsHubUtil.getSWIFT(leId, name, contactType, sdi.getRole(), xfer.getProductType(), xfer.getProcessingOrg(), xfer.getValueDate(), trade, xfer, dsCon);
        }
        return swift;
    }


    /**
     * Get SWIFT AddressCode in the External SDI Contact.
     *
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    private static String getSwiftAddressCodeExternalSdi(final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String swift = "";
        // Get External SDI
        final SettleDeliveryInstruction sdi = PaymentsHubUtil.getExternalSettleDeliveryInstruction(xfer, dsCon);
        if (sdi != null) {
            final int leId = sdi.getLegalEntityId();
            final LegalEntity legalEntity = BOCache.getLegalEntity(dsCon, leId);
            final String name = legalEntity != null ? legalEntity.getCode() : "";
            final String contactType = sdi.getBeneficiaryContactType();
            // Get LEContact Swift
            swift = PaymentsHubUtil.getSWIFT(leId, name, contactType, sdi.getRole(), xfer.getProductType(), xfer.getProcessingOrg(), xfer.getValueDate(), trade, xfer, dsCon);
        }
        return swift;
    }


    /**
     * Get Additional Info - Field 72
     * <p>
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
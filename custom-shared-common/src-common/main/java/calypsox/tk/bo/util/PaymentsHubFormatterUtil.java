package calypsox.tk.bo.util;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.*;
import com.santander.restservices.paymentshub.model.submodel.*;
import java.util.List;


public class PaymentsHubFormatterUtil {


    public static final String MC_PREFIX = "COL";
    public static final String IM_prefix = "IM";
    public static final String CSA_prefix = "CSA";
    public static final String OSLA_perfix = "OSLA";
    public static final String ISMA_prefix = "ISMA";
    public static final String MarginCall_prefix = "MARG";
    public static final String CustomerTransfer_prefix = "INT";
    public static final String NettingTransfer_prefix = "N";
    public static final String CorporateEvent_prefix = "CA";
    public static final String Bond_prefix = "BOND";
    public static final String PerformanceSwap_prefix = "DER";
    public static final String Equity_prefix = "EQT";
    public static final String Repo_prefix = "REPO";
    public static final int TRNlength = 16;


    /**
     * Build AmountCurrency
     *
     * @param value
     * @param ccy
     * @return
     */
    public static AmountCurrency buildAmountCurrency(final String value, final String ccy) {
        if (Util.isEmpty(value) || !PaymentsHubUtil.isCreatable(value)) {
            return null;
        }
        final double doubleValue = Double.parseDouble(value);
        return buildAmountCurrency(doubleValue, ccy);
    }


    /**
     * Build AmountCurrency
     *
     * @param value
     * @param ccy
     * @return
     */
    public static AmountCurrency buildAmountCurrency(final double value, final String ccy) {
        final AmountCurrency amountCurrency = new AmountCurrency();
        amountCurrency.setValue(value);
        amountCurrency.setCcy(ccy);
        return amountCurrency;
    }


    /**
     * Build InstructionForCreditorAgent
     *
     * @param instructionCode
     * @param instructionCodeInfo
     * @return
     */
    public static InstructionForCreditorAgent buildInstructionForCreditorAgent(final String instructionCode, final String instructionCodeInfo) {
        final InstructionForCreditorAgent instrForCdtrAgt = new InstructionForCreditorAgent();
        instrForCdtrAgt.setCd(instructionCode);
        instrForCdtrAgt.setInstrInf(instructionCodeInfo);
        return instrForCdtrAgt;
    }


    /**
     * Build ChargesInformation
     *
     * @param amountCcy
     * @param agent
     * @return
     */
    public static ChargesInformation buildChargesInformation(final AmountCurrency amountCcy, final FinancialInstitution agent) {
        if (amountCcy == null && agent == null) {
            return null;
        }
        final ChargesInformation chargesInfo = new ChargesInformation();
        chargesInfo.setAmt(amountCcy);
        chargesInfo.setAgt(agent);
        return chargesInfo;
    }


    /**
     * Build Purpose
     *
     * @param purpCode
     * @param purpPropietary
     * @return
     */
    public static Purpose buildPurpose(final String purpCode, final String purpPropietary) {
        final Purpose purp = new Purpose();
        purp.setCd(purpCode);
        purp.setPrtry(purpPropietary);
        return purp;
    }


    /**
     * Build PaymentIdentification
     *
     * @param paymentId
     * @param endToEndId
     * @param txId
     * @param uetr
     * @return
     */
    public static PaymentIdentification buildPaymentIdentification(final String paymentId, final String endToEndId, final String txId, final String uetr) {
        if (Util.isEmpty(paymentId)) {
            return null;
        }
        final PaymentIdentification paymentIdentification = new PaymentIdentification();
        paymentIdentification.setInstrId(paymentId);
        paymentIdentification.setEndToEndId(endToEndId);
        paymentIdentification.setTxId(txId);
        paymentIdentification.setUetr(uetr);
        return paymentIdentification;
    }


    /**
     * Build ClearingSystemIdentification
     *
     * @param clearingSystemCode
     *
     * @return
     */
    public static ClearingSystemIdentification buildClearingSystemIdentification(final String clearingSystemCode) {
        if (Util.isEmpty(clearingSystemCode)) {
            return null;
        }
        final ClearingSystemIdentification clearingSystemIdentification = new ClearingSystemIdentification();
        clearingSystemIdentification.setCd(clearingSystemCode);
        return clearingSystemIdentification;
    }


    /**
     * Build SettlementInstruction
     *
     * @param clearingSystemIdentification
     *
     * @return
     */
    public static SettlementInstruction buildSettlementInstruction(final ClearingSystemIdentification clearingSystemIdentification) {
        if (clearingSystemIdentification == null) {
            return null;
        }
        final SettlementInstruction settlementInstruction = new SettlementInstruction();
        settlementInstruction.setClrSys(clearingSystemIdentification);
        return settlementInstruction;
    }


    /**
     * Build PartyAccount
     *
     * @param accountId
     * @param othrAccount
     * @return
     */
    public static PartyAccount buildPartyAccount(Account accountId, Account othrAccount) {
        if (accountId == null && othrAccount == null) {
            return null;
        }
        final PartyAccount partyAccount = new PartyAccount();
        partyAccount.setId(accountId);
        partyAccount.setOthr(othrAccount);
        return partyAccount;
    }


    /**
     * Build Address
     *
     * @param country
     * @param townName
     * @param postCode
     * @param streetName
     * @param buildingNumber
     * @param addressLine
     * @return
     */
    public static Address buildAddress(String country, String townName, String postCode, String streetName, String buildingNumber, List<String> addressLine) {
        if (country == null && townName == null && postCode == null && streetName == null && buildingNumber == null && addressLine == null) {
            return null;
        }
        final Address address = new Address();
        address.setTwnNm(townName);
        address.setStrtNm(streetName);
        address.setPstCd(postCode);
        address.setBldgNb(buildingNumber);
        address.setCtry(country);
        address.setAdrLine(addressLine);
        return address;
    }


    /**
     * Build Accounting
     *
     * @param accountingFlag
     * @param accountingType
     * @param desc1
     * @param desc2
     * @param desc3
     * @param desc4
     * @param mirrorAccount
     * @param narratives
     * @param netted
     *
     */
    public static Accounting buildAccounting(Boolean accountingFlag, String accountingType, String desc1, String desc2, String desc3,
                                             String desc4, String mirrorAccount, List<String> narratives, Boolean netted) {
        if (accountingFlag == null && accountingType == null && desc1 == null && desc2 == null && desc3 == null
                && desc4 == null && mirrorAccount == null && narratives == null && netted == null) {
            return null;
        }
        final Accounting accounting = new Accounting();
        accounting.setAccountingFlag(accountingFlag);
        accounting.setAccountingType(accountingType);
        accounting.setDesc1(desc1);
        accounting.setDesc2(desc2);
        accounting.setDesc3(desc3);
        accounting.setDesc4(desc4);
        accounting.setMirrorAccount(mirrorAccount);
        accounting.setNarratives(narratives);
        accounting.setNetted(netted);
        return accounting;
    }


    /**
     * Build Account
     *
     * @param iban
     * @param id
     * @return
     */
    public static Account buildAccount(String iban, String id) {
        if (iban == null && id == null) {
            return null;
        }
        final Account account = new Account();
        account.setIban(iban);
        account.setId(id);
        return account;
    }


    /**
     * Build Institution
     *
     * @param party
     * @return
     */
    public static Institution buildInstitution(Party party) {
        if (party == null) {
            return null;
        }
        final Institution institution = new Institution();
        institution.setInstId(party);
        return institution;
    }


    /**
     * Build FinancialInstitution
     *
     * @param party
     * @return
     */
    public static FinancialInstitution buildFinancialInstitution(Party party) {
        if (party == null) {
            return null;
        }
        final FinancialInstitution financialInstitution = new FinancialInstitution();
        financialInstitution.setFinInstnId(party);
        return financialInstitution;
    }


    /**
     * Build Party
     *
     * @param bicfi
     * @param name
     * @param address
     * @param partyId
     * @param clrSysMmbId
     * @return
     */
    public static Party buildParty(final String bicfi, final String name, final Address address, final PartyId partyId, final ClearingSystemMemberIdentification clrSysMmbId) {
        if (bicfi == null && name == null && address == null && partyId == null && clrSysMmbId == null) {
            return null;
        }
        final Party party = new Party();
        party.setBicfi(bicfi);
        party.setNm(name);
        party.setPstlAdr(address);
        party.setPartyId(partyId);
        party.setClrSysMmbId(clrSysMmbId);
        return party;
    }


    /**
     * Build Party
     *
     * @param bicfi
     * @param name
     * @param address
     * @return
     */
    public static Party buildParty(final String bicfi, final String name, final Address address) {
        return buildParty(bicfi, name, address, null, null);
    }


    /**
     * Build Party
     *
     * @param bicfi
     * @return
     */
    public static Party buildParty(final String bicfi) {
        return buildParty(bicfi, null, null, null, null);
    }


    /**
     * Build InstructedInfo
     *
     * @param instrInf
     * @return
     */
    public static InstructedInfo buildInstructedInfo(final String instrInf) {
        if (instrInf == null) {
            return null;
        }
        final InstructedInfo instructedInfo = new InstructedInfo();
        instructedInfo.setInstrInf(instrInf);
        return instructedInfo;
    }


    /**
     * Build LocalInstrument
     *
     * @param code
     * @param propietary
     * @return
     */
    public static LocalInstrument buildLocalInstrument(final String code, final String propietary) {
        if (code == null && propietary == null) {
            return null;
        }
        final LocalInstrument localInstrument = new LocalInstrument();
        localInstrument.setCd(code);
        localInstrument.setPrtry(propietary);
        return localInstrument;
    }


    /**
     * Build RemittanceInfo
     *
     * @param ustrd
     * @return
     */
    public static RemittanceInfo buildRemittanceInfo(final List<String> ustrd) {
        if (Util.isEmpty(ustrd)) {
            return null;
        }
        final RemittanceInfo remittanceInfo = new RemittanceInfo();
        remittanceInfo.setUstrd(ustrd);
        return remittanceInfo;
    }



    /**
     * Build PaymentTypeInformation
     *
     * @param localInstrument
     * @param categoryPurpose
     * @param serviceLevel
     * @param priority
     * @return
     */
    public static PaymentTypeInformation buildPaymentTypeInformation(final String priority, final LocalInstrument localInstrument,
                                                                     final CategoryPurpose categoryPurpose, final ServiceLevel serviceLevel) {
        if (priority == null && localInstrument == null && categoryPurpose == null && serviceLevel == null) {
            return null;
        }
        final PaymentTypeInformation paymentTypeInformation = new PaymentTypeInformation();
        paymentTypeInformation.setLclInstrm(localInstrument);
        paymentTypeInformation.setCtgyPurp(categoryPurpose);
        paymentTypeInformation.setSvcLvl(serviceLevel);
        paymentTypeInformation.setInstrPrty(priority);
        return paymentTypeInformation;
    }


    /**
     * Build PaymentTypeInformation
     *
     * @param localInstrument
     * @return
     */
    public static PaymentTypeInformation buildPaymentTypeInformation(final LocalInstrument localInstrument) {
        return buildPaymentTypeInformation(null, localInstrument, null, null);
    }


    /**
     * Build CategoryPurpose
     *
     * @param priority
     * @param code
     * @return
     */
    public static CategoryPurpose buildCategoryPurpose(final String priority, final String code) {
        final CategoryPurpose categoryPurpose = new CategoryPurpose();
        categoryPurpose.setCd(code);
        categoryPurpose.setPrtry(priority);
        return categoryPurpose;
    }


    /**
     * Build BackOfficeInfo
     *
     * @param provider
     * @param entityCode
     * @param branch
     * @param contract
     * @param frontRef
     * @param backRef
     * @param folder
     * @param deliveryFlag
     * @param creditorNetting
     * @param creditorResidence
     * @param productBunding
     * @param productType
     * @param productSubType
     * @param accounting
     * @return
     */
    public static BackOfficeInfo buildBackOfficeInfo(String provider, String entityCode, String branch, String contract,
                                                     String frontRef, String backRef, String folder, String creditorNetting, Boolean creditorResidence,
                                                     String productBunding, String productType, String productSubType, Boolean deliveryFlag, Accounting accounting) {
        if (provider == null && entityCode == null && branch == null && contract == null && frontRef == null
                && backRef == null && folder == null && creditorNetting == null && creditorResidence == null
                && productBunding == null && productType == null && productSubType == null && deliveryFlag == null
                && accounting == null) {
            return null;
        }
        final BackOfficeInfo backOfficeInfo = new BackOfficeInfo();
        backOfficeInfo.setProvider(provider);
        backOfficeInfo.setEntityCode(entityCode);
        backOfficeInfo.setBranch(branch);
        backOfficeInfo.setContract(contract);
        backOfficeInfo.setFrontRef(frontRef);
        backOfficeInfo.setBackRef(backRef);
        backOfficeInfo.setFolder(folder);
        backOfficeInfo.setCreditorNetting(creditorNetting);
        backOfficeInfo.setCreditorResidence(creditorResidence);
        backOfficeInfo.setProductBunding(productBunding);
        backOfficeInfo.setProductType(productType);
        backOfficeInfo.setProductSubType(productSubType);
        backOfficeInfo.setDeliveryFlag(deliveryFlag);
        backOfficeInfo.setAccounting(accounting);
        return backOfficeInfo;
    }


    /**
     * Build BackOfficeInfo
     *
     * @param provider
     * @param entityCode
     * @param branch
     * @param contract
     * @param deliveryFlag
     * @param accounting
     * @return
     */
    public static BackOfficeInfo buildBackOfficeInfo(String provider, String entityCode, String branch, String contract,
                                                     Boolean deliveryFlag, Accounting accounting) {
        return buildBackOfficeInfo(provider, entityCode, branch, contract, null, null, null, null, null, null, null, null,
                deliveryFlag, accounting);
    }


    /**
     * Build Envelope
     *
     * @param legacyInfo
     * @param backOfficeInfo
     * @return
     */
    public static Envelope buildEnvelope(String legacyInfo, BackOfficeInfo backOfficeInfo) {
        if (legacyInfo == null && backOfficeInfo == null) {
            return null;
        }
        final Envelope envelope = new Envelope();
        envelope.setBackOfficeInfo(backOfficeInfo);
        envelope.setLegacyInfo(legacyInfo);
        return envelope;
    }


    /**
     * Build SettlementTimeRequest
     *
     * @param clearingTime
     * @param fromTime
     * @param rejectTime
     * @param tillTime
     *
     * @return
     */
    public static SettlementTimeRequest buildSettlementTimeRequest(final String clearingTime, final String fromTime,
                                                                   final String rejectTime, final String tillTime) {
        if (clearingTime == null && fromTime == null && rejectTime == null && tillTime == null) {
            return null;
        }
        final SettlementTimeRequest settlTimeRequest = new SettlementTimeRequest();
        settlTimeRequest.setClsTm(clearingTime);
        settlTimeRequest.setFrTm(fromTime);
        settlTimeRequest.setRjctTm(rejectTime);
        settlTimeRequest.setTillTm(tillTime);
        return settlTimeRequest;
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Build GroupHeader.
     *
     * @param eventType
     * @param conceptType
     * @param messageType
     * @param numberTransactions
     * @return
     */
    public static GroupHeader buildGroupHeader(final String eventType, final String conceptType, final String messageType, final int numberTransactions) {
        if (eventType == null && conceptType == null && messageType == null) {
            return null;
        }
        final GroupHeader groupHeader = new GroupHeader();
        groupHeader.setEventType(eventType);
        groupHeader.setConceptType(conceptType);
        groupHeader.setMessageType(messageType);
        groupHeader.setNbOfTxs(numberTransactions);
        return groupHeader;
    }


    /**
     * Build GroupHeader.
     *
     * @param eventType
     * @param conceptType
     * @param messageType
     * @param numberTransactions
     * @return
     */
    public static GroupHeader buildGroupHeader(final String eventType, final String conceptType, final String messageType, final String numberTransactions) {
        if (Util.isEmpty(numberTransactions) || !Util.isNumber(numberTransactions)) {
            return null;
        }
        final int intValue = Integer.parseInt(numberTransactions);
        return buildGroupHeader(eventType, conceptType, messageType, intValue);
    }


    /**
     * Check if the transfer is Netted.
     *
     * @param xfer
     * @return
     */
    public static boolean isBOTransferNetted(final BOTransfer xfer) {
        return (xfer != null && xfer.getTradeLongId() == 0 && xfer.getNettedTransferLongId() == 0);
    }


    /**
     * Gets the TRN. (Transaction Reference Number) only for Payments Hub
     * It must reach a maximun of 16x characters.
     *
     * @param msg   the msg
     * @param trade the trade
     * @return the trn
     */
    public static String buildTRN(final BOMessage msg, final Trade trade, final BOTransfer transfer) {
        String result = "0000000000000000";
        try {
            //Si es un neto generamos un TRN propio
            if (transfer.getTradeLongId() < 1) {
            	if (transfer.getProductType().equalsIgnoreCase(Bond.class.getSimpleName())) {
                    result = buildTRN_CB(msg, trade, transfer,true);
                }
            	else {
                    result = buildTRN_Netting(msg);
                }
            } else {
                //enviamos CA, Bond, Customer Transfer y Margin Calls
                if (trade.getProduct() instanceof CustomerTransfer) {
                    result = buildTRN_CT(msg, trade);
                }
                if (trade.getProduct() instanceof MarginCall) {
                    result = buildTRN_CM(msg, trade);
                }
                if (trade.getProduct() instanceof CA) {
                    result = buildTRN_CA(msg, trade);
                }
                if (trade.getProduct() instanceof Bond) {
                    result = buildTRN_CB(msg, trade, transfer, false);
                }
                if (trade.getProduct() instanceof PerformanceSwap) {
                    result = buildTRN_BRS(msg, trade);
                }
                if (trade.getProduct() instanceof Equity) {
                    result = buildTRN_EQT(msg, trade);
                }
                if (trade.getProduct() instanceof Repo) {
                    result = buildTRN_REPO(msg, trade);
                }
            }
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN:: Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and transfer " + transfer.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_BRS(final BOMessage msg, final Trade trade) {
        String result = "0000000000000000";
        String msgId = "";
        String prefix = "";
        int fillnumber = 0;
        try {
            msgId = String.valueOf(msg.getLongId());
            prefix =  PerformanceSwap_prefix;
            //check and padding till 16x
            int msgIdLength = msgId.length();
            int prefixLength = prefix.length();
            int totallong = msgIdLength + prefixLength;
            //control posiciones para no sobrepasar los 16 caracteres
            if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
                fillnumber = TRNlength - prefixLength;
                msgId = padStringZero(msgId, fillnumber);
            }
            result = prefix + msgId;
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_BRS::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_EQT(final BOMessage msg, final Trade trade) {
        String result = "0000000000000000";
        String msgId = "";
        String prefix = "";
        int fillnumber = 0;
        try {
            msgId = String.valueOf(msg.getLongId());
            prefix = Equity_prefix;
            //check and padding till 16x
            int msgIdLength = msgId.length();
            int prefixLength = prefix.length();
            int totallong = msgIdLength + prefixLength;
            //control posiciones para no sobrepasar los 16 caracteres
            if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
                fillnumber = TRNlength - prefixLength;
                msgId = padStringZero(msgId, fillnumber);
            }
            result = prefix + msgId;
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_EQT::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }

    public static String buildTRN_REPO(final BOMessage msg, final Trade trade) {
        String result = "0000000000000000";
        String msgId = "";
        String prefix = "";
        int fillnumber = 0;
        try {
            msgId = String.valueOf(msg.getLongId());
            prefix = Repo_prefix;
            //check and padding till 16x
            int msgIdLength = msgId.length();
            int prefixLength = prefix.length();
            int totallong = msgIdLength + prefixLength;
            //control posiciones para no sobrepasar los 16 caracteres
            if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
                fillnumber = TRNlength - prefixLength;
                msgId = padStringZero(msgId, fillnumber);
            }
            result = prefix + msgId;
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_REPO::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_CB(final BOMessage msg, final Trade trade, final BOTransfer transfer, boolean netting) {
        String result = "0000000000000000";
        String transferID = "", prefix = "";
        int fillnumber = 0;
        try {
            transferID = String.valueOf(transfer.getLongId());
            prefix = netting ? NettingTransfer_prefix + Bond_prefix : Bond_prefix;
            //check and padding till 16x
            int transferIDLength = transferID.length();
            int prefixLength = prefix.length();
            int totallong = transferIDLength + prefixLength;
            //control posiciones para no sobrepasar los 16 caracteres
            if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
                fillnumber = TRNlength - prefixLength;
                transferID = padStringZero(transferID, fillnumber);
            }
            result = prefix + transferID;
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_CB::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_Netting(final BOMessage msg) {
        //se compone como el prefijo N + numero mensaje (numero transfer para CA)
        String result = "0000000000000000";
        long IDobject;
        String sID, prefix = "";
        int fillnumber, totallong;
        try {
            // if CA, TRN = NCA + transfer id
            if (msg.getProductType().equalsIgnoreCase("CA")){
                prefix = "NCA";
                IDobject = msg.getTransferLongId();
                sID = Long.toString(IDobject);
                totallong = sID.length() + prefix.length();
                if (totallong <= TRNlength) {
                    //relleno
                    fillnumber = sID.length() + (TRNlength - totallong);
                    sID = padStringZero(sID, fillnumber);
                    result = prefix + sID;
                }
            //if not CA, TRN = prefix + msg id
            } else {
                IDobject = msg.getLongId();
                sID = Long.toString(IDobject);
                prefix = NettingTransfer_prefix;
                totallong = sID.length() + prefix.length();
                if (totallong <= TRNlength) {
                    //relleno
                    fillnumber = sID.length() + (TRNlength - totallong);
                    sID = padStringZero(sID, fillnumber);
                    result = prefix + sID;
                }
            }
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_Netting::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + msg.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_CM(final BOMessage msg, final Trade trade) {
        String result = "0000000000000000";
        try {
            MarginCall mc = (MarginCall) trade.getProduct();
            String acuerdo = mc.getMarginCallConfig().getContractType();
            String prefixCM = getPrefixCM(acuerdo);
            String prodPrefix = MarginCall_prefix;
            long idMsg = msg.getLongId();
            String sMsgID = Long.toString(idMsg);
            //check and padding till 16x
            int prefixLength = prefixCM.length() + prodPrefix.length();
            int MsgIdLon = sMsgID.length();
            int totalLong = prefixLength + MsgIdLon;
            //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
            int fillnumber = MsgIdLon + (TRNlength - totalLong);
            sMsgID = padStringZero(sMsgID, fillnumber);
            result = new String(prefixCM + prodPrefix + sMsgID);
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_CM::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_CT(final BOMessage msg, final Trade trade) {
        //se compone como el prefijo INT + numero mensaje
        String result = "0000000000000000";
        long IDmsg;
        String sMsgID, prefix = "";
        int fillnumber, totallong;
        try {
            IDmsg = msg.getLongId();
            sMsgID = Long.toString(IDmsg);
            prefix = CustomerTransfer_prefix;
            totallong = sMsgID.length() + prefix.length();
            if (totallong <= TRNlength) {
                //relleno
                fillnumber = sMsgID.length() + (TRNlength - totallong);
                sMsgID = padStringZero(sMsgID, fillnumber);
                result = prefix + sMsgID;
            }
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "buildTRN_CT::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String buildTRN_CA(final BOMessage msg, final Trade trade) {
        String result = "0000000000000000";
        int fillnumber, caRefLong;
        long IDtransfer;
        String sTransferID = "";
        try {
            IDtransfer = msg.getTransferLongId();
            sTransferID = Long.toString(IDtransfer);
            String caRefConci = trade.getKeywordValue("CARefConci");
            //Modificamos la lógica del campo TRN para producto CA
            //TRN = CARefConci trade keyword (Swift Event Code + Product ID del CA) + ultimos digitos del id de la transfer linkada al pago MT103/202
            //hasta llegar a 16 caracteres
            if (!Util.isEmpty(caRefConci)){
                caRefLong = caRefConci.length();
                if (TRNlength > caRefLong) {
                    int transferIdLength = TRNlength - caRefLong;
                    String subTransferId = sTransferID.substring(sTransferID.length() - transferIdLength);
                    result = caRefConci + subTransferId;
                }
            } else {
                //si el tradekeyword CARefConci está vacío, lógica antigua
                long IDmsg;
                String sMsgID= "";
                IDmsg = msg.getLongId();
                sMsgID = Long.toString(IDmsg);
                String GLCS = trade.getCounterParty().getCode();
                String prefix = CorporateEvent_prefix;
                int totallong = prefix.length() + GLCS.length() + sMsgID.length();
                //control posiciones para no sobrepasar los 16 caracteres
                if (TRNlength >= totallong) {
                    //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
                    fillnumber = sMsgID.length() + (TRNlength - totallong);
                    sMsgID = padStringZero(sMsgID, fillnumber);
                    result = prefix + GLCS + sMsgID;
                } else {  //debemos recomponer para que no pase de 16x, eliminando el GLS
                    GLCS = "";
                    //check and padding till 16x
                    totallong = prefix.length() + sMsgID.length();
                    fillnumber = sMsgID.length() + (TRNlength - totallong);
                    sMsgID = padStringZero(sMsgID, fillnumber);
                    result = prefix + GLCS + sMsgID;
                }
            }
        } catch (Exception e) {
            Log.error(PaymentsHubFormatterUtil.class, "getTRN_CA::Something went wrong composing the TRN for BOMessage " + msg.getLongId() + " and trade " + trade.getLongId(), e);
        }
        return result;
    }


    public static String padStringZero(String input, int totalLegth) {
        String output = "";
        output = String.format("%" + totalLegth + "s", input).replace(" ", "0");
        return output;
    }


    private static String getPrefixCM(String acuerdo) {
        String output = "";
        switch (acuerdo) {
            case IM_prefix:
                output = "IM";
                break;
            case CSA_prefix:
                output = "C";
                break;
            case OSLA_perfix:
                output = "O";
                break;
            case ISMA_prefix:
                output = "I";
                break;
            default:
                output = "C";
                break;
        }
        return output;
    }


}

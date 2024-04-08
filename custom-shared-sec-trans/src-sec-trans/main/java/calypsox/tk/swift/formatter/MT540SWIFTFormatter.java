package calypsox.tk.swift.formatter;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import calypsox.tk.swift.formatter.common.CustomSwiftTagHandler;
import calypsox.tk.swift.formatter.seclending.UtilSecLendingSWIFTFormatter;
import calypsox.tk.swift.formatter.xferagent.XferAgentSwiftSafeAccHandler;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class MT540SWIFTFormatter extends com.calypso.tk.swift.formatter.MT540SWIFTFormatter implements CustomSecuritySWIFTFormatter {

    /** TAG_95 for storing the value of tag95 given by SETTLEMENT_PARTY, its purpose is conditionally formatting the tags that go after it */
    protected static String TAG_95;
    /** TAG_95P PSET for storing the value of tag95P given by SETTLEMENT_PARTY, its purpose is conditionally formatting the tags that go after it */
    private String TAG_95P = null;

    @Override
    public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String messageId = super.parseMESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        CustomSwiftTagHandler customSwiftTagHandler = new CustomSwiftTagHandler();
        if(customSwiftTagHandler.isTransferAgentBond(trade)){
            int toSdiId = ((TransferAgent) trade.getProduct()).getToSdiId();
            String transferAgentMESSAGEId = customSwiftTagHandler.parseTransferAgentMessageIdentifier(trade, toSdiId, messageId);
            if(!Util.isEmpty(transferAgentMESSAGEId)){
                customSwiftTagHandler.updateTransferAgentReceiver(this.getSwiftMessage());
                return transferAgentMESSAGEId;
            }
        }
        return customizeMessageIdentifier(messageId);
    }

    @Override
    public String parseMESSAGE_LINKED_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String messageLinkedId = super.parseMESSAGE_LINKED_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        CustomSwiftTagHandler customSwiftTagHandler = new CustomSwiftTagHandler();
        if(customSwiftTagHandler.isTransferAgentBond(trade)){
            if(customSwiftTagHandler.isTAPartialSettlementCanc(transfer,this.getSwiftMessage())){
                String poRefId = transfer.getAttribute("PORef");
                if(StringUtils.isNumeric(poRefId)){
                    messageLinkedId = ":PREV//" + poRefId;
                }
            }else {
                int fromSdiId = ((TransferAgent) trade.getProduct()).getFromSdiId();
                String transferAgentMESSAGEId = customSwiftTagHandler.parseTransferAgentMessageIdentifier(trade, fromSdiId,messageLinkedId);
                if(!Util.isEmpty(transferAgentMESSAGEId)){
                    return transferAgentMESSAGEId;
                }
            }
        }
        return customizeMessageIdentifier(messageLinkedId);
    }

    @Override
    public String parseINDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String transferAgentINDICATOR = new CustomSwiftTagHandler().parseTransferAgentINDICATOR(trade);
        if(!Util.isEmpty(transferAgentINDICATOR)){
            return transferAgentINDICATOR;
        }
        transferAgentINDICATOR = new CustomSwiftTagHandler().parseXferAgentPigAccount(trade);

        if(!Util.isEmpty(transferAgentINDICATOR)){
            return transferAgentINDICATOR;
        }

        String value = new CustomSwiftTagHandler().parseIndicatorForNetting(transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = UtilSecLendingSWIFTFormatter.customizeMessageIndicator(super.parseINDICATOR(message, trade, sender, rec, transferRules, transfer, dsCon), trade, transfer);
        }
        return value;
    }

    public String parseSTAM_INDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return UtilSecLendingSWIFTFormatter.customizeMessageStamIndicator(trade, transfer);
    }

    /**
     * 90A
     */
    @Override
    public String parseDEAL_PRICE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        return UtilSecLendingSWIFTFormatter.customizeMessageDealPrice(super.parseDEAL_PRICE(message, trade, sender, rec, transferRules, transfer, format, con), trade);
    }

    /**
     * 99A
     */
    @Override
    public String parseDAYS_ACCRUED(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return UtilSecLendingSWIFTFormatter.removeFieldRepoBond(super.parseDAYS_ACCRUED(message, trade, sender, rec, transferRules, transfer, dsCon), trade);
    }

    /**
     * Remove Sequence 16R to 16S on MT54X xml for Repo
     */
    @Override
    public boolean isRepoBased(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) throws MessageFormatException {
        return super.isRepoBased(message, trade, sender, rec, transferRules, transfer, con) && SecFinanceTradeUtil.isVoighKampffSettlement(trade, transfer);
    }

    public String parseRELA_REFERENCE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        String value = UtilSecLendingSWIFTFormatter.customizeTag20(message, trade, transfer, con);
        return ":RELA//" + value;
    }

    public boolean isSecLending(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) throws MessageFormatException {
        return UtilSecLendingSWIFTFormatter.isApplicable(message, transfer, trade);
    }

    @Override
    public String parseSAFEKEEP_ACCOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String safeKeepAccount = super.parseSAFEKEEP_ACCOUNT(message, trade, sender, rec, transferRules, transfer, format, con);

        PartySDIInfo pSDi = (PartySDIInfo) this.getIteratorObject();
        if (isTransferAgent(trade)) {
            safeKeepAccount = new XferAgentSwiftSafeAccHandler(transfer, pSDi).parseSafeKeepAccount(safeKeepAccount);
            if(isIBRC(message,trade,transfer,con)){
                safeKeepAccount = new CustomSwiftTagHandler().getTABondSafeKeepAccount(trade,pSDi,safeKeepAccount);
            }
        }

        return safeKeepAccount;
    }

    private boolean isTransferAgent(Trade trade) {
        return Optional.ofNullable(trade)
                .map(Trade::getProduct)
                .map(p -> p instanceof TransferAgent).orElse(false);
    }

    public String parsePLACE_CLEARING(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return new CustomSwiftTagHandler().parsePLACE_CLEARING(transfer, this._tagValue);
    }

    @Override
    public String parsePLACE_TRADE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = new CustomSwiftTagHandler().parseCustomPlaceOfTrade(transfer, this._tagValue);
        if (!Util.isEmpty(value)) {
            return value;
        }
        return super.parsePLACE_TRADE(message, trade, sender, rec, transferRules, transfer, dsCon);
    }

    @Override
    public String parseSEC_TRADE_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String td = super.parseSEC_TRADE_DATE(message, trade, sender, rec, transferRules, transfer, dsCon);
        return this.customizeSecTradeDate(trade, transfer, td);
    }

    @Override
    public String parseSTCO_OUT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String stco_out = super.parseSTCO_OUT(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (!Util.isEmpty(stco_out)){
            //Remove double // only on TA with account property "pignoracion" : true
            if(new CustomSwiftTagHandler().isTransferAgentPigAccount(trade)){
                stco_out = stco_out.replace("STCO//","STCO/");
            }
            return stco_out;
        }

        return new CustomSwiftTagHandler().parseSTCO_OUT(transfer);
    }

    @Override
    public String parsePLACE_SAFE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "PlaceOfSafekeeping", dsCon);

        if(Util.isEmpty(result)){
            result = super.parsePLACE_SAFE(message, trade, sender, rec, transferRules, transfer, dsCon);
        }

        return result;
    }

    /**
     * Override the PROC_REF core value in case sdi attributes are filled. <br>
     * If PROC_REF (tag 20C) comes after SETTLEMENT_PARTY value BUY or SELL, checking cpty sdi attribute ProcessingReferenceBS <br>
     * Else if PROC_REF (tag 20C) comes after SETTLEMENT_PARTY value REAG or DEAG, checking cpty sdi attribute ProcessingReferenceRD <br>
     * Else, no matter the value of SETTLEMENT_PARTY, checking cpty sdi attribute ProcessingReferenceRD <br>
     *
     * If none of the conditions were met, revert to core functionality.
     */
    @Override
    public String parsePROC_REF(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String result = "";

        if (TAG_95.startsWith(":BUY") || TAG_95.startsWith(":SELL")) {
            result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "ProcessingReferenceBS", con);
        } else if (TAG_95.startsWith(":REAG") || TAG_95.startsWith(":DEAG")) {
            result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "ProcessingReferenceRD", con);
        }

        if(Util.isEmpty(result)){
            result = super.parsePROC_REF(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        return result;
    }

    /**
     * Method used to return the same core SETTLEMENT_PARTY value, but store it for checks inside PROC_REF
     */
    @Override
    public String parseSETTLEMENT_PARTY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        PartySDIInfo psdi = (PartySDIInfo)this.getIteratorObject();

        TAG_95 = super.parseSETTLEMENT_PARTY(message, trade, sender, rec, transferRules, transfer, format, con);


        String benefNif = new CustomSwiftTagHandler().parseTransferAgentSETTLEMENT_PARTY(transfer, psdi);
        if(!Util.isEmpty(benefNif)){
            this._tagValue.setOption("R");
            return ":" + psdi.getIdentifier() + "//" + benefNif;
        }

        Product security = BOCache.getExchangedTradedProduct(con, transfer.getProductId());
        if (security instanceof Bond && security.getSecCode("ISIN").startsWith("PE") && TAG_95.startsWith(":PSET/")) {
            TAG_95P = TAG_95;
            this.parseNOMINAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, con);
        }

        if(new CustomSwiftTagHandler().isTransferAgentBond(trade,(PartySDIInfo)this.getIteratorObject())){
            if(isIBRC(message,trade,transfer,con)){
                if(TAG_95.contains("IBRC")){
                    TAG_95 = TAG_95.replace("IBRC","");
                }
                if(!this._tagValue.getOption().equalsIgnoreCase("P")){
                    this._tagValue.setOption("P");
                }
            }else if(new CustomSwiftTagHandler().checkIsIBRCEURMessage(transfer,con) && ("DEAG".equalsIgnoreCase(psdi.getIdentifier()) || "REAG".equalsIgnoreCase(psdi.getIdentifier()))){
                this._tagValue.setOption("P");
            }
        }

        return TAG_95;
    }

    public String parseSETTLEMENT_PARTY_DETAIL(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String benefDescription = new CustomSwiftTagHandler().parseTransferAgentSETTLEMENT_PARTY_DETAIL(transfer, (PartySDIInfo)this.getIteratorObject());
        if(!Util.isEmpty(benefDescription)){
            return ":REGI//" + benefDescription;
        }
        return super.parseSETTLEMENT_PARTY_DETAIL(message,trade,sender,rec,transferRules,transfer,format,con);
    }

    public String parsePROC_REF_F(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "ProcessingReferenceF", con);

        if(Util.isEmpty(result)){
            result = super.parsePROC_REF(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        return result;
    }

    @Override
    public boolean isUNIT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        if (UtilSecLendingSWIFTFormatter.isBond(message, trade, sender, rec, transferRules, transfer, con) || new CustomSwiftTagHandler().isTransferAgentBond(trade)) {
            return false;
        }
        return super.isUNIT(message, trade, sender, rec, transferRules, transfer, con);
    }

    /**36B*/
    @Override
    public String parseNOMINAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = super.parseNOMINAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, dsCon);
        PartySDIInfo psetInfo = initPartySDIInfo(message, trade, sender, rec, transferRules, transfer, dsCon);
        Product security = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());

        String pset = psetInfo.getPsetSwift();
        if (security instanceof Bond && security.getSecCode("ISIN").startsWith("PE") ){
            double nominal = transfer.getSettlementAmount();
            Vector<String> domainValuesPSetDomesticoPeruano = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetDomesticoPeruano");
            Vector<String> domainValuesPSetEuroclear = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetEuroclear");
            //Liquidacion Domestico Peruano
            if(domainValuesPSetDomesticoPeruano.contains(pset)){
                try {
                    return ":SETT//UNIT/" + SwiftUtil.getSwiftAmount(formatNominal(nominal, security, transfer), (String)null, 15, false);
                } catch (MessageFormatException error) {
                    Log.error("Swift", "Message Format Exception caught ", error);
                    return "";
                }
                //Liquidacion Euroclear
            } else if(domainValuesPSetEuroclear.contains(pset)){
                if (transfer.isNominalRecalculed(security)) {
                    nominal *= security.getPrincipal();
                } else {
                    nominal = transfer.getNominalAmount();
                }
                try {
                    return ":SETT//FAMT/" + SwiftUtil.getSwiftAmount(formatNominal(nominal, security, transfer), (String)null, 15, false);
                } catch (MessageFormatException error) {
                    Log.error("Swift", "Message Format Exception caught ", error);
                    return "";
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasLinkReference(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
        return hasLinkReferenceSant(message, trade, sender, rec, transferRules, transfer, dsCon);
    }

    @Override
    public String parseLINK_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) throws MessageFormatException {
        String linkType = transfer == null ? null : transfer.getAttribute(LINK_NUMBER_ID);
        return Util.isEmpty(linkType) ? super.parseLINK_TYPE(message, trade, sender, rec, transferRules, transfer, format, dsCon) : ":LINK//" + linkType;
    }

    public String parseLINK_REFERENCE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) throws MessageFormatException {
        if (transfer != null) {
            String qualifier = transfer.getAttribute(LINK_REFERENCE_TYPE);
            if (Util.isEmpty(qualifier))
                qualifier = "RELA";
            return !Util.isEmpty(transfer.getAttribute("LinkReference")) ? ":" + qualifier + "//" + customizeMessageIdentifier(transfer.getAttribute("LinkReference")) : "";
        }
        return "";
    }


    /**
     * FormatNominal
     *
     * @param nominal
     * @param security
     * @param transfer
     * @return
     */
    private double formatNominal(double nominal, Product security, BOTransfer transfer) {
        if (security == null) {
            int digits = CurrencyUtil.getRoundingUnit(transfer.getSettlementCurrency());
            int roundingMethod = CurrencyUtil.getRoundingMethod(transfer.getSettlementCurrency());
            return RoundingMethod.round(nominal, digits, roundingMethod);
        } else {
            return SwiftUtil.roundAmount(nominal, security);
        }
    }

    /**
     *
     * @param message
     * @param trade
     * @param sender
     * @param rec
     * @param transferRules
     * @param transfer
     * @param dsCon
     * @return
     */
    private PartySDIInfo initPartySDIInfo(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        SettlementDetailIterator iterator = new SettlementDetailIterator();
        iterator.init(message, trade, sender, rec, transfer, transferRules, null, dsCon);
        Vector<PartySDIInfo> info = iterator.getSettlementDetails();
        return info.stream().filter(sdinfo->sdinfo.getIdentifier().equals("PSET")).findFirst().orElse(null);
    }

}

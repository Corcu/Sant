package calypsox.tk.swift.formatter;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import calypsox.tk.swift.formatter.common.CustomSwiftTagHandler;
import calypsox.tk.swift.formatter.seclending.UtilSecLendingSWIFTFormatter;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;

import java.util.Vector;


/**
 * @author aalonsop
 */
public class MT543SWIFTFormatter extends com.calypso.tk.swift.formatter.MT543SWIFTFormatter implements CustomSecuritySWIFTFormatter {

    /**
     * TAG_95 for storing the value of tag95 given by SETTLEMENT_PARTY, its purpose is conditionally formatting the tags that go after it
     */
    protected static String TAG_95;
    /**
     * TAG_95P PSET for storing the value of tag95P given by SETTLEMENT_PARTY, its purpose is conditionally formatting the tags that go after it
     */
    private String TAG_95P = null;

    @Override
    public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }

    @Override
    public String parseMESSAGE_LINKED_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_LINKED_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }

    @Override
    public String parseINDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = new CustomSwiftTagHandler().parseIndicatorForNetting(transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = UtilSecLendingSWIFTFormatter.customizeMessageIndicator(super.parseINDICATOR(message, trade, sender, rec, transferRules, transfer, dsCon), trade, transfer);
        }
        return value;
    }

    public String parseSTAM_INDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return UtilSecLendingSWIFTFormatter.customizeMessageStamIndicator(trade, transfer);
    }

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
        if (!Util.isEmpty(stco_out)) return stco_out;

        return new CustomSwiftTagHandler().parseSTCO_OUT(transfer);
    }

    @Override
    public String parsePLACE_SAFE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "PlaceOfSafekeeping", dsCon);

        if (Util.isEmpty(result)) {
            result = super.parsePLACE_SAFE(message, trade, sender, rec, transferRules, transfer, dsCon);
        }

        return result;
    }

    /**
     * Override the PROC_REF core value in case sdi attributes are filled. <br>
     * If PROC_REF (tag 20C) comes after SETTLEMENT_PARTY value BUY or SELL, checking cpty sdi attribute ProcessingReferenceBS <br>
     * Else if PROC_REF (tag 20C) comes after SETTLEMENT_PARTY value REAG or DEAG, checking cpty sdi attribute ProcessingReferenceRD <br>
     * Else, no matter the value of SETTLEMENT_PARTY, checking cpty sdi attribute ProcessingReferenceRD <br>
     * <p>
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

        if (Util.isEmpty(result)) {
            result = super.parsePROC_REF(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        return result;
    }

    /**
     * Method used to return the same core SETTLEMENT_PARTY value, but store it for checks inside PROC_REF
     */
    @Override
    public String parseSETTLEMENT_PARTY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        TAG_95 = super.parseSETTLEMENT_PARTY(message, trade, sender, rec, transferRules, transfer, format, con);
        Product security = BOCache.getExchangedTradedProduct(con, transfer.getProductId());
        if (security instanceof Bond && security.getSecCode("ISIN").startsWith("PE") && TAG_95.startsWith(":PSET/")) {
            TAG_95P = TAG_95;
            this.parseNOMINAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, con);
        }
        return TAG_95;
    }

    public String parsePROC_REF_F(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String result = UtilSecLendingSWIFTFormatter.customMessageParameterProductType(transfer, "ProcessingReferenceF", con);

        if (Util.isEmpty(result)) {
            result = super.parsePROC_REF(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        return result;
    }

    @Override
    public boolean isUNIT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        if (UtilSecLendingSWIFTFormatter.isBond(message, trade, sender, rec, transferRules, transfer, con)) {
            return false;
        }
        return super.isUNIT(message, trade, sender, rec, transferRules, transfer, con);
    }

    /**
     * 36B
     */
    @Override
    public String parseNOMINAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = super.parseNOMINAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, dsCon);
        PartySDIInfo psetInfo = initPartySDIInfo(message, trade, sender, rec, transferRules, transfer, dsCon);
        Product security = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());

        String pset = psetInfo.getPsetSwift();
        if (security instanceof Bond && security.getSecCode("ISIN").startsWith("PE")) {
            double nominal = transfer.getSettlementAmount();
            Vector<String> domainValuesPSetDomesticoPeruano = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetDomesticoPeruano");
            Vector<String> domainValuesPSetEuroclear = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetEuroclear");
            //Liquidacion Domestico Peruano
            if (domainValuesPSetDomesticoPeruano.contains(pset)) {
                try {
                    return ":SETT//UNIT/" + SwiftUtil.getSwiftAmount(formatNominal(nominal, security, transfer), (String) null, 15, false);
                } catch (MessageFormatException error) {
                    Log.error("Swift", "Message Format Exception caught ", error);
                    return "";
                }
                //Liquidacion Euroclear
            } else if (domainValuesPSetEuroclear.contains(pset)) {
                if (transfer.isNominalRecalculed(security)) {
                    nominal *= security.getPrincipal();
                } else {
                    nominal = transfer.getNominalAmount();
                }
                try {
                    return ":SETT//FAMT/" + SwiftUtil.getSwiftAmount(formatNominal(nominal, security, transfer), (String) null, 15, false);
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

    public String parseNOTICE_DAYS(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon)  {
        Vector<String> messageTypes =  LocalCache.getDomainValues(dsCon, "SANT_ENABLE_NOTICE_DAYS");
        if (!Util.isEmpty(messageTypes) && messageTypes.contains(this.getClass().getSimpleName().substring(0,4))) {
            String noticeDays = super.parseNOTICE_DAYS(message, trade, sender, rec, transferRules, transfer, dsCon);
            return ":CADE//000".equals(noticeDays) ? null : noticeDays;
        }
        return null;
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
        return info.stream().filter(sdinfo -> sdinfo.getIdentifier().equals("PSET")).findFirst().orElse(null);
    }

}

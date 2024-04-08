package calypsox.tk.swift.formatter.common;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import calypsox.tk.swift.formatter.CalypsoAppIdentifier;
import calypsox.tk.swift.formatter.seclending.UtilSecLendingSWIFTFormatter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.util.*;

/**
 * @author aalonsop
 */
public class CustomSwiftTagHandler {

    public static final String PLACEOFTRADE_VALUE_FOR_NET = ":TRAD//VARI";


    public String parsePLACE_CLEARING(BOTransfer transfer, TagValue tagValue) {
        String tagStr = "";
        if (isCCPCounterParty(transfer)) {
            LEContact contact = BOCache.getContact(DSConnection.getDefault(), "ALL", BOCache.getLEFromCache(transfer.getOriginalCptyId()), "Settlement", transfer.getProductType(), 0);
            if (contact != null) {
                tagValue.setOption("H");
                tagStr = ":CLEA//" + contact.getSwift();
            }
        }
        return tagStr;
    }

    public String parseCustomPlaceOfTrade(BOTransfer transfer, TagValue tagValue) {
        String value;
        String customPlaceOfTrade = UtilSecLendingSWIFTFormatter.customMessageParameter(transfer, "PlaceOfTrade", DSConnection.getDefault());
        if (!customPlaceOfTrade.isEmpty()) {
            tagValue.setOption("B");
            value = ":TRAD//" + customPlaceOfTrade;
        } else {
            value = new CustomSwiftTagHandler().parsePlaceOfTradeFromKwd(transfer, tagValue);
        }
        return value;
    }

    public String parsePlaceOfTradeFromKwd(BOTransfer transfer, TagValue tagValue) {
        String res;
        boolean isNet = Optional.ofNullable(transfer).map(BOTransfer::getNettedTransfer).orElse(false);
        long tradeId = Optional.ofNullable(transfer).map(BOTransfer::getTradeLongId).orElse(0L);
        if (isNet && tradeId == 0L) {
            res = parsePlaceOfTradeFromNet(transfer, tagValue);
        } else {
            res = parsePlaceOfTradeFromKwdSingleTransfer(transfer, tagValue);
        }
        return res;
    }

    public String parsePlaceOfTradeFromNet(BOTransfer nettedTransfer, TagValue tagValue) {
        String placeOfTrade = "";
        TransferArray underlyingTransfers = nettedTransfer.getUnderlyingTransfers();
        if (Util.isEmpty(underlyingTransfers)) {
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(nettedTransfer.getLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading Netting Transfer for BOTransfer: " + nettedTransfer.getLongId());
            }
        }
        if (!Util.isEmpty(underlyingTransfers) && underlyingTransfers.size() > 0) {
            for (int i = 0; i < underlyingTransfers.size(); i++) {
                BOTransfer transfer = underlyingTransfers.get(i);
                String undPlaceOfTrade = parsePlaceOfTradeFromKwdSingleTransfer(transfer, tagValue);
                if (!Util.isEmpty(placeOfTrade) && !undPlaceOfTrade.equalsIgnoreCase(placeOfTrade)) {
                    placeOfTrade = PLACEOFTRADE_VALUE_FOR_NET;
                    break;
                } else {
                    placeOfTrade = undPlaceOfTrade;
                }
            }
        }
        return placeOfTrade;
    }

    public String parsePlaceOfTradeFromKwdSingleTransfer(BOTransfer transfer, TagValue tagValue) {
        String tagStr = "";
        long tradeId = Optional.ofNullable(transfer).map(BOTransfer::getTradeLongId).orElse(0L);
        if (tradeId > 0L) {
            String kwdName = "PlaceOfTrade";
            List<String> kwdNames = new ArrayList<>();
            kwdNames.add(kwdName);
            try {
                Map<String, String> kwds = DSConnection.getDefault().getRemoteTrade().getTradeKeywords(tradeId, kwdNames);
                String kwdValue = Optional.ofNullable(kwds).map(k -> k.get(kwdName)).orElse("");
                if (!Util.isEmpty(kwdValue)) {
                    tagValue.setOption("B");
                    tagStr = ":TRAD//EXCH/" + kwdValue;
                }
            } catch (CalypsoServiceException exc) {
                Log.warn(this, exc.getCause());
            }
        }
        return tagStr;
    }

    public String parseIndicatorForNetting(BOTransfer xfer, DSConnection dsCon) {
        String placeOfTrade = new CustomSwiftTagHandler().parseCustomPlaceOfTrade(xfer, new TagValue());
        String indicator = "";
        if (xfer.getNettedTransfer() && xfer.getTradeLongId() == 0) { //netting
            try {
                TransferArray underlings = Util.isEmpty(xfer.getUnderlyingTransfers()) ? dsCon.getRemoteBO().getNettedTransfers(xfer.getLongId()) : xfer.getUnderlyingTransfers();

                if (underlings.stream().allMatch(u -> {
                    try {
                        return SecFinanceTradeUtil.isVoighKampffSettlement(dsCon.getRemoteTrade().getTrade(u.getTradeLongId()), u);
                    } catch (CalypsoServiceException e) {
                        Log.error(this, e);
                        return false;
                    }
                })) {
                    return ":SETR//" + ("PAY".equals(xfer.getPayReceive()) ? "REPU" : "RVPO");
                }

            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }

        }


        if (PLACEOFTRADE_VALUE_FOR_NET.equals(placeOfTrade)) {
            indicator = ":SETR//NETT";
        }
        return indicator;
    }

    public boolean isCCPCounterParty(BOTransfer xfer) {
        boolean res = false;
        if (xfer != null) {
            Vector<LegalEntityAttribute> attributes = Optional.ofNullable(BOCache.getLegalEntityAttributes(DSConnection.getDefault(), xfer.getOriginalCptyId()))
                    .orElse(new Vector<>());
            for (LegalEntityAttribute attribute : attributes) {
                if ("CCP".equals(attribute.getAttributeType())) {
                    res = "Y".equalsIgnoreCase(attribute.getAttributeValue()) || Boolean.parseBoolean(attribute.getAttributeValue());
                    break;
                }
            }
        }
        return res;
    }

    public String parseSTCO_OUT(BOTransfer transfer) {
        if (null != transfer) {
            Product p = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), transfer.getProductId());

            if (p == null) {
                return "";
            }

            TradeTransferRule tRule = transfer.toTradeTransferRule();
            SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), tRule.getProcessingOrgSDId());
            String attr = sd.getAttribute("Physical Settlement");
            if (!Util.isEmpty(attr)) {
                return ":STCO//" + attr;
            }
        }
        return "";
    }

    /**
     * Get the Mx BANKCODESP trade keyword
     *
     * @param trade the Trade
     * @return the Mx BANKCODESP
     */
    public String customizeBankCodeOfSpain(Trade trade) {
        if (isMxReference(trade)) {
            String bankCodeOfSpain = trade.getKeywordValue("Mx BANKCODESP");
            return !Util.isEmpty(bankCodeOfSpain) ? ":COMM//" + bankCodeOfSpain : "";
        }
        return "";
    }

    /**
     * Check if trade has Mx BANKCODESP
     *
     * @param trade the Trade
     * @return true if Mx BANKCODESP not empty
     */
    public boolean isMxReference(Trade trade) {
        return trade != null && !Util.isEmpty(trade.getKeywordValue("Mx BANKCODESP"));
    }


    /**
     * SETR/IBRC/BLCE on account property Bloqueo = true -> TO
     * SETR/IBRC/DLCE on account property Bloqueo = true -> FROM
     *
     * @param trade
     * @return
     */
    public String parseTransferAgentINDICATOR(Trade trade) {
        if (null != trade && trade.getProduct() instanceof TransferAgent) {
            TransferAgent transferAgent = (TransferAgent) trade.getProduct();
            int toSdiId = transferAgent.getToSdiId();
            Account account = getAccountFromSdi(toSdiId, DSConnection.getDefault());
            boolean isToBlockingAccount = isBlockingAccount(account);
            boolean isFromBlockingAccount = false;
            if (!isToBlockingAccount) {
                int fromSdiId = transferAgent.getFromSdiId();
                account = getAccountFromSdi(fromSdiId, DSConnection.getDefault());
                isFromBlockingAccount = isBlockingAccount(account);
            }
            if (isToBlockingAccount) {
                return ":SETR/IBRC/BLCE";
            }
            if (isFromBlockingAccount) {
                return ":SETR/IBRC/DLCE";
            }
        }
        return "";
    }

    /**
     * @param trade
     * @return
     */
    public String parseXferAgentPigAccount(Trade trade) {
        if (null != trade && trade.getProduct() instanceof TransferAgent) {
            if(isToPigAccount(trade)) {
                return ":SETR/IBRC/CUPG";
            }
            if (isFromPigAccount(trade)) {
                return ":SETR/IBRC/PGCU";
            }
        }
        return "";
    }

    /**
     * @param transfer
     * @param psdi
     * @return
     */
    public String parseTransferAgentSETTLEMENT_PARTY(BOTransfer transfer, PartySDIInfo psdi) {
        String attName = "NIFBeneficiario";
        return getBeneficiaryAttribute(transfer, psdi, attName);
    }

    /**
     * @param transfer
     * @param psdi
     * @return
     */
    public String parseTransferAgentSETTLEMENT_PARTY_DETAIL(BOTransfer transfer, PartySDIInfo psdi) {
        String attName = "DescripcionBeneficiario";
        return getBeneficiaryAttribute(transfer, psdi, attName);
    }

    /**
     * Add 5PSA as MT identifier only if PignoracionPropia Account attribute set to true
     *
     * @param trade
     * @param sdiId
     * @param semeValue
     * @return
     */
    public String parseTransferAgentMessageIdentifier(Trade trade, int sdiId, String semeValue) {
        if (null != trade && trade.getProduct() instanceof TransferAgent) {
            Account account = getAccountFromSdi(sdiId, DSConnection.getDefault());
            boolean isPigPropiaAccount = isPigPropiaAccount(account);
            if (isPigPropiaAccount) {
                StringBuilder builder = new StringBuilder(semeValue);
                builder.insert(semeValue.lastIndexOf('/') + 1, CalypsoAppIdentifier._5PSA.toString().replace("_", ""));
                return builder.toString();
            }
        }
        return "";
    }

    /**
     * Check PartialSettlement and CANC swift message
     * @param transfer
     * @param swiftMessage
     * @return
     */
    public boolean isTAPartialSettlementCanc(BOTransfer transfer, SwiftMessage swiftMessage) {
        boolean isPartialSettlement = Optional.ofNullable(transfer)
                .map(t -> t.getAttribute("SplitReasonFrom"))
                .filter("PartialSettlement"::equalsIgnoreCase).isPresent();
        boolean isCanc = Optional.ofNullable(swiftMessage)
                .map(swmsg -> swmsg.getSwiftField(":23G:", "", ""))
                .map(SwiftFieldMessage::getValue)
                .filter("CANC"::equalsIgnoreCase).isPresent();
        return isPartialSettlement && isCanc;
    }


    public boolean isTransferAgentPigAccount(Trade trade) {
        if (null != trade && trade.getProduct() instanceof TransferAgent) {
            int toSdiId = ((TransferAgent) trade.getProduct()).getToSdiId();
            int fromSdiId = ((TransferAgent) trade.getProduct()).getFromSdiId();
            Account toAccount = getAccountFromSdi(toSdiId, DSConnection.getDefault());
            Account fromAccount = getAccountFromSdi(fromSdiId, DSConnection.getDefault());
            return isPignoracionAccount(toAccount) || isPignoracionAccount(fromAccount);
        }
        return false;
    }

    public boolean isFromPigAccount(Trade trade) {
        if (null != trade) {
            int fromSdiId = ((TransferAgent) trade.getProduct()).getFromSdiId();
            Account fromAccount = getAccountFromSdi(fromSdiId, DSConnection.getDefault());
            return isPignoracionAccount(fromAccount);
        }
        return false;
    }

    public boolean isToPigAccount(Trade trade) {
        if (null != trade) {
            int toSdiId = ((TransferAgent) trade.getProduct()).getToSdiId();
            Account toAccount = getAccountFromSdi(toSdiId, DSConnection.getDefault());
            return isPignoracionAccount(toAccount);
        }
        return false;
    }

    /**
     * Set custom Receiver when the account contains PignoracionPropia : true
     *
     * @return
     */
    public void updateTransferAgentReceiver(SwiftMessage swiftMessage) {
        Optional.ofNullable(swiftMessage).ifPresent(message -> {
            String transferAgentReceiver = getTransferAgentReceiver();
            message.setReceiver(transferAgentReceiver);
        });
    }

    /**
     * @return
     */
    public String getTransferAgentReceiver() {
        List<String> receiver = DomainValues.values("SwiftReceiverPignoracionPropiaAccount");
        return receiver.stream().findFirst().orElse("ESPBESMMXXXX");
    }

    private String getBeneficiaryAttribute(BOTransfer transfer, PartySDIInfo psdi, String attName) {
        String attribute = Optional.ofNullable(transfer).map(xfer -> xfer.getAttribute(attName)).orElse("");
        if (!Util.isEmpty(attribute)) {
            int transferAgentSDINumber = getTransferAgentSDINumber(psdi, transfer);
            return isAcceptedBlockingAccount(transferAgentSDINumber) ? attribute : "";
        }
        return "";
    }

    private int getTransferAgentSDINumber(PartySDIInfo psdi, BOTransfer transfer) {
        int sdiId = 0;
        if (null != psdi && null != transfer && TransferAgent.class.getSimpleName().equalsIgnoreCase(transfer.getProductType())
                && (psdi.getIdentifier().equals("BUYR") || psdi.getIdentifier().equals("SELL"))) {
            if ("BUYR".equals(psdi.getIdentifier())) {
                sdiId = transfer.getReceiverSDId();
            } else if ("SELL".equals(psdi.getIdentifier())) {
                sdiId = transfer.getPayerSDId();
            }
        }
        return sdiId;
    }

    private boolean isAcceptedBlockingAccount(int sdiId) {
        if (sdiId != 0) {
            Account account = getAccountFromSdi(sdiId, DSConnection.getDefault());
            return null != account && (isBlockingAccount(account) || isPignoracionAccount(account));
        }
        return false;
    }

    /**
     *
     *Italia [ :97B::SAFE/IBRC/CEND/UNKNOWN -> :97B::SAFE/MOTI/CEND/UNKNOWN ]
     *Portugal [ :97B::SAFE/IBRC/CEND/IBLS/CEND/UNKNOWN -> :97B::SAFE/IBLS/CEND/UNKNOWN ]
     *
     * @param trade
     * @param psdi
     * @param safeKeepAccount
     * @return
     */
    public String getTABondSafeKeepAccount(Trade trade, PartySDIInfo psdi, String safeKeepAccount){
        if(isTransferAgentBond(trade, psdi) && !Util.isEmpty(safeKeepAccount)) {
            if (safeKeepAccount.contains("IBLS")) {
                safeKeepAccount = safeKeepAccount.replace("IBRC/", "").replaceFirst("CEND/", "");
            } else if(Optional.ofNullable(((TransferAgent)trade.getProduct()).getSecurity())
                    .map(sec -> sec.getSecCode("ISIN")).filter(isin -> isin.startsWith("IT")).isPresent()
                    && !Optional.ofNullable(psdi.getPartyIdentifier()).isPresent()) {
                safeKeepAccount = safeKeepAccount.replace("IBRC/","MOTI/");
            }
        }
        return safeKeepAccount;
    }

    public boolean isTransferAgentBond(Trade trade, PartySDIInfo psdi){
        List<String> identifier = Arrays.asList("BUYR", "REAG", "SELL", "DEAG");
        return null!=trade && trade.getProduct() instanceof TransferAgent
                && Optional.ofNullable(((TransferAgent) trade.getProduct()).getSecurity()).filter(product -> product instanceof Bond
                || "Bond".equalsIgnoreCase(product.getProductFamily())).isPresent()
                && Optional.ofNullable(psdi).filter(ps -> identifier.contains(ps.getIdentifier())).isPresent();
    }

    public boolean isTransferAgentBond(Trade trade) {
        return Optional.ofNullable(trade)
                .map(Trade::getProduct)
                .filter(TransferAgent.class::isInstance)
                .map(TransferAgent.class::cast)
                .map(TransferAgent::getSecurity)
                .filter(product -> product instanceof Bond || "Bond".equalsIgnoreCase(product.getProductFamily())).isPresent();
    }

    /**
     * Accept only message From IBRC to ECLR or ECLR to IBRC
     *
     * @param transfer
     * @param con
     * @return
     */
    public boolean checkIsIBRCEURMessage(BOTransfer transfer,DSConnection con){ //crappy patch
        String activated = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "DisponibleIBRC");
        if(!Util.isEmpty(activated) && null!=transfer){
            LegalEntity internalLe = BOCache.getLegalEntity(con, transfer.getInternalAgentId());
            LegalEntity externalLe = BOCache.getLegalEntity(con, transfer.getExternalAgentId());
            if(Boolean.parseBoolean(activated.toLowerCase())){
                boolean eclrToIbrc = checkDataSourceScheme(internalLe, "ECLR") && checkDataSourceScheme(externalLe, "IBRC");
                boolean ibrcToEclr = checkDataSourceScheme(internalLe, "IBRC") && checkDataSourceScheme(externalLe, "ECLR");
                return eclrToIbrc || ibrcToEclr;
            }else if("2".equalsIgnoreCase(activated)){
               return checkDataSourceScheme(internalLe, "ECLR") && checkDataSourceScheme(externalLe, "IBRC");
            }else if("3".equalsIgnoreCase(activated)){
                return checkDataSourceScheme(internalLe, "IBRC") && checkDataSourceScheme(externalLe, "ECLR");
            }
        }
       return false;
    }

    public boolean checkDataSourceScheme(LegalEntity entity, String scheme){
        Collection<LegalEntityAttribute> legalEntityAttributes = Optional.ofNullable(entity).map(LegalEntity::getLegalEntityAttributes).orElse(Collections.emptyList());
        return !legalEntityAttributes.isEmpty() && legalEntityAttributes.stream()
                .filter(att -> "Data Source Scheme".equalsIgnoreCase(att.getAttributeType()))
                .map(LegalEntityAttribute::getAttributeValue)
                .anyMatch(scheme::equalsIgnoreCase);
    }

    protected Account getAccountFromSdi(int sdiId, DSConnection dsCon) {
        SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        int accountId = Optional.ofNullable(settleDeliveryInstruction).map(SettleDeliveryInstruction::getGeneralLedgerAccount).orElse(0);
        return BOCache.getAccount(dsCon, accountId);
    }

    private boolean isBlockingAccount(Account account) {
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Bloqueo")).filter("true"::equalsIgnoreCase).isPresent();
    }

    public boolean isPignoracionAccount(Account account) {
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Pignoracion")).filter("true"::equalsIgnoreCase).isPresent();
    }

    private boolean isPigPropiaAccount(Account account) {
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("PignoracionPropia")).filter("true"::equalsIgnoreCase).isPresent();
    }

}

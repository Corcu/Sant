package calypsox.tk.swift.formatter.common;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;
import calypsox.tk.report.util.SecFinanceTradeUtil;
import calypsox.tk.swift.formatter.CalypsoAppIdentifier;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.swift.formatter.SecuritySWIFTFormatter;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import static calypsox.tk.report.util.SecFinanceTradeUtil.SD_FILTER_REPO_VKS;
import static com.calypso.tk.swift.formatter.SecuritySWIFTFormatter.LINK_QUALIFIER;
import static com.calypso.tk.swift.formatter.SecuritySWIFTFormatter.LINK_REFERENCE;

/**
 * @author aalonsop
 */
public interface CustomSecuritySWIFTFormatter {

    String LINK_REFERENCE_TYPE = "LinkReferenceType";
    String LINK_NUMBER_ID = "LinkNumberId";

    default String customizeMessageIdentifier(String semeValue) {
        StringBuilder builder = new StringBuilder(semeValue);
        builder.insert(semeValue.lastIndexOf('/') + 1, CalypsoAppIdentifier.CYO);
        return builder.toString();
    }


    default boolean isCCPCounterParty(BOTransfer xfer) {
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

    /**
     * For Repo only
     *
     * @param trade
     * @param transfer
     * @param currentTradeDate
     * @return TradeDate in every case except for PartialReturn event transfers
     */
    default String customizeSecTradeDate(Trade trade, BOTransfer transfer, String currentTradeDate) {
        String eventTypeActionName = "EventTypeActionName";
        String partialReturnStr = "Partial Return";
        String tradeDate = currentTradeDate;
        if (trade != null && trade.getProduct() instanceof Repo && transfer != null) {
            String eventTypeAction = "";
            if (transfer.getNettedTransfer()) {
                FIFlowTransferNetHandler netHandler = new FIFlowTransferNetHandler(transfer);
                TransferArray underlyings = netHandler.getTransferUnderlyings();
                for (BOTransfer underlying : underlyings) {
                    if ("SECURITY".equals(underlying.getTransferType())
                            && partialReturnStr.equals(underlying.getAttribute(eventTypeActionName))) {
                        eventTypeAction = underlying.getAttribute(eventTypeActionName);
                        tradeDate = ":TRAD//" + SwiftUtil.getSwiftDate(underlying.getTradeDate());
                        break;
                    }
                }
            } else {
                eventTypeAction = transfer.getAttribute(eventTypeActionName);
            }
            if (!partialReturnStr.equals(eventTypeAction)) {
                tradeDate = ":TRAD//" + SwiftUtil.getSwiftTradeDate(trade);
            }
        }
        return tradeDate;
    }

    default boolean hasLinkReferenceSant(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
        if (SecFinanceTradeUtil.isVoighKampffSettlement(trade, transfer) && trade.getProduct() instanceof SecFinance) {
            SecFinance secFinance = (SecFinance)trade.getProduct();
            if (transfer.getValueDate().equals(secFinance.getEndDate())) { //closing xfer
                try {
                    TransferArray xfers = dsCon.getRemoteBO().getBOTransfers(trade.getLongId(), false);


                    List<BOTransfer> opening = xfers.stream().filter(x -> "SECURITY".equals(x.getTransferType())
                            && trade.getSettleDate().equals(x.getValueDate())
                            && !Status.S_SPLIT.equals(x.getStatus())).collect(Collectors.toList());
                    if (!Util.isEmpty(opening)) {
                        BOTransfer openingXfer = opening.get(0);
                        if (opening.size() > 1) {
                            Product sec = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());
                            int nomDec = sec == null ? 2 : sec.getNominalDecimals(sec.getCurrency());
                            double tol = Math.pow(10, -nomDec);
                            int sign = "PAY".equals(transfer.getPayReceive()) ? -1 : 1;

                            Optional<BOTransfer> openingSibling = opening.stream().filter(x -> (Math.abs(x.getSettlementAmount()) - Math.abs(transfer.getSettlementAmount())) < tol
                                    && ("PAY".equals(x.getPayReceive()) ? -1 : 1) == -sign).findFirst();

                            if (!openingSibling.isPresent()) {
                                Log.warn(this, String.format("Cannot identify opening sibling for transfer %s, uneven split", transfer));
                                return false;
                            }
                            openingXfer = openingSibling.get();
                        }

                        long xferId = openingXfer.getNettedTransferLongId() > 0 ? openingXfer.getNettedTransferLongId() : openingXfer.getLongId();

                        MessageArray xferMessages = dsCon.getRemoteBO()
                                .getMessages(" template_name like 'MT54%' and transfer_id = ? and  sub_action != 'CANCEL' and message_id not in (select linked_id from bo_message where  transfer_id = ? and linked_id>0)",
                                        Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.LONG, xferId), new CalypsoBindVariable(CalypsoBindVariable.LONG, xferId)));


                        if (!xferMessages.isEmpty()) {
                            transfer.setAttribute(LINK_REFERENCE, Long.toString(xferMessages.get(0).getLongId()));

                            String numberId;
                            switch (message.getTemplateName().substring(2, 5)) {
                                case "541":
                                    numberId = "543";
                                    break;
                                case "543":
                                    numberId = "541";
                                    break;
                                case "540":
                                    numberId = "542";
                                    break;
                                case "542":
                                    numberId = "540";
                                    break;
                                default:
                                    numberId = null;
                            }

                            transfer.setAttribute(LINK_NUMBER_ID, numberId);
                            //    transfer.setAttribute(LINK_QUALIFIER, "LINK");
                            transfer.setAttribute(LINK_REFERENCE_TYPE, "PREV");
                        }
                    }

                } catch (Exception e) {
                    Log.error("Swift", "Message Format Exception caught ", e);
                }

            }

        }
        return !Util.isEmpty(transfer.getAttribute(LINK_REFERENCE));
    }

    default boolean isTriParty(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
        if (trade != null && trade.getProduct() instanceof SecFinance) {
            SecFinance secFinance = (SecFinance) trade.getProduct();
            return secFinance.isTriparty();
        }
        return false;
    }

    default boolean isOpen(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
        return trade != null && trade.getProduct().isOpen();
    }
}

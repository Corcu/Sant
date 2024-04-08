package calypsox.tk.swift.formatter.seclending;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class UtilSecLendingSWIFTFormatter {

    private static final String TAG20 = ":20C:";

    public static String customizeMessageIndicator(String indicatorValue, Trade trade, BOTransfer transfer) {
        String out = indicatorValue;

        if(null!=trade && trade.getProduct()!= null){
            Product p = trade.getProduct();

            if (p == null) {
            	return out;
            }
            
            DSConnection dsCon = DSConnection.getDefault();
            TradeTransferRule tRule = transfer.toTradeTransferRule();
            
            boolean foundAttr = false;
            SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(dsCon, tRule.getProcessingOrgSDId());
            String attr;
/*            String attr = sd.getAttribute("StampDutyIndicator");
            if (sd != null && !Util.isEmpty(attr)) {
            	out = ":" + attr;
            	foundAttr = true;
            }*/

            if (!foundAttr && ((p instanceof SecLending && ":SETR//TRAD".equals(out)) || p instanceof Repo || p instanceof TransferAgent || p instanceof Bond)) {
                sd = BOCache.getSettleDeliveryInstruction(dsCon, tRule.getCounterPartySDId());

                attr = sd.getAttribute("TypeOfSettlementIndicator");
                if (sd != null && !Util.isEmpty(attr)) {
                    out = ":SETR//" + attr;
                }
            }
        }
        return out;
    }

    public static String customizeMessageStamIndicator(Trade trade, BOTransfer transfer) {
        if(null!=transfer){
            Product p = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), transfer.getProductId());;

            if (p == null) {
                return "";
            }

            String isActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "StampDutyIndicator");
            if (Util.isEmpty(isActive)){
                return "";
            }

            String [] productTypes = isActive.trim().split(",");
            boolean isProductAccepted = Arrays.stream(productTypes).anyMatch(s -> s.trim().equalsIgnoreCase(p.getType()));

            TradeTransferRule tRule = transfer.toTradeTransferRule();
            SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), tRule.getProcessingOrgSDId());
            String attr = sd.getAttribute("StampDutyIndicator");
            if (!Util.isEmpty(attr) && isProductAccepted) {
                return  ":" + attr;
            }
        }
        return "";
    }

    public static String customizeMessageDealPrice(String dealPriceValue, Trade trade) {
        String out = dealPriceValue;

        if(null!=trade && trade.getProduct()!= null) {
            Product p = trade.getProduct();
            if (p instanceof SecLending || p instanceof MarginCall || p instanceof Repo || p instanceof TransferAgent || p instanceof Bond) {
                out = null;
            }
        }
        return out;
    }

    public static String removeFieldRepoBond(String value, Trade trade) {
        if (null != trade) {
            if (trade.getProduct() instanceof Bond || trade.getProduct() instanceof Repo) {
                return null;
            }
        }
        return value;
    }

    /**
     * @param message
     * @param trade
     * @param transfer
     * @param dsCon
     * @return
     */
    public static String customizeTag20(BOMessage message, Trade trade, BOTransfer transfer, DSConnection dsCon) {
        String out = "";
        if(null!=trade && null!=message){
            long previousMTMessage = findPreviousMTMessage(message.getTemplateName(), trade, dsCon);
            if(!(previousMTMessage <= 0L)){
                SwiftMessage swiftMessage = SantanderSwiftUtil.getSwiftMessage(previousMTMessage, DSConnection.getDefault());
                if(null!=swiftMessage){
                    SwiftFieldMessage swiftField = swiftMessage.getSwiftField(TAG20, ":SEME", null);
                    return null != swiftField && swiftField.getValue().length()>7 ? swiftField.getValue().substring(7) : "";
                }
            }
        }
        return out;
    }

    /**
     * @param trade
     * @return
     */
    public static boolean isApplicable(BOMessage boMessage, BOTransfer transfer, Trade trade) {
        return null!=trade && trade.getProduct() instanceof SecLending && isEventTypeActionAccepted(boMessage,trade, transfer);
    }

    /**
     * @param transfer
     * @return
     */
    private static boolean isEventTypeActionAccepted(BOMessage boMessage,Trade trade, BOTransfer transfer){
        if(isCustodianAccepted(boMessage)){
            if(null!=transfer && null!=trade && "SecLendingFeeCashPoolDAP".equalsIgnoreCase(transfer.getNettingType())){
                StringBuilder where = new StringBuilder();
                where.append(" trade_id = " + trade.getLongId());
                where.append(" AND netting_key LIKE 'SecLendingFeeCashPoolDAP'");

                String orderBy = "netted_transfer_id DESC";

                TransferArray transfers = null;
                try {
                    transfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(null,where.toString(),orderBy,200,null);
                    if(null!=transfers){
                        for(BOTransfer boTransfer : transfers){
                            if(isTransferAccepted(boTransfer)){
                                return true;
                            }
                        }
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(UtilSecLendingSWIFTFormatter.class,"Error loading transfers: " + e.getCause());
                }
                return false;
            }else{
                return isTransferAccepted(transfer);
            }
        }
        return false;
    }

    private static boolean isTransferAccepted(BOTransfer transfer){
        return Optional.ofNullable(transfer)
                .map(trans -> trans.getAttribute("EventTypeActionName"))
                .filter(att -> Arrays.asList("Partial Return", "Termination", "Full Return").contains(att)).isPresent();
    }


    /**
     * @param messageTemplate
     * @param trade
     * @param dsCon
     * @return
     */
    private static long findPreviousMTMessage(String messageTemplate, Trade trade, DSConnection dsCon){
        StringBuilder where = new StringBuilder();
        where.append(" TRADE_ID = " + trade.getLongId());
        where.append(" AND (MESSAGE_STATUS LIKE 'MATCHED' OR MESSAGE_STATUS LIKE 'SENT')");
        where.append(" AND ADDRESS_METHOD LIKE 'SWIFT'");
        where.append(" AND TEMPLATE_NAME LIKE '"+ mapMTMessagesTemplate(messageTemplate, trade.getProduct())+"'");

        String orderBy = "message_id DESC";

        try {
            MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(null,where.toString(),orderBy,null);
            if(null!=messages && !Util.isEmpty(messages.getMessages())){
                return messages.get(0).getLongId();
            }
        } catch (CalypsoServiceException e) {
            Log.error(UtilSecLendingSWIFTFormatter.class,"Error loading Messages from Trade: " + trade.getLongId() + " : " + e.getCause());
        }
        return 0L;
    }

    /**
     * @param messageTemplate
     * @return
     */
    private static String mapMTMessagesTemplate(String messageTemplate, Product product){
        switch (messageTemplate) {
            case "MT540":
                if (product instanceof SecLending){
                    return "MT542SecLending";
                }
                return "MT542";
            case "MT541":
                if (product instanceof SecLending){
                    return "MT543SecLending";
                }
                return "MT543";
            case "MT542SecLending":
            case "MT542":
                return "MT540";
            case "MT543SecLending":
            case "MT543":
                return "MT541";
            default:
                return "";
        }
    }

    private static boolean isCustodianAccepted(BOMessage boMessage){
        try {
            StaticDataFilter sl_mig = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter("RELA");
            if(null!=sl_mig && null!=boMessage){
                return sl_mig.accept(boMessage);
            }

        } catch (CalypsoServiceException e) {
            Log.error("","Error loading SD Filter: " + e);
        }
        return false;
    }

    public static String customMessageParameter(BOTransfer transfer, String parameter, DSConnection dsCon) {
        if (transfer != null){
            Product security = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());
            if (security == null){
                return "";
            }
            String isActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", parameter);
            if (Util.isEmpty(isActive)){
                return "";
            }
            String [] productTypes = isActive.trim().split(",");
            boolean isProductAccepted = Arrays.stream(productTypes).anyMatch(s -> s.trim().equalsIgnoreCase(security.getType()));

            TradeTransferRule tRule = transfer.toTradeTransferRule();
            SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), tRule.getProcessingOrgSDId());
            String attr = sd.getAttribute(parameter);
            if (!Util.isEmpty(attr) && isProductAccepted) {
                return attr;
            }
        }
        return "";
    }

    public static String customMessageParameterProductType(BOTransfer transfer, String parameter, DSConnection dsCon) {
        if (transfer != null){
            Product security = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());
            if (security == null){
                return "";
            }
            String isActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", parameter);
            if (Util.isEmpty(isActive)){
                return "";
            }
            String [] productTypes = isActive.trim().split(",");
            boolean isProductAccepted = Arrays.stream(productTypes).anyMatch(s -> s.trim().equalsIgnoreCase(transfer.getProductType()));

            TradeTransferRule tRule = transfer.toTradeTransferRule();
            SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), tRule.getProcessingOrgSDId());
            String attr = sd.getAttribute(parameter);
            if (!Util.isEmpty(attr) && isProductAccepted) {
                return attr;
            }
        }
        return "";
    }

    public static boolean isBond(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con){
        Vector<String> bondTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "G.Bonds");
        if (!Util.isEmpty(bondTypes)) {
            if (null != trade && !Util.isEmpty(trade.getProductType())) {
                return Arrays.stream(bondTypes.toArray()).map(String.class::cast).anyMatch(trade.getProductType()::equalsIgnoreCase);
            }
            if (null != transfer && !Util.isEmpty(transfer.getProductType())) {
                return Arrays.stream(bondTypes.toArray()).map(String.class::cast).anyMatch(transfer.getProductType()::equalsIgnoreCase);
            }
        }
        return false;
    }
}

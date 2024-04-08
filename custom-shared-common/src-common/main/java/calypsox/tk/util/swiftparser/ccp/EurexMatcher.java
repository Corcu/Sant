package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

public class EurexMatcher extends GenericPlatformMatcher {

    private final static String[] SETTLE_CONFO_TYPES = {"MT544", "MT545", "MT546", "MT547"};

    private final static String[] DAP_CONFO_TYPES = {"MT545", "MT547"};
    private final int ECAG_ID = BOCache.getLegalEntityId(DSConnection.getDefault(), "ECAG");


    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors)
            throws MessageParseException {
        if (swiftMess instanceof SwiftMessage && ds != null) {

            Vector<String>  indexingErrors = new Vector<> ();

            TransferArray result = indexByBOTransferFields(swiftMess, env, ds, dbCon, indexingErrors);

            if (!Util.isEmpty(result)) {
                errors.clear();
                errors.addAll(indexingErrors);
                if (result.size() == 1 || Arrays.stream(result.getTransfers()).filter(Objects::nonNull).noneMatch(t -> "DAP".equals(t.getDeliveryType()))) {
                    return result.get(0);
                } else {
                    SwiftMessage swiftMessage = (SwiftMessage) swiftMess;
                    String partialSettlementType = getPartialSettlement(swiftMessage);
                    Double cash = "PARS//PAIN".equals(partialSettlementType) ? getRemainingCash(swiftMessage) : getCashTotal(swiftMessage);
                    final Double cashAmount = cash != null ? cash : getCashTotal(swiftMessage);
                    final String payRec = getPayRec(swiftMessage);
                    List<BOTransfer> sortedXferas = result.stream().sorted((xfer1, xfer2) -> {
                        double msgAmount ="PAY".equals(payRec) ? cashAmount : -cashAmount;
                        double diff = Math.abs(getCashAmount(xfer1) - msgAmount) - Math.abs(getCashAmount(xfer2) - msgAmount);
                        return diff < 0 ? -1 : diff > 0 ? 1 : 0;
                    }).collect(Collectors.toList());

                    return sortedXferas.get(0);


                }
            } else {
                errors.addAll(indexingErrors);
            }
        }
        return null;
    }

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) {
        return null;
    }

    @Override
    public TransferArray indexByBOTransferFields(ExternalMessage externalMessage, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) {
        if (externalMessage instanceof SwiftMessage && ds != null && !Util.isEmpty(externalMessage.getType())) {
            SwiftMessage swiftMessage = (SwiftMessage) externalMessage;
            if (Arrays.stream(SETTLE_CONFO_TYPES).anyMatch(t -> t.equals(swiftMessage.getType()))) {
                if (isPartialSettle(swiftMessage)) {
                    return indexMT544toMT547PartialSettleTransfers(swiftMessage, ds);
                } else {
                    return indexMT544toMT547TotalConciliationTransfers(swiftMessage, ds);
                }
            } else if ("MT548".equals(swiftMessage.getType())) {
                return indexMT548ByParameters(swiftMessage, ds);
            }
        }

        return null;
    }

    protected SQLQuery filterByTradeDate(SQLQuery query, JDate tradeDate) {
        return query;
    }


    private TransferArray indexMT548ByParameters(SwiftMessage swiftMessage, DSConnection dsCon) {
        TransferArray result = getBOTransfers(buildMT548TransfersQuery(swiftMessage), dsCon);
        return filterTransfersByBIC(swiftMessage, result, dsCon);
    }

    protected SQLQuery buildSQLQuery() {
        SQLQuery query = new SQLQuery();
        query = filterBySettleTransfers(query);
        query.appendWhereClause(" bo_transfer.orig_cpty_id = " + ECAG_ID);
        return query;
    }

    private double getCashAmount(BOTransfer xfer) {

        return "SECURITY".equals(xfer.getTransferType())
                ? "PAY".equals(xfer.getPayReceive()) ? Math.abs(xfer.getRealCashAmount()) : -Math.abs(xfer.getRealCashAmount())
                : "PAY".equals(xfer.getPayReceive()) ? -Math.abs(xfer.getRealSettlementAmount()) : Math.abs(xfer.getRealSettlementAmount());
    }

    protected SQLQuery buildMT548TransfersQuery(SwiftMessage message) {
        String ccy = getCcyValue(message);

        String isin = getIsinValue(message);
        Double nominalTotal = getNominalTotal(message);
        Double cashTotal = getCashTotal(message);
        String poAccount = getMT548POAccount(message);
        String payRec = getPayRec(message);
        String swiftDeliveryType = getMT548DeliveryType(message);
        String deliveryType = Util.isEmpty(swiftDeliveryType)
                ? cashTotal == null ? "DFP" : "DAP"
                : "APMT".equals(swiftDeliveryType)
                ? "DAP"
                : "FREE".equals(swiftDeliveryType)
                ? "DFP"
                : null;

        // JDate tradeDate = getTradeDate(message);
        JDate settleDate = getSettleDate(message);

        if (!Util.isEmpty(isin) && nominalTotal != null
                && (cashTotal != null || "DFP".equals(deliveryType)) && !Util.isEmpty(poAccount) && !Util.isEmpty(payRec) && settleDate != null) {

            SQLQuery query = buildSQLQuery();
            query = filterByISIN(query, isin);

            query = filterByCcy(query, ccy);

            query = filterByNominalTotal(query, nominalTotal);
            if (cashTotal != null)
                query = filterByTotalCash(query, cashTotal);
            query = filterByPOAccount(query, poAccount);
            query = filterByPayRec(query, payRec);
            query = filterByDeliveryType(query, deliveryType);
            return filterBySettleDate(query, settleDate);


        }
        return null;
    }

    private String getMT548POAccount(SwiftMessage swift) {
        List<SwiftFieldMessage> setTran = SwiftFieldMessage.getSwiftSequence(swift.getFields(), ":16R:", ":16S:", "SETTRAN", 1);

        if (!Util.isEmpty(setTran)) {
            SwiftFieldMessage poAccountField = SwiftFieldMessage.findSwiftField(setTran, ":97A:", ":SAFE/", null);
            if (poAccountField == null) {
                poAccountField = SwiftFieldMessage.findSwiftField(setTran, ":97B:", ":SAFE/", null);
            }
            if (poAccountField != null) {
                try {

                    return (String) poAccountField.parse(swift.getType());
                } catch (MessageParseException e) {
                    Log.error("SecurityMatcher", "unable to return the POACCOUNT " + poAccountField, e);

                }
            }
        }
        return null;
    }

    protected String getMT548DeliveryType(SwiftMessage swift) {
        List<SwiftFieldMessage> setTran = SwiftFieldMessage.getSwiftSequence(swift.getFields(), ":16R:", ":16S:", "SETTRAN", 1);

        if (!Util.isEmpty(setTran)) {
            SwiftFieldMessage deliveryMthdField = SwiftFieldMessage.findSwiftField(setTran, ":22H:", ":PAYM/", null);
            if (deliveryMthdField != null) {
                try {
                    return (String) deliveryMthdField.parse(swift.getType());
                } catch (MessageParseException e) {
                    Log.error("SecurityMatcher", "unable to return the DELIVERY METHOD " + deliveryMthdField, e);

                }
            }
        }
        return null;
    }
}

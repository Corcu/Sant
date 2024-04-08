package com.calypso.tk.bo;

import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CustomSplitProductHandler extends BOProductHandler {


    /**
     * Enable nominal split limit configuration. Logic is build to be only launched from WFRules, with only one source transfer.
     *
     * @param trade
     * @param transfers  - Logic is assuming that this Vector have one transfer only,
     *                   as this method must always be called from a WFRule
     * @param exceptions
     * @param dsCon
     */
    public static void addNominalSplitTransfers(Trade trade, Vector<BOTransfer> transfers, Vector exceptions, DSConnection dsCon) {
       /* Optional.ofNullable(transfers)
                .filter(xferVector -> xferVector.size() == 1)
                .ifPresent(xferVector -> processNominalSplitTransfer(xferVector, trade, exceptions, dsCon));*/
        if(!Util.isEmpty(transfers) && transfers.size()==1){
            processNominalSplitTransfer(transfers,trade,exceptions,dsCon);
        }
    }


    /**
     * If no SplitNominal attribute is set inside any of the SDIs then core com.calypso.tk.bo.BOProductHandler#addSplitTransfers(com.calypso.tk.core.Trade, java.util.Vector, java.util.Vector, com.calypso.tk.service.DSConnection)
     * is called
     *
     * @param xferVector
     * @param trade
     * @param exceptions
     * @param dsCon
     */
    private static void processNominalSplitTransfer(Vector<BOTransfer> xferVector, Trade trade, Vector exceptions, DSConnection dsCon) {
        BOTransfer xfer = xferVector.get(0);
        Optional<String> splitNominalAmt = Optional.ofNullable(getSplitLimitAmountStringFromTransfer(xfer, dsCon))
                .filter(nominalStr -> !nominalStr.isEmpty());
        if (splitNominalAmt.isPresent()) {
            splitNominalAmt
                    .map(splitNominalAmount -> convertNominalStringToQuantity(xfer, splitNominalAmount, dsCon))
                    .map(splitQuantity -> splitTransfers(xfer, trade, splitQuantity, true, false, false, dsCon))
                    .filter(newXferVector -> !newXferVector.isEmpty())
                    .ifPresent(newXferVector -> {
                        xferVector.clear();
                        xferVector.addAll(newXferVector);
                    });
        } else {
            BOProductHandler.addSplitTransfers(trade, xferVector, exceptions, dsCon);
        }
    }

    private static String getSplitLimitAmountStringFromTransfer(BOTransfer xfer, DSConnection dsCon) {
        return Optional.ofNullable(getSplitLimitAmountStringFromSDI(xfer.getExternalSettleDeliveryId(), dsCon))
                .filter(str -> !str.isEmpty())
                .orElseGet(() -> getSplitLimitAmountStringFromSDI(xfer.getInternalSettleDeliveryId(), dsCon));
    }

    private static String getSplitLimitAmountStringFromSDI(int sdiId, DSConnection dsCon) {
        return Optional.ofNullable(BOCache.getSettleDeliveryInstruction(dsCon, sdiId))
                .map(settleDeliveryInstruction -> settleDeliveryInstruction.getAttribute("SplitNominal"))
                .orElse("");
    }

    private static double convertNominalStringToQuantity(BOTransfer xfer, String nominalAmount, DSConnection dsCon) {
        return Optional.ofNullable(nominalAmount)
                .map(Util::istringToNumber)
                .map(nominal -> nominal / getBondFaceValueFromXfer(xfer, dsCon))
                .orElse(0.0d);
    }

    private static double getBondFaceValueFromXfer(BOTransfer xfer, DSConnection dsConnection) {
        return Optional.ofNullable(BOCache.getExchangedTradedProduct(dsConnection, xfer.getProductId()))
                .filter(product -> product instanceof Bond)
                .map(bond -> ((Bond) bond).getFaceValue(xfer.getValueDate()))
                .orElse(1.0d);
    }
}

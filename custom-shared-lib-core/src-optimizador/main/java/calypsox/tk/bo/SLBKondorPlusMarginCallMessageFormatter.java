/**
 *
 */
package calypsox.tk.bo;

import calypsox.tk.util.AccClosingPosMovements;
import calypsox.tk.util.AccountClosingValues;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.collateral.PreviousPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * @author fperezur
 *
 */
public class SLBKondorPlusMarginCallMessageFormatter extends
        KondorPlusMarginCallMessageFormatter {

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    public static final String SLB_KONDOR_PLUS = "SLBKondorPlus";

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.bo.KondorPlusMarginCallMessageFormatter#parseIS_NEW_TRADE
     * (com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade,
     * com.calypso.tk.refdata.LEContact, com.calypso.tk.refdata.LEContact,
     * java.util.Vector, com.calypso.tk.bo.BOTransfer,
     * com.calypso.tk.service.DSConnection)
     */
    @SuppressWarnings("rawtypes")
    public String parseIS_NEW_TRADE(BOMessage message, Trade trade,
                                    LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {

        // We check if we have two messages to K+ in the same Trade.
        MessageArray msgArray;
        try {
            msgArray = dsConn.getRemoteBO().getMessages(trade.getLongId());
            if (null != msgArray) {
                for (int numMsg = 0; numMsg < msgArray.size(); numMsg++) {
                    BOMessage msg = msgArray.get(numMsg);
                    if ((null != msg)
                            && (message.getLongId() != msg.getLongId())
                            && SLB_KONDOR_PLUS.equals(msg.getMessageType())
                            && "CANCELED".equals(msg.getStatus().getStatus())
                            && ("ACK".equals(msg.getAction().toString()) || (("NACK"
                            .equals(msg.getAction().toString())) && msg
                            .getStatus().toString()
                            .equals("WAITING_CANCEL")))) {
                        return FALSE;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        return TRUE;
    }

    /**
     * Get de Account Closing Values for SLB Messages
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AccountClosingValues getAccountClosingValues(BOMessage message, Trade trade, LEContact po,
                                                        LEContact cp, Vector paramVector, BOTransfer transfer,
                                                        DSConnection dsConn, boolean firstMovement) {
        // First of all, we need to retrieve the Margin Call Contract related to
        // the Trade.
        AccClosingPosMovements posMovements = null;
        try {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            int mcId = Math.toIntExact(marginCall.getLinkedLongId());
            CollateralConfig marginCallConfig = CacheCollateralClient
                    .getCollateralConfig(dsConn, mcId);
            if (null != marginCallConfig) {

                prepareMarginCallEntry(message, dsConn, marginCallConfig,
                        trade.getTradeDate());
                if (null != this.entry) {
                    // We call the necessary methods to manage the cash or
                    // the security depending on the allocation
                    // vectors.
                    if (FALSE.equals(parseIS_MARGINCALL_SECURITY(message,
                            trade, po, cp, paramVector, transfer, dsConn))) {
                        cashPositionsPerCurrency(mcId, message, dsConn);
                    }
                    if (TRUE.equals(parseIS_MARGINCALL_SECURITY(message, trade,
                            po, cp, paramVector, transfer, dsConn))) {
                        posMovements = securityPositionsPerCurrencyMod(mcId,
                                message, dsConn);
                    }
                }

            }
            // We return always the value that we saved in the variable
            // returnDealType.
        } catch (Exception e) {
            Log.error(this, e);
        }

        AccountClosingValues accClosingVals = new AccountClosingValues();
        accClosingVals.setAccClosing(false);
        if (posMovements != null) {
            double diferencia = posMovements.getPositionPrev() + posMovements.getCurrentMovement();

            if (firstMovement) {
                accClosingVals.setAccClosing(false);
            } else if (diferencia == 0 || (diferencia > 0 && posMovements.getPositionPrev() < 0) || (diferencia < 0 && posMovements.getPositionPrev() > 0)) {
                accClosingVals.setAccClosing(true);
                accClosingVals.setClosingVal(Math.abs(posMovements.getPositionPrev()));
                accClosingVals.setOpenVal(diferencia);
            }
        }

        return accClosingVals;

    }

    /**
     * Distribute security allocation per currency and calculation the previous
     * cash position for each currency
     *
     * @param mcId
     *
     * @param message
     * @param dsConn
     */
    private AccClosingPosMovements securityPositionsPerCurrencyMod(int mcId, BOMessage message,
                                                                   DSConnection dsConn) {
        Trade trade = null;
        JDatetime jdateTrade = null;
        String description = new String(), posCcy = new String(),
                movCcy = new String();
        Double positionPrev = 0.0, currentMovement = 0.0;

        // Get the previous position per currency
        Collection<PreviousPosition<SecurityPosition>> prevSecPos = this.entry.getSecurityPositions();

        List<SecurityPosition> prevPos = null;

        for (PreviousPosition<SecurityPosition> pos : prevSecPos) {
            if ("THEORETICAL".equals(pos.getKey())) {
                prevPos = pos.getPositions();
            }
        }

        try {
            // We get the Trade from the message, to avoid use
            // MarginCallAllocation getting the current movement.
            trade = dsConn.getRemoteTrade().getTrade(message.getTradeLongId());
            if (null != trade) {
                MarginCall productMrgCall = (MarginCall) trade.getProduct();
                if (null != productMrgCall) {
                    Product p = productMrgCall.getSecurity();
                    if (p != null) {
                        if (p instanceof Bond) {
                            currentMovement = trade.getQuantity()
                                    * ((Bond) p).getFaceValue();
                            movCcy = trade.getTradeCurrency();
                            // Rounding.
                            currentMovement = roundPositionValue(
                                    currentMovement, movCcy);
                            // We concatenate the information for the
                            // description.
                            description = ((Bond) p).getSecCode(ISIN) + "_"
                                    + ((Bond) p).getCurrency();

                        } else if (p instanceof Equity) {
                            currentMovement = trade.getQuantity();
                            movCcy = trade.getTradeCurrency();
                            // Rounding.
                            currentMovement = roundPositionValue(
                                    currentMovement, movCcy);
                            // We concatenate the information for the
                            // description.
                            description = ((Equity) p).getSecCode(ISIN) + "_"
                                    + ((Equity) p).getCurrency();

                        }
                    }

                }

                // Set the JDate in the Trade.
                jdateTrade = trade.getTradeDate();
            }

            // Sum the previous balance to the calculated position.
            SecurityPosition position = null;
            if (!Util.isEmpty(prevPos)) {
                // We find out the previous position for the currency in the
                // allocation.
                for (int numPos = 0; numPos < prevPos.size(); numPos++) {
                    position = prevPos.get(numPos);
                    if ((null != position)
                            && (position instanceof SecurityPosition)) {
                        if (position.getCurrency()
                                .equals(trade.getSettleCurrency())
                                && position.getDescription()
                                .contains(description)) {
                            positionPrev += position.getNominal();
                            posCcy = position.getCurrency();
                            // Rounding.
                            positionPrev = roundPositionValue(positionPrev,
                                    posCcy);
                            break;
                        }
                    } else {
                        position = null;
                    }
                }
            }

            TradeArray existingMrgCallTrades = DSConnection.getDefault()
                    .getRemoteTrade().getTrades("trade, product_simplexfer",
                            "trade.product_id = product_simplexfer.product_id "
                                    + "and trade.trade_status not in ('CANCELED', 'ERROR', 'PENDING') "
                                    + "and trunc(trade.trade_date_time) = trunc("
                                    + Util.date2SQLString(jdateTrade) + ") "
                                    + "and product_simplexfer.linked_id = "
                                    + mcId
                                    + " and product_simplexfer.flow_type = 'SECURITY'",
                            null, null);

            if (null != existingMrgCallTrades) {
                // Loop to iterate in the correct position.
                for (Trade tradeAlloc : existingMrgCallTrades.getTrades()) {
                    if ((null != tradeAlloc)
                            && (tradeAlloc.getLongId() != trade.getLongId())
                            && trade.getSettleCurrency()
                            .equals(tradeAlloc.getSettleCurrency())
                            && !VERIFIED.equals(
                            tradeAlloc.getStatus().getStatus())) {
                        MarginCall product = (MarginCall) tradeAlloc
                                .getProduct();
                        if (null != product) {
                            Product p = product.getSecurity();
                            if (p != null) {
                                if (p instanceof Bond) {
                                    if (description.equals(p.getSecCode(ISIN)
                                            + "_" + p.getCurrency())) {
                                        positionPrev += tradeAlloc.getQuantity()
                                                * ((Bond) p).getFaceValue();
                                        movCcy = tradeAlloc.getTradeCurrency();
                                        // Rounding
                                        positionPrev = roundPositionValue(
                                                positionPrev, movCcy);
                                    }

                                } else if (p instanceof Equity) {
                                    if (description.equals(p.getSecCode(ISIN)
                                            + "_" + p.getCurrency())) {
                                        positionPrev += tradeAlloc
                                                .getQuantity();
                                        movCcy = tradeAlloc.getTradeCurrency();
                                        // Rounding
                                        positionPrev = roundPositionValue(
                                                positionPrev, movCcy);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        // Call to the method to set the value for the deal type, send in the
        // message to K+.
        setReturnDealTypeVariable(positionPrev, currentMovement);

        // Included to calculate the Account Closing
        AccClosingPosMovements posMovements = new AccClosingPosMovements(positionPrev, currentMovement);
        return posMovements;
    }

    // DEAL_TYPE for Security & TYPE for Cash.
    /* (non-Javadoc)
     * @see calypsox.tk.bo.KondorPlusMarginCallMessageFormatter#parseDEAL_TYPE(com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade, com.calypso.tk.refdata.LEContact, com.calypso.tk.refdata.LEContact, java.util.Vector, com.calypso.tk.bo.BOTransfer, com.calypso.tk.service.DSConnection)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String parseDEAL_TYPE(BOMessage message, Trade trade, LEContact po,
                                 LEContact cp, Vector paramVector, BOTransfer transfer,
                                 DSConnection dsConn) {
        // First of all, we need to retrieve the Margin Call Contract related to
        // the Trade.
        try {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            int mcId = Math.toIntExact(marginCall.getLinkedLongId());
            CollateralConfig marginCallConfig = CacheCollateralClient
                    .getCollateralConfig(dsConn, mcId);
            if (null != marginCallConfig) {
                if ("".equals(this.returnDealType)) {
                    prepareMarginCallEntry(message, dsConn, marginCallConfig,
                            trade.getTradeDate());
                    if (null != this.entry) {
                        // We call the necessary methods to manage the cash or
                        // the security depending on the allocation
                        // vectors.
                        if (FALSE.equals(
                                parseIS_MARGINCALL_SECURITY(message, trade, po,
                                        cp, paramVector, transfer, dsConn))) {
                            cashPositionsPerCurrency(mcId, message, dsConn);
                        }
                        if (TRUE.equals(
                                parseIS_MARGINCALL_SECURITY(message, trade, po,
                                        cp, paramVector, transfer, dsConn))) {
                            securityPositionsPerCurrencyMod(mcId, message, dsConn);
                        }
                    }
                }
            }
            // We return always the value that we saved in the variable
            // returnDealType.
            return this.returnDealType;
        } catch (Exception e) {
            Log.error(this, e);
        }

        return null;
    }

}

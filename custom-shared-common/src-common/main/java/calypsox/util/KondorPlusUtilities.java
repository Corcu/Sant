package calypsox.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.math.BigDecimal;
import java.rmi.RemoteException;

/**
 * @author Jos? David Sevillano Carretero (josedavid.sevillano@siag.es) Class used to specify common methods for the
 * Kondor+ interface.
 */
public class KondorPlusUtilities {

    /**
     *
     */
    private static String slbCode = null;

    /**
     *
     */
    private static final String SANT_SLB_LE = "SANT_SLB_LE";

    /**
     * This method checks if the sign of the balance has changed when the users create a new Margin Call Trade in the
     * system.
     *
     * @param trade New Margin Call Trade created.
     * @param ds    Connection with the DataBase.
     * @return TRUE if the sign of the balance has changed, else FALSE.
     */
    public static boolean hasChangedSignBalance(Trade trade, DSConnection ds, boolean balanceAlreadyUpdated, double currTotalAmount, double sumTotalAmount) {

        double tradeAmount = 0.0;

        try {
            // We get the Margin Call through the trade.
            MarginCall marginCall = ((MarginCall) trade.getProduct());
            if (null != marginCall) {
                // We get the total for the Margin Call Contract.
                if (null != marginCall.getSecurity()) {
                    InventorySecurityPositionArray invSecPosVec = ds
                            .getRemoteBackOffice()
                            .getInventorySecurityPositions(
                                    "POSITION_TYPE = 'THEORETICAL' AND "
                                            + "DATE_TYPE = 'TRADE' AND INTERNAL_EXTERNAL = 'MARGIN_CALL' AND CONFIG_ID = "
                                            + marginCall.getLinkedLongId() + " AND BOOK_ID = " + trade.getBookId()
                                            + " ORDER BY POSITION_DATE ASC", null);
                    if ((null != invSecPosVec) && !invSecPosVec.isEmpty()) {
                        InventorySecurityPosition secPos = invSecPosVec.get(invSecPosVec.size() - 1);
                        tradeAmount = marginCall.getPrincipal();
                        // If we call this method from the Check Rule (we are creating a new Margin Call Trade, without
                        // save the value by the moment).
                        if (!balanceAlreadyUpdated) {
                            currTotalAmount = secPos.getTotal();
                            // We sum to simulate the new amount, to look at if the sign will change or not.
                            sumTotalAmount = currTotalAmount + tradeAmount;
                        } else {
                            sumTotalAmount = secPos.getTotal();
                            // We do the difference between the sum and trade amounts because for this case we saved
                            // previously the balance (to calculate the previous one).
                            currTotalAmount = sumTotalAmount - tradeAmount;
                        }
                    }

                    tradeAmount = trade.getQuantity() * marginCall.getPrincipal();
                } else {
                    InventoryCashPositionArray invCashPosVec = ds.getRemoteBackOffice().getInventoryCashPositions(
                            "POSITION_TYPE = 'THEORETICAL' AND "
                                    + "DATE_TYPE = 'TRADE' AND INTERNAL_EXTERNAL = 'MARGIN_CALL' AND CONFIG_ID = "
                                    + marginCall.getLinkedLongId() + " AND CURRENCY_CODE = '" + trade.getTradeCurrency()
                                    + "' AND BOOK_ID = " + trade.getBookId() + " ORDER BY POSITION_DATE ASC", null);
                    if ((null != invCashPosVec) && !invCashPosVec.isEmpty()) {
                        InventoryCashPosition cashPos = invCashPosVec.get(invCashPosVec.size() - 1);
                        tradeAmount = marginCall.getPrincipal();
                        // If we call this method from the Check Rule (we are creating a new Margin Call Trade, without
                        // save the value by the moment).
                        if (!balanceAlreadyUpdated) {
                            currTotalAmount = cashPos.getTotal();
                            // We sum to simulate the new amount, to look at if the sign will change or not.
                            sumTotalAmount = currTotalAmount + tradeAmount;
                            // Rounding
                            String ccy = cashPos.getCurrency();
                            currTotalAmount = roundPositionValue(currTotalAmount, ccy);
                            sumTotalAmount = roundPositionValue(sumTotalAmount, ccy);
                        } else {
                            sumTotalAmount = cashPos.getTotal();
                            // We do the difference between the sum and trade amounts because for this case we saved
                            // previously the balance (to calculate the previous one).
                            currTotalAmount = sumTotalAmount - tradeAmount;
                            // Rounding
                            String ccy = cashPos.getCurrency();
                            sumTotalAmount = roundPositionValue(sumTotalAmount, ccy);
                            currTotalAmount = roundPositionValue(currTotalAmount, ccy);
                        }
                    }
                }

                // We check if the sign for the balance was changed or not.
                if (((sumTotalAmount < 0) && (currTotalAmount > 0)) || ((sumTotalAmount > 0) && (currTotalAmount < 0))) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            Log.error(KondorPlusUtilities.class, e); //sonar
        }

        return false;
    }

    /**
     * @param value    value to round
     * @param currency get the rounding value from the currency
     * @return
     */
    public static double roundPositionValue(Double value, String currency) {
        return BigDecimal.valueOf(value).setScale(CurrencyUtil.getRoundingUnit(currency), BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    /**
     * This method checks if the trade ProcessingOrg is SLB.
     *
     * @param trade
     * @return
     */
    public static boolean isSLB(Trade trade) {
        boolean isSLBTrade = false;
        if (slbCode == null) {
            try {
                slbCode = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(SANT_SLB_LE).firstElement();
            } catch (CalypsoServiceException e) {
                Log.error(KondorPlusUtilities.class, e);
            }
        }
        if (trade != null) {
            LegalEntity po = BOCache.getProcessingOrg(DSConnection.getDefault(), trade.getBook());
            if (po != null && po.getCode().equals(slbCode)) {
                isSLBTrade = true;
            }
        }
        return isSLBTrade;
    }


    /**
     * This method checks if the sign of the balance has changed when the users create a new Margin Call Trade in the
     * system.
     *
     * @param trade
     * @param ds
     * @param balanceAlreadyUpdated
     * @return
     */
    public static boolean hasChangedSignBalance(Trade trade, DSConnection ds, boolean balanceAlreadyUpdated) {

        double currTotalAmount = 0.0;
        double sumTotalAmount = 0.0;
        return hasChangedSignBalance(trade, ds, balanceAlreadyUpdated, currTotalAmount, sumTotalAmount);

    }

}

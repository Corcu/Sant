/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.trading;

import com.calypso.apps.trading.ShowTrade;
import com.calypso.apps.trading.TradeValidator;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * trade validaotr for simple xfer INTEREST which allows to send notification to client
 */
public class SimpleTransferINTERESTTradeValidator implements TradeValidator {

    @Override
    public boolean inputInfo(Trade trade, ShowTrade w) {
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(Trade trade, ShowTrade w, Vector messages) {
        SimpleTransfer p = (SimpleTransfer) trade.getProduct();

        Account account = BOCache.getAccount(DSConnection.getDefault(), Math.toIntExact(p.getLinkedLongId()));

        if (account == null) {
            messages.add("Account id = " + p.getLinkedLongId() + " does not exist.");
            return false;
        }

        SimpleTransfer simpleXfer = (SimpleTransfer) trade.getProduct();

        String leSimpleXfer = BOCache.getLegalEntityCode(DSConnection.getDefault(), simpleXfer.getOrdererLeId());
        String leTrade = trade.getCounterParty().getCode();
        String leAccount = BOCache.getLegalEntityCode(DSConnection.getDefault(), account.getLegalEntityId());

        if (!leSimpleXfer.equals(leAccount) || !leTrade.equals(leAccount)) {
            messages.add("Both legal entity must be equal to account legal entity i.e. " + leAccount + ".");
            return false;
        }

        String kwName = "INTEREST_TRANSFER_FROM";

        String kwValue = trade.getKeywordValue(kwName);

        if (Util.isEmpty(kwValue)) {
            messages.add(kwName + " is empty.");
            return false;
        }
        Integer tradeId = null;
        try {
            tradeId = Integer.valueOf(kwValue);
        } catch (Exception e) {
            Log.error(this, e); //sonar
        }
        if (tradeId == null) {
            messages.add(kwName + " is incorrect - not an integer.");
            return false;
        }

        Trade ibTrade = null;
        try {
            ibTrade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
        }
        if (ibTrade == null) {
            messages.add("Trade id " + tradeId + " does not exist.");
            return false;
        }

        if (!ibTrade.getProductType().equals("InterestBearing")) {
            messages.add("Trade id " + tradeId + " not an Interest Bearing.");
            return false;
        }

        if (!ibTrade.getStatus().getStatus().equals(Status.VERIFIED)) {
            messages.add("Interest Bearing Trade id " + tradeId + " not in VERIFIED status.");
            return false;
        }

        if (!isAdHoc(account)) {
            return true;
        }
        // For AdHoc only
        InterestBearing ibProduct = (InterestBearing) ibTrade.getProduct();
        if (ibProduct.getEndDate().before(trade.getSettleDate())) {
            messages.add("Interest Bearing Trade id " + tradeId + " end date before Notification settle date.");
            messages.add("Please choose the current Interest Bearing Trade Id.");
            return false;
        }
        if (ibProduct.getStartDate().after(trade.getSettleDate())) {
            messages.add("Interest Bearing Trade id " + tradeId + " start date after Notification settle date.");
            messages.add("Please choose the current Interest Bearing Trade Id.");
            return false;
        }
        return true;
    }

    private boolean isAdHoc(Account account) {
        String adHoc = account.getAccountProperty("PayInterestAdHoc");
        if (Util.isEmpty(adHoc)) {
            return false;
        }
        if (adHoc.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

}

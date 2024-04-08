/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util.interfaceImporter;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.util.TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE;
import static calypsox.util.TradeInterfaceUtils.TRADE_KWD_BO_SYSTEM;

/**
 * Thread to recover trades in bunchs of 999 items based on a list of bo references
 *
 * @author xIS16412
 */
public class SantDerivativeTradesLoader_old extends Thread {

    public final static int SQL_IN_ITEM_COUNT = 999;

    private List<String> tradesReferencesList;
    private Map<String, SantTradeContainer> tradesMap;


    public SantDerivativeTradesLoader_old(List<String> boReferenceList) {

        this.tradesReferencesList = boReferenceList;
        this.tradesMap = new HashMap<String, SantTradeContainer>(SQL_IN_ITEM_COUNT + 1);
    }

    public SantDerivativeTradesLoader_old(String[] boReferenceArray) {

        this(new ArrayList<String>(Arrays.asList(boReferenceArray)));
    }

    /**
     * Thead
     */
    public void run() {

        if (!this.tradesMap.isEmpty())
            return;

        if (this.tradesReferencesList.size() <= SQL_IN_ITEM_COUNT) {

            final String fromQuery = "trade, trade_keyword kwd1";
            final StringBuffer whereQuery = new StringBuffer("trade.trade_status<>'CANCELED' and trade.trade_id=kwd1.trade_id and "
                    + "kwd1.keyword_name='BO_REFERENCE' and kwd1.keyword_value IN ");
            whereQuery.append(Util.collectionToSQLString(tradesReferencesList));

            TradeArray existingTrades = null;
            try {
                existingTrades = DSConnection
                        .getDefault().getRemoteTrade().getTrades(fromQuery, whereQuery.toString(), null, null);
            } catch (RemoteException e) {
                Log.error(this, e.toString());
                Log.error(this, e);//sonar
            }

            for (Trade trade : existingTrades.getTrades()) {

                if (trade.getKeywordValue(TRADE_KWD_BO_REFERENCE) == null || trade.getKeywordValue(TRADE_KWD_BO_SYSTEM) == null) {
                    Log.error(this, "trade has missing bo reference or system. Trade id: " + trade.getLongId());
                    continue;
                }
                if (trade.getProduct() != null && !(trade.getProduct() instanceof CollateralExposure)) {
                    Log.error(this, "This trade is not a Collateral Exposure: " + trade.getLongId() + " External: " + trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                    continue;
                }

                final String key = trade.getKeywordValue(TRADE_KWD_BO_REFERENCE).trim() + trade.getKeywordValue(TRADE_KWD_BO_SYSTEM).trim();
                SantTradeContainer tradeContainer = new SantTradeContainer(trade);

                if (this.tradesMap.containsKey(key))
                    this.tradesMap.get(key).tradeDuplicate();

                else
                    this.tradesMap.put(key, tradeContainer);
            }

        } else {
            Log.error(SantDerivativeTradesLoader.class, "Not possible to recover more than " + SQL_IN_ITEM_COUNT + " trades per query. It has been requested " + this.tradesReferencesList.size() + " items");
        }
    }

    /**
     * @return map of {bo_reference+bo_system, SantTradeContainer}
     */
    public Map<String, SantTradeContainer> getTradesMap() {
        return tradesMap;
    }

}

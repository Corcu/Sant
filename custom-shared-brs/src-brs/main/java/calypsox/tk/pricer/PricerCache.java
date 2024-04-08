/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.pricer;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradePrice;

// CAL_ACC 591
/**
 * The Class PricerCache.
 */
public class PricerCache {

    private static final int DEFAULT_SIZE = 1000;
    private HashMap<Long, HashMap<String, TradePrice>> cache;

    /**
     * Instantiates a new pricer cache.
     */
    public PricerCache() {
        this.cache = new HashMap<Long, HashMap<String, TradePrice>>(
                DEFAULT_SIZE);
    }

    /**
     * Inits the.
     * 
     * @param ds
     *            the ds
     * @param oldDate
     *            the old date
     * @param newDate
     *            the new date
     * @param trades
     *            the trades
     * @throws RemoteException
     *             the remote exception
     */
    @SuppressWarnings("unchecked")
    public void init(final DSConnection ds, final JDate oldDate,
            final JDate newDate, final long[] trades) throws RemoteException {

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("valuation_date between "
                + Util.date2SQLString(oldDate) + " AND "
                + Util.date2SQLString(newDate));

        /* Add trade ids to where clause */
        whereClause.append(" AND trade_id IN (");
        for (int i = 0; i < (trades.length - 1); i++) {
            whereClause.append(trades[i]);
            whereClause.append(", ");
        }
        whereClause.append(trades[trades.length - 1] + ")");

        putAll(ds.getRemoteTrade().getTradePrices(null, whereClause.toString(),null));
    }

    /**
     * Put.
     * 
     * @param price
     *            the price
     */
    public void put(final TradePrice price) {
        // Fixed by Jose Luis Cuenca
        if (this.cache == null) {
            this.cache = new HashMap<Long, HashMap<String, TradePrice>>(
                    DEFAULT_SIZE);
        }

        final long tradeId = price.getTradeId();
        if (!this.cache.containsKey(tradeId)) {
            this.cache.put(tradeId, new HashMap<String, TradePrice>());
        }
        final HashMap<String, TradePrice> internalHashMap = this.cache
                .get(tradeId);
        internalHashMap.put(getKey(price), price);
    }

    /**
     * Put all.
     * 
     * @param list
     *            the list
     */
    public void putAll(final List<TradePrice> list) {
        if (list != null) {
            final Iterator<TradePrice> i = list.iterator();
            while (i.hasNext()) {
                put(i.next());
            }
        }
    }

    /**
     * Gets the.
     * 
     * @param price
     *            the price
     * @return the trade price
     */
    public TradePrice get(final TradePrice price) {
        return get(price.getTradeId(), price.getMeasureId(), price.getValDate());
    }

    /**
     * Gets the.
     * 
     * @param trade
     *            the trade
     * @param measureId
     *            the measure id
     * @param date
     *            the date
     * @return the trade price
     */
    public TradePrice get(final long trade, final int measureId, final JDate date) {
        final HashMap<String, TradePrice> internalHashMap = this.cache
                .get(trade);
        TradePrice rst = null;
        final String key = getKey(measureId, date);
        if (internalHashMap != null) {
            rst = internalHashMap.get(key);
        }
        return rst;
    }

    /**
     * Removes the.
     * 
     * @param trade
     *            the trade
     */
    public void remove(final long trade) {
        this.cache.get(trade).clear();
        this.cache.remove(trade);
    }

    /**
     * Clear.
     */
    public void clear() {
        this.cache.clear();
    }

    private String getKey(final int measureId, final JDate date) {
        final StringBuffer rst = new StringBuffer();

        rst.append(measureId);
        rst.append("----");
        rst.append(date.getJulian());

        return rst.toString();
    }

    private String getKey(final TradePrice price) {
        String rst = "";

        if (price != null) {
            rst = getKey(price.getMeasureId(), price.getValDate());
        }

        return rst;
    }
}

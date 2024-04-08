/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.util;

import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

// COL_OUT_016

/**
 * Stores a collection of PLMarks for each trade.
 *
 * @author Carlos Cejudo
 */
public class PLMarksMap {
    private static final int MAX_ITEMS_IN_CLAUSE = 1000;

    private Map<Long, Collection<PLMark>> map = null;

    /**
     * Builds a new empty map of PLMarks.
     */
    public PLMarksMap() {
        this.map = new HashMap<>();
    }

    /**
     * Builds a new map with the PLMarks of the trades in the passed array.
     *
     * @param tradeArray An array of trades
     * @throws RemoteException If there is a problem retrieving data from the database
     */
    public PLMarksMap(TradeArray tradeArray) throws RemoteException {
        this();
        addPLMarks(tradeArray);
    }

    /**
     * Builds a new map with the PLMarks of the trades in the passed array using the specified connection.
     *
     * @param dsConn     A connection with the Data Server
     * @param tradeArray An array of trades
     * @throws RemoteException If there is a problem retrieving data from the database
     */
    public PLMarksMap(DSConnection dsConn, TradeArray tradeArray) throws RemoteException {
        this();
        addPLMarks(dsConn, tradeArray);
    }

    /**
     * Adds a PLMark to the map.
     *
     * @param plMark The PLMark to add
     */
    public void add(PLMark plMark) {
        long tradeId = plMark.getTradeLongId();
        Collection<PLMark> plMarks = this.map.get(tradeId);
        if (plMarks == null) {
            plMarks = new ArrayList<>();
            this.map.put(tradeId, plMarks);
        }
        plMarks.add(plMark);
    }

    /**
     * Gets every PLMark for the specified trade.
     *
     * @param tradeId The id of the trades which PLMarks are wanted
     * @return A collection of every PLMark for the trade
     */
    public Collection<PLMark> get(long tradeId) {
        return this.map.get(tradeId);
    }

    /**
     * Adds to the map every PLMark for every trade in the specified array.
     *
     * @param tradeArray An array of trades
     * @throws RemoteException If there is a problem retrieving data from the database
     */
    public void addPLMarks(TradeArray tradeArray) throws RemoteException {
        addPLMarks(DSConnection.getDefault(), tradeArray);
    }

    /**
     * Adds to the map every PLMark for every trade in the specified array using the passed connection to the Data
     * Server.
     *
     * @param dsConn     A connection to the Data Server
     * @param tradeArray An array of trades
     * @throws RemoteException If there is a problem retrieving data from the database
     */
    public void addPLMarks(DSConnection dsConn, TradeArray tradeArray) throws RemoteException {
        int numTrades = tradeArray.size();
        int startIdx = 0;
        int endIdx = Math.min((startIdx + MAX_ITEMS_IN_CLAUSE) - 1, numTrades - 1);
        while ((startIdx < numTrades) && (endIdx < numTrades)) {
            String whereClause = getTradeIdsWhereClause(tradeArray, startIdx, endIdx);
            @SuppressWarnings("deprecation")
            Collection<PLMark> plMarks = dsConn.getRemoteMarketData().getPLMarks(whereClause, Collections.EMPTY_LIST);
            for (PLMark plMark : plMarks) {
                add(plMark);
            }

            startIdx = endIdx + 1;
            endIdx = Math.min((startIdx + MAX_ITEMS_IN_CLAUSE) - 1, numTrades - 1);
        }
    }

    /**
     * Builds the where clause to use to retrieve the PLMarks of a series of trades. This method uses only the trades in
     * a subarray between the specified indices.
     *
     * @param vTrades  An array of trades
     * @param startIdx The start index of the subarray of trades to be used
     * @param endIdx   The end index of the subarray
     * @return The where clause
     */
    private String getTradeIdsWhereClause(TradeArray vTrades, int startIdx, int endIdx) {
        StringBuilder where = new StringBuilder("TRADE_ID IN (");

        for (int idx = startIdx; idx <= endIdx; idx++) {
            where.append(vTrades.get(idx).getLongId());
            if (idx < endIdx) {
                where.append(",");
            }
        }
        where.append(")");

        return where.toString();
    }
}

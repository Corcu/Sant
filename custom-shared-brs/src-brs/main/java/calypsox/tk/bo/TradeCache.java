package calypsox.tk.bo;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

/**
 * This class maintains a hashmap based on its external reference or by the
 * trade id.
 */
public class TradeCache {
    // ORA-01795: max number of expressions in a list is 1000
    /** The Constant BATCH_SIZE. */
    public static final int BATCH_SIZE = 1000;

    private final HashMap<String, Trade> tradeCacheByExternalRef;
    private final HashMap<Long, Trade> tradeCacheByTradeId;

    /**
     * Gets the trade cache by trade id.
     * 
     * @return the trade cache by trade id
     */
    public HashMap<Long, Trade> getTradeCacheByTradeId() {
        return this.tradeCacheByTradeId;
    }

    /**
     * Instantiates a new trade cache.
     */
    public TradeCache() {
        this.tradeCacheByExternalRef = new HashMap<String, Trade>();
        this.tradeCacheByTradeId = new HashMap<Long, Trade>();
    }

    /**
     * Gets the trade.
     * 
     * @param tradeId
     *            the trade id
     * @return the trade
     */
    public Trade getTrade(final long tradeId) {
        final Trade trd = this.tradeCacheByTradeId.get(tradeId);
        return trd;
    }

    /**
     * Gets the trade.
     * 
     * @param extRef
     *            the ext ref
     * @return the trade
     */
    public Trade getTrade(final String extRef) {
        return this.tradeCacheByExternalRef.get(extRef);
    }

    private StringBuilder getWhereClause(final Vector<String> extRefs,
            final int n) {
        final String[] extRefsArray = extRefs.toArray(new String[] {});

        final StringBuilder where = new StringBuilder(
                "trade.external_reference in (");
        boolean first = true;
        final int start = BATCH_SIZE * n;
        int end = BATCH_SIZE * (n + 1);
        if (extRefsArray.length < end) {
            end = extRefsArray.length;
        }

        for (int i = start; i < end; i++) {
            if (!first) {
                where.append(", ");
            } else {
                first = false;
            }
            where.append('\'');
            where.append(extRefsArray[i]);
            where.append('\'');
        }
        where.append(')');
        return where;
    }

    /**
     * Load.
     * 
     * @param ds
     *            the ds
     * @param extRefs
     *            the ext refs
     * @throws RemoteException
     *             the remote exception
     */
    public void load(final DSConnection ds, final Vector<String> extRefs)
            throws RemoteException {

        final int iterations = ((extRefs.size() - 1) / BATCH_SIZE) + 1;
        for (int i = 0; i < iterations; i++) {
            final String where = getWhereClause(extRefs, i).toString();
            final TradeArray tradeArray = ds.getRemoteTrade().getTrades("trade", where, null, false, null);
            populateMaps(tradeArray);
        }
    }

    private void populateMaps(final TradeArray tradeArray) {
        if (!tradeArray.isEmpty()) {
            @SuppressWarnings("unchecked")
            final Iterator<Trade> i = tradeArray.iterator();
            while (i.hasNext()) {
                final Trade trade = i.next();

                // Take care. The external reference is not unique because can
                // be a mirror trade.
                // The original trade is what has the lower id, and this is
                // which we want to insert in the map
                final Trade oldTrade = this.tradeCacheByExternalRef.get(trade
                        .getExternalReference());
                if (oldTrade != null) {
                    if (oldTrade.getLongId() > trade.getLongId()) {
                        this.tradeCacheByExternalRef.put(
                                trade.getExternalReference(), trade);
                    }
                } else {
                    this.tradeCacheByExternalRef.put(
                            trade.getExternalReference(), trade);
                }
                // but in the cache by Trade Id both will be included
                this.tradeCacheByTradeId.put(trade.getLongId(), trade);
            }
        }

    }

    /**
     * Adds a single trade.
     * 
     * ONLY FOR TESTING PURPOSE!!!!!!!!!!!!!!!!!!
     * 
     * @param trade
     *            Trade to be added.
     */
    public void add(final Trade trade) {
        this.tradeCacheByTradeId.put(trade.getLongId(), trade);
    }

    /**
     * Adds a single trade.
     * 
     * ONLY FOR TESTING PURPOSE!!!!!!!!!!!!!!!!!!
     * 
     * @param externalRef
     *            External Reference.
     * @param trade
     *            Trade to be added.
     */
    public void add(final String externalRef, final Trade trade) {
        this.tradeCacheByTradeId.put(trade.getLongId(), trade);
        this.tradeCacheByExternalRef.put(externalRef, trade);
    }
}

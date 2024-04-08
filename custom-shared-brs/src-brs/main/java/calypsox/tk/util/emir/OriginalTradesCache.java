package calypsox.tk.util.emir;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OriginalTradesCache {
  private static OriginalTradesCache instance = null;

  private Map<Long, Trade> tradesCache = null;

  private OriginalTradesCache() {
    tradesCache = new TreeMap<Long, Trade>();
  }

  public static OriginalTradesCache getInstance() {
    if (instance == null) {
      instance = new OriginalTradesCache();
    }

    return instance;
  }

  public Trade getTrade(long tradeId) {
    Trade trade = tradesCache.get(tradeId);

    if (trade == null) {
      try {
        final List<?> trades = DSConnection.getDefault().getRemoteTrade().getTradeOrArchived(tradeId);
        if (!Util.isEmpty(trades) && trades.get(0) instanceof Trade) {
          trade = (Trade) trades.get(0);
          if (trade != null) {
            tradesCache.put(tradeId, trade);
          }
        }

      } catch (final RemoteException e) {
        Log.error(this, "Could not retrieve trade id: " + tradeId, e);
      }
    }

    return trade;
  }

  public void init(List<Trade> trades) {
    for (final Trade trade : trades) {
      tradesCache.put(trade.getLongId(), trade);
    }
  }
}

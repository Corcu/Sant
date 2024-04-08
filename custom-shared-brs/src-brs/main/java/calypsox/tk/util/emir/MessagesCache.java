package calypsox.tk.util.emir;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.rmi.RemoteException;

public class MessagesCache {
  private static MessagesCache instance = null;
  private MessageArray messages = null;
  private long currentTradeId = 0;

  private MessagesCache() {
    // messages = new ArrayList<BOMessage>();
  }

  public static MessagesCache getInstance() {
    if (instance == null) {
      instance = new MessagesCache();
    }

    return instance;
  }

  public MessageArray getMessages(long tradeId) {
    if (currentTradeId != tradeId) {
      try {
        messages = DSConnection.getDefault().getRemoteBO()
            .getMessages(tradeId);
        currentTradeId = tradeId;
      } catch (final RemoteException e) {
        Log.error(this,
            "Could not retrieve messages from trade " + tradeId);
      }
    }

    return messages;
  }

}

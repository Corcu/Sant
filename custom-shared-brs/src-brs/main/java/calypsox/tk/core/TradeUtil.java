package calypsox.tk.core;

import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

/**
 * The Class TradeUtil.
 */
public class TradeUtil {

  private static final int ONE_WEEK_AGO = -7;

  private static final String KEYWORDJMSREFERENCE = "JMSReference";

  private static TradeUtil instance;

  private TradeUtil() {
    // nothing to do
  }

  /**
   * Gets the single instance of TradeUtil.
   *
   * @return single instance of TradeUtil
   */
  public static synchronized TradeUtil getInstance() {
    if (instance == null) {
      instance = new TradeUtil();
    }
    return instance;
  }

  /**
   * Only to JUnit. You can use that to replace the instance with a mockito
   * instance
   *
   * @param mockInstance
   *            the new instance
   */
  public static void setInstance(final TradeUtil mockInstance) {
    instance = mockInstance;
  }


  /**
   * Gets the trade from murex reference.
   *
   * @param murexReference
   *            the murex reference
   * @return the trade from murex reference
   */
  public Trade getTradeFromMurexReference(final String murexReference) {

    TradeArray trades = null;
    try {
      trades = DSConnection.getDefault().getRemoteTrade()
          .getTradesByExternalRef(murexReference);
    } catch (final RemoteException e) {
      Log.error("calypsox.tk.bo.core.TradeUtil", e.getMessage(), e);
    }

    //    if (trades != null) {
    //      for (int i = trades.size() - 1; i >= 0; i--) {
    //        final Trade trade = trades.elementAt(i);
    //        if (Status.S_TERMINATED.equals(trade.getStatus())) {
    //            trades.remove(i);
    //        }
    //      }
    //    }

    // If it's a mirror we can have 2 elements
    if ((trades != null) && (trades.size() == 2)) {
      final Trade trade1 = trades.elementAt(0);
      final Trade trade2 = trades.elementAt(1);
      // We check these two trades are mirror
      if (trade1.getMirrorTradeId() != trade2.getLongId()) {
        return null;
      } else {
        // We return the Master - minor Id
        if (trade1.getLongId() < trade2.getLongId()) {
          return trade1.clone();
        } else {
          return trade2.clone();
        }
      }

    } else if ((trades == null) || (trades.size() != 1)) {
      return null;
    } else {
      return trades.firstElement().clone();
    }
  }

   /**
   * Check if trade is internal or not checking the legal entity attribute LEI
   * for both parties.
   *
   * @param trade
   *            Trade
   * @param poAtts
   *            po attributes
   * @param cpAtts
   *            cpty attributes
   * @return true or false if trade is internal or not
   */
  // CAL_EMIR_022
  public boolean isInternal(final Trade trade,
      final Collection<LegalEntityAttribute> poAtts,
      final Collection<LegalEntityAttribute> cpAtts) {
    boolean rst = false;

    // CAL_189_
    if (trade == null) {
      return rst;
    }

    if (poAtts == null | cpAtts == null) {
      return rst;
    }

    final Iterator<LegalEntityAttribute> poIter = poAtts.iterator();
    LegalEntityAttribute currentAtt = null;
    String poLei = "";

    while (poIter.hasNext()) {
      currentAtt = poIter.next();
      if (KeywordConstantsUtil.LE_ATTRIBUTE_LEI.contains(currentAtt
          .getAttributeType())) {
        poLei = currentAtt.getAttributeValue();
      }
    }

    final Iterator<LegalEntityAttribute> cpIter = cpAtts.iterator();
    String cpLei = "";

    while (cpIter.hasNext()) {
      currentAtt = cpIter.next();
      if (KeywordConstantsUtil.LE_ATTRIBUTE_LEI.contains(currentAtt
          .getAttributeType())) {
        cpLei = currentAtt.getAttributeValue();
      }
    }

    // CAL_189_
    if ((trade.getMirrorTradeId() != 0)
        || ((poLei != null) && (cpLei != null) && poLei
            .equalsIgnoreCase(cpLei))) {
      rst = true;
    }

    Log.debug(this, "Is Internal rst: " + rst);
    return rst;
  }


  /**
   * Get the jmsReference from keyword JMSReference with the same ACTION like
   * parameter #ACTION1-JMSCorrelationId1#ACTION2-JMSCorrelationId2#......
   *
   * @param trade
   *            the trade
   * @param action
   *            the action
   * @return the jMS reference
   */
  public String getJMSReference(final Trade trade, final String action) {
    String actionStr = action;
    if (trade != null) {
      Log.debug("calypsox.tk.bo.core.TradeUtil",
          "getJMSReference Start with action=" + actionStr
          + " and trade=" + trade.getLongId());
    } else {
      Log.debug("calypsox.tk.bo.core.TradeUtil",
          "getJMSReference Start with trade= null.");
    }
    // if action=NONE then we must use NEW
    if (actionStr.equals(Action.S_NONE)) {
      actionStr = Action.S_NEW;
    }
    String jmsReference = null;
    if (trade != null) {
      // get the keyword
      final String keyword = trade.getKeywordValue(KEYWORDJMSREFERENCE);
      Log.debug("calypsox.tk.bo.core.TradeUtil",
          "getJMSReference keyword " + KEYWORDJMSREFERENCE + "="
              + keyword);
      if (keyword != null) {
        // We look the last one with the action.
        final String[] list = keyword.split("#");
        for (int i = list.length - 1; i >= 0; i--) {
          final String value = list[i];
          if (value.startsWith(actionStr)) {
            // located
            jmsReference = value.substring(actionStr.length() + 1,
                value.length());
            break;
          }
        } // End for
      } // End if keyword!=null
    } // End if trade!=null
    Log.debug("calypsox.tk.bo.core.TradeUtil",
        "getJMSReference End returning " + jmsReference);
    return jmsReference;
  }

}

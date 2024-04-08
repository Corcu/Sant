package calypsox.tk.util;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

public class CalypsoObjectUtil {

  private static CalypsoObjectUtil instance = null;

  public static CalypsoObjectUtil getInstance() {
    if (instance == null) {
      instance = new CalypsoObjectUtil();
    }

    return instance;
  }

  public static void setInstance(CalypsoObjectUtil mockInstance) {
    instance = mockInstance;
  }

  /**
   * Put the keyword in the trade and apply the action
   *
   * To invoke this method is necessary to define the map as Map<String, Object> myMap = new
   * HashMap<String, Object>();
   *
   * @param  trade
   * @param  actionToPerform
   * @param  tradeKeywords
   *
   * @return
   * @throws CalypsoServiceException
   */
  public boolean updateTrade(final DSConnection dsCon, final Trade trade,
      final Action actionToPerform, final Map<String, Object> tradeKeywords) throws CalypsoServiceException {

    boolean result = false;

    if (trade != null) {
      final Trade clonedTrade = trade.clone();
      addTradeKeywords(clonedTrade, tradeKeywords);

      if (isTradeActionApplicable(dsCon, clonedTrade, actionToPerform)) {
        clonedTrade.setAction(actionToPerform);
        saveRemoteTrade(dsCon, clonedTrade);
        result = true;
      }
    }

    return result;
  }

  /**
   * Add a the trade keywords to the trade, taking into account
   * the type of said trade keyword
   *
   * @param trade
   * @param tradeKeywords
   */
  private void addTradeKeywords(Trade trade, final Map<String, Object> tradeKeywords) {
    if (trade == null || tradeKeywords == null || tradeKeywords.isEmpty()) {
      return;
    }

    final Set<String> keySet = tradeKeywords.keySet();

    for(final String tkName: keySet) {
      if (tradeKeywords.get(tkName) instanceof Date) {
        trade.addKeyword(tkName, (Date) tradeKeywords.get(tkName));
      } else if (tradeKeywords.get(tkName) instanceof Double) {
        trade.addKeyword(tkName, (Double) tradeKeywords.get(tkName));
      } else if (tradeKeywords.get(tkName) instanceof Integer) {
        trade.addKeyword(tkName, (Integer) tradeKeywords.get(tkName));
      } else if (tradeKeywords.get(tkName) instanceof JDate) {
        trade.addKeyword(tkName, (JDate) tradeKeywords.get(tkName));
      } else if (tradeKeywords.get(tkName) instanceof String) {
        trade.addKeyword(tkName, (String) tradeKeywords.get(tkName));
      } else {
        trade.addKeyword(tkName, tradeKeywords.get(tkName).toString());
      }
    }
  }

  /**
   * Get a trade
   *
   * @param  dsCon
   * @param  tradeId
   * @return
   * @throws CalypsoServiceException
   */
  public Trade getTrade(final DSConnection dsCon, final long tradeId) throws CalypsoServiceException {
    return dsCon.getRemoteTrade().getTrade(tradeId);
  }

  /**
   * Check if the action is applicable for the trade
   *
   * @param  dsCon
   * @param  trade
   * @param  action
   * @return
   */
  private boolean isTradeActionApplicable(final DSConnection dsCon, final Trade trade, final Action action) {
    return TradeWorkflow.isTradeActionApplicable(trade, action, dsCon, null);
  }

  /**
   * Save remote trade
   *
   * @param  dsCon
   * @param  trade
   * @return
   * @throws CalypsoServiceException
   */
  public long saveRemoteTrade(final DSConnection dsCon, final Trade trade) throws CalypsoServiceException {
    try {
      return dsCon.getRemoteTrade().save(trade);
    } catch (final CalypsoServiceException e) {
      final String msgError = String.format("Failed to remotely save trade with External Ref. = %s",
          trade.getExternalReference());
      Log.error(CalypsoObjectUtil.class, msgError, e);
      throw new CalypsoServiceException(msgError, e);
    }
  }

  /**
   * Save remote transfer
   *
   * @param  dsCon
   * @param  xfer
   * @return
   * @throws CalypsoServiceException
   */
  public long saveRemoteTransfer(final DSConnection dsCon, final BOTransfer xfer) throws CalypsoServiceException {
    try {
      return dsCon.getRemoteBO().save(xfer, 0, null);
    } catch (final CalypsoServiceException e) {
      final String msgError = String.format("Failed to remotely save transfer with id %d.", xfer.getLongId());
      Log.error(CalypsoObjectUtil.class, msgError, e);
      throw new CalypsoServiceException(msgError, e);
    }
  }

  /**
   * Get a xfer
   *
   * @param  dsCon
   * @param  xferId
   * @return
   * @throws CalypsoServiceException
   */
  public BOTransfer getBOTransfer(final DSConnection dsCon, final long xferId) throws CalypsoServiceException {
    return dsCon.getRemoteBO().getBOTransfer(xferId);
  }

  /**
   * Check if the action is applicable for the BOTransfer
   *
   * @param  dsCon
   * @param  xfer
   * @param  trade
   * @param  action
   * @return
   * @throws CalypsoServiceException
   */
  public boolean isBOTransferActionApplicable(final DSConnection dsCon, final BOTransfer xfer, Trade trade,
      final Action action) throws CalypsoServiceException {
    trade = trade == null ? getTrade(dsCon, xfer.getTradeLongId()) : trade;
    return BOTransferWorkflow.isTransferActionApplicable(xfer, trade, action, dsCon);
  }

  /**
   * Update Transfer.
   *
   * @param  dsCon
   * @param  xfer
   * @param  actionToPerform
   * @param  xferAttributes
   * @return
   * @throws CalypsoServiceException
   * @throws CloneNotSupportedException
   */
  public boolean updateTransfer(final DSConnection dsCon, final BOTransfer xfer, final Action actionToPerform,
      final Map<String, String> xferAttributes) throws CalypsoServiceException, CloneNotSupportedException {

    boolean result = false;

    if (xfer != null) {
      final BOTransfer xferClone = (BOTransfer) xfer.clone();

      if (isBOTransferActionApplicable(dsCon, xferClone, null, actionToPerform)) {
        if (!Util.isEmpty(xferAttributes)) {
          xferAttributes.forEach((attrName, attrValue) -> xferClone.setAttribute(attrName, attrValue));
        }

        xferClone.setAction(actionToPerform);

        // Save BOTransfer
        saveRemoteTransfer(dsCon, xferClone);

        result = true;
      }
    }

    return result;
  }


  /**
   * Save remote boMessage
   *
   * @param  dsCon
   * @param  boMessage
   * @return
   * @throws CalypsoServiceException
   */
  public long saveRemoteBOMessage(final DSConnection dsCon, final BOMessage boMessage) throws CalypsoServiceException {
    try {
      return dsCon.getRemoteBO().save(boMessage, 0, null);
    } catch (final CalypsoServiceException e) {
      final String msgError = String.format("Failed to remotely save boMessage with id %d.", boMessage.getLongId());
      Log.error(CalypsoObjectUtil.class, msgError, e);
      throw new CalypsoServiceException(msgError, e);
    }
  }

  /**
   * Get a boMessage
   *
   * @param  dsCon
   * @param  boMessageId
   * @return
   * @throws CalypsoServiceException
   */
  public BOMessage getBOMessage(final DSConnection dsCon, final long msgId) throws CalypsoServiceException {
    return dsCon.getRemoteBO().getMessage(msgId);
  }

  /**
   * Check if the action is applicable for the BOTransfer
   *
   * @param  dsCon
   * @param  boMessage
   * @param  xfer
   * @param  trade
   * @param  action
   * @return
   * @throws CalypsoServiceException
   */
  public boolean isBOMessageActionApplicable(final DSConnection dsCon, final BOMessage boMessage, BOTransfer xfer,
      Trade trade, final Action action) throws CalypsoServiceException {
    trade = trade == null ? getTrade(dsCon, boMessage.getTradeLongId()) : trade;
    xfer = xfer == null ? getBOTransfer(dsCon, boMessage.getTransferLongId()) : xfer;
    return BOMessageWorkflow.isMessageActionApplicable(boMessage, xfer, trade, action, dsCon);
  }

  /**
   * Update BOMessage.
   *
   * @param  dsCon
   * @param  xfer
   * @param  actionToPerform
   * @param  xferAttributes
   * @return
   * @throws CalypsoServiceException
   * @throws CloneNotSupportedException
   */
  public boolean updateBOMessage(final DSConnection dsCon, final BOMessage boMessage, final Action actionToPerform,
      final Map<String, String> msgAttributes) throws CalypsoServiceException, CloneNotSupportedException {

    boolean result = false;

    if (boMessage != null) {
      final BOMessage boMessageClone = (BOMessage) boMessage.clone();

      if (isBOMessageActionApplicable(dsCon, boMessageClone, null, null, actionToPerform)) {
        if (!Util.isEmpty(msgAttributes)) {
          msgAttributes.forEach((attrName, attrValue) -> boMessageClone.setAttribute(attrName, attrValue));
        }

        boMessageClone.setAction(actionToPerform);

        // Save BOMessage
        saveRemoteBOMessage(dsCon, boMessageClone);

        result = true;
      }
    }

    return result;
  }

}
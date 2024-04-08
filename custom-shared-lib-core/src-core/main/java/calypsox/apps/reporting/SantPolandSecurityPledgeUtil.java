package calypsox.apps.reporting;

import calypsox.tk.report.SantPolandSecurityPledgeReport;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to manage the selection of trades in the SantPolandSecurityPledge report and to
 * reverse trades.
 *
 * @author Carlos Cejudo
 */
public class SantPolandSecurityPledgeUtil {

  /** Stores the id of the trade that is reversed by this trade. */
  public static final String TRADE_KEYWORD_REVERSE_FROM = "ReverseFrom";

  /** Stores the id of the trade that reverses this trade. */
  public static final String TRADE_KEYWORD_REVERSE_TO = "ReverseTo";

  /** Use to check if an action can be performed in a trade. */
  public static final String WORKFLOW_CONFIG_SUBTYPE_ALL = "ALL";

  /**
   * Event Type of the Task Station exception that reminds the user that a trade must be canceled in
   * Murex manually.
   */
  public static final String EVENT_TYPE_REVERSE_TRADE_CANCELED = "EX_REVERSE_TRADE_CANCELED";

  /**
   * When this action is applied to a trade, a pop-up will inform the user that the trade must be
   * canceled in Murex manually.
   */
  public static final Action ACTION_REQUEST_CANCEL = Action.valueOf("REQUEST_CANCEL");

  /**
   * Creates a clone of the given trade with the following changes: <list>
   * <li>The direction of the trade is reversed
   * <li>The trade date and the settlement date of the trade are set to the given process date
   * <li>The keyword ReverseFrom is set to the id of the given trade </list> The returned trade is
   *     ready to be saved.
   *
   * @param trade Trade to be reversed.
   * @param processDate Process Date, usually the current date.
   * @return Reverse trade, ready to be saved.
   */
  public static Trade getReverseTrade(Trade trade, JDate processDate) {
    Trade newTrade = null;

    if (trade != null) {
      try {
        newTrade = (Trade) trade.clone();
        newTrade.setQuantity(-trade.getQuantity());
        newTrade.setTradeDate(processDate.getJDatetime());
        newTrade.setSettleDate(processDate);
        newTrade.addKeywordAsLong(TRADE_KEYWORD_REVERSE_FROM, trade.getLongId());

        Product newProduct = (Product) trade.getProduct().clone();
        newProduct.setId(0);
        newTrade.setProduct(newProduct);

        newTrade.setLongId(0);
        newTrade.setStatus(Status.S_NONE);
        newTrade.setAction(Action.NEW);
      } catch (CloneNotSupportedException e) {
        Log.error(
            SantPolandSecurityPledgeUtil.class.getCanonicalName(),
            "Cannot clone product from trade " + trade.getLongId(),
            e);
        newTrade = null;
      }
    }

    return newTrade;
  }

  /**
   * Returns a clone of the given trade with its keyword ReverseTo set to the given trade id. The
   * returned trade is ready to be saved.
   *
   * @param trade Trade to update.
   * @param reversedTradeId Trade id of the trade that is reversing the given one.
   * @return Trade updated, ready to be saved.
   */
  public static Trade setReversedTo(Trade trade, long reversedTradeId) {
    Trade newTrade = trade.clone();

    newTrade.addKeywordAsLong(TRADE_KEYWORD_REVERSE_TO, reversedTradeId);
    newTrade.setAction(Action.AMEND);

    return newTrade;
  }

  /**
   * Updates the reverse mark in a report row.
   *
   * @param row ReportRow. Contains the logical value of the mark.
   * @param table Graphical representation of the table.
   * @param selectedColumn Index of the selected column, where the mark is.
   * @param selectedRow Index of selected row in the report window.
   */
  public static void updateReverseMark(
      ReportRow row, JTable table, int selectedColumn, int selectedRow) {
    table.setValueAt(getReverseMark(row), selectedRow, selectedColumn);
  }

  /**
   * Gets the reverse mark value from a ReportRow.
   *
   * @param row ReportRow.
   * @return A Boolean with the value of the reverse mark.
   */
  public static Boolean getReverseMark(ReportRow row) {
    Object rawReverseMark = row.getProperty(SantPolandSecurityPledgeReport.PROPERTY_REVERSE_MARK);
    return Boolean.valueOf(
        rawReverseMark != null && rawReverseMark instanceof Boolean && ((Boolean) rawReverseMark));
  }

  /**
   * Tells if an action can be performed on a trade.
   *
   * @param trade The trade on which we want to perform the action.
   * @param action The action we want to perform
   * @return <code>true</code> if the action can be performed or <code>false</code> otherwise.
   */
  public static boolean canPerformAction(Trade trade, Action action) {
    boolean returnValue = false;

    try {
      Book book = trade.getBook();
      if (book != null) {
        LegalEntity processingOrg = book.getLegalEntity();

        ArrayList<?> rawWfConfigs =
            DSConnection.getDefault()
                .getRemoteBackOffice()
                .getWorkflowConfigArray(
                    Task.TRADE_EVENT_CLASS,
                    processingOrg,
                    WORKFLOW_CONFIG_SUBTYPE_ALL,
                    trade.getProductType(),
                    trade.getStatus());

        if (rawWfConfigs != null && rawWfConfigs.size() > 0) {
          List<TaskWorkflowConfig> workflowConfigs = new LinkedList<TaskWorkflowConfig>();
          for (Object rawWfConfig : rawWfConfigs) {
            if (rawWfConfig instanceof TaskWorkflowConfig) {
              workflowConfigs.add((TaskWorkflowConfig) rawWfConfig);
            }
          }

          // If one of the possible transitions contains the given
          // action, we assume the action can be applied to the given
          // trade.
          Iterator<TaskWorkflowConfig> iConfig = workflowConfigs.iterator();
          while (!returnValue && iConfig.hasNext()) {
            TaskWorkflowConfig config = iConfig.next();
            returnValue |= action.equals(config.getPossibleAction());
          }
        }
      }
    } catch (CalypsoServiceException e) {
      Log.error(
          SantPolandSecurityPledgeUtil.class.getCanonicalName(),
          String.format("Cannot retrieve Workflow configurations for trade %d", trade.getLongId()),
          e);
    }

    return returnValue;
  }

  /**
   * Function to retrieve Column index
   *
   * @param reportWindow window
   * @param name name of the column
   * @return index index of the column
   */
  public static int findColumnIndex(ReportWindow reportWindow, String name) {
    int col = reportWindow.getReportPanel().getTableModelWithFocus().findColumn(name);
    if (col == -1) {
      // look for filtered column
      col = reportWindow.getReportPanel().getTableModelWithFocus().findColumn(name + "(F)");
    }
    return col;
  }

  /**
   * Checks if a trade is a reverse trade.
   *
   * @param trade The trade to check.
   * @return <code>true</code> if the given trade is a reverse trade or <code>false</code>
   *     otherwise.
   */
  public static boolean isReverseTrade(Trade trade) {
    return !Util.isEmpty(trade.getKeywordValue(TRADE_KEYWORD_REVERSE_FROM));
  }
}

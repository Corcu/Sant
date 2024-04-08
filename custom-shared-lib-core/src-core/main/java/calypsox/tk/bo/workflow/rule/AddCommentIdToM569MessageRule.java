/** */
package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/** @author fperezur */
public class AddCommentIdToM569MessageRule implements WfMessageRule {

  private static final String ATTR_COMMENT_ID = "CommentId";
  private static final String ATTR_MC_ENTRY_ID = "marginCallEntryId";
  private static final String ATTR_MC_CONFIG_ID = "marginCallConfigId";
  private static final String ATTR_CONTRACT_ID = "MissingIsinContractID";

  /* (non-Javadoc)
   * @see com.calypso.tk.bo.workflow.WfMessageRule#check(com.calypso.tk.bo.TaskWorkflowConfig, com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade, com.calypso.tk.bo.BOTransfer, java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector, com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean check(
      TaskWorkflowConfig wc,
      BOMessage message,
      BOMessage oldMessage,
      Trade trade,
      BOTransfer transfer,
      Vector messages,
      DSConnection dsCon,
      Vector excps,
      Task task,
      Object dbCon,
      Vector events) {
    return true;
  }

  /* (non-Javadoc)
   * @see com.calypso.tk.bo.workflow.WfMessageRule#getDescription()
   */
  @Override
  public String getDescription() {
    return "Fill the CommentId field of the MT569 Message with the EntryId value";
  }

  /* (non-Javadoc)
   * @see com.calypso.tk.bo.workflow.WfMessageRule#update(com.calypso.tk.bo.TaskWorkflowConfig, com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade, com.calypso.tk.bo.BOTransfer, java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector, com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean update(
      TaskWorkflowConfig wc,
      BOMessage message,
      BOMessage oldMessage,
      Trade trade,
      BOTransfer transfer,
      Vector messages,
      DSConnection dsCon,
      Vector excps,
      Task task,
      Object dbCon,
      Vector events) {

    List<Integer> ids = new ArrayList<Integer>();
    ids.add(getEntryId(message));
    List<MarginCallEntryDTO> entries = null;
    JDate processDate = message.getCreationDate().getJDate(TimeZone.getDefault());
    int entryId = 0;

    try {
      int defaultContextId = ServiceRegistry.getDefaultContext().getId();
      entries =
          ServiceRegistry.getDefault()
              .getCollateralServer()
              .loadEntries(ids, processDate, defaultContextId);
      if (!Util.isEmpty(entries)) {
        entryId = entries.get(0).getId();
        message.setAttribute(ATTR_COMMENT_ID, String.valueOf(entryId));
        message.setAttribute(ATTR_MC_ENTRY_ID, String.valueOf(entryId));
        message.setAttribute(ATTR_MC_CONFIG_ID, String.valueOf(ids.get(0)));
      } else {
        Log.error(this, "Cannot find any marginCallDTO with contract id: " + ids.get(0));
      }
    } catch (RemoteException e) {
      Log.error(this, "Error with entry: " + ids.get(0) + "Error: " + e);
    }

    return true;
  }

  /**
   * Get the contract Id from a specific attribute of the message
   *
   * @param message
   * @return entryId
   */
  private int getEntryId(BOMessage message) {
    Integer entryId = 0;
    try {
      entryId = Integer.parseInt(message.getAttribute(ATTR_CONTRACT_ID));
    } catch (NumberFormatException e) {
      Log.error(this, "Cannot parse marginCallEntryId from message: " + message.getLongId(), e);
    }
    return entryId;
  }
}

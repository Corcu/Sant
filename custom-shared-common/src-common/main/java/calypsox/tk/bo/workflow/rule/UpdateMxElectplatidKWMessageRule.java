package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;


import java.sql.Connection;
import java.util.Arrays;
import java.util.Vector;

/**
 * UpdateMxElectplatidKWMessageRule update Mx Electplatid KW with message id if empty
 *
 * @author Ruben Garcia
 */
public class UpdateMxElectplatidKWMessageRule implements WfMessageRule {

    private static final String MX_ELECTPLATID = "Mx Electplatid";

    public static final String  MASTER_REFERENCE = "MasterReference";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Updates the KW Mx Electplatid trade if the trade does not have it informed, with the message field :20C::SEME";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
       if (!Action.CANCEL.equals(message.getSubAction()) && !Action.COPY.equals(message.getSubAction())) {
           Trade tradeToUpdate = null;
           try {
               if (trade == null) {
                   //this is not really necessary as Calypso will retrieve the trade automatically
                   if (message.getTradeLongId() > 0) {
                       tradeToUpdate = dbCon != null ? TradeSQL.getTrade(message.getTradeLongId(), (Connection) dbCon) : dsCon.getRemoteTrade().getTrade(message.getTradeLongId()).clone();
                   }
               } else {
                   tradeToUpdate = trade.clone();
               }

               if (tradeToUpdate == null) {
                   if (message.getTradeLongId() > 0)
                       messages.add(String.format("Error getting trade %d.", message.getTradeLongId()));
                   else
                       messages.add(String.format("Trade not found for update. Message: %s", message));
                   return false;
               }
               String kwVal = tradeToUpdate.getKeywordValue(MX_ELECTPLATID);
               //check if amend msg
              if (!Util.isEmpty(kwVal)) {
                  MessageArray tradeMessages = dsCon.getRemoteBO().getMessages(message.getTradeLongId());
                  boolean isAmend = false;
                  if (tradeMessages != null && !tradeMessages.isEmpty()) {
                      isAmend = Arrays.stream(tradeMessages.getMessages()).anyMatch(m -> m != null
                              && !Status.isCanceled(m.getStatus())
                              && message.getMessageType().equals(m.getMessageType())
                              && m.getLongId() != message.getLongId()
                              && message.getTemplateName().equals(m.getTemplateName())
                              && !Action.CANCEL.equals(m.getSubAction())
                              && !Action.COPY.equals(m.getSubAction())
                              && m.getCreationDate().before(message.getCreationDate())
                      );
                  }
                  if (!isAmend) {
                      message.setAttribute(MASTER_REFERENCE, kwVal);
                      return true;
                  }
              }

               long existing;
               try {
                   existing = Util.isEmpty(kwVal) ? -1 : Long.parseLong(kwVal);
               } catch (NumberFormatException ignore) {
                   existing = -1;
               }

               long messageId = message.getLongId() > 0 ? message.getLongId() : message.getAllocatedLongSeed();
               if (existing != messageId) {
                   String ref =  Long.toString(messageId);
                   tradeToUpdate.setAction(Action.AMEND);
                   tradeToUpdate.addKeyword(MX_ELECTPLATID, ref);

                   if (!TradeWorkflow.isTradeActionApplicable(tradeToUpdate, Action.AMEND, dsCon, dbCon)) {
                       messages.add(String.format("Action AMEND is not applicable to trade %s, unable to update KW %s with value %s.", tradeToUpdate, MX_ELECTPLATID, message.getLongId()));
                       return false;
                   }

                   try {

                       if (dbCon != null)
                           TradeSQL.save(tradeToUpdate, (Connection) dbCon);
                       else
                           dsCon.getRemoteTrade().save(tradeToUpdate);

                       message.setAttribute(MASTER_REFERENCE, ref);

                   } catch (CalypsoServiceException e) {
                       Log.error(this, e);
                       messages.add("Error saving trade " + tradeToUpdate.getLongId() + " in Data Server. Failed to update the KW " + MX_ELECTPLATID + " with value " + message.getLongId());
                       return false;
                   }
               }
           } catch (Exception e) {
               messages.add(String.format("Error updating KW %s, message %s. %s: %s ", MX_ELECTPLATID, message, e.getClass().getSimpleName(), e.getLocalizedMessage()));
               return false;
           }
       }

        return true;
    }
}

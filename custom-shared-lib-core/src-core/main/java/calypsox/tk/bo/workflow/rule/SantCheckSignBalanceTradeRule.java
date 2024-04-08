package calypsox.tk.bo.workflow.rule;

import calypsox.util.KondorPlusUtilities;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * Check if the sign of the balance when a new Margin Call Trade is created has changed or not.
 * If the result is positive, we don't save the Margin Call Trade, avoiding to create 2 messages for Kondor+ in the same transaction.
 * If the result is negative, the Margin Call is created properly, as habitual.
 *
 * @author Jos? David Sevillano Carretero (josedavid.sevillano@siag.es).
 */
public class SantCheckSignBalanceTradeRule implements WfTradeRule {
    private static final String TRUE = "TRUE";
    private static final String IS_MIGRATION = "IS_MIGRATION";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
                         Vector messages, DSConnection dsCon, Vector exception, Task task,
                         Object dbCon, Vector events) {

        return true;
    }

    @Override
    public String getDescription() {
        String desc = "Check if the sign of the balance when a new Margin Call Trade is created has changed or not.\n" +
                "If the result is positive, we don't save the Margin Call Trade, avoiding to create 2 messages for Kondor+ in the same transaction.\n" +
                "If the result is negative, the Margin Call is created properly, as habitual.";
        return desc;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
                          Vector messages, DSConnection ds, Vector exception, Task task,
                          Object dbCon, Vector events) {

        Log.debug("SantCheckSignBalanceTradeRule", "Update - Start");

        Log.debug("SantCheckSignBalanceTradeRule", "Trade id = " + trade.getLongId());

        //We check the keyword before look at the sign of the balance.
        if (!TRUE.equals(trade.getKeywordValue(IS_MIGRATION))) {
            //We call the static method specified in the KondorPlusUtilities class.
            boolean hasChangedSign = KondorPlusUtilities.hasChangedSignBalance(trade, ds, false);
            Log.debug("SantCheckSignBalanceTradeRule", "Update - End");
            //If the result is TRUE, means that the sign has changed, so we return FALSE. Thus, we don't save any information about the new Margin
            //Call Trade in the system.
            if (hasChangedSign) {
                task.setComment("The sign of the balance will change with this amount for the Margin Call Trade. " +
                        "You need to modify it or you can cancel it and rebook the Trade.");
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
}
package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author acd
 */
public class UpdateTradeSettleDateTradeRule implements WfTradeRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Update Settle date to next business day ";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        try {
            Vector<String> holidaysCollection = DSConnection.getDefault().getRemoteReferenceData().getCurrencyDefault(trade.getTradeCurrency()).getDefaultHolidays();
            JDate newSettleDate = JDate.getNow().addBusinessDays(1, holidaysCollection);
            trade.setSettleDate(newSettleDate);

        } catch (CalypsoServiceException e) {
            Log.error(this, "Error updating settle date: " + e);
        }
        return true;
    }
}
package calypsox.tk.bo.workflow.rule;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SLMigrationDateTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Add keyword SL_MIG.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        Optional.ofNullable(trade).ifPresent(trade1 -> {
            JDateFormat format = new JDateFormat("dd/MM/yyyy");
            trade.addKeyword("SL_MIG", format.format(JDate.getNow()));
        });

        return true;
    }
}

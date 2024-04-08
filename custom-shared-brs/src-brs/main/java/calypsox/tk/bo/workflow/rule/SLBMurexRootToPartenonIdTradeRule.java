package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SLBMurexRootToPartenonIdTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return Optional.ofNullable(trade).isPresent() && !Util.isEmpty(trade.getKeywordValue("MurexRootContract"));
    }

    @Override
    public String getDescription() {
        return "Copy MurexRootContract to PartenonAccountingID adding SLB.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String murexRootContract = trade.getKeywordValue("MurexRootContract");
        trade.addKeyword("PartenonAccountingID","SLB"+murexRootContract);
        return true;
    }
}

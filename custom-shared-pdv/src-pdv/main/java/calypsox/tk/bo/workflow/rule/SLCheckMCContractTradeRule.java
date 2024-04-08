package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class SLCheckMCContractTradeRule implements WfTradeRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector excps, Task task, Object dbCon, Vector events) {
        if (trade == null) {
            return true;
        }

        int contractId = trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
        if (contractId > 0) {
            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {

        return "Check if the Trade has an assigned Margin Call Contract";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                          Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}

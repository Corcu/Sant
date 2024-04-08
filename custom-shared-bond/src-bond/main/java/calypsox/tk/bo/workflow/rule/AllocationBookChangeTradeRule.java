package calypsox.tk.bo.workflow.rule;

import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.AuditFilter;
import com.calypso.tk.refdata.AuditFilterUtil;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;
import java.util.stream.Collectors;

/**
 * @author x983373
 */

public class AllocationBookChangeTradeRule implements WfTradeRule, PlatformAllocationTradeFilterAdapter {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (!isChildTrade(trade)) {
            return false;
        }
        AuditFilter indiferentChanges = AuditFilterUtil.findByName("Bond Allocation Book Change");
        AuditFilter hasBookChange = AuditFilterUtil.findByName("Trade Book Change");

        return !indiferentChanges.accept(AuditFilter.OP_NOT_ALL_IN, oldTrade, trade) &&
                hasBookChange.accept(AuditFilter.OP_IN, oldTrade, trade);
    }

    @Override
    public String getDescription() {
        return "Check if the change to allocation trade has only book change";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}

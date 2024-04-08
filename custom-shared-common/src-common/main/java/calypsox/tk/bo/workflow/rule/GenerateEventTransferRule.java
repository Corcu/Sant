package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventProcessTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;

/**
 * This rule was done to allow accounting event generation when STP transitions. If a net exists,
 * the first underlying xfer will be published within the event. Current accounting rules need
 * the underlying xfer instead of the netted one.
 */
public class GenerateEventTransferRule implements WfTransferRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "This rule will generate an intermediate Workflow event (previous status) for Workflow configs in STP. " +
                "If netted an underlying xfer's event is published";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
            BOTransfer evTransfer=transfer;
            String[] engineNames = this.getEngineNames(wc, dsCon);
            if(!"None".equalsIgnoreCase(transfer.getNettingType())&& transfer.getNettedTransferLongId()<=0L) {
                evTransfer = new FIFlowTransferNetHandler(transfer).getFirstUndTransfer();
            }
            final PSEventProcessTransfer xferEvent = new PSEventProcessTransfer(evTransfer, trade,
                        null);
            xferEvent.setStatus(wc.getStatus());
            xferEvent.setEngineNames(engineNames);
            events.addElement(xferEvent);
            return true;
    }

    private String[] getEngineNames(TaskWorkflowConfig wc, DSConnection ds) {
        List<String> engines = new ArrayList();
        if (!Util.isEmpty(wc.getComment())) {
            List<String> domains = LocalCache.getDomainValues(ds, "GenerateEventEngineNames");
            Iterator var5 = Util.string2Vector(wc.getComment()).iterator();

            while(var5.hasNext()) {
                String s = (String)var5.next();
                if (domains.contains(s)) {
                    ((List)engines).add(s);
                }
            }
        }

        if (Util.isEmpty((Collection)engines)) {
            engines = Arrays.asList("AccountingEngine", "CreEngine");
        }

        return (String[])((List)engines).toArray(new String[0]);
    }
}

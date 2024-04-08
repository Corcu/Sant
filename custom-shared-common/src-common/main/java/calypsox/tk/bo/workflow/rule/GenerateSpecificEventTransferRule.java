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
import com.calypso.tk.util.TransferArray;

import java.util.*;

public class GenerateSpecificEventTransferRule implements WfTransferRule {

    public static String ruleName = "GenerateSpecificEvent";
    public static String all = "all";
    public static String net = "net";
    public static String unnet = "unnet";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Rule param = not defined: generate an intermediate WF event for netted and all unneted transfers." +
                "Rule param = 'net': generate an intermediate WF event only for netted transfers." +
                "Rule param = 'unnet': generate an intermediate WF event only for all the unneted transfers.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String ruleParam = wc.getRuleParam(ruleName);
        String[] engineNames = this.getEngineNames(wc, dsCon);

        if(!"None".equalsIgnoreCase(transfer.getNettingType()) && transfer.getNettedTransferLongId()<=0L) {
            if(!Util.isEmpty(ruleParam)) {
                if (ruleParam.equalsIgnoreCase(net)){
                    //rule param = net: solo evento de la madre
                    addTransferEvent(transfer, trade, wc, events,engineNames);
                } else if (ruleParam.equalsIgnoreCase(unnet)){
                    //rule param = unnet: solo eventos de las hijas
                    unnetedTransferEvents(transfer, trade, wc, events,engineNames);
                }
            } else {
                //rule param empty, evento de la madre + eventos de las hijas
                addTransferEvent(transfer, trade, wc, events,engineNames);
                unnetedTransferEvents(transfer, trade, wc, events,engineNames);
            }
        } else {
            //sino es un neto, evento de la transfer:
            addTransferEvent(transfer, trade, wc, events,engineNames);
        }

        return true;
    }

    public void unnetedTransferEvents(BOTransfer transfer, Trade trade, TaskWorkflowConfig wc, Vector events,String[] engineNames) {
        TransferArray transferUnneted = new FIFlowTransferNetHandler(transfer).getTransferUnderlyings();
        if (transferUnneted != null) {
            BOTransfer[] transfersUnneted = transferUnneted.getTransfers();
            for (BOTransfer xfer : transfersUnneted) {
                //eventos para las hijas
                addTransferEvent(xfer, trade, wc, events,engineNames);
            }
        }
    }

    public void addTransferEvent(BOTransfer transfer, Trade trade, TaskWorkflowConfig wc, Vector events,String[] engineNames){
        PSEventProcessTransfer xferProcessEvent = new PSEventProcessTransfer(transfer, trade, null);
        xferProcessEvent.setStatus(wc.getStatus());
        xferProcessEvent.setEngineNames(engineNames);
        events.addElement(xferProcessEvent);
    }

    private String[] getEngineNames(TaskWorkflowConfig wc, DSConnection ds) {
        List<String> engines = new ArrayList();
        if (!Util.isEmpty(wc.getComment())) {
            List<String> domains = LocalCache.getDomainValues(ds, "GenerateSpecificEventEngineNames");
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

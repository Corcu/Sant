package calypsox.tk.bo.workflow.rule;

import com.calypso.taskenrichment.taskstation.TaskStationTabManager;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.sql.BOMessageSQL;
import com.calypso.tk.bo.util.ProcessTaskUtil;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * @author x983373
 */

public class ReprocessAllocatedTradesTradeRule implements WfTradeRule {


    private final static Action REPROCESS = Action.valueOf("REPROCESS");
    @Override
    public String getDescription() {
        return "Reprocess allocated trade task";
    }

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        TaskArray tradeTasks = getAllocatedTasks(dsCon);

        if((tradeTasks!=null) && (tradeTasks.size() > 0)){
            return reprocessAllocatedTrades(tradeTasks, trade, dsCon);
        }
        return true;
    }

    private TaskArray getAllocatedTasks(DSConnection ds){
        TaskArray tasks = new TaskArray();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("event_class = 'PSEventMessage'");
        whereClause.append(" AND ");
        whereClause.append("event_type = 'PENDING_TRADE_GATEWAYMSG'");

        try{
            tasks = ds.getRemoteBO().getTasks(whereClause.toString(), null);
        }catch (RemoteException e){
            Log.error(this, String.format("Error retrieving tasks from DB"), e);
        }

        return tasks;
    }

    private boolean reprocessAllocatedTrades(TaskArray tasks, Trade trade, DSConnection ds){
        TaskArray finalTasks = new TaskArray();

        for (Task t : tasks) {
            try {
                ProcessTaskUtil.ObjectDesc objectDesc = new ProcessTaskUtil.ObjectDesc(t);
                BOMessage message = objectDesc.getMessage();

                if (message == null){
                    message = getMessage(objectDesc.id);
                }

                if (trade.getExternalReference().equals(message.getAttribute("UploadObjectExternalRef"))){
                    finalTasks.add(t);
                }
            }catch (Exception e){
                Log.error(this, e);
            }
        }

        try {
            ProcessTaskUtil.process(finalTasks.getTasks(), true, true, null);
        } catch (Exception e) {
            Log.error(this, e);
        }

        return true;
    }

    private BOMessage getMessage(long messageLongId) throws Exception {
        if (messageLongId > 0L) {
            return TaskStationTabManager.isDataServer() ? BOMessageSQL.getMessage(messageLongId) : DSConnection.getDefault().getRemoteBO().getMessage(messageLongId);
        } else {
            return null;
        }
    }
}

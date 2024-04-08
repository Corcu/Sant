package calypsox.tk.report.carteras;

import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CarterasTaskPersistor {

    final String carterasEventType = "EXPORTED_CARTERAS_TRADE";

    boolean isTaskPersistenceEnabled;


    public CarterasTaskPersistor(boolean isTaskPersistenceEnabled) {
        this.isTaskPersistenceEnabled=isTaskPersistenceEnabled;
    }

    public Task getLastCarterasTask(long tradeId){
        TaskArray carterasTasks=getCarterasTasks(tradeId);
        return Optional.ofNullable(carterasTasks).map(TaskArray::getTasks)
                    .filter(tasks -> tasks.length > 0)
                    .map(tasks -> tasks[0]).orElse(null);
    }
    private TaskArray getCarterasTasks(long tradeId) {
        TaskArray currentTasks = null;
        try {
            currentTasks = DSConnection.getDefault().getRemoteBO().getTaskArray("", "TRADE_ID= ? AND EVENT_CLASS='PSEventTrade' AND EVENT_TYPE='EXPORTED_CARTERAS_TRADE' ORDER BY TASK_DATETIME DESC", CustomBindVariablesUtil.createNewBindVariable(tradeId));
        } catch (CalypsoServiceException exc) {
            Log.error(CarterasTaskPersistor.class.getSimpleName(), exc.getCause());
        }
       return currentTasks;
    }

    public void publishTaskIfNotExists(final Trade trade,JDatetime exportedTime) {
        if (isTaskPersistenceEnabled) {
            try {
                DSConnection.getDefault().getRemoteBO().save(createNewTask(trade,exportedTime));
            } catch (RemoteException exc) {
                Log.error(Log.WFW, exc);
            }
        }

    }

    private Task createNewTask(Trade trade, JDatetime exportedTime) {
        TradePartenonBuilder partenonBuilder=new TradePartenonBuilder(trade);
        Task t = new Task(trade);
        t.setStatus(Task.COMPLETED);
        t.setEventType(carterasEventType);
        t.setEventClass(Task.TRADE_EVENT_CLASS);
        t.setPriority(Task.PRIORITY_LOW);
        t.setBookId(trade.getBookId());
        t.setComment(partenonBuilder.buildFullPartenon());
        t.setOwner(DSConnection.getDefault().getUser());
        t.setTaskWorkflowConfigId(0);
        t.setDatetime(exportedTime);
        return t;
    }
}

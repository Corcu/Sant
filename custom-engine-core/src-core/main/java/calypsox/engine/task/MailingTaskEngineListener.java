package calypsox.engine.task;

import calypsox.tk.event.PSEventMailingAlert;
import com.calypso.engine.task.TaskEngineListener;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEventTask;
import com.calypso.tk.event.PSEventTime;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;

public class MailingTaskEngineListener implements TaskEngineListener {

    private static final String LOGCAT = "MailingTaskEngineListener";

    private final List<StaticDataFilter> filtersToAccept;

    public MailingTaskEngineListener() {
        filtersToAccept = new ArrayList<>();
        filtersToAccept.add(StaticDataFilter.valueOf("REPO_MISSINGSDI"));
        filtersToAccept.add(StaticDataFilter.valueOf("BONDALLOCATION_MISSINGCPTY"));
    }

    public void newEvent(BOException event) {
        Log.info(LOGCAT, "New BOException: " + event);
    }

    @Override
    public void newEvent(PSEventTime event) {
        Log.info(LOGCAT, "New PSEventTime: " + event);
    }


    @Override
    public void newEvent(PSEventTask event) {
        Task task = event.getTask();
        for (StaticDataFilter sdf : filtersToAccept) {
            if (sdf != null && sdf.accept(null, task) && isFirstSent(task)) {
                publishPSEventMailingEvent(task);
                break;
            }
        }

    }

    @Override
    public void newTask(Task task) {
        Log.info(LOGCAT, "New Task: " + task);
    }

    @Override
    public void onDisconnect() {
        Log.info(LOGCAT, LOGCAT + " disconnected from eventserver");
    }

    @Override
    public void onDSDisconnect() {
        Log.info(LOGCAT, LOGCAT + " disconnected from dataserver");
    }

    @Override
    public void onPSConnect() {
        Log.info(LOGCAT, LOGCAT + " connected to eventserver");
    }

    @Override
    public void onDSConnect() {
        Log.info(LOGCAT, LOGCAT + " connected to dataserver");
    }

    private void publishPSEventMailingEvent(Task task) {
        try {
            PSEventMailingAlert mailingAlertEvent = new PSEventMailingAlert();
            mailingAlertEvent.setTask(task);
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(mailingAlertEvent);
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
    }

    private boolean isFirstSent(Task task){
        Trade trade = null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(task.getTradeLongId());
            if(trade == null){
                return true;
            }
            String keyword = trade.getKeywordValue("MailSDISent");
            return keyword == null || keyword.isEmpty();
        } catch (CalypsoServiceException e) {
            Log.error(LOGCAT, "Could not load trade " + task.getTradeLongId());
        }
        return false;
    }

}

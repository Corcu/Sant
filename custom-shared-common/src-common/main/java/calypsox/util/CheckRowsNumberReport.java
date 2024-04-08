package calypsox.util;

import calypsox.tk.util.ScheduledTaskCSVREPORT;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEventTask;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.scheduling.service.RemoteSchedulingService;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;


public interface CheckRowsNumberReport {

    default void checkAndGenerateTaskReport(DefaultReportOutput output, HashMap<String ,String> externalRef) {
        if (null != output.getRows() && !externalRef.isEmpty()) {
            checkAndGenerateTaskReport(externalRef, output.getRows().length);
        }
    }

    default void checkAndGenerateTaskReport(HashMap externalRef, int rows) {
        ScheduledTask st = null;

        try {
            RemoteSchedulingService schedulingService = DSConnection.getDefault().getService(RemoteSchedulingService.class);
            st = schedulingService.getScheduledTaskByExternalReference((String) externalRef.values().iterator().next());
        }catch (CalypsoServiceException e){
            String message = String.format("Could not retrieve Scheduled Task with external reference \"%s\"", externalRef);
            Log.error(this, message, e);
        }
        String fileName = "";
		if (st != null && "CSVREPORT".equals(st.getType())) {
			fileName = getFileFormated(st.getAttribute(ScheduledTaskCSVREPORT.REPORT_FILE_NAME),
					st.getAttribute(ScheduledTaskCSVREPORT.TIMESTAMP_FORMAT));
		}
        String umbralSize = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                "ReportingControl", (String) externalRef.values().iterator().next());

        if (!Util.isEmpty(umbralSize)) {
            String comment = "The number of rows in " + fileName + " is: " +
                    rows + " and its threshold values are: [" + umbralSize + "], please check the file";
            String[] values = umbralSize.split("-");
            if (values.length == 2) {
                int minRows = Integer.parseInt(values[0]);
                int maxRows = Integer.parseInt(values[1]);
                if (rows < minRows || rows > maxRows) {
                    Task task = new Task();
                    task.setStatus(Task.NEW);
                    task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    task.setEventType("EX_REPORT_SIZE_CONTROL");
                    task.setPriority(Task.PRIORITY_NORMAL);
                    task.setObjectClassName((String) externalRef.keySet().iterator().next());
                    task.setComment(comment);
                    task.setSource(externalRef.keySet() + (String) externalRef.values().iterator().next());
                    task.setDatetime(JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault()));
                    PSEventTask psEventTask = new PSEventTask();
                    psEventTask.setTask(task);
                    TaskArray v = new TaskArray();
                    v.add(task);
                    try {
                        DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(v, 0, null);
                    } catch (CalypsoServiceException e) {
                        Log.error("Can't save the task: " + task.getEventType(), e);
                    }
                }
            }
        }
    }

    default String getFileFormated(String filePath, String timestampFormat){
        String[] splitedPath = filePath.split("/");
        String file = splitedPath[splitedPath.length-1];

        SimpleDateFormat simpleDateFormat;
        if (timestampFormat!=null && !Util.isEmpty(timestampFormat)){
            simpleDateFormat = new SimpleDateFormat(timestampFormat);
        }else {
            simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        }

        String date = simpleDateFormat.format(JDate.getNow().getDate());
        return file.concat(date);
    }
}

package calypsox.apps.startup;

import com.calypso.apps.startup.AppStarter;
import com.calypso.apps.util.CalypsoLoginDialog;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.scheduling.service.RemoteSchedulingService;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;

import java.util.List;

/**
 * @author aalonsop
 */
public class StartScheduledTaskResaver extends AppStarter {

    private static final String JVM_OPTIONS = "JVM_SETTINGS";

    public static void startNOGUI(String[] args) {
        String envName = getOption(args, "-env");// 33
        String user = getOption(args, "-user");// 34
        String passwd = getOption(args, "-password");// 35
        new StartScheduledTaskResaver().onConnect(null, user, passwd, envName);
    }

    public static void main(String[] args) {
        startLog(args, StartScheduledTaskResaver.class.getSimpleName());// 21
        startNOGUI(args);
    }

    public void onConnect(CalypsoLoginDialog dialog, String user, String passwd, String envName) {
        try {
            Log.system(this.getClass().getSimpleName(), "Hi Fran!");
            ConnectionUtil.connect(user, passwd, "Navigator", envName);
            List<ScheduledTask> allTasks = DSConnection.getDefault().getService(RemoteSchedulingService.class).getAllTasks();
            Log.system(this.getClass().getSimpleName(), "ScheduledTasks size:" + allTasks.size());
            for (ScheduledTask task : allTasks) {
                String vmOptions = task.getAttribute(JVM_OPTIONS);
                //Set whole vmOptions
                task.setAttribute(JVM_OPTIONS, "-Xms512m -Xmx1024m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=1024m");

                try {
                    Log.system(this.getClass().getSimpleName(), "ST " + task.getId() + " setted VMOptions: " + vmOptions);
                    DSConnection.getDefault().getService(RemoteSchedulingService.class).save(task);
                    Log.system(this.getClass().getSimpleName(), "ST " + task.getId() + " successfuly saved");
                } catch (CalypsoServiceException exc) {
                    Log.error(this.getClass().getSimpleName(), "Exception while saving ST:" + task.getId(), exc.getCause());
                }
            }
        } catch (CalypsoServiceException | ConnectException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getMessage(), exc.getCause());
        } finally {
            ConnectionUtil.shutdown();
        }
    }
}

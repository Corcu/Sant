package calypsox.tk.util;

import java.text.SimpleDateFormat;

import com.calypso.apps.startup.AppStarter;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.scheduling.service.RemoteSchedulingService;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;

// Project: Bloomberg tagging

public class ScheduledTaskTester {

    private static final String APP_NAME = "ScheduledTaskRunner";

    private static final String EXTERNAL_REF_OPTION = "-externalRef";
    private static final String VAL_DATE_OPTION = "-valDate";
    private static final String VAL_DATE_FORMAT = "yyyy.MM.dd";

    public static void main(String[] args) {
        ScheduledTaskTester app = new ScheduledTaskTester();
        app.run(args);
    }

    private void run(String[] args) {
        DSConnection ds = null;
        try {
            ds = ConnectionUtil.connect(args, APP_NAME);

            Log.setInfo("ALL");

            RemoteSchedulingService schedulingService = ds
                    .getService(RemoteSchedulingService.class);

            final String externalRef = AppStarter.getOption(args,
                    EXTERNAL_REF_OPTION);
            final String valDateStr = AppStarter.getOption(args,
                    VAL_DATE_OPTION);

            ScheduledTask st = schedulingService
                    .getScheduledTaskByExternalReference(externalRef);

            if (st != null) {
                if (!Util.isEmpty(valDateStr)) {
                    JDate valDate = JDate
                            .valueOf(new SimpleDateFormat(VAL_DATE_FORMAT)
                                    .parse(valDateStr));
                    st.setCurrentDate(valDate);
                }
                st.execute(ds, null);
            } else {
                Log.info(this,
                        String.format(
                                "Cannot find Scheduled Task with external reference \"%s\"",
                                externalRef));
            }

        } catch (ConnectException e) {
        	Log.error(this, e); //sonar
        } catch (CalypsoServiceException e) {
        	Log.error(this, e); //sonar
        } catch (Throwable t) {
        	Log.error(this, t); //sonar
        } finally {
            ConnectionUtil.disconnect();
        }
    }
}

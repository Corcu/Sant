package calypsox.engine.im.export;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author aalonsop
 */
public class QEFCorrelationIdGenerator {

    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final String IM_SENDING = "IM_SENDING_";

    /**
     * Get the next correlation id for the day
     *
     * @return correlation id
     */
    protected static String generateNewCorrelationId() {
        JDatetime today = new JDatetime();
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        StringBuilder correlationId = new StringBuilder(sdf.format(today));

        try {
            int id = DSConnection.getDefault().getRemoteAccess().allocateSeed(IM_SENDING + correlationId.toString(), 1);
            correlationId.append("_").append(id);
        } catch (RemoteException e) {
            Log.error(QEFCorrelationIdGenerator.class.getSimpleName(), "Could't get next seed: " + e.getMessage());
        }

        return correlationId.toString();
    }
}

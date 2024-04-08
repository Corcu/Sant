package calypsox.tk.util;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import org.jfree.util.Log;

import java.rmi.RemoteException;
import java.util.Vector;

public class ScheduledTaskSANT_FIX_FEE_MARK_DUPLICATED extends ScheduledTask {
    @Override
    public String getTaskInformation() {
        return "The wrong Fee Marks are removed to fix the trades affected";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        deleteEventTypeActions();
        return true;
    }

    private void deleteEventTypeActions() {
        Vector actions = getEventActionsId();
        if (null != actions) {
            try {
                for (int i = 2; i < actions.size(); i++) {
                    DSConnection.getDefault().getRemoteService(RemoteSantReportingService.class).executeUpdateSQL("DELETE FROM EVENT_TYPE_ACTION a WHERE a.ACTION_ID= " + ((Vector) actions.get(i)).get(0));
                }
            } catch (RemoteException e) {
                Log.error(this.getClass().getSimpleName(), e);
            }
        }
    }

    private Vector getEventActionsId() {
        Vector vector = null;
        try {
            vector = DSConnection.getDefault().getRemoteService(RemoteSantReportingService.class).executeSelectSQL("SELECT  a.ACTION_ID " +
                    "FROM EVENT_TYPE_ACTION a " +
                    "INNER JOIN PRODUCT_SECLENDING b ON a.product_id = b.product_id " +
                    "INNER JOIN TRADE t ON t.product_id = b.product_id " +
                    "WHERE SEQ_NO > 0 AND ACTION_TYPE = 'Fee Mark' AND a.CANCELLED_B = 0 AND t.TRADE_STATUS NOT IN ('CANCELED') " +
                    "AND ( SELECT COUNT(action_id) " +
                    "FROM EVENT_TYPE_ACTION  " +
                    "WHERE SEQ_NO = -1 AND ACTION_TYPE = 'Fee Mark' AND (AMOUNT < -0.009 OR AMOUNT > 0.009) AND a.product_id=product_id AND a.trade_version=trade_version) > 0 " +
                    "AND ( " +
                    "SELECT  COUNT(action_id) " +
                    "FROM EVENT_TYPE_ACTION " +
                    "WHERE a.product_id=product_id AND ACTION_TYPE = 'Partial Return') = 0 ");
        } catch (RemoteException e) {
            Log.error(this.getClass().getSimpleName() + "Fail retrieving duplicate Fee Marks", e);
        }
        return vector;
    }
}

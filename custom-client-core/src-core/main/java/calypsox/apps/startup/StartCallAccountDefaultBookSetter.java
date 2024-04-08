package calypsox.apps.startup;

import com.calypso.apps.startup.AppStarter;
import com.calypso.apps.util.CalypsoLoginDialog;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class StartCallAccountDefaultBookSetter extends AppStarter {


    public static void startNOGUI(String[] args) {
        String envName = getOption(args, "-env");// 33
        String user = getOption(args, "-user");// 34
        String passwd = getOption(args, "-password");// 35
        new StartCallAccountDefaultBookSetter().onConnect(null, user, passwd, envName);
    }

    public static void main(String[] args) {
        startLog(args, StartMCContractDefaultBookEligibilityFilterSetter.class.getSimpleName());// 21
        startNOGUI(args);
    }

    public void onConnect(CalypsoLoginDialog dialog, String user, String passwd, String envName) {
        try {
            ConnectionUtil.connect(user, passwd, "Navigator", envName);
            Vector<Account> accs = DSConnection.getDefault().getRemoteAccounting().getAccounts(true);
            Vector<Account> accsToSave = new Vector<>();
            int i = 0;
            for (Account acc : accs) {
                if (acc.getCallAccountB()) {
                    String mcId = acc.getAccountProperty("MARGIN_CALL_CONTRACT");
                    if (!Util.isEmpty(mcId)) {
                        try {
                            CollateralConfig cc = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(Integer.valueOf(mcId));
                            acc.setCallBookId(cc.getDefaultBook("BOOK_CASH_IN").getId());
                            accsToSave.add(acc);
                            Log.info(this.getClass().getSimpleName(), "Acc: " + acc.getId() + " to be saved --> Counter= " + i++);
                        } catch (Exception exc) {
                            Log.info(this.getClass().getSimpleName(), "Error parseando " + mcId + " -> " + exc.getCause());
                        }
                    }
                }
            }
            Log.info(this.getClass().getSimpleName(), "Saving accs");
            DSConnection.getDefault().getRemoteAccounting().saveAccounts(accsToSave);
        } catch (ConnectException | CalypsoServiceException exc) {
            Log.error("Error setting default book to callAccounts", exc.getCause());
        }
        ConnectionUtil.shutdown();
    }
}

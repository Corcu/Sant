package calypsox.apps.startup;

import com.calypso.apps.startup.AppStarter;
import com.calypso.apps.util.CalypsoLoginDialog;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralEligibleBooksStaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import java.util.List;

/**
 * @author aalonsop
 */
public class StartMCContractExposureOnEndDaySetter extends AppStarter {

    private static final String END_DATE_EXP_STR = "INCLUDE_END_DATE_EXPOSURE";

    public static void startNOGUI(String[] args) {
        String envName = getOption(args, "-env");// 33
        String user = getOption(args, "-user");// 34
        String passwd = getOption(args, "-password");// 35
        new StartMCContractExposureOnEndDaySetter().onConnect(null, user, passwd, envName);
    }

    public static void main(String[] args) {
        startLog(args, StartMCContractExposureOnEndDaySetter.class.getSimpleName());// 21
        startNOGUI(args);
    }

    /**
     * @param dialog
     * @param user
     * @param passwd
     * @param envName
     */
    @Override
    public void onConnect(CalypsoLoginDialog dialog, String user, String passwd, String envName) {
        try {
            ConnectionUtil.connect(user, passwd, "Navigator", envName);
            List<CollateralConfig> ccs = ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig();
            int counter = 0;
            for (CollateralConfig cc : ccs) {
                cc.setAdditionalField(END_DATE_EXP_STR, "TRUE");
                try {
                    ServiceRegistry.getDefault().getCollateralDataServer().save(cc);
                } catch (CollateralServiceException e) {
                    Log.error(this.getClass().getSimpleName(), "Couldn't save MCContract " + cc.getId());
                }
                Log.info(this.getClass().getSimpleName(), counter++ + ": Contract Id " + cc.getId() + " saved; ");
            }
        } catch (ConnectException | CollateralServiceException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getCause());
        }
        ConnectionUtil.shutdown();
    }

    private CollateralEligibleBooksStaticDataFilter buildEligibilityFilter(CollateralConfig cc, StaticDataFilter sdf) {
        CollateralEligibleBooksStaticDataFilter eligible = new CollateralEligibleBooksStaticDataFilter();
        eligible.setBookOwnerType("Contract - PO books");
        eligible.setId(cc.getId());
        eligible.setStaticDataFilterName(sdf.getName());
        eligible.setType("Static Data Filter");
        return eligible;
    }
}

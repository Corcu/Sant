package calypsox.tk.util;

import calypsox.balances.ProcessBalances;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author epalaobe && jriquell
 */
public class SantImportBalanceUtil {

    private SantImportBalanceUtil() {
        //Empty
    }

    public static boolean isValidLE(String leCode) {
        try {
            if (DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(leCode) != null) {
                return true;
            }
        } catch (CalypsoServiceException e) {
            Log.error(SantImportBalanceUtil.class, e);//Sonar
        }
        return false;
    }

    public static boolean isValidContract(String contractName) {
        try {
            if (ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByCode(null, contractName) != null) {
                return true;
            }
        } catch (CollateralServiceException e) {
            Log.error(SantImportBalanceUtil.class, e);//Sonar
        }
        return false;
    }

    public static boolean isValidType(String type) {
        if ("Security".equalsIgnoreCase(type) || "Cash".equalsIgnoreCase(type)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isValidIsinForBond(String isin, String type) {
        if (type != null) {
            if (type.equalsIgnoreCase(ProcessBalances.TYPE_SECURYTY)) {
                try {
                    if (isin != null) {
                        String where = "product_desc.PRODUCT_ID = product_sec_code.PRODUCT_ID "
                                + "AND product_sec_code.SEC_CODE = 'ISIN' "
                                + "AND product_sec_code.CODE_VALUE_UCASE = '" + isin + "'";
                        Vector bonds = DSConnection.getDefault().getRemoteProduct().getProducts("Bond", "product_sec_code", where, true, null);
                        if (!Util.isEmpty(bonds)) {
                            return true;
                        }
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(SantImportBalanceUtil.class, e);//Sonar
                    return false;
                }

            } else if (type.equalsIgnoreCase(ProcessBalances.TYPE_CASH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}

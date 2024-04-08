package calypsox.ctm.util;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;

import java.rmi.RemoteException;
import java.util.Optional;

public class CTMChildTradeExtRefGenerator {

    public static String buildChildExtRef(Trade trade){
        return trade.getExternalReference() + "_" + getChildSeed();
    }

    public static String buildCTMChildExtRef(String blockTradeExtRef){
        return blockTradeExtRef + "_" + getChildSeed();
    }

    private static long getChildSeed(){
        long childSeed=0L;
        try {
            childSeed= CalypsoIDAPIUtil.allocateSeed(DSConnection.getDefault().getRemoteAccess(),
                    CTMUploaderConstants.CTM_ALLOCATION_SEED_NAME, 1);
        } catch (RemoteException exc) {
            Log.error(CTMChildTradeExtRefGenerator.class,exc.getCause());
        }
        return childSeed;
    }
}

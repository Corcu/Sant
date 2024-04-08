package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.List;

/**
 * PlatformUtil
 *
 * @author Ruben Garcia
 */
public class PlatformUtil {

    /**
     * The ClearingIncominCounterparties domain value name
     */
    public static final String CLEARING_CTPY_DV = "ClearingIncomingCounterparties";


    /**
     * Load the clearing counterparties IDs (LGWM,LV4V,5MSR,ECAG)
     *
     * @return the clearing counterparties IDs
     */
    public static List<Integer> loadClearingCounterParties() {
        List<Integer> ctpyIds = new ArrayList<>();
        List<String> ctpyCodes = LocalCache.getDomainValues(DSConnection.getDefault(),
                CLEARING_CTPY_DV);
        if (!Util.isEmpty(ctpyCodes)) {
            for (String code : ctpyCodes) {
                LegalEntity e = BOCache.getLegalEntity(DSConnection.getDefault(), code);
                if (e != null) {
                    ctpyIds.add(e.getId());
                }
            }
        }
        return ctpyIds;
    }
}

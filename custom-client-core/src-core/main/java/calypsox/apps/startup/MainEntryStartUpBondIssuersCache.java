/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.startup;

import com.calypso.apps.startup.MainEntryStartUp;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Vector;

public class MainEntryStartUpBondIssuersCache implements MainEntryStartUp {

    private static final String ACTIVATION_DV = "MainEntryStartUpBondIssuersCache.ACTIVATE";

    @Override
    public void onStartUp() {
        // launch the load of LegalEntities in a separate thread, to not block the MainEntry launch
        if (isAllowedToRun()) {
            new Thread(new LECacheLoader()).start();
        }
    }

    /**
     * @return True if the requiredDV is setted to "true".
     */
    private boolean isAllowedToRun() {
        Vector<String> dvs = LocalCache.getDomainValues(DSConnection.getDefault(), ACTIVATION_DV);
        return (!Util.isEmpty(dvs) && Boolean.valueOf(dvs.get(0)));
    }

    class LECacheLoader implements Runnable {

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                Collection<Integer> les = DSConnection.getDefault().getRemoteReferenceData()
                        .getLegalEntityIds("product_bond", "ISSUER_LE_ID=legal_entity_id");
                if ((les != null) && (les.size() > 0)) {
                    for (Integer leId : les) {
                        BOCache.getLegalEntity(DSConnection.getDefault(), leId);
                        BOCache.getLegalEntityAttributes(DSConnection.getDefault(), leId);
                    }
                }
                Log.info(this, "Time to load LE cache for bonds issuers " + (System.currentTimeMillis() - start));
            } catch (RemoteException e) {
                Log.error(this, e);
                // the cache of legalEntities will not be loaded
                return;
            }

        }

    }

}

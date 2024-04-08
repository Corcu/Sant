/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.startup;

import java.rmi.RemoteException;

import com.calypso.apps.startup.MainEntryStartUp;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;

public class MainEntryStartUpCollatConfigCache implements MainEntryStartUp {

	@Override
	public void onStartUp() {
		// launch the load of LegalEntities in a separate thread, to not block the MainEntry launch
		new Thread(new CollateralConfigCacheLoader()).start();
	}

	class CollateralConfigCacheLoader implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig();
				Log.info(this, "Time to load Collateral config cache " + (System.currentTimeMillis() - start));
			} catch (RemoteException e) {
				Log.error(this, e);
				// the cache of the collateral configs will not be loaded
				return;
			}

		}

	}

}

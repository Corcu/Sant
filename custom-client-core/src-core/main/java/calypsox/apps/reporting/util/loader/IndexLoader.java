/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util.loader;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;

public class IndexLoader extends SantLoader<RateIndex, String> {

    @SuppressWarnings("unchecked")
    public Map<RateIndex, String> load() {
	try {
	    final Vector<AccountInterestConfig> configs = DSConnection
		    .getDefault().getRemoteAccounting()
		    .getAccountInterestConfigs();

	    for (final AccountInterestConfig config : configs) {
		if (Util.isEmpty(config.getRanges())) {
		    continue;
		}
		for (final AccountInterestConfigRange range : (Vector<AccountInterestConfigRange>) config
			.getRanges()) {
		    if (range.isFixed()) {
			continue;
		    }

		    if (range.getRateIndex() != null) {
			if (!this.map.containsKey(range.getRateIndex())) {
			    this.map.put(range.getRateIndex(), range
				    .getRateIndex().toStringNoSource());
			}

		    }
		}
	    }
	} catch (final RemoteException e) {
	    Log.error(this, "Cannot load indexes", e);
	}

	return this.map;
    }
}

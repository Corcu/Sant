/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util.loader;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

public class AgreementLoader extends SantLoader<Integer, String> {

    public Map<Integer, String> load() {
	try {
	    // this.map.put(-1, "");
	    this.map.putAll(SantReportingUtil.getSantReportingService(
		    DSConnection.getDefault())
		    .getAllMarginCallConfigIdsAndNames());
	} catch (final RemoteException e) {
	    Log.error(this, "Cannot load margin call contracts", e);
	}
	final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(
		new ValueComparator(this.map));
	sortedMap.putAll(this.map);
	return sortedMap;
    }
}

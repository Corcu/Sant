/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util.loader;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.service.DSConnection;

public class LegalEntityLoader extends SantLoader<Integer, String> {

    public Map<Integer, String> load(final String role) {
	final Vector<LegalEntity> les = BOCache.getLegalEntitiesForRole(
		DSConnection.getDefault(), role);
	this.map.put(-1, "");
	for (final LegalEntity le : les) {
	    this.map.put(le.getId(), le.getCode());
	}
	final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(
		new ValueComparator(this.map));
	sortedMap.putAll(this.map);
	return sortedMap;
    }
}

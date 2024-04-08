/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.util.Vector;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

@SuppressWarnings("deprecation")
public class FrontOfficeView implements BOPositionFilter {

	private static final String INV_DUMMY_AGENT = "INV_DUMMY_AGENT";

	@Override
	public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {

		InventorySecurityPositionArray foPositions = new InventorySecurityPositionArray();

		for (int i = 0; i < positions.size(); i++) {
			InventorySecurityPosition pos = positions.get(i);
			if (isFrontOfficePosition(pos)) {
				foPositions.add(pos);
			}
		}
		return foPositions;
	}

	@Override
	public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
		return positions;
	}

	private boolean isFrontOfficePosition(InventorySecurityPosition pos) {
		Vector<String> dummyAgents = LocalCache.getDomainValues(DSConnection.getDefault(), INV_DUMMY_AGENT);

		if (!Util.isEmpty(dummyAgents) && (pos.getAgent() != null) && dummyAgents.contains(pos.getAgent().getCode())) {
			return true;
		}
		return false;
	}

}

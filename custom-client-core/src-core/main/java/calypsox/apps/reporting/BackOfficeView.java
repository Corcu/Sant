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
public class BackOfficeView implements BOPositionFilter {

	private static final String INV_DUMMY_BOOK = "INV_DUMMY_BOOK";

	@Override
	public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
		InventorySecurityPositionArray boPositions = new InventorySecurityPositionArray();

		for (int i = 0; i < positions.size(); i++) {
			InventorySecurityPosition pos = positions.get(i);
			if (isBackOfficePosition(pos)) {
				boPositions.add(pos);
			}
		}
		return boPositions;
	}

	@Override
	public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
		return positions;
	}

	private boolean isBackOfficePosition(InventorySecurityPosition pos) {
		Vector<String> dummyBooks = LocalCache.getDomainValues(DSConnection.getDefault(), INV_DUMMY_BOOK);
		if (!Util.isEmpty(dummyBooks) && (pos.getBook() != null) && dummyBooks.contains(pos.getBook().getName())) {
			return true;
		}
		return false;
	}

}

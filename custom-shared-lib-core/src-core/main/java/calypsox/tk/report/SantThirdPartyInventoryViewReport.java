/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;

public class SantThirdPartyInventoryViewReport extends SantInventoryViewReport {

	private static final long serialVersionUID = 1L;

	@Override
	public JDate getPositionStartDate() {
		return getStartDate();
	}

	@Override
	protected boolean filterPosition(Inventory position) {
		if (!(position instanceof InventorySecurityPosition)) {
			return false;
		}
		return super.filterPosition(position);
	}
}

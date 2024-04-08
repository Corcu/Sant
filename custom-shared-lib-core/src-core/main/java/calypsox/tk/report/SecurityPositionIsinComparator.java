/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.bo.InventorySecurityPosition;

@SuppressWarnings("rawtypes")
public class SecurityPositionIsinComparator implements java.util.Comparator {

	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null) {
			return -1;
		}
		if (obj2 == null) {
			return 1;
		}

		String t1, t2;
		if (obj1 instanceof InventorySecurityPosition) {
			t1 = ((InventorySecurityPosition) obj1).getProduct().getSecCode("ISIN");
		} else {
			return -1;
		}

		if (obj2 instanceof InventorySecurityPosition) {
			t2 = ((InventorySecurityPosition) obj2).getProduct().getSecCode("ISIN");
		} else {
			return 1;
		}
		return t1.compareTo(t2);
	}

	@Override
	public boolean equals(Object obj) {
		return false;
	}
}

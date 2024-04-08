/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator<Integer> {

    protected Map<Integer, String> base;

    public ValueComparator(final Map<Integer, String> base) {
	this.base = base;
    }

    @Override
    public int compare(final Integer key1, final Integer key2) {
	final String value1 = this.base.get(key1);
	if (value1 == null) {
	    return 0;
	}
	return value1.compareTo(this.base.get(key2));
    }
}
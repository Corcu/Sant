/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.loader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SantLoader<K, V> {

    protected final Map<K, V> map = new HashMap<K, V>();

    class ValueComparator implements Comparator<K> {

	private Map<K, V> base;//Sonar

	public ValueComparator(final Map<K, V> base) {
	    this.base = base;
	}

	@Override
	public int compare(final K key1, final K key2) {
	    final V value1 = this.base.get(key1);
	    if (value1 == null) {
		return 0;
	    }
	    final V value2 = this.base.get(key2);
	    if (value1.getClass().equals(String.class)) {
		return ((String) value1).compareTo(((String) value2));
	    }

	    return 0;
	}
    }

}

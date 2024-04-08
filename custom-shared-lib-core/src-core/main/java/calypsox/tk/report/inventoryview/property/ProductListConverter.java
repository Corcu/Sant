/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;
import java.util.List;

import com.calypso.tk.core.Util;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.converter.ObjectConverter;

public class ProductListConverter implements ObjectConverter {

	@SuppressWarnings("unchecked")
	@Override
	public String toString(Object o, ConverterContext converterContext) {
		if (o == null) {
			return null;
		}
		if (o instanceof String) {
			return (String) o;
		}
		List<Integer> securityIds = (List<Integer>) o;
		return Util.collectionToString(securityIds);
	}

	@Override
	public boolean supportToString(Object o, ConverterContext converterContext) {
		return true;
	}

	@Override
	public Object fromString(String s, ConverterContext converterContext) {
		return Util.string2IntVector(s);
	}

	@Override
	public boolean supportFromString(String s, ConverterContext converterContext) {
		return true;
	}
}

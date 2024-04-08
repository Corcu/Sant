/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.util.List;
import java.util.Map;

public interface SantanderPricesIEAdapter {

	abstract public boolean subscribe(List<String> ISINsList);

	abstract public boolean processPrice(Map<String, String> PricesMap);
}

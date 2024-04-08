/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import java.util.Vector;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * Event Filter - to filter out Trades defined in the domain 'SantExcludeTrades.ProductSubTypes'
 * 
 * 
 */
public class SantExcludeTradesSubTypeEventFilter implements EventFilter {

	public static final String DOMAIN_EXCLUDE_TRADE_SUB_TYPES = "SantExcludeTrades.ProductSubTypes";

	@Override
	public boolean accept(final PSEvent event) {
		Vector<String> tradeTypeVect = LocalCache
				.getDomainValues(DSConnection.getDefault(), DOMAIN_EXCLUDE_TRADE_SUB_TYPES);

		if (event instanceof PSEventTrade) {
			final PSEventTrade tradeEvent = (PSEventTrade) event;
			if (tradeTypeVect.contains(tradeEvent.getTrade().getProductSubType())) {
				return false;
			}
		}

		return true;
	}
}

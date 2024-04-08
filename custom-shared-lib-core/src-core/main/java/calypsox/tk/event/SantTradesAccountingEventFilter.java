/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.event.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * Event Filter - to filter out Trades defined in the domain 'SantExcludeTrades.ProductTypes'
 * 
 * 
 * @author Soma
 * 
 */
public class SantTradesAccountingEventFilter implements EventFilter {


	public static final String DOMAIN_INCLUDE_TRADE_TYPES = "SantTradesAccounting.ProductTypes";//PerformanceSwap

	@Override
	public boolean accept(final PSEvent event) {
		Vector<String> tradeTypeVect = LocalCache
				.getDomainValues(DSConnection.getDefault(), DOMAIN_INCLUDE_TRADE_TYPES);

		if (event instanceof PSEventTransfer) {
			PSEventTransfer et = (PSEventTransfer)event;
			if (!tradeTypeVect.contains(et.getBoTransfer().getProductType())) {
				return false;
			}
		}

		if (event instanceof PSEventTrade) {
			PSEventTrade et = (PSEventTrade)event;
			if (!tradeTypeVect.contains(et.getTrade().getProductType())) {
				return false;
			}
		}

        if(event instanceof PSEventValuation){

            PSEventValuation et = (PSEventValuation) event;
            Trade trade = ((PSEventValuation) event).getTrade();
            if ((null!=trade)){
                if(!tradeTypeVect.contains(trade.getProductType())){
                    return false;
                }
            }

        }

		return true;
	}

}

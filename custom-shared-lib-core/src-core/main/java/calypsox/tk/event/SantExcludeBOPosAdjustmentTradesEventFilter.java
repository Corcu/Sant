/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.BOPositionAdjustment;

/**
 * Event Filter - to filter out BOPositionAdjustment Trades
 * 
 * 
 * @author Soma
 * 
 */
public class SantExcludeBOPosAdjustmentTradesEventFilter implements EventFilter {

	@Override
	public boolean accept(final PSEvent event) {
		if (event instanceof PSEventTrade) {
			PSEventTrade tradeEvent = (PSEventTrade) event;
			if (BOPositionAdjustment.BOPOSITIONADJUSTMENT.equals(tradeEvent.getTrade().getProductType())) {
				return false;
			}
		} else if (event instanceof PSEventTransfer) {
			PSEventTransfer transferEvent = (PSEventTransfer) event;
			if (BOPositionAdjustment.BOPOSITIONADJUSTMENT.equals(transferEvent.getBoTransfer().getProductType())) {
				return false;
			}
		}

		return true;
	}
}

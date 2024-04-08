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
import com.calypso.tk.product.CollateralExposure;

/**
 * Event Filter - to filter out Collateral Exposure Trades
 * 
 * 
 * @author Soma
 * 
 */
public class SantExcludeCollateralExposureTradesEventFilter implements EventFilter {

	@Override
	public boolean accept(final PSEvent event) {
		boolean isAccepted = true;
		if (event instanceof PSEventTrade) {
			final PSEventTrade tradeEvent = (PSEventTrade) event;
			if (tradeEvent.getTrade().getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {
				isAccepted = false;
			}
		} else if (event instanceof PSEventTransfer) {
			final PSEventTransfer transfer = (PSEventTransfer) event;
			if (transfer.getBoTransfer().getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {
				isAccepted = false;
			}
		}

		return isAccepted;
	}
}

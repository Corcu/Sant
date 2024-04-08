/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import java.util.Arrays;
import java.util.List;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;

/**
 * Event Filter - to filter out Trades defined in the domain
 * 'SantExcludeTrades.ProductTypes'
 * 
 * 
 * @author Soma
 * 
 */
public class SantExcludeTradeTypesFallidasEventFilter implements EventFilter {
    private static final List<String> productTypes = Arrays.asList("Repo", "MarginCall", "SecLending", "Equity","Bond",
    		"BondAssetBacked","BondBrady","BondCLN","BondConvertible","BondFRN",
    		"BondHolding","BondMMDiscount","BondMMDiscountAUD","BondMMInterest","BondRevolver","TransferAgent","NONE");

	@Override
	public boolean accept(final PSEvent event) {
		String type = null;
		if (event instanceof PSEventTrade) {
			final PSEventTrade tradeEvent = (PSEventTrade) event;
			Trade trade = tradeEvent.getTrade();
			if (trade != null) {
				type = trade.getProductType();
			}
		} else if (event instanceof PSEventTransfer) {
			final PSEventTransfer xferEvt = (PSEventTransfer) event;
			BOTransfer xfer = xferEvt.getBoTransfer();
			if (xfer != null) {
				type = xfer.getProductType();
			}
		} else if (event instanceof PSEventMessage) {
			final PSEventMessage messEvt = (PSEventMessage) event;
			BOMessage mess = messEvt.getBoMessage();
			if (mess != null) {
				type = mess.getProductType();
			}
		} else if (event instanceof PSEventInventorySecPosition) {
			final PSEventInventorySecPosition invEvt = (PSEventInventorySecPosition) event;
			final InventorySecurityPosition posicionDisponible=invEvt.getPosition();
			return (posicionDisponible!=null && InventorySecurityPosition.INTERNAL_CLASS.equals(posicionDisponible.getInternalExternal()) &&
					InventorySecurityPosition.SETTLE_DATE.equals(posicionDisponible.getDateType()) && 
					InventorySecurityPosition.ACTUAL_TYPE.equals(posicionDisponible.getPositionType()) &&
			posicionDisponible.getConfigId()==0);			
		} else {
			return true;
		}
		 return productTypes.contains(type);
	}
}

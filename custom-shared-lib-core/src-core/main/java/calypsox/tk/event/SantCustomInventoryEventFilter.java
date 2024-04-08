/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import com.calypso.tk.core.Product;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.CollateralExposure;

/**
 * Filters out transfer if the Trade type is SecLending, Repo,
 * CollateralExposure.
 * 
 * 
 * @author Soma
 * 
 */
public class SantCustomInventoryEventFilter implements EventFilter {

    @Override
    public boolean accept(final PSEvent event) {
	boolean isAccepted = false;
	if (event instanceof PSEventTransfer) {
	    final PSEventTransfer transfer = (PSEventTransfer) event;

	    if (!transfer.getBoTransfer().getProductType().equals(Product.REPO)
		    && !transfer.getBoTransfer().getProductType()
			    .equals(Product.SEC_LENDING)
		    && !transfer.getBoTransfer().getProductType()
			    .equals(CollateralExposure.PRODUCT_TYPE)) {
		isAccepted = true;
	    }

	}

	return isAccepted;
    }
}

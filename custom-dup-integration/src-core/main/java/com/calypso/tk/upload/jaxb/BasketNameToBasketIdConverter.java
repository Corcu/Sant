package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.github.dozermapper.core.DozerConverter;

public class BasketNameToBasketIdConverter extends DozerConverter<String, Integer> {

	public BasketNameToBasketIdConverter() {
        super(String.class, Integer.class);
    }

	@Override
	public String convertFrom(Integer id, String name) {
		if (id > 0) {
			try {
				com.calypso.tk.core.Product prod = DSConnection.getDefault().getRemoteProduct().getProduct(id);
				if (prod instanceof com.calypso.tk.product.Basket) {
					return prod.getName();
				}
			} catch (CalypsoServiceException e) {
				Log.debug("UPLOADER", "Exception in fetching Basket Product");
			}
		}
		return null;
	}

	@Override
	public Integer convertTo(String name, Integer id) {
		if (!Util.isEmpty(name)) {
			try {
				return DSConnection.getDefault().getRemoteProduct().isBasketNameUsed(name);
			} catch (CalypsoServiceException e) {
				Log.debug("UPLOADER", "Exception in fetching Basket Product");
			}
		}

		return 0;
	}
}

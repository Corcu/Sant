package com.calypso.tk.upload.jaxb;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.service.DSConnection;
import com.github.dozermapper.core.DozerConverter;

public class FutureContractConverter extends DozerConverter<com.calypso.tk.product.FutureContract, FutureContract> {

	public FutureContractConverter() {
        super(com.calypso.tk.product.FutureContract.class, FutureContract.class);
    }

	@Override
	public com.calypso.tk.product.FutureContract convertFrom(FutureContract futJaxb,
			com.calypso.tk.product.FutureContract futProd) {
		if (futJaxb == null) {
			return null;
		}
		
		return BOCache.getFutureContract(DSConnection.getDefault(), futJaxb.getCurrency(), futJaxb.getExchange(), futJaxb.getName());
	}

	@Override
	public FutureContract convertTo(com.calypso.tk.product.FutureContract futProd, FutureContract futJaxb) {
		if (futProd == null) {
			return null;
		}
		
		if (futJaxb == null) {
			futJaxb = new FutureContract();
		}
		
		futJaxb.setCurrency(futProd.getCurrency());
		futJaxb.setExchange(futProd.getExchange());
		futJaxb.setName(futProd.getName());
		
		return futJaxb;
	}

}

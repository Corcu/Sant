package com.calypso.tk.upload.jaxb;

import java.rmi.RemoteException;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.util.ValidationUtil;
import com.github.dozermapper.core.DozerConverter;

public class BenchmarkToBenchmarkSecConverter extends DozerConverter<com.calypso.tk.core.Product, BenchmarkSec> {

	public BenchmarkToBenchmarkSecConverter() {
        super(com.calypso.tk.core.Product.class, BenchmarkSec.class);
    }

	@Override
	public Product convertFrom(BenchmarkSec benchmarkSec, Product benchmark) {
		if (benchmarkSec == null) {
			return null;
		}
		String secCode = benchmarkSec.getBenchmarkSecCodeName();
		String secValue = benchmarkSec.getBenchmarkSecCodeValue();
		com.calypso.tk.core.Product existingProduct = null;
		
		try {
			com.calypso.tk.refdata.ProductCode productCode = DSConnection.getDefault().getRemoteProduct().getProductCode(secCode);

			if (productCode.getUniqueB()) {
				existingProduct = ValidationUtil.getProductByCode(secCode, secValue);
			}
			if (existingProduct != null) {
				benchmark = existingProduct;
				return existingProduct;
			}
		} catch (RemoteException e) {
			Log.error("UPLOADER", " Remote Exception Occured while Processing Bond Product : " + e.toString());
		}
		
		return null;
	}

	@Override
	public BenchmarkSec convertTo(Product benchmark, BenchmarkSec benchmarkSec) {
		if (benchmark == null) {
			return null;
		}
		
		if (benchmarkSec == null) {
			benchmarkSec = new BenchmarkSec();
		}
		
		benchmarkSec.setBenchmarkSecCodeName("ISIN");
		benchmarkSec.setBenchmarkSecCodeValue(benchmark.getSecCode("ISIN"));
		
		return benchmarkSec;
	}

}

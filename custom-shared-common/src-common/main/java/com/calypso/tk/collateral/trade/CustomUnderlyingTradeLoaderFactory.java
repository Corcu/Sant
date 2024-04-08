package com.calypso.tk.collateral.trade;

/**
 * @author aalonsop
 */
//@Component("underlyingTradeLoaderFactory")
//@Primary
public class CustomUnderlyingTradeLoaderFactory extends UnderlyingTradeLoaderFactory {

    public CustomUnderlyingTradeLoaderFactory() {
    }

    public UnderlyingTradeLoader getInstance(String loaderType) {
        return new CustomDefaultUnderlyingTradeLoader();
    }
}

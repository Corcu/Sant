package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.Frequency;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import com.github.dozermapper.core.DozerConverter;

public class FlipperToBondConverter extends DozerConverter<com.calypso.tk.product.Bond, Flipper> {

	public FlipperToBondConverter() {
        super(com.calypso.tk.product.Bond.class, Flipper.class);
    }

	@Override
	public com.calypso.tk.product.Bond convertFrom(Flipper flipper, com.calypso.tk.product.Bond bond) {
		if (flipper == null || !flipper.isFlipperB()) {
			return bond;
		}
		
		if (bond == null) {
			bond = new com.calypso.tk.product.Bond();
		}
		
		bond.setFlipperDate(UploaderTradeUtil.getDate(flipper.getDate()));
		bond.setFlipperFrequency(Frequency.valueOf(flipper.getFlipperFrequency()));
		
		return null;
	}

	@Override
	public Flipper convertTo(com.calypso.tk.product.Bond bond, Flipper flipper) {
		if (bond == null) {
			return flipper;
		}
		
		if (flipper == null) {
			flipper = new Flipper();
		}
		
		if (bond.getFlipperDate() != null) {
			flipper.setFlipperB(true);
			flipper.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bond.getFlipperDate()));
			if (bond.getFlipperFrequency() != null) {
				flipper.setFlipperFrequency(bond.getFlipperFrequency().toString());
			}
		}
		
		return flipper;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.columns;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;

public class SantPricerMeasureValueColumn extends SantColumn {

    private Object amount;

    private Object ccy;

    public SantPricerMeasureValueColumn(final PLMark plMark,
	    final String pricerMeasure) {
	build(plMark, pricerMeasure);

    }

    private void build(final PLMark plMark, final String pricerMeasure) {
	if (plMark == null) {
	    return;
	}

	final PLMarkValue pLMarkValue = CollateralUtilities.retrievePLMarkValue(plMark, pricerMeasure);
	if (pLMarkValue == null) {
	    return;
	}

	this.amount = getSignedAmount(pLMarkValue.getMarkValue(),
		pLMarkValue.getCurrency());
	this.ccy = pLMarkValue.getCurrency();
    }

    public Object getAmount() {
	return this.amount;
    }

    public Object getCcy() {
	return this.ccy;
    }

}

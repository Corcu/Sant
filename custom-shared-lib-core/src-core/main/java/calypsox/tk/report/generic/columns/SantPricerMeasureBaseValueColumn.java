/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.columns;

import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CollateralConfig;

public class SantPricerMeasureBaseValueColumn extends SantColumn {

	private Object amount = null;
	private Object closingPrice = null;

	public SantPricerMeasureBaseValueColumn(final PLMark plMark, final CollateralConfig marginCallConfig,
			final String pricerMeasure, final SantGenericQuotesLoader quotesLoader) {

		build(plMark, marginCallConfig, pricerMeasure, quotesLoader);
	}

	private void build(final PLMark plMark, final CollateralConfig marginCallConfig, final String pricerMeasure,
			final SantGenericQuotesLoader quotesLoader) {

		if ((plMark == null) || (marginCallConfig == null)) {
			return;
		}
		final PLMarkValue pLMarkValue = CollateralUtilities.retrievePLMarkValue(plMark, pricerMeasure);
		final String baseCcy = marginCallConfig.getCurrency();
		final String plCcy = pLMarkValue.getCurrency();
		double plvalue = pLMarkValue.getMarkValue();

		double price = 0;
		if (!baseCcy.equals(plCcy)) {
			QuoteValue qv = getFxRate(quotesLoader, plCcy, baseCcy);
			if (qv == null) {
				qv = getFxRate(quotesLoader, baseCcy, plCcy);
				if (qv == null) {
					this.amount = "No FX rate " + baseCcy + "/" + plCcy + " MTM value = " + plvalue + " " + plCcy;
					return;
				}
				price = qv.getClose();
				if (price != 0) {
					price = 1 / price;
				}
			} else {
				price = qv.getClose();
			}

			plvalue *= price;
			this.amount = getSignedAmount(plvalue, baseCcy);
			if (price != 0) {
				this.closingPrice = new Amount(price);
			}
		}

		else {
			this.amount = new SignedAmount(plvalue);
			this.closingPrice = new Amount(1.0);
		}
	}

	public Object getAmount() {
		return this.amount;
	}

	public Object getFXRate() {
		return this.closingPrice;
	}

}

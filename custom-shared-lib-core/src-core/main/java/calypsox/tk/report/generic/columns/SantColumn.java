/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.columns;

import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;

public class SantColumn {

    protected Object getSignedAmount(final double amount, final String ccy) {
	final int digits = CurrencyUtil.getRoundingUnit(ccy);
	return new SignedAmount(amount, digits);
    }

    protected Object getLegalEntity(final int leId) {
	return BOCache.getLegalEntity(DSConnection.getDefault(), leId);
    }

    protected QuoteValue getFxRate(final SantGenericQuotesLoader quotesLoader,
	    final String primCcy, final String secCcy) {

	final String quoteName = "FX." + primCcy + "." + secCcy;

	return quotesLoader.fetchQuoteValue(quoteName);
    }
}

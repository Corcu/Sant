/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.quotes;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;

public class FXQuoteHelper {

	private static PricingEnv pricingEnv;

	private PricingEnv objectPricingEnv;

	public FXQuoteHelper(String pricingEnvName){
		try {
			this.objectPricingEnv= DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pricingEnvName);
		} catch (CalypsoServiceException exc) {
			Log.warn(this.getClass().getSimpleName(),"Error while getting PricingEnv:"+pricingEnvName);
		}
	}

	public static void setPricingEnv(PricingEnv pricingEnv) {
		FXQuoteHelper.pricingEnv = pricingEnv;
	}

	/**
	 * Retrieve FX quote according market convention
	 * 
	 * @param ccy1
	 * @param ccy2
	 * @param quoteDate
	 * @return
	 * @throws MarketDataException
	 */
	public static QuoteValue getMarketConventionFXQuote(String ccy1, String ccy2, JDate quoteDate)
			throws MarketDataException {
		CurrencyPair cp = CurrencyUtil.getCurrencyPair(ccy1, ccy2);

		QuoteValue quote = new QuoteValue();
		quote.setQuoteSetName(pricingEnv.getQuoteSet().getName());
		quote.setName(cp.getQuoteName());
		quote.setQuoteType(QuoteValue.PRICE);
		quote.setDate(quoteDate);

		QuoteValue quoteMarketConvention = pricingEnv.getQuoteSet().getQuote(quote);

		if (quoteMarketConvention == null) {
			cp = CurrencyUtil.getCurrencyPair(ccy2, ccy1);
			quote.setName(cp.getQuoteName());
			quoteMarketConvention = pricingEnv.getQuoteSet().getQuote(quote);
		}

		return quoteMarketConvention;
	}

	/**
	 * Retrieve FX quote according market convention
	 *
	 * @param ccy1
	 * @param ccy2
	 * @param quoteDate
	 * @return
	 * @throws MarketDataException
	 */
	public QuoteValue getMrktConventionFXQuote(String ccy1, String ccy2, JDate quoteDate)
			throws MarketDataException {
		CurrencyPair cp = CurrencyUtil.getCurrencyPair(ccy1, ccy2);

		QuoteValue quote = new QuoteValue();
		quote.setQuoteSetName(this.objectPricingEnv.getQuoteSet().getName());
		quote.setName(cp.getQuoteName());
		quote.setQuoteType(QuoteValue.PRICE);
		quote.setDate(quoteDate);

		QuoteValue quoteMarketConvention = this.objectPricingEnv.getQuoteSet().getQuote(quote);

		if (quoteMarketConvention == null) {
			cp = CurrencyUtil.getCurrencyPair(ccy2, ccy1);
			quote.setName(cp.getQuoteName());
			quoteMarketConvention = this.objectPricingEnv.getQuoteSet().getQuote(quote);
		}

		return quoteMarketConvention;
	}

	/**
	 * Retrieve FX quote according the order of the currencies ccy1, ccyy2
	 * 
	 * @param ccy1
	 * @param ccy2
	 * @param quoteDate
	 * @return
	 * @throws MarketDataException
	 */
	public QuoteValue getFXQuote(String ccy1, String ccy2, JDate quoteDate) throws MarketDataException {
		return objectPricingEnv.getFXQuote(ccy1, ccy2, quoteDate);
	}

	public static Double convertAmountInEUR(Double value, String ccy1) throws MarketDataException {
		if ("EUR".equals(ccy1)) {
			return value;
		}
		QuoteValue qv = pricingEnv.getFXQuote(ccy1, "EUR", pricingEnv.getDate());
		if ((qv == null) || (qv.getClose() == Double.NaN)) {
			return null;
		}
		return value * qv.getClose();
	}

	public Double convertAmountInUSD(Double value, String ccy1, JDate quoteDate) throws MarketDataException {
		if ("USD".equals(ccy1)) {
			return value;
		}
		QuoteValue qv = objectPricingEnv.getFXQuote(ccy1, "USD", quoteDate);
		if ((qv == null) || (Double.isNaN(qv.getClose()))) {
			return 0.0d;
		}
		return value * qv.getClose();
	}

	public Double convertAmountInEUR(Double value, String ccy1,JDate quoteDate) throws MarketDataException {
		if ("EUR".equals(ccy1)) {
			return value;
		}
		QuoteValue qv = objectPricingEnv.getFXQuote(ccy1, "EUR", quoteDate);
		if ((qv == null) || (Double.isNaN(qv.getClose()))) {
			return 0.0d;
		}
		return value * qv.getClose();
	}
}

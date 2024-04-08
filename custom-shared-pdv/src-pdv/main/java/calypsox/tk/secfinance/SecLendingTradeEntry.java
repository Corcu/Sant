package calypsox.tk.secfinance;

import java.util.Locale;
import java.util.Vector;

import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.InvalidQuoteException;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.MissingQuoteException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.tradeentry.factory.ValuationInfoProvider;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.fieldentry.FieldEntry;
import com.calypso.tk.util.fieldentry.FieldEntry.InputValidationException;

/**
 * Bug fix for wrong conversion of String in case of Spanish Locale
 * 
 * @author CedricAllain
 *
 */
public class SecLendingTradeEntry extends com.calypso.tk.secfinance.SecLendingTradeEntry {

	private Object _initialMarginValueLocked;

	private Object _initialMarginFXRateLocked;

	public SecLendingTradeEntry(Trade trade, JDatetime valDatetime, PricingEnv pricingEnv,
			SecFinanceTradeEntryContext tradeEntryContext) {
		super(trade, valDatetime, pricingEnv, tradeEntryContext);
	}

	public SecLendingTradeEntry(Trade trade, ValuationInfoProvider valuationInfoProvider) {
		super(trade, valuationInfoProvider);
	}

	public SecLendingTradeEntry(Trade trade, ValuationInfoProvider valuationInfoProvider,
			SecFinanceTradeEntryContext tradeEntryContext) {
		super(trade, valuationInfoProvider, tradeEntryContext);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setInitialMarginValueByFieldInput(Object initialMarginValue, Vector errors) {
		String value = objectToString(initialMarginValue);
		String initialMarginValueString = CurrencyUtil.checkAmount(value, getFeeCurrencyDecimals(), false);
		double imDouble = Util.stringToNumber(initialMarginValueString);
		if (imDouble == 0.0D) {
			resetInitialMarginValue(errors);
		} else {
			setInitialMarginValue(imDouble, errors);
			this._initialMarginValueLocked = initialMarginValue;
		}
	}

	protected void resetOrNotInitialMarginValue(Vector<String> errors) {
		if (this._initialMarginValueLocked != null && isInitialMarginValueApplicable()) {
			return;
		}
		resetInitialMarginValue(errors);
	}

	private void resetInitialMarginValue(Vector<String> errors) {
		SecLending secLending = getSecLending();
		if (secLending == null) {
			return;
		}
		double initialMarginValue = 0.0D;
		if (isInitialMarginValueApplicable()) {
			Collateral selectedCollateral = getSelectedCollateral();
			if (selectedCollateral != null) {
				initialMarginValue = -selectedCollateral.getValue();
				if (isInitialMarginFXRateApplicable()) {
					initialMarginValue *= secLending.getMarginCallConversionRate(secLending.getInitialMarginFXRate());
				}
			}
		}
		setInitialMarginValue(initialMarginValue, errors);
	}

	@SuppressWarnings("rawtypes")
	public void setInitialMarginValue(double initialMarginValue, Vector errors) {
		this._initialMarginValueLocked = null;
		super.setInitialMarginValue(initialMarginValue, errors);
	}

	@SuppressWarnings("rawtypes")
	public void setInitialMarginFXRateByInputField(Object initialMarginFXRate, Vector errors) {
		double rateDouble = Util.stringToNumber(objectToString(initialMarginFXRate));
		if (rateDouble == 0.0D) {
			resetInitialMarginFXRate(errors);
		} else {
			setInitialMarginFXRate(rateDouble, errors);
			this._initialMarginFXRateLocked = initialMarginFXRate;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void resetInitialMarginFXRate(Vector errors) {
		SecLending secLending = this.getSecLending();
		if (secLending != null) {
			double fxRate;
			try {
				fxRate = secLending.lookupMarginCallFXRate(this.getPricingEnv(), this.getValDate());
			} catch (InvalidQuoteException var6) {
				fxRate = 1.0D;
				errors.add(var6.getMessage());
			} catch (MissingQuoteException var7) {
				fxRate = 1.0D;
				errors.add(var7.getMessage());
			} catch (MarketDataException var8) {
				fxRate = 1.0D;
				errors.add(var8.getMessage());
			}

			this.setInitialMarginFXRate(fxRate, errors);
		}
	}

	@SuppressWarnings("rawtypes")
	public void setFeeFxRateValue(Object feeFxRate, Vector errors) {
		setFeeFxRate(Util.stringToNumber(objectToString(feeFxRate)), errors);
	}

	@SuppressWarnings("rawtypes")
	public void setFeeValue(Object feeValue, Vector errors) {
		String feeString = CurrencyUtil.checkAmount(objectToString(feeValue), getFeeCurrencyDecimals(), false);

		setFeeValue(Util.stringToNumber(feeString), errors);
	}

	@SuppressWarnings("rawtypes")
	public void setFeeBillingRateValue(Object feeBillingRate, Vector errors) {
		SecLending secLending = getSecLending(errors);
		if (secLending == null)
			return;
		if (!checkHasFee(errors))
			return;
		setFeeBillingRate(
				SecLending.convertBillingRateTextToDouble(objectToString(feeBillingRate), secLending.getFeeType()),
				errors);
	}

	@SuppressWarnings("rawtypes")
	public void setMinimumFeeValue(Object minimumFee, Vector errors) {
		String minimumFeeString = CurrencyUtil.checkAmount(objectToString(minimumFee), getFeeCurrencyDecimals(), false);

		setMinimumFee(Util.stringToNumber(minimumFeeString.toString()), errors);
	}

	public String objectToString(Object o) {
		String value = o.toString();

		if (o instanceof Double) {
			// Use Util function for String conversion
			// core use a simple toString call which
			// makes conversion fails in case of changed Locale
			value = Util.numberToString((Double) o);
		}

		return value;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean checkHasFee(Vector errors) {
		if (this.hasFee()) {
			return true;
		} else {
			if (errors != null) {
				errors.add("Fee Type is No Fee. Fee fields cannot be changed.");
			}

			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	public void setInitialMarginFXRate(double initialMarginFXRate, Vector errors) {
		this._initialMarginFXRateLocked = null;

		super.setInitialMarginFXRate(initialMarginFXRate, errors);

	}

	@SuppressWarnings("rawtypes")
	protected void resetOrNotInitialMarginFXRate(Vector errors) {
		if (this._initialMarginFXRateLocked == null || !this.isInitialMarginFXRateApplicable()) {
			this.resetInitialMarginFXRate(errors);
		}
	}
	
	 public Object stringToType(FieldEntry fieldEntry, Class type, String stringValue) throws InputValidationException {
		    if (type == DisplayValue.class || Double.class.equals(type)) {
		    	Double d =Util.stringToNumber(stringValue,Locale.US);
		    	stringValue = Util.numberToString(d);
		    }

		    return super.stringToType(fieldEntry, type, stringValue);
		 }

}

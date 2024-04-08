package calypsox.tk.csdr;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.FdnCurrencyDefault;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;

/**
 * @author aalonsop
 */
public abstract class CSDRPenaltyAmountCalculator {

	private static final String EUR = "EUR";
	protected BOTransfer boTransfer;
	private Product product;
	private CSDRSecurityCategory securityCategory;

	private JDate buyInStartDate;
	private JDate defStartDate;
	private JDate compStartDate;

	Vector<String> holidays;

	double quoteCloseValue;

	private final String extPeriodAmountAttr = "CSDRPotencialPenaltyExtPeriod";
	private final String buyInPeriodAmountAttr = "CSDRPotencialPenaltyBuyInPeriod";
	private final String defPeriodAmountAttr = "CSDRPotencialPenaltyDefPeriod";

	private final String dailyPenaltyAttr = "CSDRPotencialPenaltyDaily";

	private final String compStartDateAttr = "CSDRCompensationDate";
	private final String buyInStartDateAttr = "CSDRBuyInDate";
	private final String defStartDateAttr = "CSDRDeferralDate";
	private final String potPenaltyCurrencyAttr = "CSDRPotencialPenaltyCcy";

	public CSDRPenaltyAmountCalculator(BOTransfer boTransfer, Product product) {
		this.boTransfer = boTransfer;
		if (boTransfer != null) {
			this.product = product;
			this.securityCategory = Optional.ofNullable(this.product).map(p -> p.getSecCode("CSDR_Penalty_Category"))
					.map(CSDRSecurityCategory::lookup).orElse(null);
			initHolidays(boTransfer);
			initPeriodDates();
			initQuote();
		}
	}

	private void initHolidays(BOTransfer boTransfer) {
		String currency = boTransfer.getSettlementCurrency();
		try {
			CurrencyDefault cd = DSConnection.getDefault().getRemoteReferenceData().getCurrencyDefault(currency);
			this.holidays = Optional.ofNullable(cd).map(FdnCurrencyDefault::getDefaultHolidays).orElse(null);
		} catch (CalypsoServiceException e) {
			Log.error(this, e.getCause());
		}
		if (this.holidays == null) {
			this.holidays = new Vector<>();
			holidays.add("SYSTEM");
		}
	}

	private void initQuote() {
		this.quoteCloseValue = getQuote(getQuoteSet());
	}

	private void initPeriodDates() {
		buyInStartDate = Optional.ofNullable(securityCategory)
				.map(cat -> cat.getAdjustedCSDRDate(boTransfer, CSDRPenaltyPeriod.BUYIN, this.holidays))
				.map(JDate::valueOf).orElse(null);
		defStartDate = Optional.ofNullable(securityCategory)
				.map(cat -> cat.getAdjustedCSDRDate(boTransfer, CSDRPenaltyPeriod.DEFERRAL, this.holidays))
				.map(JDate::valueOf).orElse(null);
		compStartDate = Optional.ofNullable(securityCategory)
				.map(cat -> cat.getAdjustedCSDRDate(boTransfer, CSDRPenaltyPeriod.COMPENSATION, this.holidays))
				.map(JDate::valueOf).orElse(null);
	}

	public void calculatePenalties() {
		if (isElegibleSecCode() && areDatesInitialized()) {
			boolean isExcl = isExcludedInst();
			boTransfer.setAttribute(extPeriodAmountAttr, calculateAsString(getNumberOfDays(buyInStartDate), isExcl));
			boTransfer.setAttribute(buyInPeriodAmountAttr, calculateAsString(getNumberOfDays(defStartDate), isExcl));
			boTransfer.setAttribute(defPeriodAmountAttr, calculateAsString(getNumberOfDays(compStartDate), isExcl));
			boTransfer.setAttribute(dailyPenaltyAttr, String.valueOf(calculateDailyPenalty(isExcl)));
			boTransfer.setAttribute(potPenaltyCurrencyAttr, getPenaltyCurrency(isExcl));
			setPeriodDatesXferAttributes();
		}
	}

	private void setPeriodDatesXferAttributes() {
		boTransfer.setAttribute(compStartDateAttr, formatJDate(compStartDate));
		boTransfer.setAttribute(buyInStartDateAttr, formatJDate(buyInStartDate));
		boTransfer.setAttribute(defStartDateAttr, formatJDate(defStartDate));
	}

	private String formatJDate(JDate jDate) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = jDate.getDate();
		return format.format(date);
	}

	private boolean isElegibleSecCode() {
		return Optional.ofNullable(this.product).map(p -> p.getSecCode("CSDR_Eligibility"))
				.map(value -> "Y".equals(value) || Boolean.parseBoolean(value)).orElse(false);
	}

	protected String calculateAsString(long periodDays, boolean isExcl) {
		return String.valueOf(calculate(periodDays, isExcl));
	}

	protected double calculate(long periodDays, boolean isExcl) {
		return calculateDailyPenalty(isExcl) * periodDays;
	}

	protected double calculateDailyPenalty(boolean isExcl) {
		double penaltyAmount = 0.0d;
		double settleAmt = getSettlementAmount();
		double price = this.quoteCloseValue;
		double rate = getPenaltyRate();
		if (rate > 0.0d) {
			rate = rate / 10000;
			double sign = this.boTransfer.getSign();
			penaltyAmount = Math.abs(settleAmt * price * rate) * sign;
		}
		if (!isExcl) {
			penaltyAmount = convertAmountToCurrency(penaltyAmount);
		}
		return penaltyAmount;
	}

	private boolean isExcludedInst() {
		int payerSDI = boTransfer.getPayerSDId();
		SettleDeliveryInstruction sdiPayer = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), payerSDI);
		int recSDI = boTransfer.getReceiverSDId();
		SettleDeliveryInstruction sdiReceiver = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), recSDI);
		Vector<String> dv = LocalCache.getDomainValues(DSConnection.getDefault(), "CSDRExceptionCustodianCCY");
		for (String strDv : dv) {
			if (Optional.ofNullable(sdiPayer).map(sdi -> sdi.getDescription()).map(str -> str.contains(strDv))
					.orElse(false)) {
				return true;
			}
			if (Optional.ofNullable(sdiReceiver).map(sdi -> sdi.getDescription()).map(str -> str.contains(strDv))
					.orElse(false)) {
				return true;
			}
		}
		return false;
	}

	private String getPenaltyCurrency(boolean isExcl) {
		String currency = EUR;
		if (isExcl) {
			currency = boTransfer.getSettlementCurrency();
		}
		return currency;
	}

	private Double convertAmountToCurrency(Double penaltyAmount) {
		String currency = boTransfer.getSettlementCurrency();
		try {
			QuoteSet qs = DSConnection.getDefault().getRemoteMarketData().getQuoteSet(getQuoteSet());
			CurrencyPair cp = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(EUR, currency);
			double fxRate = Optional.ofNullable(qs).map(q -> {
				try {
					return q.getFXQuote(cp, currency, JDate.getNow(), true);
				} catch (MarketDataException e) {
					Log.error(this, e);
				}
				return null;
			}).map(qv -> qv.getClose()).orElse(1.0d);
			return CurrencyUtil.convertAmount(penaltyAmount, currency, EUR, fxRate);
		} catch (CalypsoServiceException | MarketDataException e) {
			Log.error(this, e);
		}
		return Double.NaN;
	}

	private double getQuote(String quoteSetName) {
		String quoteName = Optional.ofNullable(product).map(Product::getQuoteName).orElse("");
		return getQuoteFromQuoteSet(quoteName, quoteSetName, JDate.getNow().getJDatetime());
	}

	private double getQuoteFromQuoteSet(String quoteName, String quoteSetName, JDatetime valDate) {
		double quoteCloseValue = 0.0d;
		Vector<String> quoteNames = new Vector<>();
		quoteNames.add(quoteName);
		try {
			Vector quotes = DSConnection.getDefault().getRemoteMarketData().getQuotes(valDate, quoteSetName,
					quoteNames);
			if (!Util.isEmpty(quotes)) {
				quoteCloseValue = Optional.ofNullable(quotes.get(0)).filter(q -> q instanceof QuoteValue)
						.map(q -> ((QuoteValue) q).getClose()).orElse(0.0d);
			}
		} catch (CalypsoServiceException exc) {
			Log.warn(this, exc.getCause());
		}
		return quoteCloseValue;
	}

	private double getPenaltyRate() {
		return Optional.ofNullable(this.securityCategory).map(CSDRSecurityCategory::getBPSRate).orElse(0.0d);
	}

	/**
	 *
	 * @param periodDate
	 * @return The number of days for each period when the penalty rate is applied
	 */
	private long getNumberOfDays(JDate periodDate) {
		JDate xferValueDate = boTransfer.getValueDate();
		return Holiday.getCurrent().numberOfBusinessDays(xferValueDate, periodDate, this.holidays) - 1;
	}

	private boolean areDatesInitialized() {
		return compStartDate != null && defStartDate != null && buyInStartDate != null;
	}

	public abstract String getQuoteSet();

	public abstract double getSettlementAmount();
}

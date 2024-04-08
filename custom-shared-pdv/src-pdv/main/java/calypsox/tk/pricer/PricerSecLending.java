package calypsox.tk.pricer;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerInput;
import com.calypso.tk.pricer.PricerSecFinanceInput;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.TradeArray;
import java.util.ArrayList;
import java.util.HashSet;

public class PricerSecLending extends com.calypso.tk.pricer.PricerSecLending {

	public static final String TRANSIENT_KEYWORD_IS_NPV_COLLAT = "isNPVCollat";
	public static final String TRANSIENT_KEYWORD_ORIGINAL_TRADE_CCY = "OriginalTradeCurrency";

	/**
	 * for npv_collat we want the result in underlying currency, not trade currency
	 */
	public double npvCollat(Trade trade, SecFinance secFinance, int curveSide, JDatetime valDatetime, PricerInput input,
			PricingEnv env) throws PricerException {
		trade.addTransientKeyword(TRANSIENT_KEYWORD_IS_NPV_COLLAT, "true"); // add transient keyword to be able to
																			// detect the current pricing is NPV_COLLAT
		return this.collat(trade, secFinance, curveSide, 2, valDatetime, input, env);
	}

	/**
	 * for npv_collat we want the result in underlying currency, not trade currency
	 */
	protected TradeArray createCollateralTradeIfRequired(Trade trade, JDatetime valDatetime, String quoteName,
			PricingEnv env) {
		TradeArray trades = super.createCollateralTradeIfRequired(trade, valDatetime, quoteName, env);

		// for NPV_COLLAT we override the OriginalTradeCurrency with collateral currency
		// to avoid any conversion.
		if (Util.isTrue(trade.getTransientKeywordValue(TRANSIENT_KEYWORD_IS_NPV_COLLAT))) {
			for (int i = 0; i < trades.size(); i++) {
				Trade collateralTrade = trades.get(i);
				collateralTrade.addTransientKeyword(TRANSIENT_KEYWORD_ORIGINAL_TRADE_CCY,
						collateralTrade.getTradeCurrency());
			}
			trade.removeTransientKeyword(TRANSIENT_KEYWORD_IS_NPV_COLLAT);
		}

		return trades;
	}

	protected static HashSet<Integer> customizedPricerMeasure = new HashSet<Integer>();

	static {
		customizedPricerMeasure.add(PricerMeasure.INDEMNITY_ACCRUAL);
	}

    protected void process(Trade trade, JDatetime valDatetime, PricingEnv env, PricerSecFinanceInput input,
                           PricerMeasure... measures) throws PricerException {


        String activate = LocalCache.getDomainValueComment(DSConnection.getDefault(), "domainName", "MondayFeeFix");
        if (!Util.isEmpty(activate) && Boolean.parseBoolean(activate) && checkNextBusinessDay(env.getJDate(valDatetime), ((SecLending)trade.getProduct()).getMaturityDate())) {

            ArrayList<PricerMeasure> customPricerMeasure = new ArrayList<PricerMeasure>();
            ArrayList<PricerMeasure> corePricerMeasure = new ArrayList<PricerMeasure>();

            for (int i = 0; i < measures.length; i++) {
                PricerMeasure msr = measures[i];
                if (customizedPricerMeasure.contains(msr.getType())) {
                    customPricerMeasure.add(msr);
                } else {
                    corePricerMeasure.add(msr);
                }
            }

            JDate valDate = env.getJDate(valDatetime);
            SecLending secLending = (SecLending) trade.getProduct();
            String measureCurrency = secLending.getCurrency();
            if (measureCurrency == null) {
                throw new PricerException("No trade currency available for Trade " + trade.getLongId());
            }

            for (PricerMeasure msr : customPricerMeasure) {
                JDate matDate;
                msr.setCurrency(measureCurrency);

                switch (msr.getType()) {

                    case PricerMeasure.INDEMNITY_ACCRUAL:
                        PricerMeasure pM = getCollateralPricerMeasure(trade, valDatetime, env, msr);

                        matDate = secLending.getMaturityDate();
                        if ((matDate == null || secLending.getStartDate() == null) && !secLending.getOpenTermB()) {
                            pM.setValue(0.);
                        } else {
                            if (matDate == null && secLending.getOpenTermB()) {
                                matDate = valDate.addDays(secLending.getNoticeDays());
                            }
                            final boolean doFirst = env.getParameters().getFirstAccrual(secLending.getType());
                            JDate processingDate = JDate.valueOf(valDate);

                            try {

                                if (matDate.equals(processingDate)) {
                                    processingDate = getProcessingDateOnMaturityDate(processingDate);
                                }
                                if (secLending.isSBL() || secLending.getMaturityDate() == null
                                        || (matDate.after(processingDate) && secLending.getStartDate().lte(processingDate))) {
                                    double accrual = previousAccrualSECLENDING_FEE(trade, processingDate, env, doFirst);
                                    if (Util.isNonZeroNumber(accrual)) {
                                        // In the hypothetical case when Collateral
                                        // INDEMNITY or INDEMNITY_ACCRUAL is computed in
                                        // a different currency than the Fee
                                        if (Util.isNonZeroNumber(pM.getValue())
                                                && !Util.isSame(pM.getCurrency(), secLending.getFeeCurrency())) {
                                            try {
                                                pM.setValue(CurrencyUtil.convertAmount(env, pM.getValue(), pM.getCurrency(),
                                                        secLending.getFeeCurrency(), valDate, env.getQuoteSet()));
                                            } catch (MarketDataException e) {
                                                throw new PricerException(e);
                                            }
                                        }
                                        pM.setValue(pM.getValue() + accrual);
                                        pM.setCurrency(secLending.getFeeCurrency());
                                    }
                                } else {
                                    pM.setValue(0.);
                                }
                            } catch (CalypsoServiceException e) {
                                Log.error(this.getClass(), "Can not retrieve SYSTEM hoidays");
                            }
                        }
                        if (!Util.isNonZeroNumber(pM.getValue()) && !Util.isEmpty(secLending.getFeeCurrency())) {
                            pM.setCurrency(secLending.getFeeCurrency());
                        }
                        break;

                }
            }
            PricerMeasure[] corePricerMeasures = corePricerMeasure.toArray(new PricerMeasure[corePricerMeasure.size()]);

            super.process(trade, valDatetime, env, input, corePricerMeasures);
        } else {

            super.process(trade, valDatetime, env, input, measures);
        }
    }

	/**
	 * @param trade
	 * @param valDate
	 * @param env
	 * @param firstAccrual
	 * @return
	 */
	private double previousAccrualSECLENDING_FEE(Trade trade, JDate valDate, PricingEnv env,
											boolean firstAccrual) {
		double accrual = 0.;
		try {
			SecLending secLending = (SecLending) trade.getProduct();
			CashFlowSet cfs = secLending.getFlows(valDate);

			cfs = cfs.findEnclosingCashFlows(valDate, CashFlow.SECLENDING_FEE);
			if (cfs.isEmpty())
				return 0;

			// cf.calculate(env.getQuoteSet(),valDate);
			JDate accrualDate = valDate;
			if (firstAccrual)
				accrualDate = valDate.addDays(1);

			for (int i = 0; i < cfs.size(); i++) {
				CashFlow cf = cfs.get(i);
				accrual += cf.accrual(secLending, cfs, env, accrualDate);
			}

		} catch (Exception e) {
			Log.error(this, e);
		}

		return accrual;
	}

    private boolean checkNextBusinessDay(JDate valDate, JDate maturityDate) {
        JDate nextBusinessDay = valDate.addBusinessDays(1, Util.string2Vector("SYSTEM"));
        if (valDate.getMonth() != nextBusinessDay.getMonth()) {
            return false;
        }
        return true;
    }

    private JDate getProcessingDateOnMaturityDate(JDate processingDate) throws CalypsoServiceException {

        Holiday holiday = DSConnection.getDefault().getRemoteReferenceData().getHolidays();
        JDate previousDate = processingDate.addDays(-1);
        boolean isBusinessDay = holiday.isBusinessDay(previousDate, Util.string2Vector("SYSTEM"));
        if (previousDate.getMonth() == processingDate.getMonth() && !isBusinessDay) {
            return previousDate;
        } else {
            return processingDate;
        }
    }


}

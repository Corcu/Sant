package calypsox.tk.bo.workflow.rule;

import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;;

public class CSDRConciliationMonthlyPenaltyTradeRule implements WfTradeRule {

	private static final String Y = "Y";
	private static final String CSDR_MONTHLY_PENALTY_DISCREPANCY = "CSDRMonthlyPenaltyDiscrepancy";
	private static final String MONTHLY_CONFIRMED_AMOUNT = "MonthlyConfirmedAmount";
	private static final String PENALTY = "PENALTY";
	private static final String STRING_PCT = "%";
	private static final String COMMENT_SEPARATOR = ",";
	private static final String PENALTY_AMOUNT = "Penalty_Amount";
	private static final String EUR = "EUR";

	@Override
	public String getDescription() {
		return "Check if the daily penalty received in monthly MT537 differs from this trade amount";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade paramTrade2,
			Vector paramVector1, DSConnection paramDSConnection, Vector paramVector2, Task paramTask,
			Object paramObject, Vector paramVector3) {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade newTrade, Vector paramVector1,
			DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
			Vector paramVector3) {
		Product p = trade.getProduct();
		if (p instanceof SimpleTransfer && p.getSubType() != null && p.getSubType().equals(PENALTY)) {
			double mconfAmt = trade.getKeywordAsDouble(MONTHLY_CONFIRMED_AMOUNT);
			double penAmt = trade.getKeywordAsDouble(PENALTY_AMOUNT);
			String param = paramTaskWorkflowConfig.getTaskComment("CSDRConciliationMonthlyPenalty");
			double amount = 0.0d;

			// CHECK SIGNS Same sign
			if (mconfAmt == 0 || penAmt == 0 || (mconfAmt >= 0 && penAmt >= 0) || (mconfAmt <= 0 && penAmt <= 0)) {
				// BASIC CASE IS: 500,10
				if (param != null && param.contains(COMMENT_SEPARATOR) && param.length() >= 4) {
					boolean isPCT = param.contains(STRING_PCT);
					param = param.replaceAll(STRING_PCT, "");
					String[] params = param.split(COMMENT_SEPARATOR);
					double limitValue = Optional.ofNullable(params[0]).map(Double::valueOf).orElse(0.0d);
					double percentage = Optional.ofNullable(params[1]).map(Double::valueOf).orElse(0.0d);
					String currency = trade.getSettleCurrency();
					Double mconfAmtEUR = convertAmountToEUR(mconfAmt, currency);
					Double penAmtEUR = convertAmountToEUR(penAmt, currency);
					if (Math.abs(mconfAmtEUR) > limitValue || Math.abs(penAmtEUR) > limitValue) { // One is greater than
																							// limitValue
						if (isPCT && (percentage < 0 || percentage > 100.0d)) {
							percentage = 0;
						}
						if (isPCT) {
							amount = Math.abs(penAmt * percentage / 100.0d);
						} else {
							amount = Math.abs(percentage);
						}
						Double valueToCompare = Math.abs((Math.abs(mconfAmtEUR) - Math.abs(penAmtEUR)));
						if (valueToCompare != null) {
							if (valueToCompare > amount) {
								trade.removeKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY);
								trade.addKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY, Y);
							} else {
								if (mconfAmt != penAmt) { // Dif between mConfAmt and PenAmt
									trade.removeKeyword(PENALTY_AMOUNT);
									trade.addKeyword(PENALTY_AMOUNT, mconfAmt, 2);
									p.setPrincipal(mconfAmt);
									double quantity = Math.abs(trade.getQuantity());
									quantity = Math.copySign(quantity, mconfAmt);
									trade.setQuantity(quantity);
									trade.setProduct(p);
								}
								trade.removeKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY);
							}
						}
					} else { // Both are lower than limitValue
						if (mconfAmt != penAmt) { // Dif between mConfAmt and PenAmt
							trade.removeKeyword(PENALTY_AMOUNT);
							trade.addKeyword(PENALTY_AMOUNT, mconfAmt, 2); // New PenaltyAmount
							p.setPrincipal(mconfAmt);
							double quantity = Math.abs(trade.getQuantity());
							quantity = Math.copySign(quantity, mconfAmt);
							trade.setQuantity(quantity);
							trade.setProduct(p);
						}
						trade.removeKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY);
					}
				} else { // Wrong task comment
					Log.error(this, "Invalid format in Task comment");
				}
			} else { // Not equals signs
				trade.removeKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY);
				trade.addKeyword(CSDR_MONTHLY_PENALTY_DISCREPANCY, Y); // This operation has discrepancy
			}
		}
		return true;
	}

	/**
	 * Convert the amount in EUR
	 * 
	 * @param penaltyAmount amount to convert
	 * @param currency      base currency
	 * @return amount in EUR
	 */
	private Double convertAmountToEUR(Double penaltyAmount, String currency) {
		if (!EUR.equals(currency)) {
			try {
				QuoteSet qs = DSConnection.getDefault().getRemoteMarketData().getQuoteSet("OFFICIAL");
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
		} else {
			return penaltyAmount;
		}
		return Double.NaN;
	}

}

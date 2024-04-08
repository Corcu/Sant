package calypsox.tk.bo.workflow.rule;

import java.util.Optional;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;;

public class CSDRConciliationPotencialPenaltyTradeRule implements WfTradeRule {

	private static final String PENALTY = "PENALTY";
	private static final String EUR = "EUR";
	private static final String AMOUNT_CURRENCY = "Amount/Currency";
	private static final String AMOUNT = "Amount";
	private static final String CURRENCY = "Currency";
	private static final String CSDR_PENALTY_DISCREPANCY_TYPE = "CSDRPenaltyDiscrepancyType";
	private static final String STRING_PCT = "%";
	private static final String CSDR_POTENCIAL_PENALTY_DAILY = "CSDRPotencialPenaltyDaily";
	private static final String PENALTY_AMOUNT = "Penalty_Amount";
	private static final String CSDR_POTENCIAL_PENALTY_CCY = "CSDRPotencialPenaltyCcy";
	private static final String ORIGINAL_TRANSFER_ID = "OriginalTransferId";

	@Override
	public String getDescription() {
		return "Check if the Original Transfer potential daily penalty amount differs from this trade amount";
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
		Product p = trade.getProduct(); // Product Trade
		if (p instanceof SimpleTransfer && p.getSubType() != null && p.getSubType().equals(PENALTY)) { // Is
																										// simpletransfer
																										// and subtype
																										// PENALTY

			String discType = null;
			Long xferId = trade.getKeywordAsLongId(ORIGINAL_TRANSFER_ID); // Id penalty operation transfer
			BOTransfer xfer;
			try {
				xfer = paramDSConnection.getRemoteBackOffice().getBOTransfer(xferId); // Transfer of penalty op
				String potCcy = xfer.getAttribute(CSDR_POTENCIAL_PENALTY_CCY);
				if (!Util.isEmpty(potCcy)) {
					// CURRENCY COMPARATION
					if (!trade.getTradeCurrency().equalsIgnoreCase(potCcy)) { // NOT EQUALS CURRENCIES
						discType = CURRENCY; // Currency
					}
					// IMPORT COMPARATION
					double penaltyAmt = trade.getKeywordAsDouble(PENALTY_AMOUNT); // Penalty_Amount
					String strAmtPotPen = xfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY); // Potential Penalty
					// Getting values for rule.
					String param = paramTaskWorkflowConfig.getTaskComment("CSDRConciliationPotencialPenalty");
					double amount = 0.0d;

					if (param.contains(",") && param.length() >= 4) { // Example task comment: 500,10 OR 5,4
						String[] paramsArr = param.split(",");

						String limitValue = paramsArr[0];
						String percentajeVal = paramsArr[1];
						double potPenaltyAmt = Optional.ofNullable(strAmtPotPen).map(String::valueOf)
								.map(Double::valueOf).orElse(0.0d); // Potential Penalty Amount
						// CHECK VALUES SIGN Both negative or positive or one is zero.
						if (strAmtPotPen != null && ((potPenaltyAmt < 0 && penaltyAmt < 0)
								|| (potPenaltyAmt >= 0 && penaltyAmt >= 0) || penaltyAmt == 0 || potPenaltyAmt == 0)) {
							double limitValueDouble = Double.parseDouble(limitValue);
							// Get the discrepancy and convert to EUR to compare with the tolerance

							String currency = trade.getSettleCurrency(); // Current of trade
							penaltyAmt = convertAmountToEUR(penaltyAmt, currency); // Currency to EUR
							potPenaltyAmt = convertAmountToEUR(potPenaltyAmt, potCcy); // potCcy to EUR
							// CHECK AMOUNTS VALUES - one bigger than limitValue
							if (Math.abs(potPenaltyAmt) > limitValueDouble || Math.abs(penaltyAmt) > limitValueDouble) {
								Double valueToCompare = 0.0;
								if (percentajeVal != null && percentajeVal.contains(STRING_PCT)) { // Have %
									percentajeVal = percentajeVal.replaceAll(STRING_PCT, ""); // Replace sign of
																								// percentage
									double percentage = Optional.ofNullable(percentajeVal).map(String::valueOf)
											.map(Double::valueOf).orElse(0.0d); // Obtain percentage
									if (percentage < 0 || percentage > 100.0d) {
										percentage = 0; // Not valid percentage
									}
									amount = Math.abs(penaltyAmt * percentage / 100.0d); // Total amount
								} else {
									amount = Math.abs(Optional.ofNullable(percentajeVal).map(String::valueOf)
											.map(Double::valueOf).orElse(0.0d)); // Total amount
								}
								// Absolute difference between Potential Penalty Amount and Penalty Amount
								valueToCompare = Math.abs((Math.abs(potPenaltyAmt) - Math.abs(penaltyAmt)));
								// Check if difference is greater than percentage --> DISCREPANCY
								if (valueToCompare != null && valueToCompare > amount) {
									if (discType == null) {
										discType = AMOUNT; //
									} else {
										discType = AMOUNT_CURRENCY;
									}
								}
								trade.removeKeyword(CSDR_POTENCIAL_PENALTY_DAILY);
								trade.removeKeyword(CSDR_POTENCIAL_PENALTY_CCY);
								trade.addKeyword(CSDR_POTENCIAL_PENALTY_DAILY,
										xfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY));
								trade.addKeyword(CSDR_POTENCIAL_PENALTY_CCY,
										xfer.getAttribute(CSDR_POTENCIAL_PENALTY_CCY));
								if (discType != null) {
									trade.removeKeyword(CSDR_PENALTY_DISCREPANCY_TYPE);
									trade.addKeyword(CSDR_PENALTY_DISCREPANCY_TYPE, discType);
								}

							} else { // NO DISCREPANCY IN CURRENCIES

								trade.removeKeyword(CSDR_POTENCIAL_PENALTY_DAILY);
								trade.removeKeyword(CSDR_POTENCIAL_PENALTY_CCY);
								trade.addKeyword(CSDR_POTENCIAL_PENALTY_DAILY,
										xfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY));
								trade.addKeyword(CSDR_POTENCIAL_PENALTY_CCY,
										xfer.getAttribute(CSDR_POTENCIAL_PENALTY_CCY));

								if (discType != null) {
									trade.removeKeyword(CSDR_PENALTY_DISCREPANCY_TYPE);
									trade.addKeyword(CSDR_PENALTY_DISCREPANCY_TYPE, discType);
								}
							}
						} else { // SIGN NOT EQUALS
							if (discType != null) { // Discrepancy in Amount and Currency
								discType = AMOUNT_CURRENCY;
							} else {
								discType = AMOUNT; // Discrepancia de amount
							}
							trade.removeKeyword(CSDR_POTENCIAL_PENALTY_DAILY);
							trade.removeKeyword(CSDR_POTENCIAL_PENALTY_CCY);
							trade.addKeyword(CSDR_POTENCIAL_PENALTY_DAILY,
									xfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY));
							trade.addKeyword(CSDR_POTENCIAL_PENALTY_CCY, xfer.getAttribute(CSDR_POTENCIAL_PENALTY_CCY));

							trade.removeKeyword(CSDR_PENALTY_DISCREPANCY_TYPE);
							trade.addKeyword(CSDR_PENALTY_DISCREPANCY_TYPE, discType);
							Log.error("Signs not equals");
						}

					} else { // Ejm: 100 or 50% .....
						Log.error("Invalid task comment format");
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, e);
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

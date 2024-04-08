package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Dividend;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportStyle;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.fieldentry.FieldEntry;

public class PirumTransferReportStyle extends TransferReportStyle {
	private static final long serialVersionUID = 3338034850955955520L;

	public static final String TYPOLOGY = "Typology";
	public static final String SEC_DESCR = "Sec_descr";
	public static final String SEC_CODE = "Sec_code";
	public static final String FEE_BASIS = "Fee_basis";
	public static final String FX_RATE = "Fx_rate";
	public static final String CURRATE_FEE_RATE = "Currate_fee_rate";
	public static final String INITIAL_FEE_RATE = "Initial_fee_rate";
	public static final String CASH_VALUE = "Cash_value";
	public static final String CASH_START = "Cash_start";
	public static final String DIRECTION = "Direction";
	public static final String CASH_END = "Cash_end";
	public static final String FACE_VALUE = "Face Value";
	public static final String BOND_COUPON = "Bond_coupon";
	public static final String BOND_MATURITY = "Bond_maturity";
	public static final String LOAN_PRICE = "Loan_price";
	public static final String HAIRCUT = "Haircut";
	public static final String INITIAL_NOMINAL = "Initial_nominal";
	public static final String CURRENT_NOMINAL = "Current_nominal";
	public static final String LOAN_VALUE = "Loan_value";
	public static final String DIVIDEND_RATE = "Dividend_Rate";
	public static final String COLLATERAL_CCY = "Collateral_currency";
	public static final String SETTLEMENT_CCY = "Settlement_ccy";
	public static final String MC_CONTRACT_ID = "MC_Contract_Id";
	public static final String MC_CONTRACT_DESCR = "MC_Contract_descr";

	private static final String INTEREST = "INTEREST";
	private static final String FLOATING = "FLOATING";
	private static final String FIXED = "FIXED";
	private static final String TRIPARTY = "TRIPARTY";
	private static final String BSB = "BSB";
	private static final String STANDARD = "Standard";
	private static final String PAY = "PAY";
	private static final String RECEIVE = "RECEIVE";
	private static final String DIRTY_PRICE = "DirtyPrice";
	private static final String OFFICIAL = "OFFICIAL";
	private static final String MX_INITIAL_DIRTY_PRICE = "MXInitialDirtyPrice";
	private static final String MX_INITIAL_EQUITY_PRICE = "MXInitialEquityPrice";
	private static final String AUTO = "AUTO";
	private static final String TYPES = "TYPES";
	private static final String STATUS = "STATUS";
	public static final String CAPITAL_FACTOR = "CapitalFactor";

	private static final String[] ACCEPTED_STATUS = { "SETTLED", "VERIFIED" };
	private static final String[] ACCEPTED_TYPES = { "SECURITY" };


	private final HashMap<Integer, Product> mapBondProductById = new HashMap<>();
	private HashMap<Long, String> mapCurrNominalPerTrade = new HashMap<>();
	private HashMap<Long, String> mapCashEndPerTrade = new HashMap<>();
	private HashMap<Long, String> mapInitialFeeRatePerTrade = new HashMap<>();
	private HashMap<Long, CashFlow> mapCurrInterestFlowPerTrade = new HashMap<>();

	private List<Long> tradesWithFlowsCalculated = new ArrayList<Long>();
	private JDate valueDate = null;


	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		if (valueDate == null) {
			valueDate = JDate.valueOf(ReportRow.getValuationDateTime(row));
		} else if (!valueDate.equals(JDate.valueOf(ReportRow.getValuationDateTime(row)))) {
			valueDate = JDate.valueOf(ReportRow.getValuationDateTime(row));
			mapCurrNominalPerTrade = new HashMap<>();
			mapCashEndPerTrade = new HashMap<>();
			mapInitialFeeRatePerTrade = new HashMap<>();
			mapCurrInterestFlowPerTrade = new HashMap<>();
			tradesWithFlowsCalculated = new ArrayList<Long>();
		}
		final Trade trade = (Trade) Optional.ofNullable(row).map(r -> r.getProperty(ReportRow.TRADE)).orElse(null);
		Object result = null;

		if (TYPOLOGY.equalsIgnoreCase(columnId)) {
			result = getTypology(trade);
		} else if (FEE_BASIS.equalsIgnoreCase(columnId)) {
			result = getFeeBasis(row, errors, trade);
		} else if (FX_RATE.equalsIgnoreCase(columnId)) {
			result = getFXRate(row, errors, trade);
		} else if (CURRATE_FEE_RATE.equalsIgnoreCase(columnId)) {
			result = getCurrateFeeRate(row, errors, trade);
		} else if (INITIAL_FEE_RATE.equalsIgnoreCase(columnId)) {
			result = getInitialFeeRate(row, errors, trade);
		} else if (HAIRCUT.equals(columnId)) {
			result = getHaircut(row, errors, trade);
		} else if (INITIAL_NOMINAL.equals(columnId)) {
			result = getInitialNominal(row, errors, trade);
		} else if (CURRENT_NOMINAL.equals(columnId)) {
			result = getCurrentNominal(row, errors, trade);
		} else if (CASH_VALUE.equalsIgnoreCase(columnId)) {
			result = getCashValue(row, errors, trade);
		} else if (CASH_START.equalsIgnoreCase(columnId)) {
			result = getCashStart(row, errors, trade);
		} else if (DIRECTION.equalsIgnoreCase(columnId)) {
			result = getDirection(row, errors, trade);
		} else if (CASH_END.equalsIgnoreCase(columnId)) {
			result = getCashEnd(row, trade, errors);
		} else if (FACE_VALUE.equalsIgnoreCase(columnId)) {
			result = getFaceValue(row, errors, trade);
		} else if (BOND_COUPON.equalsIgnoreCase(columnId)) {
			result = getCoupon(row, errors, trade);
		} else if (BOND_MATURITY.equalsIgnoreCase(columnId)) {
			result = getMaturityDate(trade);
		} else if (LOAN_PRICE.equalsIgnoreCase(columnId)) {
			result = getLoanPrice(row, trade, errors);
		} else if (LOAN_VALUE.equals(columnId)) {
			result = getLoanValue(row, errors, trade);
		} else if (DIVIDEND_RATE.equals(columnId)) {
			result = getDividendRate(row, errors, trade);
		} else if (COLLATERAL_CCY.equals(columnId)) {
			result = getCollateralCcy(row, errors, trade);
		} else if (SETTLEMENT_CCY.equals(columnId)) {
			result = getSettlementCcy(row, errors, trade);
		} else if (MC_CONTRACT_ID.equals(columnId)) {
			result = getMcContractId(row, errors, trade);
		} else if (MC_CONTRACT_DESCR.equals(columnId)) {
			result = getMcContractDescr(row, errors, trade);
		} else {
			result = formatValue(super.getColumnValue(row, columnId, errors));
		}

		return result;
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		final String tradeCustomTree = "Trade Custom";

		treeList.add(tradeCustomTree, TYPOLOGY);
		treeList.add(tradeCustomTree, FEE_BASIS);
		treeList.add(tradeCustomTree, FX_RATE);
		treeList.add(tradeCustomTree, CURRATE_FEE_RATE);
		treeList.add(tradeCustomTree, INITIAL_FEE_RATE);
		treeList.add(tradeCustomTree, CASH_VALUE);
		treeList.add(tradeCustomTree, CASH_START);
		treeList.add(tradeCustomTree, LOAN_PRICE);
		treeList.add(tradeCustomTree, CASH_END);
		treeList.add(tradeCustomTree, FACE_VALUE);
		treeList.add(tradeCustomTree, BOND_COUPON);
		treeList.add(tradeCustomTree, BOND_MATURITY);
		treeList.add(tradeCustomTree, LOAN_VALUE);
		treeList.add(tradeCustomTree, HAIRCUT);
		treeList.add(tradeCustomTree, INITIAL_NOMINAL);
		treeList.add(tradeCustomTree, CURRENT_NOMINAL);
		treeList.add(tradeCustomTree, DIVIDEND_RATE);
		treeList.add(tradeCustomTree, COLLATERAL_CCY);
		treeList.add(tradeCustomTree, SETTLEMENT_CCY);
		treeList.add(tradeCustomTree, MC_CONTRACT_ID);
		treeList.add(tradeCustomTree, MC_CONTRACT_DESCR);

		return treeList;
	}

	/**
	 * Format double, integer and date values
	 *
	 * @param obj
	 * @return
	 */
	private String formatValue(Object obj) {
		if (obj != null) {
			if (obj instanceof Integer) {
				final DecimalFormat df = new DecimalFormat("#,##0.0");
				obj = df.format(obj);
			} else if (obj instanceof Double) {
				final DecimalFormat df = new DecimalFormat("#,##0.0000");
				obj = df.format(obj);
			}
			if (obj instanceof JDatetime) {
				final SimpleDateFormat formatoSalida = new SimpleDateFormat("dd/MM/yyyy");
				obj = formatoSalida.format(obj);
			}
			String objStr = obj.toString();
			objStr = objStr.replace("(", "-").replace(")", "").replace(".", "").replace(",", ".");
			return objStr;

		} else {
			return "";
		}
	}

	/**
	 * Gets the typology of the trade depending on ProductType and ProductSubtype
	 *
	 * @param row
	 * @return
	 */

	private String getTypology(final Trade trade) {
		String typology = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo) {
				if (!TRIPARTY.equalsIgnoreCase(trade.getProductSubType())) {
					typology = formatValue(trade.getProductSubType());
				}
			} else if (product instanceof SecLending) {
				if (((SecLending) product).getSecurity() instanceof Equity) {
					typology = formatValue("PdV_EQ");
				} else if (((SecLending) product).getSecurity() instanceof Bond) {
					typology = formatValue("PdV_FI");
				}
			}
		}

		return typology;
	}

	/**
	 * Gets the FeeBasis which is Cash.Daycount for repos and Fee.Daycount for
	 * SecLending
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getFeeBasis(ReportRow row, Vector errors, final Trade trade) {
		String feeeBasis = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				feeeBasis = formatValue(super.getColumnValue(row, "Cash. DayCount", errors));
			} else if (trade.getProduct() instanceof SecLending) {
				feeeBasis = formatValue(super.getColumnValue(row, "Fee. DayCount", errors));
			}
		}

		return feeeBasis;
	}

	/**
	 * Gets the FXRate which is FX Rate for repos and Fee.FxRate for SecLending
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getFXRate(ReportRow row, Vector errors, final Trade trade) {
		String fxRate = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo || product instanceof SecLending) {
				fxRate = formatValue(super.getColumnValue(row, "FX Rate", errors));
			}
		}

		return fxRate;
	}

	/**
	 * Gets the Current Fixed Rate for Repo differentiating whether the trade is
	 * fixed or floating and the Fee. Rate (Current) for SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCurrateFeeRate(ReportRow row, Vector errors, final Trade trade) {
		String currFeeRate = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				final Repo repo = (Repo) trade.getProduct();
				final Cash cash = repo.getCash();
				if (cash != null) {
					if (FIXED.equalsIgnoreCase(cash.getRateType())) {
						currFeeRate = formatValue(super.getColumnValue(row, "Current Fixed Rate", errors));
					} else if (FLOATING.equalsIgnoreCase(cash.getRateType())) {
						currFeeRate = formatValue(
								getCurrentRate(trade, (Repo) trade.getProduct()) * 100 + repo.getSpread() * 10000);
					}
				}
			} else if (trade.getProduct() instanceof SecLending) {
				currFeeRate = formatValue(super.getColumnValue(row, "Fee. Rate (Current)", errors));
			}
		}

		return currFeeRate;
	}

	/**
	 * Get Current Fixed Rate for Repos and the Fee. Rate (Initial) for SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getInitialFeeRate(ReportRow row, Vector errors, final Trade trade) {
		String initialFeeRate = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				final Repo repo = (Repo) trade.getProduct();
				final Cash cash = repo.getCash();
				if (cash != null) {
					if (FIXED.equalsIgnoreCase((cash.getRateType()))) {
						initialFeeRate = formatValue(super.getColumnValue(row, "Fixed Rate", errors));
					} else if (FLOATING.equalsIgnoreCase(cash.getRateType())) {
						if (mapInitialFeeRatePerTrade.containsKey(trade.getLongId())) {
							initialFeeRate = mapInitialFeeRatePerTrade.get(trade.getLongId());
						} else {
							initialFeeRate = getInitialFeeRatePerFloatingRepo(trade, cash);
							mapInitialFeeRatePerTrade.put(trade.getLongId(), initialFeeRate);
						}
					}
				}
			} else if (trade.getProduct() instanceof SecLending) {
				initialFeeRate = formatValue(super.getColumnValue(row, "Fee. Rate (Initial)", errors));
			}
		}

		return initialFeeRate;
	}

	private String getInitialFeeRatePerFloatingRepo(Trade trade, Cash cash) {
		String initialFeeRate = null;
		final Repo repo = (Repo) trade.getProduct();

		if (!tradesWithFlowsCalculated.contains(trade.getLongId())) {
			final PricingEnv pricingEnv = PricingEnv.loadPE(DIRTY_PRICE, valueDate.getJDatetime());
			final CashFlowSet cashFlowSetAux = getCashFlowsOfRepoByDate(repo, valueDate, trade.getLongId());
			calculateCashFlows(cashFlowSetAux, pricingEnv, valueDate, trade.getLongId());
		}
		final CashFlowSet cashFlowSet = cash.getFlows();
		if (cashFlowSet != null) {
			for (int i = 0; i < cashFlowSet.size(); i++) {
				final CashFlow cashFlow = cashFlowSet.get(i);
				if (INTEREST.equalsIgnoreCase(cashFlow.getType())) {
					initialFeeRate = formatValue(
							((CashFlowInterest) cashFlow).getRate() * 100 + repo.getSpread() * 10000);
					break;
				}
			}
		}

		return initialFeeRate;

	}

	/**
	 * Get initial nominal (Sec. Nominal (Initial))
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getInitialNominal(ReportRow row, Vector errors, final Trade trade) {
		String initialNominal = null;

		if (trade != null) {
			initialNominal = formatValue(super.getColumnValue(row, "Sec. Nominal (Initial)", errors));
		}

		return initialNominal;
	}

	/**
	 * Gets the haircut for Repos and SecLendings
	 *
	 * @param trade
	 * @return
	 */

	private String getHaircut(ReportRow row, Vector errors, final Trade trade) {
		String haircut = null;

		haircut = formatValue(super.getColumnValue(row, "Sec. Margin Value", errors));

		return haircut;
	}

	/**
	 * Get current nominal, Sec. Nominal (Last) for Repos and Sec. Nominal (Current)
	 * for SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCurrentNominal(ReportRow row, Vector errors, final Trade trade) {
		String currentNominal = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				final String capitalFactor = trade.getKeywordValue(CAPITAL_FACTOR);
				if (Util.isEmpty(capitalFactor) || Double.valueOf(capitalFactor) >= 1) {
					currentNominal = formatValue(super.getColumnValue(row, "Sec. Nominal (Last)", errors));
				} else {
					final Object secValue = getSecNominalValue(trade);
					currentNominal = formatValue(secValue);
				}
			} else if (trade.getProduct() instanceof SecLending) {
				if (!mapCurrNominalPerTrade.containsKey(trade.getLongId())) {
					final TransferArray transfers = getTransfers(trade);
					if (transfers != null) {
						currentNominal = formatValue(getcurrentNominal(transfers));
						mapCurrNominalPerTrade.put(trade.getLongId(), currentNominal);
					}
				} else {
					currentNominal = mapCurrNominalPerTrade.get(trade.getLongId());
				}
			}
		}

		return currentNominal;
	}

	/**
	 * Gets the Sec. Nominal of the Repo using its ExternalSecFinanceTradeEntry
	 *
	 * @param trade
	 * @return
	 */
	private Object getSecNominalValue(Trade trade) {
		final SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();
		final SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade,
				null, context);
		final Object secValue = Optional.ofNullable(externalSecFinanceTradeEntry.get("Sec. Nominal"))
				.map(FieldEntry::getValue).orElse("");

		return secValue;
	}

	/**
	 * Get Cash Value for trades. For SecLendings it corresponds to the cashValue *
	 * nominal, for Repo is null
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCashValue(ReportRow row, Vector errors, final Trade trade) {
		String cashValue = null;

		if (trade != null) {
			if (trade.getProduct() instanceof SecLending) {
				final BOTransfer xfer = (BOTransfer) Optional.ofNullable(row)
						.map(r -> r.getProperty(ReportRow.TRANSFER)).orElse(null);
				if (xfer != null) {
					cashValue = formatValue(xfer.getOtherAmount());
				}
			}
		}

		return cashValue;
	}

	/**
	 * Gets the Loan Value, for Repo is null
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getLoanValue(ReportRow row, Vector errors, final Trade trade) {
		String loanValue = null;

		if (trade != null) {
			if (trade.getProduct() instanceof SecLending) {
				loanValue = formatValue(super.getColumnValue(row, "Fee. Value(Current)", errors));
			}
		}

		return loanValue;
	}

	/**
	 * Get Cash Value for trades. For Repo it corresponds to the Principal Amount
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCashStart(ReportRow row, Vector errors, final Trade trade) {
		String cashStart = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				cashStart = formatValue(super.getColumnValue(row, "Principal Amount", errors));
			}
		}

		return cashStart;
	}

	/**
	 * Gets the direction of the trade. Direction for Repos and BuyShell for
	 * SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getDirection(ReportRow row, Vector errors, final Trade trade) {
		String direction = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo) {
				if (!TRIPARTY.equalsIgnoreCase(trade.getProductSubType())) {
					if (BSB.equalsIgnoreCase(trade.getProductSubType())) {
						direction = formatValue(super.getColumnValue(row, "Buy/Sell", errors));
					} else if (STANDARD.equalsIgnoreCase(trade.getProductSubType())) {
						direction = formatValue(super.getColumnValue(row, "Direction", errors));
						if (direction.equals("Repo")) {
							direction = "Sell";
						} else if (direction.equals("Reverse")) {
							direction = "Buy";
						}
					}
				}
			} else if (product instanceof SecLending) {
				direction = formatValue(super.getColumnValue(row, "Direction", errors));
				if (direction.equals("Sec. Lending")) {
					direction = "Sell";
				} else if (direction.equals("Sec. Borrowing")) {
					direction = "Buy";
				}
			}
		}

		return direction;
	}

	/**
	 * Gets the CashEnd por Repo, for Sec Lending is null
	 *
	 * @param row
	 * @param trade
	 * @param errors
	 * @return
	 */
	private String getCashEnd(ReportRow row, final Trade trade, Vector errors) {
		String endTotal = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				if (mapCashEndPerTrade.containsKey(trade.getLongId())) {
					endTotal = mapCashEndPerTrade.get(trade.getLongId());
				} else {
					endTotal = formatValue(getCashEndRepo(row, trade, errors));
					mapCashEndPerTrade.put(trade.getLongId(), endTotal);
				}
			}
		}

		return endTotal;
	}

	/**
	 * Get End Total from Repo End Total Panel
	 *
	 * @param row
	 * @return
	 */
	private Object getCashEndRepo(ReportRow row, final Trade trade, Vector errors) {
		Double endTotalRepo = 0.0;
		Double interestAcum = 0.0;
		final Repo repo = (Repo) trade.getProduct();

		if (!tradesWithFlowsCalculated.contains(trade.getLongId())) {
			final PricingEnv pricingEnv = PricingEnv.loadPE(DIRTY_PRICE, valueDate.getJDatetime());
			final CashFlowSet cashFlowSetAux = repo.getFlows();
			calculateCashFlows(cashFlowSetAux, pricingEnv, valueDate, trade.getLongId());
		}
		final Cash cash = repo.getCash();
		if (cash != null) {
			final CashFlowSet cashFlowSet = cash.getFlows();
			if (cashFlowSet != null) {
				for (final CashFlow flow : cashFlowSet) {
					if (INTEREST.equalsIgnoreCase(flow.getType()) && (flow.getEndDate().lte(valueDate)
							|| (flow.getStartDate().lte(valueDate) && flow.getEndDate().gte(valueDate)))) {
						if (flow.getCollateralId() == 0) {
							interestAcum += flow.getAmount();
						}
					}
				}
			}
		}
		endTotalRepo = interestAcum + repo.getPrincipal();

		return endTotalRepo;
	}

	/**
	 * Gets the Collateral Currency, sec. Redeem ccy for Repos and margin call
	 * contract. Ccy for SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCollateralCcy(ReportRow row, Vector errors, final Trade trade) {
		String collateralCcy = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo) {
				final Object bond = super.getColumnValue(row, "Sec. Product", errors);
				if (bond != null && bond instanceof Bond) {
					collateralCcy = ((Bond) bond).getRedemCurrency();
				}
			} else if (product instanceof SecLending) {
				int contractId = 0;
				contractId = ((SecLending) product).getMarginCallContractId(trade);
				if (contractId != 0) {
					final CollateralConfig colConfig = getMCContract(contractId);
					if (colConfig != null) {
						collateralCcy = colConfig.getCurrency();
					}
				}
			}
		}

		return collateralCcy;
	}

	/**
	 * Gets the Settlement Currency of the given trade
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getSettlementCcy(ReportRow row, Vector errors, Trade trade) {
		String settlementCcy = null;

		if (trade.getProduct() instanceof Repo || (trade.getProduct() instanceof SecLending
				&& ((SecLending) trade.getProduct()).getSecurity() instanceof Bond)) {
			final Object secProduct = super.getColumnValue(row, "Sec. Product", errors);
			if (secProduct instanceof Bond) {
				settlementCcy = ((Bond) secProduct).getRedemCurrency();
			}
		} else if ((trade.getProduct() instanceof SecLending
				&& ((SecLending) trade.getProduct()).getSecurity() instanceof Equity)) {
			final Object secProduct = super.getColumnValue(row, "Sec. Product", errors);
			if (secProduct instanceof Equity) {
				settlementCcy = ((Equity) secProduct).getCurrency();
			}
		}

		return settlementCcy;
	}

	/**
	 * Gets the MC Contract ID
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getMcContractId(ReportRow row, Vector errors, Trade trade) {
		String contractId = null;
		final Product product = trade.getProduct();

		if (product instanceof Repo) {
			contractId = Integer.toString(((Repo) product).getMarginCallContractId(trade));
		} else if (product instanceof SecLending) {
			contractId = Integer.toString(((SecLending) product).getMarginCallContractId(trade));
		}

		return contractId;
	}

	/**
	 * Gets the MC Contract Description
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getMcContractDescr(ReportRow row, Vector errors, Trade trade) {
		String contractDescr = null;
		final Product product = trade.getProduct();
		int contractId = 0;

		if (product instanceof Repo) {
			contractId = ((Repo) product).getMarginCallContractId(trade);
		} else if (product instanceof SecLending) {
			contractId = ((SecLending) product).getMarginCallContractId(trade);
		}
		if (contractId != 0) {
			final CollateralConfig collateralConfig = getMCContract(contractId);
			if (collateralConfig != null) {
				contractDescr = collateralConfig.getName();
			}
		}

		return contractDescr;
	}

	/**
	 * Gets the CashFlows of a Repo by date
	 *
	 * @param repo
	 * @param jDate
	 * @param tradeId
	 * @return
	 */
	protected CashFlowSet getCashFlowsOfRepoByDate(final Repo repo, final JDate jDate, final long tradeId) {
		CashFlowSet cashFlowSet = null;

		try {
			cashFlowSet = repo.getFlows(jDate);
		} catch (final FlowGenerationException exception) {
			Log.error(this, "Failed to generate the CashFlows " + "of the repo trade with id = " + tradeId, exception);
		}

		return cashFlowSet;
	}

	/**
	 * Gets the CashFlows of a Repo by date
	 *
	 * @param repo
	 * @param jDate
	 * @param tradeId
	 * @return
	 */
	protected CashFlowSet getCashFlowsOfSecLendingByDate(final SecLending secLending, final JDate jDate,
			final long tradeId) {
		CashFlowSet cashFlowSet = null;

		try {
			cashFlowSet = secLending.getFlows(jDate);
		} catch (final FlowGenerationException exception) {
			Log.error(this, "Failed to generate the CashFlows " + "of the repo trade with id = " + tradeId, exception);
		}

		return cashFlowSet;
	}

	/**
	 * Calculates the CashFlows of a Trade
	 *
	 * @param cashFlowSet
	 * @param pricingEnv
	 * @param jDate
	 * @param tradeId
	 */
	protected void calculateCashFlows(CashFlowSet cashFlowSet, final PricingEnv pricingEnv, final JDate jDate,
			final long tradeId) {
		if (cashFlowSet != null && pricingEnv != null && jDate != null
				&& pricingEnv.getQuoteSet() != null) {
			final QuoteSet quoteSet = pricingEnv.getQuoteSet();
			try {
				cashFlowSet.calculate(quoteSet, jDate);
			} catch (final FlowGenerationException exception) {
				Log.error(this, "The calculation of the CashFlows of the repo trade with id = " + tradeId
						+ " could not be performed", exception);
			}
			tradesWithFlowsCalculated.add(tradeId);
		}

	}

	/**
	 * Gets the Face Value from Repos (Face Value) and SecLendings (Trading Size)
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getFaceValue(ReportRow row, Vector errors, final Trade trade) {
		String faceValue = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo || ((SecLending) product).getSecurity() instanceof Bond) {
				faceValue = formatValue(super.getColumnValue(row, "Face Value", errors));
			} else if (((SecLending) product).getSecurity() instanceof Equity) {
				faceValue = formatValue(super.getColumnValue(row, "Trading Size", errors));
			}
		}

		return faceValue;
	}

	/**
	 * Gets the Coupon from the given trade
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getCoupon(ReportRow row, Vector errors, final Trade trade) {
		String bondCoupon = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo
					|| (product instanceof SecLending && ((SecLending) product).getSecurity() instanceof Bond)) {
				bondCoupon = formatValue(super.getColumnValue(row, "Coupon", errors));
			}
		}

		return bondCoupon;
	}

	/**
	 * Gets the maturity date of the Repo Underlying
	 *
	 * @param trade
	 * @return
	 */
	private String getMaturityDate(final Trade trade) {
		String maturityDate = null;

		if (trade != null) {
			final Product product = trade.getProduct();
			if (product instanceof Repo
					|| (product instanceof SecLending && ((SecLending) product).getSecurity() instanceof Bond)) {
				final Bond bond = getRemoteProductById(product.getUnderlyingSecurityId());
				if (bond != null) {
					maturityDate = formatValue(bond.getMaturityDate());
				}
			}
		}

		return maturityDate;
	}

	/**
	 * Gets Remote Product by Id
	 *
	 * @param productId
	 * @return
	 */
	protected Bond getRemoteProductById(final int productId) {
		Bond bond = null;

		if (mapBondProductById.get(productId) != null) {
			bond = (Bond) mapBondProductById.get(productId);
		} else {
			try {
				final Product product = DSConnection.getDefault().getRemoteProduct().getProduct(productId);
				if (product instanceof Bond) {
					bond = (Bond) DSConnection.getDefault().getRemoteProduct().getProduct(productId);
					mapBondProductById.put(productId, bond);
				}
			} catch (final CalypsoServiceException calypsoServiceException) {
				Log.error(this, "Error getting Remote Product by id with the ID: " + productId,
						calypsoServiceException);

			}
		}

		return bond;
	}

	/**
	 * Getting Loan Price for Repos and SecLendings
	 *
	 * @param row
	 * @param trade
	 * @param errors
	 * @return
	 */
	private String getLoanPrice(ReportRow row, final Trade trade, Vector errors) {
		String loanPrice = null;

		if (trade != null) {
			if (trade.getProduct() instanceof Repo) {
				loanPrice = formatValue(getQuoteFromRepo(trade, DIRTY_PRICE));
			} else if (trade.getProduct() instanceof SecLending) {
				final SecLending lending = (SecLending) trade.getProduct();
				if (AUTO.equals(lending.getMarkProcedure())) {
					String quoteSetNameAux = "";
					if (lending.getSecurity() instanceof Bond) {
						quoteSetNameAux = DIRTY_PRICE;
					} else if (lending.getSecurity() instanceof Equity) {
						quoteSetNameAux = OFFICIAL;
					}
					if (!Util.isEmpty(quoteSetNameAux)) {
						loanPrice = formatValue(getQuoteFromSecurityLending(trade, quoteSetNameAux));
					}
				} else if (Util.isEmpty(lending.getMarkProcedure())) {
					if (lending.getSecurity() instanceof Bond) {
						loanPrice = formatValue(trade.getKeywordValue(MX_INITIAL_DIRTY_PRICE));
					} else if (lending.getSecurity() instanceof Equity) {
						loanPrice = formatValue(trade.getKeywordValue(MX_INITIAL_EQUITY_PRICE));
					}
				}
			}
		}

		return loanPrice;
	}

	private double getQuoteFromRepo(Trade trade, String quoteSetName) {
		final Repo repo = (Repo) trade.getProduct();
		final String quoteName = Optional.ofNullable(repo.getSecurity()).map(Product::getQuoteName).orElse("");

		return getQuoteFromQuoteSet(quoteName, quoteSetName);
	}

	private double getQuoteFromSecurityLending(Trade trade, String quoteSetName) {
		final SecLending lending = (SecLending) trade.getProduct();
		final String quoteName = Optional.ofNullable(lending.getSecurity()).map(Product::getQuoteName).orElse("");

		return getQuoteFromQuoteSet(quoteName, quoteSetName);
	}

	/**
	 * Gets the closest quote from the valueDate *100
	 *
	 * @param quoteName
	 * @param quoteSetName
	 * @return
	 */
	private double getQuoteFromQuoteSet(String quoteName, String quoteSetName) {
		double quoteClosestValue = 0.0d;

		try {
			final QuoteValue q = new QuoteValue();
			q.setQuoteSetName(quoteSetName);
			q.setName(quoteName);
			q.setDatetime(valueDate.getJDatetime());
			final QuoteValue lastQuoteValue = DSConnection.getDefault().getRemoteMarketData().getLatestQuoteValue(q);
			if (lastQuoteValue != null) {
				quoteClosestValue = lastQuoteValue.getClose();
			}
		} catch (final CalypsoServiceException exc) {
			Log.error(exc, "Getting the last quote on the quoteName " + quoteName);
		}

		return quoteClosestValue * 100;
	}

	/**
	 * Gets the Dividend Rate of the Equity SecLendings
	 *
	 * @param row
	 * @param errors
	 * @param trade
	 * @return
	 */
	private String getDividendRate(ReportRow row, Vector errors, final Trade trade) {
		String dividendRate = null;

		if (trade != null) {
			if (trade.getProduct() instanceof SecLending) {
				final SecLending secLending = (SecLending) trade.getProduct();
				if (secLending.getSecurity() instanceof Equity) {
					final Equity equity = (Equity) secLending.getSecurity();
					final Vector<Dividend> dividends = equity.getDividends();
					if (dividends != null) {
						dividendRate = getMostRecentDividend(dividends);
					}
				}
			}
		}

		return dividendRate;
	}

	/**
	 * Gets the closest dividend to the valueDate
	 *
	 * @param dividends
	 * @return
	 */
	private String getMostRecentDividend(Vector<Dividend> dividends) {
		String dividendRate = null;
		JDate auxPaymentDate = null;
		Dividend auxDividend = null;

		for (final Dividend dividend : dividends) {
			if (valueDate.gte(dividend.getExDividendDate()) && valueDate.lte(dividend.getPaymentDate())) {
				if (auxPaymentDate == null) {
					auxPaymentDate = dividend.getPaymentDate();
					auxDividend = dividend;
				} else {
					if (dividend.getPaymentDate().before(auxPaymentDate)) {
						// Gets the paymentDate closest to the valuateDate
						auxPaymentDate = dividend.getPaymentDate();
						auxDividend = dividend;
					}
				}
			}
		}
		if (auxDividend != null) {
			dividendRate = formatValue(auxDividend.getAmount());
		}

		return dividendRate;
	}

	public CollateralConfig getMCContract(int id) {
		CollateralConfig cc = null;

		if (id > 0) {
			try {
				cc = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(id);
			} catch (final CollateralServiceException exc) {
				Log.error("Error retrieving contract", exc);
			}
		}

		return cc;
	}

	protected double getCurrentRate(Trade trade, Repo repo) {
		CashFlow flow = null;
		if (mapCurrInterestFlowPerTrade.containsKey(trade.getLongId())) {
			flow = mapCurrInterestFlowPerTrade.get(trade.getLongId());
		} else {
			flow = getCurrentInterestFlow(trade, repo);
			mapCurrInterestFlowPerTrade.put(trade.getLongId(), flow);
		}

		double rate = 0.0D;
		if (flow instanceof CashFlowInterest) {
			rate = ((CashFlowInterest) flow).getRate();
		}

		return rate;
	}

	/**
	 * Gets the current interest flow as a function of the valueDate
	 *
	 * @param trade
	 * @param repo
	 * @return
	 */
	private CashFlow getCurrentInterestFlow(Trade trade, Repo repo) {
		CashFlow flow = null;

		if (!tradesWithFlowsCalculated.contains(trade.getLongId())) {
			final PricingEnv pricingEnv = PricingEnv.loadPE(DIRTY_PRICE, valueDate.getJDatetime());
			final CashFlowSet cashFlowSetAux = getCashFlowsOfRepoByDate(repo, valueDate, trade.getLongId());
			calculateCashFlows(cashFlowSetAux, pricingEnv, valueDate, trade.getLongId());
		}
		final Cash cash = repo.getCash();
		if (cash != null) {
			final CashFlowSet cashFlowSet = cash.getFlows();
			if (cashFlowSet != null && !cashFlowSet.isEmpty()) {
				flow = cashFlowSet.findEnclosingCashFlow(valueDate, CashFlow.INTEREST);
			}
		}

		return flow;
	}

	/**
	 * Gets from BD the xfers that meet the query
	 *
	 * @param trade
	 * @return
	 */
	private TransferArray getTransfers(Trade trade) {
		TransferArray transfers = null;
		final Vector<String> acceptedTypes = getCurrNominalDomainValues(TYPES);
		final Vector<String> acceptedStatus = getCurrNominalDomainValues(STATUS);

		try {
			final StringBuilder where = new StringBuilder();
			where.append(" bo_transfer.trade_id = ").append(trade.getLongId());
			where.append(" AND bo_transfer.value_date <= ").append(Util.date2SQLString(valueDate));
			where.append(" AND bo_transfer.transfer_status IN ('").append(StringUtils.join(acceptedStatus, "', '"))
			.append("')");
			where.append(" AND bo_transfer.transfer_type IN ('").append(StringUtils.join(acceptedTypes, "', '"))
			.append("')");
			transfers = DSConnection.getDefault().getRemoteBO().getTransfers(null, where.toString(), null);
		} catch (final CalypsoServiceException e) {
			Log.error(e, "An error ocurred getting the xfers of the trade wiith TradeID = " + trade.getLongId());
		}

		return transfers;
	}

	/**
	 * Gets the accepted state or type values of the domainValues depending on which
	 * variable arrives by value. If it has no value it uses the constant list
	 * ACCEPTED_+(Status or Types).
	 *
	 * @param statusOrType
	 * @return
	 */
	private Vector<String> getCurrNominalDomainValues(String statusOrType) {
		final Vector<String> acceptedTypesOrStatus = LocalCache.getDomainValues(DSConnection.getDefault(),
				"PirumCurrNominalAccepted" + statusOrType);

		if (Util.isEmpty(acceptedTypesOrStatus)) {
			String[] aux;
			if ("STATUS".equalsIgnoreCase(statusOrType)) {
				aux = ACCEPTED_STATUS;
			} else {
				aux = ACCEPTED_TYPES;
			}
			for (final String s : aux) {
				acceptedTypesOrStatus.add(s);
			}
		}

		return acceptedTypesOrStatus;
	}

	/**
	 * Obtains the cumulative CURRENT_NOMINAL of the xfers
	 *
	 * @param transfers
	 * @return
	 */
	private Double getcurrentNominal(TransferArray transfers) {
		Double currNominal = 0.0;

		for (final BOTransfer xfer : transfers) {
			if (xfer.isPayment()) {
				if ((xfer.getSettleDate().equals(valueDate) && Status.VERIFIED.equals(xfer.getStatus().getStatus()))
						|| Status.SETTLED.equals(xfer.getStatus().getStatus())) {
					final Double nominalAmount = Math.abs(xfer.getNominalAmount());
					if(PAY.equalsIgnoreCase(xfer.getPayReceiveType())) {
						currNominal += nominalAmount * (-1);
					} else if(RECEIVE.equalsIgnoreCase(xfer.getPayReceiveType())) {
						currNominal += nominalAmount;
					}
				}
			}
		}

		return currNominal;
	}
}
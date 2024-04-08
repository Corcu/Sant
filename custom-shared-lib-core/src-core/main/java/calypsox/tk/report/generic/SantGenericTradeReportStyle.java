package calypsox.tk.report.generic;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.DisplayDate;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.PLMarkReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.InstantiateUtil;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.SantTradeBrowserReportTemplate;
import calypsox.tk.report.generic.columns.SantColateralColumn;
import calypsox.tk.report.generic.columns.SantPricerMeasureBaseValueColumn;
import calypsox.tk.report.generic.columns.SantPricerMeasureValueColumn;
import calypsox.tk.report.generic.columns.SantValuationAgentColumn;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.util.SantReportingUtil;

public abstract class SantGenericTradeReportStyle extends ReportStyle {

	private static final long serialVersionUID = 9199798746171036578L;

	public static final String QUOTES_LOADER = "QuotesLoader";
	public static final String CLOSE_OF_BUSINESS = "Close of Business";
	public static final String AGREEMENT_NAME = "Agreement name";
	public static final String AGREEMENT_ID = "Contract Id";
	public static final String MARGINCALL_STATUS = "MarginCall status";
	public static final String AGREEMENT_STATUS = "Agreement status";
	public static final String VAL_AGENT = "Valuation agent";
	public static final String INSTRUMENT = "Instrument";
	public static final String INDEPENDENT_AMOUNT_BASE = "Indep. amount Base";
	public static final String INDEPENDENT_CCY_BASE = "Indep. amount Base CCY";
	public static final String INDEPENDENT_AMOUNT = "Indep. amount";
	public static final String INDEPENDENT_CCY = "Indep. amount CCY";
	public static final String INDEPENDENT_AMOUNT_PAY_RECEIVE = "Indep. amt Pay/Rec";
	public static final String INDEPENDENT_AMOUNT_BASE_PAY_RECEIVE = "Indep. amt Base Pay/Rec";
	public static final String MTM_BASE_VALUE = "MTM Base Value";
	public static final String MTM_BASE_CCY = "MTM Base CCY";
	public static final String MTM_CCY = "MTM CCY";
	public static final String MTM_VALUE = "MTM Value";
	public static final String PRINCIPAL_CCY = "Principal CCY";
	public static final String PRINCIPAL = "Principal";
	public static final String PRINCIPAL_2_CCY = "Principal 2 CCY";
	public static final String PRINCIPAL_2 = "Principal 2";
	public static final String CONTRACT_PO_SHORT_NAME = "Contract PO Short Name";
	public static final String INITIAL_COLLATERAL = "Initial Collateral";
	public static final String MTM_BASE_VALUE_FX_RATE = "MTM Base FX Rate";
	public static final String CLOSING_PRICE = "Closing Price";
	public static final String SANT_EXPOSURE = "Sant Exposure";
	public static final String UNDERLYING_1 = "Underlying_1";
	public static final String UNDERLYING_2 = "Underlying_2";
	//AAP MIG 14.4
	public static final String MATURITY_DATE="Maturity Date";

	// for RepoEnhacements
	public static final String REPO_ACCRUED_INTEREST = "Repo Accrued Interest";
	public static final String BOND_ACCRUED_INTEREST = "Bond Accrued Interest";
	public static final String CLEAN_PRICE = "Clean Price";
	public static final String CAPITAL_FACTOR = "Capital Factor";
	public static final String POOL_FACTOR = "Pool Factor";

	public static String[] DEFAULTS_COLUMNS = { AGREEMENT_NAME, CLOSE_OF_BUSINESS, VAL_AGENT, INSTRUMENT,
			CONTRACT_PO_SHORT_NAME };

	private static String[] twoLegs = { "CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP", "FX_SWAP_NON_DELIVERABLE",
			"FX_SWAP_DELIVERABLE", "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT", "FX_DELIVERABLE_FORWARD",
			"EQUITY_SWAP", "CURRENCY_SWAP", "BASIS_SWAP" };

	private static List<String> twoLegsUnderLyings = Arrays.asList(twoLegs);

	private TradeReportStyle tradeReportStyle;

	private CollateralConfigReportStyle marginCallConfigReportStyle;

	private PLMarkReportStyle plMarkReportStyle;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final CollateralConfig marginCallConfig = (CollateralConfig) row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
		final Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		final PLMark plMark = (PLMark) row.getProperty(ReportRow.PL_MARK);
		final JDate valDate = (JDate) row.getProperty(SantTradeBrowserReportTemplate.VAL_DATE);
		final SantGenericQuotesLoader quotesLoader = (SantGenericQuotesLoader) row.getProperty(QUOTES_LOADER);

		if (columnName.equals(CLOSE_OF_BUSINESS)) {
			return valDate;
		} else if (columnName.equals(VAL_AGENT)) {
			return new SantValuationAgentColumn(marginCallConfig).get();
		} else if (columnName.equals(AGREEMENT_NAME)) {
			if (marginCallConfig != null) {
				return marginCallConfig.getName();
			}
		} else if (columnName.equals(AGREEMENT_STATUS)) {
			if (marginCallConfig != null) {
				return marginCallConfig.getAgreementStatus();
			}
			// BAU GSM 28/05/2015 - Always shown 0 as contract id
		} else if (columnName.equals(AGREEMENT_ID)) {
			if (marginCallConfig != null) {
				return marginCallConfig.getId();
			}
			// BAU GSM 08/06/2015 - also missing MarginCallEntry
		} else if (columnName.equals(MARGINCALL_STATUS)) {
			if ((marginCallConfig != null) && (valDate != null)) {
				MarginCallEntryDTO mce = getMarginCallEntry(marginCallConfig, valDate);
				if (mce != null) {
					return mce.getStatus();
				}

			}

		} else if (columnName.equals(INSTRUMENT)) {
			if (trade != null) {
				if (trade.getProductType().equals(Product.REPO)) {
					return CollateralStaticAttributes.INSTRUMENT_TYPE_REPO;
				} else if (trade.getProductType().equals(Product.SEC_LENDING)) {
					return CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING;
				} else if(trade.getProductType().equals(Product.PERFORMANCESWAP)) {
					return CollateralStaticAttributes.INSTRUMENT_TYPE_PERFORMANCESWAP;
				}else{
					return trade.getProductSubType();
				}
			}
		}

		else if (columnName.equals(INDEPENDENT_AMOUNT)) {
			return getSantPricerMeasureValue(PricerMeasure.S_INDEPENDENT_AMOUNT, plMark).getAmount();
		}

		else if (columnName.equals(INDEPENDENT_CCY)) {
			return getSantPricerMeasureValue(PricerMeasure.S_INDEPENDENT_AMOUNT, plMark).getCcy();
		}

		else if (columnName.equals(INDEPENDENT_AMOUNT_BASE)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE, plMark).getAmount();
		}

		else if (columnName.equals(INDEPENDENT_CCY_BASE)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE, plMark).getCcy();
		}

		else if (columnName.equals(INDEPENDENT_AMOUNT_PAY_RECEIVE)) {
			final SignedAmount amount = (SignedAmount) getSantPricerMeasureValue(
					SantPricerMeasure.S_INDEPENDENT_AMOUNT, plMark).getAmount();
			return getIndAmtPayRecieve(amount);

		}

		else if (columnName.equals(INDEPENDENT_AMOUNT_BASE_PAY_RECEIVE)) {
			final SignedAmount amount = (SignedAmount) getSantPricerMeasureValue(
					SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE, plMark).getAmount();
			return getIndAmtPayRecieve(amount);
		}

		else if (columnName.equals(MTM_CCY)) {
			return getSantPricerMeasureValue(PricerMeasure.S_NPV, plMark).getCcy();
		}

		else if (columnName.equals(MTM_BASE_CCY)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_NPV_BASE, plMark).getCcy();
		}

		else if (columnName.equals(MTM_VALUE)) {
			return getSantPricerMeasureValue(PricerMeasure.S_NPV, plMark).getAmount();

		} else if (columnName.equals(SANT_EXPOSURE)) {
			final SignedAmount mtmValAmount = (SignedAmount) getSantPricerMeasureValue(PricerMeasure.S_NPV, plMark)
					.getAmount();
			if (mtmValAmount == null) {
				return null;
			}
			final double mtmVal = mtmValAmount.get();
			final SignedAmount initialColateral = (SignedAmount) getSantColateral(trade).getPrincipal();

			return new SignedAmount(mtmVal - initialColateral.get(), initialColateral.getDigit());
		}

		else if (columnName.equals(PRINCIPAL)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if (product instanceof CollateralExposure) {
					if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
						return getTradeReportStyle().getColumnValue(row, "#####.NOMINAL_1", errors);
					} else {
						final CollateralExposure collat = (CollateralExposure) product;
						return new SignedAmount(collat.getPrincipal(), CurrencyUtil.getRoundingUnit(collat
								.getCurrency()));
					}
				} else {
					return getSantColateral(trade).getPrincipal();
				}

			}
		} else if (columnName.equals(PRINCIPAL_CCY)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if ((product instanceof CollateralExposure)
						&& is2Legs(((CollateralExposure) product).getUnderlyingType())) {
					return getTradeReportStyle().getColumnValue(row, "#####.CCY_1", errors);
				}
				// Other products
				return trade.getProduct().getCurrency();

			}

		} else if (columnName.equals(PRINCIPAL_2)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if ((product instanceof CollateralExposure)
						&& is2Legs(((CollateralExposure) product).getUnderlyingType())) {
					return getTradeReportStyle().getColumnValue(row, "#####.NOMINAL_2", errors);
				}
				return null;
			}
		} else if (columnName.equals(PRINCIPAL_2_CCY)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if ((product instanceof CollateralExposure)
						&& is2Legs(((CollateralExposure) product).getUnderlyingType())) {
					return getTradeReportStyle().getColumnValue(row, "#####.CCY_2", errors);
				}
				return null;
			}
		} else if (columnName.equals(MTM_BASE_VALUE)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_NPV_BASE, plMark).getAmount();
		}

		else if (columnName.equals(MTM_BASE_VALUE_FX_RATE)) {

			return getSantPricerMeasureBaseValue(PricerMeasure.S_NPV, plMark, marginCallConfig, quotesLoader)
					.getFXRate();
		}

		else if (columnName.equals(CONTRACT_PO_SHORT_NAME)) {
			if (marginCallConfig != null) {
				return marginCallConfig.getProcessingOrg().getCode();
			}
		} else if (columnName.equals(INITIAL_COLLATERAL)) {
			return getSantColateral(trade).getInitialColateral();
		} else if (columnName.equals(UNDERLYING_1)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if ((product instanceof CollateralExposure)) {
					if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
						return getTradeReportStyle().getColumnValue(row, "#####.UNDERLYING_1", errors);
					} else {
						return getTradeReportStyle().getColumnValue(row, "#####.UNDERLYING", errors);
					}
				}
				// Other
				return getSantColateral(trade).getUnderLyingDescrition();
			}
		} else if (columnName.equals(UNDERLYING_2)) {
			if (trade != null) {
				final Product product = trade.getProduct();
				if ((product instanceof CollateralExposure)) {
					if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
						return getTradeReportStyle().getColumnValue(row, "BASIS_SWAP.UNDERLYING_2", errors);
					}
				}
				// Other
				return null;
			}
		} else if (columnName.equals(CLOSING_PRICE)) {
			final SantColateralColumn scc = getSantColateral(trade);

			final QuoteValue qv = quotesLoader.fetchQuoteValue(scc.getUnderlyingQuoteName());
			if (qv != null) {
				return new Amount(qv.getClose());
			}
			return null;

			// for RepoEnhacements
		} else if (columnName.equals(REPO_ACCRUED_INTEREST)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_REPO_ACCRUED_INTEREST, plMark).getAmount();
		} else if (columnName.equals(BOND_ACCRUED_INTEREST)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_BOND_ACCRUED_INTEREST, plMark).getAmount();
		} else if (columnName.equals(CLEAN_PRICE)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_CLEAN_PRICE, plMark).getAmount();
		} else if (columnName.equals(CAPITAL_FACTOR)) {
			return getSantPricerMeasureValue(SantPricerMeasure.S_CAPITAL_FACTOR, plMark).getAmount();
		} else if (columnName.equals(POOL_FACTOR)) {
			return getPoolFactor(trade, valDate);
		} else if (columnName.equals(MATURITY_DATE)) {
			return (Object) getTradeMaturityDate(row.getProperty(ReportRow.TRADE));
		}

		else {
			Object retVal = null;

			if (trade != null) {
				// Specific to trade date to remove timestamp
				if (TradeReportStyle.TRADE_DATE.equals(columnName)) {
					if (trade.getTradeDate() == null) {
						return null;
					}
					retVal = new DisplayDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
				} else {
					retVal = getTradeReportStyle().getColumnValue(row, columnName, errors);
				}
			}
			if ((retVal == null) && (marginCallConfig != null)) {
				retVal = getCollateralConfigReportStyle().getColumnValue(row, columnName, errors);
			}
			if ((retVal == null) && (plMark != null)) {
				retVal = getPLMarkReportStyle().getColumnValue(row, columnName, errors);
			}

			return retVal;
		}
		return null;

	}

	private SantColateralColumn getSantColateral(final Trade trade) {
		return new SantColateralColumn(trade);
	}

	private SantPricerMeasureValueColumn getSantPricerMeasureValue(final String pricerMeasure, final PLMark plMark) {
		return new SantPricerMeasureValueColumn(plMark, pricerMeasure);
	}

	private SantPricerMeasureBaseValueColumn getSantPricerMeasureBaseValue(final String pricerMeasure,
			final PLMark plMark, final CollateralConfig marginCallConfig, final SantGenericQuotesLoader quotesLoader) {
		return new SantPricerMeasureBaseValueColumn(plMark, marginCallConfig, pricerMeasure, quotesLoader);
	}

	private boolean is2Legs(final String underlying) {
		return twoLegsUnderLyings.contains(underlying);

	}

	private double getPoolFactor(Trade trade, JDate date) {
		if (Product.REPO.equals(trade.getProductType())) {
			Repo repo = (Repo) trade.getProduct();
			if (repo != null) {
				Product p = BOCache
						.getExchangedTradedProduct(DSConnection.getDefault(), repo.getUnderlyingSecurityId());
				if ((p != null) && (p instanceof Bond)) {
					Bond b = (Bond) p;
					return b.getPoolFactor(date);
				}
			}
		}
		return 0.0;
	}

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();

		if (this.marginCallConfigReportStyle == null) {
			this.marginCallConfigReportStyle = getCollateralConfigReportStyle();
		}
		if (this.marginCallConfigReportStyle != null) {
			treeList.add(this.marginCallConfigReportStyle.getNonInheritedTreeList());
		}
		if (this.tradeReportStyle == null) {
			this.tradeReportStyle = getTradeReportStyle();
		}

		if (this.tradeReportStyle != null) {
			treeList.add(this.tradeReportStyle.getNonInheritedTreeList());
		}
		if (this.plMarkReportStyle == null) {
			this.plMarkReportStyle = getPLMarkReportStyle();
		}
		if (this.plMarkReportStyle != null) {
			treeList.add(this.plMarkReportStyle.getNonInheritedTreeList());
		}

		return treeList;
	}

	protected TradeReportStyle getTradeReportStyle() {
		if (this.tradeReportStyle == null) {
			this.tradeReportStyle = (TradeReportStyle) getReportStyle(ReportRow.TRADE);
		}
		return this.tradeReportStyle;
	}
	
	protected CollateralConfigReportStyle getCollateralConfigReportStyle() {
		try {
			if (this.marginCallConfigReportStyle == null) {
				String className = "com.calypso.tk.report.CollateralConfigReportStyle";
				this.marginCallConfigReportStyle = (CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
						true, true);
			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		return this.marginCallConfigReportStyle;
	}

	protected PLMarkReportStyle getPLMarkReportStyle() {
		if (this.plMarkReportStyle == null) {
			this.plMarkReportStyle = (PLMarkReportStyle) getReportStyle(ReportRow.PL_MARK);
		}
		return this.plMarkReportStyle;
	}

	//AAP MIG 14.4 18/04
		protected JDate getTradeMaturityDate(Object trade){
			JDate maturity=null;
			if(trade!=null){
				maturity=((Trade) trade).getMaturityDate();
			}
			return maturity;
			
		}
	public String getIndAmtPayRecieve(final SignedAmount amount) {
		if (amount == null) {
			return null;
		}

		if (amount.get() >= 0.0) {
			return "PAY";
		} else {
			return "RECEIVE";
		}
	}

	public static String formatNumber(final double number) {
		final NumberFormat numberFormatter = new DecimalFormat("###,##0.00");
		String retVal = numberFormatter.format(number);

		// @TODO Need to see if there is a better way of doing this
		if (retVal.equals("-0,00")) {
			retVal = "0,00";
		}

		return retVal;
	}

	/**
	 * @param CollateralConfig
	 * @return MarginCallEntryDTO
	 */
	private MarginCallEntryDTO getMarginCallEntry(final CollateralConfig contract, final JDate valueDate) {

		if ((contract == null) || (contract.getId() < 1)) {
			return null;
		}
		final List<String> from = new ArrayList<String>();
		final StringBuilder sqlWhere = new StringBuilder();
		from.add(" margin_call_entries ");
		from.add(" mrgcall_config ");
		sqlWhere.append(" mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

		if (valueDate != null) {
			// process start date
			JDate processStartDate = valueDate.addBusinessDays(+1, Util.string2Vector("SYSTEM"));
			sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) >= ");
			sqlWhere.append(Util.date2SQLString(processStartDate));
			sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) <= ");
			sqlWhere.append(Util.date2SQLString(processStartDate.addBusinessDays(+1, Util.string2Vector("SYSTEM"))));
		}

		sqlWhere.append(" AND mrgcall_config.mrg_call_def = ");
		sqlWhere.append(contract.getId());

		List<MarginCallEntryDTO> l = null;
		try {
			l = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallEntriesDTO(
					sqlWhere.toString(), from, true);
		} catch (Exception e) {
			Log.error(this, e);
		}
		if ((l == null) || l.isEmpty() || (l.size() > 1)) {
			return null;
		}
		return l.get(0);
	}
}

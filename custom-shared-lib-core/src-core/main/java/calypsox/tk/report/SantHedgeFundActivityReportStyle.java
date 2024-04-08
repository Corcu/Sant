package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.SantHedgeFundActivityReport.TradePrincipalWrapper;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.tk.report.style.SantMarginCallEntryReportStyleHelper;
import calypsox.tk.report.style.SantTradeReportStyleHelper;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;

import com.calypso.apps.util.TreeList;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

public class SantHedgeFundActivityReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	// public static final String COLLATERAL_AGREE = "COLLATERAL_AGREE";
	// public static final String COLLATERAL_AGREE_TYPE = "COLLATERAL_AGREE_TYPE";
	// public static final String COUNTERPARTY = "COUNTERPARTY";
	// public static final String TRADE_ID = "TRADE_ID";
	// public static final String FRONT_OFFICE_ID = "FRONT_OFFICE_ID";
	// public static final String CLOSE_OF_BUSINESS = "CLOSE_OF_BUSINESS";
	// public static final String STRUCTURE = "STRUCTURE";
	// public static final String TRADE_DATE = "TRADE_DATE";
	// public static final String VALUE_DATE = "VALUE_DATE";
	// public static final String MATURITY_DATE = "MATURITY_DATE";
	// public static final String VALUATION_AGENT = "VALUATION_AGENT";
	// public static final String PORTFOLIO = "PORTFOLIO";
	// public static final String OWNER = "OWNER";
	// public static final String DEAL_OWNER = "DEAL_OWNER";
	public static final String COUNTERPARTY = "MCC.Legal Entity";
	public static final String VALUATION_AGENT = "MCC.ValuationAgent";
	public static final String DEAL_OWNER = "Book Legal Entity";
	public static final String INSTRUMENT = "Instrument";
	public static final String UNDERLYING = "Underlying";
	public static final String PRINCIPAL = "Principal";
	public static final String PRINCIPAL_CCY = "Principal CCY";
	public static final String INDEPENDENT_AMOUNT = "Ind. Amount";
	public static final String INDEPENDENT_AMOUNT_CCY = "Ind. Amount CCY";
	public static final String BUY_SELL = "Buy Sell";
	public static final String BASE_CCY = "Base Ccy";
	public static final String MTM_BASE_CCY = "MTM Base ccy";
	public static final String PRINCIPAL_2 = "Principal 2";
	public static final String PRINCIPAL_CCY_2 = "Principal 2 CCY";
	public static final String RATE_2 = "Rate 2";
	public static final String RATE = "Rate";

	// Contract
	private final SantMarginCallConfigReportStyleHelper mccReportStyleHelper;
	// Entry
	private final SantMarginCallEntryReportStyleHelper entryReportStyleHelper;
	// // Detail Entry
	// private final SantMarginCallDetailEntryReportStyleHelper detailEntryReportStyleHelper;

	// Trade
	private final SantTradeReportStyleHelper tradeReportStyleHelper;

	public SantHedgeFundActivityReportStyle() {
		this.mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();
		this.entryReportStyleHelper = new SantMarginCallEntryReportStyleHelper();
		this.tradeReportStyleHelper = new SantTradeReportStyleHelper();
	}

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}

		final TreeList treeList = new TreeList();
		treeList.add("SantCollateral", this.entryReportStyleHelper.getTreeList());
		treeList.add("SantCollateral", this.mccReportStyleHelper.getTreeList());
		treeList.add("SantCollateral", this.tradeReportStyleHelper.getTreeList());
		treeList.add("SantCollateral", UNDERLYING);
		treeList.add("SantCollateral", INSTRUMENT);
		treeList.add("SantCollateral", PRINCIPAL);
		treeList.add("SantCollateral", PRINCIPAL_CCY);
		treeList.add("SantCollateral", INDEPENDENT_AMOUNT);
		treeList.add("SantCollateral", INDEPENDENT_AMOUNT_CCY);
		treeList.add("SantCollateral", BUY_SELL);
		treeList.add("SantCollateral", BASE_CCY);
		treeList.add("SantCollateral", MTM_BASE_CCY);
		treeList.add("SantCollateral", PRINCIPAL_2);
		treeList.add("SantCollateral", PRINCIPAL_CCY_2);
		treeList.add("SantCollateral", RATE);
		treeList.add("SantCollateral", RATE_2);
		return treeList;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		if (row == null) {
			return null;
		}

		SantMarginCallDetailEntry santDetailEntry = (SantMarginCallDetailEntry) row
				.getProperty("SantMarginCallDetailEntry");

		if (santDetailEntry == null) {
			return null;
		}

		CollateralConfig mcc = santDetailEntry.getMarginCallConfig();

		if (mcc == null) {
			return null;
		}

		Trade trade = santDetailEntry.getTrade();

		if (trade == null) {
			return null;
		}
		MarginCallDetailEntryDTO detailEntryDTO = santDetailEntry.getDetailEntry();

		PricingEnv princingEnv = (PricingEnv) row.getProperty("PRICING_ENV");

		Product product = trade.getProduct();

		boolean isCollateralExposure = (product instanceof CollateralExposure);

		final TradePrincipalWrapper tradePrincipalWrapper = (TradePrincipalWrapper) row
				.getProperty(SantHedgeFundActivityReportTemplate.TRADE_PRINCIPAL_WRAPPER);

		// GSM: Pro incidence - Principal leg 1 and 2, and currencies corrected.
		if (columnName.equals(PRINCIPAL)) {

			return tradePrincipalWrapper.getPrincipal();

			// if (product != null) {
			// if (product instanceof Repo) {
			// return formatNumber(((Repo) product).getNominal(detailEntryDTO.getProcessDatetime().getJDate(TimeZone.getDefault())));
			// } else if (product instanceof SecLending) {
			// return formatNumber(((SecLending) product).getSecuritiesNominalValue(detailEntryDTO
			// .getProcessDatetime().getJDate(TimeZone.getDefault())));
			// } else if (isCollateralExposure) {
			// return formatNumber(((CollateralExposure) product).getPrincipal());
			// }
			// return formatNumber(0.0);
			// }

		} else if (columnName.equals(PRINCIPAL_2)) {

			return tradePrincipalWrapper.getPrincipal2();
			// if (isCollateralExposure) {
			// Double princ2 = null;
			// try {
			// princ2 = (Double) ((CollateralExposure) product)
			// .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_2);
			// } catch (Exception e) {
			// return null;
			// }
			// return formatNumber(princ2);
			// }
			// return null;

		} else if (columnName.equals(PRINCIPAL_CCY)) {

			return tradePrincipalWrapper.getPrincipalCcy();

			// return trade.getTradeCurrency();
		} else if (columnName.equals(PRINCIPAL_CCY_2)) {

			return tradePrincipalWrapper.getPrincipal2Ccy();
			// if (isCollateralExposure) {
			// return ((CollateralExposure) product).getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
			// }
			// return null;

		} else if (columnName.equals(INSTRUMENT)) {
			if (isCollateralExposure) {
				return trade.getProductSubType();
			} else {
				return trade.getProductType();
			}
		} else if (columnName.equals(UNDERLYING)) {
			String underlyingName = null;
			if (isCollateralExposure) {
				underlyingName = (String) ((CollateralExposure) product)
						.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_UNDERLYING);
				if (Util.isEmpty(underlyingName)) {
					underlyingName = (String) ((CollateralExposure) product)
							.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_UNDERLYING_1);
				}
				return underlyingName;
			} else if (product instanceof Repo) {
				try {
					Product repoUnderlying = DSConnection.getDefault().getRemoteProduct()
							.getProduct(((Repo) product).getUnderlyingSecurityId());
					return repoUnderlying.getDescription();
				} catch (Exception e) {
					Log.error(this, "Cannot retrieve security", e);
				}
			} else if (product instanceof SecLending) {
				SecLending secLending = (SecLending) product;
				final Vector<Collateral> leftCollaterals = secLending.getLeftCollaterals();
				if (leftCollaterals.size() > 0) {
					return leftCollaterals.get(0).getDescription();
				}
			}

		} else if (columnName.equals(INDEPENDENT_AMOUNT)) {
			PricerMeasure pm = detailEntryDTO.getIndependentAmount();
			return getPricerMeasureValue(pm);
		} else if (columnName.equals(INDEPENDENT_AMOUNT_CCY)) {
			PricerMeasure pm = detailEntryDTO.getIndependentAmount();
			if (pm != null) {
				return pm.getCurrency();
			}
			return null;
		} else if (columnName.equals(BUY_SELL)) {
			if (product != null) {
				if (product instanceof Repo) {
					return ((Repo) product).getDirection();
				} else if (product instanceof SecLending) {
					return ((SecLending) product).getDirection();
				} else if (isCollateralExposure) {
					return ((CollateralExposure) product).getDirection(trade);
				}
				return null;
			}
		} else if (columnName.equals(BASE_CCY)) {
			return mcc.getCurrency();
			// PricerMeasure pm =
			// detailEntryDTO.getMeasure(PricerMeasure.MARGIN_CALL);
			// if (pm != null) {
			// return pm.getCurrency();
			// }
			// return null;
		} else if (columnName.equals(MTM_BASE_CCY)) {
			PricerMeasure pm = detailEntryDTO.getMeasure(SantPricerMeasure.toString(SantPricerMeasure.MARGIN_CALL));
			return getPricerMeasureValue(pm);
		} else if (columnName.equals(RATE)) {
			if (princingEnv != null) {
				String ccy1 = trade.getTradeCurrency();
				// get the FX rate for the leg1 against the base currency
				if (isCollateralExposure) {
					CollateralExposure pColCtx = (CollateralExposure) product;
					ccy1 = (String) pColCtx.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_1);
					if (Util.isEmpty(ccy1)) {
						ccy1 = trade.getTradeCurrency();
					}
				}
				try {
					if (Util.isEmpty(ccy1)) {
						return null;
					}

					QuoteValue quote = princingEnv.getFXQuote(ccy1, mcc.getCurrency(), detailEntryDTO
							.getValueDatetime().getJDate(TimeZone.getDefault()));
					if ((quote != null) && !Double.isNaN(quote.getClose())) {
						return CollateralUtilities.formatFXQuote(quote.getClose(), ccy1, mcc.getCurrency());
					}
				} catch (MarketDataException e) {
					Log.error(this, e);
					return null;
				}
			}
			return null;

		} else if (columnName.equals(RATE_2)) {
			// get the FX rate for the leg2 against the base currency
			if ((princingEnv != null) && isCollateralExposure) {
				String ccy2 = (String) ((CollateralExposure) product)
						.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
				try {
					if (Util.isEmpty(ccy2)) {
						return null;
					}

					QuoteValue quote = princingEnv.getFXQuote(ccy2, mcc.getCurrency(), detailEntryDTO
							.getValueDatetime().getJDate(TimeZone.getDefault()));
					if ((quote != null) && !Double.isNaN(quote.getClose())) {
						return CollateralUtilities.formatFXQuote(quote.getClose(), ccy2, mcc.getCurrency());
					}
				} catch (MarketDataException e) {
					Log.error(this, e);
					return null;
				}
			}
			return null;
		} else if (columnName.equals(TradeReportStyle.TRADE_DATE)) {
			return getTradeDate(trade);
		}

		// GSM: Incidence 254. Must show the full names.
		else if (columnName.equals(COUNTERPARTY) && (santDetailEntry != null)
				&& (santDetailEntry.getMarginCallConfig() != null)
				&& (santDetailEntry.getMarginCallConfig().getLegalEntity() != null)) { // MCC.Legal Entity
			return santDetailEntry.getMarginCallConfig().getLegalEntity().getName();

		} else if (columnName.equals(VALUATION_AGENT) && (santDetailEntry != null)) { // MCC.Valuation Agent
			return getValuationAgent(santDetailEntry.getMarginCallConfig());

		} else if (columnName.equals(DEAL_OWNER) && (santDetailEntry != null) && (santDetailEntry.getTrade() != null)) { // MCC.Valuation
																														 // Agent
			final Trade ope = santDetailEntry.getTrade();
			if ((santDetailEntry.getTrade().getBook() != null)
					&& (santDetailEntry.getTrade().getBook().getLegalEntity() != null)) {
				return ope.getBook().getLegalEntity().getName();
			}
		} // end Fix 254. GSM

		// Entry
		else if (this.entryReportStyleHelper.isColumnName(columnName)
				&& (santDetailEntry.getSantEntry().getEntry() != null)) {
			row.setProperty(ReportRow.DEFAULT, santDetailEntry.getSantEntry().getEntry());
			return this.entryReportStyleHelper.getColumnValue(row, columnName, errors);
		}
		// Contract
		else if (this.mccReportStyleHelper.isColumnName(columnName) && (santDetailEntry.getMarginCallConfig() != null)) {
			row.setProperty(ReportRow.MARGIN_CALL_CONFIG, santDetailEntry.getMarginCallConfig());
			return this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
		}
		// Trade
		else if (this.tradeReportStyleHelper.isColumnName(columnName) && (santDetailEntry.getTrade() != null)) {
			row.setProperty(ReportRow.TRADE, santDetailEntry.getTrade());
			return this.tradeReportStyleHelper.getColumnValue(row, columnName, errors);
		}

		return null;
	}

	private Object getTradeDate(Trade trade) {
		if (trade == null) {
			return null;
		}
		if (trade.getProductType().equals(Product.SEC_LENDING)) {
			String kw = trade.getKeywordValue("REAL_TRADE_DATE");
			if (!Util.isEmpty(kw)) {
				return JDate.valueOf(kw);
			}
		} else {
			return trade.getTradeDate().getJDate(TimeZone.getDefault());
		}
		return null;
	}

	// GSM: Incidence 254. Must show the full names, not the short names of the Valuation agent (between others)
	// getCode() replaced with getName
	private String getValuationAgent(final CollateralConfig config) {
		final String valuationType = config.getValuationAgentType();
		if (Util.isEmpty(valuationType) || CollateralConfig.NONE.equals(valuationType)) {
			return null;
		}

		if (CollateralConfig.PARTY_A.equals(valuationType)) {
			return config.getProcessingOrg().getName();
		}

		if (CollateralConfig.PARTY_B.equals(valuationType)) {
			return config.getLegalEntity().getName();
		}

		if (CollateralConfig.BOTH.equals(valuationType)) {
			return new StringBuilder(config.getProcessingOrg().getName()).append(" ")
					.append(config.getLegalEntity().getName()).toString();
		}

		if (CollateralConfig.THIRD_PARTY.equals(valuationType)) {
			final int leId = config.getValuationAgentId();
			if (leId != 0) {
				return BOCache.getLegalEntity(DSConnection.getDefault(), leId).getName();
			}
		}

		return null;
	}

	/**
	 * @param pm
	 * @return the value of the given pricer measure
	 */
	private Object getPricerMeasureValue(PricerMeasure pm) {
		double result = 0.0;
		if (pm != null) {
			result = pm.getValue();
			if (Double.isNaN(result)) {
				result = 0.0;
			}
		}
		return formatNumber(result);
	}

	private Object formatNumber(Double result) {
		if (result == null) {
			return new Amount(0, 2);
		}
		return new Amount(result, 2);
	}
}

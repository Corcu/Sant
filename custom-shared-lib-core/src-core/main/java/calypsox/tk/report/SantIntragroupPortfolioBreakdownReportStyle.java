package calypsox.tk.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.SantIntragroupPortfolioBreakdownReport.MtmWrapper;
import calypsox.tk.report.SantIntragroupPortfolioBreakdownReport.TradeWrapper;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;

public class SantIntragroupPortfolioBreakdownReportStyle extends ReportStyle {

	private static final long serialVersionUID = 123L;

	// For Valuation Date
	public static final String CLOSE_OF_BUSINESS = "Close of Business";

	// For MTM Wrapper
	public static final String INDEPENDENT_AMOUNT_BASE = "Indep. amount Base";
	public static final String INDEPENDENT_CCY_BASE = "Indep. amount Base CCY";
	public static final String INDEPENDENT_AMOUNT = "Indep. amount";
	public static final String INDEPENDENT_CCY = "Indep. amount CCY";
	public static final String MTM_BASE_VALUE = "MTM Base Value";
	public static final String MTM_BASE_CCY = "MTM Base CCY";
	public static final String MTM_CCY = "MTM CCY";
	public static final String MTM_VALUE = "MTM Value";

	// For Trade Wrapper
	public static final String PRINCIPAL_CCY = "Principal CCY";
	public static final String PRINCIPAL = "Principal";
	public static final String PRINCIPAL_2_CCY = "Principal 2 CCY";
	public static final String PRINCIPAL_2 = "Principal 2";
	public static final String FX_RATE_1 = "FX_Rate_1";
	public static final String FX_RATE_2 = "FX_Rate_2";
	public static final String UNDERLYING_1 = "Underlying_1";
	public static final String UNDERLYING_2 = "Underlying_2";
	public static final String INSTRUMENT = "Instrument";

	// for repo enhacements
	public static final String REPO_ACCRUED_INTEREST = "Repo Accrued Interest";
	public static final String BOND_ACCRUED_INTEREST = "Bond Accrued Interest";
	public static final String CLEAN_PRICE = "Clean Price";
	public static final String CAPITAL_FACTOR = "Capital Factor";
	public static final String POOL_FACTOR = "Pool Factor";

	// TO CHECK
	public static final String INITIAL_COLLATERAL = "Initial Collateral";
	public static final String CLOSING_PRICE = "Closing Price";
	public static final String SANT_EXPOSURE = "Sant Exposure";

	// Buy Sell
	// Override Calypso behaviour
	public static final String SANT_BUY_SELL = "Buy/Sell";

	public static String[] DEFAULTS_COLUMNS = { CLOSE_OF_BUSINESS, INSTRUMENT };

	private final TradeReportStyle tradeReportStyle = (TradeReportStyle) getReportStyle(ReportRow.TRADE);

	private final SantMarginCallConfigReportStyleHelper mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		final JDate valDate = (JDate) row.getProperty(SantTradeBrowserReportTemplate.VAL_DATE);

		final TradeWrapper tradeWrapper = (TradeWrapper) row
				.getProperty(SantIntragroupPortfolioBreakdownReportTemplate.TRADE_WRAPPER);
		final MtmWrapper mtmWrapper = (MtmWrapper) row
				.getProperty(SantIntragroupPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE);
		// final MtmWrapper mtmWrapperPreviousDay = (MtmWrapper) row
		// .getProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_PREVIOUS_DAY);

		// Report Process Date
		if (columnName.equals(CLOSE_OF_BUSINESS)) {
			return valDate;
		}

		// MTM Wrapper
		else if (columnName.equals(INDEPENDENT_AMOUNT)) {
			return mtmWrapper.getIndepAmount();
		} else if (columnName.equals(INDEPENDENT_CCY)) {
			return mtmWrapper.getIndepAmountCcy();
		} else if (columnName.equals(INDEPENDENT_AMOUNT_BASE)) {
			return mtmWrapper.getIndepAmountBase();
		} else if (columnName.equals(INDEPENDENT_CCY_BASE)) {
			return mtmWrapper.getIndepAmountBaseCcy();
		} else if (columnName.equals(MTM_VALUE)) {
			if (Product.SEC_LENDING.equals(trade.getProductType())) {
				return mtmWrapper.getMarginCall();
			}
			return mtmWrapper.getNpv();
		} else if (columnName.equals(MTM_CCY)) {
			return mtmWrapper.getNpvCcy();
		} else if (columnName.equals(MTM_BASE_VALUE)) {
			return mtmWrapper.getNpvBase();
		} else if (columnName.equals(MTM_BASE_CCY)) {
			return mtmWrapper.getNpvBaseCcy();
		} else if (columnName.equals(CLOSING_PRICE)) {
			if (Product.SEC_LENDING.equals(trade.getProductType())) {
				return getOSLAClosingPrice(tradeWrapper.getPrincipal(), mtmWrapper.getNpv());
			}

			return mtmWrapper.getClosingPrice();

			// repo enhacements
		} else if (columnName.equals(REPO_ACCRUED_INTEREST)) {
			return mtmWrapper.getRepoAccruedInterest();
		} else if (columnName.equals(BOND_ACCRUED_INTEREST)) {
			return mtmWrapper.getBondAccruedInterest();
		} else if (columnName.equals(CLEAN_PRICE)) {
			return mtmWrapper.getCleanPrice();
		} else if (columnName.equals(CAPITAL_FACTOR)) {
			return mtmWrapper.getCapitalFactor();
		}

		// Trade Wrapper
		else if (columnName.equals(PRINCIPAL)) {
			return tradeWrapper.getPrincipal();
		} else if (columnName.equals(PRINCIPAL_CCY)) {
			return tradeWrapper.getPrincipalCcy();
		} else if (columnName.equals(PRINCIPAL_2)) {
			return tradeWrapper.getPrincipal2();
		} else if (columnName.equals(PRINCIPAL_2_CCY)) {
			return tradeWrapper.getPrincipal2Ccy();
		} else if (columnName.equals(FX_RATE_1)) {
			return tradeWrapper.getFXRate1();
		} else if (columnName.equals(FX_RATE_2)) {
			return tradeWrapper.getFXRate2();
		} else if (columnName.equals(UNDERLYING_1)) {
			return tradeWrapper.getUndelying1();
		} else if (columnName.equals(UNDERLYING_2)) {
			return tradeWrapper.getUndelying2();
		} else if (columnName.equals(INSTRUMENT)) {
			return tradeWrapper.getInstrument();
		} else if (columnName.equals(INITIAL_COLLATERAL)) {
			return tradeWrapper.getInitialCollateral();

			// repo enhacements
		} else if (columnName.equals(POOL_FACTOR)) {
			return tradeWrapper.getPoolFactor();
		}

		else if (columnName.equals(SANT_EXPOSURE)) {
			if (Product.SEC_LENDING.equals(trade.getProductType())) {
				if ((mtmWrapper.getMarginCall() != null) && (tradeWrapper.getInitialCollateral() != null)) {
					return new Amount(mtmWrapper.getMarginCall().get() - tradeWrapper.getInitialCollateral().get(), 2);
				}
			}
			return null;
		}

		else if (columnName.equals(SANT_BUY_SELL)) {

			String direction;

			if (tradeWrapper.getPrincipal() == null) {
				return null;
			}

			direction = tradeWrapper.getDirection();

			if ((direction == null) || direction.equals("")) {

				if (tradeWrapper.getPrincipal().get() < 0) {
					direction = "Loan";
				} else {
					direction = "Borrower";
				}
			}

			return direction;

			// if (tradeWrapper.getPrincipal().get() < 0) {
			// return "Loan";
			// }
			// return "Borrower";
		}

		else if (columnName.equals(TradeReportStyle.TRADE_DATE)) {
			return tradeWrapper.getTradeDate();
		}

		else if (this.mccReportStyleHelper.isColumnName(columnName)) {
			return this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
		}

		else {
			Object retVal = this.tradeReportStyle.getColumnValue(row, columnName, errors);
			if (retVal == null) {
				retVal = this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
			}
			return retVal;
		}

	}

	private Object getOSLAClosingPrice(DisplayValue quantity, DisplayValue mtmValue) {
		if ((quantity == null) || (mtmValue == null)) {
			return null;
		}
		double qty = Math.abs(quantity.get());
		double mtm = Math.abs(mtmValue.get());
		if (Math.abs(qty) == 0) {
			return null; // cannot divide by 0
		}
		BigDecimal bigQuantity = new BigDecimal(qty);
		BigDecimal bigMtm = new BigDecimal(mtm);
		return bigMtm.divide(bigQuantity, 4, RoundingMode.HALF_EVEN);
		// return null;
	}

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(this.tradeReportStyle.getNonInheritedTreeList());
		treeList.add(this.mccReportStyleHelper.getTreeList());

		return treeList;
	}

}

package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportStyle;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.tk.report.style.SantTradeReportStyleHelper;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;

public class SantCashAllocationReportStyle extends SantGenericTradeReportStyle {

	private static final long serialVersionUID = -1901923396240928336L;

	public static final String REPORT_DATE = "Report Date";
	public static final String MOVEMENT_AMOUNT = "Movement Amount";
	public static final String DELIVERY_DAY = "Delivery Day";
	public static final String MTM_CCY_AGREE = "MTM CCY Agree";
	public static final String COLL_SEC = "Collateral or Sec. Lending";

	private SantTradeReportStyleHelper tradeReportStyleHelper = null;
	private SantMarginCallConfigReportStyleHelper marginCallConfigReportStyleHelper = null;

	public SantCashAllocationReportStyle() {
		super();
		this.tradeReportStyleHelper = new SantTradeReportStyleHelper();
		this.marginCallConfigReportStyleHelper = new SantMarginCallConfigReportStyleHelper();
	}

	/**
	 * Calculate and get the result for every column of a row.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {
		SantCashAllocationItem item = (SantCashAllocationItem) row.getProperty("Default");
		Trade trade = item.getTrade();
		// GSM: 28/06/2013 - deprecated new core.
		CollateralConfig mcc = item.getCollateralConfig();
		PricingEnv pricingEnv = item.getPricingEnv();

		Object columnValue = "";

		if (REPORT_DATE.equals(columnName)) {
			if (trade != null) {
				columnValue = trade.getSettleDate();
			}
		} else if (MTM_CCY_AGREE.equals(columnName)) {
			MarginCall marginCall = (MarginCall) trade.getProduct();
			double principal = marginCall.getPrincipal();
			String principalCcy = marginCall.getCurrency();
			String mccCcy = mcc.getCurrency();

			// Need to convert the currency
			try {
				double amount = CollateralUtilities.convertCurrency(principalCcy, principal, mccCcy,
						trade.getSettleDate(), pricingEnv);
				columnValue = CollateralUtilities.formatAmount(amount, principalCcy);
			} catch (MarketDataException e) {
				Log.info("SantCashAllocationReportStyle", e);
				String msg = e.getMessage() + " [" + trade.getSettleDate() + "].";
				if (!errors.contains(msg)) {
					errors.add(msg);
				}
			}
		} else if (COLL_SEC.equals(columnName)) {
			return "Collateral";
		} else if (MOVEMENT_AMOUNT.equals(columnName)) {
			MarginCall marginCall = (MarginCall) trade.getProduct();
			double principal = marginCall.getPrincipal();
			columnValue = CollateralUtilities.formatAmount(principal, marginCall.getCurrency());
		} else if (DELIVERY_DAY.equals(columnName)) {
			if (mcc != null) {
				columnValue = mcc.getLeCashOffset();
			}
		} else if (this.marginCallConfigReportStyleHelper.isColumnName(columnName)) {
			if (mcc != null) {
				row.setProperty(ReportRow.MARGIN_CALL_CONFIG, mcc);
				columnValue = this.marginCallConfigReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		} else if (this.tradeReportStyleHelper.isColumnName(columnName)) {
			if (trade != null) {
				row.setProperty(ReportRow.TRADE, trade);
				columnValue = this.tradeReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		} else {
			columnValue = super.getColumnValue(row, columnName, errors);
		}

		return columnValue;
	}

	/**
	 * Get the MTM CCY agree. Movement amount converted to contract base currency
	 * 
	 * @param columnValue
	 *            value with the trade Quantity
	 * @param trade
	 *            trade to compare ccy
	 * @param mcc
	 *            margincallconfig to compare ccy
	 * @param pricingEnv
	 *            pricingEnvironment to convert the amount to the ccy
	 * @return
	 */
	@SuppressWarnings("unused")
	private Object getMtmCcyAgree(ReportRow row, Trade trade, MarginCallConfig mcc, PricingEnv pricingEnv) {
		SignedAmount sigAmount = (SignedAmount) this.tradeReportStyleHelper.getColumnValue(row, "Principal Amount",
				new Vector<String>());
		double amount = sigAmount.get();

		Object mtm = amount;

		try {
			if (!trade.getTradeCurrency().equals(mcc.getCurrency())) {
				mtm = 0.0;
				mtm = CollateralUtilities.convertCurrency(trade.getTradeCurrency(), amount, mcc.getCurrency(),
						trade.getSettleDate(), pricingEnv);

			}

		} catch (NumberFormatException e) {
			Log.error("Coudn't convert the amount to the contract ccy: " + e.getMessage(), e.getCause());
			Log.error(this, e); //sonar
		} catch (MarketDataException e) {
			Log.error("There's no Market Quote: " + e.getMessage(), e.getCause());
			Log.error(this, e); //sonar
		}

		return formatValueIfAmount(new Amount((Double) mtm));
	}

	@Override
	public TreeList getTreeList() {
		TreeList treeList = new TreeList();

		treeList.add(this.tradeReportStyleHelper.getTreeList());

		treeList.add(this.marginCallConfigReportStyleHelper.getTreeList());

		treeList.add("SantCashAllocation", REPORT_DATE);
		treeList.add("SantCashAllocation", COLL_SEC);
		treeList.add("SantCashAllocation", DELIVERY_DAY);
		treeList.add("SantCashAllocation", MOVEMENT_AMOUNT);
		treeList.add("SantCashAllocation", MTM_CCY_AGREE);

		return treeList;
	}

	/**
	 * Format the values to retrieve the data in the specified format. 2 decimals and no separator in thousands
	 * 
	 * @param value
	 *            value to format
	 * @return value formatted
	 */
	private String formatValueIfAmount(final Object value) {
		final NumberFormat numberFormatter = new DecimalFormat("###,##0.00", new DecimalFormatSymbols(Locale.GERMAN));

		if (value instanceof Amount) {
			final String numberString = numberFormatter.format(((Amount) value).get());

			return numberString;
		}

		if (value instanceof SignedAmount) {
			final String numberString = numberFormatter.format(((SignedAmount) value).get());

			return numberString;
		}

		return value.toString();
	}
}

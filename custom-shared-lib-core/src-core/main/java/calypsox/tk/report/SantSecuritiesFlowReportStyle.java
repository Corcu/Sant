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

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class SantSecuritiesFlowReportStyle extends SantGenericTradeReportStyle {

	public static final String PROCESS_DATE = "Process Date";
	private static final String CALYPSO = "CALYPSO";
	private static final String SELL = "Sell";
	private static final String BUY = "Buy";
	private JDate valDate = null;
	private SantTradeReportStyleHelper tradeReportStyleHelper = null;
	private SantMarginCallConfigReportStyleHelper marginCallConfigReportStyleHelper = null;

	public SantSecuritiesFlowReportStyle() {
		super();
		this.tradeReportStyleHelper = new SantTradeReportStyleHelper();
		this.marginCallConfigReportStyleHelper = new SantMarginCallConfigReportStyleHelper();
	}

	/**
	 * Calculate and get the result for every column of a row.
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {
		final Trade trade = (Trade) row.getProperty("Trade");
		// GSM: 28/06/2013 - deprecated new core.
		// final MarginCallConfig marginCallConfig = (MarginCallConfig) row.getProperty("SantMarginCallConfig");
		final CollateralConfig marginCallConfig = (CollateralConfig) row.getProperty("SantMarginCallConfig");
		this.valDate = (JDate) row.getProperty("valDate");
		final Bond bond = (Bond) row.getProperty("Bond");

		if (columnName.equals(SantSecuritiesFlowReportTemplate.SISTEMA_ORIGEN)) {
			return CALYPSO;
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.BUY_SELL)) {
			final Product product = trade.getProduct();
			int result = 0;
			result = product.getBuySell(trade);
			if (result > 0) {
				return SELL;
			} else {
				return BUY;
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.FRECUENCIA_CUPON)) {

			if (bond != null) {
				final Frequency freq = bond.getCouponFrequency();
				if (freq != null) {
					return freq.toString();
				} else {
					return "";
				}
			} else {
				return "";
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.FACE_AMOUNT)) {
			if (bond != null) {
				return formatValueIfAmount(new Amount(trade.getQuantity()));
			} else {
				return "";
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.CORTE_CUPON)) {
			if (bond != null) {
				final JDate corteCoupon = bond.getNextCouponDate(this.valDate);
				if (corteCoupon != null) {
					return corteCoupon;
				} else {
					return "";
				}
			} else {
				return "";
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.BOND_NAME)) {
			if (bond != null) {
				return bond.getName();
			} else {
				return "";
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.MATURITY_DATE)) {
			JDate maturityDate = null;
			if (trade != null) {
				maturityDate = trade.getProduct().getMaturityDate();
				if (maturityDate == null) {
					if (bond != null) {
						maturityDate = bond.getMaturityDate();
					}
				}
			}
			if (maturityDate != null) {
				return maturityDate;
			} else {
				return "";
			}
		} else if (columnName.equals(SantSecuritiesFlowReportTemplate.BOND_DIRTY_PRICE)) {
			return formatValueIfAmount(new Amount(getDirtyPrice(trade)));
		} else if (this.tradeReportStyleHelper.isColumnName(columnName)) {
			if (trade != null) {
				row.setProperty(ReportRow.TRADE, trade);
				return this.tradeReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		} else if (this.marginCallConfigReportStyleHelper.isColumnName(columnName)) {
			if (marginCallConfig != null) {
				row.setProperty(ReportRow.MARGIN_CALL_CONFIG, marginCallConfig);
				return this.marginCallConfigReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		} else {
			super.getColumnValue(row, columnName, errors);
		}

		return null;
	}

	private double getDirtyPrice(Trade trade) {
		double result = 0.0;

		result = trade.getTradePrice() + trade.getAccrual();

		return result * 100.0;
	}

	@Override
	public TreeList getTreeList() {
		TreeList treeList = super.getTreeList();

		treeList.add(this.tradeReportStyleHelper.getTreeList());

		treeList.add(this.marginCallConfigReportStyleHelper.getTreeList());

		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.SISTEMA_ORIGEN);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.BUY_SELL);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.BOND_DIRTY_PRICE);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.BOND_NAME);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.FACE_AMOUNT);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.FRECUENCIA_CUPON);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.MATURITY_DATE);
		treeList.add("SantSecuritiesFlow", SantSecuritiesFlowReportTemplate.CORTE_CUPON);

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
		if (value instanceof Amount) {
			final NumberFormat numberFormatter = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.ENGLISH));

			final String numberString = numberFormatter.format(((Amount) value).get());

			return numberString;
		}

		return value.toString();
	}

}

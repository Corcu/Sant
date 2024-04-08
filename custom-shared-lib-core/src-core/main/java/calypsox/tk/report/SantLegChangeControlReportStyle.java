package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportStyle;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.tk.report.style.SantTradeReportStyleHelper;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.CashAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class SantLegChangeControlReportStyle extends SantGenericTradeReportStyle {

	public static final String REPORT_DATE = "Report Date";
	public static final String OWNER = "Owner";
	public static final String GLCS = "GLCS";
	public static final String COLLATERAL_DESCR = "Collateral Description";
	public static final String COLL_AGREE_TYPE = "Coll. agree Type";
	public static final String COLL_BASE_CCY = "Coll. Base CCY";
	public static final String ELIGIBLE_COLLATERAL = "Eligible Collateral";
	public static final String CCY_ASSET = "CCY Asset";
	public static final String CPTY_CCY = "CounterParty Full Name - (CCY)";
	public static final String CCY_ASSET_CCY = "CCY Asset - (CCY)";
	public static final String INITIAL_POSITION = "Initial Position";
	public static final String INITIAL_CCY = "Initial Ccy";
	public static final String MOVEMENT_AMOUNT = "Movement Amount";
	public static final String CCY_MOVEMENT = "CCY Movement Amount";
	public static final String FINAL_SITUATION = "Final Situation";
	public static final String MOVEMENT_CCY = "Movement CCY";

	public static final String[] DEFAULTS_COLUMNS = {};

	private SantTradeReportStyleHelper tradeReportStyleHelper = null;
	private SantMarginCallConfigReportStyleHelper marginCallConfigReportStyleHelper = null;

	public SantLegChangeControlReportStyle() {
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
		final MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty("Entry");
		CollateralConfig marginCall = (CollateralConfig) row.getProperty("SantMarginConfig");
		Trade trade = (Trade) row.getProperty("Trade");

		SecurityAllocationDTO security = (SecurityAllocationDTO) row.getProperty("SecurityAllocation");
		CashAllocationDTO cash = (CashAllocationDTO) row.getProperty("CashAllocation");

		if (columnName.equals(SantLegChangeControlReportStyle.REPORT_DATE)) {
			if (security != null) {
				return security.getSettlementDate();
			} else if (cash != null) {
				return cash.getSettlementDate();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.OWNER)) {
			if (marginCall != null) {
				return marginCall.getProcessingOrg().getAuthName();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.GLCS)) {
			if (marginCall != null) {
				return marginCall.getLegalEntity().getAuthName();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.COLLATERAL_DESCR)) {
			if (marginCall != null) {
				return marginCall.getName();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.COLL_AGREE_TYPE)) {
			if (marginCall != null) {
				return marginCall.getContractType();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.COLL_BASE_CCY)) {
			if (marginCall != null) {
				return marginCall.getCurrency();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.ELIGIBLE_COLLATERAL)) {
			if (security != null) {
				return security.getProduct().getSecCode("ISIN");
			} else if (cash != null) {
				return cash.getCurrency();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.CCY_ASSET)) {
			if (security != null) {
				return security.getCurrency();
			} else if (cash != null) {
				return cash.getCurrency();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.CPTY_CCY)) {
			return marginCall.getLegalEntity().getName() + " - (" + marginCall.getCurrency() + ")";

		} else if (columnName.equals(SantLegChangeControlReportStyle.CCY_ASSET_CCY)) {
			return getColumnValue(row, SantLegChangeControlReportStyle.CCY_ASSET, errors);
		} else if (columnName.equals(SantLegChangeControlReportStyle.INITIAL_POSITION)) {
			return formatValueIfAmount(new Amount(entry.getOpeningBalance()));
		} else if (columnName.equals(SantLegChangeControlReportStyle.INITIAL_CCY)) {
			if (marginCall != null) {
				if (marginCall.getAccount() != null) {
					return marginCall.getAccount().getCurrency();
				} else {
					if (security != null) {
						return security.getCurrency();
					} else if (cash != null) {
						return cash.getCurrency();
					} else {
						return "";
					}
				}
			} else {
				if (security != null) {
					return security.getCurrency();
				} else if (cash != null) {
					return cash.getCurrency();
				} else {
					return "";
				}
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.MOVEMENT_AMOUNT)) {
			if (security != null) {
				return formatValueIfAmount(new Amount(security.getContractValue()));
			} else if (cash != null) {
				return formatValueIfAmount(new Amount(cash.getContractValue()));
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.CCY_MOVEMENT)) {
			if (security != null) {
				return security.getCurrency();
			} else if (cash != null) {
				return cash.getCurrency();
			} else {
				return "";
			}
		} else if (columnName.equals(SantLegChangeControlReportStyle.FINAL_SITUATION)) {
			return formatValueIfAmount(new Amount(entry.getClosingBalance()));
		} else if (columnName.equals(SantLegChangeControlReportStyle.MOVEMENT_CCY)) {
			return getColumnValue(row, CCY_MOVEMENT, errors);
		} else if (this.marginCallConfigReportStyleHelper.isColumnName(columnName)) {
			if (marginCall != null) {
				row.setProperty(ReportRow.MARGIN_CALL_CONFIG, marginCall);
				return this.marginCallConfigReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		} else if (this.tradeReportStyleHelper.isColumnName(columnName)) {
			if (trade != null) {
				row.setProperty(ReportRow.TRADE, trade);
				return this.tradeReportStyleHelper.getColumnValue(row, columnName, errors);
			}
		}

		return null;
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
			DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
			symbols.setGroupingSeparator(',');
			symbols.setDecimalSeparator('.');
			DecimalFormat formatter = new DecimalFormat("###,###.00", symbols);

			final String numberString = formatter.format(((Amount) value).get());

			return numberString;
		}

		return value.toString();
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();

		treeList.add("SantLegChangeControl", REPORT_DATE);
		treeList.add("SantLegChangeControl", OWNER);
		treeList.add("SantLegChangeControl", COLLATERAL_DESCR);
		treeList.add("SantLegChangeControl", COLL_AGREE_TYPE);
		treeList.add("SantLegChangeControl", COLL_BASE_CCY);
		treeList.add("SantLegChangeControl", ELIGIBLE_COLLATERAL);
		treeList.add("SantLegChangeControl", CCY_ASSET);
		treeList.add("SantLegChangeControl", CPTY_CCY);
		treeList.add("SantLegChangeControl", CCY_ASSET_CCY);
		treeList.add("SantLegChangeControl", INITIAL_POSITION);
		treeList.add("SantLegChangeControl", INITIAL_POSITION);
		treeList.add("SantLegChangeControl", MOVEMENT_AMOUNT);
		treeList.add("SantLegChangeControl", FINAL_SITUATION);
		treeList.add("SantLegChangeControl", MOVEMENT_CCY);

		return treeList;
	}
}

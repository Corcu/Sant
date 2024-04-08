package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.tk.report.style.SantMarginCallEntryReportStyleHelper;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantHedgeFundExposureAndCollateralReportStyle extends ReportStyle {

	private static final long serialVersionUID = -4641202961624280162L;

	private static String SUM_INDEP_AMOUNT_BASE = "SumIndepAmountBase";

	private static String SUM_NPV_AMOUNT_BASE = "SumNPVBase";

	private static String REPORT_DATE = "ReportDate";

	private static String NB_TRADES = "Sum of Number of Trades";

	private static String COLLATERAL_IN_TRANSIT_BASE = "Collateral Transit";

	private static String COLLATERAL_HELD_BASE = "Collateral Held";

	private static String POSSIBLE_COLLATERAL_BASE = "PossibleCollateralBase";

	public static final String[] DEFAULTS_COLUMNS = { REPORT_DATE, SUM_INDEP_AMOUNT_BASE, SUM_NPV_AMOUNT_BASE,
			NB_TRADES, COLLATERAL_IN_TRANSIT_BASE, POSSIBLE_COLLATERAL_BASE };

	private final SantMarginCallEntryReportStyleHelper entryReportStyleHelper = new SantMarginCallEntryReportStyleHelper();

	private final SantMarginCallConfigReportStyleHelper mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}

		TreeList treeList = new TreeList();
		treeList.add("SantCollateral", this.entryReportStyleHelper.getTreeList());
		treeList.add("SantCollateral", this.mccReportStyleHelper.getTreeList());
		treeList.add("SantCollateral", REPORT_DATE);
		treeList.add("SantCollateral", NB_TRADES);
		treeList.add("SantCollateral", COLLATERAL_IN_TRANSIT_BASE);
		treeList.add("SantCollateral", COLLATERAL_HELD_BASE);
		treeList.add("SantCollateral", SUM_INDEP_AMOUNT_BASE);
		treeList.add("SantCollateral", SUM_NPV_AMOUNT_BASE);
		treeList.add("SantCollateral", POSSIBLE_COLLATERAL_BASE);

		return treeList;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		if (row == null) {
			return null;
		}
		SantMarginCallEntry entry = (SantMarginCallEntry) row.getProperty("SantMarginCallEntry");

		if (entry == null) {
			return null;
		}

		MarginCallEntryDTO entryDTO = entry.getEntry();

		if (NB_TRADES.equals(columnName)) {
			return entry.getNbTrades();
		} else if (REPORT_DATE.equals(columnName)) {
			return entry.getReportDate();
		} else if (COLLATERAL_IN_TRANSIT_BASE.equals(columnName)) {
			return new Amount(entry.getCollateralInTransitBase(), 2);
		} else if (COLLATERAL_HELD_BASE.equals(columnName)) {
			// Prev Actual Cash Margin + Prev Actual Sec Margin
			return new Amount(entryDTO.getPreviousActualSecurityMargin() + entryDTO.getPreviousActualCashMargin(), 2);
			// GSM: inc. 822: Bug was calculating ActualSecurityMargin twice
			// return new Amount(entryDTO.getPreviousActualSecurityMargin() +
			// entryDTO.getPreviousActualSecurityMargin(),
		} else if (SUM_NPV_AMOUNT_BASE.equals(columnName)) {
			return new Amount(entry.getSumNPVBase(), 2);
		} else if (SUM_INDEP_AMOUNT_BASE.equals(columnName)) {
			return new Amount(entry.getSumIndepAmountBase(), 2);
		} else if (POSSIBLE_COLLATERAL_BASE.equals(columnName)) {
			// Total Prev Margin + Daily Cash Margin + Daily Sec Margin
			return new Amount(entryDTO.getPreviousTotalMargin() + entryDTO.getDailyCashMargin()
					+ entryDTO.getDailySecurityMargin(), 2);
		}

		// Entry //MarginCallEntryBase.Total Prev Mrg
		else if (this.entryReportStyleHelper.isColumnName(columnName) && (entry.getEntry() != null)) {
			row.setProperty(ReportRow.DEFAULT, entryDTO);
			return this.entryReportStyleHelper.getColumnValue(row, columnName, errors);
		}

		// Contract
		else if (this.mccReportStyleHelper.isColumnName(columnName) && (entry.getMarginCallConfig() != null)) {
			row.setProperty(ReportRow.MARGIN_CALL_CONFIG, entry.getMarginCallConfig());
			return this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
		}
		return null;
	}
}

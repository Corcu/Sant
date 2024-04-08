package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

@SuppressWarnings("serial")
public class ReposTradeReportStyle extends TradeReportStyle {
	// Constants used for the column names.
	private static final String MTM_VALUE = "MTM Base ccy";
	private static final String MTM_CURR = "Base Ccy";
	private static final String MTM_DATE = "Close of Business";
	private static final String CASH = "Principal";
	private static final String TRADE_CURR_2 = "Principal CCY";
	private static final String DIRECTION = "Buy Sell";

	// new
	private static final String COLLAT_AGREE = "Collateral agree";
	private static final String COLLAT_AGREE_TYPE = "Collateral Agree Type";
	private static final String OWNER = "Owner";
	private static final String DEAL_OWNER = "Deal Owner";
	private static final String IND_AMOUNT = "Ind. Amount";
	private static final String TRADE_DATE = "Trade Date";
	private static final String MAT_DATE = "Maturity Date";
	private static final String UNDERLYING = "Underlying";
	private static final String CLOS_PRICE = "Closing Price";
	private static final String RATE = "Rate";
	private static final String STRUCT = "Structure";
	private static final String VAL_AGENT = "Valuation Agent";
	private static final String DIRTY_PRICE = "Dirty Price";
	private static final String INT_RATE = "IntRate";
	private static final String NOMINAL = "Nominal";
	private static final String HAIRCUT = "Haircut";
	private static final String CPTY = "CounterParty";
	private static final String TRADE_ID = "TRADE_ID";

	@Override
	public TreeList getTreeList() {
		TreeList treeList = super.getTreeList();
		treeList.add(MTM_VALUE);
		treeList.add(MTM_CURR);
		treeList.add(CASH);
		treeList.add(TRADE_CURR_2);
		treeList.add(DIRECTION);
		treeList.add(COLLAT_AGREE);
		treeList.add(COLLAT_AGREE_TYPE);
		treeList.add(OWNER);
		treeList.add(DEAL_OWNER);
		treeList.add(IND_AMOUNT);
		treeList.add(TRADE_DATE);
		treeList.add(MAT_DATE);
		treeList.add(UNDERLYING);
		treeList.add(CLOS_PRICE);
		treeList.add(RATE);
		treeList.add(STRUCT);
		treeList.add(VAL_AGENT);
		treeList.add(MTM_DATE);
		treeList.add(DIRTY_PRICE);
		treeList.add(INT_RATE);
		treeList.add(NOMINAL);
		treeList.add(HAIRCUT);
		treeList.add(CPTY);
		treeList.add(TRADE_ID);

		return treeList;
	}

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		ReposTradeItem item = (ReposTradeItem) row.getProperty(ReposTradeItem.REPOS_TRADE_ITEM);

		if (columnName.compareTo(MTM_VALUE) == 0) {
			return item.getMtmValue();
		} else if (columnName.compareTo(MTM_CURR) == 0) {
			return item.getMtmCurr();
		} else if (columnName.compareTo(CASH) == 0) {
			return item.getCash();
		} else if (columnName.compareTo(TRADE_CURR_2) == 0) {
			return item.getTradeCurr2();
		} else if (columnName.compareTo(DIRECTION) == 0) {
			return item.getDirection();
		} else if (columnName.compareTo(COLLAT_AGREE) == 0) {
			return item.getCollatAgree();
		} else if (columnName.compareTo(COLLAT_AGREE_TYPE) == 0) {
			return item.getCollatAgreeType();
		} else if (columnName.compareTo(OWNER) == 0) {
			return item.getOwner();
		} else if (columnName.compareTo(DEAL_OWNER) == 0) {
			return item.getOwner();
		} else if (columnName.compareTo(IND_AMOUNT) == 0) {
			return item.getIndAmount();
		} else if (columnName.compareTo(TRADE_DATE) == 0) {
			return item.getTradeDate();
		} else if (columnName.compareTo(MAT_DATE) == 0) {
			return item.getMatDate();
		} else if (columnName.compareTo(UNDERLYING) == 0) {
			return item.getUnderlying();
		} else if (columnName.compareTo(CLOS_PRICE) == 0) {
			return item.getClosingPrice();
		} else if (columnName.compareTo(RATE) == 0) {
			return item.getRate();
		} else if (columnName.compareTo(STRUCT) == 0) {
			return item.getStructure();
		} else if (columnName.compareTo(VAL_AGENT) == 0) {
			return item.getValAgent();
		} else if (columnName.compareTo(MTM_DATE) == 0) {
			return item.getMtmDate();
		} else if (columnName.compareTo(DIRTY_PRICE) == 0) {
			return item.getDirtyPrice();
		} else if (columnName.compareTo(INT_RATE) == 0) {
			return item.getIntRate();
		} else if (columnName.compareTo(NOMINAL) == 0) {
			return item.getNominal();
		} else if (columnName.compareTo(HAIRCUT) == 0) {
			return item.getHaircut();
		} else if (columnName.compareTo(CPTY) == 0) {
			return item.getCpty();
		} else if (columnName.compareTo(TRADE_ID) == 0) {
			return item.getTradeID();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
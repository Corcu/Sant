package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.EquityReportStyle;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("rawtypes")
public class ExpTLM_EquityPricesReportStyle extends EquityReportStyle {

	private static final long serialVersionUID = -3323727852743710419L;
	// Constants used for the column names.
	private static final String FH_CONCILIA = "FHCONCILIA";
	private static final String FEED = "FEED";
	private static final String LADO = "LADO";
	private static final String PRICE = "PRECIO";
	private static final String ISIN = "ISIN";
	private static final String CURR = "DIVISA";

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		TreeList treeList = super.getTreeList();
		treeList.add(FH_CONCILIA);
		treeList.add(FEED);
		treeList.add(LADO);
		treeList.add(ISIN);
		treeList.add(CURR);
		treeList.add(PRICE);

		return treeList;
	}

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		ExpTLM_EquityPricesItem item = (ExpTLM_EquityPricesItem) row
				.getProperty(ExpTLM_EquityPricesItem.EXPTLM_EQUITYPRICES_ITEM);

		if (columnName.compareTo(FH_CONCILIA) == 0) {
			return item.getFecha();
		} else if (columnName.compareTo(FEED) == 0) {
			return item.getFeed();
		} else if (columnName.compareTo(LADO) == 0) {
			return item.getLado();
		} else if (columnName.compareTo(ISIN) == 0) {
			return item.getIsin();
		} else if (columnName.compareTo(CURR) == 0) {
			return item.getDivisa();
		} else if (columnName.compareTo(PRICE) == 0) {
			return item.getPrice();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
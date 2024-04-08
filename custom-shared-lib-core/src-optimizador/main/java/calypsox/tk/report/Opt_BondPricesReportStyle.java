package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class Opt_BondPricesReportStyle extends BondReportStyle {
	//Constants used for the column names.
	private static final String FH_CONCILIA = "FHCONCILIA";
	private static final String FEED = "FEED";
	private static final String LADO = "LADO";
	private static final String ISIN = "ISIN";
	private static final String PRICE = "PRICE";
	private static final String QUOTESET = "QUOTESET";
	
	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { FH_CONCILIA, FEED, LADO,
			ISIN, PRICE, QUOTESET };
	
	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		TreeList treeList = super.getTreeList();
		treeList.add(FH_CONCILIA);
		treeList.add(FEED);
		treeList.add(LADO);
		treeList.add(ISIN);
		treeList.add(PRICE);
		treeList.add(QUOTESET);
		
		return treeList;
	}

	@Override
	public Object getColumnValue(ReportRow row, String columnName,  @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		
		Opt_BondPricesItem item = (Opt_BondPricesItem) row.getProperty(Opt_BondPricesItem.EXPTLM_BONDPRICES_ITEM);
		
		if (item != null) {
			if (columnName.compareTo(FH_CONCILIA) == 0) {
				return item.getFecha();
			} else if (columnName.compareTo(FEED) == 0) {
				return item.getFeed();
			} else if (columnName.compareTo(LADO) == 0) {
				return item.getLado();
			} else if (columnName.compareTo(ISIN) == 0) {
				return item.getIsin();
			} else if (columnName.compareTo(PRICE) == 0) {
				return item.getPrice();
			} else if (columnName.compareTo(QUOTESET) == 0) {
				return item.getQuoteset();
			}
		}		
		
		 return super.getColumnValue(row, columnName, errors);		
	}
}
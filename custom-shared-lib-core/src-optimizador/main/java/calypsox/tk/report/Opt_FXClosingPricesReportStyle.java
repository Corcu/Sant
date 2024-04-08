package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.QuoteReportStyle;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class Opt_FXClosingPricesReportStyle extends QuoteReportStyle {
	//Constants used for the column names.
	private static final String FH_CONCILIA = "FHCONCILIA";
	private static final String FEED = "FEED";
	private static final String LADO = "LADO";
	private static final String PAIR = "PAIR";
	private static final String PRICE = "PRICE";
	private static final String QUOTE_SET = "QUOTE SET";
	
	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		TreeList treeList = super.getTreeList();
		treeList.add(FH_CONCILIA);
		treeList.add(FEED);
		treeList.add(LADO);
		treeList.add(PAIR);
		treeList.add(PRICE);
		treeList.add(QUOTE_SET);
		
		return treeList;
	}
	
	// Default columns.
	public static final String[] DEFAULT_COLUMNS = {FH_CONCILIA, FEED, LADO, PAIR, PRICE, QUOTE_SET};

	@Override
	public Object getColumnValue(ReportRow row, String columnName,  @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		Opt_FXClosingPricesItem item = (Opt_FXClosingPricesItem) row.getProperty(Opt_FXClosingPricesItem.EXPTLM_FXCLOSINGPRICES_ITEM);
		
		if(columnName.compareTo(FH_CONCILIA) == 0){
			return item.getFecha();
		} else if(columnName.compareTo(FEED) == 0){
			return item.getFeed();
		} else if(columnName.compareTo(LADO) == 0){
			return item.getLado();
		} else if(columnName.compareTo(PAIR) == 0){
			return item.getPair();	
		} else if(columnName.compareTo(PRICE) == 0){
			return item.getPrice();
		} else if(columnName.compareTo(PRICE) == 0){
			return item.getPrice();
		} else if(columnName.compareTo(QUOTE_SET) == 0){
			return item.getQuoteset();
		}else {
		    return super.getColumnValue(row, columnName, errors);
		}
	}
}
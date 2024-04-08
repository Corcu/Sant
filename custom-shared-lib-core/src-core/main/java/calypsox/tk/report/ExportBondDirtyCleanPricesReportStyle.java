package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class ExportBondDirtyCleanPricesReportStyle extends BondReportStyle {
    // Constants used for the column names.
    private static final String FH_CONCILIA = "FHCONCILIA";
    private static final String FEED = "FEED";
    private static final String LADO = "LADO";
    private static final String ISIN = "ISIN";
    private static final String CLEAN_PRICE = "CLEAN_PRICE";
    private static final String DIRTY_PRICE = "DIRTY_PRICE";
    private static final String INDICADOR = "INDICADOR";

    public static final String[] DEFAULTS_COLUMNS = { FH_CONCILIA, FEED, LADO,
	    ISIN, CLEAN_PRICE, DIRTY_PRICE, INDICADOR };

    @Override
    public TreeList getTreeList() {
    	@SuppressWarnings("deprecation")
	final TreeList treeList = super.getTreeList();
	treeList.add(FH_CONCILIA);
	treeList.add(FEED);
	treeList.add(LADO);
	treeList.add(ISIN);
	treeList.add(CLEAN_PRICE);
	treeList.add(DIRTY_PRICE);
	treeList.add(INDICADOR);

	return treeList;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
	    @SuppressWarnings("rawtypes") final Vector errors)
	    throws InvalidParameterException {

	final ExportBondDirtyCleanPricesItem item = (ExportBondDirtyCleanPricesItem) row
		.getProperty(ExportBondDirtyCleanPricesItem.EXP_BONDDCPRICES_ITEM);

	if (columnName.compareTo(FH_CONCILIA) == 0) {
	    return item.getFecha();
	} else if (columnName.compareTo(FEED) == 0) {
	    return item.getFeed();
	} else if (columnName.compareTo(LADO) == 0) {
	    return item.getLado();
	} else if (columnName.compareTo(ISIN) == 0) {
	    return item.getIsin();
	} else if (columnName.compareTo(CLEAN_PRICE) == 0) {
	    return item.getClean_price();
	} else if (columnName.compareTo(DIRTY_PRICE) == 0) {
	    return item.getDirty_price();
	} else if (columnName.compareTo(INDICADOR) == 0) {
	    return item.getIndicador();
	} else {
	    return super.getColumnValue(row, columnName, errors);
	}
    }
}
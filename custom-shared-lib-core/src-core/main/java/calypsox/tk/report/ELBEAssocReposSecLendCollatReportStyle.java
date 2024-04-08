package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

@SuppressWarnings("serial")
public class ELBEAssocReposSecLendCollatReportStyle extends TradeReportStyle {
    // Constants used for the column names.
    private static final String FRONT_ID = "Front ID";
    private static final String COLLAT_ID = "Collateral ID";
    private static String COD_LAYOUT = "CodLayout";
    private static String EXTRACT_DATE = "ExtractDate";
    private static String POS_TRANS_DATE = "PosTransDate";
    private static String SOURCE_APP = "SourceApplication";
    private static String FO_ID = "FrontOffice_Reference";

    // Default columns.
    public static final String[] DEFAULTS_COLUMNS = { COD_LAYOUT, EXTRACT_DATE,
	    POS_TRANS_DATE, SOURCE_APP, FRONT_ID, COLLAT_ID , FO_ID };

    @Override
    public TreeList getTreeList() {
	final TreeList treeList = super.getTreeList();
	treeList.add(COD_LAYOUT);
	treeList.add(EXTRACT_DATE);
	treeList.add(POS_TRANS_DATE);
	treeList.add(SOURCE_APP);
	treeList.add(FRONT_ID);
	treeList.add(COLLAT_ID);
	treeList.add(FO_ID);

	return treeList;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
	    @SuppressWarnings("rawtypes") final Vector errors)
	    throws InvalidParameterException {
	final ELBEAssocReposSecLendCollatItem item = (ELBEAssocReposSecLendCollatItem) row
		.getProperty(ELBEAssocReposSecLendCollatItem.ELBE_ASSOC_REPOS_SECLEN_COLLAT_ITEM);

	if (columnName.compareTo(FRONT_ID) == 0) {
	    return item.getFrontID();
	} else if (columnName.compareTo(COLLAT_ID) == 0) {
	    return item.getCollatID();
	} else if (columnName.compareTo(COD_LAYOUT) == 0) {
	    return item.getCodLayout();
	} else if (columnName.compareTo(EXTRACT_DATE) == 0) {
	    return item.getExtractDate();
	} else if (columnName.compareTo(POS_TRANS_DATE) == 0) {
	    return item.getPosTransDate();
	} else if (columnName.compareTo(SOURCE_APP) == 0) {
	    return item.getSourceApp();
	} else if (columnName.compareTo(FO_ID) == 0) {
		return item.getFrontOfficeReference();
	}else {
	    return super.getColumnValue(row, columnName, errors);
	}
    }
}
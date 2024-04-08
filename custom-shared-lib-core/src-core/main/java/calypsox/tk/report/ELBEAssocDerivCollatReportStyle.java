package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

@SuppressWarnings("serial")
public class ELBEAssocDerivCollatReportStyle extends TradeReportStyle {
	// Constants used for the column names.
	private static final String FRONT_ID_ELBE = "Front ID ELBE"; // MISAssocDerivCollat
	private static final String COLLAT_ID = "Collateral ID";
	private static String COD_LAYOUT = "CodLayout";
	private static String EXTRACT_DATE = "ExtractDate";
	private static String POS_TRANS_DATE = "PosTransDate";
	private static String SOURCE_APP = "SourceApplication";
	private static String BO_REFERENCE_C = "BackOffice_Reference";

	// MISAssocDerivCollat
	private static final String FRONT_ID_MIS = "Front ID MIS";
	// MISAssocDerivCollat - End

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { COD_LAYOUT, EXTRACT_DATE,
			POS_TRANS_DATE, SOURCE_APP, FRONT_ID_ELBE, COLLAT_ID , BO_REFERENCE_C};

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(COD_LAYOUT);
		treeList.add(EXTRACT_DATE);
		treeList.add(POS_TRANS_DATE);
		treeList.add(SOURCE_APP);
		treeList.add(FRONT_ID_ELBE);
		treeList.add(COLLAT_ID);

		// MISAssocDerivCollat
		treeList.add(FRONT_ID_MIS);
		// MISAssocDerivCollat - End

		treeList.add(BO_REFERENCE_C);
		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors)
			throws InvalidParameterException {
		final ELBEAssocDerivCollatItem item = (ELBEAssocDerivCollatItem) row
				.getProperty(ELBEAssocDerivCollatItem.ELBE_ASSOC_DERIV_COLLAT_ITEM);

		if (columnName.compareTo(FRONT_ID_ELBE) == 0) {
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
		} else if (columnName.compareTo(FRONT_ID_MIS) == 0) { // MISAssocDerivCollat
			return item.getFrontIDMIS();
		} else if (columnName.compareTo(BO_REFERENCE_C) == 0) {
			return item.getBoReferenceCustom();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
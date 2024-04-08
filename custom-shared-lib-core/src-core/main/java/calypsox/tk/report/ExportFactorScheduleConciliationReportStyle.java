package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

/**
 * @author Juan Angel Torija D?az
 */

@SuppressWarnings("serial")
public class ExportFactorScheduleConciliationReportStyle extends com.calypso.tk.report.BondReportStyle {

	// Constants used for the column names.

	private static final String ISIN = "ISIN";
	private static final String EFFECTIVE_DATE = "Effective Date";
	private static final String POOL_FACTOR = "Pool Factor";

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(ISIN);
		treeList.add(EFFECTIVE_DATE);
		treeList.add(POOL_FACTOR);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final ExportFactorScheduleConciliationItem item = (ExportFactorScheduleConciliationItem) row
				.getProperty(ExportFactorScheduleConciliationItem.EXPORT_FACTORSCHEDULECONCILIATION_ITEM);

		if (columnName.compareTo(ISIN) == 0) {
			return item.getISIN();
		} else if (columnName.compareTo(EFFECTIVE_DATE) == 0) {
			return item.getEffectiveDate();
		} else if (columnName.compareTo(POOL_FACTOR) == 0) {
			return item.getPoolFactor();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
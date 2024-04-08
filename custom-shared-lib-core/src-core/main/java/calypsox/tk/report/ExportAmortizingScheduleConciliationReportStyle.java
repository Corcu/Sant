package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

/**
 * @author Juan Angel Torija D?az
 */

@SuppressWarnings("serial")
public class ExportAmortizingScheduleConciliationReportStyle extends com.calypso.tk.report.BondReportStyle {

	// Constants used for the column names.

	private static final String ISIN = "ISIN";
	private static final String DATE = "Date";
	private static final String NOTIONAL = "Notional";
	private static final String COUPON_DATE_RULE = "Coupon Date Rule";
	private static final String FRQ = "Frequency";

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(ISIN);
		treeList.add(DATE);
		treeList.add(NOTIONAL);
		treeList.add(COUPON_DATE_RULE);
		treeList.add(FRQ);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final ExportAmortizingScheduleConciliationItem item = (ExportAmortizingScheduleConciliationItem) row
				.getProperty(ExportAmortizingScheduleConciliationItem.EXPORT_AMORTIZINGSCHEDULECONCILIATION_ITEM);

		if (columnName.compareTo(ISIN) == 0) {
			return item.getISIN();
		} else if (columnName.compareTo(DATE) == 0) {
			return item.getDate();
		} else if (columnName.compareTo(NOTIONAL) == 0) {
			return item.getNotional();
		} else if (columnName.compareTo(COUPON_DATE_RULE) == 0) {
			return item.getCouponDateRule();
		} else if (columnName.compareTo(FRQ) == 0) {
			return item.getFrq();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
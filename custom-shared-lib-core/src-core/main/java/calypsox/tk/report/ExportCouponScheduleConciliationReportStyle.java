package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

/**
 * @author Juan Angel Torija D?az
 */

@SuppressWarnings("serial")
public class ExportCouponScheduleConciliationReportStyle extends com.calypso.tk.report.BondReportStyle {

	// Constants used for the column names.

	private static final String ISIN = "ISIN";
	private static final String PERIOD_START_DATE = "Period Start Date";
	private static final String PERIOD_END_DATE = "Period End Date";
	private static final String COUPON = "Coupon";
	private static final String FRQ = "Frequency";

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(ISIN);
		treeList.add(PERIOD_START_DATE);
		treeList.add(PERIOD_END_DATE);
		treeList.add(COUPON);
		treeList.add(FRQ);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final ExportCouponScheduleConciliationItem item = (ExportCouponScheduleConciliationItem) row
				.getProperty(ExportCouponScheduleConciliationItem.EXPORT_COUPONSCHEDULECONCILIATION_ITEM);

		if (columnName.compareTo(ISIN) == 0) {
			return item.getISIN();
		} else if (columnName.compareTo(PERIOD_START_DATE) == 0) {
			return item.getPeriodStartDate();
		} else if (columnName.compareTo(PERIOD_END_DATE) == 0) {
			return item.getPeriodEndDate();
		} else if (columnName.compareTo(COUPON) == 0) {
			return item.getCoupon();
		} else if (columnName.compareTo(FRQ) == 0) {
			return item.getFrq();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
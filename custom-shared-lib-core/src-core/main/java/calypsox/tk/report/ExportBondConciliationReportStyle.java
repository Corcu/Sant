package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

/**
 * @author Juan Angel Torija Díaz
 */

@SuppressWarnings("serial")
public class ExportBondConciliationReportStyle extends com.calypso.tk.report.BondReportStyle {

	// Constants used for the column names.

	private static final String SPREAD = "Spread";
	private static final String COUPON = "Coupon";
	private static final String TOTAL_ISSUED = "Total Issued";
	private static final String RATE_INDEX_FACTOR = "Rate Index Factor";
	private static final String PAYMENT_RULE = "Payment Rule";
	private static final String REDEMPTION_CURRENCY = "Redemption Currency";
	private static final String RESET_HOLIDAYS = "Reset Holidays";
	private static final String REDEMPTION_PRICE = "Redemption Price";
	private static final String MIN_PURCHASE_AMOUNT = "Min Purchase Amount";
	private static final String SETTLE_DAYS = "Settle days";
	private static final String CUPON_CURRENCY = "Cupon Currency";
	private static final String RESET_DAYS = "Reset Days";
	private static final String RESET_BUS_LAG = "Reset Bus Lag";
	private static final String RESET_IN_ARREAR = "Reset In Arrear";
	private static final String PAYMENT_LAG = "Payment Lag";
	private static final String FREQUENCY = "Frequency";
	private static final String RECORD_DAYS = "Record Days";
	private static final String FLIPPER = "Flipper";
	private static final String FLIPPERDATE = "Flipper Date";
	private static final String FLIPPERFREQUENCY = "Flipper Frequency";
	private static final String DAYCOUNT = "DayCount";
	private static final String POOL_FACTOR_EFECTIVE_DATE = "Pool Factor Efective Date";
	private static final String POOL_FACTOR_KNOW_DATE = "Pool Factor Know Date";

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(SPREAD);
		treeList.add(COUPON);
		treeList.add(TOTAL_ISSUED);
		treeList.add(RATE_INDEX_FACTOR);
		treeList.add(PAYMENT_RULE);
		treeList.add(REDEMPTION_CURRENCY);
		treeList.add(RESET_HOLIDAYS);
		treeList.add(REDEMPTION_PRICE);
		treeList.add(MIN_PURCHASE_AMOUNT);
		treeList.add(SETTLE_DAYS);
		treeList.add(CUPON_CURRENCY);
		treeList.add(RESET_DAYS);
		treeList.add(RESET_BUS_LAG);
		treeList.add(RESET_IN_ARREAR);
		treeList.add(PAYMENT_LAG);
		treeList.add(FREQUENCY);
		treeList.add(RECORD_DAYS);
		treeList.add(FLIPPER);
		treeList.add(FLIPPERDATE);
		treeList.add(FLIPPERFREQUENCY);
		treeList.add(DAYCOUNT);
		treeList.add(POOL_FACTOR_EFECTIVE_DATE);
		treeList.add(POOL_FACTOR_KNOW_DATE);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final ExportBondConciliationItem item = (ExportBondConciliationItem) row
				.getProperty(ExportBondConciliationItem.EXPORT_BONDCONCILIATION_ITEM);

		if (columnName.compareTo(SPREAD) == 0) {
			return item.getSpread();
		}if (columnName.compareTo(COUPON) == 0) {
			return item.getRate();
		}if (columnName.compareTo(TOTAL_ISSUED) == 0) {
			return item.getTotalIssued();
		} else if (columnName.compareTo(RATE_INDEX_FACTOR) == 0) {
			return item.getRateIndexFactor();
		} else if (columnName.compareTo(PAYMENT_RULE) == 0) {
			return item.getPaymentRule();
		} else if (columnName.compareTo(REDEMPTION_CURRENCY) == 0) {
			return item.getRedemptionCurrency();
		} else if (columnName.compareTo(RESET_HOLIDAYS) == 0) {
			return item.getResetHolidays();
		} else if (columnName.compareTo(REDEMPTION_PRICE) == 0) {
			return item.getRedemptionPrice();
		} else if (columnName.compareTo(MIN_PURCHASE_AMOUNT) == 0) {
			return item.getMinPurchaseAmount();
		} else if (columnName.compareTo(SETTLE_DAYS) == 0) {
			return item.getSettleDays();
		} else if (columnName.compareTo(CUPON_CURRENCY) == 0) {
			return item.getCuponCurrency();
		} else if (columnName.compareTo(RESET_DAYS) == 0) {
			return item.getResetDays();
		} else if (columnName.compareTo(RESET_BUS_LAG) == 0) {
			return item.getResetBusLag();
		} else if (columnName.compareTo(RESET_IN_ARREAR) == 0) {
			return item.getResetInArrear();
		} else if (columnName.compareTo(PAYMENT_LAG) == 0) {
			return item.getPaymentLag();
		} else if (columnName.compareTo(FREQUENCY) == 0) {
			return item.getFrecuency();
		} else if (columnName.compareTo(RECORD_DAYS) == 0) {
			return item.getRecordDays();
		} else if (columnName.compareTo(FLIPPER) == 0) {
			return item.getFlipper();
		} else if (columnName.compareTo(FLIPPERDATE) == 0) {
			return item.getFlipperDate();
		} else if (columnName.compareTo(FLIPPERFREQUENCY) == 0) {
			return item.getFlipperFrequency();
		} else if (columnName.compareTo(DAYCOUNT) == 0) {
			return item.getDayCount();
		} else if (columnName.compareTo(POOL_FACTOR_EFECTIVE_DATE) == 0) {
			return item.getPoolFactorEfectiveDate();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
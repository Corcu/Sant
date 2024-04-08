package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.ReportRow;

@SuppressWarnings("serial")
public class ExportBondStaticDataReportStyle extends BondReportStyle {
	// Constants used for the column names.
	private static final String FECHA = "Fecha";
	private static final String ISIN = "ISIN";
	private static final String TYPE = "Bond_Type";
	private static final String SUBTYPE = "Sub_type";
	private static final String COUPON_CURR = "Coupon_Currency";
	private static final String CURRENCY = "Currency";
	private static final String DATED_DATE = "Dated_date";
	private static final String DAYCOUNT = "Daycount";
	private static final String FIXEDB = "Fixed_b";
	private static final String COUPON_FREQ = "Coupon_frequency";
	private static final String HOLIDAYS = "Holidays";
	private static final String RATE_INDEX = "Rate_index";
	private static final String ISSUE_DATE = "Issue_Date";
	private static final String ISSUER = "Issuer";
	private static final String MAT_DATE = "Maturity_Date";
	private static final String COUPON = "Coupon";
	private static final String SPREAD = "Spread";
	private static final String FACE_VALUE = "Face_value";
	private static final String FIRST_COUPON_DATE = "First_coupon_date";
	private static final String NOT_INDEX = "Notional_index";
	private static final String EXT_REF = "ExternalReference";
	private final NumberFormat numberFormatter = new DecimalFormat("#0.00000000");

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(FECHA);
		treeList.add(ISIN);
		treeList.add(TYPE);
		treeList.add(SUBTYPE);
		treeList.add(COUPON_CURR);
		treeList.add(CURRENCY);
		treeList.add(DATED_DATE);
		treeList.add(DAYCOUNT);
		treeList.add(FIXEDB);
		treeList.add(COUPON_FREQ);
		treeList.add(HOLIDAYS);
		treeList.add(RATE_INDEX);
		treeList.add(ISSUE_DATE);
		treeList.add(ISSUER);
		treeList.add(MAT_DATE);
		treeList.add(COUPON);
		treeList.add(SPREAD);
		treeList.add(FACE_VALUE);
		treeList.add(FIRST_COUPON_DATE);
		treeList.add(NOT_INDEX);
		treeList.add(EXT_REF);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final ExportBondStaticDataItem item = (ExportBondStaticDataItem) row
				.getProperty(ExportBondStaticDataItem.EXPORT_BONDSTATICDATA_ITEM);

		if (columnName.compareTo(ISIN) == 0) {
			return item.getIsin();
		} else if (columnName.compareTo(TYPE) == 0) {
			return item.getBondType();
		} else if (columnName.compareTo(SUBTYPE) == 0) {
			return item.getBondSubType();
		} else if (columnName.compareTo(COUPON_CURR) == 0) {
			return item.getCouponCurrency();
		} else if (columnName.compareTo(CURRENCY) == 0) {
			return item.getCurrency();
		} else if (columnName.compareTo(DATED_DATE) == 0) {
			return item.getDatedDate();
		} else if (columnName.compareTo(DAYCOUNT) == 0) {
			return item.getDaycount();
		} else if (columnName.compareTo(FIXEDB) == 0) {
			return item.getFixedB();
		} else if (columnName.compareTo(COUPON_FREQ) == 0) {
			return item.getCouponFrequency();
		} else if (columnName.compareTo(HOLIDAYS) == 0) {
			return item.getHolidays();
		} else if (columnName.compareTo(RATE_INDEX) == 0) {
			return item.getRateIndex();
		} else if (columnName.compareTo(ISSUE_DATE) == 0) {
			return item.getIssueDate();
		} else if (columnName.compareTo(ISSUER) == 0) {
			return item.getIssuer();
		} else if (columnName.compareTo(MAT_DATE) == 0) {
			return item.getMaturityDate();
		} else if (columnName.compareTo(COUPON) == 0) {
			return this.numberFormatter.format(Util.numberToRate(item.getCoupon()));
		} else if (columnName.compareTo(SPREAD) == 0) {
			return Util.numberToSpread(item.getSpread());
		} else if (columnName.compareTo(FACE_VALUE) == 0) {
			return this.numberFormatter.format(item.getFaceValue());
		} else if (columnName.compareTo(FIRST_COUPON_DATE) == 0) {
			return item.getFirstCouponDate();
		} else if (columnName.compareTo(NOT_INDEX) == 0) {
			return item.getNotionalIndex();
		} else if (columnName.compareTo(FECHA) == 0) {
			return item.getFecha();
		} else if (columnName.compareTo(EXT_REF) == 0) {
			return item.getExternalRef();
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
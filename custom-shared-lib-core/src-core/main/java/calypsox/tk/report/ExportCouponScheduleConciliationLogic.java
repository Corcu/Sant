package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.product.Bond;
import com.calypso.tk.product.util.CouponDate;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the Coupon Schedule Conciliation report.
 * 
 * @author Juan Angel Torija
 */
public class ExportCouponScheduleConciliationLogic {

	public static final String BLANK = "";
	public static final String ISIN = "ISIN";

	public ExportCouponScheduleConciliationLogic() {
	}

	/**
	 * Method used to add the rows in the report generated.
	 * 
	 * @param bond
	 *            Bond.
	 * @param dsConn
	 *            Database connection.
	 * @param errorMsgs
	 *            Vector with the different errors occurred.
	 * @return Vector with the rows added.
	 */
	public static Vector<ExportCouponScheduleConciliationItem> getReportRows(final Bond bond, final String date,
			final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<ExportCouponScheduleConciliationItem> reportRows = new Vector<ExportCouponScheduleConciliationItem>();
		final ExportCouponScheduleConciliationLogic verifiedRow = new ExportCouponScheduleConciliationLogic();
		Vector<ExportCouponScheduleConciliationItem> rowCreated = null;

		rowCreated = verifiedRow.getExportCouponScheduleConciliationItem(bond, dsConn, errorMsgs);
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.addAll(rowCreated);
		}
		return reportRows;
	}

	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector with the result to show.
	 * 
	 * @param bond
	 *            Bond.
	 * @param dsConn
	 *            Database connection.
	 * @param errors
	 *            Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary information.
	 * @throws RemoteException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Vector<ExportCouponScheduleConciliationItem> getExportCouponScheduleConciliationItem(final Bond bond,
			final DSConnection dsConn, final Vector<String> errors) {

		final Vector<ExportCouponScheduleConciliationItem> exp_CouponScheduleConciliationItem = new Vector<ExportCouponScheduleConciliationItem>();

		// Set values
		if (bond != null) {

			Enumeration coupons = bond.getCouponSchedule().elements();
			List<CouponDate> couponSchedule = Collections.list(coupons);
			Collections.sort(couponSchedule);

			for (int i = 0; couponSchedule.size() > i; i++) {

				ExportCouponScheduleConciliationItem exp_CouponScheduleConciliationItemProv = new ExportCouponScheduleConciliationItem();

				exp_CouponScheduleConciliationItemProv.setISIN(bond.getSecCodes().get(ISIN).toString());

				if (i == 0) {
					exp_CouponScheduleConciliationItemProv.setPeriodStartDate(bond.getDatedDate().toString());
				} else {
					exp_CouponScheduleConciliationItemProv.setPeriodStartDate(couponSchedule.get(i - 1).getEndDate()
							.toString());
				}

				exp_CouponScheduleConciliationItemProv.setPeriodEndDate(couponSchedule.get(i).getEndDate().toString());
				exp_CouponScheduleConciliationItemProv.setCoupon(String
						.valueOf(couponSchedule.get(i).getCouponRate() * 100));

				if (bond.getCouponFrequency() != null) {
					exp_CouponScheduleConciliationItemProv.setFrq(bond.getCouponFrequency().toString());
				} else {
					exp_CouponScheduleConciliationItemProv.setFrq(BLANK);
				}

				exp_CouponScheduleConciliationItem.add(exp_CouponScheduleConciliationItemProv);

			}

			return exp_CouponScheduleConciliationItem;
		}

		return null;
	}
}

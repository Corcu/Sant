package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.product.Bond;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the Bonds Static Data report.
 * 
 * @author Juan Angel Torija
 */
public class ExportAmortizingScheduleConciliationLogic {

	public static final String ISIN = "ISIN";
	public static final String BLANK = "";

	public ExportAmortizingScheduleConciliationLogic() {
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
	public static Vector<ExportAmortizingScheduleConciliationItem> getReportRows(final Bond bond, final String date,
			final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<ExportAmortizingScheduleConciliationItem> reportRows = new Vector<ExportAmortizingScheduleConciliationItem>();
		final ExportAmortizingScheduleConciliationLogic verifiedRow = new ExportAmortizingScheduleConciliationLogic();
		Vector<ExportAmortizingScheduleConciliationItem> rowCreated = null;

		rowCreated = verifiedRow.getExportAmortizingScheduleConciliationItem(bond, dsConn, errorMsgs);
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
	@SuppressWarnings("rawtypes")
	private Vector<ExportAmortizingScheduleConciliationItem> getExportAmortizingScheduleConciliationItem(
			final Bond bond, final DSConnection dsConn, final Vector<String> errors) {

		final Vector<ExportAmortizingScheduleConciliationItem> exp_AmortizingScheduleConciliationItem = new Vector<ExportAmortizingScheduleConciliationItem>();

		// Set values
		if (bond != null) {

			Enumeration notionals = bond.getAmortSchedule().elements();
			@SuppressWarnings("unchecked")
			List<NotionalDate> amortizingSchedule = Collections.list(notionals);
			Collections.sort(amortizingSchedule);

			for (NotionalDate amortizing : amortizingSchedule) {

				ExportAmortizingScheduleConciliationItem exp_AmortizingScheduleConciliationItemProv = new ExportAmortizingScheduleConciliationItem();

				exp_AmortizingScheduleConciliationItemProv.setISIN(bond.getSecCodes().get(ISIN).toString());
				exp_AmortizingScheduleConciliationItemProv.setDate(amortizing.getStartDate().toString());
				exp_AmortizingScheduleConciliationItemProv.setNotional(String.valueOf(amortizing.getNotionalAmt()));

				if (bond.getCouponDateRule() != null) {
					exp_AmortizingScheduleConciliationItemProv.setCouponDateRule(bond.getCouponDateRule().toString());
				} else {
					exp_AmortizingScheduleConciliationItemProv.setCouponDateRule(BLANK);
				}

				if (bond.getCouponFrequency() != null) {
					exp_AmortizingScheduleConciliationItemProv.setFrq(bond.getCouponFrequency().toString());
				} else {
					exp_AmortizingScheduleConciliationItemProv.setFrq(BLANK);
				}

				exp_AmortizingScheduleConciliationItem.add(exp_AmortizingScheduleConciliationItemProv);

			}

			return exp_AmortizingScheduleConciliationItem;
		}

		return null;
	}
}

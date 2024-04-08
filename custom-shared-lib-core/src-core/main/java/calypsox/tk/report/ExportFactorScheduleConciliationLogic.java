package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.PoolFactorEntry;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for Factor Schedule Conciliation report.
 * 
 * @author Juan Angel Torija
 */
public class ExportFactorScheduleConciliationLogic {

	public static final String BLANK = "";
	public static final String ISIN = "ISIN";
	public static final String ONE = "1";

	public ExportFactorScheduleConciliationLogic() {
	}

	public String getISIN(final String isin) {

		return isin;

	}

	public String getEffectiveDate(final String effectiveDate) {

		return effectiveDate;

	}

	public String getPoolFactor(final String poolFactor) {

		return poolFactor;

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
	public static Vector<ExportFactorScheduleConciliationItem> getReportRows(final BondAssetBacked bond,
			final String date, final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<ExportFactorScheduleConciliationItem> reportRows = new Vector<ExportFactorScheduleConciliationItem>();
		final ExportFactorScheduleConciliationLogic verifiedRow = new ExportFactorScheduleConciliationLogic();
		Vector<ExportFactorScheduleConciliationItem> rowCreated = null;

		rowCreated = verifiedRow.getExportFactorScheduleConciliationItem(bond, dsConn, errorMsgs);
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
	private Vector<ExportFactorScheduleConciliationItem> getExportFactorScheduleConciliationItem(
			final BondAssetBacked bond, final DSConnection dsConn, final Vector<String> errors) {

		final Vector<ExportFactorScheduleConciliationItem> exp_factorScheduleConciliationItem = new Vector<ExportFactorScheduleConciliationItem>();

		// Set values
		if (bond != null) {

			if (bond.getPoolFactorSchedule().size() == 0) {

				ExportFactorScheduleConciliationItem exp_factorScheduleConciliationItem2 = new ExportFactorScheduleConciliationItem();

				exp_factorScheduleConciliationItem2.setISIN(getISIN(bond.getSecCodes().get(ISIN).toString()));
				exp_factorScheduleConciliationItem2.setEffectiveDate(getEffectiveDate(BLANK));
				exp_factorScheduleConciliationItem2.setPoolFactor(getPoolFactor(ONE));

				exp_factorScheduleConciliationItem.add(exp_factorScheduleConciliationItem2);
			}

			else if (bond.getPoolFactorSchedule().size() > 0) {
				Set keys = bond.getPoolFactorSchedule().keySet();
				for (Iterator i = keys.iterator(); i.hasNext();) {
					String key = i.next().toString();
					PoolFactorEntry poolFactorEntry = (PoolFactorEntry) bond.getPoolFactorSchedule().get(
							JDate.valueOf(key));

					ExportFactorScheduleConciliationItem exp_factorScheduleConciliationItem2 = new ExportFactorScheduleConciliationItem();

					exp_factorScheduleConciliationItem2.setISIN(getISIN(bond.getSecCodes().get(ISIN).toString()));
					exp_factorScheduleConciliationItem2.setEffectiveDate(getEffectiveDate(poolFactorEntry
							.getEffectiveDate().toString()));
					exp_factorScheduleConciliationItem2.setPoolFactor(getPoolFactor(String.valueOf(poolFactorEntry
							.getPoolFactor())));

					exp_factorScheduleConciliationItem.add(exp_factorScheduleConciliationItem2);

				}
			}

			return exp_factorScheduleConciliationItem;
		}

		return null;
	}
}

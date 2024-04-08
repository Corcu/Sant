package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the Bonds Static Data report.
 * 
 * @author David Porras Mart?nez
 */
public class ExportBondStaticDataLogic {

	private static String fecha;
	public static final String SI = "SI";
	public static final String NO = "NO";
	public static final String BLANK = "";

	public ExportBondStaticDataLogic() {
	}

	/**
	 * Retrieve the date.
	 * 
	 * @param
	 * @return String with the date in dd/mm/yyyy fomat.
	 */
	public String getFecha() {
		return fecha;
	}

	public String getIsin(final Bond bond) {

		return bond.getSecCode("ISIN");

	}

	public String getBondType(final Bond bond) {

		return bond.getType();

	}

	public String getBondSubType(final Bond bond) {

		return bond.getSubType();

	}

	public String getCurrency(final Bond bond) {

		return bond.getCurrency();

	}

	public String getCouponCurrency(final Bond bond) {

		return bond.getRedemptionCurrency();

	}

	public JDate getDatedDate(final Bond bond) {

		return bond.getDatedDate();

	}

	public String getDayCount(final Bond bond) {
		if (bond.getDaycount() != null) {
			return bond.getDaycount().toString();
		} else {
			return BLANK;
		}
	}

	public int getFixedB(final Bond bond) {
		if (bond.getFixedB()) {
			return 1;
		} else {
			return 0;
		}
	}

	public String getCouponFrequency(final Bond bond) {
		if (bond.getCouponFrequency() != null) {
			return bond.getCouponFrequency().toString();
		} else {
			return BLANK;
		}
	}

	@SuppressWarnings("rawtypes")
	public String getHolidays(final Bond bond) {
		Vector holidays = bond.getHolidays();
		if ((holidays != null) && (holidays.size() > 0)) {
			return holidays.get(0).toString();
		} else {
			return BLANK;
		}
	}

	public String getRateIndex(final Bond bond) {
		if (bond.getRateIndex() != null) {
			String rateIndex = bond.getRateIndex().getStringValue();
			if (!rateIndex.equals(BLANK)) {

				return rateIndex.substring(0, rateIndex.lastIndexOf('/'));
			} else {
				return BLANK;
			}
		} else {
			return BLANK;
		}
	}

	public JDate getIssueDate(final Bond bond) {
		return bond.getIssueDate();
	}

	public String getIssuer(final Bond bond) {
		LegalEntity issuerLe;
		try {
			issuerLe = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
			if (issuerLe != null) {
				return issuerLe.getAuthName();
			}
		} catch (final RemoteException e) {
			Log.error(this, e); //sonar
		}
		return BLANK;

	}

	public JDate getMaturityDate(final Bond bond) {
		return bond.getMaturityDate();
	}

	public double getCouponRate(final Bond bond) {

		return bond.getCoupon();
	}

	public double getSpread(final Bond bond) {

		return bond.getRateIndexSpread();
	}

	public double getFaceValue(final Bond bond) {

		return bond.getFaceValue();

	}

	public JDate getFirstCouponDate(final Bond bond) {
		return bond.getFirstCouponDate();
	}

	public String getNotionalIndex(final Bond bond) {
		if (bond.getNotionalIndex() != null) {
			return SI;
		} else {
			return NO;
		}
	}

	public String getExternalReference(final Bond bond) {
		LegalEntity issuerLe;
		try {
			issuerLe = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
			if (issuerLe != null) {
				return issuerLe.getExternalRef();
			}
		} catch (final RemoteException e) {
			Log.error(this, e); //sonar
		}
		return BLANK;
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
	public static Vector<ExportBondStaticDataItem> getReportRows(final Bond bond, final String date,
			final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<ExportBondStaticDataItem> reportRows = new Vector<ExportBondStaticDataItem>();
		final ExportBondStaticDataLogic verifiedRow = new ExportBondStaticDataLogic();
		ExportBondStaticDataItem rowCreated = null;
		fecha = date;

		rowCreated = verifiedRow.getExportBondStaticDataItem(bond, dsConn, errorMsgs);
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.add(rowCreated);
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
	private ExportBondStaticDataItem getExportBondStaticDataItem(final Bond bond, final DSConnection dsConn,
			final Vector<String> errors) {

		final ExportBondStaticDataItem exp_BondStaticDataItem = new ExportBondStaticDataItem();

		// Set values
		if (bond != null) {
			exp_BondStaticDataItem.setFecha(getFecha());
			exp_BondStaticDataItem.setIsin(getIsin(bond));
			exp_BondStaticDataItem.setBondType(getBondType(bond));
			exp_BondStaticDataItem.setBondSubType(getBondSubType(bond));
			exp_BondStaticDataItem.setCouponCurrency(getCouponCurrency(bond));
			exp_BondStaticDataItem.setCurrency(getCurrency(bond));
			exp_BondStaticDataItem.setDatedDate(getDatedDate(bond));
			exp_BondStaticDataItem.setDaycount(getDayCount(bond));
			exp_BondStaticDataItem.setFixedB(getFixedB(bond));
			exp_BondStaticDataItem.setCouponFrequency(getCouponFrequency(bond));
			exp_BondStaticDataItem.setHolidays(getHolidays(bond));
			exp_BondStaticDataItem.setRateIndex(getRateIndex(bond));
			exp_BondStaticDataItem.setIssueDate(getIssueDate(bond));
			exp_BondStaticDataItem.setIssuer(getIssuer(bond));
			exp_BondStaticDataItem.setMaturityDate(getMaturityDate(bond));
			exp_BondStaticDataItem.setCoupon(getCouponRate(bond));
			exp_BondStaticDataItem.setSpread(getSpread(bond));
			exp_BondStaticDataItem.setFaceValue(getFaceValue(bond));
			exp_BondStaticDataItem.setFirstCouponDate(getFirstCouponDate(bond));
			exp_BondStaticDataItem.setNotionalIndex(getNotionalIndex(bond));
			exp_BondStaticDataItem.setExternalRef(getExternalReference(bond));
			return exp_BondStaticDataItem;
		}

		return null;
	}

}

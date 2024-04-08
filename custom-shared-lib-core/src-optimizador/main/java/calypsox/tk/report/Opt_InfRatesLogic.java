package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Vector;

import org.jfree.util.Log;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the FXClosingPrices report.
 * 
 * @author David Porras Mart?nez
 */
public class Opt_InfRatesLogic {

	private static final String LADO = "S";
	private static String fecha;
	private static String feed_name;
	private static final int NUM_MAX_DECIMALS = 10;

	public Opt_InfRatesLogic() {
	}

	@SuppressWarnings("unused")
	private Opt_InfRatesItem getExpTLM_InfRatesItem(final Vector<String> errors) {
		return null;
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

	/**
	 * Retrieve the feed of the bond.
	 * 
	 * @param
	 * @return String with the feed.
	 */
	public String getFeed() {
		return feed_name;
	}

	/**
	 * Retrieve the lado of the bond.
	 * 
	 * @param
	 * @return String with the lado.
	 */
	public String getLado() {
		return LADO;
	}

	/**
	 * Retrieve the price of the bond.
	 * 
	 * @param qv
	 *            QuoteValue associated with the bond.
	 * @return String with the close price in the correct format
	 */
	public String getPrice(final QuoteValue qv) {

		try {
			return Util.numberToString(CollateralUtilities.truncDecimal(NUM_MAX_DECIMALS, qv.getClose())).replace(".",
					"");
		} catch (NumberFormatException e) {
			Log.error("Close quote is NaN", e);
			return "";
		}
	}

	/**
	 * Retrieve the index key of the inflation rate using the mapping address.
	 * 
	 * @param con
	 *            DSConnection.
	 * @param qv
	 *            QuoteValue associated with the bond.
	 * @return String with the index key corresponding.
	 */
	private String getIndexKey(final QuoteValue qv, final HashMap<String, String> feedMap) {

		String indexKey = null;
		if (null != feedMap) {
			indexKey = feedMap.get(qv.getName());
		}
		return indexKey;
	}

	/**
	 * Method used to add the rows in the report generated.
	 * 
	 * @param qv
	 *            QuoteValue.
	 * @param dsConn
	 *            Database connection.
	 * @param errorMsgs
	 *            Vector with the different errors occurred.
	 * @return Vector with the rows added.
	 */
	public static Vector<Opt_InfRatesItem> getReportRows(final QuoteValue qv, final String date, final String feed,
			final HashMap<String, String> feedMap, final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<Opt_InfRatesItem> reportRows = new Vector<Opt_InfRatesItem>();
		final Opt_InfRatesLogic verifiedRow = new Opt_InfRatesLogic();
		Opt_InfRatesItem rowCreated = null;
		fecha = date;
		feed_name = feed;

		rowCreated = verifiedRow.getExpTLM_InfRatesItem(qv, feedMap, dsConn, errorMsgs);
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.add(rowCreated);
		}

		return reportRows;
	}

	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector with the result to show.
	 * 
	 * @param qv
	 *            QuoteValue.
	 * @param dsConn
	 *            Database connection.
	 * @param errors
	 *            Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary information.
	 * @throws RemoteException
	 */
	private Opt_InfRatesItem getExpTLM_InfRatesItem(final QuoteValue qv, final HashMap<String, String> feedMap,
			final DSConnection dsConn, final Vector<String> errors) {

		String indexKey;
		final Opt_InfRatesItem expTLM_InfRatesItem = new Opt_InfRatesItem();

		if (qv != null) {

			// get index key
			indexKey = getIndexKey(qv, feedMap);
			if (indexKey == null) {
				return null;
			}

			expTLM_InfRatesItem.setFecha(getFecha());
			expTLM_InfRatesItem.setFeed(getFeed());
			expTLM_InfRatesItem.setLado(getLado());
			expTLM_InfRatesItem.setPrice(getPrice(qv));
			expTLM_InfRatesItem.setIndex(indexKey);
			expTLM_InfRatesItem.setQuoteSetName(qv.getQuoteSetName());

			return expTLM_InfRatesItem;
		}
		return null;
	}

}

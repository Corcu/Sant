package calypsox.tk.report;

import java.rmi.RemoteException;
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
public class Opt_FXClosingPricesLogic {

	private static final String LADO = "S";
	private static String fecha;
	private static String feed_name;
	private static final int NUM_MAX_DECIMALS = 10;

	public Opt_FXClosingPricesLogic() {
	}

	@SuppressWarnings("unused")
	private Opt_FXClosingPricesItem getExpTLM_FXClosingPricesItem(Vector<String> errors) {
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
	public String getPrice(QuoteValue qv) {

		try {
			return Util.numberToString(CollateralUtilities.truncDecimal(NUM_MAX_DECIMALS, qv.getClose())).replace(".",
					"");
		} catch (NumberFormatException e) {
			Log.error("Close quote is NaN", e);
			return "";
		}
	}

	/**
	 * Retrieve the currency pair in XXX/YYY format.
	 * 
	 * @param qv
	 *            QuoteValue.
	 * @return String with the currency pair.
	 */
	public String getPair(QuoteValue qv) {
		String pair = "";
		String quoteName = qv.getName();
		if (quoteName != null) {
			pair = quoteName.substring(3, 6) + '/' + quoteName.substring(7, 10);
		}
		return pair;
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
	public static Vector<Opt_FXClosingPricesItem> getReportRows(QuoteValue qv, String date, String feed,
			DSConnection dsConn, Vector<String> errorMsgs) {

		Vector<Opt_FXClosingPricesItem> reportRows = new Vector<Opt_FXClosingPricesItem>();
		Opt_FXClosingPricesLogic verifiedRow = new Opt_FXClosingPricesLogic();
		Opt_FXClosingPricesItem rowCreated = null;
		fecha = date;
		feed_name = feed;

		rowCreated = verifiedRow.getExpTLM_FXClosingPricesItem(qv, dsConn, errorMsgs);
		if (null != rowCreated) { // If the result row is equals to NULL, we don't add this row to the report.
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
	private Opt_FXClosingPricesItem getExpTLM_FXClosingPricesItem(QuoteValue qv, DSConnection dsConn,
			Vector<String> errors) {

		Opt_FXClosingPricesItem expTLM_FXClosingPricesItem = new Opt_FXClosingPricesItem();

		if (qv != null) {

			expTLM_FXClosingPricesItem.setFecha(getFecha());
			expTLM_FXClosingPricesItem.setFeed(getFeed());
			expTLM_FXClosingPricesItem.setLado(getLado());
			expTLM_FXClosingPricesItem.setPrice(getPrice(qv));
			expTLM_FXClosingPricesItem.setPair(getPair(qv));
			expTLM_FXClosingPricesItem.setQuoteset(qv.getQuoteSetName());

			return expTLM_FXClosingPricesItem;
		}
		return null;
	}

}

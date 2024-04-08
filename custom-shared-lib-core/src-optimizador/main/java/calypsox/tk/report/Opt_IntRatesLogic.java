package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import org.jfree.util.Log;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the FXClosingPrices report.
 * 
 * @author David Porras Mart?nez
 */
public class Opt_IntRatesLogic {

	private static final String LADO = "S";
	private static String fecha;
	private static String feed_name;
	private static final int NUM_MAX_DECIMALS = 10;
	private static final String FUENTE = "FC_CL_MAD";

	public Opt_IntRatesLogic() {
	}

	@SuppressWarnings("unused")
	private Opt_IntRatesItem getExpTLM_IntRatesItem(Vector<String> errors) {
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

	public static String getPrice(QuoteValue qv) {

		try {
			return Util.numberToString(CollateralUtilities.truncDecimal(NUM_MAX_DECIMALS, qv.getClose() * 100))
					.replace(".", "");
		} catch (NumberFormatException e) {
			Log.error("Close quote is NaN", e);
			return "";
		}
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
	public static Vector<Opt_IntRatesItem> getReportRows(QuoteValue qv, String date,
			Map<String, RateIndex> rateIndexMap, Map<String, String> feedMap, DSConnection dsConn,
			Vector<String> errorMsgs) {

		Vector<Opt_IntRatesItem> reportRows = new Vector<Opt_IntRatesItem>();
		Opt_IntRatesLogic verifiedRow = new Opt_IntRatesLogic();
		Opt_IntRatesItem rowCreated = null;
		fecha = date;

		rowCreated = verifiedRow.getExpTLM_IntRatesItem(qv, rateIndexMap, feedMap, dsConn, errorMsgs);
		// rowCreated = verifiedRow.getExpTLM_IntRatesItem(qv, ffiBeans, feedMap, dsConn, errorMsgs);
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
	private Opt_IntRatesItem getExpTLM_IntRatesItem(QuoteValue qv, Map<String, RateIndex> rateIndexMap,
			Map<String, String> feedMap, DSConnection dsConn, Vector<String> errors) {

		final Opt_IntRatesItem expTLM_IntRatesItem = new Opt_IntRatesItem();

		if (qv != null) {

			// GSM: 21/03/2014. We required the mapping for the feed name between Calypso <-> Asset Control
			RateIndex rateIndex = rateIndexMap.get(qv.getName());

			if (rateIndex != null) {
				expTLM_IntRatesItem.setFecha(getFecha());
				expTLM_IntRatesItem.setFeed("FC_CL_MAD");
				expTLM_IntRatesItem.setLado(getLado());

				final String indexNameMapped = feedMap.get(qv.getName());
				expTLM_IntRatesItem.setIndex(indexNameMapped);
				expTLM_IntRatesItem.setPrice(getPrice(qv));
				expTLM_IntRatesItem.setQuoteSet(qv.getQuoteSetName());

				return expTLM_IntRatesItem;
			}
		}

		return null;
	}

	public static Opt_IntRatesItem buildRow(RateIndex x, Map<String, QuoteValue> quotesMap, JDate valDate,
			Map<String, String> feedMap, DSConnection dsConn, @SuppressWarnings("rawtypes") Vector errorMsgsP) {

		final Opt_IntRatesItem expTLM_IntRatesItem = new Opt_IntRatesItem();
		final QuoteValue qv = quotesMap.get(x.getQuoteName());

		if (qv != null) {
			if (x.getDefaults() != null && !Util.isEmpty(x.getDefaults().getAttribute(Opt_IntRatesReport.EXPORT_OPT_QUOTE_VALUE_OFFSET))) {
				expTLM_IntRatesItem.setFecha(qv.getDate() != null ? qv.getDate().toString() : valDate.toString());
			} else {
				expTLM_IntRatesItem.setFecha(valDate.toString());
			}
			expTLM_IntRatesItem.setFeed(FUENTE);
			expTLM_IntRatesItem.setLado(LADO);

			final String indexNameMapped = feedMap.get(qv.getName());
			if (indexNameMapped == null) {
				return null;
			}

			expTLM_IntRatesItem.setIndex(indexNameMapped);
			expTLM_IntRatesItem.setPrice(getPrice(qv));
			expTLM_IntRatesItem.setQuoteSet(qv.getQuoteSetName());

			return expTLM_IntRatesItem;

		}
		return null;

	}
}

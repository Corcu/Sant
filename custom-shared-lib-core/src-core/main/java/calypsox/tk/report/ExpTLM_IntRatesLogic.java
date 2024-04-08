package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.jfree.util.Log;

import calypsox.tk.util.bean.FeedFileInfoBean;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the FXClosingPrices report.
 * 
 * @author David Porras Mart?nez
 */
public class ExpTLM_IntRatesLogic {

	private static final String LADO = "S";
	private static String fecha;
	private static String feed_name;
	private static final int NUM_MAX_DECIMALS = 10;

	public ExpTLM_IntRatesLogic() {
	}

	@SuppressWarnings("unused")
	private ExpTLM_IntRatesItem getExpTLM_IntRatesItem(Vector<String> errors) {
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
			return Util.numberToString(CollateralUtilities.truncDecimal(NUM_MAX_DECIMALS, qv.getClose() * 100))
					.replace(".", "");
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
	private String getIndexKey(QuoteValue qv, HashMap<String, String> feedMap) {

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
	public static Vector<ExpTLM_IntRatesItem> getReportRows(QuoteValue qv, String date, String feed,
			ArrayList<FeedFileInfoBean> ffiBeans, HashMap<String, String> feedMap, DSConnection dsConn,
			Vector<String> errorMsgs) {

		Vector<ExpTLM_IntRatesItem> reportRows = new Vector<ExpTLM_IntRatesItem>();
		ExpTLM_IntRatesLogic verifiedRow = new ExpTLM_IntRatesLogic();
		ExpTLM_IntRatesItem rowCreated = null;
		fecha = date;
		feed_name = feed;

		rowCreated = verifiedRow.getExpTLM_IntRatesItem(qv, ffiBeans, feedMap, dsConn, errorMsgs);
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
	private ExpTLM_IntRatesItem getExpTLM_IntRatesItem(QuoteValue qv, ArrayList<FeedFileInfoBean> ffiBeans,
			HashMap<String, String> feedMap, DSConnection dsConn, Vector<String> errors) {

		ExpTLM_IntRatesItem expTLM_IntRatesItem = new ExpTLM_IntRatesItem();
		int flag = 0;
		String indexKey;
		long quoteTime = qv.getEnteredDate().getTime();

		if (qv != null) {
			if (ffiBeans != null) {
				for (int i = 0; i < ffiBeans.size(); i++) {
					long ffiBeanStartTime = ffiBeans.get(i).getStartTime().getTime();
					long ffiBeanEndTime = ffiBeans.get(i).getEndTime().getTime();

					if ((quoteTime >= ffiBeanStartTime) && (quoteTime <= ffiBeanEndTime)) {
						indexKey = getIndexKey(qv, feedMap);
						if (indexKey == null) {
							return null;
						}
						expTLM_IntRatesItem.setFecha(getFecha());
						expTLM_IntRatesItem.setFeed(getFeed());
						expTLM_IntRatesItem.setLado(getLado());
						expTLM_IntRatesItem.setPrice(getPrice(qv));
						expTLM_IntRatesItem.setIndex(indexKey);
						flag = 1;
						break;
					}
				}
			}
		}
		if (flag == 0) {
			return null;
		}
		return expTLM_IntRatesItem;
	}

}

package calypsox.tk.report;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.QuoteReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;

@SuppressWarnings("serial")
public class Opt_InfRatesReport extends QuoteReport {

	private HashMap<String, String> feedMap = new HashMap<String, String>();

	public static final String FEED_NAME = "Feed Name for TLM";

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		final DSConnection dsConn = getDSConnection();
		final RemoteMarketData remoteMarketData = dsConn.getRemoteMarketData();

		String feed = "";

		// get feed map address for obtain later the interest rates key-indexes
		// (feed addresses)
		this.feedMap = getFeedAddress(dsConn);

		// Get attributes
		final ReportTemplate reportTemp = getReportTemplate();
		final Attributes attributes = reportTemp.getAttributes();
		// date
		JDate jdate = reportTemp.getValDate();

		// Feed
		if (null == attributes.get(FEED_NAME)) {
			Log.error(this, "ExpTLM_InfRatesReport - FEED field is blank");
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
			return null;
		} else {
			feed = attributes.get(FEED_NAME).toString();
		}
		// Main process
		try {

			// get quote values
			final StringBuilder sb = new StringBuilder(" quote_set_name= 'OFFICIAL'")
					.append(" and quote_name like 'Inflation.%' and TRUNC(quote_date) = ")
					.append(Util.date2SQLString(jdate)).append(" and close_quote is not NULL");
			vQuotes = remoteMarketData.getQuoteValues(sb.toString());
			if (vQuotes != null) {
				for (int i = 0; i < vQuotes.size(); i++) {

					final Vector<Opt_InfRatesItem> expTLM_InfRatesItem = Opt_InfRatesLogic.getReportRows(
							vQuotes.get(i), jdate.toString(), feed, this.feedMap, dsConn, errorMsgsP);
					for (int j = 0; j < expTLM_InfRatesItem.size(); j++) {
						final ReportRow repRow = new ReportRow(expTLM_InfRatesItem.get(j));
						reportRows.add(repRow);
					}
				}
			}
			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
			return output;
		} catch (final RemoteException e) {
			Log.error(this, "ExpTLM_InfRatesReport - " + e.getMessage());
			Log.error(this, e); //sonar
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, String> getFeedAddress(final DSConnection conn) {
		final HashMap<String, String> feedHash = new HashMap<String, String>();

		try {
			final Vector<FeedAddress> feeds = conn.getRemoteMarketData().getAllFeedAddress("AC_TLM"); // only the
																									  // addresses for
																									  // TLM
			// export
			if ((null != feeds) && (feeds.size() > 0)) {
				for (int i = 0; i < feeds.size(); i++) {
					feedHash.put(feeds.get(i).getQuoteName(), feeds.get(i).getFeedAddress());
				}
			}
		} catch (final RemoteException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving Feed Addresses", e);
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
																											   // ERROR
		}
		return feedHash;
	}

}

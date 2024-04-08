package calypsox.tk.report;

import static calypsox.tk.util.ScheduledTaskCSVREPORT.USE_ENTERED_QUOTE_DATE;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.QuoteReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;

/**
 * Export the interest rates imported on the system according a input date.
 * 
 * @author David Porras & Guillermo Solano
 * @version 1.2
 * 
 */
public class Opt_IntRatesReport extends QuoteReport {

	public static final String EXPORT_OPT_QUOTE_VALUE_OFFSET = "Export_Opt_Quote_Value_Offset";

	private static final long serialVersionUID = -7735890564244377595L;

	/*
	 * GSM: EONIA arrives in D-1. To avoid problems, we use the process date instead of the enter date. As we are not
	 * sure we this boolean decides one or another option
	 */
	protected final static boolean RUN_PROCESS_OR_ENTER_DATE = true; // true -> entered date

	public static final String EXPTLM_INTRATES_REPORT = "ExpTLM_IntRatesReport";
	private Map<String, String> feedMap = new HashMap<String, String>();
	// private final Map<String, RateIndex> rateIndexMap = new HashMap<String, RateIndex>();

	public static final String DATE_EXPORT = "Date to export";
	public static final String FORMAT_EXPORT = "Format date to export";
	public static final String FEED_NAME = "Feed Name for TLM";
	public static final String FILE_ID = "File id for TLM";

	private static final String ASSET_CONTROL = "ASSET_CONTROL";

	/**
	 * load main method called by the core of calypso to generate this report
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {
		String clausule;
		Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
		final Map<String, QuoteValue> quotesMap = new HashMap<String, QuoteValue>();
		Vector<RateIndex> vIndexRates = new Vector<RateIndex>();
		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		final DSConnection dsConn = getDSConnection();
		final RemoteMarketData remoteMarketData = dsConn.getRemoteMarketData();
		// String file_id = "";
		final ReportTemplate reportTemp = getReportTemplate();

		// get feed map address for obtain later the interest rates key-indexes (feed addresses)
		// GSM: 21/03/2014. We required the mapping for the feed name between Calypso <-> Asset Control
		this.feedMap = buildFeedMapping(dsConn);

		// Main process
		try {

			final StringBuffer sb = new StringBuffer();
			final StringBuffer sbPast = new StringBuffer();
			// GSM: 16/05/2013. Fix to use entered date instead of quote date. Decided by boolean
			// get quote values
			if (readTemplateEnteredDate()) {
				sb.append("TRUNC(ENTERED_DATETIME) = ");
			} else {
				sb.append("TRUNC(quote_date) = ");
			}
			sbPast.append(sb.toString());
			sb.append(Util.date2SQLString(reportTemp.getValDate()));
			clausule = sb.toString();

			try {

				vIndexRates = DSConnection.getDefault().getRemoteReferenceData().getAllRateIndex();

			} catch (final RemoteException e1) {
				Log.error(this, e1); //sonar
				ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
																												   // ERROR
				return null;
			}

			// get quote values entered today
			vQuotes = remoteMarketData.getQuoteValues(clausule);

			if ((vQuotes != null) && !vQuotes.isEmpty()) {

				for (QuoteValue q : vQuotes) {
					quotesMap.put(q.getName(), q);
				}

				for (RateIndex x : vIndexRates) {
					if (x.getDefaults() != null && !Util.isEmpty(x.getDefaults().getAttribute(EXPORT_OPT_QUOTE_VALUE_OFFSET))) {
						StringBuffer sbPastQuoteValue = new StringBuffer();
						sbPastQuoteValue.append(sbPast);
						// get quote with offset
						Vector<QuoteValue> vQuotesPast = new Vector<QuoteValue>();
						String offset = x.getDefaults().getAttribute(EXPORT_OPT_QUOTE_VALUE_OFFSET);
						int offsetInt = -Integer.valueOf(offset);
						sbPastQuoteValue.append(Util.date2SQLString(reportTemp.getValDate().addBusinessDays(offsetInt, Util.string2Vector("SYSTEM"))));
						
						sbPastQuoteValue.append(" AND quote_name = ");
						sbPastQuoteValue.append(Util.string2SQLString(x.getQuoteName()));
				
						vQuotesPast = remoteMarketData.getQuoteValues(sbPastQuoteValue.toString());
						if (vQuotesPast.size() == 1) {
							quotesMap.put(x.getQuoteName(), vQuotesPast.get(0));
						} else {
							quotesMap.put(x.getQuoteName(), null);
						}
						
					}
					// this.rateIndexMap.put(x.getQuoteName(), x);
					final Opt_IntRatesItem row = Opt_IntRatesLogic.buildRow(x, quotesMap, reportTemp.getValDate(),
							this.feedMap, dsConn, errorMsgsP);
	
					if (row != null) {
						final ReportRow repRow = new ReportRow(row, Opt_IntRatesItem.EXPTLM_INTRATES_ITEM);
						reportRows.add(repRow);
					}
				}
			}

			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
			return output;

		} catch (final RemoteException e) {
			Log.error(this, "ExpTLM_IntRatesReport - " + e.getMessage());
			Log.error(this, e); //sonar
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
																											   // ERROR
		}
		return null;
	}

	/*
	 * Reads from the ST template if the quote to pick has to be based on the quote date or the entered date (of the
	 * quote).
	 */
	private boolean readTemplateEnteredDate() {

		String useEnteredDate = null;
		// We retrieve all the attributes.
		final Attributes attributes = getReportTemplate().getAttributes();

		if (null != attributes.get(USE_ENTERED_QUOTE_DATE)) {
			useEnteredDate = attributes.get(USE_ENTERED_QUOTE_DATE).toString().trim();
		}
		final String error = "Missing field " + USE_ENTERED_QUOTE_DATE + ". Please set the ScheduleTask properly.";

		if (((useEnteredDate == null) || useEnteredDate.trim().isEmpty())
				|| (!useEnteredDate.trim().equalsIgnoreCase("true") && !useEnteredDate.trim().equalsIgnoreCase("false"))) {
			// throw new Exception(error);
			Log.error(this, error);
		}

		if (useEnteredDate != null) {
			return useEnteredDate.trim().equalsIgnoreCase("true");
		}

		return RUN_PROCESS_OR_ENTER_DATE;
	}

	/*
	 * Return the feeds of AC_TLM
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, String> buildFeedMapping(final DSConnection conn) {
		final HashMap<String, String> feedHash = new HashMap<String, String>();

		try {
			final Vector<FeedAddress> feeds = conn.getRemoteMarketData().getAllFeedAddress(ASSET_CONTROL);
			// only the addresses for TLM export
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

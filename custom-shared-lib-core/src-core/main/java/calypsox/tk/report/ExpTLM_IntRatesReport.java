package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.tk.util.bean.FeedFileInfoBean;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import static calypsox.tk.util.ScheduledTaskCSVREPORT.USE_ENTERED_QUOTE_DATE;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * Export the interest rates imported on the system according a input date.
 *
 * @author David Porras & Guillermo Solano
 * @version 1.2
 */
public class ExpTLM_IntRatesReport extends QuoteReport {

    private static final long serialVersionUID = -7735890564244377595L;

    /*
     * GSM: EONIA arrives in D-1. To avoid problems, we use the process date instead of the enter date. As we are not
     * sure we this boolean decides one or another option
     */
    protected final static boolean RUN_PROCESS_OR_ENTER_DATE = true; // true -> entered date

    public static final String EXPTLM_INTRATES_REPORT = "ExpTLM_IntRatesReport";
    private HashMap<String, String> feedMap = new HashMap<String, String>();
    public static final String DATE_EXPORT = "Date to export";
    public static final String FORMAT_EXPORT = "Format date to export";
    public static final String FEED_NAME = "Feed Name for TLM";
    public static final String FILE_ID = "File id for TLM";

    /**
     * load main method called by the core of calypso to generate this report
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

        String clausule;
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        final DSConnection dsConn = getDSConnection();
        final RemoteMarketData remoteMarketData = dsConn.getRemoteMarketData();
        String feed = "";
        String file_id = "";
        final ReportTemplate reportTemp = getReportTemplate();

        // get feed map address for obtain later the interest rates key-indexes
        // (feed addresses)
        this.feedMap = getFeedAddress(dsConn);

        // Get attributes
        final Attributes attributes = reportTemp.getAttributes();
        // date

        // Feed
        if (null == attributes.get(FEED_NAME)) {
            Log.error(this, "ExpTLM_IntRatesReport - FEED field is blank");
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
            // ERROR
            return null;
        } else {
            feed = attributes.get(FEED_NAME).toString();
        }
        // File id
        if (null == attributes.get(FILE_ID)) {
            Log.error(this, "ExpTLM_IntRatesReport - FILE_ID field is blank");
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
            // ERROR
            return null;
        } else {
            file_id = attributes.get(FILE_ID).toString();
        }

        // Main process
        try {
            ArrayList<FeedFileInfoBean> ffiBeanArray;

            final StringBuffer sb = new StringBuffer();
            // GSM: 16/05/2013. Fix to use entered date instead of quote date. Decided by boolean
            // get quote values
            if (readTemplateEnteredDate()) {
                sb.append("TRUNC(ENTERED_DATETIME) = ");
            } else {
                sb.append("TRUNC(quote_date) = ");
            }
            sb.append(Util.date2SQLString(reportTemp.getValDate()));
            clausule = sb.toString();

            // deprecated
            // clausule = "trunc(quote_date) = " + Util.date2SQLString(reportTemp.getValDate());

            try {

                ffiBeanArray = SantReportingUtil
                        .getSantReportingService(dsConn)
                        .getFeedFileInfoData(
                                "select process,processing_org,start_time,end_time,"
                                        + "process_date,file_imported,inout,result,number_ok,number_warning,number_error,"
                                        + "original_file,comments from san_feed_file_info where process = 'importInterestRates' "
                                        + " and trunc(process_date) = " + Util.date2SQLString(reportTemp.getValDate())
                                        + " and file_imported = " + Util.string2SQLString(file_id));

            } catch (final RemoteException e1) {
                Log.error(this, e1); //sonar
                ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
                return null;
            }

            // get quote values entered today
            vQuotes = remoteMarketData.getQuoteValues(clausule);
            if (vQuotes != null) {
                for (int i = 0; i < vQuotes.size(); i++) {

                    final Vector<ExpTLM_IntRatesItem> expTLM_IntRatesItem = ExpTLM_IntRatesLogic.getReportRows(
                            vQuotes.get(i), reportTemp.getValDate().toString(), feed, ffiBeanArray, this.feedMap,
                            dsConn, errorMsgsP);
                    for (int j = 0; j < expTLM_IntRatesItem.size(); j++) {
                        final ReportRow repRow = new ReportRow(expTLM_IntRatesItem.get(j));
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

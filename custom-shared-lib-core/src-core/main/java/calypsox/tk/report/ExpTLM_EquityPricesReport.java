package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

@SuppressWarnings("serial")
public class ExpTLM_EquityPricesReport extends EquityReport {

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

        // Get attributes
        final ReportTemplate reportTemp = getReportTemplate();
        final Attributes attributes = reportTemp.getAttributes();
        // date
        JDate jdate = reportTemp.getValDate();

        // Feed
        if (null == attributes.get(FEED_NAME)) {
            Log.error(this, "ExpTLM_EquityPricesReport - FEED field is blank");
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
            return null;
        } else {
            feed = attributes.get(FEED_NAME).toString();
        }

        // Main process
        try {

            // get quote values
            final StringBuilder sb = new StringBuilder(" quote_set_name= 'OFFICIAL'")
                    .append(" and quote_name like 'Equity%'  and TRUNC(quote_date) = ")
                    .append(Util.date2SQLString(jdate)).append(" and close_quote is not NULL");
            vQuotes = remoteMarketData.getQuoteValues(sb.toString());
            if (vQuotes != null) {
                for (QuoteValue qv : vQuotes) {
                    final Vector<ExpTLM_EquityPricesItem> expTLM_EquityPricesItem = ExpTLM_EquityPricesLogic
                            .getReportRows(qv, jdate.toString(), feed, dsConn, errorMsgsP);
                    for (int j = 0; j < expTLM_EquityPricesItem.size(); j++) {
                        final ReportRow repRow = new ReportRow(expTLM_EquityPricesItem.get(j));
                        reportRows.add(repRow);
                    }
                }
            }
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ExpTLM_EquityPricesReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
        }
        return null;
    }

}

package calypsox.tk.report;

import calypsox.tk.report.dailytask.SantDailyTaskItem;
import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class SantDailyTaskReport extends SantReport {

    private static final long serialVersionUID = -5467927754459694225L;

    private static final List<String> MARGIN_CALL_NOT_CALCULATED_STATUS = new ArrayList<>();

    static {
        @SuppressWarnings("rawtypes")
        Vector notCalculatedStatusDomain = LocalCache.getDomainValues(DSConnection.getDefault(),
                "MarginCallNotCalculatedStatus");
        if (!Util.isEmpty(notCalculatedStatusDomain)) {
            for (int i = 0; i < notCalculatedStatusDomain.size(); i++) {
                String domainValue = (String) notCalculatedStatusDomain.get(i);
                if (domainValue != null) {
                    MARGIN_CALL_NOT_CALCULATED_STATUS.add(domainValue);
                }
            }
        }
    }

    @Override
    protected ReportOutput loadReport(Vector<String> errorMsgs) {

        try {
            return getReportOutput();

        } catch (final Exception e) {
            String msg = "Cannot load MarginCallEntry ";
            Log.error(this, msg, e);
            errorMsgs.add(msg + e.getMessage());
        }

        return null;
    }

    private ReportOutput getReportOutput() throws CollateralServiceException {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();


        final Collection<SantMarginCallEntry> santEntries = loader.loadSantMarginCallEntries(getReportTemplate(), this.getProcessStartDate(), this.getProcessEndDate());

        final List<ReportRow> rows = new ArrayList<>();
        for (final SantMarginCallEntry santEntry : santEntries) {
            SantDailyTaskItem item = new SantDailyTaskItem(santEntry, MARGIN_CALL_NOT_CALCULATED_STATUS);
            final ReportRow row = new ReportRow(item, "SantDailyTaskItem");
            rows.add(row);
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

}

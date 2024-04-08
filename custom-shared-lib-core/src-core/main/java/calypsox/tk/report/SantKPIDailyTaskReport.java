/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.report.kpidailytask.SantFrequencyHelper;
import calypsox.tk.report.kpidailytask.SantKPIDailyTaskItem;
import com.calypso.tk.collateral.MarginCallEntry;
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

/**
 * Reporte KPI Daily Task
 *
 * @author various
 */

public class SantKPIDailyTaskReport extends SantReport {

    private static final long serialVersionUID = -8964303714368663610L;

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

    /**
     * Main report method
     */
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

    @Override
    protected boolean checkProcessStartDate() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private ReportOutput getReportOutput() throws CollateralServiceException {

          /* Unused
        Vector<String> holidays = getReportTemplate().getHolidays();
        PricingEnv env = getPricingEnvironment(getProcessEndDate().addBusinessDays(-1, holidays));
        */

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();
        final Collection<SantMarginCallEntry> santEntries = loader.loadSantMarginCallEntries(getReportTemplate(),
                getProcessEndDate(), true);
        SantKPIDailyTaskItem.cleanCache();
        final List<ReportRow> rows = new ArrayList<>();
        for (final SantMarginCallEntry santEntry : santEntries) {
            MarginCallEntry entry = SantMarginCallUtil.getMarginCallEntry(santEntry.getEntry(),
                    santEntry.getMarginCallConfig(), true);

            SantKPIDailyTaskItem item = new SantKPIDailyTaskItem(santEntry, entry, MARGIN_CALL_NOT_CALCULATED_STATUS,
                    new SantFrequencyHelper(getDSConnection()));

            final ReportRow row = new ReportRow(item.getColumnMap(), "SantKPIDailyTask");
            rows.add(row);
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    /* Unused
    private PricingEnv getPricingEnvironment(JDate valDate) throws CalypsoServiceException {
        return getDSConnection().getRemoteMarketData().getPricingEnv("DirtyPrice", new JDatetime(valDate, TimeZone.getDefault()));
    }
    */
}

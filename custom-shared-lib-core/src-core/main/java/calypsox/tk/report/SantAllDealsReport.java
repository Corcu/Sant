/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class SantAllDealsReport extends SantReport {

    private static final long serialVersionUID = 123L;

    @Override
    protected boolean checkProcessStartDate() {
        return false;
    }

    /**
     * Generate report output
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

    /**
     * Collect report data row per row
     *
     * @return
     * @throws Exception
     */
    private ReportOutput getReportOutput() throws Exception {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final List<ReportRow> rows = new ArrayList<>();

        // get margin call entries
        final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();
        final Collection<SantMarginCallEntry> santMccEntries = loader.loadSantMarginCallEntries(getReportTemplate(),
                getProcessEndDate());

        // for each margin call entry get data item
        for (final SantMarginCallEntry santMccEntry : santMccEntries) {
            SantAllDealsItem dealItem = new SantAllDealsItem(santMccEntry);
            final ReportRow row = new ReportRow(dealItem, "SantAllDealsItem");
            rows.add(row);
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

}

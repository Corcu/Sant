package calypsox.tk.report;

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.delinquent.SantDelinquentItem;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;

/**
 * Main class for the SantDelinquentMarginCall report.
 *
 * @author Carlos Cejudo
 */
public class SantDelinquentMarginCallReport extends SantReport {

    private static final long serialVersionUID = 6736104013077922860L;

    private static String SANT_DELINQUENT_HIDE_STATUSES = "SantDelinquentHideStatuses";

    private static Vector<String> hideStatuses = null;

    static {
        hideStatuses = LocalCache.getDomainValues(DSConnection.getDefault(), SANT_DELINQUENT_HIDE_STATUSES);
        if (hideStatuses == null) {
            hideStatuses = new Vector<String>();
        }
    }

    @Override
    protected boolean checkProcessStartDate() {
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput loadReport(final Vector errors) {

        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        try {
            Double threshold = (Double) getReportTemplate().get(SantGenericTradeReportTemplate.DELINQUENT_THRESHOLD);
            if (threshold == null) {
                threshold = 0.0;
            }
            JDate processDate = getProcessEndDate();

            // Load entries from database
            final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();
            final Collection<SantMarginCallEntry> marginCallEntries = loader.loadSantMarginCallEntries(getReportTemplate(), processDate);

            final List<ReportRow> rows = getDelinquentRows(marginCallEntries, threshold);

            reportOutput.setRows(rows.toArray(new ReportRow[rows.size()]));
        } catch (final Exception e) {
            Log.error(this, "Cannot load MarginCallEntries", e);
            errors.add(e.getMessage());
        }

        return reportOutput;
    }

    private List<ReportRow> getDelinquentRows(Collection<SantMarginCallEntry> santEntries, double threshold) {
        List<ReportRow> rows = new ArrayList<ReportRow>();
        for (final SantMarginCallEntry santEntry : santEntries) {

            if (hideStatuses.contains(santEntry.getEntry().getStatus())) {
                continue;
            }

            boolean isDelinquent = santEntry.getEntry().getAttribute(CollateralStaticAttributes.DELINQUENT_AMOUNT) == null ? false
                    : true;
            if (!isDelinquent) {
                continue;
            }

            Double delinquentAmount = (Double) santEntry.getEntry().getAttribute(
                    CollateralStaticAttributes.DELINQUENT_AMOUNT);

            String delinquentAmountStr = Util.numberToString(delinquentAmount, 2, Locale.ENGLISH, false);
            delinquentAmount = Double.valueOf(delinquentAmountStr);

            if (delinquentAmount.doubleValue() > threshold) {
                ReportRow row = new ReportRow(new SantDelinquentItem(santEntry), "SantDelinquentItem");
                rows.add(row);
            }
        }
        return rows;
    }
}

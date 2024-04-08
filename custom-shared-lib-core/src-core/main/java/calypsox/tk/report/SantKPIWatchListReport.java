/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.report.kpidailytask.SantFrequencyHelper;
import calypsox.tk.report.kpiwatchlist.SantKPIWatchListItem;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;

public class SantKPIWatchListReport extends SantReport {

    private static final long serialVersionUID = 1236230416304935796L;

    private static String SANT_KPI_WATCHLIST_HIDE_STATUSES = "SantKPIWatchListHideStatuses";

    private static Vector<String> hideStatuses = null;

    static {
        hideStatuses = LocalCache.getDomainValues(DSConnection.getDefault(), SANT_KPI_WATCHLIST_HIDE_STATUSES);
        if (hideStatuses == null) {
            hideStatuses = new Vector<String>();
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

    @Override
    protected boolean checkProcessStartDate() {
        return false;
    }

    private ReportOutput getReportOutput() throws Exception {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();

        final Collection<SantMarginCallEntry> santEntries = loader.loadSantMarginCallEntries(getReportTemplate(),
                getProcessEndDate(), false);
        final List<ReportRow> rows = getRows(santEntries);
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    private PricingEnv getPricingEnvironment(JDate valDate) throws Exception {
        return getDSConnection().getRemoteMarketData().getPricingEnv("DirtyPrice", new JDatetime(valDate, TimeZone.getDefault()));
    }

    @SuppressWarnings("unchecked")
    private List<ReportRow> getRows(Collection<SantMarginCallEntry> santEntries) throws Exception {
        Vector<String> holidays = getReportTemplate().getHolidays();
        PricingEnv env = getPricingEnvironment(getProcessEndDate().addBusinessDays(-1, holidays));

        List<ReportRow> rows = new ArrayList<ReportRow>();
        for (final SantMarginCallEntry santEntry : santEntries) {

            MarginCallEntryDTO entryDTO = santEntry.getEntry();
            if (hideStatuses.contains(entryDTO.getStatus())) {
                continue;
            }

            boolean isDeficit = entryDTO.getAttribute(CollateralStaticAttributes.DELINQUENT_AMOUNT) == null ? false
                    : true;
            if (!isDeficit) {
                continue;
            }

            Double delinquentAmount = (Double) santEntry.getEntry().getAttribute(
                    CollateralStaticAttributes.DELINQUENT_AMOUNT);
            String delinquentAmountStr = Util.numberToString(delinquentAmount, 2, Locale.ENGLISH, false);
            delinquentAmount = Double.valueOf(delinquentAmountStr);

            if (delinquentAmount.doubleValue() <= 0) {
                continue;
            }

            JDate delinquentSince = CollateralUtilities.getEntryAttributeAsJDate(entryDTO,
                    CollateralStaticAttributes.DELINQUENT_SINCE);

            if ((delinquentSince == null)
                    || delinquentSince.addBusinessDays(2, holidays).after(
                    santEntry.getEntry().getProcessDatetime().getJDate(TimeZone.getDefault()))) {
                continue;
            }

            SantKPIWatchListItem item = new SantKPIWatchListItem(santEntry, env, holidays, new SantFrequencyHelper(
                    getDSConnection()));
            ReportRow row = new ReportRow(item, "SantKPIWatchListItem");
            rows.add(row);
        }
        return rows;
    }
}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantSQLQueryUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.SecurityTemplateHelper;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Cash Positions report. Uses SantPositionReport
 *
 * @author Guillermo Solano
 * @version 1.0
 */
public class SantCashPositionReport extends SantPositionReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 8753788279625125491L;
    public static final String DEFAULT = "Default";
    public static final String POS_DATE = "POSITION_DATE";
    public static final String POSITION = "POSITIONS";
    /**
     * Base currency of the Collateral Agreement
     */
    private String baseCurrency;

    /**
     * @param errors
     * @return rows for cash positions
     */
    @Override
    ReportRow[] buildBOPositionRows(Vector<String> errors) {

        // cash report retrieved each one in a thread
        CashPositionThread cashThread = new CashPositionThread(errors);
        // start threads
        cashThread.start();

        // join: wait all threads till last one finishes
        try {
            cashThread.join();

        } catch (InterruptedException e) {
            Log.error(this, e);
        }

        // recover rows
        return cashThread.getCashPositionsRows();
    }

    /**
     * @return rows after adding zero positions and date in column
     */
    @Override
    protected List<ReportRow> postProcessPositionRows(List<ReportRow> rowsList, Vector<String> errors) {

        // Add missing positions with value zero
        List<ReportRow> rowsReturned = new ArrayList<ReportRow>();

        for (ReportRow row : rowsList) {
            addMissingPositions(row, errors);
            List<ReportRow> newRows = getRowsFromPositions(row, errors);
            rowsReturned.addAll(newRows);
        }

        return rowsReturned;
    }

    /**
     * @return true if base currency filters are required
     */
    @Override
    boolean checkFilterRows() {

        baseCurrency = (String) getReportTemplate().get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        return !Util.isEmpty(baseCurrency);
    }

    /**
     * @return true if base currency filter applied
     */
    @Override
    boolean filterRow(final ReportRow row) {

        final Inventory pos = (Inventory) row.getProperty(ReportRow.INVENTORY);
        final MarginCallConfig mcConfig = (pos.getMarginCallConfigId() == 0) ? null : BOCache.getMarginCallConfig(
                DSConnection.getDefault(), pos.getMarginCallConfigId());
        if (mcConfig != null) {
            //base currency
            if (!Util.isEmpty(baseCurrency) && !mcConfig.getCurrency().equals(baseCurrency))
                return true;

        } else {
            Log.error(this, "No marginCallConfig found in for BO position ");
        }
        return false;
    }

    /**
     * @param template attributes for BOCashPosition
     */
    @Override
    void updateTemplate(ReportTemplate template) {

        template.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");
        template.put(BOPositionReportTemplate.POSITION_DATE, "Trade");
        template.put(BOPositionReportTemplate.POSITION_CLASS, "Margin_Call");
        template.put(BOPositionReportTemplate.POSITION_TYPE, "Not settled,Actual");
        template.put(BOPositionReportTemplate.POSITION_VALUE, "Nominal");
        template.put("AGGREGATION", "Book/Agent/Account");
        template.put(BOPositionReportTemplate.FILTER_ZERO, "false");
        template.put(BOPositionReportTemplate.MOVE, "Balance");

        //search by book
        final String bookIdStr = (String) getReportTemplate().get(SantGenericTradeReportTemplate.PORTFOLIO);

        if (!Util.isEmpty(bookIdStr)) {

            Vector<Book> books = BOCache.getBooksFromBookIds(getDSConnection(), Util.string2IntVector(bookIdStr));
            Vector<String> bookNames = new Vector<String>(books.size());
            for (Book b : books) {
                bookNames.add(b.getName());
            }
            template.put(BOPositionReportTemplate.BOOK_LIST, Util.collectionToString(bookNames));
        }

        // Search by counterparty (agent)
        final String counterpartiesIds = SantSQLQueryUtil.getCounterparty(getReportTemplate());
        if (!Util.isEmpty(counterpartiesIds)) {
            template.put(BOPositionReportTemplate.AGENT_ID, counterpartiesIds);
        }
    }

    /**
     * @param reportRow adding positions that are zero not included by the call to BOCashPosition
     * @param errors
     */
    private void addMissingPositions(ReportRow reportRow, @SuppressWarnings("rawtypes") Vector errors) {

        //v14 Migration - GSM adaption to new format
        JDate endDate = getEndDate(getReportTemplate(), getValDate());
        JDate startDate = getStartDate(getReportTemplate(), getValDate());

        @SuppressWarnings("unchecked")
        Map<JDate, Vector<InventoryCashPosition>> positions = reportRow
                .getProperty(POSITION);
        JDate date = startDate;
        while (!date.after(endDate)) {
            // v14 Mig - GSM change key for JDate
            //String dateString = Util.dateToMString(date);// BOPositionReport.dateToMString(date);
            if (positions.get(date) == null) {
                Vector<InventoryCashPosition> positionVectorModel = new ArrayList<Vector<InventoryCashPosition>>(
                        positions.values()).get(0);
                InventoryCashPosition newCashPosition = (InventoryCashPosition) positionVectorModel.get(0).clone();
                newCashPosition.setTotal(0.0);
                Vector<InventoryCashPosition> newPositionVector = new Vector<InventoryCashPosition>();
                newPositionVector.add(newCashPosition);
                positions.put(date, newPositionVector);
            }
            date = date.addDays(1);
        }
    }

    /**
     * @param reportRow
     * @param errors
     * @return list of rows with positions dates as column POS_DATE
     */
    @SuppressWarnings("unchecked")
    private List<ReportRow> getRowsFromPositions(ReportRow reportRow, @SuppressWarnings("rawtypes") Vector errors) {
        List<ReportRow> rows = new ArrayList<>();

        Map<String, Vector<InventoryCashPosition>> positions = reportRow
                .getProperty("POSITIONS");
        for (Object dateObject : positions.keySet()) {
            // v14 Mig - GSM change key for JDate
            //SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            //String dateString = ((JDate) dateObject).toString();
            JDate date = ((JDate) dateObject);//JDate.valueOf(dateFormat.parse(dateString));
            JDate startDate = getStartDate(getReportTemplate(), getValDate());

            if (!date.before(startDate)) {//(JDate) reportRow.getProperty("StartDate"))) {
                ReportRow newRow = (ReportRow) reportRow.clone();

                InventoryCashPosition position = positions.get(date).get(0);//.get(dateString).get(0);
                newRow.setProperty(DEFAULT, position);
                newRow.setProperty(POS_DATE, date);

                rows.add(newRow);
            }
        }

        return rows;
    }

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.generic.loader.SantGenericPLMarksLoader;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.*;

/**
 * Portfolio Reconciliation is intended to satisfy the portfolio reconcilation data to trioptima for the new DFA USA and
 * EMIR EU normative. Description DDR (incoming) on Port. Reconciliation v1.3 - Calypso (internal) DDR description on
 * Port. Reconciliation v1.1 wrote by Nacho
 *
 * @author Guillermo Solano
 * @version 0.9
 */
public class PortfolioReconciliationReport extends Report {

    /* SERIAL UID */
    private static final long serialVersionUID = 5294933204506370783L;

    /* Constanst */
    public static final String COLLATERALIZED_TRADES_REPORT = "PortfolioReconciliationReport";
    public static final String ROW_PROPERTY_ID = "PortfolioReconciliationReport";
    public static final String NO_VALUEDATE_ERROR_MESSAGE = "No valuation Date read from the template. Please check the configuration";
    private final static String CLASS_NAME = PortfolioReconciliationReport.class.getCanonicalName();
    private final static String ERROR_RESULT = "Not document generated";
    private final static String PRICING_ENV = "DirtyPrice";

    // class variables
    private JDate processDate;
    private SantGenericQuotesLoader quotesLoader; // thread 1
    private SantGenericPLMarksLoader plMarksLoader;// thread 2
    private final static long SLEEP_THREAD = 500;

    /**
     * Override method load to generate the file (the report).
     *
     * @param errorsMsgs passed by parameter
     * @return the ReportOutput to generate the report
     */
    @SuppressWarnings("unchecked")
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") Vector vector) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        List<ReportRow> reportRowsList = null;
        List<CollateralizedTradesDataWrapper> processingList = null;

        Log.debug(CLASS_NAME, "1. PortfolioReconciliation report started.");
        /* control check and retrieve the data from the template of the schedule task */
        if (!readAndCheckTemplateAttributes(vector)) {

            Log.error(CLASS_NAME, "Template Attributes has NOT been received"); // log messages
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, ERROR_RESULT);// CONTROL-M
            return output;
        }

        // independent thread 1 for fetching the FX quotes
        startFetchingFXQuotesThread();

        // retrieve the Derivative operations
        final List<MarginCallDetailEntryDTO> mCDEntriesList = CollateralizedTradesReportLogic.retriveMCAliveTradesList(
                vector, this.processDate); // retrieve all MCDetailEntries alive -> ExposureTrades

        Log.debug(CLASS_NAME, "2. Margin call detail entries for derivates and Quotes obtained for Date "
                + this.processDate.toString() + "Thread for Fetching FX quotes started");
        CollateralizedTradesReportLogic logic = new CollateralizedTradesReportLogic();
        Log.debug(CLASS_NAME, "3. Gathering Contracts and trades for the Margin call detail entries list.");

        Map<Long, SantTradeBrowserItem> tradeDataBrowserMap = logic
                .buildListOfTradeContractAndMCDetailEntryDTOFromList(mCDEntriesList);

        // independent thread 2 for fetching the PL Marks
        startPLMarkLoaderThread(tradeDataBrowserMap);
        Log.debug(CLASS_NAME, "4. All data retrieved and Thread two started to fech PLMarks. Formatting rows");

        processingList = new ArrayList<CollateralizedTradesDataWrapper>(tradeDataBrowserMap.keySet().size());
        reportRowsList = new ArrayList<ReportRow>(tradeDataBrowserMap.keySet().size());
        Log.debug(CLASS_NAME, "5. Retriving data independent of threads");

        // data independence first generation
        for (SantTradeBrowserItem entry : tradeDataBrowserMap.values()) {

            final CollateralizedTradesDataWrapper temp = new CollateralizedTradesDataWrapper();
            temp.itemData = entry;
            temp.currentMapData = logic.formatRowThreadIndependent(entry, this);
            processingList.add(temp);
        }

        tradeDataBrowserMap = null; // no more needed
        Log.debug(CLASS_NAME,
                "5. Independent threads data retrieved. Obtaining dependent data if threads have finished.");
        waitToThreadsPlMarkAndFXQuotes();

        // process again the rows, now with the thread dependency data. At this point thread have finished.
        for (CollateralizedTradesDataWrapper rowData : processingList) {

            logic.formatRowThreadsDependancy(rowData, this);
            final Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> collateralTrade = rowData.currentMapData;
            final ReportRow repRow = new ReportRow(rowData);
            repRow.setProperty(ROW_PROPERTY_ID, collateralTrade);
            reportRowsList.add(repRow);
        }

        // set output rows
        output.setRows(reportRowsList.toArray(new ReportRow[0]));

        Log.debug(CLASS_NAME, "5. PortfolioReconciliation report generated.");

        return output;
    }

    /**
     * Starts the thread to load the PLMarks from the system.
     *
     * @param tradeDataBrowserList , including a list of candidates to read the PLMarks
     */
    private void startPLMarkLoaderThread(Map<Long, SantTradeBrowserItem> tradeDataBrowserList) {

        // 3. Load PLMarks
        Set<JDate> valDateSet = new HashSet<>();
        valDateSet.add(getValDate());
        this.plMarksLoader = new SantGenericPLMarksLoader(true, tradeDataBrowserList, valDateSet, PRICING_ENV);
        // start the thread
        this.plMarksLoader.load();
    }

    /**
     * Starts the thread to load the FX Quotes from the system.
     */
    private void startFetchingFXQuotesThread() {

        // load quotes
        List<String> quotesWhereList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(
                " and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ").append(
                Util.date2SQLString(getValDate()));

        quotesWhereList.add(sb.toString());

        this.quotesLoader = new SantGenericQuotesLoader(true, quotesWhereList);
        // start the thread
        this.quotesLoader.load();
    }

    /**
     * Checks is the thread is working, if is the case, sleeps main thread for SLEEP_THREAD
     */
    private void waitToThreadsPlMarkAndFXQuotes() {

        while (this.plMarksLoader.isAlive() || this.quotesLoader.isAlive()) {
            try {
                Thread.sleep(SLEEP_THREAD);// just wait...
            } catch (InterruptedException e) {
                Log.error(CLASS_NAME, e);
            }
        }
    }

    /**
     * @return the SantGenericPLMarksLoader instance
     */
    public SantGenericPLMarksLoader getPLMarkLoader() {

        return this.plMarksLoader;
    }

    /**
     * @return the quotesLoader instance
     */
    public SantGenericQuotesLoader getQuotesLoader() {

        return this.quotesLoader;
    }

    /**
     * Reads all the attributes passed through the schedule task template, acts as a constructor of all the require
     * class variables and finally it checks that all the necessary data for constructing the data is available
     *
     * @param vector
     * @return true is all the data is available, false othercase
     */
    // All attributes to be checked HERE!!
    private boolean readAndCheckTemplateAttributes(Vector<String> vector) {

        @SuppressWarnings("unused") final Attributes attributes = getReportTemplate().getAttributes();

        // We get the valuation date to put in the export properly.
        this.processDate = getValDate(); // getReportTemplate().getValDate();

        if (this.processDate == null) {
            vector.add(NO_VALUEDATE_ERROR_MESSAGE);
            this.processDate = getValDate();
            Log.error(CLASS_NAME,
                    NO_VALUEDATE_ERROR_MESSAGE + ". Putting today as Process date " + this.processDate.toString());
            // return false;
        }

        // check conditions necessary to continue with the processing
        return true;

    }

    /**
     * Reads from the template the process date. If this value is null, it will take the current date.
     */
    @Override
    protected JDate getValDate() {

        // return CollateralUtilities.getJDate("10/04/2013"); //tests
        if (this.processDate != null) {
            return this.processDate;
        }

        JDate valDate = getReportTemplate().getValDate();

        if (valDate == null) { // not read from the template.
            valDate = JDate.getNow();

        }
        return valDate;

    }

    /* inner class containing SantTradeBrowserItem and the current columns map */
    class CollateralizedTradesDataWrapper {

        protected SantTradeBrowserItem itemData;
        protected Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> currentMapData;

        /**
         * @return the itemData
         */
        public SantTradeBrowserItem getItemData() {
            return this.itemData;
        }

        /**
         * @param itemData the itemData to set
         */
        public void setItemData(SantTradeBrowserItem itemData) {
            this.itemData = itemData;
        }

        /**
         * @return the currentMapData
         */
        public Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> getCurrentMapData() {
            return this.currentMapData;
        }

        /**
         * @param currentMapData the currentMapData to set
         */
        public void setCurrentMapData(Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> currentMapData) {
            this.currentMapData = currentMapData;
        }

    }

}

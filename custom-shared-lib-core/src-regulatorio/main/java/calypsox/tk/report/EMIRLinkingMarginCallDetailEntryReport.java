package calypsox.tk.report;

import calypsox.regulation.util.EmirUtil;
import calypsox.tk.collateral.manager.worker.impl.FullLoadTaskWorker;
import calypsox.tk.util.interfaceImporter.SantDerivativeTradesLoader;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantCalypsoUtilities;
import calypsox.util.SantReportingUtil;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallDetailEntryReport;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * EMIR Linking report, full or delta version.
 * Full: recover all the exposure collateralized trades associated to CSA contracts for certain branches
 * Delta: only recovers trades that have changed the assigned CollateralContract or CollateralConfigs which EMIR additional field
 * has changed (OneWay, partially, Fully).
 *
 * @author Rafael Nishizaki & Tetyana Yavorska
 * @version 1.1
 */
public class EMIRLinkingMarginCallDetailEntryReport extends MarginCallDetailEntryReport implements CheckRowsNumberReport {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -2653598214389427013L;

    private static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    
    private static final String DV_EMIR_LINKING_MODIF_KEYS = "EmirLinkingModifKeys";

    /*
     * Process Date, introduced by date in Panel
     */
    private JDate processDate;

    /**
     * ID type introduced in Panel
     */
    private String submitterValue;

    private String groupingReport;

    private String replaceOwner;

    private String reportType;

    /*
     * Delta report
     */
    //private List<Integer> tradeIds = new ArrayList<Integer>();

    /*
     * Saves DV parameterization of used
     */
    private static Map<String, String> dvConfiguration = null;

    /**
     * Each collection of rows to be processed from MCEntries uses as many available processors in the system
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    /*
     * Default for DS threads
     */
    private static final int NUM_DS_THREADS = NUM_CORES;

    private static final String REPORT_TYPE_ERROR = "Linking Report Type field is mandatory.";

    private static final int SQL_IN_ITEM_COUNT = 999;

    private boolean isDelta = false;
    /*
     * These variables are a little bit tricky. Its a Map of {contractsIDs, List of tradesIDs} that are new or have changed. Is required to notify
     * if a Trade is NEW in the system or MODIFIED because its keyword MC_CONTRACT_NUMBER has changed
     */
    private Map<Integer, List<Long>> newTradesContract = null;
    private Map<Integer, List<Long>> modifiedTradesContract = null;

    /*
     * Alternative configurations by DV
     */
    public enum CONFIGURATIONS {
        SKIP_ERRORS("No_Errors"), //will disable errors
        NUMBER_THREADS_MCENTRY("Thread_number_mce"), //number of thread to recover MCEntries, 4 default
        NUMBER_THREADS_MCPOS("Thread_number_mcpos"), //number of thread to recover MC Positions, defaul number processors
        CHUNK_SIZE("Chunk_size"); // number of contracts per thread, default 999

        private String name;

        CONFIGURATIONS(final String n) {
            this.name = n;
        }

        public String getName() {
            return name;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector errors) {

        /**
         * Check the mandorory fields
         */
        if (!mandatoryFields(errors))
            return null;

        // recovers DV configurations
        final DomainValuesThread dvThread = new DomainValuesThread();
        dvThread.start();

        // avoid problems adding errors by independent threads
        final List<String> errorList = Collections.synchronizedList(errors);
        final List<ReportRow> rows = Collections.synchronizedList(new ArrayList<ReportRow>());

        List<Integer> panelContractsIds = new ArrayList<Integer>();

        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        //get the CM filter based on the template values
        final MarginCallConfigFilter mcFilter = buildMCContractsFilter();

        //build MC Report template
        final MarginCallReportTemplate cmTemplate = buildReportTemplate();

        try {
            panelContractsIds = CollateralUtilities.getAllMarginCallContractIds(mcFilter);
            dvThread.join();
            // if used, stores configuration in DV Named SACCRCMBalancesReport
            dvConfiguration = dvThread.getDomainValuesAndComments();

        } catch (CollateralServiceException | InterruptedException e) {
            String mess = "ERROR: Executor in getAllMarginCallContractIds error: ";
            Log.error(this, mess + e);
            errorList.add(mess + e.getLocalizedMessage());

        } catch (Exception e2) {
            Log.error(this, e2);
            errorList.add("ERROR: Cannot gather contracts. Check other Filter if using one");
        }

        List<MarginCallDetailEntry> mcDetailEntries = new ArrayList<MarginCallDetailEntry>();

        /**
         * If is Delta Report, filter using Audit
         */
        /////////////////
        // DELTA VERSION
        /////////////////
        if (isDelta()) {

            //Si un contrato ha cambiado o es nuevo -> debe mostrar todas sus MCDetailEntries
            //si una trade ha cambiado, solo debe mostrar esa trade Entry
            //ojo, si la trade es nueva, debe reportarse como new, en cualquiera de los dos casos anteriores y solo una vez.

            isDelta = true;
            List<Integer> contractsModifiedIds = new ArrayList<Integer>();

            //recover thread 1 audit, contracts that have changed
            ContractsAuditThread modifiedContractsThread = new ContractsAuditThread(panelContractsIds);
            modifiedContractsThread.start();

            //recover thread 2 audit, new trades
            NewTradesAuditThread newTradesThread = new NewTradesAuditThread(panelContractsIds);
            newTradesThread.start();

            //recover thread 3 audit, modify trades (different assigned contracts)
            ModifiedTradesAuditThread modifiedTradesThread = new ModifiedTradesAuditThread(panelContractsIds);
            modifiedTradesThread.start();

            try {
                modifiedContractsThread.join();

                //NEW CONTRACTS OR CHANGES: Recover contracts, MCEntries & MCDetailEntries
                contractsModifiedIds = modifiedContractsThread.getContractsModifiedIds();

                final List<MarginCallEntry> contractsMCEntries = workersMCEntriesRecover(contractsModifiedIds, cmTemplate, errorList);
                List<MarginCallDetailEntry> contractsMCDetailEntries = getDetailEntries(contractsMCEntries, errorList);

                //add details entries to contracts that have changed or are new
                mcDetailEntries.addAll(contractsMCDetailEntries);

                //we get the difference - contracts from panel - contracts of audit
                contractsModifiedIds = this.difference(panelContractsIds, contractsModifiedIds);

                modifiedTradesThread.join();
                newTradesThread.join();

                /*
                 * NEW TRADES OR CHANGES: recover new trades, recover changed trades, MCEntries & MCDetailsEntries
                 */
                modifiedTradesContract = modifiedTradesThread.getChangedTradesContract();
                newTradesContract = newTradesThread.getNewTradesContract();

                //intersection with contracts of trades
                List<Integer> idsContractsOfTradesToSearch = new ArrayList<>(newTradesContract.keySet());
                idsContractsOfTradesToSearch.addAll(modifiedTradesContract.keySet());
                //if it has been added by a contract change, the contract of the trade should be discarded
                contractsModifiedIds = this.intersection(contractsModifiedIds, idsContractsOfTradesToSearch);

                //recover MCEntries for contracts of trades that have change or are new (only)
                final List<MarginCallEntry> tradesMCEntries = workersMCEntriesRecover(contractsModifiedIds, cmTemplate, errorList);
                List<MarginCallDetailEntry> tradesMCDetailEntries = getDetailEntries(tradesMCEntries, errorList);
                //clean MCDetailsEntries which trades does not match with Trades id changes
                tradesMCDetailEntries = filterByModifiedTrade(tradesMCEntries, newTradesContract, modifiedTradesContract, errorList);

                mcDetailEntries.addAll(tradesMCDetailEntries);


            } catch (InterruptedException e) {
                Log.error(this, e);
            }

            /////////////////
            // FULL VERSION
            /////////////////

        } else {

            isDelta = false;
            //incluir filas de trades CANCEL en el d?a por el AUDIT. // Descartado, solo en el viejo FULL
            final List<MarginCallEntry> mcEntries = workersMCEntriesRecover(panelContractsIds, cmTemplate, errorList);
            mcDetailEntries = getDetailEntries(mcEntries, errorList);
        }

        /**
         *  Create rows
         */

        final ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCPOS, errorList);

        try {
            // Multithread execution, one contract per core
            for (final MarginCallDetailEntry detailEntry : mcDetailEntries) {

                taskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {

                        //current row
                        final Collection<ReportRow> threadColRows = buildRows(detailEntry, errorList);

                        if (!threadColRows.isEmpty())
                            rows.addAll(threadColRows);
                    }

                });
            }
        } finally {
            taskExecutor.shutdown();
            // important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.error(SACCRCMPositionReport.class, "FAIL: Thread interruption Service: " + e);
                errorList.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            }
        }

        // if option activate, dont care errors occurred, it will send an OK to
        // Scheduler
        if (Util.isTrue(dvConfiguration.get(CONFIGURATIONS.SKIP_ERRORS.name)))
            errors.clear();
        else if (!errorList.isEmpty()) // attach to errors
            errors.addAll(errorList);


        List<ReportRow> resultRows = EMIRReportLogic.discardRows(rows);
        reportOutput.setRows(resultRows.toArray(new ReportRow[0]));

        //Generate a task exception if the number of rows is out of an umbral defined
        HashMap<String , String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        checkAndGenerateTaskReport(reportOutput, value);

        return reportOutput;
    } //end

    /**
     * @param tradesMCEntries
     * @param newTradesContract
     * @param modifiedTradesContract
     * @param errorList
     * @return details entries for trades that are new in the system or which keyword MC_CONTRACT_NUMBER has changed
     */
    private List<MarginCallDetailEntry> filterByModifiedTrade(final List<MarginCallEntry> tradesMCEntries,
                                                              final Map<Integer, List<Long>> newTradesContract, final Map<Integer, List<Long>> modifiedTradesContract, List<String> errorList) {


        final List<MarginCallDetailEntry> dEntries = Collections.synchronizedList(new ArrayList<MarginCallDetailEntry>());
        final ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCPOS, errorList);

        try {
            // Multithread execution, one contract per core
            for (final MarginCallEntry marginCallEntry : tradesMCEntries) {

                taskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {

                        Integer contractId = marginCallEntry.getCollateralConfigId();

                        if (newTradesContract.containsKey(contractId) || modifiedTradesContract.containsKey(contractId)) {

                            int count = 0;
                            final List<Long> tradesIdsNew = newTradesContract.get(contractId); //list of trades for this contract that are new
                            final List<Long> tradesIdsModified = modifiedTradesContract.get(contractId); //idem but modified
                            int size = tradesIdsNew != null ? newTradesContract.get(contractId).size() : 0;
                            size += tradesIdsModified != null ? modifiedTradesContract.get(contractId).size() : 0;


                            for (MarginCallDetailEntry mcde : marginCallEntry.getDetailEntries()) {

                                if (tradesIdsNew != null && tradesIdsNew.contains(mcde.getTradeId())) {
                                    dEntries.add(mcde);
                                    count++;

                                } else if (tradesIdsModified != null && tradesIdsModified.contains(mcde.getTradeId())) {
                                    dEntries.add(mcde);
                                    count++;
                                }
                                //found all trades IDs in MCEntries, stop iterating
                                if (count >= size)
                                    continue;
                            } //end for
                        }
                    }
                });
            }
        } finally {
            taskExecutor.shutdown();
            // important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.error(SACCRCMPositionReport.class, "FAIL: Thread interruption Service: " + e);
                errorList.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            }
        }

        return dEntries;
    }


    /*
     * Set difference
     */
    private List<Integer> difference(List<Integer> setA, List<Integer> setB) {

        Set<Integer> result = new TreeSet<>(setA);
        Set<Integer> b = new TreeSet<>(setB);
        result.removeAll(b);
        return new ArrayList<>(result);
    }

    /*
     * Set intersection
     */
    private List<Integer> intersection(List<Integer> setA, List<Integer> setB) {

        Set<Integer> a = new TreeSet<>(setA);
        Set<Integer> b = new TreeSet<>(setB);

        Set<Integer> result = new TreeSet<>();
        for (Integer id : a) {
            if (b.contains(id))
                result.add(id);
        }

        return new ArrayList<>(result);
    }


    private boolean isDelta() {
        final String type = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.REPORT_TYPES);
        if (type.equalsIgnoreCase("delta")) {
            return true;
        }
        return false;
    }

    private JDatetime getStartTime(JDate jdate) {
        return new JDatetime(jdate, 0, 0, 0, TimeZone.getDefault());
    }

    private JDatetime getEndTime(JDate jdate) {
        return new JDatetime(jdate, 23, 59, 59, TimeZone.getDefault());
    }

//	private List<MarginCallDetailEntry> filterByModifiedTrade(List<MarginCallDetailEntry> entries){
//		List<MarginCallDetailEntry> result = new ArrayList<MarginCallDetailEntry>();
//		for(MarginCallDetailEntry entry : entries){
//			if(tradeIds.contains(entry.getTradeLongId())){
//				result.add(entry);
//			}
//		}
//		return result;
//	}

    /**
     * @param detailEntry
     * @return the trade if the type is CollateralExposure
     */
    private Trade checkTradeType(final MarginCallDetailEntry detailEntry) {
        if (detailEntry == null) return null;
        long tradeID = detailEntry.getTradeId();
        if (tradeID > 0) {
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeID);
                if (trade.getProduct() instanceof CollateralExposure || trade.getProduct() instanceof PerformanceSwap) {
                    return trade;
                }
            } catch (CalypsoServiceException e) {
                String mess = "ERROR: Failed to recover trade (" + tradeID + "): ";
                Log.error(this, mess + e.getMessage());
                Log.error(this, e); //sonar purpose
            }
        }
        return null;
    }


    /**
     * @param detailEntry
     * @param errorList
     * @return valid row for FULL LINKING version
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Collection<ReportRow> buildRows(final MarginCallDetailEntry detailEntry, final List<String> errorList) {

        Vector v = new Vector();
        Trade trade = checkTradeType(detailEntry);
        if (trade != null) {
            ReportRow row = new ReportRow(detailEntry.toDTO());
            MarginCallEntry entry = detailEntry.getMarginCallEntry();
            CollateralConfig config = entry.getCollateralConfig();
            String submitter = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT);

            row.setProperty(EmirUtil.TRADE_NAME, trade);
            row.setProperty(EmirUtil.MARGIN_CALL_DETAIL_ENTRY_NAME, detailEntry.toDTO());
            //too heavy, remove.
            //row.setProperty(EmirUtil.MARGIN_CALL_ENTRY_NAME, entry.toDTO());
            row.setProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME, config);
            row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT, submitter);
            row.setProperty(EmirUtil.PROCESSDATE, processDate);
            row.setProperty(EmirUtil.ISDELTA, isDelta);
            /*
             * Logic to ensure that a Trade that is new but has been modified during to process date is announced as NEW
             */
            if (isDelta) {

                row.setProperty(EmirUtil.TRADE_ACTION, EMIRReportLogic.ACTION_VALUE_MODIF);

                if (!this.newTradesContract.isEmpty()) {
                    if (this.newTradesContract.containsKey(config.getId())) {
                        if (this.newTradesContract.get(config.getId()).contains(trade.getLongId()))
                            row.setProperty(EmirUtil.TRADE_ACTION, EMIRReportLogic.ACTION_VALUE_NEW);
                    }
                }
            }

            String group = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES);
            if (!Util.isEmpty(group)) {
                String replace = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME);
                Vector<String> namesLE = Util.string2Vector(group);
                LegalEntity le = BOCache.getLegalEntity(getDSConnection(), replace.trim());
                row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES, namesLE);
                row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME, le);
            }

            v.add(row);
        }

        return v;
    }

    private List<MarginCallDetailEntry> getDetailEntries(List<MarginCallEntry> mcEntries, List<String> errorList) {

        final List<MarginCallDetailEntry> dEntries = Collections.synchronizedList(new ArrayList<MarginCallDetailEntry>());
        final ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCPOS, errorList);

        try {
            // Multithread execution, one contract per core
            for (final MarginCallEntry marginCallEntry : mcEntries) {

                taskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        dEntries.addAll(marginCallEntry.getDetailEntries());
                    }
                });
            }
        } finally {
            taskExecutor.shutdown();
            // important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.error(SACCRCMPositionReport.class, "FAIL: Thread interruption Service: " + e);
                errorList.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
                return null;
            }
        }

        return dEntries;
    }

    /**
     * @return filter obtained from template configurations
     */
    private MarginCallConfigFilter buildMCContractsFilter() {
        final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        List<Integer> ownerIds = new ArrayList<>();
        if (!Util.isEmpty(ownersNames)) {
            ownerIds = getIdsLe(ownersNames);
        }
        final String agreementIds = (String) getReportTemplate().get(EMIRValuationMCReportTemplate.AGREEMENT_ID);
        List<Integer> contractIds = new ArrayList<>();
        if (!Util.isEmpty(agreementIds)) {
            contractIds = Util.string2IntVector(agreementIds);
        }
        final String typeAgrString = (String) getReportTemplate().get(EMIRValuationMCReportTemplate.CONTRACT_TYPE);
        List<String> typesAgr = new ArrayList<>();
        if (!Util.isEmpty(typeAgrString)) {
            typesAgr = Util.string2Vector(typeAgrString);
        }
        return SantMCConfigFilteringUtil.getInstance().getInstance().buildMCConfigFilter(this.processDate, contractIds, ownerIds, typesAgr);
    }

    /**
     * @return generic MarginCallReportTemplate
     */
    private MarginCallReportTemplate buildReportTemplate() {

        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, this.processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                Boolean.FALSE);

        return template;
    }

    /**
     * @param errors
     * @return true if all mandatory filters in panel are included
     */
    private boolean mandatoryFields(final Vector<String> errors) {
        initializeFields();
        errors.clear();
        EmirUtil.checkReportTemplate(getReportTemplate(), errors);
        EmirUtil.computeProcessDate(this.processDate, errors);
        EmirUtil.checkSubmitterReportField(this.submitterValue, errors);
        checkReportType(errors);
        EmirUtil.checkGroupingReport(this.groupingReport, this.replaceOwner, errors);
        return Util.isEmpty(errors);
    }

    private void initializeFields() {
        this.processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        this.submitterValue = getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT);
        this.reportType = getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.REPORT_TYPES);
        this.groupingReport = getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES);
        this.replaceOwner = getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME);
    }

    private void checkReportType(final Vector<String> errors) {
        if (Util.isEmpty(reportType)) {
            EmirUtil.addErrors(errors, REPORT_TYPE_ERROR);
        }
    }

    /**
     * @param leNames
     * @return list of ids of the LE
     */
    private List<Integer> getIdsLe(final String leNames) {

        Vector<String> namesLE = Util.string2Vector(leNames);
        ArrayList<Integer> leIds = new ArrayList<Integer>();
        if (!Util.isEmpty(namesLE)) {

            for (String name : namesLE) {
                final LegalEntity le = BOCache.getLegalEntity(getDSConnection(), name.trim());
                if (le != null)
                    leIds.add(le.getId());
            }
            return leIds;

        }
        return leIds;
    }

    /**
     * @param contractsIds
     * @param template
     * @param errorList
     * @return list of mc entries from the contracts ids
     */
    private List<MarginCallEntry> workersMCEntriesRecover(final List<Integer> contractsIds, final MarginCallReportTemplate template, List<String> errorList) {

        ExecutionContext context = ExecutionContext.getInstance(
                ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), template);

        context.setProcessDate(processDate);

        int chunkSize = SantDerivativeTradesLoader.SQL_IN_ITEM_COUNT;
        try {
            String size = dvConfiguration.get(CONFIGURATIONS.CHUNK_SIZE);
            if (!Util.isEmpty(size)) {
                chunkSize = Integer.parseInt(size);
            }

        } catch (Exception e) {
            Log.error(this, e);
        }

        /*
         * Split contract ids in chunks for each worker
         */
        final List<Integer[]> list = TradeInterfaceUtils.splitArray(contractsIds.toArray(new Integer[]{}), chunkSize);
        final Iterator<Integer[]> contractIdsChunks = list.iterator();

        /*
         *Result List, sum of list per each worker
         */
        final List<MarginCallEntry> resultEntries = Collections.synchronizedList(new ArrayList<MarginCallEntry>());

        Integer numberThreads = getNumberOfThreads(EMIRLinkingMarginCallDetailEntryReport.CONFIGURATIONS.NUMBER_THREADS_MCENTRY, errorList);
        CollateralTaskWorker worker[] = new FullLoadTaskWorker[numberThreads];

        /**
         * Logic to create workers based on number of threads & chunks available
         */
        while (contractIdsChunks.hasNext()) {

            ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCENTRY, errorList);
            List<List<MarginCallEntry>> entriesLists = new ArrayList<List<MarginCallEntry>>(numberThreads);

            for (int i = 0; i < numberThreads; i++) {
                worker[i] = null;
                entriesLists.add(i, new ArrayList<MarginCallEntry>());
                if (contractIdsChunks.hasNext()) {

                    final ExecutionContext contextChunk = context.clone();
                    final MarginCallConfigFilter mccFilter = new MarginCallConfigFilter();
                    //next set of contracts
                    mccFilter.setContractIds(Arrays.asList(contractIdsChunks.next()));
                    contextChunk.setFilter(mccFilter);

                    worker[i] = new FullLoadTaskWorker(contextChunk, entriesLists.get(i));
                    taskExecutor.submit(worker[i]);
                }
            }

            //wait threads
            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                String mess = "ERROR: Executor in recoverMCEntriesWorkers error: ";
                Log.error(this, mess + e.getMessage());
                errorList.add(mess + e.getLocalizedMessage());

            }
            //append to common list
            for (List<MarginCallEntry> entryThread : entriesLists) {
                if (!entryThread.isEmpty()) {
                    resultEntries.addAll(entryThread);
                }
            }

        } //end chunks

        //return entries from all workers
        return resultEntries;
    }

    /**
     * @param option
     * @param errorList
     * @return execute configurations based on option selection
     */
    private ExecutorService getFixedThreadPool(CONFIGURATIONS option, List<String> errorList) {
        return Executors.newFixedThreadPool(getNumberOfThreads(option, errorList));
    }

    /**
     * @param option
     * @param errorList
     * @return number of threads based on option selection
     */
    private Integer getNumberOfThreads(CONFIGURATIONS option, List<String> errorList) {

        int numberThreads = NUM_DS_THREADS;
        if (option.equals(CONFIGURATIONS.NUMBER_THREADS_MCPOS))
            numberThreads = NUM_CORES;

        try {
            if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS) != null && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCPOS)) {
                numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS));

            } else if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY) != null && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCENTRY)) {
                numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY));
            }


        } catch (Exception e) {
            final String message = "\n Error reading number in getFixedThreadPool: " + e.getLocalizedMessage();
            Log.error(this, e + message);
            errorList.add(message);
        }
        return numberThreads;

    }

    /**
     * Thread to run the read of DomainValues configuration. These configuration tend to be general for all
     */
    private class DomainValuesThread extends Thread {

        private Map<String, String> dvMap;

        DomainValuesThread() {
            this.dvMap = null;
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative configurations are used.
         */
        @Override
        public void run() {

            String domainName = SACCRCMPositionReportTemplate.class.getSimpleName();
            this.dvMap = CollateralUtilities.initDomainValueComments(domainName);
            domainName += ".";

            for (CONFIGURATIONS t : CONFIGURATIONS.values()) {
                this.dvMap.putAll(CollateralUtilities.initDomainValueComments(domainName + t.getName()));
            }
        }

        public Map<String, String> getDomainValuesAndComments() {
            return this.dvMap;
        }
    }


    /**
     * Thread to recover from AUDIT contracts changes IN AD EMIR_LINKING_VALUE
     */
    private class ContractsAuditThread extends Thread {

        private List<Integer> contractsModifiedIds = new ArrayList<Integer>();
        private List<Integer> contractsIds;

        ContractsAuditThread(final List<Integer> contractsIdsList) {
            this.contractsModifiedIds = new ArrayList<Integer>();
            this.contractsIds = contractsIdsList;
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative configurations are used.
         */
        @Override
        public void run() {

            this.contractsModifiedIds = getModifiedContractIDs(this.contractsIds); //ok

        }

        public List<Integer> getContractsModifiedIds() {
            return contractsModifiedIds;
        }
    }

    @SuppressWarnings("rawtypes")
    private List<Integer> getModifiedContractIDs(List<Integer> mmcIdList) {
        Set<Integer> ids = new HashSet<Integer>();
        if (mmcIdList.isEmpty()) {
            return new ArrayList<Integer>();
        }

        List<Integer[]> list = TradeInterfaceUtils.splitArray(mmcIdList.toArray(new Integer[]{}), SQL_IN_ITEM_COUNT);
        Iterator<Integer[]> ite = list.iterator();
        while (ite.hasNext()) {

            List<Integer> sublistConfigIds = Arrays.asList(ite.next());

            try {
                SQLQuery queryMod = buildModifiedContractQuery(sublistConfigIds);
                Vector audits = DSConnection.getDefault().getRemoteTrade().getAudit(queryMod.getWhereClause(), null, null);

                for (Object o : audits) {
                    if (o instanceof AuditValue) {
                        ids.add(((AuditValue) o).getEntityId());
                    }
                }

                SQLQuery queryNew = buildNewContractQuery(sublistConfigIds);
                audits = DSConnection.getDefault().getRemoteTrade().getAudit(queryNew.getWhereClause(), null, null);

                for (Object o : audits) {
                    if (o instanceof AuditValue) {
                        ids.add(((AuditValue) o).getEntityId());
                    }
                }

            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }

        return new ArrayList<Integer>(ids);
    }

    /**
     * @param mmcIdList
     * @return audit query to recover contracts which AF EMIR_COLLATERAL_VALUE has changed
     */
    private SQLQuery buildModifiedContractQuery(List<Integer> mmcIdList) {
        SQLQuery query = new SQLQuery();
        query.appendWhereClause("entity_class_name IN ('CollateralConfig', 'MarginCallConfig')");
        query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(this.processDate)));
        //query.appendWhereClause("modif_date<=" + Util.datetime2SQLString(getEndTime(this.processDate)));
        query.appendWhereClause("entity_field_name = '__additionalFields'"); //or entity_field_name = '_CREATE_'
        query.appendWhereClause("old_value like 'EMIR_COLLATERAL_VALUE%'");

        StringBuilder whereClause = new StringBuilder();
        whereClause.append("entity_id IN (");
        whereClause.append(Util.collectionToString(mmcIdList));
        whereClause.append(")");

        query.appendWhereClause(whereClause.toString());
        return query;
    }

    /**
     * @param mmcIdList
     * @return audit query to recover new contracts
     */
    private SQLQuery buildNewContractQuery(List<Integer> mmcIdList) {
        SQLQuery query = new SQLQuery();
        query.appendWhereClause("entity_class_name IN ('CollateralConfig', 'MarginCallConfig')");
        query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(this.processDate)));
        query.appendWhereClause("entity_field_name = '_CREATE_'");

        StringBuilder whereClause = new StringBuilder();
        whereClause.append("entity_id IN (");
        whereClause.append(Util.collectionToString(mmcIdList));
        whereClause.append(")");

        query.appendWhereClause(whereClause.toString());
        return query;
    }

    /**
     * Thread to recover from AUDIT new trades
     * (KEYWORD MC_CONTRACT_NUMBER)
     */
    private class NewTradesAuditThread extends Thread {

        private List<Integer> contractsIds;
        private Map<Integer, List<Long>> newTradesContract;

        NewTradesAuditThread(final List<Integer> contractsIdsList) {
            this.contractsIds = contractsIdsList;
            this.newTradesContract = new HashMap<>();
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative configurations are used.
         */
        @Override
        public void run() {

            //recover trades that has entered the system (new ones)
            List<Long> newtradeIds = getAuditNewTradeIDs();
            newTradesContract = buildTradesContracts(newtradeIds);
        }


        public Map<Integer, List<Long>> getNewTradesContract() {
            return newTradesContract;
        }

    }


    /**
     * Thread to recover from AUDIT  modified assigned trades to contracts (KEYWORD MC_CONTRACT_NUMBER)
     */
    private class ModifiedTradesAuditThread extends Thread {

        private List<Integer> contractsIds;
        private Map<Integer, List<Long>> changedTradesContract;

        ModifiedTradesAuditThread(final List<Integer> contractsIdsList) {
            this.contractsIds = contractsIdsList;
            this.changedTradesContract = new HashMap<>();
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative configurations are used.
         */
        @Override
        public void run() {

            //recover trades that has been modified the keyword
            List<Long> modifiedtradeIds = getAuditModifiedTradeIDs(contractsIds);
            changedTradesContract = buildTradesContracts(modifiedtradeIds);

        }


        public Map<Integer, List<Long>> getChangedTradesContract() {
            return changedTradesContract;
        }

    }

    //methods to recover and process data from trades audit
    private Map<Integer, List<Long>> buildTradesContracts(List<Long> ntradeIds) {

        Map<Integer, List<Long>> contractsTradesMap = new HashMap<>();
        TradeArray existingTrades = null;
        long[] tradesIdArray = new long[ntradeIds.size()];
        int i = 0;
        if (ntradeIds.isEmpty())
            return contractsTradesMap;

        for (Long id : ntradeIds) {
            tradesIdArray[i] = id;
            i++;
        }
        try {
            existingTrades = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(tradesIdArray);
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        for (Trade trade : existingTrades.getTrades()) {
            if (trade.getKeywordValue(MC_CONTRACT_NUMBER) != null && !trade.getKeywordValue(MC_CONTRACT_NUMBER).isEmpty()) {
                try {
                    Integer contractId = Integer.valueOf(trade.getKeywordValue(MC_CONTRACT_NUMBER).trim());
                    if (!contractsTradesMap.containsKey(contractId)) {
                        contractsTradesMap.put(contractId, new ArrayList<>());
                        contractsTradesMap.get(contractId).add(trade.getLongId());
                    } else {
                        contractsTradesMap.get(contractId).add(trade.getLongId());
                    }

                } catch (NumberFormatException e) {
                    Log.error(this, e);
                }
            }
        }

        return contractsTradesMap;
    }

    @SuppressWarnings("rawtypes")
    private List<Long> getAuditModifiedTradeIDs(List<Integer> mmcIdList) {
        List<Long> ids = new ArrayList<>();
        if (mmcIdList.isEmpty()) {
            return ids;
        }

        List<Integer[]> list = TradeInterfaceUtils.splitArray(mmcIdList.toArray(new Integer[]{}), SQL_IN_ITEM_COUNT);
        Iterator<Integer[]> ite = list.iterator();
        while (ite.hasNext()) {
            SQLQuery query = buildModifiedTradeQuery(Arrays.asList(ite.next()));
            addTradeIdsFromAuditValues(ids, query);
        }
        return ids;
    }

    /**
     * @return
     */
    private List<Long> getAuditNewTradeIDs() {
        List<Long> ids = new ArrayList<>();
        SQLQuery query = buildNewTradeQuery();
        addTradeIdsFromAuditValues(ids, query);
        return ids;
    }

    private void addTradeIdsFromAuditValues(List<Long> ids, SQLQuery query) {
        try {
            Vector audits = DSConnection.getDefault().getRemoteTrade().getAudit(query.getWhereClause(), null, null);
            for (Object o : audits) {
                if (o instanceof AuditValue) {
                    ids.add(((AuditValue) o).getEntityLongId());
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
    }


	private SQLQuery buildNewTradeQuery() {

		// By default, the date used for the query is the pocessDate
		JDate queryDate = this.processDate;

		// It gets the before day between Monday and Friday
		JDate weekDayBefore = CollateralUtilities
				.getMonFridayBefore(this.processDate);
		// If the weekDayBefore is festive in the report Calendar it will be
		// used in the query from Audit
		if (!CollateralUtilities.isBusinessDay(weekDayBefore,
				getReportTemplate().getHolidays())) {
			queryDate = weekDayBefore;
		}

		SQLQuery query = new SQLQuery();
		query.appendWhereClause("entity_class_name = 'Trade'");
		query.appendWhereClause("modif_date>="
				+ Util.datetime2SQLString(getStartTime(queryDate)));
		// query.appendWhereClause("modif_date<=" +
		// Util.datetime2SQLString(getEndTime(this.processDate)));

		StringBuilder whereClause = new StringBuilder();
		whereClause
				.append("((entity_field_name = 'CREATE' AND audit_action = 'NEW')");
		whereClause
				.append(" OR ((entity_field_name IN ('ADDKEY#PreviousLEIvalue','MODKEY#PreviousLEIvalue')");
		whereClause.append(" AND NOT EXISTS");
		whereClause.append(" (SELECT 1");
		whereClause.append(" FROM trade,");
		whereClause.append("   book,");
		whereClause.append("   le_attribute le_attribute_po,");
		whereClause.append("   le_attribute le_attribute_cpty");
		whereClause.append("  WHERE trade.trade_id = entity_id");
		whereClause.append("  AND book.book_id  = trade.book_id");
		whereClause
				.append("  AND le_attribute_cpty.legal_entity_id=trade.cpty_id");
		whereClause.append("  AND le_attribute_cpty.attribute_type  ='LEI'");
		whereClause
				.append(" AND book.legal_entity_id          = le_attribute_po.legal_entity_id");
		whereClause.append("  AND le_Attribute_po.attribute_type='LEI'");
		whereClause
				.append(" AND le_attribute_po.attribute_value = le_attribute_cpty.attribute_value)");
		whereClause.append(" AND EXISTS");
		whereClause.append(" (SELECT 1");
		whereClause.append(" FROM bo_audit bo_audit2,");
		whereClause.append(" trade,");
		whereClause.append(" book,");
		whereClause.append(" le_attribute le_attribute_po,");
		whereClause.append(" le_attribute le_attribute_audit");
		whereClause.append(" WHERE ");
		whereClause
				.append(" trade.trade_id   = bo_audit.entity_id");
		whereClause.append("  AND trade.book_id = book.book_id ");
		whereClause
				.append("  AND le_attribute_po.legal_entity_id  = book.legal_entity_id");
		whereClause.append("   AND le_attribute_po.attribute_type ='LEI' ");
		whereClause
				.append("  AND bo_audit2.entity_class_name = 'LegalEntityAttribute'");
		whereClause.append(" AND bo_audit2.entity_name     ='LEI'");
		whereClause
				.append(" AND bo_audit2.entity_id = le_attribute_audit.le_attribute_id");
		whereClause
				.append(" AND le_attribute_audit.legal_entity_id = trade.cpty_id");
		whereClause
				.append(" AND  bo_audit2.modif_date>=" + Util.datetime2SQLString(getStartTime(queryDate)));
		whereClause.append(" AND bo_audit2.modif_date<=" + Util.datetime2SQLString(getEndTime(queryDate)));
		whereClause
				.append(" AND le_attribute_po.attribute_value = bo_audit2.old_value");
		whereClause
				.append("  AND bo_audit2.entity_field_name IN ('_attributeValue'))");
		whereClause.append(" )))");

		query.appendWhereClause(whereClause.toString());

		return query;
	}

    private SQLQuery buildModifiedTradeQuery(List<Integer> mmcIdList) {

        // By default, the date used for the query is the pocessDate
        JDate queryDate = this.processDate;

        // It gets the before day between Monday and Friday
        JDate weekDayBefore = CollateralUtilities.getMonFridayBefore(this.processDate);
        // If the weekDayBefore is festive in the report Calendar it will be
        // used in the query from Audit
        if (!CollateralUtilities.isBusinessDay(weekDayBefore, getReportTemplate().getHolidays())) {
            queryDate = weekDayBefore;
        }
        
        // Get traceable audit keywords
        Vector<String> modifKeys = LocalCache.getDomainValues(
				this.getDSConnection(), DV_EMIR_LINKING_MODIF_KEYS);
        

        SQLQuery query = new SQLQuery();
        query.appendWhereClause("entity_class_name = 'Trade'");
        query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(queryDate)));
        query.appendWhereClause("modif_date<=" + Util.datetime2SQLString(getEndTime(queryDate)));

        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(((entity_field_name like 'ADDKEY#MC_CONTRACT_NUMBER%' OR ");
        whereClause.append("entity_field_name like 'MODKEY#MC_CONTRACT_NUMBER%') AND ");
		whereClause.append("new_value IN ");
		whereClause.append(Util.collectionToSQLString(mmcIdList));
		whereClause.append(")");
						
		if (!Util.isEmpty(modifKeys)) {
			whereClause.append(" OR (entity_field_name in "
					+ Util.collectionToSQLString(modifKeys) + " ) ");
		}
		
		whereClause.append(") AND ");
		whereClause.append("(audit_action = 'AMEND')");
		query.appendWhereClause(whereClause.toString());

        return query;
    }
    
}

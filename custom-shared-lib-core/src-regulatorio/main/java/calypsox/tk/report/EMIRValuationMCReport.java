package calypsox.tk.report;

import calypsox.regulation.util.EmirUtil;
import calypsox.tk.util.interfaceImporter.SantDerivativeTradesLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * EMIR Valuation report, full version.
 * Full: recover all the margin calls to CSA contracts for certain branches.
 * layout adapted to EMIR specificiation
 *
 * @author Rafael Nishizaki & Tetyana Yavorska
 * @version 1.1
 */
public class EMIRValuationMCReport extends MarginCallEntryReport {

    public static final String MARGIN_CALL_CONFIG_PROP = "MarginCallConfig";

    private static final long serialVersionUID = 8815243328163078328L;

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

    /*
     * Saves DV parameterization of used
     */
    private static Map<String, String> dvConfiguration = null;

    /**
     * Each collection of rows to be processed from MCEntries uses as many
     * available processors in the system
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    /*
     * Default for DS threads
     */
    private static final int NUM_DS_THREADS = 8;


    /*
     * Alternative configurations by DV
     */
    public enum CONFIGURATIONS {
        SKIP_ERRORS("No_Errors"), // will disable errors
        NUMBER_THREADS_MCENTRY("Thread_number_mce"), // number of thread to
        // recover MCEntries, 4
        // default
        NUMBER_THREADS_MCPOS("Thread_number_mcpos"), // number of thread to
        // recover MC Positions,
        // defaul number
        // processors
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

        if (!mandatoryFields(errors))
            return null;

        // recovers DV configurations
        final DomainValuesThread dvThread = new DomainValuesThread();
        dvThread.start();

        // avoid problems adding errors by independent threads
        final List<String> errorList = Collections.synchronizedList(errors);
        final List<ReportRow> rows = Collections.synchronizedList(new ArrayList<ReportRow>());

        List<Integer> contractsIds = new ArrayList<Integer>();
        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        // get the CM filter based on the template values
        final MarginCallConfigFilter mcFilter = buildMCContractsFilter();

        // build MC Report template
        final MarginCallReportTemplate cmTemplate = buildReportTemplate();

        try {
            contractsIds = CollateralUtilities.getAllMarginCallContractIds(mcFilter);
            dvThread.join();
            // if used, stores configuration in DV Named SACCRCMBalancesReport
            dvConfiguration = dvThread.getDomainValuesAndComments();

        } catch (CollateralServiceException | InterruptedException e) {
            String mess = "ERROR: Executor in getAllMarginCallContractIds error: ";
            Log.error(this, e); //for sonar purpose
            Log.error(this, mess + e.getMessage());
            errorList.add(mess + e.getLocalizedMessage());

        } catch (Exception e2) {
            Log.error(this, e2); //sonar purpose
            errorList.add("ERROR: Cannot gather contracts. Check other Filter if using one");
        }

        /**
         * Recover Margin Call Entries from workers
         */
        final List<MarginCallEntry> mcEntries = workersMCEntriesRecover(contractsIds, cmTemplate, errorList);
        if (mcEntries.size() == 0) {
            errorList.add("ERROR: Not Margin Call found. Calculation has been done for process date?");
        }

        // Build rows
        final ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCPOS, errorList);

        try {
            // Multithread execution, one contract per core
            for (final MarginCallEntry marginCallEntry : mcEntries) {
            	
            	// Only contracts with trades available
            	if (!Util.isEmpty(marginCallEntry.getTradeNumbers())){
	                taskExecutor.submit(new Runnable() {
	                    @Override
	                    public void run() {
	
	                        // for each contract, multiple lines based on type of
	                        // report TODO
	                        final Collection<ReportRow> threadColRows = buildRows(marginCallEntry, errorList);
	
	                        if (!threadColRows.isEmpty())
	                            rows.addAll(threadColRows);
	                    }
	
	                });
            	}
            }
        } finally {
            taskExecutor.shutdown();
            // important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Log.error(SACCRCMPositionReport.class, "FAIL: Thread interruption Service: " + e);
                errors.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            }
        }

        // if option activate, dont care errors occurred, it will send an OK to
        // Scheduler
        if (Util.isTrue(dvConfiguration.get(CONFIGURATIONS.SKIP_ERRORS.name)))
            errors.clear();
        else if (!errorList.isEmpty()) // attach to errors
            errors.addAll(errorList);

        reportOutput.setRows(rows.toArray(new ReportRow[0]));
        return reportOutput;
    }

    /**
     * @param entry     to be processed
     * @param errorList
     * @return rows with positions of the entry
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<ReportRow> buildRows(final MarginCallEntry entry, final List<String> errorList) {

        if (entry != null) {
            ReportRow row = new ReportRow(entry.toDTO());
            String submitter = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT);

            row.setProperty(EmirUtil.MARGIN_CALL_ENTRY_NAME, entry);
            row.setProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME, entry.getCollateralConfig());
            row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT, submitter);
            row.setProperty(EmirUtil.PROCESSDATE, processDate);
            row.setProperty(EmirUtil.ISDELTA, false);

            String group = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES);
            if (!Util.isEmpty(group)) {
                String replace = (String) getReportTemplate().get(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME);
                Vector<String> namesLE = Util.string2Vector(group);
                LegalEntity le = BOCache.getLegalEntity(getDSConnection(), replace.trim());
                row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES, namesLE);
                row.setProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME, le);
            }

            Vector v = new Vector();
            v.add(row);
            return v;
        }

        return null;
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
        return SantMCConfigFilteringUtil.getInstance().buildMCConfigFilter(this.processDate, contractIds, ownerIds, typesAgr);
    }

    /**
     * @return generic MarginCallReportTemplate
     */
    private MarginCallReportTemplate buildReportTemplate() {

        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, this.processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES, Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION, Boolean.FALSE);

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
        EmirUtil.checkGroupingReport(this.groupingReport, this.replaceOwner, errors);
        return Util.isEmpty(errors);
    }


    private void initializeFields() {
        this.processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        this.submitterValue = getReportTemplate().get(EMIRValuationMCReportTemplate.SUBMITTER_REPORT);
        this.groupingReport = getReportTemplate().get(EMIRValuationMCReportTemplate.GROUPING_REPORT_NAMES);
        this.replaceOwner = getReportTemplate().get(EMIRValuationMCReportTemplate.REPLACE_OWNER_NAME);
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
    private List<MarginCallEntry> workersMCEntriesRecover(final List<Integer> contractsIds,
                                                          final MarginCallReportTemplate template, List<String> errorList) {

        ExecutionContext context = ExecutionContext.getInstance(ServiceRegistry.getDefaultContext(),
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
         * Result List, sum of list per each worker
         */
        final List<MarginCallEntry> resultEntries = Collections.synchronizedList(new ArrayList<MarginCallEntry>());

        Integer numberThreads = getNumberOfThreads(CONFIGURATIONS.NUMBER_THREADS_MCENTRY, errorList);
        CollateralTaskWorker worker[] = new LoadTaskWorker[numberThreads];

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
                    // next set of contracts
                    mccFilter.setContractIds(Arrays.asList(contractIdsChunks.next()));
                    contextChunk.setFilter(mccFilter);

                    worker[i] = new LoadTaskWorker(contextChunk, entriesLists.get(i));
                    taskExecutor.submit(worker[i]);
                }
            }

            // wait threads
            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                String mess = "ERROR: Executor in recoverMCEntriesWorkers error: ";
                Log.error(this, mess + e.getMessage());
                errorList.add(mess + e.getLocalizedMessage());

            }
            // append to common list
            for (List<MarginCallEntry> entryThread : entriesLists) {
                if (!entryThread.isEmpty())
                    resultEntries.addAll(entryThread);
            }

        } // end chunks

        // return entries from all workers
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
            if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS) != null
                    && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCPOS)) {
                numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS));

            } else if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY) != null
                    && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCENTRY)) {
                numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY));
            }

        } catch (Exception e) {
            final String message = "Error reading number in getFixedThreadPool: " + e.getLocalizedMessage();
            Log.error(this, e + " " + message);
            errorList.add(message);
        }
        return numberThreads;

    }

    /**
     * Thread to run the read of DomainValues configuration. These configuration
     * tend to be general for all
     */
    private class DomainValuesThread extends Thread {

        private Map<String, String> dvMap;

        DomainValuesThread() {
            this.dvMap = null;
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative
         * configurations are used.
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

}

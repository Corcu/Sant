/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.util.interfaceImporter.SantDerivativeTradesLoader;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashPositionFacade;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SA CCR Balances (or new IRIS balances files). This report provides collateral balances and other information. Positions are built from Margin Call Entries based
 * on panel filters.
 * It includes columns for Collateral Config, Margin Call Entries and some custom columns, extracted from MC positions.
 *
 * @author Guillermo Solano
 * @version 1.1
 * @Date 09/01/2017
 */
public class SACCRCMPositionReport extends SantReport {


    /**
     * Serial UID
     */
    private static final long serialVersionUID = 3216889092848520104L;

    private static final String IMIRISMAPPING = "IMIRISMapping";
    private static final String GUARANTEE_TYPE = "GUARANTEE_TYPE";

    /*
     * Alternative configurations by DV
     */
    public enum CONFIGURATIONS {
        SKIP_ERRORS("No_Errors"), //will disable errors
        MATURITY_RANGE("Maturity_Range"), //allows to add new values in range
        NUMBER_THREADS_MCENTRY("Thread_number_mce"), //number of thread to recover MCEntries, 4 default
        NUMBER_THREADS_MCPOS("Thread_number_mcpos"), //number of thread to recover MC Positions, defaul number processors
        CHUNK_SIZE("Chunk_size"), // number of contracts per thread, default 999
        NUMBER_DECIMALS("Number_Decimal"); //default 4, allows to precise with more decimals


        private String name;

        CONFIGURATIONS(final String n) {
            this.name = n;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Each collection of rows to be processed from MCEntries uses as many available processors in the system
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    /*
     * Default for DS threads
     */
    private static final int NUM_DS_THREADS = 8;

    //variables
    /*
     * Process Date, introduced by date in Panel
     */
    private JDate processDate;

    /*
     * Process date -1 business day
     */
    private JDate valueDate;

    /*
     *  Process date + Matury offset to business days, determined by Maturity offset in panel.
     *  If empty, default is 7
     */
    private JDate collateralMaturityDate;


    /*
     * Static constant to define how many decimals should be use
     */
    private static Integer DECIMALS_POSITIONS = null;

    /*
     * Saves DV parameterization of used
     */
    private static Map<String, String> dvConfiguration = null;

    /**
     * Main method, call by super class. Some exceptions & control are in super class.
     *
     * @param errors
     * @return rows for securities positions
     */
    @Override
    public ReportOutput loadReport(Vector<String> errors) {

        //Verified mandatory fields and initialize variables
        if (!mandatoryFields(errors))
            return null;

        //recovers DV configurations
        final DomainValuesThread dvThread = new DomainValuesThread();
        dvThread.start();
        //avoid problems adding errors by independent threads
        final List<String> errorList = Collections.synchronizedList(errors);
        final List<ReportRow> rows = Collections.synchronizedList(new ArrayList<>());

        List<Integer> contractsIds = new ArrayList<>();
        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        //get the CM filter based on the template values
        final MarginCallConfigFilter mcFilter = buildMCContractsFilter();
        //build MC Report template
        final MarginCallReportTemplate cmTemplate = buildReportTemplate();
        //get ids of contracts

        try {

            contractsIds = CollateralUtilities.getAllMarginCallContractIds(mcFilter);
            //contractsIds.clear();
            //contractsIds.add(44548801);
            dvThread.join();
            //if used, stores configuration in DV Named SACCRCMBalancesReport
            dvConfiguration = dvThread.getDomainValuesAndComments();

        } catch ( InterruptedException e) {
            String mess = "ERROR: Executor in getAllMarginCallContractIds error: ";
            Log.error(this, mess + e.getMessage());
            Log.error(this, e);//sonar
            errorList.add(mess + e.getLocalizedMessage());

        } catch (Exception e2) {
            Log.error(this, e2); //sonar
            errorList.add("ERROR: Cannot gather contracts. Check other Filter if using one");
        }

        /**
         * Recover Margin Call Entries from workers
         */
        final List<MarginCallEntry> mcEntries = workersMCEntriesRecover(contractsIds, cmTemplate, errorList);
        if (mcEntries.size() == 0) {
            errorList.add("ERROR: Not Margin Call found. Calculation has been done for process date?");
        }

        final ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCPOS, errorList);

        try {
            // Multithread execution, one contract per core
            for (final MarginCallEntry marginCallEntry : mcEntries) {

                taskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {

                        //for each contract, multiple lines based on type of report
                        final Collection<ReportRow> threadColRows = buildMCBalancesRows(marginCallEntry, errorList);

                        if (!threadColRows.isEmpty())
                            rows.addAll(threadColRows);
                    }

                });
            }
        } finally {
            taskExecutor.shutdown();
            //important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Log.error(SACCRCMPositionReport.class, "FAIL: Thread interruption Service: " + e);
                errors.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            }
        }

        //if option activate, dont care errors occurred, it will send an OK to Scheduler
        if (Util.isTrue(dvConfiguration.get(CONFIGURATIONS.SKIP_ERRORS.name)))
            errors.clear();
        else if (!errorList.isEmpty()) //attach to errors
            errors.addAll(errorList);

        reportOutput.setRows(rows.toArray(new ReportRow[0]));
        return reportOutput;
    }

    /**
     * @param entry     to be processed
     * @param errorList
     * @return rows with positions of the entry
     */
    private Collection<ReportRow> buildMCBalancesRows(final MarginCallEntry entry, final List<String> errorList) {

        final String cashSec = getReportTemplate().get(BOSecurityPositionReportTemplate.CASH_SECURITY);
        final String positionType = getReportTemplate().get(BOSecurityPositionReportTemplate.POSITION_TYPE);

        ArrayList<String> positionkeys = new ArrayList<String>();
        if (Util.isEmpty(positionType))
            positionkeys.add(entry.getPositionType());

        else if (positionType.equals(SACCRCMPositionReportTemplate.ACTUAL))
            positionkeys.add(SACCRCMPositionReportTemplate.ACTUAL);

        else if (positionType.equals(SACCRCMPositionReportTemplate.THEORETICAL))
            positionkeys.add(SACCRCMPositionReportTemplate.THEORETICAL);

        // Recover positions for specified type
        List<MarginCallPosition> mcPositions = entry.getPositions(positionkeys);

        //Add SecLending SecVsSec positions beans
        PricingEnv pricingEnv = getPricingEnv();
        if(null!=pricingEnv && SecLendingPositionUtil.PE_MEXICO.equalsIgnoreCase(pricingEnv.getName())){
            mcPositions.addAll(SecLendingPositionUtil.getSecVsSecPositions(entry));
        }


        //filter cash or securities if necessary
        boolean includeCash = true;
        boolean includeSec = true;
        if (!Util.isEmpty(cashSec)) {
            if (cashSec.equals(SACCRCMPositionReportTemplate.CASH))
                includeSec = false;
            else if (cashSec.equals(SACCRCMPositionReportTemplate.SECURITY))
                includeCash = false;
        }

        final List<ReportRow> rowsList = new ArrayList<ReportRow>(mcPositions.size());

        //set MarginType of the contract
        CollateralConfig contract = entry.getCollateralConfig();
        String marginType = getCollateralMarginType(contract);

        /*
         * Build rows
         */
        for (MarginCallPosition pos : mcPositions) {

            if ((SACCRCMPositionReportTemplate.CASH.equals(pos.getType())) && (!includeCash))
                continue;
            if ((SACCRCMPositionReportTemplate.SECURITY.equals(pos.getType())) && (!includeSec))
                continue;

            final ReportRow currentRow = new ReportRow(entry.toDTO(), SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY);
            currentRow.setProperty(SACCRCMPositionReportTemplate.MC_POSITION, pos);
            currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_CONFIG, entry.getCollateralConfig());
            currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_CONFIG_TYPE, getCollateralConfigType(entry.getCollateralConfig()));
            currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MOVEMENT_TYPE, getCollateralMovementType(pos));
            currentRow.setProperty(SACCRCMPositionReportTemplate.MARGIN_TYPE, marginType);

            buildCollateralDates(currentRow);

            if (pos instanceof SecurityPosition) {
                final Product security = ((SecurityPosition) pos).getProduct();
                currentRow.setProperty(SACCRCMPositionReportTemplate.POSITION_PRODUCT, security);
            }
            rowsList.add(currentRow);

        }

        return rowsList;
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
            String chunkSizeStr = dvConfiguration.get(CONFIGURATIONS.CHUNK_SIZE);
            if (!Util.isEmpty(chunkSizeStr)) {
                chunkSize = Integer.parseInt(chunkSizeStr);
            }
        } catch (Exception e) {
            Log.error(this, e); //sonar
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
                    //next set of contracts
                    List<Integer> integers = Arrays.asList(contractIdsChunks.next());
                    mccFilter.setContractIds(integers);
                    contextChunk.setFilter(mccFilter);

                    worker[i] = new LoadTaskWorker(contextChunk, entriesLists.get(i));
                    taskExecutor.submit(worker[i]);
                    /*
                    //TODO REMOVE
                    String from  = "product_desc, trade_keyword ";
                    StringBuilder where = new StringBuilder();
                    where.append(" product_desc.product_id  = trade.product_id ");
                    where.append(" AND trade_keyword.trade_id = trade.trade_id ");
                    where.append(" AND trade.trade_status IN ('VERIFIED','PARTENON')");
                    where.append(" AND product_desc.product_type IN ('SecLending')");
                    where.append(" AND product_desc.product_sub_type LIKE 'Sec Vs Sec'");
                    where.append(" AND trade_keyword.keyword_name = 'MARGIN_CALL_CONFIG_ID'");
                    where.append(" AND trade_keyword.keyword_value IN (41451879)");
                    where.append(" AND trade.trade_id = 41259515");
                    try {
                        TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(from, where.toString(), "trade.trade_id desc", true, null);
                    } catch (CalypsoServiceException e) {
                        Log.error(this, e);
                    }
                     */
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
            //append to common list and fill lightweight entries (MIG 16.1)
            for (List<MarginCallEntry> entryThread : entriesLists) {
                if (!entryThread.isEmpty() && SantMarginCallEntryUtil.fillLightWeightMarginCallEntry(context, entryThread)) {
                    resultEntries.addAll(entryThread);
                }
            }
        } //end chunks

        //return entries from all workers
        return resultEntries;
    }


    private void getAllPDvSecvsSec(){

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
            final String message = "Error reading number in getFixedThreadPool: " + e.getLocalizedMessage();
            Log.error(this, message);
            Log.error(this, e); //sonar
            errorList.add(message);
        }
        return numberThreads;

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
     * @return filter obtained from template configurations
     */
    private MarginCallConfigFilter buildMCContractsFilter() {

        final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        List<Integer> ownerIds = new ArrayList<>();
        if (!Util.isEmpty(ownersNames)) {
            ownerIds = getIdsLe(ownersNames);
        }

        final String cpty = (String) getReportTemplate().get(SantGenericTradeReportTemplate.COUNTERPARTY);
        List<Integer> idsLe = new ArrayList<>();
        if (!Util.isEmpty(cpty)) {
            idsLe = Util.string2IntVector(cpty);
        }

        final String typeAgrString = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        List<String> typesAgr = new ArrayList<>();
        if (!Util.isEmpty(typeAgrString)) {
            typesAgr = Util.string2Vector(typeAgrString);
        }

        final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        List<Integer> contractIds = new ArrayList<>();
        if (!Util.isEmpty(agreementIds)) {
            contractIds = Util.string2IntVector(agreementIds);
        }
        final String sdf = (String) getReportTemplate().get(BOSecurityPositionReportTemplate.SEC_FILTER);

        final String contractGroupsFilter = (String) getReportTemplate().get(BOSecurityPositionReportTemplate.CUSTOM_FILTER);
        List<String> contractGroupFiltersList = new ArrayList<>();
        if (!Util.isEmpty(contractGroupsFilter)) {
            contractGroupFiltersList = Util.stringToList(contractGroupsFilter);
        }

        return SantMCConfigFilteringUtil.getInstance().getInstance().buildMCConfigFilter(this.processDate, contractIds, ownerIds, typesAgr, idsLe, sdf, contractGroupFiltersList);

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


    /*
     * Override to disable end date, NOT USED.
     */
    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    /**
     * Stores process, value & collateral dates. Iquals for all rows.
     *
     * @param currentRow
     */
    private void buildCollateralDates(ReportRow currentRow) {

        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_PROCESS_DATE, this.processDate);
        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_VALUE_DATE, this.valueDate);
        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MATURITY_DATE, this.collateralMaturityDate);
    }


    /**
     * @param pos current position
     * @return Cash, Bond or Equity depending of the product
     */
    private String getCollateralMovementType(final MarginCallPosition pos) {

        if (pos instanceof CashPositionFacade) {
            return "Cash";

        } else {
            String p = pos.getDescription();
            if (!Util.isEmpty(p)) {
                if (p.contains("Bond"))
                    return "Bond";
                else if (p.contains("Equity"))
                    return "Equity";
            }
        }
        return "";
    }


    /**
     * @param mcConfig
     * @return IM for CSD contract; CSA, ISMA, OSLA, MMOO for VM Contracts
     */
    private String getCollateralConfigType(CollateralConfig mcConfig) {

        final String collateralMarginType = mcConfig.getContractType();

        if (!Util.isEmpty(collateralMarginType)) {
            if (collateralMarginType.equals("CSD")) {
                return "IM";
            }
        }
        return collateralMarginType;
    }


    /**
     * @param errors
     * @return true if all mandatory filters in panel are included
     */
    private boolean mandatoryFields(final Vector<String> errors) {

        errors.clear();

        if (getReportTemplate() == null) {
            errors.add("Template not assign.");
            return false;
        }
        computeProcessDate(errors);

        //Cash, Security, Both
        if (Util.isEmpty((String) getReportTemplate().get(BOSecurityPositionReportTemplate.CASH_SECURITY)))
            errors.add("Cash/Sec cannot be empty.");

        //recover dates, common for all rows
        Integer collateralMaturityOffset = 7;// by default if empty selection in panel
        final String offset = getReportTemplate().get(SACCRCMPositionReportTemplate.MATURITY_OFFSET);
        if (!Util.isEmpty(offset)) {
            collateralMaturityOffset = Integer.parseInt(offset);
        }

        this.valueDate = this.processDate.addBusinessDays(-1, getReportTemplate().getHolidays());
        this.collateralMaturityDate = this.processDate.addBusinessDays(collateralMaturityOffset, getReportTemplate().getHolidays());

        return (Util.isEmpty(errors));
    }

    /**
     * @param errors
     * @return true if process date has been read from the template
     */
    private boolean computeProcessDate(final Vector<String> errors) {

        this.processDate = null;
        this.processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        if (this.processDate == null) {
            errors.add("Process Start Date cannot be empty.");
            return false;
        }
        return true;
    }


    public static Integer decimalsPositions4Number() {
        if (DECIMALS_POSITIONS != null)
            return DECIMALS_POSITIONS;

        DECIMALS_POSITIONS = 4;
        String domainName = SACCRCMPositionReport.class.getSimpleName() + "." + (SACCRCMPositionReport.CONFIGURATIONS.NUMBER_DECIMALS.getName());
        Map<String, String> decimalMap = CollateralUtilities.initDomainValueComments(domainName);
        if (decimalMap.containsKey(domainName)) {
            try {
                DECIMALS_POSITIONS = Integer.parseInt(decimalMap.get(domainName));
            } catch (NumberFormatException e) {
                Log.error(SACCRCMPositionReport.class, "Cannot read number in DV " + SACCRCMPositionReport.CONFIGURATIONS.NUMBER_DECIMALS.getName());
            }
        }

        return DECIMALS_POSITIONS;
    }

    /**
     * Method getCollateralMarginType, return the guarantee type
     *
     * @param marginCall
     * @return mapped value from domain value IMIRISMapping
     */
    private String getCollateralMarginType(final CollateralConfig marginCall) {
        if (marginCall != null) {
            //maps the domain value values with their comments
            Map<String, String> map = CollateralUtilities.initDomainValueComments(IMIRISMAPPING);
            //gets the additional field from the contract
            String field = marginCall.getAdditionalField(GUARANTEE_TYPE);
            //checks if it is not empty
            if (!Util.isEmpty(field)) {
                //get the comment related to the value
                String comment = map.get(field);
                //returns it, if it is not empty
                if (!Util.isEmpty(comment)) return comment;
            }
        }
        //any other cases returns blank
        return "";
    }


////////////////////////////////////////////////////////////////////
/////////////	INDEPENDENT INNERS CLASS THREADS //////////////////	
//////////////////////////////////////////////////////////////////

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

            for (CONFIGURATIONS t : SACCRCMPositionReport.CONFIGURATIONS.values()) {
                this.dvMap.putAll(CollateralUtilities.initDomainValueComments(domainName + t.getName()));
            }
        }

        public Map<String, String> getDomainValuesAndComments() {
            return this.dvMap;
        }
    }
}



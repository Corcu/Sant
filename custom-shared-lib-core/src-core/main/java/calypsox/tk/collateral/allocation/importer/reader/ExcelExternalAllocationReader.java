/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */

package calypsox.tk.collateral.allocation.importer.reader;

import calypsox.tk.collateral.allocation.importer.CashExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalTripartyBean;
import calypsox.tk.collateral.allocation.importer.SecurityExternalAllocationBean;
import calypsox.tk.collateral.manager.worker.impl.FullLoadTaskWorker;
import calypsox.tk.report.SACCRCMPositionReport;
import calypsox.tk.report.SACCRCMPositionReport.CONFIGURATIONS;
import calypsox.tk.report.SACCRCMPositionReportTemplate;
import calypsox.tk.util.interfaceImporter.SantDerivativeTradesLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.MarginCallReportTemplate;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author aela
 */
public class ExcelExternalAllocationReader extends AbstractExternalAllocationReader {
    private static NumberFormat numberFormatter = new DecimalFormat("#0");

    private static Map<String, String> dvConfiguration = null;
    private static final int NUM_DS_THREADS = 8;

    /**
     * Each collection of rows to be processed from MCEntries uses as many available processors in the system
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    public ExcelExternalAllocationReader(String fileToImport) {
        super(fileToImport);
    }

    @Override
    public List<ExternalAllocationBean> readAllocations(List<String> messages) throws Exception {
        InputStream is = null;
        List<ExternalAllocationBean> listAllocs = new ArrayList<>();
        try {
            is = new FileInputStream(getFileToImportPath());
            Workbook wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(0);
            int rowNb = 0;
            for (Iterator<Row> iter = sheet.iterator(); iter.hasNext(); ) {
                try {
                    rowNb++;
                    Row row = iter.next();
                    if ("OWNER".equalsIgnoreCase(row.getCell(0).getStringCellValue())) {
                        continue;
                    }
                    ExternalAllocationBean alloc = getExternalAllocationBean((String) getCellvalue(row.getCell(2)));
                    alloc.setRowNumber(rowNb);
                    alloc.setPoShortName((String) getCellvalue(row.getCell(0)));
                    try {
                        alloc.setCtrShortName((String) getCellvalue(row.getCell(1)));
                    } catch (Exception e) {
                        Log.error(this, e); //sonar
                        int id = 0;
                        Object obj = getCellvalue(row.getCell(1));
                        if (obj instanceof Double) {
                            id = ((Double) obj).intValue();
                            alloc.setContractID(id);
                        } else {
                            alloc.setContractID((int) getCellvalue(row.getCell(1)));
                        }
                    }
                    alloc.setFatherId(numberFormatter.format(getCellvalue(row.getCell(4))));
                    Double nominalValue = null;
                    Object nominal = getCellvalue(row.getCell(3));
                    if (nominal instanceof Double) {
                        nominalValue = (Double) nominal;
                    } else if (nominal instanceof String) {
                        NumberFormat format = DecimalFormat.getInstance(Locale.ENGLISH);
                        try {
                            nominalValue = format.parse(((String) nominal).trim()).doubleValue();
                        } catch (Exception e) {
                            messages.add("Unable to read the Face Value (" + nominal + ") from row : " + rowNb);
                            Log.error(this, e);
                            continue;
                        }
                    }
                    alloc.setNominal(nominalValue);

                    if (row.getCell(5) != null) {
                        Object valueDate = null;
                        try {
                            valueDate = getCellvalue(row.getCell(5));
                            alloc.setSettlementDate((Date) valueDate);
                        } catch (Exception e) {
                            messages.add("Unable to read the Value Date (" + valueDate + ") from row : " + rowNb);
                            Log.error(this, e);
                            continue;
                        }
                    }
                    listAllocs.add(alloc);
                } catch (Exception e) {
                    messages.add("Unable to read row : " + rowNb);
                    Log.error(this, e);
                }

            }
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return listAllocs;
    }

    public List<ExternalTripartyBean> tripartyAgreedAmountReader(List<String> messages) throws Exception {
        InputStream is = null;
        List<ExternalTripartyBean> listTripartyAgreedAmount = new ArrayList<>();
        try {
            is = new FileInputStream(getFileToImportPath());
            Workbook wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(0);
            int rowNb = 0;

            for (Iterator<Row> iter = sheet.iterator(); iter.hasNext(); ) {
                try {
                    rowNb++;
                    Row row = iter.next();
                    Object obj1 = getCellvalue(row.getCell(0));
                    if (obj1 instanceof String) {
                        String contractID = ((String) obj1).replaceAll(" ", "");
                        if ("CONTRACTID".equalsIgnoreCase(contractID)) {
                            continue;
                        }
                    }

                    ExternalTripartyBean bean = new ExternalTripartyBean();
                    bean.setRowNumber(rowNb);
                    try {
                        bean.setCtrShortName((String) getCellvalue(row.getCell(0)));
                    } catch (Exception e) {
                        Log.error(this, e); //sonar
                        int id = 0;
                        Object obj = getCellvalue(row.getCell(0));
                        if (obj instanceof Double) {
                            id = ((Double) obj).intValue();
                            bean.setContractID(id);
                        } else {
                            bean.setContractID((int) getCellvalue(row.getCell(0)));
                        }
                    }
                    bean.setCurrency((String) getCellvalue(row.getCell(1)));
                    Double nominalValue = null;
                    Object nominal = getCellvalue(row.getCell(2));
                    if (nominal instanceof Double) {
                        nominalValue = (Double) nominal;
                    } else if (nominal instanceof String) {
                        NumberFormat format = DecimalFormat.getInstance(Locale.ENGLISH);
                        try {
                            nominalValue = format.parse(((String) nominal).trim()).doubleValue();
                        } catch (Exception e) {
                            messages.add("Unable to read the Face Value (" + nominal + ") from row : " + rowNb);
                            Log.error(this, e);
                            continue;
                        }
                    }
                    bean.setNominal(nominalValue);
                    listTripartyAgreedAmount.add(bean);
                } catch (Exception e) {
                    messages.add("Unable to read row : " + rowNb);
                    Log.error(this, e);
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return listTripartyAgreedAmount;
    }


    /**
     * TO BE IMPROVED
     *
     * @param cellvalue
     * @return
     */
    private ExternalAllocationBean getExternalAllocationBean(String cellvalue) {
        if (cellvalue.trim().length() == 3) {
            CashExternalAllocationBean bean = new CashExternalAllocationBean();
            bean.setCurrency(cellvalue.trim());
            return bean;
        }
        SecurityExternalAllocationBean bean = new SecurityExternalAllocationBean();
        bean.setIsin(cellvalue);
        return bean;
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        ExcelExternalAllocationReader e = new ExcelExternalAllocationReader("C:/test/EXAMPLE FILE.xlsx");
        try {
            List<ExternalAllocationBean> list = e.readAllocations(new ArrayList<String>());
            System.out.println("");
        } catch (Exception e1) {
            Log.error(ExcelExternalAllocationReader.class, e1); //sonar
        }
    }

    Object getCellvalue(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();
            case Cell.CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    public void setEntries(List<ExternalAllocationBean> allocs, JDate processDate, List<String> errorList) {
        for (ExternalAllocationBean alloc : allocs) {
            setEntry(alloc, processDate, errorList);
        }
    }

    private void setEntry(ExternalAllocationBean alloc, JDate processDate, List<String> errorList) {
        MarginCallReportTemplate cmTemplate = buildReportTemplate(processDate);
        if (alloc.getMcc() != null) {
            int contractID = alloc.getMcc().getId();
            ArrayList<Integer> contractsIds = new ArrayList<Integer>();
            contractsIds.add(contractID);
            List<MarginCallEntry> mcEntries = workersMCEntriesRecover(contractsIds, cmTemplate, errorList, processDate);
            if (!mcEntries.isEmpty() && mcEntries.size() == 1) {
                alloc.setMarginCallEntry(mcEntries.get(0));
            } else {
                errorList.add("More than one entry found for the contract " + contractID);
            }
        }
    }

    public void setEntriesTAA(List<ExternalTripartyBean> taas, JDate processDate, List<String> errorList) {
        for (ExternalTripartyBean taa : taas) {
            setEntryTAA(taa, processDate, errorList);
        }
    }

    private void setEntryTAA(ExternalTripartyBean taa, JDate processDate, List<String> errorList) {
        MarginCallReportTemplate cmTemplate = buildReportTemplate(processDate);
        if (taa.getMcc() != null) {
            int contractID = taa.getMcc().getId();
            ArrayList<Integer> contractsIds = new ArrayList<Integer>();
            contractsIds.add(contractID);
            List<MarginCallEntry> mcEntries = workersMCEntriesRecover(contractsIds, cmTemplate, errorList, processDate);
            if (!mcEntries.isEmpty() && mcEntries.size() == 1) {
                taa.setEntry(mcEntries.get(0));
            } else {
                errorList.add("More than one entry found for the contract " + contractID);
            }
        }
    }

    private MarginCallReportTemplate buildReportTemplate(JDate processDate) {
        MarginCallReportTemplate template = new MarginCallReportTemplate();
        template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                Boolean.FALSE);
        return template;
    }

    /**
     * @param contractsIds
     * @param template
     * @param errorList
     * @return list of mc entries from the contracts ids
     */
    private List<MarginCallEntry> workersMCEntriesRecover(final List<Integer> contractsIds, final MarginCallReportTemplate template, List<String> errorList, JDate processDate) {

        final DomainValuesThread dvThread = new DomainValuesThread();
        dvThread.start();
        List<String> errorListThreads = new ArrayList<>();
        ExecutionContext context = ExecutionContext.getInstance(
                ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), template);
        context.setProcessDate(processDate);
        int chunkSize = SantDerivativeTradesLoader.SQL_IN_ITEM_COUNT;
        if (dvConfiguration != null) {
            try {
                chunkSize = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.CHUNK_SIZE));
            } catch (NumberFormatException exc) {
                Log.error(this, "Exception while trying to parse an integer's string", exc.getCause()); //sonar
            }
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

            ExecutorService taskExecutor = getFixedThreadPool(CONFIGURATIONS.NUMBER_THREADS_MCENTRY, errorListThreads);
            List<List<MarginCallEntry>> entriesLists = new ArrayList<>(numberThreads);

            for (int i = 0; i < numberThreads; i++) {
                worker[i] = null;
                entriesLists.add(i, new ArrayList<MarginCallEntry>());
                if (contractIdsChunks.hasNext()) {

                    final ExecutionContext contextChunk = context.clone();
                    final MarginCallConfigFilter mccFilter = new MarginCallConfigFilter();
                    //next set of contracts
                    mccFilter.setContractIds(Arrays.asList(contractIdsChunks.next()));
                    contextChunk.setFilter(mccFilter);

                    worker[i] = new LoadTaskWorker(contextChunk, entriesLists.get(i));
                    taskExecutor.submit(worker[i]);
                }
            }

            //wait threads
            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                String mess = "ERROR: Executor in recoverMCEntriesWorkers error: ";
                errorList.add(mess + e.getLocalizedMessage());

            }
            //append to common list
            for (List<MarginCallEntry> entryThread : entriesLists) {
                if (!entryThread.isEmpty())
                    resultEntries.addAll(entryThread);
            }

        } //end chunks

        //return entries from all workers
        return resultEntries;
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
        if (dvConfiguration != null) {
            try {
                if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS) != null && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCPOS)) {
                    numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCPOS));

                } else if (dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY) != null && option.equals(CONFIGURATIONS.NUMBER_THREADS_MCENTRY)) {
                    numberThreads = Integer.parseInt(dvConfiguration.get(CONFIGURATIONS.NUMBER_THREADS_MCENTRY));
                }


            } catch (NumberFormatException exc) {
                final String message = "Error reading number in getFixedThreadPool: " + exc.getLocalizedMessage();
                errorList.add(message);
                Log.error(this, exc.getMessage() + "\n" + exc); //sonar
            }
        }
        return numberThreads;

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

        @SuppressWarnings("unused")
        public Map<String, String> getDomainValuesAndComments() {
            return this.dvMap;
        }
    }
}

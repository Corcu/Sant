/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */

package calypsox.apps.reporting.balanza;

import calypsox.tk.report.SACCRCMPositionReport.CONFIGURATIONS;
import calypsox.tk.report.SACCRCMPositionReportTemplate;
import calypsox.tk.util.bean.ExternalBalanzaBean;
import calypsox.tk.util.interfaceImporter.SantDerivativeTradesLoader;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallReportTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author aela
 */
public class CSVExternalBalanzaReader extends AbstractExternalBalanzaReader {
    private static final String COMMA_DELIMITER = ";";
    private static NumberFormat numberFormatter = new DecimalFormat("#0");
    private static SimpleDateFormat periodFormatter  = new SimpleDateFormat("yyyyMM");
    private static JDate valuationDate;
    private static Map<String, String> dvConfiguration = null;
    private static final int NUM_DS_THREADS = 8;
    private static String[] _headers;

    private static final int PERIODO =	0;
    private static final int INSTRUMENTO =	1;
    private static final int ISIN =	2;
    private static final int NIF_EMISOR	= 3;
    private static final int NOMBRE_ISIN =	4;
    private static final int EPIGRAFE =	5;
    private static final int SI_NOMINAL	= 6;
    private static final int SI_VALORACION =	7;
    private static final int TRASPASO_ENTRADA =	8;
    private static final int TRASPASO_SALIDA =	9;
    private static final int ENTRADA_NOMINAL= 	10;
    private static final int ENTRADA_VALORACION =	11;
    private static final int SALIDA_NOMINAL	= 12;
    private static final int SALIDA_VALORACION = 	13;
    private static final int CUPON_NOMINAL	= 14;
    private static final int CUPON_VALORACION	= 15;
    private static final int SF_NOMINAL	= 16;
    private static final int SF_VALORACION	= 17;


    /**
     * Each collection of rows to be processed and parsed according as many available processors in the system
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    public CSVExternalBalanzaReader(JDate valuationDate, String fileToImport) {
        super(fileToImport);
        this.valuationDate = valuationDate;
    }

    @Override
    public List<ExternalBalanzaBean> readLines(List<String> messages) throws Exception {
        InputStream is = null;
        List<ExternalBalanzaBean> records = new ArrayList<>();

        try {

            FileReader fr = new FileReader(getFileToImportPath());
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            String[] tempArr;
            int rowNb = 1;
            int ignoredRows = 0;
            line = br.readLine();
            _headers = line.split(COMMA_DELIMITER);
            while((line = br.readLine()) != null) {

                tempArr = line.split(COMMA_DELIMITER);
                try {
                    rowNb++;
                    ExternalBalanzaBean bean = new ExternalBalanzaBean();
                    try {
                        Date periodo = periodFormatter.parse(tempArr[PERIODO]);
                        /*
                        if (Util.isEmpty(tempArr[PERIODO])
                                || (!tempArr[PERIODO].equals(periodFormatter.format(valuationDate.getDate())))) {
                         */

                        if (Util.isEmpty(tempArr[PERIODO]))  {
                            messages.add("Period does not match with valuation parameter from row : " + rowNb +" Line ignored.");
                            ignoredRows++;
                            continue;

                        }

                        bean.setPeriodo(periodo);
                    } catch (Exception ex) {
                        messages.add("Error parsing column : " + _headers[PERIODO] + " from row : " + rowNb);
                        ignoredRows++;
                        Log.error(this, ex);
                        continue;
                    }

                    try {
                        bean.setInstrumento(Integer.parseInt(tempArr[INSTRUMENTO]));
                    } catch (Exception ex) {
                        messages.add("Error parsing column : " + _headers[INSTRUMENTO] + " from row : " + rowNb);
                        Log.error(this, ex);
                        ignoredRows++;
                        continue;
                    }

                    try {
                        String isin =  tempArr[ISIN];
                        if (Util.isEmpty(isin) || isin.length() != 12) {
                            messages.add("Error parsing column : " + _headers[ISIN] + " from row : " + rowNb);
                            ignoredRows++;
                            continue;
                        }
                        bean.setIsin(isin);
                    } catch (Exception ex) {
                        messages.add("Error parsing column : " + _headers[ISIN] + " from row : " + rowNb);
                        ignoredRows++;
                        Log.error(this, ex);
                        continue;
                    }

                    bean.setNif_emmisor(tempArr[NIF_EMISOR]);
                    bean.setNombre_isin(tempArr[NOMBRE_ISIN]);
                    bean.setEpigrafe(tempArr[EPIGRAFE]);

                    try {
                        double d = getNumber(rowNb, SI_NOMINAL, tempArr, messages);
                        bean.setSi_nominal(d);

                        d = getNumber(rowNb, SI_VALORACION, tempArr, messages);
                        bean.setSi_valoracion(d);

                        d = getNumber(rowNb, TRASPASO_ENTRADA, tempArr, messages);
                        bean.setTrespaso_entrada(d);

                        d = getNumber(rowNb, TRASPASO_SALIDA, tempArr, messages);
                        bean.setTrespaso_salida(d);

                        d = getNumber(rowNb, ENTRADA_NOMINAL, tempArr, messages);
                        bean.setEntrada_nominal(d);

                        d = getNumber(rowNb, ENTRADA_VALORACION, tempArr, messages);
                        bean.setEntrada_valoracion(d);

                        d = getNumber(rowNb, SALIDA_NOMINAL, tempArr, messages);
                        bean.setSalida_nominal(d);

                        d = getNumber(rowNb, SALIDA_VALORACION, tempArr, messages);
                        bean.setSalida_valoracion(d);

                        d = getNumber(rowNb, CUPON_NOMINAL, tempArr, messages);
                        bean.setCupon_nominal(d);

                        d = getNumber(rowNb, CUPON_VALORACION, tempArr, messages);
                        bean.setCupon_valoracion(d);

                        d = getNumber(rowNb, SF_NOMINAL, tempArr, messages);
                        bean.setSf_nominal(d);

                        d = getNumber(rowNb, SF_VALORACION, tempArr, messages);
                        bean.setSf_valoracion(d);
                    } catch (Exception ex) {
                        ignoredRows++;
                        continue;
                    }
                    records.add(bean);

                } catch (Exception e) {
                    messages.add("Unable to read row : " + rowNb) ;
                    Log.error(this, e);
                    ignoredRows++;

                }
            }

            if (ignoredRows>0) {
                messages.add("Lines with errors will be ignored :" + ignoredRows);
            }

            br.close();
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return records;
    }

    private Double getNumber(int rowNb, int columnIdx, String[] values, List<String> messages)  throws Exception {
        String columnName = _headers[columnIdx];
        try {

            BigDecimal db = new BigDecimal(values[columnIdx]).setScale(2, RoundingMode.HALF_EVEN);
            if (db.doubleValue() < 0 ) {
                messages.add("Negative number converted to positive at line : " + rowNb + ". Column [" + columnName + "] value :" + db.doubleValue());
            }
            return db.doubleValue();
        } catch (Exception ex) {
            messages.add("Error parsing column : " + columnName + " from row : " + rowNb);
            Log.error(this, ex);
            throw ex;
        }
    }


    @SuppressWarnings("unused")
    public static void main(String[] args) {
        CSVExternalBalanzaReader e = new CSVExternalBalanzaReader(JDate.getNow(), "C:/test/EXAMPLE FILE.xlsx");
        try {
            List<ExternalBalanzaBean> list = e.readLines(new ArrayList<String>());
            System.out.println("");
        } catch (Exception e1) {
            Log.error(CSVExternalBalanzaReader.class, e1); //sonar
        }
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

            for (CONFIGURATIONS t : CONFIGURATIONS.values()) {
                this.dvMap.putAll(CollateralUtilities.initDomainValueComments(domainName + t.getName()));
            }
        }

        @SuppressWarnings("unused")
        public Map<String, String> getDomainValuesAndComments() {
            return this.dvMap;
        }
    }
}

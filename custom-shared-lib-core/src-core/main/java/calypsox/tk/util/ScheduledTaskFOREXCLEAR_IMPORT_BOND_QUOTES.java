/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.util.log.LogGeneric;
import calypsox.tk.util.quotes.QuoteBondBean;
import calypsox.tk.util.quotes.QuotesBondAdapter;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import calypsox.util.SantCalypsoUtilities;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ScheduledTaskFOREXCLEAR_IMPORT_BOND_QUOTES extends ScheduledTask {

    private static final long serialVersionUID = -7120148288106382302L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public static final String SCHEDULED_TASK = "ScheduledTaskFOREXCLEAR_IMPORT_BOND_QUOTES";

    protected static final String EXTRACT_FILE_NAME = "EXTRACT_FILE_NAME";
    protected static final String OUTPUT_FOLDER = "OUTPUT_FOLDER";
    protected static final String TIMESTAMP = "TIMESTAMP";
    protected static final String LINE_DELIMITER = "LINE_DELIMITER";
    protected static final String QUOTE_SET = "Quote Set";
    protected static final String QUOTE_TYPE = "Quote Type";
    private static final String ISIN = "ISIN";

    private transient RemoteMarketData remoteMarketData;

    protected boolean returnExit = true;

    protected String fileName = "";
    protected String path = "";
    protected int line = 0;

    // Logs
    protected transient LogGeneric logGen = new LogGeneric();

    @Override
    public String getTaskInformation() {
        return "This scheduled task import the quotes of bonds. REP00034";
    }

    @Override
    protected boolean process(final DSConnection ds, final PSConnection ps) {
        final JDate valDate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
        path = getAttribute(ForexClearSTUtil.FILE_PATH);
        fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
        fileName = ForexClearSTUtil.getFileName(valDate, fileName);

        startLogs();

        if (!ForexClearFileReader.copyFile(path, fileName)) {
            Log.error(this, "ERROR: Failed to copy file");
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, valDate, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            return false;
        }

        final Vector<String> vectorLines = readFile();

        final HashMap<String, QuoteBondBean> vectorQuotes = getQuoteBondsFromLines(ds, vectorLines);

        final Vector<Bond> bonds = getBondsFromDatabase(vectorQuotes);

        final Vector<QuoteValue> quoteValues = createQuoteValues(bonds, vectorQuotes);

        saveQuotes(quoteValues);

        // post process
        try {
            ForexClearFileReader.postProcess(this.returnExit, valDate, fileName,
                    path);
        } catch (Exception e1) {
            Log.error(this, e1); // sonar
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
        }

        stopLogs();

        return this.returnExit;
    }

    /**
     * Save all quotes in Data Base
     *
     * @param bool
     * @param quoteValues
     * @return
     */
    private void saveQuotes(final Vector<QuoteValue> quoteValues) {

        if (this.returnExit) {
            try {
                this.returnExit = this.remoteMarketData.saveQuoteValues(quoteValues);
                Log.info(this, String.format("%s QuoteValues saved", quoteValues.size()));

            } catch (final RemoteException e) {
                Log.error(this, "Error while saving trades " + e);
                this.returnExit = false;
            }
        }

    }

    /**
     * Return QuoteBondsBean in a Map, from Lines
     *
     * @param ds
     * @param vectorLines
     * @return
     */
    private HashMap<String, QuoteBondBean> getQuoteBondsFromLines(final DSConnection ds, final Vector<String> vectorLines) {

        final HashMap<String, QuoteBondBean> vectorQuotes = new HashMap<>();
        final QuotesBondAdapter quotesBondAdapter = new QuotesBondAdapter();
        this.remoteMarketData = ds.getRemoteMarketData();

        if (this.returnExit) {

            for (final String line : vectorLines) {

                // All lines less header (first one)
                if (!line.isEmpty() && !vectorLines.firstElement().equals(line)) {

                    try {
                        quotesBondAdapter.adaptMessage(line, vectorQuotes);
                    } catch (final AdapterException e) {
                        Log.error(this, "Error while adapting each line. " + e);
                    }
                }
            }
        }

        return vectorQuotes;
    }

    /**
     * @return
     */
    private Vector<String> readFile() {

        final Vector<String> vector = new Vector<String>();

        BufferedReader inputFileStream = null;

        if (!Util.isEmpty(fileName) && !Util.isEmpty(path)) {
            try {
                // We read the file.
                inputFileStream = new BufferedReader(new FileReader(path + fileName));
                String line;
                while (inputFileStream.ready()) {
                    line = inputFileStream.readLine();
                    vector.add(line);
                    this.logGen.incrementTotal();
                }

                Log.info(Log.CALYPSOX, "Finished reading process of Collateral Prices file");

            } catch (final FileNotFoundException e) {

                Log.error("Error: File didn't found", e);
                this.returnExit = false;

            } catch (final Exception e) {

                Log.error("Reading Error", e);
                this.returnExit = false;

            } finally {
                try {
                    if (inputFileStream != null) {
                        inputFileStream.close();
                    }
                } catch (final Exception e) {
                    Log.error("File Loader", e);
                }
            }
        }
        return vector;
    }

    /**
     * Get the proper file name to write the output.
     *
     * @return Proper file name.
     */
    protected String getFileName() {

        final String outputFolder = getAttribute(OUTPUT_FOLDER);
        final File file = new File(outputFolder);
        if (!file.exists()) {
            Log.error(this, "The directory doesn't exist:" + outputFolder);
        }

        final String fileName = getAttribute(EXTRACT_FILE_NAME);
        String timestamp = getAttribute(TIMESTAMP);
        if (!Util.isEmpty(timestamp)) {
            final SimpleDateFormat sdf = new SimpleDateFormat(timestamp);
            timestamp = sdf.format(getValuationDatetime());
        } else {
            timestamp = "";
        }
        return (new StringBuilder().append(outputFolder).append("/").append(timestamp).append(fileName)).toString();

    }

    /**
     * @return
     */
    protected String getDestinyFolder() {

        String outputFolder = getAttribute(OUTPUT_FOLDER);
        outputFolder = outputFolder.substring(0, outputFolder.length() - 1);
        return (new StringBuilder().append(outputFolder).append("/copy/")).toString();
    }


    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(ForexClearSTUtil.FILE_NAME));
        attributeList.add(attribute(ForexClearSTUtil.FILE_PATH));

        attributeList.add(attribute(QUOTE_SET));
        attributeList.add(attribute(QUOTE_TYPE));

        // Logs
        attributeList.add(attribute(ForexClearSTUtil.SUMMARY_LOG));
        attributeList.add(attribute(ForexClearSTUtil.DETAILED_LOG));
        attributeList.add(attribute(ForexClearSTUtil.FULL_LOG));
        attributeList.add(attribute(ForexClearSTUtil.STATIC_DATA_LOG));
        // Logs
        return attributeList;
    }

    /**
     * Get all values for a attribute.
     *
     * @param attr        Attribute.
     * @param currentAttr Current attributes.
     * @return Vector<String> containing all values.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
        if (attr.equals(QUOTE_SET)) {
            Vector v = null;
            try {
                v = DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames();
            } catch (final RemoteException e) {
                Log.error(this, "Error loading Quote Set Names " + e);
            }
            return v;
        }

        return super.getAttributeDomain(attr, currentAttr);
    }

    /**
     * Checks is the configuration is a valid input into the Scheduled Task.
     *
     * @param messages Vector<String> to be shown in case of error.
     * @return True if is valid input false if not.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);

        if (getAttribute(QUOTE_SET).isEmpty()) {
            messages.add("The attribute QUOTE_SET is mandatory.");
        }

        if (getAttribute(QUOTE_TYPE).isEmpty()) {
            messages.add("The attribute QUOTE_TYPE is mandatory.");
        }
        return messages.isEmpty();
    }

    /**
     * Method that creates the new Quote Values to insert into Calypso, in the
     * specified date.
     *
     * @param bonds
     * @param vectorQuotes
     * @return
     */
    public Vector<QuoteValue> createQuoteValues(final Vector<Bond> bonds, final HashMap<String, QuoteBondBean> vectorQuotes) {
        final Vector<QuoteValue> quoteValues = new Vector<>();
        List<String> excludeDuplicates = new ArrayList<String>();
        for (final Bond bond : bonds) {

            final QuoteBondBean bondBean = vectorQuotes.get(bond.getSecCode(ISIN));
            final String quoteSet = getAttribute(QUOTE_SET);
            QuoteValue result = null;

            if (bondBean != null && !bond.getQuoteName().isEmpty()) {
                if (!excludeDuplicates.contains(bond.getQuoteName())) {
                    excludeDuplicates.add(bond.getQuoteName());

                    result = new QuoteValue(quoteSet, bond.getQuoteName(), bondBean.getCloseDate(), getAttribute(QUOTE_TYPE));
                    result.setClose(Double.valueOf(bondBean.getCollprice()) / 100);

                    quoteValues.add(result);
                    logGen.incrementOK();
                    logGen.setOkLine(this.getClass().getSimpleName(), " " + quoteSet + ": " + bond.getSecCode(ISIN) + " - Price: " + result.getClose(), 0, "");
                } else {
                    logGen.incrementRecordErrors();
                    logGen.setErrorDuplicatedIsin(this.getClass().getSimpleName(), " " + bond.getSecCode(ISIN) + " - repeated in the file or in the calypso system.", " ", "", "");
                    Log.error(this, "Bond " + bond.getSecCode(ISIN) + " repeated in the file or in the calypso system.");
                }


            }
            line++;
        }

        return quoteValues;
    }

    /**
     * Method used to retrieve from the database of our system (Calypso) the
     * bonds with the ISIN codes specified into the file to import.
     *
     * @param localQuoteHashMap
     * @return
     */

    @SuppressWarnings("unchecked")
    public Vector<Bond> getBondsFromDatabase(final Map<String, QuoteBondBean> localQuoteHashMap) {

        final Vector<Bond> products = new Vector<>();
        final HashMap<String, Integer> mapBondsCalypso = new HashMap<>();
        final Set<String> isinSet = localQuoteHashMap.keySet();

        final String from = "PRODUCT_SEC_CODE PSC";

        if (this.returnExit) {
            Collection<List<String>> isinSetCollection = cropIsinSet(isinSet);
            for (List<String> croppedIsinList : isinSetCollection) {
                List<CalypsoBindVariable> bindVariables = new ArrayList<>();
                final StringBuffer where = new StringBuffer().append("PSC.SEC_CODE='ISIN' AND PRODUCT_DESC.PRODUCT_ID=PSC.PRODUCT_ID AND PSC.CODE_VALUE IN(");
                where.append(CustomBindVariablesUtil.collectionToPreparedInString(croppedIsinList, bindVariables) + ")");
                where.deleteCharAt(where.length() - 1);
                where.append(')');
                try {
                    final Vector<Bond> bonds = SantCalypsoUtilities.getBondAndBondAssetBackedProducts(from, where.toString(), bindVariables);
                    if (!Util.isEmpty(bonds)) {
                        products.addAll(bonds);
                    }
                } catch (final ExecutionException exc) {
                    Log.error(this, "There are bonds in the file that do not exist in Calypso. " + exc);
                }
            }

        }

        // We load the products from the DataBase into the HashMap, to check if
        // there are products specified in the CSV file but not in Calypso.
        for (int posProducts = 0; posProducts < products.size(); posProducts++) {
            mapBondsCalypso.put(products.get(posProducts).getSecCode(ISIN), products.get(posProducts).getId());
        }
        // If the number of products retrieved is not the expected.
        if (isinSet.size() > products.size()) {
            for (String isinFromFile : isinSet) {
                // We check if the bonds specified in the CSV file exist in
                // Calypso or not (to write an error into the log file).
                if (!mapBondsCalypso.containsKey(isinFromFile)) {
                    logGen.incrementRecordErrors();
                    logGen.setErrorRequiredFieldNotPresent(this.getClass().getSimpleName(), String.format("The bond <%s> specified in the file doesn't exist in Calypso.", isinFromFile), "", "", "", "", "");
                    Log.error(this, String.format("The bond <%s> specified in the file doesn't exist in Calypso.", isinFromFile));
                }

            }
        }

        return products;
    }

    private Collection<List<String>> cropIsinSet(Set<String> fullIsinSet) {
        final int chunkSize = 999;
        final AtomicInteger counter = new AtomicInteger();

        return fullIsinSet.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values();
    }

    /**
     * @param isin      the product isin for which to get the quouteName,
     * @param quoteDate date at which the product should be alive (not matured)
     * @return the quote name for this product
     * @throws RemoteException if a problem occurs when getting quotes names from the
     *                         DataServer
     */
    @SuppressWarnings("rawtypes")
    public String getQuoteNameFromISIN(final String isin, final JDate quoteDate) throws RemoteException {
        String quoteName = "";
        final Vector quoteNames = this.remoteMarketData.getAllQuoteName(quoteDate, "ISIN", isin, false);
        if (!Util.isEmpty(quoteNames)) {
            // we're querying by isin so it should give only one quote name
            final String name = (String) quoteNames.get(0);
            if (name != null) {
                final int separtaorIndex = name.indexOf('|');
                quoteName = (separtaorIndex > 0 ? name.substring(0, separtaorIndex) : name);
            }
        }
        return quoteName;
    }

    public void setRemoteMarketData(final RemoteMarketData remoteMarketData) {
        this.remoteMarketData = remoteMarketData;
    }


    private void startLogs() {
        String time = "";
        synchronized (ForexClearSTUtil.timeFormat) {
            final Date d = new Date();
            time = ForexClearSTUtil.timeFormat.format(d);
        }
        this.logGen.generateFiles(getAttribute(ForexClearSTUtil.DETAILED_LOG),
                getAttribute(ForexClearSTUtil.FULL_LOG),
                getAttribute(ForexClearSTUtil.STATIC_DATA_LOG), time);
        try {
            this.logGen.initializeFiles(this.getClass().getSimpleName());
        } catch (IOException e1) {
            this.logGen.incrementError();
            this.logGen.setErrorCreatingLogFile(this.getClass().getSimpleName(),
                    fileName);
            Log.error(this, e1);
        }
    }

    private void stopLogs() {
        try {
            this.logGen.feedGenericLogProcess(fileName,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName(),
                    this.logGen.getNumberTotal() - 1);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            //add OK lines
            this.logGen.initializeErrorLine();
            this.logGen.feedFullLog(0);
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); // sonar
        }
    }

}

package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImportCSVBondsPrices extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 123L;

    private static final String ISIN = "ISIN";
    private static final String SEPARATOR_DOMAIN_STRING = "Separator";
    private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name";
    private static final String TASK_INFORMATION = "Import Market Data Bonds from a CSV file.";
    private static final String BONDS = "BONDS";
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    private static final String SUBJECT = "Log files for Import Bond prices on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    protected static final String PROCESS = "Load bond prices";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    private static final String SYSTEM = "SUSI";
    private static final String SOURCE = "BOND_PRICES";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // private HashMap<String,Throwable> errors=new HashMap<String,Throwable>();

    private RemoteMarketData remoteMarketData;
    private RemoteProduct remoteProduct;
    private BufferedReader inputFileStream;
    private boolean bResult = true;
    private String file = "";

    private boolean proccesOK = true;
    private boolean controlMOK = true;

    protected LogGeneric logGen = new LogGeneric();

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    /**
     * ST attributes definition
     */

    @SuppressWarnings("unchecked")
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
        try {
            attributeList.add(attribute(QUOTENAME_DOMAIN_STRING)
                    .domain(new ArrayList<String>(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames())));
        } catch (CalypsoServiceException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
        }
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(DETAILED_LOG));
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(FULL_LOG));

        return attributeList;
    }

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);

        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
                time);

        this.remoteProduct = conn.getRemoteProduct();
        this.remoteMarketData = conn.getRemoteMarketData();

        try {
            // if (!this.logGen.validateFilesExistence()) {
            this.logGen.initializeFiles(PROCESS);

            // We check all the files kept into the path specified in the
            // configuration for the Scheduled Task.
            final String date = CollateralUtilities.getValDateString(this.getValuationDatetime());
            final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + date);

            // We check if the number of matches is 1.
            if (files.size() == 1) {
                this.file = files.get(0);
                this.logGen.initilizeStaticDataLog(this.file, "LINE");
                final String filePath = path + this.file;
                try {
                    if (feedPreProcess(filePath)) {
                        final JDate jdate = CollateralUtilities.getFileNameDate(this.file);
                        if (jdate == null) {
                            this.logGen.incrementRecordErrors();
                            this.logGen.setErrorNumberOfLines(SOURCE, this.file);

                            this.bResult = false;
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);

                            this.proccesOK = false;
                        } else {

                            // Just after file verifications, this method will make a copy into the
                            // ./import/copy/ directory
                            FileUtility.copyFileToDirectory(filePath, path + "/copy/");

                            String line = null;
                            String[] values = null;
                            final HashMap<String, String[]> importedQuotes = new HashMap<String, String[]>();
                            HashMap<String, String[]> importedQuotesAux = new HashMap<String, String[]>();
                            Vector<QuoteValue> quoteValues = new Vector<QuoteValue>();
                            Vector<Product> bonds = null;
                            QuoteValue qv = new QuoteValue();
                            boolean stopFile = false;

                            try {
                                this.inputFileStream = new BufferedReader(new FileReader(filePath));

                                // We read the file.
                                for (int i = 0; !stopFile && ((line = this.inputFileStream.readLine()) != null); i++) {
                                    this.logGen.initializeError();
                                    this.logGen.initializeWarning();
                                    this.logGen.initializeOK();
                                    this.logGen.initializeErrorLine();
                                    this.logGen.initializeWarningLine();
                                    this.logGen.initializeOkLine();

                                    if (!line.contains("*****")) {

                                        if (CollateralUtilities.checkFields(line, '|', 2)) {

                                            values = CollateralUtilities.splitMejorado(2, "|", true, line);
                                            for (int ii = 0; ii < values.length; ii++) {
                                                values[ii] = values[ii].trim();
                                            }

                                            importedQuotesAux = new HashMap<String, String[]>();
                                            quoteValues = new Vector<QuoteValue>();
                                            if (!importedQuotes.containsKey(values[0])) {
                                                try {
                                                    if (Double.parseDouble(values[1]) > 0) {
                                                        if (!values[0].equals("")) {
                                                            importedQuotes.put(values[0], values);
                                                        }
                                                        importedQuotesAux.put(values[0], values);
                                                        bonds = getBonds(importedQuotesAux,
                                                                String.valueOf(this.logGen.getNumberTotal()), line,
                                                                this.file);
                                                        if (bonds.size() != 0) {
                                                            qv = createQuoteValues(importedQuotesAux, bonds, jdate,
                                                                    i + 1, line, values[0]);
                                                            if (qv != null) {
                                                                quoteValues.add(qv);
                                                                try {
                                                                    this.remoteMarketData.saveQuoteValues(quoteValues);
                                                                } catch (final RemoteException e) {
                                                                    // Cannot
                                                                    // save
                                                                    // the
                                                                    // trade.
                                                                    // Error
                                                                    // 4
                                                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                                            "Error while saving trades", e);
                                                                    this.logGen.incrementError();
                                                                    this.logGen.setErrorSavingQuote(SOURCE,
                                                                            startFileName, String.valueOf(i + 1),
                                                                            values[0], line);

                                                                    this.bResult = false;
                                                                    this.logGen.feedFullLog(0);
                                                                    this.logGen.feedDetailedLog(0);
                                                                    this.proccesOK = false;
                                                                    // inputFileStream=null;
                                                                    // ControlMErrorLogger
                                                                    // .addError(
                                                                    // ErrorCodeEnum.InvalidData,
                                                                    // "Unexpected error saving price");
                                                                }
                                                            } else {
                                                                this.bResult = false;
                                                            }
                                                        }
                                                    } else {
                                                        this.logGen.incrementError();
                                                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
                                                                this.file,
                                                                String.valueOf(this.logGen.getNumberTotal()), "44",
                                                                "PRICE", values[0], line);

                                                        this.bResult = false;
                                                    }
                                                } catch (final NumberFormatException e) {
                                                    this.logGen.incrementError();
                                                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
                                                            this.file, String.valueOf(this.logGen.getNumberTotal()),
                                                            "44", "PRICE", values[0], line);

                                                    this.bResult = false;
                                                }

                                                if (this.logGen.getNumberError() > 0) {
                                                    this.logGen.incrementRecordErrors();
                                                }
                                                if ((this.logGen.getNumberWarning() > 0)
                                                        && (this.logGen.getNumberError() == 0)) {
                                                    this.logGen.incrementRecordWarning();
                                                    this.addBadLine(line, "The bond has incorrect type in Calypso.");
                                                }
                                                if ((this.logGen.getNumberError() == 0)
                                                        && (this.logGen.getNumberWarning() == 0)) {
                                                    this.logGen.incrementOK();
                                                }
                                                this.logGen.incrementTotal();
                                                if (this.logGen.getNumberOk() == 1) {
                                                    this.logGen.setOkLine(SOURCE, this.file,
                                                            (this.logGen.getNumberTotal() - 1), "0");

                                                }

                                                if ((this.logGen.getNumberWarning() > 0)
                                                        && (this.logGen.getNumberError() == 0)) {
                                                    try {
                                                        this.logGen.feedStaticDataLog(
                                                                String.valueOf(this.logGen.getNumberTotal()), SYSTEM);

                                                    } catch (final Exception e) {
                                                        Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                                "Error. Error writing in log files.");
                                                        Log.error(this, e); //sonar
                                                        this.logGen.setErrorWritingLog(SOURCE, this.file,
                                                                String.valueOf(i + 1));

                                                        this.logGen.feedFullLog(0);
                                                        this.logGen.feedDetailedLog(0);
                                                        this.bResult = false;
                                                        ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
                                                                "Unexpected error in log file");
                                                    }
                                                }

                                                this.logGen.feedFullLog(0);
                                                this.logGen.feedDetailedLog(0);

                                            } else {
                                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Duplicate isin in the file.");
                                                this.bResult = false;
                                                this.logGen.incrementRecordErrors();
                                                this.logGen.incrementTotal();
                                                this.logGen.setErrorDuplicatedIsin(SOURCE, this.file,
                                                        String.valueOf(i + 1), values[0], line);

                                                this.logGen.feedFullLog(0);
                                                this.logGen.feedDetailedLog(0);
                                            }

                                        } else {
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                    "Error checking the number of fields.");
                                            this.bResult = false;
                                            this.logGen.incrementRecordErrors();
                                            this.logGen.incrementTotal();
                                            this.logGen.setErrorBadRecordFormat(SOURCE, this.file,
                                                    String.valueOf(i + 1), "", line, values[0]);

                                            this.logGen.feedFullLog(0);
                                            this.logGen.feedDetailedLog(0);
                                        }

                                    } else {
                                        stopFile = true;
                                    }

                                }
                            } catch (final RemoteException e) {
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", e);
                                this.logGen.incrementError();
                                this.logGen.setErrorSavingQuote(SOURCE, startFileName,
                                        String.valueOf(this.logGen.getNumberTotal()), values[0], line);

                                this.bResult = false;
                                this.logGen.feedFullLog(0);
                                this.logGen.feedDetailedLog(0);
                                this.proccesOK = false;
                                // ControlMErrorLogger.addError(
                                // ErrorCodeEnum.InvalidData,
                                // "Unexpected error saving price");
                                // inputFileStream=null;
                            } catch (final FileNotFoundException e) {
                                // critical error 1
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
                                this.bResult = false;
                                this.logGen.incrementRecordErrors();
                                this.proccesOK = false;
                                this.logGen.setErrorNumberOfFiles(SOURCE, this.file);

                                this.logGen.feedFullLog(0);
                                this.logGen.feedDetailedLog(0);
                                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
                                this.controlMOK = false;
                            } catch (final IOException e) {
                                // Unexpected error opening the file.
                                // Critical error 2
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
                                this.bResult = false;
                                this.logGen.incrementRecordErrors();
                                this.proccesOK = false;
                                this.logGen.setErrorOpeningFile(SOURCE, this.file, "0");

                                this.logGen.feedFullLog(0);
                                this.logGen.feedDetailedLog(0);
                                ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
                                        "Unexpected error opening the file");
                                this.controlMOK = false;
                            } finally {
                                if (this.inputFileStream != null) {
                                    try {
                                        this.inputFileStream.close();
                                    } catch (final IOException e) {
                                        Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                "Error while trying close input stream for the CSV file <"
                                                        + getFileName() + "> open previously", e);
                                        this.bResult = false;
                                        ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
                                                "Unexpected error closing the file");
                                        this.controlMOK = false;
                                    }
                                }
                            }
                        }
                    } else {
                        // Number of lines in file does not match with
                        // number of lines in control record. Critical error
                        // 3
                        this.logGen.incrementRecordErrors();
                        this.logGen.setErrorNumberOfLines(SOURCE, this.file);

                        this.bResult = false;
                        this.logGen.feedFullLog(0);
                        this.logGen.feedDetailedLog(0);
                        this.proccesOK = false;
                        ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
                        this.controlMOK = false;
                    }
                } catch (final Exception e) {
                    // Number of lines in file does not match with number of
                    // lines in control record. Critical error 3
                    Log.error(this, e); //sonar
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorNumberOfLines(SOURCE, this.file);
                    this.bResult = false;
                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    this.proccesOK = false;
                    ControlMErrorLogger
                            .addError(ErrorCodeEnum.InputFileNotFound,
                                    "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
                    this.controlMOK = false;
                }
            } else {
                this.bResult = false;
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorNumberOfFiles(SOURCE, startFileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
                this.proccesOK = false;
                ControlMErrorLogger
                        .addError(ErrorCodeEnum.InputFileNotFound,
                                "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
                this.controlMOK = false;
            }

        } catch (final IOException e2) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Log files is already existing in the system.");
            Log.error(this, e2); //sonar
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            this.bResult = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file");
        }

        try {
            // We handle the errors writing them into the log files.
            this.feedPostProcess(this.bResult);

            this.logGen.closeLogFiles();

            String sumLog = "";
            if (this.file.equals("")) {
                sumLog = this.logGen.feedGenericLogProcess(startFileName, getAttribute(SUMMARY_LOG), PROCESS,
                        this.logGen.getNumberTotal() - 1);
            } else {
                sumLog = this.logGen.feedGenericLogProcess(this.file, getAttribute(SUMMARY_LOG), PROCESS,
                        this.logGen.getNumberTotal() - 1);
            }

            try {
                if (!sumLog.equals("")) {
                    final List<String> to = conn.getRemoteReferenceData().getDomainValues(BONDS);
                    final ArrayList<String> attachments = new ArrayList<String>();
                    attachments.add(sumLog);
                    attachments.add(this.logGen.getStringDetailedLog());
                    attachments.add(this.logGen.getStringFullLog());
                    attachments.add(this.logGen.getStringStaticDataLog());
                    CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                }
            } catch (final MailException me) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error sending log mail. \n" + me); //sonar
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSentEmail(SOURCE, startFileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                this.proccesOK = false;
                this.bResult = false;
                // ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
            }

        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files. \n" + e); //sonar
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorMovingFile(SOURCE, startFileName);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "");
            this.controlMOK = false;
        }

        try {
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); //sonar
        }

        if (this.controlMOK) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }

        return this.proccesOK;
    }

    /**
     * Method that creates the new Quote Values to insert into Calypso, in the specified date.
     *
     * @param importedQuotes Hashmap object with the information about the lines read from the file to import.
     * @param bonds          Bonds retrieved from the database.
     * @param jdate          Date in which we import the prices for the bonds.
     * @param isin
     * @return A vector with the new Quote Values created to insert into Calypso.
     */
    private QuoteValue createQuoteValues(final HashMap<String, String[]> importedQuotes, final Vector<Product> bonds,
                                         final JDate jdate, final int line, final String stringLine, String isin) {
        QuoteValue result = null;
        final String quoteSetName = getAttribute(QUOTENAME_DOMAIN_STRING);

        for (final Product bond : bonds) {

            if (!Util.isEmpty(bond.getQuoteName())) {
                result = new QuoteValue(quoteSetName, bond.getQuoteName(), jdate, bond.getQuoteType());
                result.setClose(new Double(importedQuotes.get(bond.getSecCode(ISIN))[1]).doubleValue() / 100);
            } else {
                this.logGen.incrementWarning();
                this.logGen.setWarningConversionIsin(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
                        isin, stringLine);

                this.bResult = false;
            }
        }

        return result;
    }

    /**
     * This method calls other one to retrieve the Bonds from Calypso.
     *
     * @param importedQuotes Hashmap object with the information about the lines read from the file to import.
     * @return A vector with the bonds retrieved from the system.
     * @throws RemoteException Exception occurred when there is an error while trying to obtain the bonds from the system with the
     *                         ISIN codes specified into the file to import.
     */
    private Vector<Product> getBonds(final HashMap<String, String[]> importedQuotes, final String line,
                                     final String stringLine, final String file) throws RemoteException {

        final HashMap<String, String[]> localQuoteHashMap = new HashMap<String, String[]>(importedQuotes);
        return getBondsFromDatabase(localQuoteHashMap, line, stringLine, file);
    }

    /**
     * Method used to retrieve from the database of our system (Calypso) the bonds with the ISIN codes specified into
     * the file to import.
     *
     * @param localQuoteHashMap Hashmap with the information about the lines read from the file to import.
     * @return A vector with all bonds retrieved from the database.
     * @throws RemoteException Exception occurred when there is an error while trying to obtain the bonds from the system with the
     *                         ISIN codes specified into the file to import.
     */
    @SuppressWarnings("unchecked")
    protected Vector<Product> getBondsFromDatabase(final HashMap<String, String[]> localQuoteHashMap,
                                                   final String line, final String stringLine, final String file) {
        final Vector<Product> products = new Vector<Product>();
        final Set<String> isinSet = localQuoteHashMap.keySet();
        Iterator<String> keySetIterator = isinSet.iterator();
        String isinFromFile = "";

        while (keySetIterator.hasNext()) {
            try {
                Vector<Product> bonds = this.remoteProduct.getProductsByCode("ISIN", keySetIterator.next());
                if (!Util.isEmpty(bonds)) {
                    products.addAll(bonds);
                }
            } catch (final Exception e) {
                // Required field ISIN <XX> not present or not valid. Warning 22
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, file, line, "22", "ISIN", isinFromFile,
                        isinFromFile, stringLine);
                Log.error(this, e); //sonar
                this.bResult = false;
            }
        }

        // We load the products from the DataBase into the HashMap, to check if
        // there are products specified in the CSV file but not in Calypso.
        // for (int posProducts = 0; posProducts < products.size(); posProducts++) {
        // mapBonds.put(products.get(posProducts).getSecCode(ISIN), products.get(posProducts).getId());
        // }
        //
        // // If the number of products retrieved is not the expected.
        // if (numberProducts > products.size()) {
        // keySetIterator = isinSet.iterator();
        // while (keySetIterator.hasNext()) {
        // // We check if the bonds specified in the CSV file exist in
        // // Calypso or not (to write an error into the log file).
        // isinFromFile = keySetIterator.next();
        // if (!mapBonds.containsKey(isinFromFile)) {
        //
        // this.logGen.incrementWarning();
        // this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, file, line, "22", "ISIN",
        // isinFromFile, isinFromFile, stringLine);
        // this.bResult = false;
        // // this.addBadLine(stringLine, "The bond <" + isinFromFile +
        // // "> specified in the CSV file doesn't exist in Calypso");
        // }
        // }
        // }

        return products;
    }

    @Override
    public String getFileName() {
        return this.file;
    }
}

package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.DOMUtility;
import calypsox.util.FileUtility;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteReferenceData;
import com.calypso.tk.util.email.MailException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * <p>
 * Title: ScheduledTaskImportCSVFXClosingPrices
 * </p>
 * <p>
 * Description: Import FX Closing Prices into Calypso from a XML file
 * </p>
 *
 * @author Jos? David Sevillano Carretero
 * @version 1.0
 */

public class ScheduledTaskImportXMLFXClosingPrices extends AbstractProcessFeedScheduledTask {

    private static final long serialVersionUID = 123L;

    /**
     * The fixed Task information returned by getTaskInformation method
     */
    private static final String TASKINFORMATION = "Import Quotes from Asset XML file";
    /**
     * Scheduled Task param with the path of the file. Example: C:\Santander
     */
    private static final String FILEPATH = "File Path";
    /**
     * Scheduled Task param with the start of file name. Example: M3FF_NPV#
     */
    private static final String STARTFILENAME = "Start of File Name";
    /**
     * Scheduled Task param with the File extension
     */
    private static final String FILEEXTENSION = "File Extension";

    /**
     * Root XML node
     */
    private static final String ROOTNODE = "fxsp:spot";
    /**
     * Quote Type
     */
    private static final String QUOTETYPE = "Price";
    /**
     * QuoteSet
     */
    private static final String QUOTESET = "OFFICIAL";
    private boolean bReturn = true;
    private String file = "";
    private RemoteMarketData rmd;
    private RemoteReferenceData rrd;
    @SuppressWarnings("unused")
    private PricingEnv pricingEnv;

    private boolean proccesOK = true;
    private boolean controlMOK = true;

    private static final String FX_CLOSING_PRICES = "FX_CLOSING_PRICES";
    private static final String SUBJECT = "Log files for fx closing prices on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    protected static final String PROCESS = "Load of fx closing prices";
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    private static final String SYSTEM = "SUSI";
    private static final String SOURCE = "FX_CLOSING_PRICES";

    protected LogGeneric logGen = new LogGeneric();

    /**
     * Return task description
     */
    @Override
    public String getTaskInformation() {
        return TASKINFORMATION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(FILEPATH));
        attributeList.add(attribute(STARTFILENAME));
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(DETAILED_LOG));
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(FULL_LOG));

        return attributeList;
    }
    // /**
    // * Get Schedule Task params
    // */
    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // @Override
    // public Vector getDomainAttributes() {
    // Log.debug(this, "ScheduledTaskImportQuotes.getDomainAttributes Start");
    //
    // final Vector v = new Vector();
    // v.addElement(FILEPATH);
    // v.addElement(STARTFILENAME);
    // v.add(SUMMARY_LOG);
    // v.add(DETAILED_LOG);
    // v.add(STATIC_DATA_LOG);
    // v.add(FULL_LOG);
    //
    // Log.debug(this, "ScheduledTaskImportQuotes.getDomainAttributes End");
    // return v;
    // }

    /**
     * Check the mandatory params
     *
     * @param messages Vector
     * @return boolean
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        Log.debug(this, "ScheduledTaskImportQuotes.isValidInput Start");
        boolean retVal = super.isValidInput(messages);

        // Check FILEPATH
        final String filePath = getAttribute(FILEPATH);
        if (Util.isEmpty(filePath)) {
            messages.addElement("File Path to Upload is not specified");
            retVal = false;
        }

        // Check Start FILENAME
        final String fileName = getAttribute(STARTFILENAME);
        if (Util.isEmpty(fileName)) {
            messages.addElement("Start File Name to Upload is not specified");
            retVal = false;
        }

        Log.debug(this, "ScheduledTaskImportQuotes.isValidInput End");
        return retVal;
    }

    /**
     * Process the XML File to import the Quotes
     *
     * @param ds DSConnection
     * @param ps PSConnection
     * @return boolean
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {

        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);

        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
                time);

        try {
            // if (!this.logGen.validateFilesExistence()) {
            this.logGen.initializeFiles(PROCESS);

            this.rmd = ds.getRemoteMarketData();
            this.rrd = ds.getRemoteReferenceData();

            Document document = null;

            try {
                this.pricingEnv = ds.getRemoteMarketData().getPricingEnv(getPricingEnv(), this.getValuationDatetime());
            } catch (final RemoteException e1) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for pricing environment", e1);
                ControlMErrorLogger.addError(ErrorCodeEnum.PricingEnviroment, "");
                this.controlMOK = false;
            }

            // We check all the files kept into the path specified in the
            // configuration for the Scheduled Task.
            final String dateString = CollateralUtilities.getValDateString(this.getValuationDatetime());
            final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + dateString);

            // We check if the number of matches is 1.
            if (files.size() == 1) {
                this.file = files.get(0);
                this.logGen.initilizeStaticDataLog(this.file, "LINE");
                final String filePath = path + this.file;

                // Just after file verifications, this method will make a copy
                // into the
                // ./import/copy/ directory
                FileUtility.copyFileToDirectory(filePath, path + "/copy/");

                // parse the input file
                try {
                    document = creaDOMDocumentDesdeFich(filePath);
                } catch (final FileNotFoundException e) {
                    // critical error 1
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
                    this.bReturn = false;
                    this.logGen.incrementRecordErrors();
                    this.proccesOK = false;
                    this.logGen.setErrorNumberOfFiles(SOURCE, this.file);

                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
                    this.controlMOK = false;
                } catch (final IOException e) {
                    // Unexpected error opening the file. Critical error 2
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
                    this.bReturn = false;
                    this.logGen.incrementRecordErrors();
                    this.proccesOK = false;
                    this.logGen.setErrorOpeningFile(SOURCE, this.file, "0");

                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeRead, "");
                    this.controlMOK = false;
                } catch (final SAXException e) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
                    this.bReturn = false;
                    this.logGen.incrementRecordErrors();
                    this.proccesOK = false;
                    this.logGen.setErrorOpeningFile(SOURCE, this.file, "0");
                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    ControlMErrorLogger.addError(ErrorCodeEnum.InputXMLFileCanNotBeParsed, "");
                    this.controlMOK = false;
                }

                if (this.rrd == null) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error in variable remoteReferenceData");
                    this.bReturn = false;
                }

                // Get the root node
                final Node rootNode = DOMUtility.getFirstChildElementRecursivoObligatorio(document, ROOTNODE, true);
                if (rootNode == null) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "The XML file is empty");
                    this.bReturn = false;
                } else {
                    // Process all childs
                    final NodeList listaNodosConectores = rootNode.getChildNodes();

                    for (int i = 0; i < listaNodosConectores.getLength(); ) {
                        this.logGen.initializeError();
                        this.logGen.initializeWarning();
                        this.logGen.initializeOK();
                        this.logGen.initializeErrorLine();
                        this.logGen.initializeWarningLine();
                        this.logGen.initializeOkLine();

                        try {
                            final Node nodoConector = listaNodosConectores.item(i++);
                            if (nodoConector.getNodeType() == Node.ELEMENT_NODE) {
                                // Get attribute xc:value
                                Vector vQuotes = null;
                                String currencyPair = "";
                                String quote = "";
                                String sDateValue = "";
                                JDate jDate = null;
                                try {
                                    currencyPair = DOMUtility.getAttribute(nodoConector, "xc:value", true);
                                    if ((this.rrd.getCurrencyPair(
                                            currencyPair.substring(0, 3) + "_" + currencyPair.substring(4, 7)) == null)
                                            || (currencyPair.length() != 7) || (currencyPair.equals(""))) {
                                        this.logGen.incrementWarning();
                                        this.logGen.setWarningNoCcy(SOURCE, this.file,
                                                String.valueOf(this.logGen.getNumberTotal()), currencyPair,
                                                nodoConector.toString());

                                        this.bReturn = false;
                                    }
                                } catch (final Exception e) {
                                    Log.error(this, e); //sonar
                                    this.logGen.incrementWarning();
                                    this.logGen.setWarningNoCcy(SOURCE, this.file,
                                            String.valueOf(this.logGen.getNumberTotal()), currencyPair,
                                            nodoConector.toString());
                                    this.bReturn = false;
                                }

                                // Get the text from Element mp:mid
                                try {
                                    quote = DOMUtility.getTextFromChild(nodoConector, "mp:mid", true);

                                    if ((Double.parseDouble(quote) < 0) || quote.equals("")) {
                                        this.logGen.incrementError();
                                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
                                                String.valueOf(this.logGen.getNumberTotal()), "44", "PRICE",
                                                currencyPair, "");

                                        this.bReturn = false;
                                    }
                                } catch (final Exception e) {
                                    Log.error(this, e); //sonar
                                    this.logGen.incrementError();
                                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
                                            String.valueOf(this.logGen.getNumberTotal()), "44", "PRICE", currencyPair,
                                            "");
                                    this.bReturn = false;
                                }

                                // Get the value from Element mp:date
                                try {
                                    sDateValue = DOMUtility.getTextFromChild(nodoConector, "mp:date", true);
                                    if (!sDateValue.equals("")) {
                                        jDate = JDate.valueOf(new Integer(sDateValue.substring(6, 10)),
                                                new Integer(sDateValue.substring(3, 5)),
                                                new Integer(sDateValue.substring(0, 2)));
                                        synchronized (dateFormat) {
                                            dateFormat.setLenient(false);
                                            dateFormat.parse(sDateValue);
                                        }
                                    } else {
                                        this.logGen.incrementError();
                                        this.logGen.setErrorDateNotValid(SOURCE, this.file,
                                                String.valueOf(this.logGen.getNumberTotal()), currencyPair, "");

                                        this.bReturn = false;
                                    }
                                } catch (final Exception e) {
                                    Log.error(this, e); //sonar
                                    this.logGen.incrementError();
                                    this.logGen.setErrorDateNotValid(SOURCE, this.file,
                                            String.valueOf(this.logGen.getNumberTotal()), currencyPair, "");
                                    this.bReturn = false;
                                }

                                if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                                    vQuotes = processOneQuote(vQuotes, currencyPair, quote, jDate);
                                }

                                try {
                                    if (this.logGen.getNumberError() > 0) {
                                        this.logGen.incrementRecordErrors();
                                    }
                                    if ((this.logGen.getNumberWarning() > 0) && (this.logGen.getNumberError() == 0)) {
                                        this.logGen.incrementRecordWarning();
                                    }
                                    if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                                        this.logGen.incrementOK();
                                    }
                                    this.logGen.incrementTotal();
                                    if (this.logGen.getNumberOk() == 1) {
                                        this.logGen.setOkLine(SOURCE, this.file, this.logGen.getNumberTotal() - 1, "0");

                                        if (this.rmd == null) {
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                    "Error in variable remoteMarketData");
                                            this.bReturn = false;
                                        } else {
                                            final boolean bSave = this.rmd.saveQuoteValues(vQuotes);
                                            if ((this.bReturn == true) && (bSave == false)) {
                                                this.bReturn = false;
                                            }
                                        }
                                    }
                                    if ((this.logGen.getNumberWarning() > 0) && (this.logGen.getNumberError() == 0)) {
                                        try {
                                            this.logGen.feedStaticDataLog(String.valueOf(0), SYSTEM);

                                        } catch (final Exception e) {
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                    "Error. Error writing in log files.");
                                            Log.error(this, e); //sonar
                                            this.logGen.setErrorWritingLog(SOURCE, this.file, String.valueOf(i + 1));

                                            this.logGen.feedFullLog(0);
                                            this.logGen.feedDetailedLog(0);
                                            this.bReturn = false;
                                            ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
                                                    "Unexpected error in log file"); // TODO
                                            // LOG
                                            // IO_ERROR
                                            // this.controlMOK = false;
                                        }
                                    }

                                    this.logGen.feedFullLog(0);
                                    this.logGen.feedDetailedLog(0);

                                } catch (final RemoteException ex) {
                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", ex);
                                    this.logGen.incrementError();
                                    this.logGen.setErrorSavingQuote(SOURCE, startFileName,
                                            String.valueOf(this.logGen.getNumberTotal()), currencyPair, "");

                                    this.bReturn = false;
                                    this.logGen.feedFullLog(0);
                                    this.logGen.feedDetailedLog(0);
                                    this.proccesOK = false;
                                    i = listaNodosConectores.getLength();
                                    // ControlMErrorLogger
                                    // .addError(
                                    // ErrorCodeEnum.InvalidData,
                                    // "Unexpected error saving quote");

                                }
                            }

                        } catch (final Exception ex) {
                            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
                            Log.error(this, ex); //sonar
                            this.bReturn = false;
                            this.logGen.incrementRecordErrors();
                            this.logGen.incrementTotal();
                            this.logGen.setErrorBadRecordFormat(SOURCE, this.file,
                                    String.valueOf(this.logGen.getNumberTotal() - 1), "", "", "");

                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);
                        }
                    } // End for

                }

            } else {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
                this.bReturn = false;
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorNumberOfFiles(SOURCE, startFileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);

                this.proccesOK = false;
                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
                this.controlMOK = false;
            }

            /*
             * } else { Log.error(LOG_CATEGORY_SCHEDULED_TASK,
             * "Error. Log files is already existing in the system.");
             * this.logGen.incrementRecordErrors();
             * this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
             *
             * this.logGen.feedFullLog(0); this.logGen.feedDetailedLog(0);
             * this.proccesOK = false; this.bReturn = false;
             * ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
             * "log file is already existing in the system"); // TODO // LOG //
             * IO_ERROR this.controlMOK = false; }
             */
        } catch (final IOException e2) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Log files is already existing in the system.");
            Log.error(this, e2); //sonar
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);

            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            this.bReturn = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file"); // TODO
            // LOG
            // IO_ERROR
            // this.controlMOK = false;
        }

        try {
            this.feedPostProcess(this.bReturn);
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                    "Error. Controled exception. For xml file we don?t use feedPreProcess and this is the reason for this exception.");
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.MoveXmlFile, "");
            this.controlMOK = false;
        }

        try {
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
                    final List<String> to = ds.getRemoteReferenceData().getDomainValues(FX_CLOSING_PRICES);
                    final ArrayList<String> attachments = new ArrayList<String>();
                    attachments.add(sumLog);
                    attachments.add(this.logGen.getStringDetailedLog());
                    attachments.add(this.logGen.getStringFullLog());
                    attachments.add(this.logGen.getStringStaticDataLog());
                    CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                }
            } catch (final MailException me) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error sending log mail.");
                Log.error(this, me);//sonar
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSentEmail(SOURCE, startFileName);
                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                this.proccesOK = false;
                this.bReturn = false;
                // ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
            }

        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files.");
            Log.error(this, e); //sonar
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
            Log.error(this, e);
        }

        if (this.controlMOK) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }

        return this.proccesOK;
    }

    /**
     * Get the complete File Name
     *
     * @return The complete File Name including the date (yyyymmdd)
     */
    @SuppressWarnings("unused")
    private String getCompleteFileName(final String path, final String name) {
        String nameToReturn = null;
        Log.debug(this, "ScheduledTaskImportQuotes.getCompleteFileName Start");

        // toSQLString return yyyyMMdd
        final JDatetime valDate = this.getValuationDatetime();
        final String todayDateString = new SimpleDateFormat("yyyyMMdd").format(valDate);
        nameToReturn = path + name + todayDateString + "." + getAttribute(FILEEXTENSION);
        Log.debug(this, "ScheduledTaskImportQuotes.getCompleteFileName End returning " + nameToReturn);

        return nameToReturn;
    }

    /**
     * Get the complete File Name
     *
     * @return The complete File Name including the date (yyyymmdd)
     */
    @SuppressWarnings("unused")
    private String getCompleteFileName(final String name) {
        String nameToReturn = null;
        Log.debug(this, "ScheduledTaskImportQuotes.getCompleteFileName Start");

        // toSQLString return yyyyMMdd
        final JDatetime valDate = this.getValuationDatetime();
        final String todayDateString = new SimpleDateFormat("yyyyMMdd").format(valDate);
        nameToReturn = name + todayDateString + "." + getAttribute(FILEEXTENSION);
        Log.debug(this, "ScheduledTaskImportQuotes.getCompleteFileName End returning " + nameToReturn);

        return nameToReturn;
    }

    /**
     * Add one quote
     *
     * @param vQuotes      vector of Quote Value objects
     * @param currencyPair Format:JPY/USD
     * @param quote
     * @param date
     * @return Vector vector with QuoteValue
     * @throws Exception
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Vector processOneQuote(Vector vQuotes, final String currencyPair, final String quote, final JDate date) {
        if (vQuotes == null) {
            vQuotes = new Vector();
        }
        final String currency1 = currencyPair.substring(0, 3);
        final String currency2 = currencyPair.substring(4, 7);
        final String myName = "FX." + currency1 + "." + currency2;
        final QuoteValue pDstValue = new QuoteValue();
        pDstValue.setQuoteSetName(QUOTESET);
        pDstValue.setName(myName);
        pDstValue.setDate(date);
        pDstValue.setQuoteType(QUOTETYPE);
        final Double quoteDouble = new Double(quote);
        pDstValue.setClose(quoteDouble.doubleValue());
        vQuotes.add(pDstValue);
        return vQuotes;
    }

    /**
     * Obtiene el document raiz a partir de una URL a un fichero
     *
     * @param URLArchivo el path del fichero xml
     * @return el document del fichero
     * @throws IOException
     * @throws SAXException
     * @throws AdSysException Fichero no encontrado o error al parsearlo.
     */
    public static Document creaDOMDocumentDesdeFich(final String URLArchivo) throws SAXException, IOException {
        InputStream streamXML = null;
        Document retornoDoc;
        streamXML = new FileInputStream(new File(URLArchivo));
        retornoDoc = DOMUtility.createDOMDocument(streamXML);

        return retornoDoc;
    }

    @Override
    public String getFileName() {
        return this.file;
    }
}
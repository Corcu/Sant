package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.bean.SecLendingTradesBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import calypsox.util.GenericValidations;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.util.CollateralBasedUtil;
import com.calypso.tk.service.*;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.calypso.tk.core.PricerMeasure.S_NPV;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImportCSVSecLendingTrades extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 123L;

    private static final String BO_SYSTEM = "BO_SYSTEM";
    private static final String MATURITY = "MATURITY";
    private static final String MTM = "MTM";
    private static final String ROLLOVER = "ROLLOVER";
    private static final String NEW = "NEW";
    private static final String AMEND = "AMEND";
    private static final String MATURE = "MATURE";
    private static final String CANCEL = "CANCEL";
    private static final String REGULAR = "Regular";
    private static final String UNMATURE = "UNMATURE";

    private static final String NONE = "NONE";
    private static final String ALIAS_BOOK_SYSTEM = "ALIAS_BOOK_";
    protected static final String SEPARATOR_DOMAIN_STRING = "Separator";
    protected static final String IS_SLB = "IS_SLB"; //For SLB files
    protected static final String TASK_INFORMATION = "Import TRADES from a CSV file.";
    private static final String SYSTEM = "SUSI";
    protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();
    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    protected static final String BUY_DIRECTION_STRING = "BUY";
    protected static final String SELL_DIRECTION_STRING = "SELL";
    protected static final String GIVE_DIRECTION = "GIVE";
    protected static final String RECEIVE_DIRECTION = "RECEIVE";
    private static final String ORIG_SOURCE_CPTY = "ORIG_SOURCE_CPTY";
    private static final String ORIG_SOURCE_BOOK = "ORIG_SOURCE_BOOK";
    private static final String ORIG_SOURCE_ISIN = "ORIG_SOURCE_ISIN";
    private static Map<String, String> KEYWORD_CM_AFTER_MATURITY;
    private static String KEYWORD_SLB_BUNDLE = "SLB_BUNDLE"; //keyword SLB Bundle
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    protected static final String CONTROL_LINE = "No Control Line";
    protected static final String ADD_END_SEPARATOR = "Add End Separator";
    protected static final String NO_BOOK_MAPPING = "No Book Mapping";

    private static final String SEC_LENDING_TRADES = "SEC_LENDING_TRADES";
    private static final String SUBJECT = "Log files for Import Stock Lending Trades on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    private static final String PROCESS = "Load of Stock Lending from SUSI";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private DSConnection conn;
    private RemoteReferenceData remoteReferenceData = null;
    private RemoteTrade remoteTrade = null;
    protected RemoteProduct remoteProduct = null;
    protected RemoteSantCollateralService remoteColService = null;
    protected HashMap<String, Throwable> errors = new HashMap<String, Throwable>();
    private PricingEnv pricingEnv;
    private boolean bResult = false;
    @SuppressWarnings("unused")
    private boolean proccesOK = true;
    private boolean controlMOK = true;
    private String file = "";
    private String fileName = "";
    private Vector<String> currencies;

    // added (Bean)
    private SecLendingTradesBean secLendTradesBean;

    private static final String SOURCE = "SEC_LENDING";

    protected LogGeneric logGen = new LogGeneric();

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    /**
     * ST attributes definition
     */
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(DETAILED_LOG));
        attributeList.add(attribute(FULL_LOG));
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(CONTROL_LINE));
        attributeList.add(attribute(ADD_END_SEPARATOR));
        attributeList.add(attribute(NO_BOOK_MAPPING));
        attributeList.add(attribute(IS_SLB).booleanType());//for SLB files

        return attributeList;
    }

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);
        this.currencies = LocalCache.getCurrencies();

        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
                time);

        // we add the header and assign the fileWriter to the logs files.
        try {
            // if (!this.logGen.validateFilesExistence()) {
            this.logGen.initializeFiles(PROCESS);

            this.conn = conn;
            this.remoteProduct = conn.getRemoteProduct();
            this.remoteTrade = conn.getRemoteTrade();
            this.remoteColService = (RemoteSantCollateralService) conn.getRMIService("baseSantCollateralService",
                    RemoteSantCollateralService.class);

            try {
                this.pricingEnv = conn.getRemoteMarketData().getPricingEnv(getPricingEnv(),
                        this.getValuationDatetime());
                KEYWORD_CM_AFTER_MATURITY = CollateralUtilities.getCMAfterMaturityKeywordFromDV();
            } catch (final RemoteException e1) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for pricing environment", e1);
                this.bResult = false;
                ControlMErrorLogger.addError(ErrorCodeEnum.PricingEnviroment, "");
                this.controlMOK = false;
            }

            // We check all the files kept into the path specified in the
            // configuration for the Scheduled Task.
            final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName);

            // We check if the number of matches is 1.
            if (files.size() == 1) {
                this.file = files.get(0);
                this.logGen.initilizeStaticDataLog(this.file, "BO_REFERENCE");
                this.fileName = files.get(0);
                final String filePath = path + this.file;

                try {
                    if (feedPreProcessSusiGBO(filePath)) {

                        // Just after file verifications, this method will make
                        // a copy into the
                        // ./import/copy/ directory
                        FileUtility.copyFileToDirectory(filePath, path + "/copy/");

                        final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);
                        @SuppressWarnings("unused")
                        Vector<Trade> trades = new Vector<Trade>();
                        trades = getTrades(filePath, separator);
                    } else {
                        // Number of lines in file does not match with
                        // number of lines in control record. Critical error
                        // 3
                        this.logGen.incrementRecordErrors();
                        this.logGen.setErrorNumberOfLines(SOURCE, this.file);

                        this.logGen.feedFullLog(0);
                        this.logGen.feedDetailedLog(0);
                        this.proccesOK = false;
                        this.bResult = false;
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
                    ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
                    this.controlMOK = false;
                }
            } else {
                this.bResult = false;
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorNumberOfFiles(SOURCE, this.file);
                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
                this.proccesOK = false;
                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
                this.controlMOK = false;
            }
            /*
             * } else { Log.error(LOG_CATEGORY_SCHEDULED_TASK,
             * "Error. Log files is already existing in the system.");
             * this.logGen.incrementRecordErrors();
             * this.logGen.setErrorCreatingLogFile(SOURCE, this.fileName);
             *
             * this.logGen.feedFullLog(0); this.logGen.feedDetailedLog(0);
             * this.proccesOK = false; this.bResult = false;
             * ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
             * "log file is already existing in the system"); // TODO // LOG //
             * IO_ERROR this.controlMOK = false; }
             */
        } catch (final IOException e2) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files. \n" + e2); //sonar
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorCreatingLogFile(SOURCE, this.fileName);

            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            this.bResult = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file"); // TODO
            // LOG
            // IO_ERROR
            // this.controlMOK = false;
        }

        try {
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
                    final List<String> to = conn.getRemoteReferenceData().getDomainValues(SEC_LENDING_TRADES);
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

        return this.controlMOK;
    }

    /**
     * Method for create PLMarks
     *
     * @param tradeId            id for the trade
     * @param bookID
     * @param secLendTradesBean2 SecLendingTradesBean with the data read from the file.
     * @return
     * @throws RemoteException
     */
    @SuppressWarnings({"unchecked"})
    private PLMark createPLMarks(final Trade trade, final String pricingEnvName, final JDate processDate,
                                 final SecLendingTradesBean secLendTradesBean2, final String stringLine, final ArrayList<String> errorMsgs)
            throws Exception {
        if (secLendTradesBean2.getMtmDate().equals("")) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "31", "MTM_DATE", secLendTradesBean2.getBoReference(),
                    stringLine);

        } else {
            try {
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    dateFormat.parse(secLendTradesBean2.getMtmDate());
                }
                trade.getKeywords().put(TradeInterfaceUtils.TRD_IMP_FIELD_MTM_DATE, secLendTradesBean2.getMtmDate());
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "31", "MTM_DATE",
                        secLendTradesBean2.getBoReference(), stringLine);
                Log.error(this, e); //sonar
            }
        }
        if (secLendTradesBean2.getMtm().equals("")) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "34", "MTM", secLendTradesBean2.getBoReference(),
                    stringLine);

        }
        if (secLendTradesBean2.getMtmCcy().equals("") || !this.currencies.contains(secLendTradesBean2.getMtmCcy())) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "35", "MTM_CCY", secLendTradesBean2.getBoReference(),
                    stringLine);

        }

        trade.addKeyword("MTM_DATE", secLendTradesBean2.getMtmDate());

        // create NPV PLMArkValue
        JDatetime jdatetime = null;
        synchronized (dateFormat) {
            jdatetime = new JDatetime(dateFormat.parse(secLendTradesBean2.getMtmDate()));
        }
        final JDate jdt = jdatetime.getJDate(TimeZone.getDefault());

        final PLMark plMark = CollateralUtilities.createPLMarkIfNotExists(trade, DSConnection.getDefault(),
                pricingEnvName, jdt);

        final double npvValue = CollateralUtilities.convertCurrency(secLendTradesBean2.getMtmCcy(),
                Double.parseDouble(secLendTradesBean2.getMtm()), trade.getTradeCurrency(), jdt, this.pricingEnv);

        final PLMarkValue nPVMarkValue = CollateralUtilities.buildPLMarkValue(S_NPV, trade.getTradeCurrency(), npvValue,
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

        plMark.addPLMarkValue(nPVMarkValue);

        Double hairCut = null;
        try {
            if (!secLendTradesBean2.getHaircut().equals("")) {
                hairCut = Double.parseDouble(secLendTradesBean2.getHaircut());
            }
        } catch (final Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "26", "HAIRCUT", secLendTradesBean2.getBoReference(),
                    stringLine);
            Log.error(this, e); //sonar
            this.bResult = false;
        }

        CollateralUtilities.calculateMCAndNpvBase(this.conn, trade, plMark, this.pricingEnv, jdt, hairCut, errorMsgs);

        if (!Util.isEmpty(secLendTradesBean2.getIndependentAmount())
                && !Util.isEmpty(secLendTradesBean2.getIndependentAmountCcy())) {
            if (this.currencies.contains(secLendTradesBean2.getIndependentAmountCcy())) {
                final double indAmountValue = CollateralUtilities.convertCurrency(
                        secLendTradesBean2.getIndependentAmountCcy(),
                        Double.parseDouble(secLendTradesBean2.getIndependentAmount()), trade.getTradeCurrency(), jdt,
                        this.pricingEnv);
                CollateralUtilities.handleIndAmountAndBaseMarks(plMark, trade, null, indAmountValue,
                        trade.getTradeCurrency(), jdt, this.pricingEnv, errorMsgs);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "54", "INDEPENDENT_AMOUNT_CCY",
                        secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        }

        // Closing_Price
        if (!this.secLendTradesBean.getClosingPrice().equals("")) {
            try {
                CollateralUtilities.handleClosingPrice(plMark,
                        Double.parseDouble(this.secLendTradesBean.getClosingPrice()),
                        this.secLendTradesBean.getMtmCcy());
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "32", "CLOSING_PRICE",
                        this.secLendTradesBean.getBoReference(), stringLine);
                Log.error(this, e); //sonar
                this.bResult = false;
            }
        }

        return plMark;
    }

    /**
     * Method for get Trades
     *
     * @param filePath
     * @param separator
     * @return MODIFIED (Bean)
     */

    @SuppressWarnings({"rawtypes", "unused"})
    private Vector<Trade> getTrades(final String filePath, final String separator) {
        BufferedReader inputFileStream = null; // Input stream to read the CSV
        // file.;
        Trade trade;
        String line = null;
        String[] values = null;
        Vector<Trade> trades = null;
        boolean stopFile = false;
        Boolean isSLB = getBooleanAttribute(IS_SLB);

        try {
            inputFileStream = new BufferedReader(new FileReader(filePath));
            trades = new Vector<Trade>();
            for (int i = 0; (line = inputFileStream.readLine()) != null; i++) {
                this.logGen.initializeError();
                this.logGen.initializeWarning();
                this.logGen.initializeOK();
                this.logGen.initializeErrorLine();
                this.logGen.initializeWarningLine();
                this.logGen.initializeOkLine();
                long tradeId = 0;
                if ((line != null) && !line.contains("*****")) {
                    line = addEndSeparator(line);
                    /*Check if the file is from a SLB trade */
                    int nbColumns = isSLB ? 40 : 39;


                    if (CollateralUtilities.checkFields(line, '|', nbColumns)) {
                        values = CollateralUtilities.splitMejorado(nbColumns, separator, true, line);

                        for (int ii = 0; ii < values.length; ii++) {
                            values[ii] = values[ii].trim();
                        }

                        this.secLendTradesBean = new SecLendingTradesBean(values, isSLB);//constructor for the SLB
                        trade = getTrade(this.secLendTradesBean, i + 1, line);

                        if (trade != null) {
                            trade.setSalesPerson(NONE);
                            // We save the trade created.
                            final Vector tradeErrors = new Vector();
                            final Vector secLendingErrors = new Vector();

                            /* Chequeo de si es correcta la trade a importar */
                            if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                                try {
                                    final SecLending secLending = (SecLending) trade.getProduct();
                                    if (secLending.isValidInput(secLendingErrors) && trade.isValid(tradeErrors)) {
                                        try {
                                            //Mig v14 - GSM 10/05/2016 - adaptation to module 2.7.9 - Mig 14.
                                            addCMMaturityKeyword(trade);

                                            if (isSLB) {
                                                addSLBBundleKeyword(trade);
                                            }
                                            tradeId = saveTrade(trade, this.secLendTradesBean, line);
                                            trades.add(trade);
                                        } catch (final RemoteException e) {
                                            // Cannot save the trade. Error 4
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", e);
                                            this.logGen.incrementError();
                                            if (e.getMessage().contains("CANCELED")) {
                                                this.logGen.setErrorCancelingTrade(SOURCE, this.fileName,
                                                        String.valueOf(i + 1), this.secLendTradesBean.getBoReference(),
                                                        line);
                                            } else {
                                                this.logGen.setErrorSavingTrade(SOURCE, this.fileName,
                                                        String.valueOf(i + 1), this.secLendTradesBean.getBoReference(),
                                                        line);
                                            }

                                            this.bResult = false;
                                            this.proccesOK = false;
                                        }
                                    } else {
                                        // We create a String to concat the
                                        // errors
                                        // retrieved when we check if the Trade
                                        // is
                                        // or not valid - Jos? David Sevillano.
                                        String errorsConcat = "";
                                        for (int numErrors = 0; numErrors < tradeErrors.size(); numErrors++) {
                                            errorsConcat = errorsConcat + " " + tradeErrors.get(numErrors);
                                        }

                                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking Sec Lending Trade");
                                        this.bResult = false;
                                        this.logGen.incrementError();
                                        this.logGen.setErrorInformationIncomplete(SOURCE, this.fileName,
                                                String.valueOf(i + 1), this.secLendTradesBean.getBoReference(),
                                                errorsConcat, line);

                                    }
                                } catch (final Exception e) {
                                    this.bResult = false;
                                    this.logGen.incrementError();
                                    this.logGen.setErrorInformationIncomplete(SOURCE, this.fileName,
                                            String.valueOf(i + 1), this.secLendTradesBean.getBoReference(), "", line);
                                    Log.error(this, e); //sonar
                                }
                            }

                            if (this.logGen.getNumberError() > 0) {
                                this.logGen.incrementRecordErrors();
                            }
                            if ((this.logGen.getNumberWarning() > 0) && (this.logGen.getNumberError() == 0)) {
                                this.logGen.incrementRecordWarning();
                                if (this.errors.size() > 0) {
                                    this.addBadLine(line, "Error in the line");
                                    this.errors = new HashMap<String, Throwable>();
                                }
                            }
                            if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                                this.logGen.incrementOK();
                            }
                            this.logGen.incrementTotal();
                            if (this.logGen.getNumberOk() == 1) {
                                this.logGen.setOkLine(SOURCE, this.fileName, i + 1, String.valueOf(tradeId));
                            }
                            this.logGen.feedFullLog(tradeId);
                            this.logGen.feedDetailedLog(tradeId);

                            if ((this.logGen.getNumberWarning() > 0) && (this.logGen.getNumberError() == 0)) {
                                try {
                                    this.logGen.feedStaticDataLog(this.secLendTradesBean.getBoReference(), SYSTEM);

                                } catch (final Exception e) {
                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error writing in log files.");
                                    Log.error(this, e); //sonar
                                    this.logGen.setErrorWritingLog(SOURCE, this.fileName, String.valueOf(i + 1));

                                    this.logGen.feedFullLog(0);
                                    this.logGen.feedDetailedLog(0);
                                    this.bResult = false;
                                    ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
                                            "Unexpected error in log file"); // TODO
                                    // LOG
                                    // IO_ERROR
                                    // this.controlMOK = false;
                                }
                            }
                        } else { // trade == null cause in getting process there
                            // was some error
                            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error getting the trade.");
                            this.bResult = false;
                            this.logGen.incrementRecordErrors();
                            this.logGen.incrementTotal();
                            // errorLine+="SEC_LENDING;"+this.fileName+";"+(i+1)+";ERROR;;5;Bad
                            // record
                            // format;"+this.secLendTradesBean.getBoReference()+";"+line
                            // +".\n";
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);
                        }

                    } else {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
                        this.bResult = false;
                        this.logGen.incrementRecordErrors();
                        this.logGen.incrementTotal();
                        if (this.secLendTradesBean != null) {
                            this.logGen.setErrorBadRecordFormat(SOURCE, this.fileName, String.valueOf(i + 1), "", line,
                                    this.secLendTradesBean.getBoReference());
                        } else {
                            this.logGen.setErrorBadRecordFormat(SOURCE, this.fileName, String.valueOf(i + 1), "", line, "");
                        }
                        this.logGen.feedFullLog(0);
                        this.logGen.feedDetailedLog(0);
                    }


                    stopFile = true;
                }
            }

        } catch (final FileNotFoundException e) {
            // critical error 1
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
            this.bResult = false;
            this.logGen.incrementRecordErrors();
            this.proccesOK = false;
            this.logGen.setErrorNumberOfFiles(SOURCE, this.fileName);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
            this.controlMOK = false;
        } catch (final IOException e) {
            // Unexpected error opening the file. Critical error 2
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
            this.bResult = false;
            this.logGen.incrementRecordErrors();
            this.proccesOK = false;
            this.logGen.setErrorOpeningFile(SOURCE, this.fileName, String.valueOf(0));
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeRead, "");
            this.controlMOK = false;
        } finally {
            if (inputFileStream != null) {
                try {
                    inputFileStream.close();
                } catch (final IOException e) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while trying close input stream for the CSV file <"
                            + getFileName() + "> open previously", e);
                    this.bResult = false;
                    ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
                            "Error while trying close input stream for the CSV file.");
                    this.controlMOK = false;
                }
            }
        }
        return trades;
    }

    /**
     * add keyword CM acceptance for mature trades
     *
     * @param trade
     */
    //Mig v14 - GSM 10/05/2016 - adaptation to module 2.7.9 - Mig
    private void addCMMaturityKeyword(final Trade trade) {

        if (this.secLendTradesBean == null || Util.isEmpty(this.secLendTradesBean.getMtm()) || Util.isEmpty(this.secLendTradesBean.getMaturityDate()))
            return;

        String mtm = this.secLendTradesBean.getMtm();
        Double mtmValue = 0.0;

        try {
            if (!Util.isEmpty(mtm)) {
                mtmValue += Math.abs(Double.valueOf(mtm));
            }
            //remove mark if mtm == 0 or cancel action;
            if (mtmValue == 0 || CANCEL.equals(trade.getAction().toString())) {
                if (KEYWORD_CM_AFTER_MATURITY != null) {
                    final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
                    trade.addKeyword(keywordValue, "false");
                }
                return; //do nothing
            }

        } catch (Exception e) {
            Log.error(this, e);
            return;
        }
        JDate mtmDate = null;
        try {

            mtmDate = JDate.valueOf(dateFormat.parse(this.secLendTradesBean.getMaturityDate()));

        } catch (ParseException e) {
            Log.error(this, "Repo BO Ref: " + this.secLendTradesBean.getBoReference() + "Cannot convert to mtm date field " + this.secLendTradesBean.getMaturityDate());
        }

        if (mtmDate != null) {

            SecLending secLend = (SecLending) trade.getProduct();
            if (secLend.getMaturityDate().before(mtmDate)) {

                if (KEYWORD_CM_AFTER_MATURITY != null) {
                    final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
                    final String KeywordComment = KEYWORD_CM_AFTER_MATURITY.get(keywordValue);
                    trade.addKeyword(keywordValue, KeywordComment);
                }
            }
        }
    }


    /**
     * Add keyword SLB_Bundle for the SLB trades
     *
     * @param trade
     */
    private void addSLBBundleKeyword(final Trade trade) {

        if (this.secLendTradesBean == null)
            return;

        String slb_bundle = this.secLendTradesBean.getSLBBundle();
        if (!Util.isEmpty(slb_bundle)) {
            trade.addKeyword(KEYWORD_SLB_BUNDLE, this.secLendTradesBean.getSLBBundle());
        }

    }


    /**
     * Method for save a trade
     *
     * @param trade
     * @param secLendTradesBean2 SecLendingTradesBean with the values read.
     *                           <p>
     *                           MODIFIED (Bean)
     */

    private long saveTrade(Trade trade, final SecLendingTradesBean secLendTradesBean2, final String stringLine)
            throws RemoteException {
        // int tradeSaved = 0;
        final ArrayList<String> errorMsgs = new ArrayList<>();
        boolean correctPLMark = true;

        if (null != trade) {
            try {
                if (!Util.isEmpty(secLendTradesBean2.getMtm())) {
                    CollateralUtilities.handleUnSettledTrade(trade, Double.parseDouble(secLendTradesBean2.getMtm()),
                            getValuationDatetime().getJDate(TimeZone.getDefault()));
                }
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "34", "MTM", secLendTradesBean2.getBoReference(),
                        stringLine);
                Log.error(this, e); //sonar
            }
            // if ((trade.getLongId() == 0) ||
            // UNMATURE.equals(trade.getAction().toString())
            // || CANCEL.equals(trade.getAction().toString()) ||
            // MATURE.equals(trade.getAction().toString())
            // || AMEND.equals(trade.getAction().toString())) {
            // trade = this.remoteColService.saveTrade(trade, errorMsgs);
            // }
            if (CANCEL.equals(trade.getAction().toString())) {
                if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                    trade = this.remoteColService.saveTrade(trade, errorMsgs);
                    return trade.getLongId();
                }
            }

            // if (trade.getLongId() == 0) {
            try {

                if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                    //GSM 10/05/2016 - V14 Mig. Trades with action mature only can be save after plmaks - save too in
                    // method remoteColService.saveTradeWithPLMarks(trade, plMark, errorMsgs), lin. 742
                    if (!secLendTradesBean2.getAction().equals(MATURITY))
                        trade = this.remoteColService.saveTrade(trade, errorMsgs);
                    //trade = this.remoteColService.saveTrade(trade, errorMsgs);
                    if (!trade.getAction().equals(Action.valueOf(MATURE))) {
                        trade.setAction(Action.valueOf(AMEND));
                    }
                } else {
                    correctPLMark = false;
                }
            } catch (Exception e) {
                correctPLMark = false;
                this.bResult = false;
                Log.error(this, e); //sonar
            }
            // }

            // If the Action is CANCEL, we don't save the Trade. Jos? David
            // Sevillano.
            if (!CANCEL.equals(trade.getAction().toString()) && correctPLMark) {
                try {
                    final PLMark plMark = createPLMarks(trade, this.pricingEnv.getName(),
                            getValuationDatetime().getJDate(TimeZone.getDefault()), secLendTradesBean2, stringLine, errorMsgs);
                    //save plmark & trade

                    trade = this.remoteColService.saveTradeWithPLMarks(trade, plMark, errorMsgs);

                } catch (final Exception e) {
                    if (!e.toString().contains("MATURED")) {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error creating PL Mark", e);
                        this.bResult = false;
                        this.logGen.incrementError();
                        this.logGen.setErrorCreatingPlMark(SOURCE, this.fileName, stringLine,
                                secLendTradesBean2.getBoReference());
                    }
                }
                // if (!trade.getStatus().getStatus().equals(
                // CollateralStaticAttributes.STATUS_VERIFIED)) {
                // trade.setAction(Action.AMEND);
                // tradeSaved = this.remoteTrade.save(trade);
                // }
            }

            return trade.getLongId();
        }
        return 0;
    }

    private Trade getTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade trade = null;
        if (!secLendTradesBean2.getBoReference().equals("")) {
            if (secLendTradesBean2.getAction().equals(NEW)) {
                trade = newTrade(secLendTradesBean2, line, stringLine);
            } else if (secLendTradesBean2.getAction().equals(CANCEL)) {
                trade = cancelTrade(secLendTradesBean2, line, stringLine);
            } else if (secLendTradesBean2.getAction().equals(AMEND)) {
                trade = amendTrade(secLendTradesBean2, line, stringLine);
            } else if (secLendTradesBean2.getAction().equals(MATURITY)) {
                trade = matureTrade(secLendTradesBean2, line, stringLine);
            } else if (secLendTradesBean2.getAction().equals(ROLLOVER)) {
                trade = rolloverTrade(secLendTradesBean2, line, stringLine);
            } else if (secLendTradesBean2.getAction().equals(MTM)) {
                trade = mtmTrade(secLendTradesBean2, line, stringLine);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "6", "ACTION",
                        secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                    "BO_REFERENCE", "", stringLine);

            this.bResult = false;
        }
        return trade;
    }

    private Trade mtmTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        SecLending product = new SecLending();
        result.setProduct(product);

        try {
            if (!secLendTradesBean2.getBoReference().equals("")) {
                existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                        "trade.trade_id=trade_keyword.trade_id and trade.trade_status<>'CANCELED' and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                                + "'" + secLendTradesBean2.getBoReference() + "'",
                        null, null);
                for (int i = 0; i < existBoReference.size(); i++) {
                    result = existBoReference.get(i);
                    if (!secLendTradesBean2.getBoSystem().equals("")) {
                        if (result.getKeywordValue(BO_SYSTEM).contains(secLendTradesBean2.getBoSystem())) {
                            if (!result.getAction().equals(Action.MATURE)) {
                                product = (SecLending) result.getProduct();
                                result.setAction(Action.valueOf(AMEND));
                                result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                                return result;
                            } else {
                                product = (SecLending) result.getProduct();
                                result.setAction(Action.valueOf(UNMATURE));
                                result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                                return result;
                            }
                        }
                    }
                }
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            result.setAction(Action.valueOf(NEW));
            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
            return result;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
            Log.error(this, e); //sonar
        }
        return null;
    }

    private Trade rolloverTrade(final SecLendingTradesBean secLendTradesBean2, final int line,
                                final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        SecLending product = new SecLending();
        result.setProduct(product);

        try {
            existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                    "trade.trade_id=trade_keyword.trade_id and trade.trade_status<>'CANCELED' and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                            + "'" + secLendTradesBean2.getBoReference() + "'",
                    null, null);
            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(secLendTradesBean2.getBoSystem())) {
                    product = (SecLending) result.getProduct();
                    result.setAction(Action.valueOf(secLendTradesBean2.getAction()));
                    result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                    return result;
                }
            }
            // Record ROLLOVER received but transaction <XX> not present. Error
            // 13
            if (secLendTradesBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            this.logGen.incrementError();
            this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                    secLendTradesBean2.getBoReference(), secLendTradesBean2.getBoSystem(), stringLine, "ROLLOVER",
                    "13");

            this.bResult = false;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
            Log.error(this, e); //sonar
        }
        return null;
    }

    private Trade matureTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        SecLending product = new SecLending();
        result.setProduct(product);

        try {
            existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                    "trade.trade_id=trade_keyword.trade_id and trade.trade_status<>'CANCELED' and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                            + "'" + secLendTradesBean2.getBoReference() + "'",
                    null, null);
            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                product = (SecLending) result.getProduct();
                if (result.getKeywordValue(BO_SYSTEM).equals(secLendTradesBean2.getBoSystem())) {
                    if (!result.getAction().equals(Action.MATURE)) {
                        if (Double.parseDouble(secLendTradesBean2.getMtm()) != 0.0) {
                            result.setAction(Action.valueOf(AMEND));
                            return result;
                        } else {
                            result.setAction(Action.valueOf(MATURE));
                            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                            return result;
                        }
                    } else {
                        return result;
                    }
                }
            }
            // Record MATURITY received but transaction <XX> not present. Error
            // 12
            if (secLendTradesBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            if (Double.parseDouble(secLendTradesBean2.getMtm()) != 0.0) {
                result.setAction(Action.valueOf(NEW));
                result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                return result;
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                        secLendTradesBean2.getBoReference(), secLendTradesBean2.getBoSystem(), stringLine, "MATURITY",
                        "12");

                this.bResult = false;
            }
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
            Log.error(this, e); //sonar
        }
        return null;
    }

    private Trade amendTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        SecLending product = new SecLending();
        result.setProduct(product);

        try {
            existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                    "trade.trade_id=trade_keyword.trade_id and trade.trade_status<>'CANCELED' and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                            + "'" + secLendTradesBean2.getBoReference() + "'",
                    null, null);
            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(secLendTradesBean2.getBoSystem())) {
                    product = (SecLending) result.getProduct();
                    if (!result.getAction().equals(Action.MATURE)) {
                        if (!secLendTradesBean2.getMaturityDate().equals(secLendTradesBean2.getMtmDate())) {
                            product = (SecLending) result.getProduct();
                            result.setAction(Action.AMEND);
                            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                            return result;
                        } else {
                            result.setAction(Action.MATURE);
                            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                            return result;
                        }
                    } else {
                        if (!secLendTradesBean2.getMaturityDate().equals(secLendTradesBean2.getMtmDate())) {
                            product = (SecLending) result.getProduct();
                            result.setAction(Action.valueOf(UNMATURE));
                            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
                            return result;
                        } else {
                            return result;
                        }
                    }
                }

            }
            if (secLendTradesBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            result.setAction(Action.valueOf(NEW));
            result = buildTrade(result, product, true, secLendTradesBean2, line, stringLine);
            return result;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
            Log.error(this, e); //sonar
        }
        return null;
    }

    private Trade cancelTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;

        try {
            existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                    "trade.trade_id=trade_keyword.trade_id and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                            + "'" + secLendTradesBean2.getBoReference() + "'",
                    null, null);
            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(secLendTradesBean2.getBoSystem())) {
                    result.setAction(Action.valueOf(secLendTradesBean2.getAction()));
                    return result;
                }
            }
            // Record CANCEL received but transaction <XX> not present. Error 10
            if (secLendTradesBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            this.logGen.incrementError();
            this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                    secLendTradesBean2.getBoReference(), secLendTradesBean2.getBoSystem(), stringLine, "CANCEL", "10");

            this.bResult = false;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
            Log.error(this, e); //sonar
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Trade newTrade(final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        result.setAction(Action.valueOf(secLendTradesBean2.getAction()));
        final SecLending product = new SecLending();
        result.setProduct(product);

        TradeArray existBoReference = null;
        List existBoSystem = null;
        boolean isNew = true;

        try {
            existBoReference = this.remoteTrade.getTrades("trade, trade_keyword",
                    "trade.trade_id=trade_keyword.trade_id and trade.trade_status<>'CANCELED' and trade_keyword.keyword_name='BO_REFERENCE' and trade_keyword.keyword_value="
                            + "'" + secLendTradesBean2.getBoReference() + "'",
                    null, null);
            existBoSystem = this.remoteTrade.getKeywordDomain(BO_SYSTEM);

            if ((existBoReference.size() > 0) && existBoSystem.contains(secLendTradesBean2.getBoSystem())) {
                isNew = false;
            }

            if (secLendTradesBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (secLendTradesBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            if ((!secLendTradesBean2.getBoSystem().equals("")) && (!secLendTradesBean2.getBoReference().equals(""))) {
                if (isNew) {
                    result = buildTrade(result, product, isNew, secLendTradesBean2, line, stringLine);
                    return result;
                } else {
                    result = amendTrade(secLendTradesBean2, line, stringLine);
                    return result;
                }
            }

        } catch (final Exception e1) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e1.toString());
            Log.error(this, e1); //sonar
        }
        return null;
    }

    /**
     * Method for build a trade
     *
     * @param secLendTradesBean2 SecLendingTradesBean with the values read.
     * @param MODIFIED           (Bean)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Trade buildTrade(final Trade trade, final SecLending product, final boolean isNew,
                             final SecLendingTradesBean secLendTradesBean2, final int line, final String stringLine) {
        boolean noBookMapping = false;

        if ("true".equalsIgnoreCase(getAttribute(NO_BOOK_MAPPING))) {
            noBookMapping = true;
        }

        if (this.remoteReferenceData == null) {
            this.remoteReferenceData = this.conn.getRemoteReferenceData();
        }

        if (trade.getKeywords() == null) {
            trade.setKeywords(new Hashtable<String, String>());
        }

        if (!secLendTradesBean2.getFoSystem().equals("")) {
            trade.addKeyword("FO_SYSTEM", secLendTradesBean2.getFoSystem());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "52",
                    "FO_SYSTEM", secLendTradesBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        trade.setTradePrice(1);

        try {
            if (isNew) {
                if (!secLendTradesBean2.getNumFrontId().equals("")) {
                    trade.setExternalReference(secLendTradesBean2.getNumFrontId());
                    trade.addKeyword("NUM_FRONT_ID", secLendTradesBean2.getNumFrontId());
                    if (KEYWORD_CM_AFTER_MATURITY != null) {
                        final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
                        trade.addKeyword(keywordValue, "false");
                    }
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "51", "NUM_FRONT_ID", secLendTradesBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
            }
        } catch (Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "51",
                    "NUM_FRONT_ID", secLendTradesBean2.getBoReference(), stringLine);
            Log.error(this, e); //sonar
            this.bResult = false;
        }
        if (!secLendTradesBean2.getOwner().equals("")) {
            try {

                if (GenericValidations.validateLegalEntity(secLendTradesBean2.getOwner())) {
                    @SuppressWarnings("unused") final LegalEntity po = this.remoteReferenceData.getLegalEntity(secLendTradesBean2.getOwner());
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "14", "PROCESSING_ORG", secLendTradesBean2.getBoReference(), stringLine);

                    this.bResult = false;
                }

            } catch (final RemoteException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "14",
                        "PROCESSING_ORG", secLendTradesBean2.getBoReference(), stringLine);
                Log.error(this, e); //sonar
                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "14",
                    "PROCESSING_ORG", secLendTradesBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        try {

            if (GenericValidations.validateLegalEntity(secLendTradesBean2.getCounterparty())) {
                trade.setCounterParty(this.remoteReferenceData.getLegalEntity(secLendTradesBean2.getCounterparty()));
            } else {
                trade.setCounterParty(this.remoteReferenceData.getLegalEntity(NONE));
                trade.addKeyword(ORIG_SOURCE_CPTY, secLendTradesBean2.getCounterparty());
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "15",
                        "COUNTERPARTY", secLendTradesBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

        } catch (final RemoteException e) {
            this.logGen.incrementWarning();
            this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "15",
                    "COUNTERPARTY", secLendTradesBean2.getBoReference(), stringLine);
            Log.error(this, e); //sonar
            this.bResult = false;
        }

        if (!secLendTradesBean2.getInstrument().equals("")) {
            trade.getKeywords().put("INSTRUMENT", secLendTradesBean2.getInstrument());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "16", "INSTRUMENT",
                    secLendTradesBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        try {

            // GSM: 26/06/2013. Short/long Portfolio Development. Returns first
            // short name, if not tries long
            final String shortAlias = ALIAS_BOOK_SYSTEM + secLendTradesBean2.getBoSystem();
            final String longAlias = shortAlias + "_LONG";

            Book book = null;
            String bookMapped = "";

            if (noBookMapping && !Util.isEmpty(secLendTradesBean2.getPortfolio())) {
                book = this.conn.getRemoteReferenceData().getBook(secLendTradesBean2.getPortfolio());
                bookMapped = secLendTradesBean2.getPortfolio();
            } else if (GenericValidations.validatePortfolioMapped(secLendTradesBean2.getPortfolio(), longAlias, shortAlias)) {
                bookMapped = CollateralUtilities.getBookMappedName(secLendTradesBean2.getPortfolio(),
                        ALIAS_BOOK_SYSTEM + secLendTradesBean2.getBoSystem());
                book = this.conn.getRemoteReferenceData().getBook(bookMapped);
            }

            if (book != null) {
                secLendTradesBean2.setPortfolio(bookMapped);
                trade.setBook(book);
                trade.setBook(this.remoteReferenceData.getBook(secLendTradesBean2.getPortfolio()));
                final LegalEntity po = this.remoteReferenceData.getLegalEntity(secLendTradesBean2.getOwner());
                if (this.remoteReferenceData.getBook(secLendTradesBean2.getPortfolio()).getProcessingOrgBasedId() != po
                        .getId()) {
                    // BAU 5.2.0 - return warning instead of error in order to
                    // add this line in bad file
                    this.errors.put(line + ":PO is different from book LE:",
                            new Exception("PO is different from book LE."));
                    this.logGen.incrementWarning();
                    this.logGen.setWarningDifferentLegalEntities(SOURCE, this.fileName, String.valueOf(line), "73",
                            "PROCESSING_ORG", secLendTradesBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
            } else {
                this.errors.put(line + ":Error while looking for book:" + secLendTradesBean2.getPortfolio(),
                        new Exception("Book is not in the system."));
                trade.addKeyword(ORIG_SOURCE_BOOK, secLendTradesBean2.getPortfolio());
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "17",
                        "PORTFOLIO", secLendTradesBean2.getPortfolio(), secLendTradesBean2.getBoReference(),
                        stringLine);
                this.bResult = false;
            }
        } catch (final RemoteException e) {
            this.errors.put(line + ":Error while looking for book:" + secLendTradesBean2.getPortfolio(),
                    new Exception("Book is not in the system."));
            trade.addKeyword(ORIG_SOURCE_BOOK, secLendTradesBean2.getPortfolio());
            this.logGen.incrementWarning();
            this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "17",
                    "PORTFOLIO", secLendTradesBean2.getPortfolio(), secLendTradesBean2.getBoReference(), stringLine);
            Log.error(this, e); //sonar
            this.bResult = false;
        }

        addDates(secLendTradesBean2, line, trade, product, stringLine);// [7][8][9]

        if (!secLendTradesBean2.getDirection().equals("")) {
            if (secLendTradesBean2.getDirection().equals("BORROW")
                    || secLendTradesBean2.getDirection().equals("LEND") || secLendTradesBean2.getDirection().equals("LOAN")) {
                if (secLendTradesBean2.getDirection().equals("BORROW")) {
                    product.setDirection("Borrow");
                    trade.setQuantity(1);
                } else {
                    product.setDirection("Lend");
                    trade.setQuantity(-1);
                }
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "21",
                        "DIRECTION", secLendTradesBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "21",
                    "DIRECTION", secLendTradesBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        addCollaterals(trade, secLendTradesBean2, line, product, stringLine);
        // Controlar el collateral
        if ((null != product.getCollaterals()) && (product.getCollaterals().size() > 0)) {
            if (!secLendTradesBean2.getClosingPriceAtStar().equals("")) {
                final Vector collaterals = product.getCollaterals();
                final Collateral collateral = (Collateral) collaterals.get(0);
                if (collateral != null) {
                    collateral.setInitialPrice(CollateralBasedUtil.convertToCleanPrice(
                            Double.parseDouble(secLendTradesBean2.getClosingPriceAtStar()), collateral));
                }
            }
        }

        if (!secLendTradesBean2.getNominalCcy().equals("")
                && this.currencies.contains(secLendTradesBean2.getNominalCcy())) {
            product.setCurrency(secLendTradesBean2.getNominalCcy());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "25",
                    "NOMINAL_CCY", secLendTradesBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        // The information not defined in Calypso objects, we need to insert by
        // KEYWORDS. We use the same names for the configured KEYWORDS.
        if (!secLendTradesBean2.getBoSystem().equals("")) {
            trade.getKeywords().put(BO_SYSTEM, secLendTradesBean2.getBoSystem());
        }
        // The error information is showed above
        // else {
        // this.logGen.incrementError();
        // this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName,
        // String.valueOf(line), "8", "BO_SYSTEM",
        // secLendTradesBean2.getBoReference(), stringLine);
        //
        // this.bResult = false;
        // }

        if (!secLendTradesBean2.getBoReference().equals("")) {
            trade.getKeywords().put("BO_REFERENCE", secLendTradesBean2.getBoReference());
        }
        // else {
        // this.logGen.incrementError();
        // this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName,
        // String.valueOf(line), "7", "BO_REFERENCE",
        // secLendTradesBean2.getBoReference(), stringLine);
        //
        // this.bResult = false;
        // }

        if (secLendTradesBean2.getClosingPrice() != null) {
            trade.getKeywords().put("CLOSING_PRICE", secLendTradesBean2.getClosingPrice());
        }
        if (secLendTradesBean2.getStructureId() != null) {
            trade.getKeywords().put("STRUCTURE_ID", secLendTradesBean2.getStructureId());
        }

        if (!secLendTradesBean2.getIndependentAmount().equals("")) {
            final Fee fee = new Fee();
            fee.setType("SECURITY");
            double amount = 0.0;
            amount = Double.parseDouble(secLendTradesBean2.getIndependentAmount());
            if (secLendTradesBean2.getIndependentAmountPayRecieve().equals("PAY")) {
                amount = amount * (-1);
            }
            fee.setAmount(amount);
            if (!secLendTradesBean2.getIndependentAmountCcy().equals("")) {
                fee.setCurrency(secLendTradesBean2.getIndependentAmountCcy());
            }
            fee.setDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
            trade.addFee(fee);
        }

        // @todo we need to use always - SEC_COLLATERAL_POOL
        if (!secLendTradesBean2.getTransactionType().equals("")) {
            product.setSecLendingType("Sec Vs Collateral Pool"); // Put always
            // to Sec Vs
            // Collateral
            // Pool by
            // default,
            // Jos? David
            // Sevillano.
            if (secLendTradesBean2.getTransactionType().equals("SEC_VS_CASH")) {
                trade.getKeywords().put("SL_TYPE", "DVP");
            } else if (secLendTradesBean2.getTransactionType().equals("SEC_VS_POOL")
                    || "UNSECURED".equalsIgnoreCase(secLendTradesBean2.getTransactionType())) {
                trade.getKeywords().put("SL_TYPE", "FOP");
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorTransactionType(SOURCE, this.fileName, stringLine,
                        secLendTradesBean2.getBoReference());

                this.bResult = false;
            }
        }
        // else {
        // this.logGen.incrementError();
        // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
        // this.fileName, String.valueOf(line), "21.1",
        // "TRANSACTION_TYPE", secLendTradesBean2.getBoReference(),
        // stringLine);
        // this.bResult = false;
        // }

        if (secLendTradesBean2.getOpenTerm().equals("OPEN")) {
            product.setOpenTermB(true);
        }
        if (!secLendTradesBean2.getTerminable().equals("")) {
            product.setTerminableFlag(Integer.parseInt(secLendTradesBean2.getTerminable()));
        }
        if (!secLendTradesBean2.getSubstitution().equals("")) {
            if (Integer.parseInt(secLendTradesBean2.getSubstitution()) == 1) {
                product.setSubstitutionB(true);
            }
        }
        if (!secLendTradesBean2.getNoticeDays().equals("")) {
            product.setNoticeDays(Integer.parseInt(secLendTradesBean2.getNoticeDays()));
        }

        /* We set this two values for the valid method */
        product.setFillType(NONE);
        product.setAllocationType("Under Collateralized");
        if (!secLendTradesBean2.getNominalCcy().equals("")) {
            trade.setSettleCurrency(secLendTradesBean2.getNominalCcy());
            trade.setTradeCurrency(secLendTradesBean2.getNominalCcy());
        }

        try {
            if (!secLendTradesBean2.getLastModified().equals("")) {
                synchronized (dateFormat) {
                    trade.setUpdatedTime(new JDatetime(dateFormat.parse(secLendTradesBean2.getLastModified())));
                }
            }
        } catch (final ParseException e) {
            this.logGen.incrementError();
            this.logGen.setErrorLastModified(SOURCE, this.fileName, stringLine, secLendTradesBean2.getBoReference());

            this.bResult = false;
        }

        if (!secLendTradesBean2.getTradeVersion().equals("")) {
            trade.setVersion(Integer.parseInt(secLendTradesBean2.getTradeVersion()));
        }
        if (!secLendTradesBean2.getTrader().equals("")) {
            trade.setTraderName(secLendTradesBean2.getTrader());
        } else {
            trade.setTraderName(NONE);
        }

        if (!secLendTradesBean2.getSusiDate().equals("")) {
            synchronized (dateFormat) {
                dateFormat.setLenient(false);
                try {
                    dateFormat.parse(secLendTradesBean2.getSusiDate());
                    trade.getKeywords().put("REAL_TRADE_DATE", secLendTradesBean2.getSusiDate());
                } catch (ParseException e) {
                    this.logGen.incrementError();
                    this.logGen.setErrorSusiDateNotValid(SOURCE, this.fileName, String.valueOf(line),
                            secLendTradesBean2.getBoReference(), stringLine);

                    this.bResult = false;
                }
            }

        }
        // if (!secLendTradesBean2.getTrader().equals("")) {
        // trade.setTraderName(secLendTradesBean2.getTrader());
        // } else {
        // trade.setTraderName(NONE);
        // }

        return trade;
    }

    /**
     * Method for add security to the product
     *
     * @param secLendTradesBean2 SecLendingTradesBean with the values imported.
     * @param line               number of line reading actually for the file
     * @param product            SecLending for collateral
     *                           <p>
     *                           MODIFIED (Bean)
     */
    private void addCollaterals(final Trade trade, final SecLendingTradesBean secLendTradesBean2, final int line,
                                final SecLending product, final String stringLine) {
        final HashSet<String> isinSet = new HashSet<String>();
        isinSet.add(secLendTradesBean2.getUnderlying());
        Vector<Equity> equity = null;

        try {
            equity = getEquities(isinSet, secLendTradesBean2.getNominalCcy());

            if (equity.size() != 1) {
                product.setCollaterals(new Vector<Collateral>());
                trade.addKeyword(ORIG_SOURCE_ISIN, secLendTradesBean2.getUnderlyingType());
                // Required field ISIN <XX> not present or not valid. Warning 22
                this.logGen.incrementWarning();
                this.bResult = false;
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "22",
                        "ISIN", secLendTradesBean2.getUnderlying(), secLendTradesBean2.getBoReference(), stringLine);

                this.errors.put(line + ":Error while looking for equity:" + secLendTradesBean2.getUnderlying(),
                        new Exception("Equity not found or Equity is not enabled"));
            } else {
                final Vector<Collateral> collaterals = new Vector<Collateral>();
                final Collateral collateral = new Collateral();
                collaterals.add(collateral);
                collateral.setSecurity(equity.get(0));
                if (!secLendTradesBean2.getQtyNom().equals("")) {
                    if (secLendTradesBean2.getQtyNom().equals("QTY")) {
                        if (!secLendTradesBean2.getNominal().equals("")) {
                            try {
                                if (secLendTradesBean2.getDirection().equals("BORROW")) {
                                    collateral.setQuantity(Double.parseDouble(secLendTradesBean2.getNominal()));// *equity.get(0).getNominalDecimals());
                                } else if (secLendTradesBean2.getDirection().equals("LEND") || secLendTradesBean2.getDirection().equals("LOAN")) {
                                    collateral.setQuantity(-1 * Double.parseDouble(secLendTradesBean2.getNominal()));// *equity.get(0).getNominalDecimals());
                                }
                            } catch (final Exception e) {
                                this.logGen.incrementError();
                                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                                        String.valueOf(line), "24", "NOMINAL", secLendTradesBean2.getBoReference(),
                                        stringLine);
                                Log.error(this, e); //sonar
                                this.bResult = false;
                            }
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                                    String.valueOf(line), "24", "NOMINAL", secLendTradesBean2.getBoReference(),
                                    stringLine);
                            this.bResult = false;
                        }
                    } else if (secLendTradesBean2.getQtyNom().equals("NOM")) {
                        if (!secLendTradesBean2.getNominal().equals("")) {
                            collateral.setQuantity(Double.parseDouble(secLendTradesBean2.getNominal()));// *equity.get(0).getNominalDecimals());
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                                    String.valueOf(line), "24", "NOMINAL", secLendTradesBean2.getBoReference(),
                                    stringLine);
                            this.bResult = false;
                        }
                    } else {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "46", "QTY_NOM", secLendTradesBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                }

                // Haircut
                final String hairCutStr = secLendTradesBean2.getHaircut();
                if (!Util.isEmpty(hairCutStr)) {
                    try {
                        if (!secLendTradesBean2.getHaircut().equals("")) {
                            collateral.setHaircut("".equals(secLendTradesBean2.getHaircut()) ? 0
                                    : Double.parseDouble(secLendTradesBean2.getHaircut()));
                            trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT, hairCutStr);
                        }
                    } catch (final Exception e) {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "26", "HAIRCUT", secLendTradesBean2.getBoReference(), stringLine);
                        Log.error(this, e); //sonar
                        this.bResult = false;
                    }

                    if (!secLendTradesBean2.getHaircutDirection().equals("")) {
                        collateral.setHaircutSign(
                                secLendTradesBean2.getHaircutDirection().equals(GIVE_DIRECTION) ? -1 : 1);
                    } else {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "27", "HAIRCUT_DIRECTION", secLendTradesBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                } else {
                    collateral.setHaircut(0);
                }

                collateral.setStartDate(JDate.valueOf(secLendTradesBean2.getValueDate()));
                collateral.setTradeDate(JDate.valueOf(secLendTradesBean2.getTradeDate()));
                collateral.setHaircutType(REGULAR);

                try {
                    if (!secLendTradesBean2.getClosingPriceAtStar().equals("")) {
                        collateral.setCollateralPrice(Double.parseDouble(secLendTradesBean2.getClosingPriceAtStar()));
                    } else {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "23", "CLOSING_PRICE_AT_START", secLendTradesBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                } catch (final Exception e) {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "23", "CLOSING_PRICE_AT_START", secLendTradesBean2.getBoReference(), stringLine);
                    Log.error(this, e); //sonar
                    this.bResult = false;
                }

                product.setCollaterals(collaterals);
                // product.computeValue(arg0, arg1, arg2, arg3);
            }
        } catch (final RemoteException e) {
            this.logGen.incrementWarning();
            this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "22",
                    "ISIN", secLendTradesBean2.getUnderlying(), secLendTradesBean2.getBoReference(), stringLine);
            this.bResult = false;
            Log.error(this, e); //sonar
            this.errors.put(line + ":Error while looking for equity:" + secLendTradesBean2.getUnderlying(),
                    new Exception("Equity not found or Equity is not enabled"));
        }
    }

    /**
     * Method for insert the date into the file to the trade
     *
     * @param secLendTradesBean2 SecLendingTradesBean with the values imported.
     * @param line               number of line reading actually for the file
     * @param trade              Object trade where we set the dates
     *                           <p>
     *                           MODIFIED (Bean)
     */

    private void addDates(final SecLendingTradesBean secLendTradesBean2, final int line, final Trade trade,
                          SecLending product, final String stringLine) {

        try {
            synchronized (dateFormat) {
                dateFormat.setLenient(false);
                dateFormat.parse(secLendTradesBean2.getTradeDate());
                final JDatetime jdt = new JDatetime(dateFormat.parse(secLendTradesBean2.getTradeDate()));
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(jdt.getTime());
                cal.set(Calendar.AM_PM, Calendar.AM);
                cal.set(Calendar.HOUR, 2);
                trade.setTradeDate(new JDatetime(cal.getTime()));
            }
        } catch (final ParseException e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "18",
                    "TRADE_DATE", secLendTradesBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        try {
            synchronized (dateFormat) {
                dateFormat.setLenient(false);
                dateFormat.parse(secLendTradesBean2.getValueDate());
                final JDatetime jdt = new JDatetime(dateFormat.parse(secLendTradesBean2.getValueDate()));
                final JDate jd = jdt.getJDate(TimeZone.getDefault());
                product.setStartDate(jd);
                trade.setSettleDate(jd);
            }
        } catch (final ParseException e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "19",
                    "VALUE_DATE", secLendTradesBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        try {
            if (secLendTradesBean2.getOpenTerm().equals("TERM")) {
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    dateFormat.parse(secLendTradesBean2.getMaturityDate());
                    final JDate jd = new JDatetime(dateFormat.parse(secLendTradesBean2.getMaturityDate())).getJDate(TimeZone.getDefault());
                    product.setMaturityDate(jd);
                }
            }
        } catch (final ParseException e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "20",
                    "MATURITY_DATE", secLendTradesBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

    }

    @SuppressWarnings("unchecked")
    private Vector<Equity> getEquities(final Set<String> isinSet, String curr) throws RemoteException {

        Vector<Equity> products = new Vector<Equity>();
        final Vector<Equity> productsEnabled = new Vector<Equity>();
        String isin;
        final Iterator<String> keySetIterator = isinSet.iterator();
        while (keySetIterator.hasNext()) {
            for (int i = 0; (i < 1000) && keySetIterator.hasNext(); i++) {
                isin = keySetIterator.next();
                products = DSConnection.getDefault().getRemoteProduct().getProductsByCode("ISIN", isin);
                if (!Util.isEmpty(products)) {
                    for (int j = 0; j < products.size(); j++) {
                        final Product equity = products.get(j);
                        if ((equity instanceof Equity) && equity.getCurrency().equals(curr)) {
                            productsEnabled.add((Equity) equity);
                            break;
                        }
                    }
                }
            }
        }
        return productsEnabled;
    }

    @Override
    public String getFileName() {
        return this.file;
    }

    @Override
    public boolean feedPreProcessSusiGBO(String file) throws Exception {

        if ("true".equalsIgnoreCase(getAttribute(CONTROL_LINE))) {
            return true;
        }
        return super.feedPreProcessSusiGBO(file);
    }

    private String addEndSeparator(String line) {
        if ("true".equalsIgnoreCase(getAttribute(ADD_END_SEPARATOR))) {
            String sep = "|";
            if (!Util.isEmpty(getAttribute(SEPARATOR_DOMAIN_STRING))) {
                sep = getAttribute(SEPARATOR_DOMAIN_STRING);
            }
            return line += sep;
        }
        return line;
    }


}

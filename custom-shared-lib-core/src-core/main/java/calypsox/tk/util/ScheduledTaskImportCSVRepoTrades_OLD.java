package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.util.bean.RepoBean;
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
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
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

/**
 * To import Repo Trades from a CSV file.
 *
 * @author Jose David Sevillano (josedavid.sevillano@siag.es)
 */
public class ScheduledTaskImportCSVRepoTrades_OLD extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 123L;

    private static final String BO_SYSTEM = "BO_SYSTEM";
    private static final String MATURE = "MATURE";
    private static final String MTM = "MTM";
    private static final String ROLLOVER = "ROLLOVER";
    private static final String MATURITY = "MATURITY";
    private static final String AMEND = "AMEND";
    private static final String CANCEL = "CANCEL";
    private static final String NEW = "NEW";
    private static final String UNMATURE = "UNMATURE";
    private static final String REGULAR = "Regular";
    private static final String ORIG_SOURCE_CPTY = "ORIG_SOURCE_CPTY";
    private static final String ORIG_SOURCE_BOOK = "ORIG_SOURCE_BOOK";
    private static final String ORIG_SOURCE_ISIN = "ORIG_SOURCE_ISIN";
    private static final String ALIAS_BOOK_SYSTEM = "ALIAS_BOOK_";
    protected static final String SEPARATOR_DOMAIN_STRING = "Separator";

    private static String FORBIDDEN_CHARS = "!@$&*?:,[]<>'`|={}";

    private static final int ORIGINAL_NUMBER_OF_FIELDS = 35;
    private static final int NEW_NUMBER_OF_FIELDS = 41;
    private static int fileNumberOfFields;

    private static final String REPO_TRADES = "REPO_TRADES";
    private static final String SOURCE_SYSTEM = "Source System";
    private static final String ALIAS_BOOK_SYSTEM_ST = "Alias Book System";
    private static final String SUBJECT = "Log files for Import Repo Trades on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    //GSM 27/07/2016 - Cross Currency Repos Allowed
    private final static String ALLOW_CC_REPO = "Allow Cross Currency Repos";

    protected static final String TASK_INFORMATION = "Import TRADES from a CSV file.";
    protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();
    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    protected static final String BUY_DIRECTION_STRING = "BUY";
    protected static final String SELL_DIRECTION_STRING = "SELL";
    protected static final String GIVE_DIRECTION = "GIVE";
    protected static final String RECEIVE_DIRECTION = "RECEIVE";
    protected static final String PROCESS = "Load of Repos from ";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static Map<String, String> KEYWORD_CM_AFTER_MATURITY;
    private DSConnection conn;
    private RemoteReferenceData remoteReferenceData = null;
    protected RemoteProduct remoteProduct = null;
    protected RemoteSantCollateralService remoteColService = null;
    private RemoteTrade remoteTrade = null;
    protected HashMap<String, Throwable> errors = new HashMap<String, Throwable>();
    private PricingEnv pricingEnv;
    private boolean bResult = true;
    @SuppressWarnings("unused")
    private boolean proccesOK = true;
    private boolean controlMOK = true;
    private String file = "";
    private String fileName = "";
    private RepoBean repoBean;
    private static final String SOURCE = "REPO";
    private Vector<String> currencies;

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

        attributeList.add(attribute(SOURCE_SYSTEM));
        attributeList.add(attribute(ALIAS_BOOK_SYSTEM_ST));
        attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(DETAILED_LOG));
        attributeList.add(attribute(FULL_LOG));
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(ALLOW_CC_REPO).booleanType());

        return attributeList;
    }


//	@Override
//	public Vector<String> getDomainAttributes() {
//		@SuppressWarnings("deprecation")
//		final Vector<String> vectorAttr = super.getDomainAttributes();
//		vectorAttr.add(SOURCE_SYSTEM);
//		vectorAttr.add(ALIAS_BOOK_SYSTEM_ST);
//		vectorAttr.add(SEPARATOR_DOMAIN_STRING);
//		vectorAttr.add(SUMMARY_LOG);
//		vectorAttr.add(DETAILED_LOG);
//		vectorAttr.add(FULL_LOG);
//		vectorAttr.add(STATIC_DATA_LOG);
//		return vectorAttr;
//	}

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
            this.logGen.initializeFiles(PROCESS + getAttribute(SOURCE_SYSTEM));

            this.conn = conn;
            this.remoteProduct = conn.getRemoteProduct();
            this.remoteColService = (RemoteSantCollateralService) conn.getRMIService("baseSantCollateralService",
                    RemoteSantCollateralService.class);

            this.remoteTrade = conn.getRemoteTrade();
            this.remoteReferenceData = conn.getRemoteReferenceData();

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
                final String filePath = path + this.file;
                this.fileName = this.file.toString();
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
                        this.bResult = false;

                        this.proccesOK = false;
                        ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
                        this.controlMOK = false;
                    }
                } catch (final Exception e) {
                    // Number of lines in file does not match with number of
                    // lines in control record. Critical error 3
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorNumberOfLines(SOURCE, this.file);

                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    this.bResult = false;

                    this.proccesOK = false;
                    ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
                    this.controlMOK = false;
                }
            } else {
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorNumberOfFiles(SOURCE, this.file);
                this.bResult = false;
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
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files.");
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorCreatingLogFile(SOURCE, this.fileName);

            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            this.bResult = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file");
            // this.controlMOK = false;
        }

        try {
            this.feedPostProcess(this.bResult);

            this.logGen.closeLogFiles();

            String sumLog = "";
            if (this.file.equals("")) {
                sumLog = this.logGen.feedGenericLogProcess(startFileName, getAttribute(SUMMARY_LOG),
                        PROCESS + getAttribute(SOURCE_SYSTEM), this.logGen.getNumberTotal() - 1);

            } else {
                sumLog = this.logGen.feedGenericLogProcess(this.file, getAttribute(SUMMARY_LOG),
                        PROCESS + getAttribute(SOURCE_SYSTEM), this.logGen.getNumberTotal() - 1);
            }

            try {
                if (!sumLog.equals("")) {
                    final List<String> to = conn.getRemoteReferenceData().getDomainValues(REPO_TRADES);
                    final ArrayList<String> attachments = new ArrayList<String>();
                    attachments.add(sumLog);
                    attachments.add(this.logGen.getStringDetailedLog());
                    attachments.add(this.logGen.getStringFullLog());
                    attachments.add(this.logGen.getStringStaticDataLog());
                    CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                }
            } catch (final MailException me) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error sending log mail.");
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSentEmail(SOURCE, this.fileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                this.proccesOK = false;
                // ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
            }

        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files.");
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorMovingFile(SOURCE, this.fileName);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.proccesOK = false;
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "");
            this.controlMOK = false;
        }

        try {
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
        }

        if (this.controlMOK) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }

        // Se ha cambiado para devolver siempre lo mismo que se devuelve para
        // ControlM
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
                                 final RepoBean repoBean2, final String stringLine, final ArrayList<String> errorMsgs) throws Exception {

        if (repoBean2.getDate().equals("")) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "31", "MTM_DATE", repoBean2.getBoReference(),
                    stringLine);

        } else {
            try {
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    dateFormat.parse(repoBean2.getDate());
                }
                trade.getKeywords().put(TradeInterfaceUtils.TRD_IMP_FIELD_MTM_DATE, repoBean2.getDate());
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "31", "MTM_DATE", repoBean2.getBoReference(),
                        stringLine);
            }

        }
        if (repoBean2.getMtm().equals("")) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "34", "MTM", repoBean2.getBoReference(), stringLine);

        }
        if (repoBean2.getMtmCcy().equals("") || !this.currencies.contains(repoBean2.getMtmCcy())) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "35", "MTM_CCY", repoBean2.getBoReference(),
                    stringLine);

        }
        trade.addKeyword("MTM_DATE", repoBean2.getDate());

        // create PLMark
        JDatetime jdatetime = null;
        synchronized (dateFormat) {
            jdatetime = new JDatetime(dateFormat.parse(repoBean2.getDate()));
        }
        final JDate jdt = jdatetime.getJDate(TimeZone.getDefault());

        final PLMark plMark = CollateralUtilities.createPLMarkIfNotExists(trade, DSConnection.getDefault(),
                pricingEnvName, jdt);

        // NPV PLMArkValue
        final double npvValue = CollateralUtilities.convertCurrency(repoBean2.getMtmCcy(),
                Double.parseDouble(repoBean2.getMtm()), trade.getTradeCurrency(), jdt, this.pricingEnv);

        final PLMarkValue nPVMarkValue = CollateralUtilities.buildPLMarkValue(S_NPV, trade.getTradeCurrency(), npvValue,
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

        plMark.addPLMarkValue(nPVMarkValue);

        // MARGIN_CALL PLMArkValue
        Double hairCut = null;
        try {
            if (!repoBean2.getHaircut().equals("")) {
                hairCut = Double.parseDouble(repoBean2.getHaircut());
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "26", "HAIRCUT", repoBean2.getBoReference(),
                        stringLine);

                this.bResult = false;
            }
        } catch (final Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "26", "HAIRCUT", repoBean2.getBoReference(),
                    stringLine);

            this.bResult = false;
        }
        CollateralUtilities.calculateMCAndNpvBase(this.conn, trade, plMark, this.pricingEnv, jdt, hairCut, errorMsgs);

        // IA PLMArkValue
        if (!Util.isEmpty(repoBean2.getIndependentAmount()) && !Util.isEmpty(repoBean2.getIndependentAmountCcy())) {

            if (this.currencies.contains(repoBean2.getIndependentAmountCcy())) {
                final double indAmountValue = CollateralUtilities.convertCurrency(repoBean2.getIndependentAmountCcy(),
                        Double.parseDouble(repoBean2.getIndependentAmount()), trade.getTradeCurrency(), jdt,
                        this.pricingEnv);

                CollateralUtilities.handleIndAmountAndBaseMarks(plMark, trade, null, indAmountValue,
                        trade.getTradeCurrency(), jdt, this.pricingEnv, errorMsgs);

            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "54", "INDEPENDENT_AMOUNT_CCY",
                        repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        }

        // CLOSING_PRICE PLMArkValue
        if (!this.repoBean.getClosingPriceDaily().equals("")) {
            try {
                CollateralUtilities.handleClosingPrice(plMark, Double.parseDouble(this.repoBean.getClosingPriceDaily()),
                        this.repoBean.getMtmCcy());
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "32", "CLOSING_PRICE",
                        this.repoBean.getBoReference(), stringLine);

                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "32", "CLOSING_PRICE", this.repoBean.getBoReference(),
                    stringLine);
            this.bResult = false;
        }

        // Insercción RepoEnhacement fields //

        if (fileNumberOfFields > ORIGINAL_NUMBER_OF_FIELDS) {
            // REPO_ACCRUED_INTEREST
            try {
                String repoAccruedInterest = repoBean2.getRepoAccruedInterest();
                CollateralUtilities.handleRepoEnhacementField(SantPricerMeasure.S_REPO_ACCRUED_INTEREST, plMark,
                        Double.parseDouble(repoAccruedInterest), this.repoBean.getMtmCcy());
            } catch (NumberFormatException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "67", "REPO_ACCRUED_INTEREST",
                        repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

            // BOND_ACCRUED_INTEREST
            if (!Util.isEmpty(repoBean2.getBondAccruedInterest())) {
                try {
                    String bondAccruedInterest = repoBean2.getBondAccruedInterest();
                    CollateralUtilities.handleRepoEnhacementField(SantPricerMeasure.S_BOND_ACCRUED_INTEREST, plMark,
                            Double.parseDouble(bondAccruedInterest), this.repoBean.getMtmCcy());
                } catch (NumberFormatException e) {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                            String.valueOf(this.logGen.getNumberTotal()), "68", "BOND_ACCRUED_INTEREST",
                            repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
            }

            // CLEAN_PRICE
            try {
                String cleanPrice = repoBean2.getCleanPrice();
                CollateralUtilities.handleRepoEnhacementField(SantPricerMeasure.S_CLEAN_PRICE, plMark,
                        Double.parseDouble(cleanPrice), this.repoBean.getMtmCcy());
            } catch (NumberFormatException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "69", "CLEAN_PRICE", repoBean2.getBoReference(),
                        stringLine);
                this.bResult = false;
            }

            // CAPITAL_FACTOR
            try {
                String capitalFactor = repoBean2.getCapitalFactor();
                CollateralUtilities.handleRepoEnhacementField(SantPricerMeasure.S_CAPITAL_FACTOR, plMark,
                        Double.parseDouble(capitalFactor), this.repoBean.getMtmCcy());
            } catch (NumberFormatException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "70", "CAPITAL_FACTOR",
                        repoBean2.getBoReference(), stringLine);
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
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private Vector<Trade> getTrades(final String filePath, final String separator) {
        BufferedReader inputFileStream = null; // Input stream to read the CSV
        // file.;
        Trade trade;
        String line = null;
        String[] values = null;
        Vector<Trade> trades = null;
        boolean stopFile = false;

        try {
            inputFileStream = new BufferedReader(new FileReader(filePath));
            trades = new Vector<Trade>();
            for (int i = 0; !stopFile && ((line = inputFileStream.readLine()) != null); i++) {
                this.logGen.initializeError();
                this.logGen.initializeWarning();
                this.logGen.initializeOK();
                this.logGen.initializeErrorLine();
                this.logGen.initializeWarningLine();
                this.logGen.initializeOkLine();
                long tradeId = 0;
                this.errors = new HashMap<String, Throwable>();

                if ((line != null) && !line.contains("*****")) {

                    values = getValuesFromLine(line);
                    fileNumberOfFields = values.length;

                    if (checkNumberOfFields(values)) {

                        values = mapValues(values);

                        this.repoBean = new RepoBean(values);
                        trade = getTrade(this.repoBean, i + 1, line);

                        if (trade != null && trade.getProduct() instanceof Repo) {
                            // We save as NONE the information by default about
                            // the Trader and the Sales Person - José David
                            // Sevillano.
                            trade.setTraderName("NONE");
                            trade.setSalesPerson("NONE");

                            // We save the trade created.
                            final Vector tradeErrors = new Vector();
                            final Vector repoErrors = new Vector();
                            final Repo repo = (Repo) trade.getProduct();

                            /* Chequeo de si es correcta la trade a importar */
                            if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                                try {
                                    if (repo.isValidInput(repoErrors) && trade.isValid(tradeErrors)) {
                                        try {
                                            //Mig v14 - GSM 10/05/2016 - adaptation to module 2.7.9 - Mig 14.
                                            addCMMaturityKeyword(trade);

                                            tradeId = saveTrade(trade, this.repoBean, line);
                                            trades.add(trade);
                                        } catch (final RemoteException e) {
                                            // Cannot save the trade. Error 4
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", e);
                                            this.logGen.incrementError();
                                            if (e.getMessage().contains("CANCELED")) {
                                                this.logGen.setErrorCancelingTrade(SOURCE, this.fileName,
                                                        String.valueOf(i + 1), this.repoBean.getBoReference(), line);
                                            } else {
                                                this.logGen.setErrorSavingTrade(SOURCE, this.fileName,
                                                        String.valueOf(i + 1), this.repoBean.getBoReference(), line);
                                            }

                                            this.bResult = false;
                                            this.proccesOK = false;
                                        }
                                    } else {
                                        // We create a String to concat the
                                        // errors
                                        // retrieved when we check if the Trade
                                        // is
                                        // or not valid - José David Sevillano.
                                        String errorsConcat = "";
                                        for (int numErrors = 0; numErrors < tradeErrors.size(); numErrors++) {
                                            errorsConcat = errorsConcat + " " + tradeErrors.get(numErrors);
                                        }

                                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking Repo and Trade");
                                        this.bResult = false;
                                        this.logGen.incrementError();
                                        this.logGen.setErrorInformationIncomplete(SOURCE, this.fileName,
                                                String.valueOf(i + 1), errorsConcat, this.repoBean.getBoReference(),
                                                line);

                                        // en caso de no poder salvar esta
                                        // operación
                                        // errror crítico. Se para el proceso.
                                    }
                                } catch (final Exception e) {
                                    this.bResult = false;
                                    this.logGen.incrementError();
                                    this.logGen.setErrorInformationIncomplete(SOURCE, this.fileName,
                                            String.valueOf(i + 1), "", this.repoBean.getBoReference(), line);
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
                                    this.logGen.feedStaticDataLog(this.repoBean.getBoReference(),
                                            getAttribute(SOURCE_SYSTEM));

                                } catch (final Exception e) {
                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error writing in log files.");
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
                            if (trade != null) {
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "The product of the trade is not a Repo, trade id: " + trade.getLongId());
                            } else {
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Cannot load, trade = null");
                            }
                            this.bResult = false;
                            this.logGen.incrementRecordErrors();
                            this.logGen.incrementTotal();
                            // this.logGen.setErrorSavingTrade(SOURCE,
                            // this.fileName, String.valueOf(i + 1),
                            // this.repoBean.getBoReference(), line);
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);
                        }

                    } else {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
                        this.bResult = false;
                        this.logGen.incrementRecordErrors();
                        this.logGen.incrementTotal();
                        this.logGen.setErrorBadRecordFormat(SOURCE, this.fileName, String.valueOf(i + 1), "", line,
                                this.repoBean.getBoReference());
                        this.logGen.feedFullLog(0);
                        this.logGen.feedDetailedLog(0);
                    }
                } else {
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

        if (this.repoBean == null || Util.isEmpty(this.repoBean.getMtm()) || Util.isEmpty(this.repoBean.getMaturityDate()))
            return;

        String mtm = this.repoBean.getMtm();
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

            mtmDate = JDate.valueOf(dateFormat.parse(this.repoBean.getMaturityDate()));

        } catch (ParseException e) {
            Log.error(this, "Repo BO Ref: " + this.repoBean.getBoReference() + "Cannot convert to mtm date field " + this.repoBean);
        }

        if (mtmDate != null && trade.getProduct() instanceof Repo) {

            Repo repo = (Repo) trade.getProduct();
            if (repo.getMaturityDate().before(mtmDate)) {

                if (KEYWORD_CM_AFTER_MATURITY != null) {
                    final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
                    final String KeywordComment = KEYWORD_CM_AFTER_MATURITY.get(keywordValue);
                    trade.addKeyword(keywordValue, KeywordComment);
                }
            }
        }
    }

    /**
     * Method for save a trade
     *
     * @param trade
     * @param repoBean2 Array of string.
     */

    private long saveTrade(Trade trade, final RepoBean repoBean2, final String stringLine) throws RemoteException {
        // final int tradeSaved = 0;
        final ArrayList<String> errorMsgs = new ArrayList<String>();
        boolean correctPLMark = true;

        if (null != trade) {
            try {
                if (!Util.isEmpty(repoBean2.getMtm())) {
                    CollateralUtilities.handleUnSettledTrade(trade, Double.parseDouble(repoBean2.getMtm()),
                            getValuationDatetime().getJDate(TimeZone.getDefault()));
                }

            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "34", "MTM", repoBean2.getBoReference(),
                        stringLine);

            }
            // Added the condition for the CANCEL action, to save the Trade and
            // cancel it. José David Sevillano.
            // if ((trade.getLongId() == 0) ||
            // UNMATURE.equals(trade.getAction().toString())
            // || CANCEL.equals(trade.getAction().toString()) ||
            // MATURE.equals(trade.getAction().toString())
            // || AMEND.equals(trade.getAction().toString())) {
            if (CANCEL.equals(trade.getAction().toString())) {
                if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                    trade = this.remoteColService.saveTrade(trade, errorMsgs);
                    return trade.getLongId();
                }
            }

            // if (trade.getLongId() == 0) {
            try {
                // createPLMarks(trade, this.pricingEnv.getName(),
                // getValuationDatetime().getJDate(TimeZone.getDefault()), repoBean2,
                // stringLine, errorMsgs);
                if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                    //GSM 10/05/2016 - V14 Mig. Trades with action mature only can be save after plmaks - save too in
                    // method remoteColService.saveTradeWithPLMarks(trade, plMark, errorMsgs), lin. 890
                    if (!repoBean2.getAction().equals(MATURITY))
                        trade = this.remoteColService.saveTrade(trade, errorMsgs);
                    if (trade.getLongId() != 0 && !trade.getAction().equals(Action.valueOf(MATURE))) {
                        trade.setAction(Action.valueOf(AMEND));
                    }
                } else {
                    correctPLMark = false;
                }
            } catch (Exception e) {
                correctPLMark = false;
                this.bResult = false;
            }
            // }

            // If the Action is CANCEL, we don't save the Trade. José David
            // Sevillano.
            if (!CANCEL.equals(trade.getAction().toString()) && correctPLMark) {
                // @TODO We might need to save the trade again to move it to
                // correct status -Soma
                try {
                    final PLMark plMark = createPLMarks(trade, this.pricingEnv.getName(),
                            getValuationDatetime().getJDate(TimeZone.getDefault()), repoBean2, stringLine, errorMsgs);
                    if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
                        // if (!NEW.equals(trade.getAction().toString())) {
                        // trade = this.remoteColService.saveTrade(trade,
                        // errorMsgs);
                        // }
                        trade = this.remoteColService.saveTradeWithPLMarks(trade, plMark, errorMsgs);
                    }

                } catch (final Exception e) {
                    if (!e.toString().contains("MATURED")) {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error creating PL Mark", e);
                        this.bResult = false;
                        this.logGen.incrementError();
                        this.logGen.setErrorCreatingPlMark(SOURCE, this.fileName, stringLine,
                                repoBean2.getBoReference());
                    }
                }

            }

            return trade.getLongId();
        }
        return 0;
    }

    private Trade getTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade trade = null;

        if (!repoBean2.getBoReference().equals("")) {
            if (repoBean2.getAction().equals(NEW)) {
                trade = newTrade(repoBean2, line, stringLine);
            } else if (repoBean2.getAction().equals(CANCEL)) {
                trade = cancelTrade(repoBean2, line, stringLine);
            } else if (repoBean2.getAction().equals(AMEND)) {
                trade = amendTrade(repoBean2, line, stringLine);
            } else if (repoBean2.getAction().equals(MATURITY)) {
                trade = matureTrade(repoBean2, line, stringLine);
            } else if (repoBean2.getAction().equals(ROLLOVER)) {
                trade = rolloverTrade(repoBean2, line, stringLine);
            } else if (repoBean2.getAction().equals(MTM)) {
                trade = mtmTrade(repoBean2, line, stringLine);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "6", "ACTION",
                        repoBean2.getBoReference(), stringLine);

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

    private Trade mtmTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        Repo product = new Repo();
        result.setProduct(product);

        try {
            if (!repoBean2.getBoReference().equals("")) {

                existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                        this.repoBean.getBoReference());

                for (int i = 0; i < existBoReference.size(); i++) {
                    result = existBoReference.get(i);
                    if (!repoBean2.getBoSystem().equals("")) {
                        if (result.getKeywordValue(BO_SYSTEM).equals(repoBean2.getBoSystem()) && result.getProduct() instanceof Repo) {
                            if (!result.getAction().equals(Action.MATURE)) {
                                product = (Repo) result.getProduct();
                                result.setAction(Action.valueOf(AMEND));
                                result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                                return result;
                            } else {
                                product = (Repo) result.getProduct();
                                result.setAction(Action.valueOf(UNMATURE));
                                result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                                return result;
                            }
                        }
                    }
                }

            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

            result = new Trade();
            result.setProduct(product);
            result.setAction(Action.valueOf(NEW));
            result = introduceRepo(result, product, true, repoBean2, line, stringLine);
            return result;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
        }
        return null;
    }

    private Trade rolloverTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        Repo product = new Repo();
        result.setProduct(product);

        try {
            existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                    this.repoBean.getBoReference());

            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(repoBean2.getBoSystem()) && result.getProduct() instanceof Repo) {
                    product = (Repo) result.getProduct();
                    result.setAction(Action.valueOf(repoBean2.getAction()));
                    result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                    return result;
                }
            }

            if (repoBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            // Record ROLLOVER received but transaction <XX> not present. Error
            // 13
            this.logGen.incrementError();
            this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                    repoBean2.getBoReference(), repoBean2.getBoSystem(), stringLine, "ROLLOVER", "13");

            this.bResult = false;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
        }
        return null;
    }

    private Trade matureTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        Repo product = new Repo();
        result.setProduct(product);

        try {
            existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                    this.repoBean.getBoReference());

            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getProduct() instanceof Repo) {
                    product = (Repo) result.getProduct();
                    if (result.getKeywordValue(BO_SYSTEM).equals(repoBean2.getBoSystem())) {
                        if (!result.getAction().equals(Action.MATURE)) {
                            if (Double.parseDouble(repoBean2.getMtm()) != 0.0) {
                                result.setAction(Action.valueOf(AMEND));
                                return result;
                            } else {
                                result.setAction(Action.valueOf(MATURE));
                                result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                                return result;
                            }
                        } else {
                            return result;
                        }
                    }
                }
            }

            if (repoBean2.getBoReference().equals("")) {

                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

            // Record MATURITY received but transaction <XX> not present. Error
            // 12
            if (Double.parseDouble(repoBean2.getMtm()) != 0) {
                result = new Trade();
                result.setProduct(product);
                result.setAction(Action.valueOf(NEW));
                result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                return result;
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                        repoBean2.getBoReference(), repoBean2.getBoSystem(), stringLine, "MATURITY", "12");

                this.bResult = false;
            }
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
        }
        return null;
    }

    private Trade amendTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;
        Repo product = new Repo();
        result.setProduct(product);

        try {
            existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                    this.repoBean.getBoReference());

            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(repoBean2.getBoSystem()) && result.getProduct() instanceof Repo) {
                    product = (Repo) result.getProduct();
                    if (!result.getAction().equals(Action.MATURE)) {
                        if (!repoBean2.getMaturityDate().equals(repoBean2.getDate())) {
                            product = (Repo) result.getProduct();
                            result.setAction(Action.AMEND);
                            result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                            return result;
                        } else {
                            result.setAction(Action.MATURE);
                            result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                            return result;
                        }
                    } else {
                        if (!repoBean2.getMaturityDate().equals(repoBean2.getDate())) {
                            product = (Repo) result.getProduct();
                            result.setAction(Action.valueOf(UNMATURE));
                            result = introduceRepo(result, product, true, repoBean2, line, stringLine);
                            return result;
                        } else {
                            return result;
                        }
                    }
                }
            }

            if (repoBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }
            result = new Trade();
            result.setProduct(product);
            result.setAction(Action.valueOf(NEW));
            result = introduceRepo(result, product, true, repoBean2, line, stringLine);
            return result;

        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
        }
        return null;
    }

    private Trade cancelTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        TradeArray existBoReference = null;

        try {
            existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                    this.repoBean.getBoReference());

            for (int i = 0; i < existBoReference.size(); i++) {
                result = existBoReference.get(i);
                if (result.getKeywordValue(BO_SYSTEM).equals(repoBean2.getBoSystem()) && result.getProduct() instanceof Repo) {
                    result.setAction(Action.valueOf(repoBean2.getAction()));
                    return result;
                }
            }
            if (repoBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

            // Record CANCEL received but transaction <XX> not present. Error 10
            this.logGen.incrementError();
            this.logGen.setErrorTransactionNotExist(SOURCE, this.fileName, String.valueOf(line),
                    repoBean2.getBoReference(), repoBean2.getBoSystem(), stringLine, "CANCEL", "10");

            this.bResult = false;
        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString());
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Trade newTrade(final RepoBean repoBean2, final int line, final String stringLine) {
        Trade result = new Trade();
        result.setAction(Action.valueOf(repoBean2.getAction()));
        final Repo product = new Repo();
        result.setProduct(product);

        TradeArray existBoReference = null;
        List existBoSystem = null;
        boolean isNew = true;

        try {
            existBoReference = TradeInterfaceUtils.getTradeByBORefAndBOSystem(this.repoBean.getBoSystem(),
                    this.repoBean.getBoReference());

            existBoSystem = this.remoteTrade.getKeywordDomain(BO_SYSTEM);

            if ((existBoReference.size() > 0) && existBoSystem.contains(repoBean2.getBoSystem()) && existBoReference.get(0).getProduct() instanceof Repo) {
                isNew = false;
            }

            if (repoBean2.getBoReference().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "8",
                        "BO_REFERENCE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
            if (repoBean2.getBoSystem().equals("")) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7",
                        "BO_SYSTEM", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }

            if ((!repoBean2.getBoSystem().equals("")) & (!repoBean2.getBoReference().equals(""))) {
                if (isNew) {
                    result = introduceRepo(result, product, isNew, repoBean2, line, stringLine);
                    return result;
                } else {
                    result = amendTrade(repoBean2, line, stringLine);
                    return result;
                }
            }

        } catch (final Exception e1) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e1.toString());
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Trade introduceRepo(final Trade result, final Repo product, final boolean isNew, final RepoBean repoBean2,
                                final int line, final String stringLine) {
        if (result.getKeywords() == null) {
            result.setKeywords(new Hashtable<String, String>());
        }
        if (!repoBean2.getFoSystem().equals("")) {
            result.addKeyword("FO_SYSTEM", repoBean2.getFoSystem());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "52",
                    "FO_SYSTEM", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        result.setTradePrice(1);

        try {
            if (isNew) {
                if (!repoBean2.getNumFrontId().equals("")) {
                    Integer.parseInt(repoBean2.getNumFrontId());
                    result.setExternalReference(repoBean2.getNumFrontId());
                    result.addKeyword("NUM_FRONT_ID", repoBean2.getNumFrontId());
                    if (KEYWORD_CM_AFTER_MATURITY != null) {
                        final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
                        result.addKeyword(keywordValue, "false");
                    }
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "51", "NUM_FRONT_ID", repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
            }
        } catch (Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "51",
                    "NUM_FRONT_ID", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        if (!repoBean2.getProcessingOrg().equals("")) {
            try {

                if (GenericValidations.validateLegalEntity(repoBean2.getProcessingOrg())) {
                    @SuppressWarnings("unused") final LegalEntity po = this.remoteReferenceData.getLegalEntity(repoBean2.getProcessingOrg());
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "14", "PROCESSING_ORG", repoBean2.getBoReference(), stringLine);

                    this.bResult = false;
                }

            } catch (final RemoteException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "14",
                        "PROCESSING_ORG", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "14",
                    "PROCESSING_ORG", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        try {

            if (GenericValidations.validateLegalEntity(repoBean2.getCounterparty())) {
                result.setCounterParty(this.remoteReferenceData.getLegalEntity(repoBean2.getCounterparty()));
            } else {
                result.addKeyword(ORIG_SOURCE_CPTY, repoBean2.getCounterparty());
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "15",
                        "COUNTERPARTY", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

        } catch (final RemoteException e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "15",
                    "COUNTERPARTY", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        if (!repoBean2.getInstrument().equals("")) {
            result.addKeyword("INSTRUMENT", repoBean2.getInstrument());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "16",
                    "INSTRUMENT", repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        try {

            // GSM: 26/06/2013. Short/long Portfolio Development. Returns first
            // short name, if not tries long
            final String shortAlias = ALIAS_BOOK_SYSTEM + getAttribute(ALIAS_BOOK_SYSTEM_ST);
            final String longAlias = shortAlias + "_LONG";
            if (GenericValidations.validatePortfolioMapped(repoBean2.getPortfolio(), longAlias, shortAlias)) {
                // GSM: deprecated
                // if
                // (GenericValidations.validatePortfolioMapped(repoBean2.getPortfolio(),
                // ALIAS_BOOK_SYSTEM + repoBean2.getBoSystem())) {
                // final String bookMapped =
                // CollateralUtilities.getBookMapped(repoBean2.getPortfolio(),
                // ALIAS_BOOK_SYSTEM
                // + repoBean2.getBoSystem());
                final String bookMapped = CollateralUtilities.getBookMappedName(repoBean2.getPortfolio(),
                        ALIAS_BOOK_SYSTEM + getAttribute(ALIAS_BOOK_SYSTEM_ST));

                // We get the BOOK from the system.
                final Book book = this.conn.getRemoteReferenceData().getBook(bookMapped);
                repoBean2.setPortfolio(bookMapped);
                result.setBook(book);
                final LegalEntity po = this.remoteReferenceData.getLegalEntity(repoBean2.getProcessingOrg());
                if (this.remoteReferenceData.getBook(repoBean2.getPortfolio()).getProcessingOrgBasedId() != po
                        .getId()) {
                    // BAU 5.2.0 - return warning instead of error in order to
                    // add this line in bad file
                    this.errors.put(line + ":PO is different from book LE:",
                            new Exception("PO is different from book LE."));
                    this.logGen.incrementWarning();
                    this.logGen.setWarningDifferentLegalEntities(SOURCE, this.fileName, String.valueOf(line), "73",
                            "PROCESSING_ORG", repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
            } else {
                this.errors.put(line + ":Error while looking for book:" + repoBean2.getPortfolio(),
                        new Exception("Book is not in the system."));
                result.addKeyword(ORIG_SOURCE_BOOK, repoBean2.getPortfolio());
                this.bResult = false;
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "17",
                        "PORTFOLIO", repoBean2.getPortfolio(), repoBean2.getBoReference(), stringLine);
            }
        } catch (final RemoteException e) {
            this.errors.put(line + ":Error while looking for book:" + repoBean2.getPortfolio(),
                    new Exception("Book is not in the system."));
            result.addKeyword(ORIG_SOURCE_BOOK, repoBean2.getPortfolio());
            this.bResult = false;
            this.logGen.incrementWarning();
            this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "17",
                    "PORTFOLIO", repoBean2.getPortfolio(), repoBean2.getBoReference(), stringLine);
        }

        addDates(repoBean2, line, result, stringLine);
        addCollaterals(repoBean2, line, product, result, stringLine);
        // Controlar el collateral
        if ((null != product.getCollaterals()) && (product.getCollaterals().size() > 0)) {
            if (!repoBean2.getClosingPriceStart().equals("")) {
                final Vector collaterals = product.getCollaterals();
                final Collateral collateral = (Collateral) collaterals.get(0);
                if (collateral != null) {
                    collateral.setInitialPrice(CollateralBasedUtil.convertToCleanPrice(
                            Double.parseDouble(repoBean2.getClosingPriceStart()), collateral) / 100);
                }
            }
        }

        try {
            if (!repoBean2.getRepoAmount().isEmpty()) {
                double repoAmount = 0;
                // Cash cash = product.getCash();
                // cash.setPrincipal(Double.parseDouble(repoBean2.getRepoAmount()));
                if ("BUY".equals(repoBean2.getDirection()) && (Double.parseDouble(repoBean2.getRepoAmount()) > 0)) {
                    // this.logGen.incrementError();
                    // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
                    // this.fileName, String.valueOf(line),
                    // "28", "REPO_AMOUNT", repoBean2.getBoReference(),
                    // stringLine);
                    //
                    // this.bResult = false;
                    repoAmount = Double.parseDouble(repoBean2.getRepoAmount()) * (-1);
                    product.getCash().setPrincipal(repoAmount);
                } else if ("SELL".equals(repoBean2.getDirection())
                        && (Double.parseDouble(repoBean2.getRepoAmount()) < 0)) {
                    // this.logGen.incrementError();
                    // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
                    // this.fileName, String.valueOf(line),
                    // "28", "REPO_AMOUNT", repoBean2.getBoReference(),
                    // stringLine);
                    //
                    // this.bResult = false;
                    repoAmount = Double.parseDouble(repoBean2.getRepoAmount()) * (-1);
                    product.getCash().setPrincipal(repoAmount);
                } else {
                    repoAmount = Double.parseDouble(repoBean2.getRepoAmount());
                    product.getCash().setPrincipal(repoAmount);
                }

                final Vector collaterals = product.getCollaterals();

                if ((collaterals != null) && (collaterals.size() > 0)) {
                    final Collateral collateral = (Collateral) collaterals.get(0);
                    if (collateral != null) {
                        collateral.setPrincipal(product.getCash().getPrincipal());
                    }
                }
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "28",
                        "REPO_AMOUNT", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        } catch (final Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "28",
                    "REPO_AMOUNT", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        try {
            if (!repoBean2.getRepoRate().equals("")) {
                product.getCash().setFixedRateB(true);
                product.getCash().setFixedRate(Double.parseDouble(repoBean2.getRepoRate()) / 100);
                final Vector collaterals = product.getCollaterals();
                if ((collaterals != null) && (collaterals.size() > 0)) {
                    final Collateral collateral = (Collateral) collaterals.get(0);
                    if (collateral != null) {
                        collateral.setInitialFXRate(1);
                    }
                }
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "29",
                        "REPO_RATE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        } catch (final Exception e) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "29",
                    "REPO_RATE", repoBean2.getBoReference(), stringLine);
        }

        if (!repoBean2.getRepoCcy().equals("") && this.currencies.contains(repoBean2.getRepoCcy())) {
            product.getCash().setCurrency(repoBean2.getRepoCcy());
            result.setTradeCurrency(repoBean2.getRepoCcy());
            result.setSettleCurrency(repoBean2.getRepoCcy());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "30",
                    "CCY", repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        //GSM 27/07/2016 - Allow Cross Currency Repos - nominal ccy != repo ccy
        if (!Util.isTrue(getAttribute(ALLOW_CC_REPO), false)) {

            if (!repoBean2.getNominalSecCcy().equals(repoBean2.getRepoCcy())
                    || !this.currencies.contains(repoBean2.getNominalSecCcy())) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "25",
                        "NOMINAL_CCY", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        }

        // The information not defined in Calypso objects, we need to insert by
        // KEYWORDS. We use the same names for the configured KEYWORDS.
        if (!repoBean2.getClosingPriceDaily().equals("")) {
            try {
                Double.parseDouble(repoBean2.getClosingPriceDaily());
                result.getKeywords().put("CLOSING_PRICE_DAILY", repoBean2.getClosingPriceDaily());
            } catch (final Exception e) {
                // No hago nada, porque ya está tratado este error en otro
                // punto del código.
            }
        }

        if (!repoBean2.getRepoCashVal().equals("")) {
            try {
                Double.parseDouble(repoBean2.getRepoCashVal());
                result.getKeywords().put("REPO_CASH_VAL", repoBean2.getRepoCashVal());
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "33",
                        "REPO_CASH_VAL", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "33",
                    "REPO_CASH_VAL", repoBean2.getBoReference(), stringLine);
            this.bResult = false;
        }

        if (!repoBean2.getCustodian().equals("")) {
            result.getKeywords().put("CUSTODIAN", repoBean2.getCustodian());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "36",
                    "CUSTODIAN", repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        if (repoBean2.getAccount().equals("")) {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "37",
                    "ACCOUNT", repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        } else {

            result.getKeywords().put("ACCOUNT", repoBean2.getAccount());

        }

        // KEYWORD(BO_SYSTEM)[27]|KEYWORD(BO_REFERENCE)[28]|type[29]|basis[30]|frequency[31]|KEYWORD(STRUCTURE_ID)[32]|fee.independentAmmount[33]|fee.independentAmmountcurrency[34]
        // From here on the values are optional
        if (!repoBean2.getBoSystem().equals("")) {
            result.getKeywords().put(BO_SYSTEM, repoBean2.getBoSystem());
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "7", "BO_SYSTEM",
                    repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        // La comprobación de si está vacío para informarlo en el log ya se
        // ha hecho antes.
        if (!repoBean2.getBoReference().equals("")) {
            result.getKeywords().put("BO_REFERENCE", repoBean2.getBoReference());
        }

        if (repoBean2.getRepoType() != null) {
            product.setSubType(repoBean2.getRepoType());
        }
        if (repoBean2.getCalcBasis() != null) {
            product.getCash().setFixedDayCount(DayCount.valueOf(repoBean2.getCalcBasis().toUpperCase()));
        }
        if (!Util.isEmpty(repoBean2.getRepoFreq())) {
            product.getCash().setPaymentFrequency(Frequency.valueOf(repoBean2.getRepoFreq()));
        } else {
            product.getCash().setPaymentFrequency(Frequency.valueOf("ZC"));
        }

        if (repoBean2.getStructureId() != null) {
            result.getKeywords().put("STRUCTURE_ID", repoBean2.getStructureId());
        }

        if (!Util.isEmpty(repoBean2.getIndependentAmount()) && !Util.isEmpty(repoBean2.getIndependentAmountCcy())) {
            final Fee fee = new Fee();
            fee.setType("INDEPENDENT_AMOUNT");
            fee.setAmount(Double.parseDouble(repoBean2.getIndependentAmount()));
            fee.setCurrency(repoBean2.getIndependentAmountCcy());
            fee.setDate(result.getTradeDate().getJDate(TimeZone.getDefault()));
            if (result.getFees() != null) {
                result.getFees().add(fee);
            }
        }

        // new fields (for the moment save them on keywords)
        if (fileNumberOfFields > ORIGINAL_NUMBER_OF_FIELDS) {

            // // REPO_ACCRUED_INTEREST
            // try {
            // String repoAccruedInterest = repoBean2.getRepoAccruedInterest();
            // Double.parseDouble(repoAccruedInterest);
            // result.getKeywords().put("REPO_ACCRUED_INTEREST",
            // repoAccruedInterest);
            // } catch (NumberFormatException e) {
            // this.logGen.incrementError();
            // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
            // this.fileName, String.valueOf(line), "67",
            // "REPO_ACCRUED_INTEREST", repoBean2.getBoReference(), stringLine);
            // this.bResult = false;
            // }
            // HAIRCUT_FORMULA
            String haircutFormula = repoBean2.getHaircutFormula();
            if (isValidHaircutFormula(haircutFormula)) {
                result.getKeywords().put("HAIRCUT_FORMULA", haircutFormula);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "71",
                        "HAIRCUT_FORMULA", repoBean2.getBoReference(), stringLine);
                this.bResult = false;
            }
            // // CLEAN_PRICE
            // try {
            // String cleanPrice = repoBean2.getCleanPrice();
            // Double.parseDouble(cleanPrice);
            // result.getKeywords().put("CLEAN_PRICE", cleanPrice);
            // } catch (NumberFormatException e) {
            // this.logGen.incrementError();
            // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
            // this.fileName, String.valueOf(line), "69",
            // "CLEAN_PRICE", repoBean2.getBoReference(), stringLine);
            // this.bResult = false;
            // }
            // BOND_ACCRUED_INTEREST (not mandatory)
            // if (!Util.isEmpty(repoBean2.getBondAccruedInterest())) {
            // try {
            // String bondAccruedInterest = repoBean2.getBondAccruedInterest();
            // Double.parseDouble(bondAccruedInterest);
            // result.getKeywords().put("BOND_ACCRUED_INTEREST",
            // bondAccruedInterest);
            // } catch (NumberFormatException e) {
            // this.logGen.incrementError();
            // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
            // this.fileName, String.valueOf(line),
            // "68", "BOND_ACCRUED_INTEREST", repoBean2.getBoReference(),
            // stringLine);
            // this.bResult = false;
            // }
            // }
            // RIG-CODE
            result.getKeywords().put("RIG_CODE", repoBean2.getRigCode());
            // CAPITAL_FACTOR
            // try {
            // String capitalFactor = repoBean2.getCapitalFactor();
            // Double.parseDouble(capitalFactor);
            // result.getKeywords().put("CAPITAL_FACTOR", capitalFactor);
            // } catch (NumberFormatException e) {
            // this.logGen.incrementError();
            // this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE,
            // this.fileName, String.valueOf(line), "70",
            // "CAPITAL_FACTOR", repoBean2.getBoReference(), stringLine);
            // this.bResult = false;
            // }

        }

        return result;
    }

    /**
     * Method for add security to the product
     *
     * @param repoBean2 values in the file to import
     * @param line      number of line reading actually for the file
     * @param product   repo for collateral
     */
    private void addCollaterals(final RepoBean repoBean2, final int line, final Repo product, final Trade result,
                                final String stringLine) {
        final HashSet<String> isinSet = new HashSet<String>();
        isinSet.add(repoBean2.getIsin());
        Vector<Bond> bond = null;

        if (repoBean2.getDirection().equals("BUY") || repoBean2.getDirection().equals("SELL")) {
            if (repoBean2.getDirection().equals("BUY")) {
                result.setQuantity(-1);
            } else if (repoBean2.getDirection().equals("SELL")) {
                // it´s a sell of secutity ---> Repo
                result.setQuantity(1);
            }
        } else {
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "21",
                    "DIRECTION", repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }

        try {
            bond = getBonds(isinSet, repoBean2.getNominalSecCcy());

            if (bond.size() != 1) {
                product.setCollaterals(new Vector<Collateral>());
                result.addKeyword(ORIG_SOURCE_ISIN, repoBean2.getIsin());
                // Required field ISIN <XX> not present or not valid. Warning 22
                this.errors.put(line + ":Error while looking for bond:" + repoBean2.getIsin(),
                        new Exception("bond not found or Bond is not enabled"));
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "22",
                        "ISIN", repoBean2.getIsin(), repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            } else {
                final Vector<Collateral> collaterals = new Vector<Collateral>();
                final Collateral collateral = new Collateral();
                collaterals.add(collateral);

                // revisar si se debe insertar en este campo!!
                if (!repoBean2.getClosingPriceStart().equals("")) {
                    try {
                        collateral.setCollateralPrice(Double.parseDouble(repoBean2.getClosingPriceStart()));
                    } catch (final Exception e) {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "23", "CLOSING_PRICE_AT_START", repoBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "23", "CLOSING_PRICE_AT_START", repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
                collateral.setSecurity(bond.get(0));
                try {
                    if (!repoBean2.getNominalSec().equals("")) {
                        Double qty = Math.abs(Double.parseDouble(repoBean2.getNominalSec()))
                                / bond.get(0).getFaceValue();
                        if (repoBean2.getDirection().equals("SELL")) {
                            // it is a sell of secutity ---> Repo
                            qty *= -1;
                        }
                        // Repo repo = (Repo) result.getProduct();
                        // if (repo.getType().equals("Repo")) {
                        // qty = -qty;
                        // }
                        collateral.setQuantity(qty);
                    } else {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "24", "NOMINAL", repoBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                } catch (final Exception e) {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "24", "NOMINAL", repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }

                try {
                    if (!repoBean2.getHaircut().equals("")) {
                        collateral.setHaircut(
                                "".equals(repoBean2.getHaircut()) ? 0 : Double.parseDouble(repoBean2.getHaircut()));
                    } else {
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                                "26", "HAIRCUT", repoBean2.getBoReference(), stringLine);

                        this.bResult = false;
                    }
                } catch (final Exception e) {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "26", "HAIRCUT", repoBean2.getBoReference(), stringLine);
                    this.bResult = false;
                }
                if (!repoBean2.getHaircutDirection().equals("")) {
                    collateral.setHaircutSign(repoBean2.getHaircutDirection().equals(GIVE_DIRECTION) ? -1 : 1);
                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line),
                            "27", "HAIRCUT_DIRECTION", repoBean2.getBoReference(), stringLine);

                    this.bResult = false;
                }

                /*
                 * Estas dos fechas que hacemos con ellas? No las controlamos??
                 * Al ser las mismas que antes, ya están controladas
                 */
                collateral.setStartDate(JDate.valueOf(repoBean2.getValueDate()));
                collateral.setTradeDate(JDate.valueOf(repoBean2.getTradeDate()));

                collateral.setHaircutType(REGULAR);

                product.setCollaterals(collaterals);

                // Set the information about the Coupon Holidays and Coupon Day
                // Count.
                final Bond secBond = bond.get(0);
                product.getCash().setPaymentHolidays(secBond.getHolidays());
                product.getCash().setPmtOffset(0);
            }
        } catch (final RemoteException e) {
            this.errors.put(line + ":Error while looking for bond:" + repoBean2.getIsin(),
                    new Exception("bond not found or Bond is not enabled"));
            this.logGen.incrementWarning();
            this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "22",
                    "ISIN", repoBean2.getIsin(), repoBean2.getBoReference(), stringLine);

            this.bResult = false;
        }
    }

    /**
     * Method for insert the date into the file to the trade
     *
     * @param repoBean2 values in the file to import
     * @param line      number of line reading actually for the file
     * @param result    Object trade where we set the dates
     */
    @SuppressWarnings({"unused"})
    private void addDates(final RepoBean repoBean2, final int line, final Trade result, final String stringLine) {
        if (result.getProduct() instanceof Repo) {
            try {
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    final Date parse = dateFormat.parse(repoBean2.getTradeDate());
                    final JDatetime jdt = new JDatetime(dateFormat.parse(repoBean2.getTradeDate()));
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(jdt.getTime());
                    cal.set(Calendar.AM_PM, Calendar.AM);
                    cal.set(Calendar.HOUR, 2);
                    result.setTradeDate(new JDatetime(cal.getTime()));

                }
            } catch (final ParseException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "18",
                        "TRADE_DATE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            try {
                Cash cash = ((Repo) result.getProduct()).getCash();
                if (cash == null) {
                    cash = new Cash();
                    ((Repo) result.getProduct()).setCash(cash);
                }
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    final Date parse = dateFormat.parse(repoBean2.getValueDate());
                    final JDatetime jdt = new JDatetime(dateFormat.parse(repoBean2.getValueDate()));
                    final JDate jd = jdt.getJDate(TimeZone.getDefault());
                    cash.setStartDate(jd);
                    result.setSettleDate(jd);
                }
            } catch (final ParseException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.fileName, String.valueOf(line), "19",
                        "VALUE_DATE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }

            try {
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    final Date parse = dateFormat.parse(repoBean2.getMaturityDate());
                    final JDate jd = new JDatetime(dateFormat.parse(repoBean2.getMaturityDate())).getJDate(TimeZone.getDefault());
                    ((Repo) result.getProduct()).setMaturityDate(jd);
                }
            } catch (final ParseException e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(SOURCE, this.fileName, String.valueOf(line), "20",
                        "MATURITY_DATE", repoBean2.getBoReference(), stringLine);

                this.bResult = false;
            }
        }

    }

    @SuppressWarnings("unchecked")
    private Vector<Bond> getBonds(final Set<String> isinSet, String curr) throws RemoteException {

        Vector<Bond> products = new Vector<Bond>();
        final Vector<Bond> productsEnabled = new Vector<Bond>();
        String isin;
        final Iterator<String> keySetIterator = isinSet.iterator();
        while (keySetIterator.hasNext()) {
            for (int i = 0; (i < 1000) && keySetIterator.hasNext(); i++) {
                isin = keySetIterator.next();
                products = DSConnection.getDefault().getRemoteProduct().getProductsByCode("ISIN", isin);
                if (!Util.isEmpty(products)) {
                    for (int j = 0; j < products.size(); j++) {
                        final Product bond = products.get(j);
                        if ((bond instanceof Bond) && bond.getCurrency().equals(curr)) {
                            productsEnabled.add((Bond) bond);
                            break;
                        }
                    }
                }

            }
        }
        return productsEnabled;
    }

    // *** Read file management *** //

    // Get values from text line and save them in string array
    private String[] getValuesFromLine(String line) {

        String aux = line.substring(0, line.lastIndexOf("|"));
        return aux.split("\\|", -1);

    }

    // Check fields number is above minimum limit
    private boolean checkNumberOfFields(String[] values) {

        return (values.length >= ORIGINAL_NUMBER_OF_FIELDS);

    }

    // Map original array to size-formated array
    private String[] mapValues(String[] values) {

        int totalFields = NEW_NUMBER_OF_FIELDS;

        String[] totalValues = new String[totalFields];

        // put blank
        for (int i = 0; i < totalFields; i++) {
            totalValues[i] = "";
        }

        final int limit = (values.length <= totalFields) ? values.length : totalFields;
        // copy read values
        for (int j = 0; j < limit; j++) {
            totalValues[j] = values[j].trim();
        }

        return totalValues;
    }

    // Check haircut formula format is correct
    private boolean isValidHaircutFormula(String haircutFormula) {
        if (!Util.isEmpty(haircutFormula)) {
            // check format
            for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
                if (haircutFormula.contains(FORBIDDEN_CHARS.subSequence(i, i + 1))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String getFileName() {
        return this.file;
    }
}

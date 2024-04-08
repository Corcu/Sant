package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.refdata.CustomLeveragePercentage;
import calypsox.tk.refdata.service.RemoteCustomLeverageService;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static calypsox.tk.util.SantEquitySwapUtil.ISIN;

/**
 * ScheduledTaskUPDATE_EQUITY_SWAP_LEVERAGE import equity swap leverage from CSV
 *
 * @author Ruben Garcia
 */
public class ScheduledTaskUPDATE_EQUITY_SWAP_LEVERAGE extends AbstractProcessFeedScheduledTask {

    /**
     * The serial UID
     */
    private static final long serialVersionUID = -2233296019342900635L;

    /**
     * Number of lines by executor. Default 100.
     */
    public static final String BATCH_SIZE = "Batch Size";

    /**
     * CSV separator, by default ";"
     */
    private static final String SEPARATOR_DOMAIN_STRING = "Separator";

    /**
     * The destination email to send summary log.
     */
    protected static final String DESTINATION_EMAIL = "Destination email";

    /**
     * Process description to log.
     */
    protected static final String PROCESS = "Load EquitySwap Leverage";

    /**
     * The static data log file path.
     */
    protected static final String STATIC_DATA_LOG = "Static Data Log";

    /**
     * The summary log file path.
     */
    protected static final String SUMMARY_LOG = "Summary Log";

    /**
     * The detailed log file path.
     */
    protected static final String DETAILED_LOG = "Detailed Log";

    /**
     * The full log file path.
     */
    protected static final String FULL_LOG = "Full Log";

    /**
     * The currency filter if there is more than one product for the same ISIN
     */
    private static final String CCY = "Currency";

    /**
     * Email subject
     */
    private static final String SUBJECT = "Log files for Exposure Trades Leverage update";

    /**
     * Default destination email with ST result
     */
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

    /**
     * True if you do not use check control line. Control line, first line start with ***** and number of lines.
     */
    private final static String DO_NOT_USE_CONTROL_LINE = "Do not use Control Line";

    /**
     * True if the file do not contains captions.
     */
    private final static String DO_NOT_USE_CAPTIONS = "Do not use Captions";

    /**
     * Source type for log purposes.
     */
    private static final String SOURCE = "EQUITY_SWAP_LEVERAGE";

    /**
     * The number of fields ISN;Leverage;LE
     */
    private static final int NUM_OF_FIELDS = 3;

    /**
     * The default ccy for duplicate ISIN filter.
     */
    private static final String DEFAULT_CCY = "BRL";

    /**
     * The file time format
     */
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    /**
     * Error if there is more than one file in the path
     */
    private static final String ERR_BAD_NUM_OF_FILES = "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.";

    /**
     * Error if the log file cannot be created
     */
    private static final String ERR_LOG_FILES = "Unexpected error creating log file";

    /**
     * Error if number of line does not match with number of lines in control record
     */
    private static final String ERR_NUM_LINES_CHECK = "Number of lines in file does not match number of lines in control record";

    /**
     * Error if file name does not contain the date part
     */
    private static final String ERR_NO_FILE_DATE = "File name does not contain the date part";

    /**
     * Error ISIN not valid
     */
    private static final String ERR_NO_PRODUCT_ISIN = "There is no product in the system with the ISIN";

    /**
     * Error Legal Entity code not valid
     */
    private static final String ERR_NO_LE_CODE = "There is no Legal Entity in the system with the code";

    /**
     * Error if number of fields is not equals to 3
     */
    private static final String ERR_WRONG_NUM_OF_FIELDS = "Wrong number of fields. Expected number of fields " + NUM_OF_FIELDS;

    /**
     * Error ISIN is empty
     */
    private static final String ERR_EMPTY_ISIN = "ISIN is empty.";

    /**
     * Error Leverage is empty
     */
    private static final String ERR_EMPTY_LEVERAGE = "Leverage is empty.";

    /**
     * Error Levrage is not number
     */
    private static final String ERR_BAD_LEVERAGE_FORMAT = "Bad leverage format.";

    /**
     * Error Legal Entity code is empty
     */
    private static final String ERR_INVALID_LE_CODE = "Legal entity code is empty.";

    /**
     * Error sending log email
     */
    private static final String ERR_SEND_MAIL = "Error. Error sending log mail.";

    /**
     * Error creating log files
     */
    private static final String ERR_MOVING_FILE = "Error. Error creating log files.";

    /**
     * Class that records the log
     */
    protected transient LogGeneric logGen = new LogGeneric();

    /**
     * Process result
     */
    private transient boolean bResult = true;

    /**
     * File name
     */
    private transient String file = "";

    /**
     * False if error
     */
    private transient boolean processOK = true;

    /**
     * False if error
     */
    private transient boolean controlMOK = true;

    /**
     * True if you use captions
     */
    private transient boolean useCaptions = true;

    @Override
    public String getTaskInformation() {
        return "Imports the % Leverage from an input CSV file. " +
                "\nThe file must contain the three informed fields ISIN;Leverage;LegalEntityCode." +
                "\nExample: EQ0000000056409328;12.5;BSTE";
    }

    @Override
    protected List<ScheduledTask.AttributeDefinition> buildAttributeDefinition() {
        List<ScheduledTask.AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());

        attributeList.addAll(Arrays.asList(
                        attribute(SEPARATOR_DOMAIN_STRING).description("File field separator.Default ;."),
                        attribute(BATCH_SIZE).description("Number of lines by executor. Default 20.").integer(),
                        attribute(THREAD_COUNT).description("Number of threads.").integer(),
                        attribute(SUMMARY_LOG).description("Summary log path."),
                        attribute(DETAILED_LOG).description("Detailed log path."),
                        attribute(FULL_LOG).description("Full log path."),
                        attribute(STATIC_DATA_LOG).description("Static data log path."),
                        attribute(DESTINATION_EMAIL).description("Destination emails separated by ;."),
                        attribute(DO_NOT_USE_CONTROL_LINE).description("File to checking the control line, " +
                                "identifying if the information is correct.").booleanType(),
                        attribute(DO_NOT_USE_CAPTIONS).description("True to skip first line field names.")
                                .booleanType(),
                        attribute(CCY).mandatory().domainName("currency").description("Currency to filter if " +
                                "there is more than one product per ISIN.")
                )
        );

        return attributeList;
    }


    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        if (Log.isCategoryLogged(LOG_CATEGORY)) {
            Log.debug(LOG_CATEGORY, "Calling Execute ON " + this + " PublishB: " + getPublishB());
        }

        boolean ret = true;

        if (getExecuteB()) {
            try {
                ret = doProcess();
            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                ret = false;
            }
        }

        if (getPublishB()) {
            try {
                PSEventScheduledTask ev = new PSEventScheduledTask();
                ev.setScheduledTask(this);
                ps.publish(ev);

            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                ret = false;
            }
        }

        if (getSendEmailB() && ret) {
            sendMail(ds, ps);
        }

        return ret;
    }

    /**
     * Process the CSV input file. Initialize log file. Check file properties.
     *
     * @return true if process OK
     */
    public boolean doProcess() {
        final String path = getAttribute(FILEPATH);
        String startFileName = getAttribute(STARTFILENAME);

        String time = timeFormat.format(new Date());
        logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
                time);

        boolean useControlLine = !getBooleanAttribute(DO_NOT_USE_CONTROL_LINE, false);
        useCaptions = !getBooleanAttribute(DO_NOT_USE_CAPTIONS, false);


        try {

            logGen.initializeFiles(PROCESS);

            if (!logGen.validateFilesExistence()) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Log files cannot be initialized - check Log attributes");
                return false;
            }

            List<String> files = CollateralUtilities.getListFiles(path, startFileName);

            if (files.size() > 1) {
                List<String> selectedFiles = new ArrayList<>();
                JDate valDate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
                for (String name : files) {
                    JDate date = CollateralUtilities.getFileNameDate(name);
                    if (date != null && date.equals(valDate)) {
                        selectedFiles.add(name);
                    }
                }
                files = selectedFiles;
            }

            if (files.size() == 1) {
                file = files.get(0);
                logGen.initilizeStaticDataLog(file, "LINE");
                final String filePath = path + file;

                try {
                    if (!useControlLine || feedPreProcess(filePath)) {
                        final JDate jdate = CollateralUtilities.getFileNameDate(this.file);
                        if (jdate != null) {
                            // FileUtility.copyFileToDirectory(filePath, path + "/copy/");
                            readFileAndProcessPercentege(filePath);
                        } else {
                            logGen.setErrorNumberOfLines(SOURCE, file);
                            reportError(ErrorCodeEnum.InputFileNotFound, ERR_NO_FILE_DATE);
                        }
                    } else {
                        logGen.setErrorNumberOfLines(SOURCE, file);
                        reportError(ErrorCodeEnum.ControlLine, ERR_NUM_LINES_CHECK);
                    }
                } catch (Exception e) {
                    Log.error(this, e);
                    logGen.setErrorNumberOfLines(SOURCE, file);
                    reportError(ErrorCodeEnum.ScheduledTaskFailure, e.getMessage());
                }
            } else {
                logGen.setErrorNumberOfFiles(SOURCE, startFileName);
                reportError(ErrorCodeEnum.InputFileNotFound, ERR_BAD_NUM_OF_FILES);
            }

        } catch (IOException e) {
            logGen.setErrorCreatingLogFile(SOURCE, startFileName);
            reportError(ErrorCodeEnum.LogException, ERR_LOG_FILES);
        }

        try {
            // We handle the errors writing them into the log files.
            try {
                feedPostProcess(bResult);
            } catch (IOException ignore) {

            }

            logGen.closeLogFiles();

            String sumLog = logGen.feedGenericLogProcess(Util.isEmpty(file) ? startFileName : file,
                    getAttribute(SUMMARY_LOG), PROCESS, logGen.getNumberTotal() - 1);

            try {
                if (!Util.isEmpty(sumLog)) {
                    final List<String> to = new ArrayList<>();

                    String emails = getAttribute(DESTINATION_EMAIL);
                    if (!Util.isEmpty(emails)) {
                        to.addAll(Arrays.asList(emails.split(";")));
                    }
                    if (!to.isEmpty()) {
                        final ArrayList<String> attachments = new ArrayList<>();
                        attachments.add(sumLog);
                        attachments.add(logGen.getStringDetailedLog());
                        attachments.add(logGen.getStringFullLog());
                        attachments.add(logGen.getStringStaticDataLog());

                        CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                    }
                }
            } catch (final MailException e) {
                Log.error(this, e);
                logGen.setErrorSentEmail(SOURCE, startFileName);
                reportError(ErrorCodeEnum.MailSending, ERR_SEND_MAIL);
            }

        } catch (Exception e) {
            Log.error(this, e);
            logGen.setErrorMovingFile(SOURCE, startFileName);
            reportError(ErrorCodeEnum.InputFileCanNotBeMoved, ERR_MOVING_FILE);
        }

        try {
            logGen.closeLogFiles();
        } catch (IOException e) {
            Log.error(this, e);
        }

        if (this.controlMOK) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }

        return this.processOK;
    }


    /**
     * Read from CSV file the fields of leverage. ISIN;Leverage;LegalEntityCode
     *
     * @param filePath the CSV input file path
     * @throws Exception error in the process
     */
    private void readFileAndProcessPercentege(String filePath) throws Exception {

        String separator = getAttribute(SEPARATOR_DOMAIN_STRING);

        if (Util.isEmpty(separator)) {
            separator = ";";
        }


        List<Entity> batch = new ArrayList<>();
        int batchSize = getBatchSize();

        ExecutorService executorService = Executors.newFixedThreadPool(getThreadCount(), Executors.defaultThreadFactory());

        Map<String, Entity> entityMap = new HashMap<>();

        try (FileReader fr = new FileReader(filePath); BufferedReader br = new BufferedReader(fr)) {
            for (int i = 0; ; i++) {
                String line = br.readLine();

                if (line == null || line.contains("*****"))
                    break;

                if (i == 0 && useCaptions) // skip field names
                    continue;

                parseFileLine(entityMap, line, separator, i + 1);
                logGen.incrementTotal();
            }
        }

        int i = 0;
        for (String code : entityMap.keySet()) {
            batch.add(entityMap.get(code));
            i += entityMap.get(code).items.size();
            if (i >= batchSize) {
                executorService.submit(new BatchTask(batch));
                batch = new ArrayList<>();
                i = 0;
            }
        }

        if (!batch.isEmpty()) {
            executorService.submit(new BatchTask(batch));
        }

        executorService.shutdown();
        while (!executorService.awaitTermination(15L, TimeUnit.MINUTES)) {
            Log.system(LOG_CATEGORY_SCHEDULED_TASK, "Await Termination");
        }
    }

    /**
     * Parse file line ISIN;Leverage;LegalEntityCode
     *
     * @param line       the current file line
     * @param separator  field separator. Default ;
     * @param lineNumber the current line number
     */
    private void parseFileLine(Map<String, Entity> entityMap, String line, String separator, int lineNumber) {

        if (!Util.isEmpty(line) && !Util.isEmpty(separator) && line.split(separator).length != NUM_OF_FIELDS) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", "\"" + line + "\"", "");
            reportError(ErrorCodeEnum.InvalidData, ERR_WRONG_NUM_OF_FIELDS);
            return;
        }

        String[] fields = CollateralUtilities.splitMejorado(NUM_OF_FIELDS, separator, false, line);

        if (fields.length != NUM_OF_FIELDS) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", "\"" + line + "\"", "");
            reportError(ErrorCodeEnum.InvalidData, ERR_WRONG_NUM_OF_FIELDS);
            return;
        }

        String isin = fields[0];
        if (Util.isEmpty(isin)) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", "\"" + line + "\"", fields[0]);
            reportError(ErrorCodeEnum.InvalidData, ERR_EMPTY_ISIN);
            return;
        }


        double leverage;
        if (Util.isEmpty(fields[1])) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", "\"" + line + "\"", fields[1]);
            reportError(ErrorCodeEnum.InvalidData, ERR_EMPTY_LEVERAGE + fields[1]);
            return;
        } else {
            try {
                leverage = Util.stringToNumberThrowException(fields[1], Locale.US);
            } catch (Exception e) {
                logGen.setErrorBadRecordFormat(SOURCE, this.file,
                        String.valueOf(lineNumber), "", "\"" + line + "\"", fields[1]);
                reportError(ErrorCodeEnum.InvalidData, ERR_BAD_LEVERAGE_FORMAT + fields[1]);
                return;
            }
        }

        String leCode = fields[2];
        if (Util.isEmpty(leCode)) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", "\"" + line + "\"", fields[2]);
            reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_LE_CODE);
            return;
        }

        if (entityMap.containsKey(isin)) {
            entityMap.get(isin).items.add(new DataItem(leCode, leverage, lineNumber));
        } else {
            Entity e = new Entity(isin);
            e.addDataItem(new DataItem(leCode, leverage, lineNumber));
            entityMap.put(isin, e);
        }
    }


    @Override
    public String getFileName() {
        return file;
    }


    /**
     * The number of items per thread
     *
     * @return number of items per thread
     */
    private int getBatchSize() {
        return getIntegerAttribute(BATCH_SIZE, 20);
    }

    /**
     * The currency to filter duplicate ISIN
     *
     * @return the currency attribute or default CCY
     */
    private String getCurrency() {
        return !Util.isEmpty(getAttribute(CCY)) ? getAttribute(CCY) : DEFAULT_CCY;
    }

    @Override
    public int getThreadCount() {
        return Math.max(1, super.getThreadCount());
    }

    /**
     * Log error
     *
     * @param controlMCode error type Control-M
     * @param message      error message
     */
    private void reportError(ErrorCodeEnum controlMCode, String message) {
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, message);
        ControlMErrorLogger.addError(controlMCode, message);
        logGen.incrementRecordErrors();
        logGen.feedFullLog(0);
        logGen.feedDetailedLog(0);
        logGen.initializeErrorLine();
        bResult = false;
        processOK = false;
        controlMOK = false;
    }

    /**
     * Entity product class
     */
    static class Entity {

        /**
         * The product ISIN
         */
        final String isin;

        /**
         * The product data items blob in DB
         */
        List<DataItem> items;

        /**
         * Constructor
         *
         * @param isin the product isin
         */
        Entity(String isin) {
            this.isin = isin;
            this.items = new ArrayList<>();
        }

        /**
         * Add new data item
         *
         * @param item the new data item
         */
        void addDataItem(DataItem item) {
            this.items.add(item);
        }

    }

    /**
     * Data item class, contains file line object to process
     */
    static class DataItem {

        /**
         * Legal Entity code value
         */
        final String leCode;

        /**
         * Leverage value
         */
        final double leverage;


        /**
         * Current object line number
         */
        final int lineNumber;

        /**
         * Constructor
         *
         * @param code       the Legal entity code value
         * @param leverage   the Leverage value
         * @param lineNumber the file line number value
         */
        public DataItem(String code, double leverage, int lineNumber) {
            this.leCode = code;
            this.leverage = leverage;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * BatchTask class
     */
    class BatchTask implements Runnable {

        /**
         * Entities to process in batch
         */
        final List<Entity> entities;

        /**
         * Constructor
         *
         * @param entities the list of entities to process
         */
        public BatchTask(List<Entity> entities) {
            this.entities = entities;
        }

        @Override
        public void run() {
            String ccy = getCurrency();
            for (Entity e : entities) {
                Product product = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(),
                        ISIN + "#Ccy=" + ccy, e.isin);
                if (product == null) {
                    bResult = false;
                    for (DataItem di : e.items) {
                        logGen.setErrorGettingProduct(SOURCE, file,
                                String.valueOf(di.lineNumber), "", e.isin);
                        reportError(ErrorCodeEnum.InvalidData, ERR_NO_PRODUCT_ISIN + " " + e.isin);
                    }
                } else {
                    CustomLeveragePercentage result = loadByProduct(product.getId());
                    if (result != null) {
                        for (DataItem di : e.items) {
                            int entityId = CustomLeveragePercentage.CustomLeveragePercentageItem.ERROR_ID;
                            if (CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_CODE.equals(di.leCode)) {
                                entityId = CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_ID;
                            } else {
                                LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), di.leCode);
                                if (legalEntity == null) {
                                    logGen.setErrorGettingLegalEntity(SOURCE, file,
                                            String.valueOf(di.lineNumber), "", di.leCode);
                                    reportError(ErrorCodeEnum.InvalidData, ERR_NO_LE_CODE + " " + di.leCode);
                                } else {
                                    entityId = legalEntity.getId();
                                }
                            }
                            if (entityId != CustomLeveragePercentage.CustomLeveragePercentageItem.ERROR_ID) {
                                int finalEntityId = entityId;
                                if (!Util.isEmpty(result.getItems())) {

                                    Optional<CustomLeveragePercentage.CustomLeveragePercentageItem> dbItem =
                                            result.getItems().stream().
                                                    filter(i -> i.getLegalEntityId() == finalEntityId).findFirst();
                                    if (dbItem.isPresent()) {
                                        //Update
                                        dbItem.get().setPercentage(di.leverage);
                                    } else {
                                        //Insert
                                        result.addPercentage(new CustomLeveragePercentage.
                                                CustomLeveragePercentageItem(product.getId(), finalEntityId, di.leverage));
                                    }
                                } else {
                                    //New leverage for product
                                    result.addPercentage(new CustomLeveragePercentage.
                                            CustomLeveragePercentageItem(product.getId(), finalEntityId, di.leverage));
                                }
                            }
                        }
                        saveCustomLeveragePercentage(result);
                    }
                }
            }

        }

        /**
         * Load CustomLeveragePercentage by product id
         *
         * @param productId the product id
         * @return the CustomLeveragePercentage
         */
        private CustomLeveragePercentage loadByProduct(int productId) {
            RemoteCustomLeverageService service = DSConnection.getDefault().getService(RemoteCustomLeverageService.class);
            try {
                return service.loadByProduct(productId);
            } catch (RemoteException ex) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, ex);
                logGen.setErrorSaveCustomPercentageLeverage(SOURCE, file,
                        String.valueOf(entities.get(0).items.get(0).lineNumber), "", "");
                reportError(ErrorCodeEnum.DataserverError, ex.getMessage());
            }
            return null;
        }

        /**
         * Save the custom leverage percentage
         *
         * @param result the CustomLeveragePercentage to save
         */
        private void saveCustomLeveragePercentage(CustomLeveragePercentage result) {
            RemoteCustomLeverageService service = DSConnection.getDefault().getService(RemoteCustomLeverageService.class);
            if (!Util.isEmpty(result.getItems())) {
                try {
                    service.save(result, false);
                } catch (RemoteException ex) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, ex);
                    logGen.setErrorSaveCustomPercentageLeverage(SOURCE, file,
                            String.valueOf(entities.get(0).items.get(0).lineNumber), "", "");
                    reportError(ErrorCodeEnum.DataserverError, ex.getMessage());
                }
            }
        }
    }

}
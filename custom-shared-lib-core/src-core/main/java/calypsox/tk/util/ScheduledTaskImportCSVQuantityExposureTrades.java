package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Import additional CSV file with Quantity Information
 *
 * @author x865229
 * date 19/11/2022
 */
@SuppressWarnings("unused")
public class ScheduledTaskImportCSVQuantityExposureTrades extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = -2108537583671180872L;

    // Attributes
    public static final String TRADE_ACTION = "Action to Apply";
    public static final String BATCH_SIZE = "Batch Size";
    private static final String SEPARATOR_DOMAIN_STRING = "Separator";
    protected static final String DESTINATION_EMAIL = "Destination email";
    protected static final String PROCESS = "Load EquitySwap Exposure Quantity";

    protected static final String STATIC_DATA_LOG = "Static Data Log";
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    private static final String SEARCH_TRADE_BY = "Search trade by";
    private static final String BY_FO_NUM = "By Num Front Id"; //Number of transaction in Front systems
    private static final String BY_BO_REF = "By BO Reference"; //Back Office Reference of transaction

    private static final String SUBJECT = "Log files for Exposure Trades Quantity update";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    private final static String DO_NOT_USE_CONTROL_LINE = "Do not use Control Line";
    private final static String DO_NOT_USE_CAPTIONS = "Do not use Captions";

    private static final String SOURCE = "EQUITY_SWAP_QUANTITY";

    private static final String KW_BO_REFERENCE = "BO_REFERENCE";
    private static final String KW_EQ_SWAP_QUANTITY = "EqSwapQuantity";
    private static final int NUM_OF_FIELDS = 3;
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String ERR_BAD_NUM_OF_FILES = "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.";
    private static final String ERR_LOG_FILES = "Unexpected error creating log file";
    private static final String ERR_NUM_LINES_CHECK = "Number of lines in file does not match number of lines in control record";
    private static final String ERR_NO_FILE_DATE = "File name does not contain the date part";
    private static final String ERR_WRONG_NUM_OF_FIELDS = "Wrong number of fields";

    private static final String ERR_INVALID_NUM_FRONT_ID = "Invalid NUM_FRONT_ID";
    private static final String ERR_INVALID_BO_REF_ID = "Invalid BO_Reference";

    private static final String ERR_INVALID_ACTION = "Invalid Action";

    private static final String ERR_INVALID_QUANTITY = "Invalid quantity value";
    private static final String ERR_SEND_MAIL = "Error. Error sending log mail.";
    private static final String ERR_MOVING_FILE = "Error. Error creating log files.";


    protected transient LogGeneric logGen = new LogGeneric();

    private transient boolean bResult = true;
    private transient String file = "";

    private transient boolean processOK = true;
    private transient boolean controlMOK = true;
    private transient Action tradeAction;
    private transient boolean useControlLine = true;
    private transient boolean useCaptions = true;
    private transient boolean searchByBoRef = true;

    @Override
    public String getTaskInformation() {
        return "Import Quantity from CSV file and put it to CollateralExposure Trade keyword";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        // Get superclass attributes
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());

        attributeList.addAll(Arrays.asList(
                        attribute(SEPARATOR_DOMAIN_STRING),

                        attribute(TRADE_ACTION),
                        attribute(BATCH_SIZE).integer(),
                        attribute(THREAD_COUNT).integer(),

                        attribute(SUMMARY_LOG),
                        attribute(DETAILED_LOG),
                        attribute(FULL_LOG),
                        attribute(STATIC_DATA_LOG),
                        attribute(DESTINATION_EMAIL),
                        attribute(DO_NOT_USE_CONTROL_LINE)
                                .booleanType(),
                        attribute(DO_NOT_USE_CAPTIONS)
                                .booleanType(),
                        attribute(SEARCH_TRADE_BY)
                                .mandatory()
                                .domain(Arrays.asList(BY_BO_REF, BY_FO_NUM))
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

    public boolean doProcess() {
        final String path = getAttribute(FILEPATH);
        String startFileName = getAttribute(STARTFILENAME);

        String time = timeFormat.format(new Date());
        logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
                time);

        useControlLine = !getBooleanAttribute(DO_NOT_USE_CONTROL_LINE, false);
        useCaptions = !getBooleanAttribute(DO_NOT_USE_CAPTIONS, false);

        searchByBoRef = !BY_FO_NUM.equals(getAttribute(SEARCH_TRADE_BY));

        try {

            logGen.initializeFiles(PROCESS);

            if (!logGen.validateFilesExistence()) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Log files cannot be initialized - check Log attributes");
                this.processOK = false;
                this.controlMOK = false;
                return false;
            }

            String date = CollateralUtilities.getValDateString(this.getValuationDatetime());

            List<String> files = CollateralUtilities.getListFiles(path, startFileName + date);

            if (files.size() == 1) {

                file = files.get(0);
                logGen.initilizeStaticDataLog(file, "LINE");
                final String filePath = path + file;

                try {
                    if (!useControlLine || feedPreProcess(filePath)) {
                        final JDate jdate = CollateralUtilities.getFileNameDate(this.file);
                        if (jdate != null) {
                            // FileUtility.copyFileToDirectory(filePath, path + "/copy/");
                            readFileAndProcessTrades(filePath);
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
                this.controlMOK = false;
                this.processOK = false;
            }

        } catch (IOException e) {
            logGen.setErrorCreatingLogFile(SOURCE, startFileName);
            reportError(ErrorCodeEnum.LogException, ERR_LOG_FILES);
            this.controlMOK = false;
            this.processOK = false;
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
                    final List<String> to = new ArrayList<>();//conn.getRemoteReferenceData().getDomainValues(EQUITIES);
                    // add the scheduledTask attribute emails
                    String emails = getAttribute(DESTINATION_EMAIL);
                    if (!com.calypso.infra.util.Util.isEmpty(emails)) {
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
            this.controlMOK = false;
            this.processOK = false;
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


    private void readFileAndProcessTrades(String filePath) throws Exception {

        final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);

        tradeAction = getTradeActionAttribute(TRADE_ACTION, Action.UPDATE);

        List<DataItem> batch = new ArrayList<>();
        int batchSize = getBatchSize();

        ExecutorService executorService = Executors.newFixedThreadPool(getThreadCount(), Executors.defaultThreadFactory());

        int itemNum = 0;
        int lineNum = 0;
        try (FileReader fr = new FileReader(filePath); BufferedReader br = new BufferedReader(fr)) {
            for (int i = 0; ; i++) {
                String line = br.readLine();

                if (line == null || line.contains("*****"))
                    break;

                if (i == 0 && useCaptions) // skip field names
                    continue;


                DataItem dataItem = parseFileLine(line, separator, i + 1);
                lineNum++;
                if (dataItem != null) {
                    batch.add(dataItem);
                    if (itemNum % batchSize == 0) {
                        executorService.submit(new BatchTask(batch));
                        batch = new ArrayList<>();
                    }
                    itemNum++;
                }
                logGen.incrementTotal();

            }
        }
        if(lineNum > 0 && itemNum == 0){
            //Move to failed the file. Not valid items.
            this.bResult = false;
        }

        if (!batch.isEmpty()) {
            executorService.submit(new BatchTask(batch));
        }

        executorService.shutdown();
        while (!executorService.awaitTermination(15L, TimeUnit.MINUTES)) {
            Log.system(LOG_CATEGORY_SCHEDULED_TASK, "Await Termination");
        }
    }

    private DataItem parseFileLine(String line, String separator, int lineNumber) {
        String[] fields;

        try {
            fields = CollateralUtilities.splitMejorado(NUM_OF_FIELDS, separator, false, line);
        } catch (StringIndexOutOfBoundsException e) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", line, "");
            reportError(ErrorCodeEnum.InvalidData, ERR_WRONG_NUM_OF_FIELDS);
            return null;
        }

        if (fields.length != NUM_OF_FIELDS) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", line, "");
            reportError(ErrorCodeEnum.InvalidData, ERR_WRONG_NUM_OF_FIELDS);
            return null;
        }

        String dealRef;
        if (searchByBoRef) {
            dealRef = fields[1];
            if (Util.isEmpty(dealRef)) {
                logGen.setErrorBadRecordFormat(SOURCE, this.file,
                        String.valueOf(lineNumber), "", line, fields[1]);
                reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_BO_REF_ID + dealRef);
                return null;
            }
        } else {
            dealRef = fields[0];
            if (Util.isEmpty(dealRef)) {
                logGen.setErrorBadRecordFormat(SOURCE, this.file,
                        String.valueOf(lineNumber), "", line, fields[1]);
                reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_NUM_FRONT_ID + dealRef);
                return null;
            }
        }

        double quantity;
        if (Util.isEmpty(fields[2])) {
            logGen.setErrorBadRecordFormat(SOURCE, this.file,
                    String.valueOf(lineNumber), "", line, fields[1]);
            reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_QUANTITY + fields[2]);
            return null;
        } else {
            try {
                quantity = Util.stringToNumberThrowException(fields[2], Locale.US);
            } catch (Exception e) {
                logGen.setErrorBadRecordFormat(SOURCE, this.file,
                        String.valueOf(lineNumber), "", line, fields[1]);
                reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_QUANTITY + fields[2]);
                return null;
            }
        }

        return new DataItem(dealRef, searchByBoRef, quantity, lineNumber);
    }


    @Override
    public String getFileName() {
        return file;
    }


    private int getBatchSize() {
        return getIntegerAttribute(BATCH_SIZE, 100);
    }

    @Override
    public int getThreadCount() {
        return Math.max(1, super.getThreadCount());
    }


    private void reportError(ErrorCodeEnum controlMCode, String message) {
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, message);
        ControlMErrorLogger.addError(controlMCode, message);
        logGen.incrementRecordErrors();
        logGen.feedFullLog(0);
        logGen.feedDetailedLog(0);
        logGen.initializeErrorLine();
    }


    static class DataItem {
        final String dealRef;
        final boolean searchByBoRef;
        final double quantity;
        final int lineNumber;

        public DataItem(String dealRef, boolean searchByBoRef, double quantity, int lineNumber) {
            this.dealRef = dealRef;
            this.searchByBoRef = searchByBoRef;
            this.quantity = quantity;
            this.lineNumber = lineNumber;
        }
    }

    class BatchTask implements Runnable {
        final List<DataItem> dataItems;

        public BatchTask(List<DataItem> dataItems) {
            this.dataItems = dataItems;
        }

        @Override
        public void run() {
            try {
                for (DataItem di : dataItems) {
                    Trade trade = di.searchByBoRef ? findTradeByBOReference(di.dealRef) : findTradeByExternalRef(di.dealRef);
                    if (trade == null) {
                        logGen.setErrorSavingTrade(SOURCE, file,
                                String.valueOf(di.lineNumber), "", di.dealRef);
                        reportError(ErrorCodeEnum.InvalidData, di.searchByBoRef ? ERR_INVALID_BO_REF_ID : ERR_INVALID_NUM_FRONT_ID);
                    } else {
                        trade = trade.clone();
                        trade.setAction(tradeAction);
                        trade.addKeyword(KW_EQ_SWAP_QUANTITY, di.quantity);
                        if(TradeWorkflow.isTradeActionApplicable(trade, tradeAction, DSConnection.getDefault(), null)) {
                            try {
                                DSConnection.getDefault().getRemoteTrade().save(trade);
                            } catch (Exception e) {
                                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
                                logGen.setErrorSavingTrade(SOURCE, file,
                                        String.valueOf(di.lineNumber), "", di.dealRef);
                                reportError(ErrorCodeEnum.DataserverError, e.getMessage());
                            }
                        }else{
                            logGen.setErrorSavingTrade(SOURCE, file,
                                    String.valueOf(di.lineNumber), "", di.dealRef);
                            reportError(ErrorCodeEnum.InvalidData, ERR_INVALID_ACTION + " " + tradeAction
                                    + " on trade  " + di.dealRef  + " status is " + trade.getStatus());
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
                logGen.setErrorSavingTrade(SOURCE, file,
                        String.valueOf(dataItems.get(0).lineNumber), "", "");
                logGen.incrementRecordErrors();
                logGen.feedFullLog(0);
                logGen.feedDetailedLog(0);
                controlMOK = false;
                processOK = false;
            }
        }

        private Trade findTradeByBOReference(String dealRef) throws CalypsoServiceException {
            TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade()
                    .getTradesByKeywordNameAndValue(KW_BO_REFERENCE, dealRef);
            return tradeArray.isEmpty() ? null
                    : Stream.of(tradeArray.getTrades())
                    .filter(t -> !Status.S_CANCELED.equals(t.getStatus()))
                    .findFirst()
                    .orElse(null);
        }

        private Trade findTradeByExternalRef(String extDealRef) throws CalypsoServiceException {
            TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade()
                    .getTradesByExternalRef(extDealRef);
            return tradeArray.isEmpty() ? null
                    : Stream.of(tradeArray.getTrades())
                    .filter(t -> !Status.S_CANCELED.equals(t.getStatus()))
                    .findFirst()
                    .orElse(null);
        }

    }

}

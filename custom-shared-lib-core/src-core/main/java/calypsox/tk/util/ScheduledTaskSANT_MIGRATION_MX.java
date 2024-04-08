package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import calypsox.util.SantMigrationMxFileLine;
import calypsox.util.SantMigrationMxFileReader;
import calypsox.util.SantMigrationMxUtil;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Scheduled Task to initialize Interest & Inflation Rates.
 */
public class ScheduledTaskSANT_MIGRATION_MX extends ScheduledTask {
    private static final long serialVersionUID = -1L;

    /**
     * The Constant IMPORT_FILE_PATH.
     */
    public static final String IMPORT_FILE_PATH = "Path to import file";

    /**
     * The Constant IMPORT_FILE_NAME.
     */
    public static final String IMPORT_FILE_NAME = "Name of import file";

    /**
     * The Constant IMPORT_SEPARATOR.
     */
    public static final String IMPORT_SEPARATOR = "Separator";

    /**
     * The Constant IMPORT_FILEEXTENSION.
     */
    public static final String IMPORT_FILEEXTENSION = "File Extension";

    /**
     * The File Name Date Format
     */
    private static final String FILE_NAME_DATE_FORMAT = "yyyyMMdd";

    /**
     * The Constant IMPORT_FILEEXTENSION.
     */
    private static final String DEFAULT_EXTENSION = "dat";

    /**
     * The Constant EMTPY_SPACE.
     */
    public static final String EMPTY_SPACE = "";

    /**
     * The Constant REGEX_NOT_DIGIT.
     */
    public static final String REGEX_NOT_DIGIT = "[\\D]";

    /**
     * The exceptions.
     */
    protected SantExceptions exceptions;

    /**
     * Instantiates a new scheduled task csvreport.
     */
    public ScheduledTaskSANT_MIGRATION_MX() {
        super();
        this.exceptions = new SantExceptions();
    }

    @Override
    public String getTaskInformation() {
        return "Migration MX2.11 to MX3.1.";
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.util.ScheduledTaskCRE_SENDER#isValidInput(java.util.Vector
     * )
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);
        final String attribute = getAttribute(IMPORT_FILE_NAME);
        if (attribute.trim().isEmpty()) {
            messages.add("Should input 'Path to import file'");
        }

        final String attribute2 = getAttribute(IMPORT_FILE_PATH);
        if (attribute2.trim().isEmpty()) {
            messages.add("Should input 'Path to import file'");
        }

        final String attribute3 = getAttribute(IMPORT_SEPARATOR);
        if (attribute3.trim().isEmpty() && attribute3.length() > 1) {
            messages.add("Should input 'Separator'");
        }

        final String attribute4 = getAttribute(IMPORT_FILEEXTENSION);
        if (attribute4.trim().isEmpty()) {
            messages.add("Should input 'File Extension'");
        }

        return messages.isEmpty();
    }

    // @Override
    // @SuppressWarnings("rawtypes")
    // public Vector getDomainAttributes() {
    // final Vector<String> result = new Vector<String>();
    // result.add(IMPORT_FILE_PATH);
    // result.add(IMPORT_FILE_NAME);
    // result.add(IMPORT_SEPARATOR);
    // result.add(IMPORT_FILEEXTENSION);
    // return result;
    // }

    // Like getDomainAttributes
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(IMPORT_SEPARATOR));
        attributeList.add(attribute(IMPORT_FILE_NAME));
        attributeList.add(attribute(IMPORT_FILE_PATH));
        attributeList.add(attribute(IMPORT_FILEEXTENSION));

        return attributeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.util.ScheduledTaskREPORT#getAttributeDomain(java.lang.
     * String, java.util.Hashtable)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Vector getAttributeDomain(final String attribute,
                                     final Hashtable hashtable) {
        Vector vector = new Vector();
        if (attribute.equals(IMPORT_FILEEXTENSION)) {
            vector.addElement(DEFAULT_EXTENSION);
        }
        return vector;
    }

    /**
     * Gets the file extension.
     *
     * @return
     */
    private String getExtension() {
        final String fileExtension = getAttribute(IMPORT_FILEEXTENSION);
        return (fileExtension.isEmpty()) ? DEFAULT_EXTENSION : fileExtension;
    }

    /**
     * Gets the file name.
     *
     * @return the file name
     */
    public String getFileName() {
        final String filename = getAttribute(IMPORT_FILE_NAME);
        final String fileExtension = getExtension();

        final Date date = getValuationDatetime()
                .getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault());
        final DateFormat sdf = new SimpleDateFormat(FILE_NAME_DATE_FORMAT);
        final String sDate = sdf.format(date);

        final StringBuilder sbFileName = new StringBuilder(filename);
        sbFileName.append(sDate).append(".").append(fileExtension);

        Log.debug(this, "Input file name compounded: \"" + sbFileName + "\".");
        return sbFileName.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.util.ScheduledTaskREPORT#process(com.calypso.tk.service
     * .DSConnection, com.calypso.tk.event.PSConnection)
     */
    @Override
    public boolean process(final DSConnection dsconnection,
                           final PSConnection psconnection) {

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.process");

        ArrayList<String> error = new ArrayList<String>();

        boolean checkProcess = true;
        if (isValidInputData()) {

            // Read file
            final String fileName = getCompleteFileName();
            Log.info(this, "Read file: " + fileName);
            final Vector<SantMigrationMxFileLine> lines = readFile(
                    getAttribute(IMPORT_SEPARATOR), fileName);

            if (!Util.isEmpty(lines)) {
                // Process Lines
                processOtcLines(lines, error);
            }

        } else {
            Log.error(this, "Input Data not valid");
            checkProcess = false;
        }

        if (!error.isEmpty()) {
            for (String msg : error) {
                Log.error(this, msg);
                publishError(msg, SantExceptionType.TECHNICAL_EXCEPTION, true);
            }
        }

        // Move the file
        moveFile(checkProcess);

        Log.info(this, "END ScheduledTaskSANT_MIGRATION_MX.process");

        return checkProcess;
    }

    /**
     * Move file if the process is or not correct.
     *
     * @param checkProcess
     */
    private void moveFile(final boolean checkProcess) {
        boolean isMoveFile = false;
        final String fileName = getFileName();
        final String filePath = SantMigrationMxUtil.getInstance().getFilePath(
                getAttribute(IMPORT_FILE_PATH));
        isMoveFile = SantMigrationMxUtil.getInstance().moveFileTo(fileName,
                filePath, checkProcess);
        if (!isMoveFile) {
            final StringBuilder str = new StringBuilder(
                    "Could not move the file: ");
            str.append(fileName);
            publishError(ErrorCodeEnum.InputFileCanNotBeMoved,
                    new String[]{str.toString()},
                    SantExceptionType.TECHNICAL_EXCEPTION, true);
        }
    }

    /**
     * Gets the complete file name.
     *
     * @return the complete file name
     */
    public String getCompleteFileName() {
        final String filePath = SantMigrationMxUtil.getInstance().getFilePath(
                getAttribute(IMPORT_FILE_PATH));
        final String filename = filePath.concat(getFileName());

        Log.debug(this, "Input file name compounded: \"" + filename + "\".");

        return (filename);
    }

    /**
     * Checks if is valid input data.
     *
     * @return true, if is valid input data
     */
    private boolean isValidInputData() {
        final String fileName = getCompleteFileName();
        final boolean existsFile = SantMigrationMxUtil.getInstance()
                .isFileExists(fileName);

        if (!existsFile) {
            final StringBuilder str = new StringBuilder(
                    "Could not read input file: ");
            str.append(fileName);
            publishError(ErrorCodeEnum.InputFileNotFound,
                    new String[]{str.toString()},
                    SantExceptionType.TECHNICAL_EXCEPTION, true);
        }

        Log.info(this, "ScheduledTaskSANT_MIGRATION_MX - Exists file: "
                + existsFile);

        return existsFile;
    }

    /**
     * Read the file.
     *
     * @param file
     * @return
     */
    protected Vector<SantMigrationMxFileLine> readFile(final String separator,
                                                       final String fileName) {

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.readFile");
        Log.info(this, "GET all info reading the file:  " + fileName);

        Vector<SantMigrationMxFileLine> data = new Vector<SantMigrationMxFileLine>();

        // Get data from file
        data = getDataFromFile(separator, fileName);

        if (data.isEmpty()) {
            final StringBuilder str = new StringBuilder();

            str.append("No data was processed from file: ");
            str.append(fileName);

            Log.info(this, str.toString());
        }

        Log.info(this, "END ScheduledTaskSANT_MIGRATION_MX.readFile");

        return data;

    }

    /**
     * Get data from parameter file and using scheduled task attributes.
     *
     * @param fileName
     * @return
     */
    protected Vector<SantMigrationMxFileLine> getDataFromFile(
            final String separator, final String fileName) {

        Vector<SantMigrationMxFileLine> data = new Vector<SantMigrationMxFileLine>();
        final SantMigrationMxFileReader reader = new SantMigrationMxFileReader(
                false);
        try {
            data = reader.readFile(separator, fileName);
        } catch (final IOException e) {
            final StringBuilder str = new StringBuilder();

            str.append("Error while reading file: ");
            str.append(fileName);
            str.append("Exception: ");
            str.append(e.toString());
            Log.error(this, str.toString(), e);

            publishError(ErrorCodeEnum.InputFileCanNotBeRead,
                    new String[]{str.toString()},
                    SantExceptionType.TECHNICAL_EXCEPTION, true);

        }

        // // Show lines with error on log
        // if (!reader.getErrorLineMap().isEmpty()) {
        // final HashMap<Integer, MigrationMxFileLine> map = reader
        // .getErrorLineMap();
        // final String lines = StringUtils.join(map.keySet(), ",");
        // final StringBuilder str = new StringBuilder("Lines: [");
        // str.append(lines);
        // str.append("] have empty value in NB_MX2, CONTRACT_REFERENCE or CAP_INT column: They won't processed.");
        //
        // Log.info(this, str.toString());
        // }

        return data;
    }

    /**
     * Process OTC lines. No mirror trade.
     * <p>
     * - External Reference: value ID MX3.1FXFI with MX
     * <p>
     * - TRADE_KEYWORD.NUM_FRONT_ID: numeric value ID MX3.1FXFI.
     * <p>
     * - TRADE_KEYWORD.FO_SYSTEM: value ID "MX".
     * <p>
     * - TRADE_KEYWORD.Mx211Migrated: old extRef value (MX2.11) (without ME)
     *
     * @param otcLines
     */
    private void processOtcLines(
            final Vector<SantMigrationMxFileLine> otcLines,
            final ArrayList<String> error) {

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.processOtcLines");

        for (SantMigrationMxFileLine line : otcLines) {

            Log.info(this, "Process Line: " + line.toString());

            final Trade trade = getTradeFromMx211Reference(line, error);
            if (trade != null) {
                Log.info(this, "Get Trade Mx2.11: " + trade.getLongId());
                updateTrade(line, trade, error);
            } else {
                final String msg = "The trade to get does not exist. ";
                SantMigrationMxUtil.getInstance().addLogError(
                        this.getClass().getSimpleName(), line, "", msg,
                        new String[]{}, error);
            }
        }

        Log.info(this, "END ScheduledTaskSANT_MIGRATION_MX.processOtcLines");
    }

    /**
     * Get trade from Mx211 Id.
     *
     * @param line
     * @return
     */
    private Trade getTradeFromMx211Reference(SantMigrationMxFileLine line,
                                             final ArrayList<String> error) {

        Log.info(this,
                "START ScheduledTaskSANT_MIGRATION_MX.getTradeFromMx211Reference");

        Trade trade = null;
        String murexReference = (String) line
                .getColumnValue(SantMigrationMxFileLine.NUMERO_DE_MX2);

        if (!Util.isEmpty(murexReference)) {
            trade = SantMigrationMxUtil.getInstance()
                    .getTradeFromMurexReference(murexReference);
        } else {
            final String msg = "The Mx211 reference is empty or null.. ";
            SantMigrationMxUtil.getInstance().addLogError(
                    this.getClass().getSimpleName(), line, "", msg,
                    new String[]{}, error);
        }

        Log.info(this,
                "END ScheduledTaskSANT_MIGRATION_MX.getTradeFromMx211Reference");

        return trade;
    }

    /**
     * Update the trade: - Change ExtRef from Mx2.11 to Mx3.1 - Add new
     * keywords.
     *
     * @param line
     * @param trade
     * @param error
     */
    private void updateTrade(SantMigrationMxFileLine line, Trade trade,
                             final ArrayList<String> error) {
        boolean isSaveOk = false;

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.updateTradeOtc");
        Log.info(this, "Update and save trade in OTC process: " + trade.getLongId());

        // Get the new keywords
        final Hashtable<String, String> newKws = getNewKeywordsFromFile(line);

        if (!Util.isEmpty(newKws)) {
            // Save trade and keywords
            isSaveOk = saveTradeKeywords(line, trade, newKws);

            if (!isSaveOk) {
                final String msg = "Cannot save the trade %s. ";
                SantMigrationMxUtil.getInstance().addLogError(
                        this.getClass().getSimpleName(), line, "", msg,
                        new String[]{String.valueOf(trade.getLongId())}, error);
            }
        } else {
            final String msg = "Cannot get the new keywords to add to the trade %s.";
            SantMigrationMxUtil.getInstance().addLogError(
                    this.getClass().getSimpleName(), line, "", msg,
                    new String[]{String.valueOf(trade.getLongId())}, error);
        }

        Log.info(this, "END ScheduledTaskSANT_MIGRATION_MX.updateTradeOtc");

    }

    /**
     * It saves the trade and its keywords.
     *
     * @param trade
     * @param keywords
     * @return true if the save process was successful, false in other case.
     */
    public boolean saveTradeKeywords(final SantMigrationMxFileLine line,
                                     final Trade trade, final Hashtable<String, String> keywords) {

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.saveTradeKeywords");

        boolean isSaveTradeOk = false;

        // Include new keywords
        final Trade newTrade = SantMigrationMxUtil.getInstance()
                .includeKeywords(trade, keywords);

        // Set UPDATE action and the new ExternalReference
        SantMigrationMxUtil.getInstance().setTradeConfigMigration(line,
                newTrade);

        // save Trade
        isSaveTradeOk = SantMigrationMxUtil.getInstance().saveTrade(newTrade);

        Log.info(this, "START ScheduledTaskSANT_MIGRATION_MX.saveTradeKeywords");

        return isSaveTradeOk;
    }

    /**
     * Gets the new keywords to add the trade.
     *
     * @param line
     * @return
     */
    private Hashtable<String, String> getNewKeywordsFromFile(
            SantMigrationMxFileLine line) {
        Log.info(this,
                "START ScheduledTaskSANT_MIGRATION_MX.getNewKeywordsFromFile");

        final String contractRef = ((String) line
                .getColumnValue(SantMigrationMxFileLine.CONTRACT_REFERENCE));
        final String newMx211Migrated = (String) line
                .getColumnValue(SantMigrationMxFileLine.NUMERO_DE_MX2);

        if (Util.isEmpty(contractRef) || Util.isEmpty(newMx211Migrated)) {
            Log.info(this, "Values CONTRACT_REFERENCE [" + contractRef
                    + "] and NB_MX2 [" + newMx211Migrated
                    + "] must not be empty or null.");
            return null;
        }

        final String numFromtId = contractRef.replaceAll(REGEX_NOT_DIGIT,
                EMPTY_SPACE);
        final String foSystem = SantMigrationMxUtil.PREFIX_MUREX_3_1;

        Log.info(this, "Set New Keywords:");
        Log.info(this, "-------------------------------------------------");
        Log.info(this, "TRADE_kEYWORD NUM_FRONT_ID: " + numFromtId);
        Log.info(this, "TRADE_kEYWORD MX211_MIGRATED: " + newMx211Migrated);
        Log.info(this, "TRADE_kEYWORD FO_SYSTEM: " + foSystem);
        Log.info(this, "-------------------------------------------------");

        // Set the new keywords
        Hashtable<String, String> newKws = new Hashtable<String, String>();
        newKws.put(TradeInterfaceUtils.TRADE_KWD_NUM_FRONT_ID, numFromtId);
        newKws.put(SantMigrationMxUtil.TRADE_KWD_MX211_MIGRATED,
                newMx211Migrated);
        newKws.put(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, foSystem);

        Log.info(this,
                "END ScheduledTaskSANT_MIGRATION_MX.getNewKeywordsFromFile");

        return newKws;
    }

    /**
     * Publish error.
     *
     * @param errorCode       the error code
     * @param ex              the ex
     * @param publishTasksNow the publish tasks now
     */
    private void publishError(final String comment, final SantExceptionType ex,
                              final boolean publishTasksNow) {
        this.exceptions.addException(ex, getExternalReference(), comment, 0, 0,
                getId(), this.getClass().getSimpleName(), 0);
        Log.error(this, comment);
        if (publishTasksNow) {
            this.exceptions.publishTasks(getDSConnection(), 0, null);
        }
    }

    /**
     * Publish error.
     *
     * @param errorCode       the error code
     * @param errorParams     the error params
     * @param ex              the ex
     * @param publishTasksNow the publish tasks now
     */
    private void publishError(final ErrorCodeEnum errorCode,
                              final String[] errorParams, final SantExceptionType ex,
                              final boolean publishTasksNow) {
        final String errorCodeDetail = errorCode
                .getFullTextMesssage(errorParams);
        ControlMErrorLogger.addError(errorCode, errorCodeDetail);

        this.exceptions.addException(ex, getExternalReference(),
                errorCodeDetail, 0, 0, getId(),
                this.getClass().getSimpleName(), 0);
        Log.error(this, errorCodeDetail);
        if (publishTasksNow) {
            this.exceptions.publishTasks(getDSConnection(), 0, null);
        }
    }

}

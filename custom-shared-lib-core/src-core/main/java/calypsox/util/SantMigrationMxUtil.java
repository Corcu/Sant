package calypsox.util;

import calypsox.tk.util.log.LogGeneric;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.io.*;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

public class SantMigrationMxUtil {

    /**
     * Constant DEFULT_SEPARATOR
     */
    private static final char DEFAULT_SEPARATOR = ';';

    /**
     * The Constant PREFIX_MUREX_3_1.
     */
    public static final String PREFIX_MUREX_3_1 = "MX";

    /**
     * The Constant PREFIX_MUREX_2_11.
     */
    public static final String PREFIX_MUREX_2_11 = "ME";

    /**
     * The Constant OK.
     */
    public static final String OK = "ok";

    /**
     * The Constant FAIL.
     */
    public static final String FAIL = "fail";

    /**
     * The Constant KEYWORD_MX211_MIGRATED.
     */
    public static final String TRADE_KWD_MX211_MIGRATED = "Mx211Migrated";

    private static SantMigrationMxUtil instance;

    /**
     * Instantiates a new MigrationMxSTUtil util.
     */
    protected SantMigrationMxUtil() {
        // nothing to do
    }

    /**
     * Gets the single instance of MigrationMxSTUtil.
     *
     * @return single instance of MigrationMxSTUtil
     */
    public static synchronized SantMigrationMxUtil getInstance() {
        if (instance == null) {
            instance = new SantMigrationMxUtil();
        }
        return instance;
    }

    /**
     * Only to JUnit. You can use that to replace the instance with a mockito
     * instance
     *
     * @param mockInstance the new instance
     */
    public static void setInstance(final SantMigrationMxUtil mockInstance) {
        instance = mockInstance;
    }

    /**
     * Gets the file Separator.
     *
     * @param importSeparator
     * @return
     */
    private char getSeparator(final String separatorImported) {

        if (!Util.isEmpty(separatorImported) && separatorImported.length() != 1) {
            final StringBuilder str = new StringBuilder();

            str.append("The attribute 'Separator' is not correct: ");
            str.append(separatorImported);
            str.append("Using the DEFAULT_SEPARATOR: " + DEFAULT_SEPARATOR);
            Log.warn(this, str.toString());

            return DEFAULT_SEPARATOR;
        }

        return separatorImported.charAt(0);
    }

    /**
     * Create an instance of FileInputStream from an input file path.
     *
     * @param file
     * @return
     */
    public FileInputStream createFileInputStream(final String file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.error(this, "Error: File " + file + " Not Found", e);
        }

        return fis;
    }

    /**
     * createReader.
     *
     * @param inputStream InputStream
     * @param charSet     Charset
     * @return Reader
     * @throws IOException
     */
    public Reader createReader(final InputStream inputStream)
            throws IOException {
        return new InputStreamReader(inputStream, Charset.defaultCharset());
    }

    /**
     * Create an instance of BufferedReader from a String.
     *
     * @param file String
     * @return an instance of BufferedReader
     * @throws IOException
     */
    public BufferedReader createBufferedReader(final String file)
            throws IOException {
        final InputStream inputStream = createFileInputStream(file);

        if (inputStream == null) {
            throw new FileNotFoundException("Error: File " + file
                    + " Not Found.");
        }

        final Reader reader = createReader(inputStream);
        return new BufferedReader(reader);
    }

    /**
     * Create an instance of Scanner from a String
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public Scanner createScanner(String fileName) throws IOException {
        return new Scanner(createBufferedReader(fileName));
    }

    /**
     * Parse a CSV line.
     *
     * @param cvsLine
     * @param separator
     * @return
     */
    public List<String> parseLine(final String cvsLine, final String separator) {

        List<String> result = new ArrayList<String>();

        final char fileSeparator = getSeparator(separator);

        // if empty, return!
        if (cvsLine == null || cvsLine.isEmpty()) {
            return result;
        }

        StringBuffer curVal = new StringBuffer();
        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {

            if (ch == fileSeparator) {
                result.add(curVal.toString());
                curVal = new StringBuffer();
            } else if (ch == '\r') {
                // ignore LF characters
                continue;
            } else if (ch == '\n') {
                // the end, break!
                break;
            } else {
                curVal.append(ch);
            }
        }

        result.add(curVal.toString());

        return result;
    }

    // /**
    // * Add prefix MX to the String argument.
    // *
    // * @param value
    // * @return
    // */
    // public String addPrefixMx31(final String value) {
    // Log.info(this, "MigrationMxFileReader.addPrefixMx31 - Value is ["
    // + value + "]");
    // if (!Util.isEmpty(value)) {
    // return SantMigrationMxUtil.PREFIX_MUREX_3_1.concat(value);
    // }
    //
    // return value;
    // }
    //
    // /**
    // * Add prefix ME to the String argument.
    // *
    // * @param value
    // * @return
    // */
    // public String addPrefixMx211(final String value) {
    // Log.info(this, "MigrationMxFileReader.addPrefixMx211 - Value is ["
    // + value + "]");
    // if (!Util.isEmpty(value)) {
    // return SantMigrationMxUtil.PREFIX_MUREX_2_11.concat(value);
    // }
    //
    // return value;
    // }

    /**
     * Gets the trade from murex reference.
     *
     * @param murexReference the murex reference
     * @return the trade from murex reference
     */
    public Trade getTradeFromMurexReference(final String murexReference) {

        Trade trade = null;
        TradeArray tradeArray = null;
        try {
            tradeArray = DSConnection.getDefault().getRemoteTrade()
                    .getTradesByExternalRef(murexReference);
        } catch (final RemoteException e) {
            Log.error(SantMigrationMxUtil.class.getSimpleName(),
                    e.getMessage(), e);
        }

        // Only one trade. No mirror trade.
        if (!Util.isEmpty(tradeArray) && tradeArray.size() == 1) {
            trade = tradeArray.get(0);
        }

        return trade;
    }

    /**
     * It gets the file path with correct format: it ends with
     * File.separatorChar (/ or \)
     *
     * @param filePath
     * @return
     */
    public String getFilePath(String filePath) {
        final char lastChar = filePath.charAt(filePath.length() - 1);

        if (lastChar != File.separatorChar) {
            filePath += File.separatorChar;
        }

        return filePath;
    }

    /**
     * It moves the file from source folder to OK folder or FAIL folder if the
     * process of ST was successful.
     *
     * @param filename
     * @param origFolder
     * @param success
     * @return
     */
    public boolean moveFileTo(final String filename, final String origFolder,
                              final boolean success) {

        final String origin = origFolder.concat(filename);
        boolean isMoved = false;
        StringBuilder target = new StringBuilder(origFolder);

        if (success) {
            target.append(OK);
        } else {
            target.append(FAIL);
        }

        target.append(File.separator).append(filename);

        isMoved = isFileMoved(origin, target.toString());

        return isMoved;
    }

    /**
     * Call FileUtiliy method moveFile.
     *
     * @param origin
     * @param target
     * @return
     */
    protected boolean isFileMoved(final String origin, final String target) {
        try {
            FileUtility.moveFile(origin, target);
        } catch (final IOException e) {
            final String message = String
                    .format("Could not move file from \"%s\" to \"%s\"",
                            origin, target);
            Log.error(this, message, e);
            return false;
        }

        return true;
    }

    /**
     * It checks if the file exists or not.
     *
     * @param fileName
     * @return true if the file exists, false in other case.
     */
    public boolean isFileExists(final String fileName) {
        final File f = new File(fileName);
        return f.exists();
    }

    /**
     * It saves the trade.
     *
     * @param trade
     * @return true if the trade was save properly. False, in other case.
     */
    public boolean saveTrade(final Trade trade) {
        Log.info(this, "START SantMigrationMxUtil.saveTrade");

        boolean isSaved = true;
        long idTrade = -1;
        try {
            idTrade = DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (RemoteException e) {
            isSaved = false;
            String message = String.format("Cannot save the trade %d",
                    trade.getLongId());
            Log.error(this, message, e);
        }

        if (idTrade <= 0) {
            Log.error(this, "Failed to save the Trade");
            isSaved = false;
        } else {
            Log.info(SantMigrationMxUtil.class.getSimpleName(),
                    "Trade saved with id " + idTrade);
        }

        Log.info(this, "END SantMigrationMxUtil.saveTrade");

        return isSaved;
    }

    @SuppressWarnings("unchecked")
    public Trade includeKeywords(final Trade dbTrade,
                                 final Hashtable<String, String> kwToInsert) {

        // Clones from the old Trade
        Trade newTrade = new Trade();
        newTrade = (Trade) dbTrade.clone();

        // Get old Keywords
        final Hashtable<String, String> tradeAtts = dbTrade.getKeywords();

        // Delete all current keyword values
        final Iterator<Entry<String, String>> it = tradeAtts.entrySet()
                .iterator();
        while (it.hasNext()) {
            final Entry<String, String> entry = it.next();
            newTrade.removeKeyword(entry.getKey());
        }

        // Add the new keywords to the old keywords, and put all in new Trade
        tradeAtts.putAll(kwToInsert);
        newTrade.setKeywords(tradeAtts);

        return newTrade;
    }

    /**
     * Show log error
     *
     * @param className
     * @param line
     * @param processError
     * @param messageError
     * @param params
     * @param errors
     */
    public void addLogError(final String className,
                            final SantMigrationMxFileLine line, final String processError,
                            String messageError, final Object[] params,
                            final List<String> errors) {

        messageError = String.format(messageError, params);

        final StringBuilder str = new StringBuilder(processError);
        str.append(messageError);
        str.append("Line: ").append(line.toString());
        errors.add(str.toString());
        Log.error(className, str.toString());
    }

    public static void returnErrorLog(LogGeneric logGen, boolean status,
                                      JDate date, String fileName, String path, String summaryLog,
                                      String className) {

        try {
            ForexClearFileReader.postProcess(status, date, fileName, path);
        } catch (Exception e1) {
            Log.error(className, e1); // sonar
        }
        try {
            logGen.feedGenericLogProcess(fileName, summaryLog, className,
                    logGen.getNumberTotal() - 1);
            logGen.feedFullLog(0);
            logGen.feedDetailedLog(0);
            logGen.closeLogFiles();
        } catch (Exception e) {
            Log.error(SantMigrationMxUtil.class, e);
        }
    }

    /**
     * Set the new configuration of the trade to migrate.
     *
     * @param line
     * @param trade
     */
    public void setTradeConfigMigration(final SantMigrationMxFileLine line,
                                        final Trade trade) {

        Log.info(this,
                "START ScheduledTaskSANT_MIGRATION_MX.setTradeConfigMigration");

        // Set the new ExtRef
        final String extRefFromFile = (String) line
                .getColumnValue(SantMigrationMxFileLine.CONTRACT_REFERENCE);

        if (!Util.isEmpty(extRefFromFile)) {
            // Set the Action
            trade.setAction(Action.AMEND);

            // Set the new Ext Reference
            trade.setExternalReference(extRefFromFile);

            Log.info(this, "Set UPDATE action: " + trade.getAction().toString()
                    + " to trade: " + trade.getLongId());
            Log.info(this, "ExternalReference: " + extRefFromFile);
        }

        Log.info(this,
                "END ScheduledTaskSANT_MIGRATION_MX.setTradeConfigMigration");

    }
}

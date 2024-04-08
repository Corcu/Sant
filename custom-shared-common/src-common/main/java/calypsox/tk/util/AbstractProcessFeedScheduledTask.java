package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.exception.SantExceptionType;
import calypsox.tk.util.exception.SantExceptionUtil;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.FileUtility;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * This class is an abstract ScheduledTask to process Feeds coming in to the system. This has functionality to handle
 * bad entries in the feeds that we receive. All the Feed process Scheduled task should extend this class.
 *
 * @author Soma & Jose David Sevillano
 */
public abstract class AbstractProcessFeedScheduledTask extends ScheduledTask {
    private static final long serialVersionUID = 123L;
    
    public static final String LOG_CATEGORY_SCHEDULED_TASK = "ScheduledTask";

    // Class to manage the different Exceptions.
    protected SantExceptionUtil santExUtil;//Sonar

    public static final String FILEPATH = "File Path";
    public static final String STARTFILENAME = "Start of File Name";

    private static final String OK = "ok";
    private static final String FAIL = "fail";

    protected static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private static final String EMAILS_MAPPING = "tradeImportLogRecipients";
    public static final String PRODUCT_MAPPING_DOMAIN_VALUE = "ColExpProductsMapping";

    private final ArrayList<BadFeedEntry> badEntries = new ArrayList<BadFeedEntry>();
    private final ArrayList<BadFeedEntry> badEntries2 = new ArrayList<BadFeedEntry>();

    /**
     * This method returns the file name excluding path
     *
     * @return
     */
    public abstract String getFileName();

    public void feedStaticDataLog(final File log, final String warningLine, final String tradeID, final FileWriter fw,
                                  final String system) throws IOException {
        final String[] warningLines = warningLine.split("\n");
        for (int i = 0; i < warningLines.length; i++) {
            if (warningLines[i].contains("INSTRUMENT")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String instrument = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";INSTRUMENT;" + instrument + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";INSTRUMENT;" + "" + "\n");
                }
            } else if (warningLines[i].contains("CURRENCY")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String currency = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY;" + currency + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY;" + "" + "\n");
                }

            } else if (warningLines[i].contains("Legal Entity")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String counterparty = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";COUNTERPARTY;" + counterparty + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";COUNTERPARTY;" + "" + "\n");
                }
            } else if (warningLines[i].contains("PORTFOLIO")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String portfolio = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";PORTFOLIO;" + portfolio + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";PORTFOLIO;" + "" + "\n");
                }

            } else if (warningLines[i].contains("ISIN")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String isin = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";ISIN;" + isin + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";ISIN;" + "" + "\n");
                }
            } else if (warningLines[i].contains("CURRENCY_PAIR")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String currencyPair = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY PAIR;" + currencyPair + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY PAIR;" + "" + "\n");
                }

            } else if (warningLines[i].contains("The type")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String type = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";TYPE;" + type + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";TYPE;" + "" + "\n");
                }

            } else if (warningLines[i].contains("ISIN and its")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 7) {
                    final String currencyForQuote = warningLin[7];
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY FOR QUOTE;"
                            + currencyForQuote + "\n");
                } else {
                    fw.write("CALYPSO_COLATERALES;" + system + ";" + tradeID + ";CURRENCY FOR QUOTE;" + "" + "\n");
                }

            }

        }
    }

    /**
     * Method to log the process information before the load.
     *
     * @param counters
     * @throws Exception Any Exception occurred during the pre process.
     */
    public void feedFullLog(final File log, final String errorLine, final String warningLine, final String okLine,
                            final int tradeID, final FileWriter fw) {
        if (!errorLine.equals("")) {
            try {
                fw.write(errorLine);
            } catch (final IOException e) {
                Log.error(this, e);
            }
        }

        if (!warningLine.equals("") && errorLine.equals("")) {
            final String[] warningLines = warningLine.split("\n");
            for (int i = 0; i < warningLines.length; i++) {
                final String[] warningLin = warningLines[i].split(";");
                final String warningLin_1 = warningLin[0] + ";" + warningLin[1] + ";" + warningLin[2] + ";"
                        + warningLin[3] + ";";
                final String warningLin_2 = ";" + warningLin[5] + ";" + warningLin[6] + "\n";
                final String warningL = warningLin_1 + tradeID + warningLin_2;
                try {
                    fw.write(warningL);
                } catch (final IOException e) {
                    Log.error(this, e);
                }
            }
        }

        if (!okLine.equals("") && warningLine.equals("") && errorLine.equals("")) {
            try {
                fw.write(okLine);
            } catch (final IOException e) {
                Log.error(this, e);
            }
        }
    }

    /**
     * Method to log the process information before the load.
     *
     * @param counters
     * @throws Exception Any Exception occurred during the pre process.
     */
    public void feedDetailedLog(final File log, final String errorLine, final String warningLine, final int tradeID,
                                final FileWriter fw) {
        if (!errorLine.equals("")) {
            try {
                fw.write(errorLine);
            } catch (final IOException e) {
                Log.error(this, e);
            }
        }

        if (!warningLine.equals("") && errorLine.equals("")) {
            final String[] warningLines = warningLine.split("\n");
            for (int i = 0; i < warningLines.length; i++) {
                final String[] warningLin = warningLines[i].split(";");
                final String warningLin_1 = warningLin[0] + ";" + warningLin[1] + ";" + warningLin[2] + ";"
                        + warningLin[3] + ";";
                final String warningLin_2 = ";" + warningLin[5] + ";" + warningLin[6] + "\n";
                final String warningL = warningLin_1 + tradeID + warningLin_2;
                try {
                    fw.write(warningL);
                } catch (final IOException e) {
                    Log.error(this, e);
                }
            }
        }
    }

    /**
     * Method to log the process information before the load.
     *
     * @param counters
     * @throws Exception Any Exception occurred during the pre process.
     */
    public String feedGenericLogProcess(final int total, final int error, final int warning, final String file,
                                        final String summaryLog, final String load) {
        try {
            final Date d = new Date();
            String time = "";
            synchronized (timeFormat) {
                time = timeFormat.format(d);
            }

            final File fichero = new File(summaryLog + "_" + time + ".txt");
            final BufferedWriter bw = new BufferedWriter(new FileWriter(fichero.toString()));
            final Date dat = new Date();
            bw.write(dat.toString());
            bw.write("\n");
            bw.write("PROCESS: " + load + "\n");
            bw.write("FILE: " + file + "\n");
            if (error > 0) {
                bw.write("STATUS: ERROR \n");
            } else if (warning > 0) {
                bw.write("STATUS: WARNING \n");
            } else {
                bw.write("STATUS: OK \n");
            }
            bw.write("NUMBER OF RECORDS PROCESSED: " + (total) + "\n");
            bw.write("NUMBER OF RECORDS OK: " + (total - error - warning) + "\n");
            bw.write("NUMBER OF RECORDS ERRORS: " + error + "\n");
            bw.write("NUMBER OF RECORDS WARNINGS: " + warning + "\n");
            bw.write("SEE LOG FOR MORE DETAILS." + "\n");

            bw.close();
            return fichero.toString();
        } catch (final IOException ioe) {
            Log.error(this, ioe);//Sonar
            return "";
        }
    }

    /**
     * Method to pre process the information before the load (Only for Susi and GBO imports)
     *
     * @param file File to checking the control line, identifying if the information is correct.
     * @return TRUE if correct, else FALSE.
     * @throws Exception Any Exception occurred during the pre process.
     */
    public boolean feedPreProcessSusiGBO(final String file) throws Exception {
        this.santExUtil = new SantExceptionUtil(getDSConnection(), 0, this.getValuationDatetime(), getDatetime(),
                this.getUndoDatetime());

        BufferedReader inputFileStream = null; // Input stream to read the CSV
        // file.;
        String line = null;
        boolean numLinesIsOK = false;
        boolean controlLine = false;

        try {
            inputFileStream = new BufferedReader(new FileReader(file));
            for (int i = 0; (line = inputFileStream.readLine()) != null; i++) {
                if (line.startsWith("*****")) {
                    controlLine = true;
                    if (i == Integer.parseInt(line.substring(5, 13))) {
                        numLinesIsOK = true;
                    }
                }
            }
            inputFileStream.close();
            if (!controlLine) {
                throw new Exception("No line control in the file.");
            }
            if (!numLinesIsOK) {
                // Marisa y Alberto 20/01/2015; correcci?n para evitar la perdida futura del throw
                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileInvalidFormat, "Error in the number of lines.");

                throw new Exception("Error in the number of lines.");
            }

        } catch (final FileNotFoundException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + file, e);
            throw e;
        } catch (final IOException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + file, e);
            throw e;
        }

        return numLinesIsOK;
    }

    /**
     * Method to pre process the information before the load.
     *
     * @param file File to checking the control line, identifying if the information is correct.
     * @return TRUE if correct, else FALSE.
     * @throws Exception Any Exception occurred during the pre process.
     */
    public boolean feedPreProcess(final String file) throws Exception {
        this.santExUtil = new SantExceptionUtil(getDSConnection(), 0, this.getValuationDatetime(), getDatetime(),
                this.getUndoDatetime());

        BufferedReader inputFileStream = null; // Input stream to read the CSV
        // file.;
        String line = null;
        boolean numLinesIsOK = false;
        boolean dateIsOK = false;
        boolean controlLine = false;

        try {
            inputFileStream = new BufferedReader(new FileReader(file));
            for (int i = 0; (line = inputFileStream.readLine()) != null; i++) {
                if (line.startsWith("*****")) {
                    controlLine = true;
                    if (i == Integer.parseInt(line.substring(5, 13))) {
                        numLinesIsOK = true;
                    }
                    if (file.contains(line.substring(13, 21))) {
                        dateIsOK = true;
                    }
                }
            }
            inputFileStream.close();
            if (!controlLine) {
                throw new Exception("No line control in the file.");
            }
            if (!numLinesIsOK) {
                throw new Exception("Error in the number of lines.");
            }
            if (!dateIsOK) {
                throw new Exception("Error in date of the file.");
            }

        } catch (final FileNotFoundException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + file, e);
            throw e;
        } catch (final IOException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + file, e);
            throw e;
        }

        return numLinesIsOK && dateIsOK;
    }

    /**
     * This should be called by all Feed processing scheduled tasks for post processing. If the feed has been processed
     * successfully the file will be moved to OK folder.
     *
     * @param isProcessedSuccessfully
     * @throws Exception
     */
    public void feedPostProcess(final boolean isProcessedSuccessfully) throws Exception {
        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        final String filePath = getAttribute(FILEPATH);
        final String fileName = getFileName();
        final String inputFileName = filePath + File.separator + fileName;

        // If there are no errors, move the file to ok folder
        if (isProcessedSuccessfully) {
            Log.info(this, "Feed has been processed successfully. No bad entries found.");
            final String outputFileName = filePath + OK + File.separator + fileName + "_" + time;
            try {
                FileUtility.moveFile(inputFileName, outputFileName);
            } catch (final IOException e) {
                Log.error(this, e.getMessage(), e);
                throw e;
            }
        } else {
            // There are some bad entries so move the file to fail folder
            // And also create a file with bad entries
            Log.info(this, "Failed to process the file.");
            final String badFileName = filePath + FAIL + File.separator + fileName + "_" + time;
            try {
                FileUtility.moveFile(inputFileName, badFileName);
            } catch (final IOException e) {
                Log.error(this, e.getMessage(), e);
                throw e;
            }

            // Create a file with only bad entries
            if (hasBadEntries()) {
                Log.info(this, "There are some bad entries in the feed so creating .bad file.");
                final String badEntriesFileName = filePath + FAIL + File.separator + fileName + ".bad_" + time;
                createBdEntriesFile(badEntriesFileName, true);
            }
        }

        // We save all the exceptions (if any) into the Task Station Window.
        if (this.santExUtil != null) {
            this.santExUtil.publishTasks();
        }
    }

    public void feedPostProcessForTwoFiles(File file1, File file2, String path, LogGeneric logGen) throws Exception {

        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        if (file1 != null) {
            postProcessFile(file1, path, logGen);
            // Create a file with only bad entries
            if (hasBadEntries()) {
                Log.info(this, "There are some bad entries in the feed so creating .bad file.");
                final String badEntriesFileName = path + FAIL + File.separator + file1.getName() + ".bad_" + time;
                createBdEntriesFile(badEntriesFileName, true);
            }
        }

        if (file2 != null) {
            postProcessFile(file2, path, logGen);
            if (hasBadEntries2()) {
                Log.info(this, "There are some bad entries in the feed 2 so creating .bad file.");
                final String badEntriesFileName2 = path + FAIL + File.separator + file2.getName() + ".bad_" + time;
                createBdEntriesFile2(badEntriesFileName2, true);
            }

        }

        // We save all the exceptions (if any) into the Task Station Window.
        // this.santExUtil.publishTasks();
    }

    public void postProcessFile(File file, String path, LogGeneric logGen) throws Exception {

        // get time
        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        // get input file name
        String inputFileName = path + File.separator + file.getName();

        // move file
        if ((logGen.getRecordErrors() > 0) || (logGen.getRecordWarning() > 0)) {
            // FAIL
            String outputFileName = path + "fail" + File.separator + file.getName() + "_" + time;
            FileUtility.moveFile(inputFileName, outputFileName);
        } else {
            // OK
            String outputFileName = path + "ok" + File.separator + file.getName() + "_" + time;
            FileUtility.moveFile(inputFileName, outputFileName);
        }
    }

    /**
     * This should be called by all Feed processing scheduled tasks for post processing. If the feed has been processed
     * successfully the file will be moved to OK folder. if there are any bad entries then it moves the file to Fail
     * folder and creates a file with all the bad entries and logs all the lines with either error message or Exception
     * stack trace.
     * <p>
     * You need to call this method when you called addbadLines method.
     *
     * @throws Exception
     */
    public void feedPostProcess() throws Exception {
        feedPostProcess(!hasBadEntries());
    }

    /**
     * Creates a file with bad entries and logs the line with error message/stacktrace
     *
     * @param badEntriesFileName
     * @param logErrormsgs
     * @param isTodayBadDate     : true if control line date must be valDate, false if control line date must be valDate-1
     * @throws IOException
     */
    private void createBdEntriesFile(final String badEntriesFileName, final boolean logErrormsgs) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(badEntriesFileName));
            for (final BadFeedEntry entry : this.badEntries) {
                bufferedWriter.write(entry.getLine());
                bufferedWriter.newLine();
                if (logErrormsgs) {
                    Log.info(this, entry.getLine());
                    if (entry.getMessage() != null) {
                        Log.info(this, entry.getMessage());
                    } else {
                        Log.info(this, entry.getException());
                    }
                }
            }
            // *****+8 digit no of lines(Padded by Zeros)+ YYYYMMDD

            bufferedWriter.write("*****" + String.format("%08d", this.badEntries.size())
                    + new SimpleDateFormat("yyyyMMdd").format(new Date()));

        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }

    }

    private void createBdEntriesFile2(final String badEntriesFileName, final boolean logErrormsgs) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(badEntriesFileName));
            for (final BadFeedEntry entry : this.badEntries2) {
                bufferedWriter.write(entry.getLine());
                bufferedWriter.newLine();
                if (logErrormsgs) {
                    Log.info(this, entry.getLine());
                    if (entry.getMessage() != null) {
                        Log.info(this, entry.getMessage());
                    } else {
                        Log.info(this, entry.getException());
                    }
                }
            }
            // *****+8 digit no of lines(Padded by Zeros)+ YYYYMMDD

            bufferedWriter.write("*****" + String.format("%08d", this.badEntries2.size())
                    + new SimpleDateFormat("yyyyMMdd").format(new Date()));

        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }

    }

    /**
     * Method that retrieve (if any) the bad entries.
     *
     * @return TRUE if are bad entries, else FALSE.
     */
    public boolean hasBadEntries() {
        return (this.badEntries.size() > 0) ? true : false;
    }

    public boolean hasBadEntries2() {
        return (this.badEntries2.size() > 0) ? true : false;
    }

    /**
     * To add a bad line into the array, to save after in the log file.
     *
     * @param line Line that is incorrect.
     * @param msg  Message to specified the particular error.
     */
    public void addBadLine(final String line, final String msg) {
        final BadFeedEntry badFeedEntry = new BadFeedEntry(line, msg);
        this.badEntries.add(badFeedEntry);
    }

    public void addBadLine2(final String line, final String msg) {
        final BadFeedEntry badFeedEntry = new BadFeedEntry(line, msg);
        this.badEntries2.add(badFeedEntry);
    }

    /**
     * To add a bad line into the array, to save after in the log file.
     *
     * @param line      Line that is incorrect.
     * @param throwable Exception to specified the particular error.
     */
    public void addBadLine(final String line, final Throwable throwable) {
        final BadFeedEntry badFeedEntry = new BadFeedEntry(line, throwable);
        this.badEntries.add(badFeedEntry);
    }

    public void addBadLine2(final String line, final Throwable throwable) {
        final BadFeedEntry badFeedEntry = new BadFeedEntry(line, throwable);
        this.badEntries2.add(badFeedEntry);
    }

    //v14 Migration
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(FILEPATH));
        attributeList.add(attribute(STARTFILENAME));

        return attributeList;
    }

//	@SuppressWarnings("rawtypes")
//	@Override
//	public Vector getDomainAttributes() {
//		final Vector<String> v = new Vector<String>();
//		v.addElement(FILEPATH);
//		v.addElement(STARTFILENAME);
//		return v;
//	}

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        Log.debug(this, "AbstractProcessFeedScheduledTask.isValidInput Start");

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
        Log.debug(this, "AbstractProcessFeedScheduledTask.isValidInput End");

        return retVal;
    }

    /**
     * To add a new Exception produced during an import process.
     *
     * @param exceptionType Type for the exception occurred.
     * @param source        Scheduled Task origin of the Exception throw.
     * @param tradeId       ID for the operation failed.
     * @param message       Message that we want to show in the Task Station.
     */
    public void addExceptionToTask(final SantExceptionType exceptionType, final String source, final long tradeId,
                                   final String message) {
        this.santExUtil.addExceptionToTask(exceptionType, source, tradeId, message);
    }

    /**
     * To add a new Exception produced during an import process.
     *
     * @param exceptionType Type for the exception occurred.
     * @param tradeId       ID for the operation failed.
     * @param message       Message that we want to show in the Task Station.
     * @param t             Exception thrown.
     */
    public void addExceptionToTask(final SantExceptionType exceptionType, final long tradeId, final String message,
                                   final Throwable t) {
        this.santExUtil.addExceptionToTask(exceptionType, tradeId, message, t);
    }

    /**
     * To add a new Exception produced during an import process.
     *
     * @param exceptionType Type for the exception occurred.
     * @param source        Scheduled Task origin of the Exception throw.
     * @param tradeId       ID for the operation failed.
     * @param t             Exception thrown.
     */
    public void addExceptionToTask(final SantExceptionType exceptionType, final String source, final long tradeId,
                                   final Throwable t) {
        this.santExUtil.addExceptionToTask(exceptionType, source, tradeId, t);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getImportRecipientsEmails(DSConnection dsConn, String sourceSystem) {

        List<String> emailRecipients = new ArrayList<String>();
        // String EMAILS_VALUES = "ImportLogRecipients";

        Vector emailsMapping = LocalCache.getDomainValues(dsConn, EMAILS_MAPPING);
        if ((emailsMapping != null) && (emailsMapping.size() > 0)) {
            for (int i = 0; i < emailsMapping.size(); i++) {
                final String domainValue = (String) emailsMapping.get(i);
                if (sourceSystem.equals(domainValue)) {
                    final String domainComment = LocalCache.getDomainValueComment(dsConn, EMAILS_MAPPING, domainValue);
                    if (!Util.isEmpty(domainComment)) {
                        Vector emailsRecs = LocalCache.getDomainValues(dsConn, domainComment);
                        if (!Util.isEmpty(emailsRecs)) {
                            emailRecipients.addAll(emailsRecs);
                        }
                    }
                }
            }
        }
        return emailRecipients;
    }
}

class BadFeedEntry {
    private String line;
    private String message;
    private Throwable exception;

    BadFeedEntry(final String line, final String message) {
        this.line = line;
        this.message = message;
    }

    BadFeedEntry(final String line, final Throwable throwable) {
        this.line = line;
        this.exception = throwable;
    }

    public String getLine() {
        return this.line;
    }

    public void setLine(final String line) {
        this.line = line;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Throwable getException() {
        return this.exception;
    }

    public void setException(final Throwable throwable) {
        this.exception = throwable;
    }

}

package calypsox.tk.util.interfaceImporter;

import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Read
 *
 * @author aela
 */
public class InterfaceLogWriter extends ImportExecutor<TradeImportStatus, TradeImportStatus> {

    private List<TradeImportStatus> writerBuffer = null;
    private TradeImportTracker importTracker = null;
    private int logsBufferSize = 1000;
    public static final String LOG_FILE_SEPARATOR = ";";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    protected FileWriter fwSummaryLog = null;
    protected FileWriter fwDetailedLogs = null;
    protected FileWriter fwWarningLogs = null;
    protected FileWriter fwfullLog = null;
    protected FileWriter badFileWriter = null;
    protected int nbBadEntries = 0;
    private Map<Integer, String> bufferBadEntries = null;
    private static boolean logClosed = false;

    private ArrayList<String> logFiles = null;

    public InterfaceLogWriter(BlockingQueue<TradeImportStatus> inWorkQueue,
                              BlockingQueue<TradeImportStatus> outWorkQueue, ImportContext context) {
        super(inWorkQueue, outWorkQueue, context);
        this.writerBuffer = new ArrayList<TradeImportStatus>(this.logsBufferSize);
        this.importTracker = context.getTradeImportTracker();
        this.bufferBadEntries = new Hashtable<Integer, String>();
    }

    /**
     * Log files initialization
     */
    private void initLogFiles() {
        this.logFiles = this.importTracker.getLogFiles();
        String detailedFileName = getFileNameWithTimeStamp(this.importTracker.getDetailedLogFileName());
        String summaryFileName = getFileNameWithTimeStamp(this.importTracker.getSummaryLogFileName());
        String fullFileName = getFileNameWithTimeStamp(this.importTracker.getFullLogFileName());
        String warningFileName = getFileNameWithTimeStamp(this.importTracker.getWarningLogFileName());
        try {
            this.fwDetailedLogs = initFileWriter(this.fwDetailedLogs, detailedFileName);
            this.fwfullLog = initFileWriter(this.fwfullLog, fullFileName);
            this.fwWarningLogs = initFileWriter(this.fwWarningLogs, warningFileName);
            this.fwSummaryLog = initFileWriter(this.fwSummaryLog, summaryFileName);
            this.fwfullLog.write("PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE\n");
            this.fwDetailedLogs
                    .write("PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE\n");
            //
            Date now = new Date();
            this.fwWarningLogs.write(now.toString());
            this.fwWarningLogs.write("\n");
            this.fwWarningLogs.write("PROCESS: Load of " + this.importTracker.getProcessName() + " from "
                    + this.importTracker.getSourceSystem() + " \n");
            this.fwWarningLogs.write("FILE: " + this.importTracker.getFileBeingImported() + "\n");
            this.fwWarningLogs.write("REQUIRED STATIC DATA \n");
            this.fwWarningLogs.write("SYSTEM;SOURCE SYSTEM;BO_REFERENCE;TYPE;OBJECT; \n");
            //
            this.logFiles.add(detailedFileName);
            this.logFiles.add(fullFileName);
            this.logFiles.add(warningFileName);
            this.logFiles.add(summaryFileName);
        } catch (IOException e) {
            Log.error(this, e);
            throw new RuntimeException("Error creating log files.");
        }
    }

    public InterfaceLogWriter(BlockingQueue<TradeImportStatus> inWorkQueue,
                              BlockingQueue<TradeImportStatus> outWorkQueue, ImportContext context, int logsBufferSize) {
        this(inWorkQueue, outWorkQueue, context);
        this.logsBufferSize = logsBufferSize;
    }

    // GSM: this point!

    /**
     * Call that actually performs the log generation (detailed, full and .bad -warnings-)
     */
    @Override
    public TradeImportStatus execute(TradeImportStatus item) throws Exception {
        // at the creation of this logger not all of the information for logging are present, so delay the
        // initialization till the first log writing
        if (this.fwSummaryLog == null) {
            initLogFiles();
        }
        if (item == null) {
            return null;
        }
        this.writerBuffer.add(item);
        if (this.writerBuffer.size() >= this.logsBufferSize) {
            // call to flush the detailed, full log. Also the ".bad" to be reprocessed (warnings)
            flushLogs(this.writerBuffer);
            this.writerBuffer.clear();

        }
        return null;
    }

    /**
     * Fills the Summary log in this point.
     */
    @Override
    public void finishPendingWork() {

        // GSM: 24/07/2013. Sometimes receives the signal to stop everything but it hasn't finish the log!
        if (pendingLogBufferFinished()) {
            if (!logClosed) {
                closeLogfilesBuffer();
            }
            return;
        }

        try {
            Log.debug(TradeInterfaceUtils.LOG_CATERGORY, getExecutorName() + "***********>end finishing pending work");

            // GSM: fix to synch
            while (!this.context.allThreadsHaveFinished()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            // in case there still some log, this will flush it
            flushLogs(this.writerBuffer);

            // now add the control line on for bad file if it was created
            if (this.nbBadEntries > 0) {

                getBadFileWriter().write(
                        "*****" + String.format("%08d", this.nbBadEntries)
                                + new SimpleDateFormat("yyyyMMdd").format(new Date()));
            }
            // now flush the summary file
            BufferedWriter bw = new BufferedWriter(this.fwSummaryLog);
            Date now = new Date();
            bw.write(now.toString());
            bw.write("\n");
            bw.write("PROCESS: Load of " + this.importTracker.getProcessName() + " from "
                    + this.importTracker.getSourceSystem() + " \n");
            bw.write("FILE: " + this.importTracker.getFileBeingImported() + "\n");
            if (this.importTracker.isThereErrors()) {
                bw.write("STATUS: ERROR \n");
            } else if (this.importTracker.isOnlyWarnings()) {
                bw.write("STATUS: WARNING \n");
            } else {
                bw.write("STATUS: OK \n");
            }
            bw.write("NUMBER OF RECORDS PROCESSED: " + (this.importTracker.getNbRowsToBeImported()) + "\n");
            bw.write("NUMBER OF RECORDS OK: " + this.importTracker.getNbRowsImportedOK() + "\n");
            bw.write("NUMBER OF RECORDS ERRORS: " + this.importTracker.getNbRowsError() + "\n");
            bw.write("NUMBER OF RECORDS WARNINGS: " + this.importTracker.getNbRowsWarning() + "\n");
            bw.write("NUMBER OF RECORDS EXCLUDED: " + this.importTracker.getNbRowsExclude() + "\n");
            bw.write("\n");
            bw.write("NUMBER OF TRADES PROCESSED: " + (this.importTracker.getNbTradesToBeImported()) + "\n");
            bw.write("NUMBER OF TRADES OK: " + this.importTracker.getNbTradesImportedOK() + "\n");
            bw.write("NUMBER OF TRADES ERRORS: " + this.importTracker.getNbTradesError() + "\n");
            bw.write("NUMBER OF TRADES WARNINGS: " + this.importTracker.getNbTradesWarning() + "\n");
            bw.write("NUMBER OF TRADES EXCLUDED: " + this.importTracker.getNbTradesExclude() + "\n");
            bw.write("SEE LOG FOR MORE DETAILS." + "\n");
            bw.close();

        } catch (IOException e) {
            Log.error(this, e);
        } finally {
            this.writerBuffer.clear();
            closeLogfilesBuffer();
            // try {
            // if (this.fwSummaryLog != null) {
            //
            // this.fwSummaryLog.close();
            // }
            // if (this.fwDetailedLogs != null) {
            // this.fwDetailedLogs.close();
            // }
            //
            // if (this.fwWarningLogs != null) {
            // this.fwWarningLogs.close();
            // }
            //
            // if (this.fwfullLog != null) {
            // this.fwfullLog.close();
            // }
            //
            // if (this.badFileWriter != null) {
            // this.badFileWriter.close();
            // }
            // } catch (IOException e) {
            // Log.error(this, e);
            // }

        }
    }

    /**
     * close of the log files fileWriters
     */
    private void closeLogfilesBuffer() {

        logClosed = true;

        try {
            if (this.fwSummaryLog != null) {

                this.fwSummaryLog.close();
            }
            if (this.fwDetailedLogs != null) {
                this.fwDetailedLogs.close();
            }

            if (this.fwWarningLogs != null) {
                this.fwWarningLogs.close();
            }

            if (this.fwfullLog != null) {
                this.fwfullLog.close();
            }

            if (this.badFileWriter != null) {
                this.badFileWriter.close();
            }
        } catch (IOException e) {
            Log.error(this, e);
        }

    }

    /**
     * @return true if the log buffer is empty
     */
    // GSM: 24/07/2013. Sometimes receives the signal to stop everything but it hasn't finish the log!
    private boolean pendingLogBufferFinished() {

        return (this.writerBuffer == null) || this.writerBuffer.isEmpty();
    }

    /**
     * Flushes the detailed and full log. Also the ".bad" to be reprocessed for the warnings
     *
     * @param logsToFlush
     * @throws IOException
     */
    private void flushLogs(List<TradeImportStatus> logsToFlush) throws IOException {
        if (Util.isEmpty(logsToFlush)) {
            return;
        }
        long start = System.currentTimeMillis();
        for (TradeImportStatus error : logsToFlush) {

            if (error == null) { // discard
                continue;
            }
            // generates a line like: PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE
            StringBuffer line = generateLogDetailedLine(error, error.getRowBeingImportedNb());
            flushLine(this.fwDetailedLogs, this.fwfullLog, error, line.toString());
            // if the error line concerns a two leg trade then flush the second line too
            InterfaceTradeBean tradeBean = error.getTradeBean();

            /*
             * Second line for two legs products
             */
            if ((tradeBean != null) && (tradeBean.getLegTwo() != null)
                    && !(error.getErrorType() == TradeImportStatus.EXCLUDED)) {

                // generates a line like: PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE
                StringBuffer line2 = generateLogDetailedLine(error, tradeBean.getLegTwo().getLineNumber());
                // distinguish between detailed and full
                flushLine(this.fwDetailedLogs, this.fwfullLog, error, line2.toString());
            }

        } // END LOOP log LINES

        // flash static data file
        if (this.importTracker.isThereWarnings()) {
            // this.fwWarningLogs = new FileWriter(warningFileName, true);
            // this.logFiles.add(warningFileName);
            for (TradeImportStatus error : logsToFlush) {
                if ((error.getErrorType() == TradeImportStatus.WARNING) && !Util.isEmpty(error.getFieldName())) {
                    StringBuffer line = new StringBuffer("");
                    line.append(this.importTracker.getProcessName());
                    line.append(LOG_FILE_SEPARATOR);
                    line.append(this.importTracker.getSourceSystem());
                    line.append(LOG_FILE_SEPARATOR);
                    line.append(error.getBoReference());
                    line.append(LOG_FILE_SEPARATOR);
                    line.append(error.getFieldName());
                    line.append(LOG_FILE_SEPARATOR);
                    line.append(error.getFieldValue());
                    line.append("\n");
                    this.fwWarningLogs.write(line.toString());
                    // prepare the bad file entries
                    if (error.isOnlyWarning()) {
                        InterfaceTradeBean tradeBean = error.getTradeBean();
                        if (tradeBean != null) {
                            this.bufferBadEntries.put(tradeBean.getLineNumber(), tradeBean.getLineContent());
                            // if the error line concerns a two leg trade then flush the second line too
                            if (tradeBean.getLegTwo() != null) {
                                if (!Util.isEmpty(tradeBean.getLegTwo().getLineContent())) {
                                    this.bufferBadEntries.put(tradeBean.getLegTwo().getLineNumber(), tradeBean
                                            .getLegTwo().getLineContent());
                                }
                            }
                        }
                    }
                }
            }

            if ((this.bufferBadEntries != null) && (this.bufferBadEntries.size() > 0)) {
                for (String line : this.bufferBadEntries.values()) {
                    if (!Util.isEmpty(line)) {
                        getBadFileWriter().write(line + "\n");
                    }
                }
                this.nbBadEntries += this.bufferBadEntries.size();
                this.bufferBadEntries.clear();
            }
        }

        long end = System.currentTimeMillis();
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
                getExecutorName() + "***********>end flusing " + logsToFlush.size() + " rows in " + (end - start));
    }

    /**
     * Generates a full detailed line for the log
     */
    private StringBuffer generateLogDetailedLine(TradeImportStatus error, int lineNb) {

        StringBuffer line = new StringBuffer("");
        line.append(this.importTracker.getProcessName());
        line.append(LOG_FILE_SEPARATOR);
        line.append(this.importTracker.getFileBeingImported());
        line.append(LOG_FILE_SEPARATOR);
        line.append(lineNb);
        line.append(LOG_FILE_SEPARATOR);
        line.append(error.getLineStatus());
        // GSM: 17/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
        line.append(LOG_FILE_SEPARATOR);
        if (error.getErrorType() != TradeImportStatus.EXCLUDED) {
            line.append(error.getTradeId());
        }
        line.append(LOG_FILE_SEPARATOR);
        if (error.getErrorType() != TradeImportStatus.EXCLUDED) {
            line.append(error.getErrorCode());
        }
        line.append(LOG_FILE_SEPARATOR);
        if (error.getErrorType() != TradeImportStatus.EXCLUDED) {
            line.append((Util.isEmpty(error.getErrorMessage()) ? "" : error.getErrorMessage()));
        }
        line.append(LOG_FILE_SEPARATOR);
        // line.append(LOG_FILE_SEPARATOR);
        // line.append(error.getTradeLongId());
        // line.append(LOG_FILE_SEPARATOR);
        // line.append(error.getErrorCode());
        // line.append(LOG_FILE_SEPARATOR);
        // line.append((Util.isEmpty(error.getErrorMessage()) ? "" : error.getErrorMessage()));
        // line.append(LOG_FILE_SEPARATOR);
        line.append((Util.isEmpty(error.getBoReference()) ? "" : error.getBoReference()));
        line.append(LOG_FILE_SEPARATOR);
        // GSM. End trade exclusion log

        if (error.getErrorType() != TradeImportStatus.EXCLUDED) {
            line.append((Util.isEmpty(error.getRowBeingImportedContent()) ? "" : error.getRowBeingImportedContent()));
        }

        line.append("\n");

        return line;
    }

    /**
     * @return the hasToContinue
     */
    @Override
    public boolean getHasToContinue() {
        return this.context.isTradePersistorRunning();
    }

    @Override
    protected void stopProcess() {
        this.context.stopLogWriterProcess();
    }

    /**
     * @param fileName
     * @return a file name with the current date
     */
    public static String getFileNameWithTimeStamp(String fileName) {
        StringBuilder datedFileName = new StringBuilder(fileName);
        Date now = new Date();

        datedFileName.append("_");
        synchronized (timeFormat) {
            datedFileName.append(timeFormat.format(now));
        }
        datedFileName.append(".txt");
        return datedFileName.toString();
    }

    /**
     * @param fwDetailedLogs
     * @param fwfullLog
     * @param error
     * @param line
     * @throws IOException
     */
    private void flushLine(FileWriter fwDetailedLogs, FileWriter fwfullLog, TradeImportStatus error, String line)
            throws IOException {

        // GSM: 15/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
        // if (error.getErrorType() == TradeImportStatus.OK ) { //old
        if ((error.getErrorType() == TradeImportStatus.OK) || (error.getErrorType() == TradeImportStatus.EXCLUDED)) {

            fwfullLog.write(line);

        } else {
            // flush this line only if it's an error or a warning (if
            // it's a warning but there is another error line on the
            // same deal, then flush just the error line)
            if ((error.getErrorType() == TradeImportStatus.WARNING)) {
                // fwDetailedLogs.write(line);
                if (error.isOnlyWarning()) {
                    fwfullLog.write(line);
                }
            } else {
                fwDetailedLogs.write(line);
                fwfullLog.write(line);
            }
        }
    }

    FileWriter initFileWriter(FileWriter fileWriter, String fullFileName) throws IOException {
        if (fileWriter != null) {
            return fileWriter;
        }
        fileWriter = new FileWriter(fullFileName, true);
        return fileWriter;
    }

    private FileWriter getBadFileWriter() throws IOException {

        if (this.badFileWriter != null) {
            return this.badFileWriter;
        }

        this.badFileWriter = initFileWriter(this.badFileWriter, getBadFileName());
        return this.badFileWriter;
    }

    private String getBadFileName() {
        String fileBeingImportedName = this.importTracker.getFileBeingImported();
        String fileBeingImportedPath = this.importTracker.getFileBeingImportedFullPath();

        String badFileName = fileBeingImportedPath + "fail" + File.separator + fileBeingImportedName + ".bad";

        StringBuilder datedFileName = new StringBuilder(badFileName);
        Date now = new Date();

        datedFileName.append("_");
        synchronized (timeFormat) {
            datedFileName.append(timeFormat.format(now));
        }

        return datedFileName.toString();

    }

}
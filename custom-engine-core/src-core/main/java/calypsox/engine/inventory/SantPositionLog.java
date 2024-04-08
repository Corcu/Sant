/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory;

import calypsox.engine.inventory.util.PositionLogHelper;
import calypsox.engine.inventory.util.PositionLogHelper.LineStatus;
import com.calypso.tk.core.Log;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static calypsox.engine.inventory.SantPositionConstants.*;

/**
 * This class has the duty to log the processing result of each message, directly into a file. Each time a message
 * arrives, the file will be opened at the end
 *
 * @author Guillermo Solano
 * @version 1.1, read from attributes file PROPERTIES_FILE_NAME
 */
public class SantPositionLog {

    // public constants
    public static final String PROPERTIES_FILE_NAME = "log.properties.onlinePositions";
    // private constants. Mainly configuration and formatter
    private final static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
    private static String LOG_DIRECTORY;
    private static String LOG_FILE_NAME;
    private static String LOG_FILE_EXTENSION;
    private static Boolean TIMESTAMP_OPTION;
    // properties on the file PROPERTIES_FILE_NAME
    private static String LOG_DIRECTORY_ATT = "directoy";
    private static String FILE_NAME_ATT = "file_name";
    private static String FILE_EXTENSION_ATT = "file_extension";
    private static String TIME_STAMP_ATT = "use_timestamp";

    // class variables
    /**
     * File pointer to the write file (full path)
     */
    private File writeLogfile;
    /**
     * Count the message number
     */
    private Integer messageCount;

    /**
     * Constructor
     */
    public SantPositionLog() {

        this.writeLogfile = null;
        this.messageCount = 1;
        setInitialLogConfiguration();
    }

    // temp
    public final static boolean DEP = true;

    /**
     * Generates all the logging new lines and writes/appends them the the log
     *
     * @param messLogTrack containing all the message processing and raw data
     */
    public void appendProcessMessageLog(final List<PositionLogHelper> messLogTrack) {

        final List<StringBuffer> parserMessageErrors = new ArrayList<StringBuffer>();

        for (PositionLogHelper line : messLogTrack) {

            final String row = line.getRow();
            final SantPositionBean bean = line.getBean();

            final StringBuffer buildLogLine = new StringBuffer();

            if ((bean != null) && (bean.getSentMode() != null)) {

                // append the sending mode
                if (bean.getSentMode().equals(EOD_MODE)) {
                    buildLogLine.append("EOD");
                } else {
                    buildLogLine.append("NORMAL");
                }
                // append separator
                buildLogLine.append(EQUAL_SEPARATOR);
            }

            // append the row data
            buildLogLine.append(row.trim());
            buildLogLine.append(STATUS_SEPARATOR);

            // in case it has parser errors
            if (line.hasParserErrors()) {

                buildLogLine.append(RESPONSE_KO).append(PARSER_ERRORS_SEPARATOR);
                // add all parser errors to the log line
                buildLogLine.append(buildParserErrors(line.getLinesParserStatuses()));

            } else {
                // othercase, OK
                buildLogLine.append(RESPONSE_OK);
                buildLogLine.append(TRADE_ID);
                buildLogLine.append(line.getTradeId());
            }

            // add new line
            buildLogLine.append(RESPONSE_LINE_SEPARATOR);

            // add line to be added to the log file
            parserMessageErrors.add(buildLogLine);
            // warning
            Log.warn(this, buildLogLine.toString());

        } // end for, proccessing of all lines

        // write into new file or append to the existance file
        write(parserMessageErrors);
    }

    /**
     * Opens the file writer on the file system, writes the log and closes the file. If any configuration error occurs,
     * nothing is done
     *
     * @param parserMessageErrors with the list of lines. Each line is a new position
     */
    public void write(List<StringBuffer> parserMessageErrors) {

        // first set & check the configuration.
        buildConfiguration();

        // append the message log to the end of the daily file
        openWriterAppendAndClose(parserMessageErrors);
    }

    // /////////////////////////////////////////////
    // //////// PRIVATE METHODS ///////////////////
    // ///////////////////////////////////////////

    /**
     * Checks the directory and builds the file name
     */
    private void buildConfiguration() {

        // already setted up
        if (this.writeLogfile != null) {
            // if day has change, start new log file
            if (this.writeLogfile.toString().contains(getDateFormatted())) {
                return;
            }
        }

        final boolean dirAccess = logFileDirectoryConfigured();

        if (!dirAccess) {
            Log.error(this, "Online Prices log: not possible to access the directory.");
            return;
        }

        final boolean logFileBuild = LogFileBuild();

        if (!logFileBuild) {
            Log.error(this, "Online Prices log: not possible to create the file.");
            return;
        }
    }

    /**
     * Opens the file and appends the message positions log
     *
     * @param parserMessageErrors withe the positions results (one per line)
     * @return the success or not of the process
     */
    private boolean openWriterAppendAndClose(List<StringBuffer> parserMessageErrors) {

        try {

            final PrintWriter pw = new PrintWriter(new FileWriter(this.writeLogfile, true));

            // write timestamp message
            pw.append(generateMessageHeader());
            // write new positions results
            for (StringBuffer line : parserMessageErrors) {

                pw.append(line);
            }

            // force to write on file
            pw.flush();

            // close the buffer
            pw.close();

        } catch (IOException e) {
            // should not happen
            Log.error(
                    this,
                    "Online Prices log IOException (Filesystem error). Log wasn't able to be written. "
                            + e.getLocalizedMessage());
            return false;
        }

        return true;
    }

    /**
     * @return a String with the number of message processed and the time in which the log of this message has been
     * generated
     */
    private String generateMessageHeader() {

        final StringBuffer sb = new StringBuffer();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        final Calendar cal = Calendar.getInstance();

        sb.append("-----------------------------------------------------------\n");
        sb.append("F.I. ONLINE POSITIONS INTERFACE -").append(" - Message ");
        sb.append(this.messageCount++).append(" received. \n");
        sb.append("Calypso processed time: ").append(format.format(cal.getTime()));
        sb.append("\n");
        sb.append("-----------------------------------------------------------\n");
        return sb.toString();
    }

    /**
     * @return the success or not if the directory is does not exist
     */
    private boolean logFileDirectoryConfigured() {

        // already setted up
        if (this.writeLogfile != null) {
            // if day has change, start new log file
            if (this.writeLogfile.toString().contains(getDateFormatted())) {
                return true;
            }
        }

        final File directory = new File(LOG_DIRECTORY.trim());

        if (!directory.exists()) {

            Log.error(this, "Online Prices log directory " + LOG_DIRECTORY
                    + " does not exist in the system. Check configuration.");
            return false;

        } else if (!directory.isDirectory()) {

            Log.error(this, "Online Prices log directory " + LOG_DIRECTORY
                    + " is not a directory. Check configuration.");
            return false;

        }

        return true;
    }

    /**
     * @return if the write file has beenc created/is accesible
     */
    private boolean LogFileBuild() {

        if (this.writeLogfile != null) {
            // if day has change, start new log file
            if (this.writeLogfile.toString().contains("_" + getDateFormatted())) {
                return true;
            }
        }

        final String fullLogFilePath = buildFullfilePath();

        if (fullLogFilePath.equals(EMPTY)) {
            return false;
        }

        this.writeLogfile = new File(fullLogFilePath);
        return true;
    }

    /**
     * @return true if the file writer full name has been generated properly
     */
    private String buildFullfilePath() {

        if (LOG_DIRECTORY.isEmpty() || LOG_FILE_NAME.isEmpty()) {
            return EMPTY;
        }

        // get directory
        String path = LOG_DIRECTORY.trim();
        if (!path.endsWith("/")) {
            path += "/";
        }

        // timestamp if enable
        String time = "";
        if (TIMESTAMP_OPTION) {
            time = "_" + getDateFormatted();
        }

        // Remove extension if added by error
        String filename = LOG_FILE_NAME.trim();
        String extension = LOG_FILE_EXTENSION.trim();

        // check extension
        if (filename.contains(".")) {

            filename = filename.substring(0, filename.lastIndexOf("."));
            if (extension.isEmpty()) {
                extension = filename.substring(filename.lastIndexOf("."), filename.length());
            }
        }

        if (!filename.contains(".") && extension.isEmpty()) {

            Log.info(this, "Online Prices log extension is incorrect, read parameter is: " + LOG_FILE_EXTENSION
                    + ". Check configuration. Default .txt will be use");
            extension = ".txt";

        }

        // building file (except extension)
        String file = path + filename + time;

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        // added extension
        file += extension;

        return file;
    }

    /**
     * @returns a String with the date formatted
     */
    private synchronized String getDateFormatted() {
        return timeFormat.format(new Date());

    }

    /**
     * Builds the initial configuration (log directory, log name, extension and add or not timestamp to the end of the
     * file name.
     */
    private void setInitialLogConfiguration() {

        if (!readAttributesFromConfigFile()) {
            // if attributes file not found, try hardcored configuration
            Log.error(this, "No properties file " + PROPERTIES_FILE_NAME + " found. Loading default class options.");
            LOG_DIRECTORY = "/calypso_interfaces/gd/logs/";
            LOG_FILE_NAME = "Online_Positions_Full";
            LOG_FILE_EXTENSION = ".txt";
            TIMESTAMP_OPTION = true;
        }
    }

    /**
     * @return true if the properties were setup and read fromt the configuration file
     */
    private boolean readAttributesFromConfigFile() {

        final URL attributesFile = Thread.currentThread().getContextClassLoader().getResource(PROPERTIES_FILE_NAME);
        InputStream inStream = null;

        // GSM: 09/10/2013. Not configuration file on the server side, just default conf
        if (attributesFile == null) {
            Log.info(this, " No Log configuration file found:  " + PROPERTIES_FILE_NAME
                    + ". Using default Log configuration properties.");
            return false;
        }

        try {

            inStream = attributesFile.openStream();

        } catch (IOException e) {
            return false;
        }

        final Properties properties = new Properties();

        if (inStream != null) {
            try {
                properties.load(inStream);
            } catch (final IOException e) {
                Log.error(this, "Can not load properties from: " + PROPERTIES_FILE_NAME);
                return false;
            }
        } else {
            Log.error(this, "Can not find properties file from: " + PROPERTIES_FILE_NAME);
            return false;
        }
        // parse the configuration properties
        return parseProperties(properties);
    }

    /**
     * reads the user and pass (optional). If MainConfig hasn't read the server and port generates error
     */
    private boolean parseProperties(final Properties properties) {

        int count = 0;
        // directory
        String temp = properties.getProperty(LOG_DIRECTORY_ATT);
        if ((temp != null) && !temp.isEmpty()) {
            LOG_DIRECTORY = temp;
            count++;
        } else {
            Log.error(this, "Cannot read property: " + LOG_DIRECTORY_ATT);
        }
        // file name
        temp = properties.getProperty(FILE_NAME_ATT);
        if ((temp != null) && !temp.isEmpty()) {
            LOG_FILE_NAME = temp;
            count++;
        } else {
            Log.error(this, "Cannot read property: " + FILE_NAME_ATT);
        }
        // extension
        temp = properties.getProperty(FILE_EXTENSION_ATT);
        if ((temp != null) && !temp.isEmpty()) {
            if (!temp.startsWith(".")) {
                temp = "." + temp;
            }
            LOG_FILE_EXTENSION = temp;
            count++;
        } else {
            Log.error(this, "Cannot read property: " + FILE_EXTENSION_ATT);
        }
        // timestamp
        temp = properties.getProperty(TIME_STAMP_ATT);
        if ((temp != null) && !temp.isEmpty()) {
            TIMESTAMP_OPTION = temp.equalsIgnoreCase("true") || temp.equalsIgnoreCase("1");
            count++;
        } else {
            Log.error(this, "Cannot read property: " + TIME_STAMP_ATT);
        }

        return count == 4;
    }

    /**
     * @param logHelper has all the lines with the parser errors for log (for each Position Bean)
     * @return A StringBuffer
     */
    private StringBuffer buildParserErrors(final List<LineStatus> linesErrors) {

        final StringBuffer lineErrors = new StringBuffer();
        // add each parser error
        for (LineStatus err : linesErrors) {

            lineErrors.append(err.toString().trim()).append(PARSER_ERRORS_SEPARATOR);
        }
        // remove the last append
        lineErrors.delete(lineErrors.length() - PARSER_ERRORS_SEPARATOR.length(), lineErrors.length());

        return lineErrors;
    }
}

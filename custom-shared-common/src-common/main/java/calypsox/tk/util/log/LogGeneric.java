/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util.log;

import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogGeneric {
    // Files logs and variables for write at this files.
    private File detailedLog;
    private File fullLog;
    private File staticDataLog;
    private FileWriter fw = null;
    private FileWriter fwfullLog = null;
    private FileWriter fwStaticDataLog = null;

    // To format dates.
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // Variables for errors control
    private int record_errors = 0;
    private int record_warning = 0;
    private int error = 0;
    private int warning = 0;
    private int ok = 0;
    private int total = 1;
    private String errorLine = "";
    private String warningLine = "";
    private String okLine = "";

    /**
     * Method to generate the files variables (detailed log, full log and static data log), including the time in the
     * name of each one.
     *
     * @param detailedLog   String identifying the detailed log.
     * @param fullLog       String identifying the full log.
     * @param staticDataLog String identifying the static data log.
     * @param timec         Time to include in the name of the file.
     */
    public void generateFiles(final String detailedLog, final String fullLog, final String staticDataLog,
                              final String time) {
        this.detailedLog = new File(detailedLog + "_" + time + ".txt");
        this.fullLog = new File(fullLog + "_" + time + ".txt");
        this.staticDataLog = new File(staticDataLog + "_" + time + ".txt");
    }

    /**
     * Initialization for the logs files.
     *
     * @param process Process in which we are creating the files.
     * @throws IOException Exception occurred while we are treating the files and writers.
     */
    public void initializeFiles(final String process) throws IOException {
        this.fw = new FileWriter(this.detailedLog.toString(), true);
        this.fwfullLog = new FileWriter(this.fullLog.toString(), true);
        this.fwStaticDataLog = new FileWriter(this.staticDataLog.toString(), true);
        initilizeDetailedLog();
        initilizeFullLog();
        final Date dat = new Date();
        this.fwStaticDataLog.write(dat.toString());
        this.fwStaticDataLog.write("\n");
        this.fwStaticDataLog.write("PROCESS: " + process + "\n");
    }

    /**
     * Include the firsts lines in the Static Data Log.
     *
     * @param file File to insert the lines.
     * @throws IOException Exception occurred during the treatment of the static data log.
     */
    public void initilizeStaticDataLog(final String file, final String id) throws IOException {
        this.fwStaticDataLog.write("FILE: " + file + "\n");
        this.fwStaticDataLog.write("REQUIRED STATIC DATA \n");
        this.fwStaticDataLog.write("SYSTEM;SOURCE SYSTEM;" + id + ";TYPE;OBJECT;\n");
    }

    /**
     * Initializing the detailed log.
     */
    private void initilizeDetailedLog() {
        this.errorLine = "PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE \n";
        feedDetailedLog(0);
        initializeErrorLine();
    }

    /**
     * Initializing the full log.
     */
    private void initilizeFullLog() {
        this.errorLine = "PROCESS;FILE;LINE;STATUS;TRADEID;ERROR_1;ERROR_1_DESC;BO_REFERENCE;STRING_LINE \n";
        feedFullLog(0);
        initializeErrorLine();
    }

    /**
     * Put a value in the ok line, to identify which ones are correct in the file imported.
     *
     * @param source     Source system.
     * @param file       File imported.
     * @param numberOfOk Number of line correct.
     * @param tradeId    Trade ID to include in the log.
     */
    public void setOkLine(final String source, final String file, final int numberOfOk, final String tradeId) {
        this.okLine += source + ";" + file + ";" + numberOfOk + ";OK;" + tradeId + "\n";
    }

    /**
     * Error in which we set the problem with the lines number.
     *
     * @param file   Filename to put in the error trace.
     * @param source Process in which we are executing the import.
     */
    public void setErrorNumberOfLines(final String source, final String file) {
        this.errorLine += source
                + ";"
                + file
                + ";"
                + 0
                + ";ERROR;;3;Number of lines in file does not match with number of lines in control record or date not correct;\n";
    }

    /**
     * Error in which we set the problem with the lines number.
     *
     * @param file   Filename to put in the error trace.
     * @param source Process in which we are executing the import.
     */
    public void setErrorAttributeSTmissing(final String source, final String file, final String message) {
        this.errorLine += source + ";" + file + ";" + 0 + ";ERROR;;3;" + message + ";\n";
    }

    /**
     * Error in which we set the problem with the files number.
     *
     * @param startFileName Filename to put in the error trace.
     * @param source        Process in which we are executing the import.
     */
    public void setErrorNumberOfFiles(final String source, final String startFileName) {
        this.errorLine += source + ";" + startFileName + ";" + 0 + ";ERROR;;1;Expected File Not Found;\n";
    }

    /**
     * Error in which we define that the logs are already in the system, or another reason to fail in the log creation.
     * New error: Error creating log files
     *
     * @param startFileName Filename to put in the error trace.
     * @param source        Process in which we are executing the import.
     */
    public void setErrorCreatingLogFile(final String source, final String startFileName) {
        this.errorLine += source + ";" + startFileName + ";" + 0 + ";ERROR;;11;Reserved for future use;\n";
    }

    /**
     * Method to close all the log files previously created.
     *
     * @throws IOException Error occurred during the treatment of the log files.
     */
    public void closeLogFiles() throws IOException {
        this.fw.close();
        this.fwfullLog.close();
        this.fwStaticDataLog.close();
    }

    /**
     * Error during we are sending the email with the result of the process. New error: Error sending log mail
     *
     * @param startFileName Filename to put in the error trace.
     * @param source        Process in which we are executing the import.
     */
    public void setErrorSentEmail(final String source, final String startFileName) {
        this.errorLine += source + ";" + startFileName + ";" + 0 + ";ERROR;;11;Reserved for future use;\n";
    }

    /**
     * Error during we are moving the source file to another folder (fail or ok). New error: Error moving historic files
     * and creating bad file
     *
     * @param startFileName Filename to put in the error trace.
     * @param source        Process in which we are executing the import.
     */
    public void setErrorMovingFile(final String source, final String startFileName) {
        this.errorLine += source + ";" + startFileName + ";" + 0 + ";ERROR;;11;Reserved for future use;\n";
    }

    /**
     * Initializing variable 'record errors'.
     */
    public void initializeRecordErrors() {
        this.record_errors = 0;
    }

    /**
     * Initializing variable 'record warnings'.
     */
    public void initializeRecordWarning() {
        this.record_warning = 0;
    }

    /**
     * Initializing variable 'error'.
     */
    public void initializeError() {
        this.error = 0;
    }

    /**
     * Initializing variable 'warning'.
     */
    public void initializeWarning() {
        this.warning = 0;
    }

    /**
     * Initializing variable 'ok'.
     */
    public void initializeOK() {
        this.ok = 0;
    }

    /**
     * Initializing variable 'total'.
     */
    public void initializeTotal() {
        this.total = 0;
    }

    /**
     * Sum in one the value for the variable 'record errors'.
     */
    public void incrementRecordErrors() {
        this.record_errors++;
    }

    /**
     * Sum in one the value for the variable 'record warnings'.
     */
    public void incrementRecordWarning() {
        this.record_warning++;
    }

    /**
     * Sum in one the value for the variable 'error'.
     */
    public void incrementError() {
        this.error++;
    }

    /**
     * Sum in one the value for the variable 'warning'.
     */
    public void incrementWarning() {
        this.warning++;
    }

    /**
     * Sum in one the value for the variable 'ok'.
     */
    public void incrementOK() {
        this.ok++;
    }

    /**
     * Sum in one the value for the variable 'total'.
     */
    public void incrementTotal() {
        this.total++;
    }

    /**
     * Initializing string 'error line'.
     */
    public void initializeErrorLine() {
        this.errorLine = "";
    }

    /**
     * Initializing string 'warning line'.
     */
    public void initializeWarningLine() {
        this.warningLine = "";
    }

    /**
     * Initializing string 'ok line'.
     */
    public void initializeOkLine() {
        this.okLine = "";
    }

    /**
     * Error included in the logs when a field is not present in the file to import.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setErrorRequiredFieldNotPresent(final String source, final String filename, final String line,
                                                final String numberOfError, final String fieldError, final String boReference, final String stringLine) {
        this.errorLine += source + ";" + filename + ";" + line + ";ERROR;;" + numberOfError + ";Required field "
                + fieldError + " not present;" + boReference + ";" + stringLine + "\n";
    }

    /**
     * Error included in the logs when a field is not present or not valid in the file to import.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setErrorRequiredFieldNotPresentNotValid(final String source, final String filename, final String line,
                                                        final String numberOfError, final String fieldError, final String boReference, final String stringLine) {
        this.errorLine += source + ";" + filename + ";" + line + ";ERROR;;" + numberOfError + ";Required field "
                + fieldError + " not present or not valid;" + boReference + ";" + stringLine + "\n";
    }

    /**
     * Error included in the logs when a field is not present or not valid in the file to import.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setWarningRequiredFieldNotPresentNotValid(final String source, final String filename,
                                                          final String line, final String numberOfError, final String fieldError, final String boReference,
                                                          final String stringLine) {
        this.warningLine += source + ";" + filename + ";" + line + ";WARNING;;" + numberOfError + ";Required field "
                + fieldError + " not present or not valid;" + boReference + ";" + stringLine + "\n";
    }

    /**
     * Error included in the logs when a field is not present or not valid in the file to import. New error: Rating type
     * not present
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setErrorRatingTypeNotValid(final String source, final String filename, final String line,
                                           final String numberOfError, final String fieldError, final String boReference, final String stringLine) {
        this.errorLine += source + ";" + filename + ";" + line + ";ERROR;;" + numberOfError + ";Required field "
                + fieldError + " not present or not valid;" + boReference + ";" + stringLine + "\n";
    }

    /**
     * Warning included in the logs when a field is not present in the file to import.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param data
     */
    public void setWarningRequiredFieldNotPresent(final String source, final String filename, final String line,
                                                  final String numberOfError, final String error, final String fieldError, final String boReference,
                                                  final String data) {
        this.warningLine += source + ";" + filename + ";" + line + ";WARNING;;" + numberOfError + ";Required field "
                + error + " : " + fieldError + " not present or not valid;" + boReference + ";" + data + ";"
                + fieldError + "\n";
    }

    /**
     * Warning included in the logs when a field is not present or not valid in the file to import.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param data
     */
    public void setWarningRequiredFieldNotPresentNotValid(final String source, final String filename,
                                                          final String line, final String numberOfError, final String error, final String fieldError,
                                                          final String boReference, final String data) {
        this.warningLine += source + ";" + filename + ";" + line + ";WARNING;;" + numberOfError + ";Required field "
                + error + " : " + fieldError + " not present or not valid;" + boReference + ";" + data + "\n";
    }

    /**
     * Error when we save the trade in the system.
     *
     * @param source     Import process in which we have the error saving the trade.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorSavingTrade(final String source, final String fileName, final String numberLine,
                                    final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;4;Cannot save the trade;" + boReference
                + ";" + strLine + "\n";
    }

    /**
     * Error when we try to CANCEL a trade with status CANCELED
     *
     * @param source     Import process in which we have the error.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorCancelingTrade(final String source, final String fileName, final String numberLine,
                                       final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;4;Cannot save the trade: Cannot modify a trade with status CANCELED;" + boReference + ";"
                + strLine + "\n";
    }

    /**
     * Error due to invalid date.
     *
     * @param source     Import process in which we have the error related to the date.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorDateNotValid(final String source, final String fileName, final String numberLine,
                                     final String isin, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;41;Date not present or not valid;"
                + isin + ";" + strLine + "\n";
    }

    /**
     * Error due to invalid date.
     *
     * @param source     Import process in which we have the error related to the date.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorSusiDateNotValid(final String source, final String fileName, final String numberLine,
                                         final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;60;Real Trade Date not valid;"
                + boReference + ";" + strLine + "\n";
    }

    /**
     * Error indicating information incomplete from the source system, in the file to import in Calypso. New error: Not
     * complete information for the trade
     *
     * @param source       Import process in which we have the error.
     * @param fileName     Name of the file imported.
     * @param numberLine   Value identifying the number of error, to inform other supporter teams.
     * @param errorsConcat Errors concatenated in this string passed as a parameter in the method.
     */
    public void setErrorInformationIncomplete(final String source, final String fileName, final String numberLine,
                                              final String errorsConcat, final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;11;Reserved for future use. Calypso error:" + errorsConcat + ";" + boReference + ";"
                + strLine + "\n";
    }

    /**
     * New error: Not correct agency
     *
     * @param source       Import process in which we have the error.
     * @param fileName     Name of the file imported.
     * @param numberLine   Value identifying the number of error, to inform other supporter teams.
     * @param errorsConcat Errors concatenated in this string passed as a parameter in the method.
     */
    public void setErrorAgencyNotValid(final String source, final String fileName, final String numberLine,
                                       final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;53;Agency not valid;" + boReference
                + ";" + strLine + "\n";
    }

    /**
     * Bad format of the file to import in the system.
     *
     * @param source       Import process in which we have the error related to bad format.
     * @param fileName     Name of the file imported.
     * @param numberLine   Value identifying the number of error, to inform other supporter teams.
     * @param errorsConcat Errors concatenated in this string passed as a parameter in the method.
     * @param strLine
     * @param boReference  BackOffice Reference.
     */
    public void setErrorBadRecordFormat(final String source, final String fileName, final String numberLine,
                                        final String errorsConcat, final String strLine, final String boReference) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;5;Bad record format;" + boReference
                + ";" + strLine + "\n";
    }

    /**
     * Include the error in the log files, to see what is happening in use.
     *
     * @param source     Import process in which we have the error opening the file.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorOpeningFile(final String source, final String fileName, final String numberLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;2;Unexpected error opening the file.\n";
    }

    /**
     * To put a generic error in the string for logs.
     *
     * @param strError Generic error to add in the string error variable.
     */
    public void setGenericError(final String strError) {
        this.errorLine += strError;
    }

    /**
     * Error indicating that we have an issue creating PlMark values. New error: Error creating PL Mark
     *
     * @param source   Import process in which we have the error creating the pl mark.
     * @param fileName Name of the file imported.
     */
    public void setErrorCreatingPlMark(final String source, final String fileName, final String strLine,
                                       final String boReference) {
        this.errorLine += source + ";" + fileName + ";" + this.total + ";ERROR;;55;Error creating PL Mark;"
                + boReference + ";" + strLine + "\n";
    }

    /**
     * Error indicating that we have an issue creating PlMark values. New error: Incorrect value for transaction type
     *
     * @param source   Import process in which we have the error creating the pl mark.
     * @param fileName Name of the file imported.
     */
    public void setErrorTransactionType(final String source, final String fileName, final String strLine,
                                        final String boReference) {
        this.errorLine += source + ";" + fileName + ";" + this.total + ";ERROR;;60;Transaction type field not valid;"
                + boReference + ";" + strLine + "\n";
    }

    /**
     * Error indicating that we have an issue creating PlMark values. New error: Incorrect date for last modified
     *
     * @param source   Import process in which we have the error creating the pl mark.
     * @param fileName Name of the file imported.
     */
    public void setErrorLastModified(final String source, final String fileName, final String strLine,
                                     final String boReference) {
        this.errorLine += source + ";" + fileName + ";" + this.total + ";ERROR;;11;Reserved for future use;"
                + boReference + ";" + strLine + "\n";
    }

    /**
     * Error when the transaction does not exist, if we want to get it from the system.
     *
     * @param source         Import process in which we have the error.
     * @param fileName       Name of the file imported.
     * @param numberLine     Value identifying the number of error, to inform other supporter teams.
     * @param boReference    BackOffice Reference.
     * @param boSystem       BackOffice system.
     * @param strLine
     * @param statusReceived
     */
    public void setErrorTransactionNotExist(final String source, final String fileName, final String numberLine,
                                            final String boReference, final String boSystem, final String strLine, final String statusReceived,
                                            String numError) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;" + numError + ";Record "
                + statusReceived + " received but transaction with BO_REFERENCE:" + boReference + " and BO_SYSTEM: "
                + boSystem + " is not in the system;" + boReference + ";" + strLine + ".\n";
    }

    /**
     * Error shown when a field changed.
     *
     * @param source        Import process in which we have the error.
     * @param fileName      Name of the file imported.
     * @param numberLine    Value identifying the number of error, to inform other supporter teams.
     * @param boReference   BackOffice Reference.
     * @param strLine
     * @param fieldReceived Field changed.
     * @param changedFrom   Source of the change.
     */
    public void setErrorFieldChanged(final String source, final String fileName, final String numberLine,
                                     final String boReference, final String strLine, final String fieldReceived, final String changedFrom) {
        this.errorLine += source + ";" + fileName + ";" + numberLine + ";ERROR;;8;Required field " + fieldReceived
                + " not present ? cambiado desde " + changedFrom + ";" + boReference + ";" + strLine + ".\n";
    }

    /**
     * Error when we are trying to insert a new Trade in the system and another previous to this one is currently in
     * Calypso.
     *
     * @param source      Import process in which we have the error.
     * @param fileName    Name of the file imported.
     * @param numberLine  Value identifying the number of error, to inform other supporter teams.
     * @param boReference BackOffice Reference.
     * @param strLine
     */
    public void setErrorTransactionExists(final String source, final String fileName, final String numberLine,
                                          final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;9;Record received is already in the system. It?s not possible create the same transaction;"
                + boReference + ";" + strLine + ".\n";
    }

    // NEW ERRORS

    /**
     * New error: field Custodian contains a LegalEntity without rol AGENT
     *
     * @param source       Import process in which we have the error.
     * @param fileName     Name of the file imported.
     * @param numberLine   Value identifying the number of error, to inform other supporter teams.
     * @param errorsConcat Errors concatenated in this string passed as a parameter in the method.
     */
    public void setWarningCustodianRol(final String source, final String fileName, final String numberLine,
                                       final String boReference, final String strLine) {
        this.warningLine += source + ";" + fileName + ";" + numberLine
                + ";WARNING;;63;Required field CUSTODIAN contains a LegalEntity without role AGENT;" + boReference
                + ";" + strLine + "\n";
    }

    /**
     * Error included in the logs when field ISIN is related to a matured bond
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    // GSM: 28/05/2013. I_066. MaturedBond as Warning
    // public void setErrorMaturedBond(final String source, final String fileName, final String numberLine,
    // final String boReference, final String strLine) {
    // this.errorLine += source + ";" + fileName + ";" + numberLine
    // + ";ERROR;;62;Required field ISIN is related to a matured bond;" + boReference + ";" + strLine + "\n";
    // }
    public void setWarningMaturedBond(final String source, final String fileName, final String numberLine,
                                      final String boReference, final String strLine) {

        this.warningLine += source + ";" + fileName + ";" + numberLine
                + ";WARNING;;62;Required field ISIN is related to a matured bond;" + boReference + ";" + strLine + "\n";
    }

    /**
     * Error included in the logs when field PORTFOLIO matches with more than 1 book in Calypso
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setErrorPortfolioManyReferences(final String source, final String fileName, final String numberLine,
                                                final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;61;Required field PORTFOLIO matches with more than 1 book in Calypso;" + boReference + ";"
                + strLine + "\n";
    }

    /**
     * Error included in the logs when field bond currency and operation currency don't match
     *
     * @param source      Import process in which we have the error.
     * @param filename    Name of the file imported.
     * @param line        Line of the file.
     * @param boReference BackOffice Reference.
     * @param stringLine
     */
    public void setWarningNoMatchCcyBond(final String source, final String fileName, final String numberLine,
                                         final String boReference, final String strLine) {
        this.warningLine += source + ";" + fileName + ";" + numberLine + ";WARNING;;64;Currency related to product: "
                + boReference + " doesn't match with operation currency;" + boReference + ";" + strLine + "\n";
    }

    /**
     * To populate the information in the static data log.
     *
     * @param id     Trade ID to put in the log.
     * @param system Source system.
     * @throws IOException
     */
    public void feedStaticDataLog(final String id, final String system) throws IOException {
        final String[] warningLines = this.warningLine.split("\n");
        for (int i = 0; i < warningLines.length; i++) {
            if (warningLines[i].contains("INSTRUMENT")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String instrument = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";INSTRUMENT;" + instrument
                            + "\n");
                } else {
                    final String instrument = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";INSTRUMENT;" + instrument
                            + "\n");
                }
            } else if (warningLines[i].contains("CURRENCY_PAIR")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String currencyPair = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY PAIR;"
                            + currencyPair + "\n");
                } else {
                    final String currencyPair = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY PAIR;"
                            + currencyPair + "\n");
                }
            } else if (warningLines[i].contains("LEGAL_ENTITY")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String counterparty = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";COUNTERPARTY;"
                            + counterparty + "\n");
                } else {
                    final String counterparty = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";COUNTERPARTY;"
                            + counterparty + "\n");
                }
            } else if (warningLines[i].contains("PORTFOLIO")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String portfolio = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";PORTFOLIO;" + portfolio
                            + "\n");
                } else {
                    if (system.equals("SUSI")) {
                        final String portfLine = warningLin[6];
                        final String portfolio = portfLine.substring(portfLine.indexOf(':') + 2,
                                portfLine.indexOf("not"));
                        this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";PORTFOLIO;"
                                + portfolio + "\n");
                    } else {
                        final String portfolio = warningLin[7];
                        this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";PORTFOLIO;"
                                + portfolio + "\n");
                    }
                }
            } else if (warningLines[i].contains("ISIN")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String isin = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";ISIN;" + isin + "\n");
                } else {
                    // updated for Repo and SecLending
                    if (system.equals("SUSI")) {
                        final String isinLine = warningLin[6];
                        final String isin = isinLine.substring(isinLine.indexOf(':') + 2, isinLine.indexOf(':') + 14);
                        this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";ISIN;" + isin + "\n");
                    } else {
                        final String isin = warningLin[7];
                        this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";ISIN;" + isin + "\n");
                    }
                }
            } else if (warningLines[i].contains("REF_INTERNA")) {
                final String[] warningLin = warningLines[i].split(";");
                String refInterna = warningLin[7];
                this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";REF_INTERNA;" + refInterna
                        + "\n");
            } else if (warningLines[i].contains("CURRENCY")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String currency = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY;" + currency
                            + "\n");
                } else {
                    final String currency = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY;" + currency
                            + "\n");
                }
            } else if (warningLines[i].contains("The type")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String type = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";TYPE;" + type + "\n");
                } else {
                    final String type = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";TYPE;" + type + "\n");
                }
            } else if (warningLines[i].contains("ISIN and its")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String currencyForQuote = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY FOR QUOTE;"
                            + currencyForQuote + "\n");
                } else {
                    final String currencyForQuote = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";CURRENCY FOR QUOTE;"
                            + currencyForQuote + "\n");
                }
            } else if (warningLines[i].contains("INDEX")) {
                final String[] warningLin = warningLines[i].split(";");
                if (warningLin.length > 9) {
                    final String index = warningLin[9];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";INDEX;" + index + "\n");
                } else {
                    final String index = warningLin[7];
                    this.fwStaticDataLog.write("CALYPSO_COLATERALES;" + system + ";" + id + ";INDEX;" + index + "\n");
                }
            }
        }
    }

    /**
     * Method to log the process information before the load.
     *
     * @param tradeID Trade ID to put in the log.
     */
    public void feedFullLog(final long tradeID) {
        if (!this.errorLine.equals("")) {
            try {
                this.fwfullLog.write(this.errorLine);
            } catch (final IOException e) {
                Log.error(this, e); //sonar
            }
        }

        if (!this.warningLine.equals("") && this.errorLine.equals("")) {
            final String[] warningLines = this.warningLine.split("\n");
            for (int i = 0; i < warningLines.length; i++) {
                final String[] warningLin = warningLines[i].split(";");
                final String warningLin_1 = warningLin[0] + ";" + warningLin[1] + ";" + warningLin[2] + ";"
                        + warningLin[3] + ";";
                final String warningLin_2 = ";" + warningLin[5] + ";" + warningLin[6] + ";" + warningLin[7] + ";"
                        + warningLin[8] + "\n";
                final String warningL = warningLin_1 + tradeID + warningLin_2;
                try {
                    this.fwfullLog.write(warningL);
                } catch (final IOException e) {
                    Log.error(this, e); //sonar
                }
            }
        }

        if (!this.okLine.equals("") && this.warningLine.equals("") && this.errorLine.equals("")) {
            try {
                this.fwfullLog.write(this.okLine);
            } catch (final IOException e) {
                Log.error(this, e); //sonar
            }
        }
    }

    /**
     * Method to log the process information before the load.
     *
     * @param counters
     * @throws Exception Any Exception occurred during the pre process.
     */
    public void feedDetailedLog(final long tradeID) {
        if (!this.errorLine.equals("")) {
            try {
                this.fw.write(this.errorLine);
            } catch (final IOException e) {
                Log.error(this, e); //sonar
            }
        }

        if (!this.warningLine.equals("") && this.errorLine.equals("")) {
            final String[] warningLines = this.warningLine.split("\n");
            for (int i = 0; i < warningLines.length; i++) {
                final String[] warningLin = warningLines[i].split(";");
                final String warningLin_1 = warningLin[0] + ";" + warningLin[1] + ";" + warningLin[2] + ";"
                        + warningLin[3] + ";";
                final String warningLin_2 = ";" + warningLin[5] + ";" + warningLin[6] + ";" + warningLin[7] + ";"
                        + warningLin[8] + "\n";
                final String warningL = warningLin_1 + tradeID + warningLin_2;
                try {
                    this.fw.write(warningL);
                } catch (final IOException e) {
                    Log.error(this, e); //sonar
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
    public String feedGenericLogProcess(final String file, final String summaryLog, final String load, final int total) {
        try {
            synchronized (this.timeFormat) {
                final Date d = new Date();
                final String time = this.timeFormat.format(d);
                final File fichero = new File(summaryLog + "_" + time + ".txt");

                final BufferedWriter bw = new BufferedWriter(new FileWriter(fichero.toString()));
                final Date dat = new Date();
                bw.write(dat.toString());
                bw.write("\n");
                bw.write("ENV: " + Defaults.getEnvName() + "\n"); // added execution enviroment
                bw.write("PROCESS: " + load + "\n");
                bw.write("FILE: " + file + "\n");
                if (this.record_errors > 0) {
                    bw.write("STATUS: ERROR \n");
                } else if (this.record_warning > 0) {
                    bw.write("STATUS: WARNING \n");
                } else {
                    bw.write("STATUS: OK \n");
                }
                bw.write("NUMBER OF RECORDS PROCESSED: " + (total) + "\n");
                if ((total - this.record_errors - this.record_warning) < 0) {
                    bw.write("NUMBER OF RECORDS OK: " + ("0") + "\n");
                } else {
                    bw.write("NUMBER OF RECORDS OK: " + (total - this.record_errors - this.record_warning) + "\n");
                }

                bw.write("NUMBER OF RECORDS ERRORS: " + this.record_errors + "\n");
                bw.write("NUMBER OF RECORDS WARNINGS: " + this.record_warning + "\n");
                bw.write("SEE LOG FOR MORE DETAILS." + "\n");

                bw.close();
                return fichero.toString();
            }
        } catch (final IOException ioe) {
            Log.error(this, ioe); //sonar
            return "";
        }
    }

    /**
     * To validate if the log files exist or not previous to write on them.
     *
     * @return TRUE if exist the logs, FALSE in the other case.
     */
    public boolean validateFilesExistence() {
        if (this.detailedLog.exists() && this.fullLog.exists() && this.staticDataLog.exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the string with the name of full log.
     *
     * @return The name of full log.
     */
    public String getStringFullLog() {
        return this.fullLog.toString();
    }

    /**
     * Returns the string with the name of detailed log.
     *
     * @return The name of detailed log.
     */
    public String getStringDetailedLog() {
        return this.detailedLog.toString();
    }

    /**
     * Returns the string with the name of static data log.
     *
     * @return The name of static data log.
     */
    public String getStringStaticDataLog() {
        return this.staticDataLog.toString();
    }

    /**
     * Returns the value of the variable error.
     *
     * @return The value for error.
     */
    public int getNumberError() {
        return this.error;
    }

    /**
     * Returns the value of the variable ok.
     *
     * @return The value for ok.
     */
    public int getNumberOk() {
        return this.ok;
    }

    /**
     * Returns the value of the variable warning.
     *
     * @return The value for warning.
     */
    public int getNumberWarning() {
        return this.warning;
    }

    /**
     * Returns the value of the variable total.
     *
     * @return The value for total.
     */
    public int getNumberTotal() {
        return this.total;
    }

    /**
     * Returns the value of the variable record warning.
     *
     * @return The value for record warning.
     */
    public int getRecordWarning() {
        return this.record_warning;
    }

    /**
     * Returns the value of the variable record errors.
     *
     * @return The value for record errors.
     */
    public int getRecordErrors() {
        return this.record_errors;
    }

    // EXTRA
    public void setErrorSavingRating(final String source, final String fileName, final String nLinea,
                                     final String isin, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;4;Cannot save the rating;" + isin + ";"
                + strLine + "\n";
    }

    public void setErrorWritingLog(final String source, final String fileName, final String nLinea) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Error updating log;\n";
    }

    public void setErrorSavingQuote(final String source, final String fileName, final String nLinea, final String isin,
                                    final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;4;Cannot save the quote value;" + isin
                + ";" + strLine + "\n";
    }

    /**
     * New error: Duplicate ISIN in the file
     *
     * @param source   Import process in which we have the error.
     * @param fileName Name of the file imported.
     * @param nLinea   Value identifying the number of error, to inform other supporter teams.
     */
    public void setErrorDuplicatedIsin(final String source, final String fileName, final String nLinea,
                                       final String isin, final String stringLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Reserved for future use;" + isin + ";"
                + stringLine + "\n";
    }

    /**
     * New warning: Duplicate ISIN in the file
     *
     * @param source   Import process in which we have the error.
     * @param fileName Name of the file imported.
     * @param nLinea   Value identifying the number of error, to inform other supporter teams.
     */
    public void setWarningDuplicatedIsin(final String source, final String fileName, final String nLinea,
                                         final String isin, final String stringLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Duplicate ISIN in the file;" + isin + ";"
                + stringLine + "\n";
    }

    /**
     * New error: Type not present
     *
     * @param source   Import process in which we have the error.
     * @param fileName Name of the file imported.
     * @param nLinea   Value identifying the number of error, to inform other supporter teams.
     */
    public void setWarningTypeNotPresent(final String source, final String fileName, final String nLinea,
                                         final String isin, final String stringLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea
                + ";ERROR;;68;Quote Type definition for the bond is not correct;" + isin + ";" + stringLine + "\n";
    }

    /**
     * New error: Conversion isin
     *
     * @param source   Import process in which we have the error.
     * @param fileName Name of the file imported.
     * @param nLinea   Value identifying the number of error, to inform other supporter teams.
     */
    public void setWarningConversionIsin(final String source, final String fileName, final String nLinea,
                                         final String isin, final String stringLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea
                + ";ERROR;;69;Error in the mapping between ISIN y QuoteType;" + isin + ";" + stringLine + "\n";
    }

    /**
     * New error: Currency not valid for ISIN
     *
     * @param source   Import process in which we have the error.
     * @param fileName Name of the file imported.
     * @param nLinea   Value identifying the number of error, to inform other supporter teams.
     */
    public void setWarningDifferentCccy(final String source, final String fileName, final String nLinea,
                                        final String isin, final String stringLine) {
        this.warningLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Reserved for future use;" + isin + ";"
                + stringLine + "\n";
    }

    /* 61;Face value for the bond is zero; */
    public void setErrorFaceValue(final String source, final String fileName, final String nLinea, final String isin,
                                  final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;65;Face Value for Bond with ISIN: " + isin
                + " is zero;" + isin + ";" + strLine + "\n";

    }

    /* ERROR;;62;Kind of trade is not COLLATERAL */
    public void setErrorCollateral(final String source, final String fileName, final String nLinea, final String isin,
                                   final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;66;Kind of trade is COLLATERAL;" + isin
                + ";" + strLine + "\n";

    }

    /* ERROR;;63;LegalEntityAttribute not found */
    public void setErrorNoLEAttribute(final String source, final String fileName, final String nLinea,
                                      final String isin, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Reserved for future use;" + isin + ";"
                + strLine + "\n";

    }

    /* ;ERROR;;64;Multiple Legal Entity Attributes found; */
    public void setErrorMultipleLEAttribute(final String source, final String fileName, final String nLinea,
                                            final String isin, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + nLinea + ";ERROR;;11;Reserved for future use;" + isin + ";"
                + strLine + "\n";

    }

    public void setWarningNoCcy(final String source, final String fileName, final String nLinea, final String ccy,
                                final String strLinea) {
        this.warningLine += source + ";" + fileName + ";" + nLinea + ";WARNING;;39;CURRENCY_PAIR " + ccy
                + " not found;" + ccy + ";" + ".\n";

    }

    public void decrementTotal() {
        this.total--;
    }

    public void setWarningAgentNotValid(final String source, final String fileName, final String nLinea,
                                        final String agent, final String strLinea) {
        this.warningLine += source + ";" + fileName + ";" + nLinea + ";WARNING;;54;LEGAL ENTITY " + agent
                + " not found with role Agent in the system;" + agent + ";" + ".\n";
    }

    /**
     * New error: Error getting bond
     *
     * @param source       Import process in which we have the error.
     * @param fileName     Name of the file imported.
     * @param numberLine   Value identifying the number of error, to inform other supporter teams.
     * @param errorsConcat Errors concatenated in this string passed as a parameter in the method.
     */
    public void setErrorGettingBond(final String source, final String fileName, final String numberLine,
                                    final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;67;Error getting product. Product is not a bond/equity;" + boReference + ";" + strLine
                + "\n";
    }

    /**
     * New error: Error saving StockLending rate
     *
     * @param source      Import process in which we have the error.
     * @param fileName    Name of the file imported.
     * @param numberLine  Value identifying the number of error, to inform other supporter teams.
     * @param boReference Errors concatenated in this string passed as a parameter in the method.
     * @param strLine     Data line
     */
    public void setErrorSavingStockLendingRate(final String source, final String fileName, final String numberLine,
                                               final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;72;Error saving rate in product custom data;" + boReference + ";" + strLine + "\n";
    }

    // BAU 5.2.0 - New warning, book LE is different from PO.

    /**
     * Warning included in the logs when processing org is different from book le.
     *
     * @param source        Import process in which we have the error.
     * @param filename      Name of the file imported.
     * @param line          Line of the file.
     * @param numberOfError Value identifying the number of error, to inform other supporter teams.
     * @param fieldError    Field in which we have the error.
     * @param boReference   BackOffice Reference.
     * @param stringLine
     */
    public void setWarningDifferentLegalEntities(final String source, final String filename, final String line,
                                                 final String numberOfError, final String fieldError, final String boReference, final String stringLine) {
        this.warningLine += source + ";" + filename + ";" + line + ";WARNING;;" + numberOfError + ";" + fieldError
                + " is different from book LE;" + boReference + ";" + stringLine + "\n";
    }


    /**
     * New error: Error getting product
     *
     * @param source     Import process in which we have the error.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     * @param boReference       The back office reference
     * @param strLine    Current file line
     */
    public void setErrorGettingProduct(final String source, final String fileName, final String numberLine,
                                       final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;;Error getting product. Product is NULL or there is more than " +
                "one product and it does not meet the filtering criteria by CCY;" + boReference + ";" + strLine
                + "\n";
    }

    /**
     * New error: Error getting legal entity by code
     *
     * @param source     Import process in which we have the error.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     * @param boReference       The back office reference
     * @param strLine    Current file line
     */
    public void setErrorGettingLegalEntity(final String source, final String fileName, final String numberLine,
                                       final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;;Error getting legal entity. Legal entity is NULL;" + boReference + ";" + strLine
                + "\n";
    }

    /**
     * New error: Error save custom percentage leverage
     *
     * @param source     Import process in which we have the error.
     * @param fileName   Name of the file imported.
     * @param numberLine Value identifying the number of error, to inform other supporter teams.
     * @param boReference       The back office reference
     * @param strLine    Current file line
     */
    public void setErrorSaveCustomPercentageLeverage(final String source, final String fileName, final String numberLine,
                                           final String boReference, final String strLine) {
        this.errorLine += source + ";" + fileName + ";" + numberLine
                + ";ERROR;;;Error save custom percentage leverage;" + boReference + ";" + strLine
                + "\n";
    }


}
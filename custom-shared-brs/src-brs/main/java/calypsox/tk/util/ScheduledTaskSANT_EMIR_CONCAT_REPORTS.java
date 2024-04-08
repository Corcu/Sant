/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * ScheduledTaskSANT_EMIR_CONCAT_REPORTS takes snapshot report and concat it.
 *
 * @author Philippe Morange
 *
 */
public class ScheduledTaskSANT_EMIR_CONCAT_REPORTS extends SantScheduledTask {
  private static final long serialVersionUID = -2543684866102808948L;

  private static final String TASK_INFORMATION = "This scheduled task concat the input files, Dodd-Frank Batch reports Valuation and Snapshot in a unique output file with a md5 extension";

  private static final String INPUT_REPORT_FILEPATH = "Input File Path";
  private static final String VALUATION_REPORT_FILENAME = "Valuation File Name";
  private static final String SNAPSHOT_REPORT_FILENAME = "Snapshot File Name";
  private static final String FILEFORMATINPUT = "Input File Format";
  private static final String OUTPUT_FILEPATH = "Output file Path";
  private static final String OUTPUT_FILENAME = "Output File Name";
  private static final String FILEFORMATOUTPUT = "Output File Format";

  /**
   * Returns task information.
   *
   * @return task information.
   */
  @Override
  public String getTaskInformation() {
    return TASK_INFORMATION;
  }

  /**
   * Return the domain attributes.
   *
   * @return all domain attributes.
   */
  // @Override
  // public Vector<String> getDomainAttributes() {
  // final Vector<String> attr = new Vector<String>();
  // attr.add(INPUT_REPORT_FILEPATH);
  // attr.add(VALUATION_REPORT_FILENAME);
  // attr.add(SNAPSHOT_REPORT_FILENAME);
  // attr.add(FILEFORMATINPUT);
  // attr.add(OUTPUT_FILEPATH);
  // attr.add(OUTPUT_FILENAME);
  // attr.add(FILEFORMATOUTPUT);
  // return attr;
  // }

  @Override
  protected List<AttributeDefinition> buildAttributeDefinition() {
    final List<AttributeDefinition> attrDefList = new ArrayList<>();

    final AttributeDefinition attrInputReportFilePath = attribute(INPUT_REPORT_FILEPATH).mandatory();
    final AttributeDefinition attrValuationRepFileName = attribute(VALUATION_REPORT_FILENAME).mandatory();
    final AttributeDefinition attrSnapshotReportFileName = attribute(SNAPSHOT_REPORT_FILENAME).mandatory();
    final AttributeDefinition attrFileFormatInput = attribute(FILEFORMATINPUT).mandatory();
    final AttributeDefinition attrOutFilePath = attribute(OUTPUT_FILEPATH).mandatory();
    final AttributeDefinition attrOutFileName = attribute(OUTPUT_FILENAME).mandatory();
    final AttributeDefinition attrFileFormatOut = attribute(FILEFORMATOUTPUT).mandatory();

    attrDefList.add(attrInputReportFilePath);
    attrDefList.add(attrValuationRepFileName);
    attrDefList.add(attrSnapshotReportFileName);
    attrDefList.add(attrFileFormatInput);
    attrDefList.add(attrOutFilePath);
    attrDefList.add(attrOutFileName);
    attrDefList.add(attrFileFormatOut);

    return attrDefList;
  }

  /**
   * Checks if the input is valid.
   *
   * @param errorsP
   *            Vector containing all error.
   * @return True if is valid, false if not.
   */
  @Override
  public boolean isValidInput(
          @SuppressWarnings("rawtypes") final Vector errorsP) {
    @SuppressWarnings("unchecked")
    final Vector<String> errors = errorsP;

    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.isValidInput Starts");

    // Check INPUT_REPORT_FILEPATH
    final String inputReportFilePath = getAttribute(INPUT_REPORT_FILEPATH);

    if (Util.isEmpty(inputReportFilePath)) {
      errors.addElement("Snapshot Report FilePath is not specified");
    }

    // Check VALUATION_REPORT_FILENAME
    final String valuationFileName = getAttribute(VALUATION_REPORT_FILENAME);
    if (Util.isEmpty(valuationFileName)) {
      errors.addElement("Valuation File Name is not specified");
    }

    // Check SNAPSHOT_REPORT_FILENAME
    final String snapshotFileName = getAttribute(SNAPSHOT_REPORT_FILENAME);
    if (Util.isEmpty(snapshotFileName)) {
      errors.addElement("Snapshot File Name is not specified");
    }

    // Check FILEEXTENSIONINPUT
    final String inputFileExtension = getAttribute(FILEFORMATINPUT);
    if (Util.isEmpty(inputFileExtension)) {
      errors.addElement("Input File Extension is not specified");
    }

    // Check OUTPUT_REPORT_FILEPATH
    final String outputFilePath = getAttribute(OUTPUT_FILEPATH);

    if (Util.isEmpty(outputFilePath)) {
      errors.addElement("Output Report FilePath is not specified");
    }

    // Check SNAPSHOT_REPORT_FILENAME
    final String outputFileName = getAttribute(OUTPUT_FILENAME);
    if (Util.isEmpty(outputFileName)) {
      errors.addElement("Output File Name is not specified");
    }

    // Check FILEEXTENSIONINPUT
    final String outputFileExtension = getAttribute(FILEFORMATOUTPUT);
    if (Util.isEmpty(outputFileExtension)) {
      errors.addElement("Output File Extension is not specified");
    }

    Log.debug(this, "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.isValidInput Ends");

    return errors.size() == 0;
  }

  /**
   * ScheduledTask process method. Executes the Scheduled Task.
   *
   * @param ds
   *            DSConnection.
   * @param ps
   *            PSConnection.
   * @return True if properly processed, false if not.
   */
  @Override
  public boolean process(final DSConnection ds, final PSConnection ps) {

    exceptions = new SantExceptions();

    Log.debug(this, "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process Starts");

    boolean bReturn = true;

    // Just for local Testing
    // setUpLocalTesting();

    final String valuationURLFile = getCompleteFileName(
            getAttribute(INPUT_REPORT_FILEPATH),
            getAttribute(VALUATION_REPORT_FILENAME), false, false);
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process URLFile to get File="
                    + valuationURLFile);

    final File valuationFile = new File(valuationURLFile);

    bReturn = isExistingFile(valuationFile);

    if (!bReturn) {
      return bReturn;
    }

    final String snapshotURLFile = getCompleteFileName(
            getAttribute(INPUT_REPORT_FILEPATH),
            getAttribute(SNAPSHOT_REPORT_FILENAME), false, false);
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process URLFile to get File="
                    + snapshotURLFile);

    final File snapshotFile = new File(snapshotURLFile);

    bReturn = isExistingFile(snapshotFile);

    if (!bReturn) {
      return bReturn;
    }

    final String outputURLFile = getCompleteFileName(
            getAttribute(OUTPUT_FILEPATH), getAttribute(OUTPUT_FILENAME),
            true, false);
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process URLFile to get File="
                    + snapshotURLFile);

    final File outputFile = new File(outputURLFile);

    bReturn = concatenateInputFiles(valuationFile, snapshotFile, outputFile);

    Log.debug(this, "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process Ends");

    return bReturn;
  }

  private boolean isExistingFile(final File file) {
    if (!file.exists()) {

      Log.debug(
              this,
              "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: File "
                      + file.getName() + " does not exist in the path  "
                      + file.getPath());
      return false;
    }

    return true;
  }

  /**
   * This method concatenate the valuation and snapshot files into outputFile
   * If the whole process was succesful the input files are deleted from the
   * directory.
   *
   * @param valuationFile
   *            Input File.
   * @param snapshotFile
   *            Input File.
   * @param outputFile
   *            Result of the concatenation.
   * @return True or false whether the concatenation and the md5 creation were
   *         succesfully created.
   */
  private boolean concatenateInputFiles(final File valuationFile,
                                        final File snapshotFile, final File outputFile) {

    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.concatenateInputFiles Starts");

    boolean bReturn = false;
    boolean foundFile1 = false;
    boolean foundFile2 = false;

    FileWriter fileWriter = null;
    BufferedWriter bufferedWriter = null;

    try {
      fileWriter = new FileWriter(outputFile);
      bufferedWriter = new BufferedWriter(fileWriter);

      /*
       * Process the valuation and snapshot reports, writing in the output
       * file
       */
      foundFile1 = processInputFile(valuationFile, bufferedWriter);

      foundFile2 = processInputFile(snapshotFile, bufferedWriter);

      /* Creation of md5 file */
      handleMD5file(outputFile.getPath());

      Log.debug(this,
              "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.process End returning "
                      + bReturn);

      // Save the tasks
      exceptions.publishTasks(getDSConnection(), 0, null);

    } catch (final IOException e1) {
      publishError(ErrorCodeEnum.UndefinedException, new String[] {
                      e1.getClass().getSimpleName(), e1.getMessage() },
              SantExceptionType.TECHNICAL_EXCEPTION, true);
      bReturn = false;
      Log.error(this, e1);
    } finally {
      try {
        if (bufferedWriter != null) {
          bufferedWriter.close();
        }

        /*
         * Delete the incoming files valuation and snapshot leaving only
         * the output file and checksum.md5
         */
        if (foundFile1 && foundFile2) {
          bReturn = valuationFile.delete();
          bReturn &= snapshotFile.delete();
          if (!bReturn) {
            Log.error(this,
                    "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error deleting the report files.");
          }
        }

        bReturn = true;
      } catch (final IOException e) {
        Log.error(
                this,
                "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error closing the bufferedWriter",
                e);
      }
    }

    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.concatenateInputFiles Ends");

    return bReturn;

  }

  /**
   * This operation creates the md5 code using method digest and calls the
   * method createChecksumFile.
   *
   * @param String
   *            The URL of the file that we want to create the md5 file.
   *
   * @return True or false whether the md5 sequence was succesfully created.
   */
  private boolean handleMD5file(final String outputURLFile) {
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.handleMD5file Starts");
    boolean bReturn = false;
    MessageDigest md;
    InputStream is = null;
    try {
      is = new FileInputStream(outputURLFile);

      final byte[] buffer = new byte[1024];
      md = MessageDigest.getInstance("MD5");
      int numRead;

      do {
        numRead = is.read(buffer);
        if (numRead > 0) {
          md.update(buffer, 0, numRead);
        }
      } while (numRead != -1);

      final byte[] digest = md.digest();

      final String mdFileName = getCompleteFileName(
              getAttribute(OUTPUT_FILEPATH), "DTCCGTR#", true, true);

      final String digestHex = getMD5Checksum(digest);
      bReturn = createChecksumFile(digestHex, mdFileName);

    } catch (final NoSuchAlgorithmException e2) {
      Log.error(
              this,
              "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error to getInstance(MD5) in MessigeDigest", e2);
    } catch (final FileNotFoundException e1) {
      Log.error(this,
              "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Input file not found in path "
                      + outputURLFile, e1);
    } catch (final IOException e) {
      Log.error(this,
              "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Input Data was not found", e);
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (final IOException e) {
        Log.error(this,
                "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error closing InputStream", e);
      }
    }

    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.handleMD5file Ends");
    return bReturn;

  }

  /**
   * see this How-to for a faster way to convert a byte array to a HEX string
   *
   * @param sequence
   *            sequence
   * @return MD5 checksum.
   */
  public static String getMD5Checksum(final byte[] sequence) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < sequence.length; i++) {
      result.append(Integer.toString((sequence[i] & 0xff) + 0x100, 16)
              .substring(1));
    }
    return result.toString();
  }

  /**
   * This operation creates the checksume file. The input sequence
   *
   * @param bs
   *            MD5 Sequence
   *
   * @param url
   *            Path of the file source needed to the sequence creation.
   *
   * @return True if the file was created.
   */
  private boolean createChecksumFile(final String bs, final String url) {
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.createChecksumFile Starts");
    boolean rst = false;
    if (bs != null) {
      FileWriter fw = null;
      BufferedWriter bw = null;

      final StringBuilder line = new StringBuilder();
      line.append("MD5");
      line.append('(');
      line.append(url);
      line.append(')');
      line.append("= ");
      line.append(bs);

      try {
        fw = new FileWriter(url);
        bw = new BufferedWriter(fw);

        bw.write(line.toString());
        bw.flush();

        rst = true;
      } catch (final IOException e) {
        Log.error(this,
                "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error with BufferedWriter", e);
        rst = false;
      } finally {
        try {
          if (bw != null) {
            bw.close();
          }
        } catch (final IOException e) {
          Log.error(this,
                  "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error closing BufferedWriter", e);
        }
      }

    }
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.createChecksumFile Ends");
    return rst;
  }

  /**
   * This method read every line of the input file and write them into the new
   * file. After every line it is necessary to insert a new line (/n)
   *
   * @param inputFile
   *            Source of the data for the new file.
   * @param bufferedWriter
   *            Writer of the new file.
   * @return True True or false whether the concatenation was succesfully
   *         created.
   */
  private boolean processInputFile(final File inputFile,
                                   final BufferedWriter bufferedWriter) {

    boolean foundFile = false;
    FileReader fileReader = null;
    BufferedReader bufferedReader = null;

    try {
      fileReader = new FileReader(inputFile);
      bufferedReader = new BufferedReader(fileReader);
      foundFile = true;

      String line = null;

      while ((line = bufferedReader.readLine()) != null) {
        Log.debug(this,
                "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.processInputFile Reading line ="
                        + line);

        if (Util.isEmpty(line) || "No Records".equalsIgnoreCase(line)) {
          continue;
        }

        bufferedWriter.write(line);
        bufferedWriter.flush();
        bufferedWriter.newLine();
        bufferedWriter.flush();
      }

    } catch (final FileNotFoundException fnf) {
      Log.error(this, fnf);
      publishError(ErrorCodeEnum.InputFileNotFound,
              new String[] { inputFile.getPath() },
              SantExceptionType.TECHNICAL_EXCEPTION, true);
      return false;
    } catch (final Exception ex) {
      Log.error(this, ex);
      publishError(ErrorCodeEnum.UndefinedException, new String[] {
                      ex.getClass().getSimpleName(), ex.getMessage() },
              SantExceptionType.TECHNICAL_EXCEPTION, true);
      return false;
    } finally {
      try {

        if (bufferedReader != null) {
          bufferedReader.close();
        }
      } catch (final IOException e) {
        Log.error(this,
                "ScheduledTaskSANT_EMIR_CONCAT_REPORTS: Error closing BufferedReader", e);
      }
    }

    return foundFile;
  }

  /**
   * Get the complete File Name
   *
   * @param path
   *            Path of the file.
   * @param name
   *            Name of the file.
   * @param output
   *            Indicates if it is input or output file.
   * @param md5
   *            md5 format.
   * @return The complete File Name including the date (yyyymmdd) and the
   *         specific format.
   */
  private String getCompleteFileName(final String path, final String name,
                                     final boolean output, final boolean md5) {

    final StringBuilder nameToReturn = new StringBuilder();
    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.getCompleteFileName Start");
    final JDatetime valDate = this.getValuationDatetime();
    final String todayDateString = new SimpleDateFormat("yyyyMMdd",
            Locale.getDefault()).format(valDate);

    nameToReturn.append(path);
    nameToReturn.append(name);
    nameToReturn.append(todayDateString);
    nameToReturn.append('.');

    if (md5) {

      nameToReturn.append("EOF");

    } else {

      if (!output) {
        nameToReturn.append(getAttribute(FILEFORMATINPUT));
      } else {
        nameToReturn.append(getAttribute(FILEFORMATOUTPUT));
      }
    }

    Log.debug(this,
            "ScheduledTaskSANT_EMIR_CONCAT_REPORTS.getCompleteFileName End returning "
                    + nameToReturn);
    return nameToReturn.toString();
  }

}

package calypsox.tk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.TimeZone;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;
import calypsox.tk.report.StandardReportOutput;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTaskREPORT;


public class ScheduledTaskSTC_EMIR_UTI_TEMP_EMAIL extends ScheduledTaskREPORT {
  
  
  private static final long serialVersionUID = -1L;
  private static final String EMAIL_SUBJECT = "TEMPORARY UTI REPORT";
  public static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
  private static final String ZIP = ".zip";
  private static final String ADDRESS = "Address";
  private static final String SUBJECT_SEPARATOR = " - ";
  private static final int PROTOCOL_FILE_LENGTH = 7;
  private static final int BUFFER_SIZE = 1024;
  
  
  /*
   * (non-Javadoc)
   *
   * @see com.calypso.tk.util.ScheduledTaskREPORT#getTaskInformation()
   */
  @Override
  public String getTaskInformation() {
    return "Generate reports with trades without UTI and send them to Counterparties by email";
  }
  
  
  /*
   * (non-Javadoc)
   *
   * @see
   * com.calypso.tk.util.ScheduledTaskREPORT#process(com.calypso.tk.service
   * .DSConnection, com.calypso.tk.event.PSConnection)
   */
  @Override
  public boolean process(final DSConnection dsconnection, final PSConnection psconnection) {
    boolean res = true;
    Map<LegalEntity, DefaultReportOutput> reports = null;

    try {
      Log.debug(this, "Run the report SantEmirTempUtiEmail.");
      // run the report 
      final ReportOutput reportoutput = load(dsconnection);     
      if (null != reportoutput &&  reportoutput.getNumberOfRows() > 0) {
        Log.debug(this, "Split the report rows by LegalEntity.");
        // split the report 
        reports = splitByLegalEntity((DefaultReportOutput) reportoutput);
        Log.debug(this, "Send emails by LegalEntity.");
        // send reports
        res = sendReports(reports, dsconnection);
      } else {Log.info(this, "Report empy");}
    } catch (final RemoteException ex) {
      Log.error(this, "Error trying To send reports: " + ex.getMessage());
      res = false;
    } catch (final CloneNotSupportedException e) {
      Log.error(this, "Error cloning reportRow: " + e.getMessage());
      res = false;
    }

    return res;
  }

  
  /**
   * Create new reportOutput using trades without UTITradeId.
   *
   * @param dsconnection
   * @return
   * @throws RemoteException
   * @throws CloneNotSupportedException
   */
  protected ReportOutput load(final DSConnection dsconnection) throws RemoteException, CloneNotSupportedException {
    Log.debug(this, "Start LOAD trades entered today without UTI but with Temporary UTI.");
    final StringBuffer errors = new StringBuffer("desc");
    final JDatetime jdatetime = getValuationDatetime();
    final ReportOutput reportoutput = generateReportOutput(getAttribute(REPORT_TYPE), getAttribute(REPORT_TEMPLATE_NAME), jdatetime, dsconnection, errors);
    return reportoutput;
  }


  /**
   * Send each report to the contact of the counter party.
   *
   * @param reports
   *            reportOutput sorted by cptys.
   * @param dsconnection
   *            DataServer Connection.
   * @param errors
   *            errors reached through the sending.
   * @return true if the email was send correctly.
   */
  protected boolean sendReports(final Map<LegalEntity, DefaultReportOutput> reports, final DSConnection dsconnection) {
    Log.debug(this, "Start SEND reports by LegalEntity.");
    boolean sendResult = true;

    // send different email to different LE.
    for (final Entry<LegalEntity, DefaultReportOutput> entry : reports.entrySet()) {
      try {
        final LegalEntity le = entry.getKey();
        final DefaultReportOutput report = entry.getValue();
        sendEmail(le, report, dsconnection);
      } catch (final Exception ex) {
        Log.error(this, ex);
        sendResult = false;
      }
    }

    Log.debug(this, "End SEND reports by LegalEntity. Send result: " + sendResult);
    return sendResult;
  }

  
  /**
   * send The email in oder to send it.
   *
   * @param legalEntity
   *            LE to send the report.
   * @param defaultReportOutput
   *            output for this Cpty.
   * @param dsconnection
   *            DataServer connection.
   * @param errors
   *            errors reached through the sending.
   * @throws Exception
   */
  private void sendEmail(final LegalEntity legalEntity, final DefaultReportOutput reportoutput, final DSConnection dsconnection) throws Exception {
    
	Log.debug(this, "Start SEND EMAIL.");
    if (legalEntity != null) {
      final String leFullName = legalEntity.getName();
      final String[] emailAddressTo = getEmailAdress(legalEntity, dsconnection);
      if (emailAddressTo != null) {
        Log.info(this, "Send email to LegalEntity: " + legalEntity.getCode());
        Log.info(this, "Emails: " + StringUtils.join(emailAddressTo, ", "));

        // Get the body
        final String body = getBody();

        // Get the name of the file to attach
        final String fileFormat = getAttribute(REPORT_FORMAT);
        final String fileNamePrefix = getAttribute(REPORT_FILE_NAME);

        // create Report
        final String fileName = saveReportView(reportoutput, fileFormat, fileNamePrefix);

        if (null == fileName) {
          throw new Exception("Unable to save Report");
        }

        // zip the report
        final String zipFileName = fileName + ZIP;
        FileUtilityBRS.createZip(new String[] { fileName }, zipFileName);

        final ArrayList<String> attachments = new ArrayList<String>();
        attachments.add(zipFileName);

        Log.debug(this, "Create email message");
        final StringBuilder emailSubject = new StringBuilder();
        emailSubject.append(leFullName).append(SUBJECT_SEPARATOR).append(EMAIL_SUBJECT);
        final List<String> addressesTo = createAddresses(emailAddressTo);

        CollateralUtilities.sendEmail(addressesTo, emailSubject.toString(), body, DEFAULT_FROM_EMAIL, attachments);  
        Log.info(this, "Email sent to " + addressesTo.toString());

        // delete the report file from the system
        Log.debug(this, String.format("Delete Files %s and %s ", fileName, zipFileName));
        FileUtilityBRS.delete(fileName);
        FileUtilityBRS.delete(zipFileName);
      } else {
        final String logMessage = "[Class: ScheduledTaskSANT_EMIR_TEMP_UTI_EMAIL - Method: sendEmail] Unable to retrieve email address or addresses for the Counterparty %s.";
        Log.info(this, String.format(logMessage, legalEntity.getAuthName()));
      }
    }
    Log.debug(this, "End SEND EMAIL.");
  }


  /**
   * Get the body text.
   *
   * @return
   */
  private String getBody() {

    final StringBuffer bodyText = new StringBuffer();
    bodyText.append("<p>Dear team,</p>");
    bodyText.append("<p>Please be informed that Banco Santander has entered with your "
        + "entity into a new transaction subject to EMIR with the below economic details. "
        + "Banco Santander wants to be the entity responsible for generating the UTI "
            + "(the \"UTI Generation Party\"), please see UTI we have generated  and will report "
        + "to Trade Repository. If you disagree with Banco Santander being UTI Generator Party, "
        + "please let us know and provide us with UTI generated by you and we will proceed to update it accordingly.</p>");
    bodyText.append("<p>In case you are interested in Banco Santander being the UTI Generator Party for all transactions you "
        + "entered into with Banco Santander going forward, please inform our group email address: "
        + "<a href=\"mailto:reportingops@gruposantander.com\">reportingops@gruposantander.com</a> and we'll provide "
        + "this information further on.</p>");
    bodyText.append("<p>Thank you.<br>Kind regards.<br>Reporting Ops Team</p>");

    return bodyText.toString();
  }

  
  
  /**
   * Find the email to be used from the Legal Entity.
   *
   * @param legalEntity
   *            Legal Entity to get the contacts.
   * @param dsconnection
   *            DataServer connection.
   * @return Contacts email addresses.
   * @throws Exception
   */
  @SuppressWarnings("rawtypes")
  private String[] getEmailAdress(final LegalEntity legalEntity, final DSConnection dsconnection) throws Exception {
    Log.debug(this, "Start GET email to send inforamtion.");
    LEContact leContact = null;
    String[] mailsSet = null;
    final String delimiter = ";";
    try {
      Vector contacts = DSConnection.getDefault().getRemoteReferenceData().getLEContacts(legalEntity.getId());
      for (int i = 0; i < contacts.size(); i++) {
        leContact = (LEContact) contacts.get(i);
        if (leContact.getContactType().equalsIgnoreCase(ADDRESS)) {
        	break;
        }
      }
      if(leContact != null) {
        final String emails = leContact.getEmailAddress();
        if (!Util.isEmpty(emails)) {
          mailsSet = emails.split(delimiter);
        }
        Log.info(this, "Send email to " + emails);
      } else {
        Log.info(this, "No email for this Counter party: " + legalEntity.getAuthName());
      }
    } catch (final Exception e) {
      throw new Exception(
          "Unable to retrieve email adress or addresses for Counter Party " + legalEntity.getAuthName() + " by its contact.");
    }

    Log.debug(this, "End GET email to send inforamtion.");
    return mailsSet;
  }


  /**
   * Split report output according by counter party
   *
   * @param oldReportoutput
   *            report output to be split by cptys.
   * @return legalEntity maps with the report output.
   * @throws RemoteException
   */
  protected Map<LegalEntity, DefaultReportOutput> splitByLegalEntity(final DefaultReportOutput oldReportoutput) throws RemoteException {

    Log.debug(this, "Start SPLIT by LegalEntity.");
    final Map<LegalEntity, DefaultReportOutput> reports = new HashMap<LegalEntity, DefaultReportOutput>();
    final ReportRow[] rows = oldReportoutput.getRows();
    for (final ReportRow reportRow : rows) {
      final Trade trade = (Trade) reportRow.getProperty(ReportRow.TRADE);
      final LegalEntity cPty = trade.getCounterParty();
      if (reports.containsKey(cPty)) {
        ((StandardReportOutput) reports.get(cPty)).addReportRow(null,reportRow);
      }
      else {
        final StandardReportOutput newReportOutPut = new StandardReportOutput(
            oldReportoutput.getReport());
        newReportOutPut.setRows(new ReportRow[] { reportRow });
        newReportOutPut.setDelimiteur("|");
        reports.put(cPty, newReportOutPut);
      }
    }

    Log.debug(this, "End SPLIT by LegalEntity. Reports size: " + reports.size());
    return reports;
  }


  /**
   * Convert address to InternetAddress.
   *
   * @param emailAddressTo
   *            contact email addresses.
   * @return contact email addresses to be transformed into InternetAddresses
   */
  private List<String> createAddresses(final String[] emailAddressTo) {
    final List<String> addressesTo = new ArrayList<String>();
    for (int i = 0; i < emailAddressTo.length; i++) {
      addressesTo.add(emailAddressTo[i]);
    }
    return addressesTo;
  }


  @SuppressWarnings("rawtypes")
  protected ReportOutput generateReportOutput(final String reportType, final String reportTemplate, final JDatetime valDate,
		  									  final DSConnection dsconnection, final StringBuffer errors) throws RemoteException {
    
	final PricingEnv pricingenv = dsconnection.getRemoteMarketData().getPricingEnv(_pricingEnv, valDate);
    final Report report = createReport(reportType, reportTemplate, errors, pricingenv);
    if (report == null) {
      Log.error(this, (new StringBuilder()).append("Invalid report type: ").append(reportType).toString());
      errors.append((new StringBuilder()).append("Invalid report type: ").append(reportType).append("\n").toString());
      return null;
    }
    if (report.getReportTemplate() == null) {
      Log.error(this, (new StringBuilder()).append("Invalid report template: ").append(reportType).toString());
      errors.append((new StringBuilder()).append("Invalid report template: ").append(reportType).append("\n").toString());
      return null;
    }
    final Vector vector = getHolidays();
    report.getReportTemplate().setHolidays(vector);
    if (getTimeZone() != null) {
      report.getReportTemplate().setTimeZone(getTimeZone());
    }
    final Vector vector1 = new Vector();
    return report.load(vector1);
  }


  protected String saveReportView(final ReportOutput reportoutput, final String fileFormat, final String fileNamePrefix) throws IOException {
    return saveReportView(reportoutput, fileFormat, fileNamePrefix, false);
  }


  protected String saveReportView(final ReportOutput reportoutput, final String fileFormat, final String fileNamePrefix, final boolean printControlLine) throws IOException {
	  
	  String res;
	  String reportView = null;
	  String viewerName = "html";

	  if (fileFormat != null) {
		  if ("Excel".equals(fileFormat)) {
			  viewerName = "xls";
	      } else if ("csv".equals(fileFormat)) {
	    	  viewerName = "csv";
	      }
	  }
	  
	  com.calypso.tk.report.ReportViewer reportviewer = null;
	  reportviewer = DefaultReportOutput.getViewer(viewerName);
	  reportoutput.format(reportviewer);

	  final StringBuilder reportViewBuilder = new StringBuilder();
	  reportViewBuilder.append(reportviewer.toString());
	  if (printControlLine) {
		  reportViewBuilder.append(UtilReport.getControlLine(reportoutput.getNumberOfRows(), reportoutput.getValDate().getJDate(TimeZone.getDefault())));
	  }
	  
	  reportView = reportViewBuilder.toString();

	  final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
	  final Date valDate = new Date(this.getValuationDatetime().getTime());
	  res = DisplayInBrowser.buildDocument(reportView, viewerName, fileNamePrefix + "_" + format.format(valDate), false, 1);

	  if (res.startsWith("file:")) {
		  res = res.substring(PROTOCOL_FILE_LENGTH);
	  }
	  
	  return res;
  }


  /**
   * Delete the given file name
   * 
   * @param fileName
   *            file name
   */
  public static void deleteFile(final String fileName) {

      // A File object to represent the filename
      final File f = new File(fileName);

      // Make sure the file or directory exists and isn't write protected
      if (!f.exists()) {
          throw new IllegalArgumentException("Delete: no such file or directory: " + fileName);
      }

      if (!f.canWrite()) {
          throw new IllegalArgumentException("Delete: write protected: " + fileName);
      }

      // If it is a directory, make sure it is empty
      if (f.isDirectory()) {
          final String[] files = f.list();
          if (files.length > 0) {
              throw new IllegalArgumentException("Delete: directory not empty: " + fileName);
          }
      }

      // Attempt to delete it
      final boolean success = f.delete();

      if (!success) {
          throw new IllegalArgumentException("Delete: deletion failed");
      }
  }


  /**
   * Create a zip file containing the given filenames in arguments
   * 
   * @param filenames
   *            an array containing the file names
   * @param outFilename
   *            output file name
   */
  public void createZipFile(final String[] filenames, final String outFilename) {

	  // Create a buffer for reading the files
      final byte[] buf = new byte[BUFFER_SIZE];

      ZipOutputStream out = null;
      FileInputStream in = null;
      try {
          // Create the ZIP file
          out = new ZipOutputStream(new FileOutputStream(outFilename));

          // Compress the files
          for (int i = 0; i < filenames.length; i++) {
              in = new FileInputStream(filenames[i]);
              final File file = new File(filenames[i]);
              // Add ZIP entry to output stream.
              out.putNextEntry(new ZipEntry(file.getName()));
              // Transfer bytes from the file to the ZIP file
              int len;
              while ((len = in.read(buf)) > 0) {
                  out.write(buf, 0, len);
              }
              // Complete the entry
              out.closeEntry();
              in.close();
          }
          
          // Complete the ZIP file
          out.close();
      } catch (final IOException e) {
          Log.error(this, e);
      } finally {
          try {
              if (out != null) {
                  out.close();
              }
              if (in != null) {
                  in.close();
              }
          } catch (final IOException e) {
              Log.error(this, "Couldn't close Stream: " + e.getMessage(), e);
          }
      }
  }
  

}

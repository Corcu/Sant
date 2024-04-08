package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;
import calypsox.tk.report.StandardReportOutput;
import calypsox.util.collateral.CollateralUtilities;


	/**
	 * This scheduled task's purpose is to send an email with the Material Terms
	 * report
	 *
	 */
public class ScheduledTaskSTC_MATERIAL_TERMS_EMAIL extends ScheduledTaskREPORT {
	  
  private static final long serialVersionUID = -1L;
  private static final String EMAIL_SUBJECT = "Calypso STC: Material Terms BRS extraction";
  public static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
  
  @SuppressWarnings("unused")
  private static String htmlReport = null;
   
  @Override
  public String getTaskInformation() {
	  return "Send by email the Material Terms report";
  }


  /**
   * this is the main method for the scheduled task's execution
   *
   * @see com.calypso.tk.util.ScheduledTask#process(com.calypso.tk.service.DSConnection,
   *      com.calypso.tk.event.PSConnection)
   * @param ds
   *            Data Server connection
   * @param ps
   *            Event server connection
   * @return true if the scheduled task have ran ok, false in case of error
   */
  @Override
  public boolean process(final DSConnection dsCon, final PSConnection ps) {
    boolean res = false;
    final JDatetime valDate = getValuationDatetime();
    final StringBuffer errors = new StringBuffer("desc");
    final String reportType = getAttribute(REPORT_TYPE);
    final String reportTemplateName = getAttribute(REPORT_TEMPLATE_NAME);
    
    try {
      // run the report
      final ReportOutput reportoutput = generateReportOutput(reportType, reportTemplateName, valDate, dsCon, errors);

      // Print delimiter and headings in report output
      if (reportoutput instanceof StandardReportOutput) {
        final StandardReportOutput standardReportOutput = (StandardReportOutput) reportoutput;
        standardReportOutput.setDelimiteur(";");
        standardReportOutput.setShowHeadings(true);
      }

      // send the report by email 
      res = sendReports(reportoutput, dsCon);

    } catch (final RemoteException ex) {
      res = false;
      Log.error(this, "Error trying To create Material Terms report file: " + ex.getMessage());
    }

    return res;
  }


  /**
   * Send each report to the contact of the counter party
   *
   * @param reports
   * @param dsconnection
   * @param errors
   * @return
   */
  private boolean sendReports(final ReportOutput reportoutput, final DSConnection dsconnection) {
    boolean sendResult = true;
    try {
      sendEmail(reportoutput, dsconnection);
    } catch (final Exception e) {
      Log.error(this, "Error trying to send Material Terms email: " + e.getMessage());
      sendResult = false;
    }
    return sendResult;
  }


  /**
   * send The email in oder to send it
   *
   * @param legalEntity
   * @param defaultReportOutput
   * @param dsconnection
   * @param errors
   * @throws Exception
   */
  private void sendEmail(final ReportOutput reportoutput, final DSConnection dsconnection) throws Exception {

    final String fileFormat = getAttribute(REPORT_FORMAT);
    final String fileNamePrefix = getAttribute(REPORT_FILE_NAME);

    // Addresses to
    final String attrAdresses = getAttribute(EMAIL_LIST).trim();
    final List<String> addressesTo = createAddresses(attrAdresses.split(";"));

    // Create Report
    String fileName;
    if ("pdf".equals(fileFormat)) {
      fileName = FileUtilityBRS.savePDFReportView(reportoutput, fileNamePrefix);
    } else {
      fileName = FileUtilityBRS.saveReportView(reportoutput, fileFormat, fileNamePrefix, true);
    }

    // Attachments
    final ArrayList<String> attachments = new ArrayList<String>();
    if (Util.isEmpty(fileName)) {
      throw new Exception("Unable to save Report");
    }
    else {
    	attachments.add(fileName);
    }
    	
    // Body
    final String body = createHtmlMessage().toString();
    Log.debug(this, body);

    // Send email
    CollateralUtilities.sendEmail(addressesTo, EMAIL_SUBJECT, body, DEFAULT_FROM_EMAIL, attachments);

    // delete the report file from the system
    //Log.debug(this, String.format("Delete File %s", fileName));
    //FileUtilityBRS.delete(fileName);
  }


  private StringBuilder createHtmlMessage() {
    final StringBuilder htmlMsg = new StringBuilder("<br>Buenos dias,<br><br>Adjunto se encuentra el reporte Material Terms de Calypso Pro para la operativa de BRS.<br><br>");
    htmlMsg.append("Un saludo.<br>");
    return htmlMsg;
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


}

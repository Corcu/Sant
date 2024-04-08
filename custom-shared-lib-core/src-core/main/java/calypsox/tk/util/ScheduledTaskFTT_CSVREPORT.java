package calypsox.tk.util;

import calypsox.tk.report.StandardReportOutput;
import com.calypso.tk.core.*;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;

import java.text.SimpleDateFormat;
import java.util.*;


public class ScheduledTaskFTT_CSVREPORT extends ScheduledTaskCSVREPORT {

	public static final String DELIMITEUR = "CSV Delimiter";
	public static final String ATTR_REPORT_FREQUENCY = "REPORT FREQUENCY";
	public static final String ATTR_HEADER_ACCOUNT = "HEADER ACCOUNT";
	public static final String ATTR_PREVIOUS_MONTH = "Previous Month";
	public static final String ATTR_FROM_TEMPLATE = "From Template";

	private int checkDelim = 0;

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(ATTR_REPORT_FREQUENCY);
		result.add(ATTR_HEADER_ACCOUNT);
		return result;
	}

	@Override
	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
		Vector v = super.getAttributeDomain(attribute, hashtable);
		if (attribute.equals(ATTR_REPORT_FREQUENCY)) {
			v.addElement(ATTR_PREVIOUS_MONTH);
			v.addElement(ATTR_FROM_TEMPLATE);
		}
		return v;
	}

	@Override
	protected void modifyTemplate(Report reportToFormat) {
		// set Frequency to ReportTemplate
		reportToFormat.getReportTemplate().put(ATTR_REPORT_FREQUENCY, getAttribute(ATTR_REPORT_FREQUENCY));
	}


	/**
	 * To generate header Line
	 *
	 * @param reportOutput Output for the report.
	 * @return The text control line
	 */
	protected String generateHeaderLine(final ReportOutput reportOutput) {

		int numberOfRows = reportOutput.getNumberOfRows();
		JDate refDate = getValuationDatetime().getJDate(TimeZone.getDefault()).addMonths(-1);
		int month = refDate.getMonth();
		int year = refDate.getYear();
		if (month==12)  {
			month = 0;
			year = year +1;
		} else {
			month = month -1;
		}

		final String delim = getAttribute(DELIMITEUR);
		String headerAccount = getAttribute(ATTR_HEADER_ACCOUNT);
		StringBuilder header = new StringBuilder();
		//header.append("96606").append(delim);
		header.append(headerAccount).append(delim);
		header.append("D").append(delim);
		header.append("0").append(delim);
		header.append(numberOfRows).append(delim);
		header.append(new SimpleDateFormat("yyyyMMdd").format(getValuationDatetime())).append(delim);
		header.append(new SimpleDateFormat("yyyyMM").format(refDate.getDate())).append(delim);

		String yearMonth = String.valueOf(year).concat(String.format("%02d", month));
		header.append("BSCHESMM-").append(yearMonth).append("00-T").append(delim);
		header.append("BSCHESMM").append(delim);
		header.append("BANCO SANTANDER").append(delim);
		header.append("PASEO DE PEREDA").append(delim);
		header.append("ESP").append(delim);
		header.append("A39000013").append(delim).append("\n");
		return header.toString();
	}


	/**
	 * Call AFTER generating the Report
	 */
	@Override
	protected String saveReportOutput(final ReportOutput reportOutput, String type, final String reportName,
									  final String[] errors, final StringBuffer notifications) {

		final String delimiteur = getAttribute(DELIMITEUR);
		final String ctrlLine = getAttribute(CTRL_LINE);
		final String fileFormat = getAttribute(REPORT_FORMAT);

		Log.debug(Log.CALYPSOX, "Entering ScheduledTaskReport::reportViewer");

		if ((delimiteur == null) && !"Excel".equals(fileFormat) && (reportOutput instanceof StandardReportOutput)) {
			((StandardReportOutput) reportOutput).setDelimiteur("@");
			this.checkDelim = 1;
		}

		if ((reportOutput instanceof StandardReportOutput) && (delimiteur != null) && !delimiteur.equals("")) {
			((StandardReportOutput) reportOutput).setDelimiteur(delimiteur);
		}

		// a silly workaround to convey the delimiter info to the CSV viewer!!!!
		((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_DELIMITER", delimiteur);

		String reportStr = super.saveReportOutput(reportOutput, type, reportName, errors, notifications);

		String headerLine = generateHeaderLine(reportOutput);

		if (Util.isEmpty(reportStr)
				|| reportStr.startsWith("No Records")) {
			reportStr = "";
		}

		reportStr = headerLine + reportStr;

		// set extension
		String fileName = getFileName();
		if (fileName.startsWith("file://")) {
			fileName = fileName.substring(7);
		}

		if ((ctrlLine != null) && (ctrlLine.equals("false"))) {
			return generateReportFile(reportOutput, reportStr, fileName, false);
		} else {
			return generateReportFile(reportOutput, reportStr, fileName, true);
		}
	}

	@Override
	public String[] getRealFilenames(JDatetime valDatetime, String fileName) {
		Calendar cal = valDatetime.getJDate(TimeZone.getDefault()).addMonths(-1).asCalendar();
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		Date  startDate = cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("MMyyyy");
		fileName = fileName + sdf.format(startDate) + "_";
		return super.getRealFilenames(valDatetime, fileName);
	}
}
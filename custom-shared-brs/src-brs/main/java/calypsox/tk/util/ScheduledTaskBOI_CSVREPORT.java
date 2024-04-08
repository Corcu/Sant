package calypsox.tk.util;

import calypsox.tk.bo.boi.BOIStaticData;
import calypsox.tk.report.StandardReportOutput;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


public class ScheduledTaskBOI_CSVREPORT extends com.calypso.tk.util.ScheduledTaskREPORT {

	private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMdd");
	public static final String DELIMITEUR = "CSV Delimiter";
	public static final String CTRL_LINE = "Control Line";
	private int checkDelim = 0;

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(DELIMITEUR);
		result.add(CTRL_LINE);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.add(attribute(DELIMITEUR));
		attributeList.add(attribute(CTRL_LINE).booleanType());
		return attributeList;
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

	/**
	 * To generate the report file
	 *
	 * @param reportOutput Output for the report.
	 * @param reportString String with report data.
	 * @param fileName     String with report file name.
	 * @param ctrlLine     true for include control line, false for not include.
	 * @return The report text
	 */
	protected String generateReportFile(final ReportOutput reportOutput, String reportString, final String fileName,
										final boolean ctrlLine) {
		if (ctrlLine) {
			final String controlLine = generateControlLine(reportOutput);
			reportString = reportString + controlLine;
		}
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)) {
			writer.write(reportString);
		} catch (final FileNotFoundException e) {
			Log.error(this,
					"The filename is not valid. Please configure the scheduled task with a valid filename: " + fileName,
					e);
		} catch (final IOException e) {
			Log.error(this, "An error ocurred while writing the files: " + fileName, e);
		}
		return reportString;
	}

	/**
	 * To generate the final control line
	 *
	 * @param reportOutput Output for the report.
	 * @return The text control line
	 */
	protected String generateControlLine(final ReportOutput reportOutput) {
		String controlLine = "*****";
		controlLine = controlLine + String.format("%08d", reportOutput.getNumberOfRows())
				+ DATEFORMAT.format(getValuationDatetime().getJDate(TimeZone.getDefault()).getDate(this._timeZone));
		return controlLine;
	}

	@Override
	public String[] getRealFilenames(JDatetime valDatetime, String fileName) {
		return new String[] {getFileName(getAttribute(REPORT_FILE_NAME), valDatetime)};
	}

	/**
	 *
	 * @param fileName
	 * @return Name of the file with timestamp added (if ST attr option es true)
	 *         and file extension
	 */
	private String getFileName(final String fileName, JDatetime valDate) {
		StringBuilder file = new StringBuilder();
		String addTime = getAttribute(TIMESTAMP_FILENAME);

		String route = fileName.substring(0, fileName.lastIndexOf('/') + 1);
		String fileNameIn = fileName.substring(fileName.lastIndexOf('/') + 1);

		String part1 = StringUtils.substringBefore(fileNameIn, "_");
		String part2 = StringUtils.substringAfter(fileNameIn, "_");

		// add timestamp to the file name
		if (!com.calypso.infra.util.Util.isEmpty(fileName) && Boolean.valueOf(addTime)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(getAttribute(TIMESTAMP_FORMAT));
			if (valDate != null) {
				file.append(part1);
				file.append(BOIStaticData.CTE_DELIMITEUR);
				file.append(dateFormat.format(valDate));
				file.append(BOIStaticData.CTE_DELIMITEUR);
				file.append(part2);
				file.append(BOIStaticData.CTE_DELIMITEUR);
			} else {
				file.append(part1);
				file.append(BOIStaticData.CTE_DELIMITEUR);
				file.append(dateFormat.format(getValuationDatetime()));
				file.append(BOIStaticData.CTE_DELIMITEUR);
				file.append(part2);
				file.append(BOIStaticData.CTE_DELIMITEUR);
			}

		}
		return route + getFile(file.toString(), route);
	}

	public static String getFile(final String fileName, final String route) {

		// Aquí la carpeta donde queremos buscar
		String path = route;
		String files;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		int j = 1;
		String file = fileName + String.format(Locale.ENGLISH, "%02d", j);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				files = listOfFiles[i].getName();
				if (files.startsWith(file)) {
					j++;
					file = fileName + String.format(Locale.ENGLISH, "%02d", j);
				}
			}
		}
		return file;
	}

	public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
		Vector v = super.getAttributeDomain(attr, currentAttr);
		if (attr.equals("REPORT FORMAT")) {
			v.addElement("dat");
		}
		return v;
	}
}
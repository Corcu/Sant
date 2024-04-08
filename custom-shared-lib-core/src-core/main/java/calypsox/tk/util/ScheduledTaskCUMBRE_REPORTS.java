
package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.CumbreReportLogic;
import org.apache.commons.lang.StringEscapeUtils;

public class ScheduledTaskCUMBRE_REPORTS extends ScheduledTaskCSVREPORT {

	public static final String CUMBRE_REPORT = "Cumbre Report Type";
	public static final String REPORT_FREQUENCY = "Report frequency";

	private static final String HEADER_FILE = "Header report";
	private static final String EMPTY_FILE = "Empty report";
	private static final String BALANCE_FILE = "Balance report";
	private static final String MOVEMENT_FILE = "Movement report";
	private static final String DATE_FORMAT = "yyMMddHHmm";
	private static final String MONTHLY = "Monthly";
	private static final String DAILY = "Daily";
	private static final String HEADER_DATE_FORMAT = "1YYMMddHHmm";
	
	private static final String CUMBRE_NEWLINE_SEPARATOR = "CumbreNewLineSeparator";


	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		boolean fileCreated = true;
		String cumbreReportType = getAttribute(CUMBRE_REPORT);
		String fileName = getFileName(getAttribute(REPORT_FILE_NAME), null);
		if (fileName == null) {
			Log.error(this, "Can't get filename from '" + REPORT_FILE_NAME + "' attribute");
			fileCreated = false;
		} else {
			try ( BufferedWriter bw = Files.newBufferedWriter(new File(fileName).toPath(), StandardCharsets.UTF_8) ) {			// Java 7's try-with-resources structure automatically handles closing the resources that the try itself opens
				if (cumbreReportType.equals(HEADER_FILE)) {
					fillHeader(bw);
				} else if (cumbreReportType.equals(BALANCE_FILE)) {
					fileCreated = super.process(ds, ps);
				} else if (cumbreReportType.equals(MOVEMENT_FILE)) {
					fileCreated = createMovReport(ds, ps, getAttribute(REPORT_FILE_NAME), bw);
				}
			}
			catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e1) {
				Log.error(this, "Error creating file: " + fileName + "\n" + e1);
				fileCreated = false;
			}
		}
		return fileCreated;
	}

	/**
	* @deprecated (for Sonar)
	*/
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@Deprecated
	public Vector getDomainAttributes() {
		Vector domain = new Vector();
		domain.add(REPORT_TYPE);
		domain.add(REPORT_FILE_NAME);
		domain.add(REPORT_FORMAT);
		domain.add(REPORT_TEMPLATE_NAME);
		domain.add(TIMESTAMP_FILENAME);
		domain.add(TIMESTAMP_FORMAT);
		domain.add(DELIMITEUR);
		domain.add(CTRL_LINE);
		domain.add(CUMBRE_REPORT);
		domain.add(REPORT_FREQUENCY);

		return domain;
	}

	/**
	* @deprecated (for Sonar)
	*/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Deprecated
	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
		Vector vector = super.getAttributeDomain(attribute, hashtable);
		if (attribute.equals(CUMBRE_REPORT)) {
			vector.addElement(HEADER_FILE);
			vector.addElement(EMPTY_FILE);
			vector.addElement(BALANCE_FILE);
			vector.addElement(MOVEMENT_FILE);
		} else if (attribute.equals(REPORT_FREQUENCY)) {
			vector.addElement(MONTHLY);
			vector.addElement(DAILY);
		}
		return vector;
	}

	/**
	 * 
	 * @param fileName
	 * @return Name of the file with timestamp added (if ST attr option es true)
	 *         and file extension
	 */
	private String getFileName(final String fileName, JDatetime valDate) {
		StringBuilder file = new StringBuilder(fileName);
		String addTime = getAttribute(TIMESTAMP_FILENAME);
		// add timestamp to the file name
		if (!Util.isEmpty(fileName) && Boolean.valueOf(addTime)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(getAttribute(TIMESTAMP_FORMAT));
			if (valDate != null) {
				file.append(dateFormat.format(valDate));
			} else {
				file.append(dateFormat.format(getValuationDatetime()));
			}
			String frequency = getAttribute(REPORT_FREQUENCY);
			if (!Util.isEmpty(frequency) && MONTHLY.equals(frequency)) {
				file.append("AG");
			}
		}
		return file.toString();
	}

	@Override
	public String[] getRealFilenames(JDatetime valDatetime, String fileName) {		
		return new String[] {getFileName(getAttribute(REPORT_FILE_NAME), valDatetime)};
	}

	@Override
	protected String generateReportFile(ReportOutput reportOutput, String reportString, String fileName, boolean ctrlLine) {
		if (ctrlLine) {
			final String controlLine = generateControlLine(reportOutput);
			reportString = reportString + controlLine;
		}

		// Personalized line separator		See separators at https://stackoverflow.com/a/6374360
		String newLineSep = getNewLineSeparator();
		reportString = reportString.replaceAll("\\r\\n|\\n|\\r", Matcher.quoteReplacement(newLineSep));			// quote the replacement to avoid references to the regex matching
		
		if (fileName == null) {
			Log.error(this, "fileName is null");
		} else {
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		
			try (final BufferedWriter writer = Files.newBufferedWriter(new File(fileName).toPath(), StandardCharsets.UTF_8)) {			// Java 7's try-with-resources structure automatically handles closing the resources that the try itself opens
				writer.write(reportString);
			} catch (final FileNotFoundException | InvalidPathException e) {
				Log.error(this,
						"The filename is not valid. Please configure the scheduled task with a valid filename: " + fileName,
						e);
			} catch (final IOException | UnsupportedOperationException | SecurityException e) {
				Log.error(this, "An error ocurred while writing the file: " + fileName, e);
			}
		}
		return reportString;
	}

	private void fillHeader(BufferedWriter bw) throws IOException {
		StringBuilder header = new StringBuilder("011");
		JDatetime processDate = getValuationDatetime();
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		header.append(dateFormat.format(processDate));
		header.append("00001");
		header.append(CumbreReportLogic.appendChar("0", 30));
		bw.write(header.toString());
	}

	/**
	 * Generate movement report file and add header created with information 
	 * from the file. After creating movement report, rest of the files are
	 * deleted.
	 * 
	 * @param ds
	 * @param ps
	 * @param reportFileName
	 * @param bw
	 * @param file
	 * @return file with header that contain today and last business day
	 *         operations.
	 * @throws IOException
	 */
	private boolean createMovReport(DSConnection ds, PSConnection ps, String reportFileName, BufferedWriter bw
			) throws IOException {

		boolean fileCreated = true;
		String todayReport = reportFileName + "today";

		// generar el reporte de movimientos (contiene hoy y ayer)
		this.setAttribute(REPORT_FILE_NAME, todayReport);
		fileCreated &= super.process(ds, ps);
		this.setAttribute(REPORT_FILE_NAME, reportFileName);

		if (! fileCreated) {
			Log.error(this, "Could not generate movement report file.");
		} else {
			String todayFileComplete = getFileName(todayReport, null);		// adds timestamp (if ST attr option is true) and file extension
			if (todayFileComplete == null) {
				Log.error(this, "Can't add timestamp and extension to today report filename '" + todayReport + "'");
				fileCreated = false;
			}
			else {
				addHeaderToFile(todayFileComplete, bw);					// generate header
				readFileAndCopyToBuffer(todayFileComplete, bw);			// add file

				// Delete auxiliary files  (not fatal if fails)
				try {
					File txtFile = new File(todayFileComplete);
					File csvFile = new File(todayFileComplete + ".csv");
					Files.delete( txtFile.toPath() );
					Files.delete( csvFile.toPath() );
				} catch (Exception e) {
					Log.warn(this, "Can't delete file '" + todayFileComplete + "' (or the corresponding csv file): " + e, e);
				}
			}
		}

		return fileCreated;
	}

	private void addHeaderToFile(String todayReport, BufferedWriter bw) throws IOException {

		// leer el fichero y sacar infomación
		int numberOfLines = 1; // se cuenta el header
		double debAmounts = 0.0;
		
		if (todayReport == null) {
			Log.error(this, "todayReport is null");
		} else {
			try ( BufferedReader buffReader = Files.newBufferedReader(new File(todayReport).toPath(), StandardCharsets.UTF_8)) {
				String line;
				while ((line = buffReader.readLine()) != null) {
					numberOfLines++;
					debAmounts += getDebAmountFromLine(line);
				}
			}
			catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e1) {
				throw new IOException("Can't read today report file '" + todayReport + "': " + e1, e1);
			}
		}

		// crear header y escribir header al buffer
		bw.write(createHeader(numberOfLines, debAmounts));
		bw.write(getNewLineSeparator());

	}

	private double getDebAmountFromLine(String line) {
		double debAmount = 0.0;
		String movementType = line.substring(58, 59);

		if ("D".equals(movementType)) {
			String amount = line.substring(62, 77);
			debAmount = Double.valueOf(amount);
		}

		return debAmount;
	}

	private String createHeader(int lines, double debAmounts) {
		StringBuilder header = new StringBuilder("01");

		SimpleDateFormat dateFormat = new SimpleDateFormat(HEADER_DATE_FORMAT);
		JDatetime processDate = getValuationDatetime();
		header.append(dateFormat.format(processDate));
		header.append(CumbreReportLogic.appendChar("0", 5 - String.valueOf(lines).length()));
		header.append(lines);
		header.append(CumbreReportLogic.formatDoubleToString(15, debAmounts, false, 0));	// 0 decimals to force the use of new formatter (String.format() instead of String.valueOf())
		header.append(CumbreReportLogic.formatDoubleToString(15, debAmounts, false, 0));
		return header.toString();
	}

	/**
	 * Reads the file names "fileName" and writes his content to the buffer
	 * writer "bw"
	 */
	private void readFileAndCopyToBuffer(String fileName, BufferedWriter bw) throws IOException {

		if (fileName == null) {
			Log.error(this, "fileName is null");
		} else {
			try ( BufferedReader buffReader = Files.newBufferedReader(new File(fileName).toPath(), StandardCharsets.UTF_8) ) {			// Java 7's try-with-resources structure automatically handles closing the resources that the try itself opens
				String lineSep = getNewLineSeparator();
				String line;
				while ((line = buffReader.readLine()) != null) {
					bw.write(line);
					bw.write(lineSep);
				}
			}
			catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e1) {
				throw new IOException("Can't read file '" + fileName + "': " + e1, e1);
			}
		}
	}

	/** @return Returns the line separator configured at the domainValue 'CumbreNewLineSeparator'.
	 * 			Special characters (\n, \r, \t, etc) are already interpreted, same as in a Java 
	 * 			String literal. Returns Windows newline (\r\n, interpreted) if the domainValue
	 * 			doesn't exist or is empty. Never returns null.
	 */
	private String getNewLineSeparator() {
		Vector<String> values = null;
		try {
			values = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(CUMBRE_NEWLINE_SEPARATOR);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Can't get DomainValues for domain '" + CUMBRE_NEWLINE_SEPARATOR + "': " + e);
		}
		return (values == null || values.isEmpty() || values.get(0) == null || values.get(0).isEmpty())
				? "\r\n"											// default fallback
				: StringEscapeUtils.unescapeJava(values.get(0));	// unescape to interpret special characters (\n, \r, \t, etc)
	}

	
}
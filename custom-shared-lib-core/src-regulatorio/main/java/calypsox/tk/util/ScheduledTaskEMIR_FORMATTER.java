/**
 * 
 */
package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

/**
 * Scheduled Task that customize the report output for Emir reports.
 * 
 * @author tyavorsk
 *
 */
public class ScheduledTaskEMIR_FORMATTER extends ScheduledTaskCSVREPORT {

	public static final String COLUMN_ATTR = "Fixed number of columns";
	public static final String RECOVER_WEEKEND = "Recover weekend";

	private static final String NO_FILE_ERROR = "Could not find report output file.";
	private static final String PROCESS_FILE_ERROR = "Could not process report output.";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(Vector messages) {
		if (Util.isEmpty(getAttribute(REPORT_TYPE))) {
			messages.add("Report type should be selected.");
		}
		if (Util.isEmpty(getAttribute(REPORT_FILE_NAME))) {
			messages.add("Report file name should be filed.");
		}
		if (Util.isEmpty(getAttribute(REPORT_FORMAT))) {
			messages.add("Report format should be selected.");
		}
		if (Util.isEmpty(getAttribute(REPORT_TEMPLATE_NAME))) {
			messages.add("Report template name should be selected.");
		}
		if (Util.isEmpty(getAttribute(REPORT_OUTPUT_TEMPLATE))) {
			messages.add("Report output file name should be defined.");
		}
		if (!Util.isEmpty(getAttribute(TIMESTAMP_FILENAME)) && Boolean.valueOf(getAttribute(TIMESTAMP_FILENAME))
				&& Util.isEmpty(getAttribute(TIMESTAMP_FORMAT))) {
			messages.add("Timestamp format should be defined.");
		}
		if (Util.isEmpty(getAttribute(DELIMITEUR))) {
			messages.add("Report Delimiter should be defined.");
		}
		if (Util.isEmpty(getAttribute(COLUMN_ATTR))) {
			messages.add("Field Fixed number of columns should be defined.");
		}
		if (Util.isEmpty(getAttribute(RECOVER_WEEKEND)) && getAttribute(REPORT_TYPE).contains("Linking")) {
			messages.add("Recover weekend attribute should be true o false.");
		}


		return Util.isEmpty(messages);
	}

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {

		String recoverWeekend = getAttribute(RECOVER_WEEKEND);
		if (!Util.isEmpty(recoverWeekend) && Boolean.toString(true).equalsIgnoreCase(recoverWeekend)
				&& getAttribute(REPORT_TYPE).contains("Linking")) {
			modifyExecutionDate();
		}

		boolean processOk = super.process(ds, ps);

		if (processOk) {
			String fileName = getFileName();
			if (!Util.isEmpty(fileName)) {
				processFile(fileName);
			} else {
				Log.error(this, NO_FILE_ERROR);
			}
		}
		// TODO revisar salida
		return processOk;
	}

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attrList = new ArrayList<AttributeDefinition>();
		attrList.add(attribute(DELIMITEUR));
		attrList.add(attribute(SHOWHEADINGS).booleanType());
		attrList.add(attribute(COLUMN_ATTR));
		attrList.add(attribute(RECOVER_WEEKEND).booleanType());
		attrList.add(attribute(SAVE_OR_EMAIL_BLANK_REPORT).booleanType());
		return attrList;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Vector getDomainAttributes() {
		Vector domain = new Vector();
		domain.add(REPORT_TYPE);
		domain.add(REPORT_FILE_NAME);
		domain.add(REPORT_FORMAT);
		domain.add(REPORT_TEMPLATE_NAME);
		domain.add(REPORT_OUTPUT_TEMPLATE);
		domain.add(TIMESTAMP_FILENAME);
		domain.add(TIMESTAMP_FORMAT);
		domain.add(DELIMITEUR);
		domain.add(SHOWHEADINGS);
		domain.add(COLUMN_ATTR);
		domain.add(RECOVER_WEEKEND);
		domain.add(SAVE_OR_EMAIL_BLANK_REPORT);

		return domain;
	}

	private void modifyExecutionDate() {
		JDate date = this.getCurrentDate();
		if (isBusinessDayAfterWeekend(date)) {
			this.setCurrentDate(getSaturdayBeforeDate(date));
		}
	}

	private boolean isBusinessDayAfterWeekend(JDate date) {
		Vector<String> holidays = getHolidays();
		Holiday holiday = Holiday.getCurrent();

		// Caso 1: Lunes despues de fin de semana
		int dayOfWeek = date.getDayOfWeek();
		if (Calendar.MONDAY == dayOfWeek) {
			return true;
		} else {// Caso 2: Festivo despu?s de fin de semana
			while (dayOfWeek > 1) {
				if (holiday.isBusinessDay(date, holidays)) {
					return false;
				}
				dayOfWeek--;
				date = date.addDays(-1);
			}
			return true;
		}
	}

	private JDate getSaturdayBeforeDate(JDate date) {
		return date.addDays(-date.getDayOfWeek());
	}

	private void processFile(final String fileName) {
		try {
			File file = new File(getFileName(fileName));
			File fileOut = new File(getOutputFileName());

			FileReader fileReader = new FileReader(file);
			BufferedReader buffer = new BufferedReader(fileReader);

			FileWriter fileWriter = new FileWriter(fileOut);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			
			String[] columnHeaders = null;
			String[] rowValues = null;
			int columnNumber = getColumnNumber(getAttribute(COLUMN_ATTR));
			String delimiter = getAttribute(DELIMITEUR);
			String line;
			while ((line = buffer.readLine()) != null) {
				if (columnHeaders == null) {
					columnHeaders = line.split(delimiter);
					continue;
				}
				if (!line.contains("*****")) {
					rowValues = line.split(delimiter);
					if(rowValues.length > columnHeaders.length){
						Log.info(ScheduledTaskEMIR_FORMATTER.class, "Trade with trade keyword BO_REFERENCE: " + rowValues[0] + "is not included. Some fields may contain comma character.");
						continue;
					}
					
					if (rowValues != null && columnNumber < rowValues.length) {
						String fixedLine = getFixedLine(rowValues, columnNumber);
						for (int i = columnNumber; i < rowValues.length; i++) {
							if (!Util.isEmpty(rowValues[i])) {
								StringBuilder newLine = new StringBuilder(fixedLine);
								newLine.append(columnHeaders[i] + delimiter);
								newLine.append(rowValues[i]);
								writer.write(newLine.toString());
								writer.write("\n");
							}
						}
					} else {
						Log.error(this, PROCESS_FILE_ERROR + "\n");
					}
				}

			}
			writer.flush();
			buffer.close();
			writer.close();

		} catch (Exception e) {
			Log.error(this, e + ": " + PROCESS_FILE_ERROR);
			Log.error(this, e);
		}
	}

	private String getFileName(final String fileName) {
		String file = fileName.substring(7, fileName.length() - 3);
		file += getAttribute(REPORT_FORMAT);
		return file;
	}

	private String getFixedLine(String[] rowValues, int columnNumber) {
		StringBuilder fixedLine = new StringBuilder();
		for (int i = 0; i < columnNumber; i++) {
			fixedLine.append(rowValues[i] + ",");
		}
		return fixedLine.toString();
	}

	private int getColumnNumber(String attribute) {
		if (!Util.isEmpty(attribute)) {
			return Integer.valueOf(attribute);
		}
		// TODO revise output
		return 0;
	}

	private String getOutputFileName() {
		StringBuilder fileName = new StringBuilder(getAttribute(REPORT_OUTPUT_TEMPLATE));
		String addTimestamp = getAttribute(TIMESTAMP_FILENAME);
		String dateFormat = getAttribute(TIMESTAMP_FORMAT);
		if (!Util.isEmpty(addTimestamp) && Boolean.valueOf(addTimestamp) && !Util.isEmpty(dateFormat)) {
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			fileName.append(
					formatter.format(getValuationDatetime().getJDate(TimeZone.getDefault()).getDate(this._timeZone)));
		}
		fileName.append(".");
		fileName.append(getAttribute(REPORT_FORMAT));

		return fileName.toString();
	}
	
	protected void addScheduledTaskHolidays(ReportTemplate template) {
		if(template!=null && getHolidays()!=null){
			template.setHolidays(getHolidays());
		}
				
	}

}

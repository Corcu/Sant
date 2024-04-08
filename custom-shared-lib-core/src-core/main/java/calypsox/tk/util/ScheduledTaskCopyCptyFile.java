package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

/**
 * Scheduled Task to initialize Interest & Inflation Rates.
 * 
 * @author Jose David Sevillano (josedavid.sevillano@siag.es) interface control by David Porras Mart?nez
 */
public class ScheduledTaskCopyCptyFile extends AbstractProcessFeedScheduledTask {
	private static final long serialVersionUID = 123L;

	private static final String TASK_INFORMATION = "Import Market Data Interest & Inflation Rates from a CSV file.";
	protected static final String PROCESS = "Load interest and inflation rates";
	private BufferedReader inputFileStream;
	private BufferedWriter outputFileStream;
	private String file = "";

	private boolean processOK = false;

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {
		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);
		JDate jdate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
		Vector holidays = getHolidays();

		String sentPath = path + "sent/";

		// We remove the extension of the filename, checking first the string length.
		String filenameWithoutExt = new String();
		if (!startFileName.isEmpty() && !"".equals(startFileName)) {
			filenameWithoutExt = startFileName.substring(0, startFileName.indexOf(".txt"));
		}

		final ArrayList<String> files = checkFile(sentPath, jdate.addBusinessDays(-1, holidays), filenameWithoutExt);
		String line, newLine;

		if ((files != null) && (files.size() > 0)) {
			this.file = files.get(0);
			final String fileSentPath = sentPath + this.file;
			final String fileExportPath = path + startFileName;

			// modify file
			try {
				this.inputFileStream = new BufferedReader(new FileReader(fileSentPath));
				this.outputFileStream = new BufferedWriter(new FileWriter(fileExportPath));

				for (@SuppressWarnings("unused")
				int i = 0; (line = this.inputFileStream.readLine()) != null; i++) {
					if (!line.startsWith("*****")) {
						// create new line from old line

						String[] fields = CollateralUtilities.splitMejorado(4, "|", false, line);
						newLine = jdate.toString();
						for (int j = 1; j < fields.length; j++) {
							newLine = newLine + "|" + fields[j];
						}
						newLine += "\n";
						// write new line
						this.outputFileStream.write(newLine);
					} else {
						// control line, change date
						String strMonth = String.valueOf(jdate.getMonth());
						if (strMonth.length() == 1) {
							strMonth = "0" + strMonth;
						}

						String strDay = String.valueOf(jdate.getDayOfMonth());
						if (strDay.length() == 1) {
							strDay = "0" + strDay;
						}

						String strDate = jdate.getYear() + "" + strMonth + "" + strDay;
						String ctrlLine = line.substring(0, 13) + strDate;
						this.outputFileStream.write(ctrlLine);
					}

				}

				// close files
				this.inputFileStream.close();
				this.outputFileStream.close();
				this.processOK = true;
			}

			catch (Exception e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error creating new file", e);
			}

		}

		return this.processOK;

	}

	public static ArrayList<String> checkFile(final String path, JDate date, String filenameWithoutExt) {
		final ArrayList<String> array = new ArrayList<String>();
		final File files = new File(path);
		final String[] listFiles = files.list();
		String strDate = "", strMonth = "", strDay = "";

		if ((null != date) && (listFiles != null) && (listFiles.length > 0)) {
			strMonth = String.valueOf(date.getMonth());
			if (strMonth.length() == 1) {
				strMonth = "0" + strMonth;
			}

			strDay = String.valueOf(date.getDayOfMonth());
			if (strDay.length() == 1) {
				strDay = "0" + strDay;
			}

			strDate = date.getYear() + "" + strMonth + "" + strDay;

			for (int i = 0; i < listFiles.length; i++) {
				if (listFiles[i].contains(strDate) && listFiles[i].contains(filenameWithoutExt)) {
					final String f = listFiles[i];
					array.add(f);
					break;
				}
			}
		}

		return array;
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}
}
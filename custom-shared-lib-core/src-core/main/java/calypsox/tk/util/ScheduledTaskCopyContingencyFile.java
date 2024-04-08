package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

/**
 * Copy previous business date file and update it in order to load it applying contingency action
 * 
 */
public class ScheduledTaskCopyContingencyFile extends AbstractProcessFeedScheduledTask {

	private static final long serialVersionUID = 123L;

	private static final String FIELD_SEPARATOR = "Field separator";
	private static final String MTM_DATE_FIELD_NUMBER = "MtM date field number";
	private static final String OUTPUT_DATE = "Output Date format(dd/MM/yyyy)";
	private static final String TASK_INFORMATION = "Copy previous business date file and update it in order to load it applying contingency action.";
	private BufferedReader inputFileStream;
	private BufferedWriter outputFileStream;
	private boolean processOK = true;

	//v14.4 GSM
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();attributeList.addAll(super.buildAttributeDefinition());
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(OUTPUT_DATE));
		attributeList.add(attribute(FIELD_SEPARATOR));
		attributeList.add(attribute(MTM_DATE_FIELD_NUMBER));	
	
		return attributeList;	
	}
	

//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(FIELD_SEPARATOR);
//		attr.add(MTM_DATE_FIELD_NUMBER);
//		return attr;
//	}

	@Override
	/** 
	 * Main process
	 */
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);
		String separator = getAttribute(FIELD_SEPARATOR);
		String mtmDateFieldNumber = getAttribute(MTM_DATE_FIELD_NUMBER);
		JDate fileDate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
		
		JDate outputfileDate;
		
		if (!Util.isEmpty(getAttribute(OUTPUT_DATE)) && isValidDate(getAttribute(OUTPUT_DATE), "dd/MM/yyyy")) {
			outputfileDate = JDate.valueOf(getAttribute(OUTPUT_DATE));
		} else {
			outputfileDate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
		}

		// get file using file last modified date
		File file = getFileToApplyContingency(path + "/copy/", startFileName, fileDate);
		if (file == null) {
			Log.error(this, "No file found to apply contingency process.\n");
			return false;
		}

		String line = null, updatedLine = null;

		// modify file
		try {
			this.inputFileStream = new BufferedReader(new FileReader(path + "/copy/" + file.getName()));
			this.outputFileStream = new BufferedWriter(new FileWriter(path + getOriginalFileName(file.getName())));

			for (; (line = this.inputFileStream.readLine()) != null;) {
				if (!line.startsWith("*****")) {
					// get updated line
					updatedLine = getUpdatedLine(line, separator, mtmDateFieldNumber, outputfileDate);
					// write updated line
					this.outputFileStream.write(updatedLine + "\n");
				} else {
					// get updated control line
					String updatedCtrlLine = getUpdatedControlLine(line, outputfileDate);
					// write updated control line
					this.outputFileStream.write(updatedCtrlLine);
				}
			}

			// close files
			this.inputFileStream.close();
			this.outputFileStream.close();

		}

		catch (Exception e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error creating new file", e);
			this.processOK = false;
		}

		return this.processOK;

	}

	/**
	 * Remove from filename "_date" to get original filename
	 * 
	 * @param fileName
	 * @return
	 */
	public String getOriginalFileName(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf("_"));
	}

	/**
	 * Update register line changing MTM date field by new date.
	 * 
	 * @param line
	 * @param separator
	 * @param mtmDateFieldNumber
	 * @param fileDate
	 * @return
	 */
	public String getUpdatedLine(String line, String separator, String mtmDateFieldNumber, JDate fileDate) {

		// update mtm_date field
		String updatedLine = "";
		try{
			String[] fields = line.split("\\" + separator, -1);
			fields[Integer.valueOf(mtmDateFieldNumber) - 1] = fileDate.toString();
			for (int j = 0; j < fields.length; j++) {
				if (j == (fields.length - 1)) {
					updatedLine += fields[j];
				} else {
					updatedLine += fields[j] + separator;
				}

			}
		}catch(ArrayIndexOutOfBoundsException ex){
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error Field separator: '"+ separator+"' it does not match.", ex);
		}
		
		return updatedLine;

	}
	
	public static boolean isValidDate(String fecha, String formato) {
		try {
			SimpleDateFormat formatoFecha = new SimpleDateFormat(formato, Locale.getDefault());
			formatoFecha.setLenient(false);
			formatoFecha.parse(fecha);
		} catch (ParseException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Bad output date format", e);
			return false;
		}
		return true;
	}

	/**
	 * Update control line changing date by new date.
	 * 
	 * @param line
	 * @param fileDate
	 * @return
	 */
	public String getUpdatedControlLine(String line, JDate fileDate) {

		// control line, change date
		String strMonth = String.valueOf(fileDate.getMonth());
		if (strMonth.length() == 1) {
			strMonth = "0" + strMonth;
		}

		String strDay = String.valueOf(fileDate.getDayOfMonth());
		if (strDay.length() == 1) {
			strDay = "0" + strDay;
		}
		String strDate = fileDate.getYear() + "" + strMonth + "" + strDay;

		return line.substring(0, 13) + strDate;

	}

	/**
	 * Get file to use in contingency process, generally last business date file (passing as date last business date).
	 * 
	 * @param path
	 * @param fileName
	 * @param date
	 * @return
	 */
	public File getFileToApplyContingency(String path, String fileName, JDate date) {

		final String fileNameFilter = fileName;
		// name filter
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				return fileName.startsWith(fileNameFilter);
			}
		};

		final File directory = new File(path);
		final File[] listFiles = directory.listFiles(filter);

		for (File file : listFiles) {

			final Long dateFileMilis = file.lastModified();
			final Date dateFile = new Date(dateFileMilis);
			final JDate jdateFile = JDate.valueOf(dateFile);

			if (JDate.diff(date, jdateFile) == 0) {
				return file;
			}

		}

		return null;

	}

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}
}
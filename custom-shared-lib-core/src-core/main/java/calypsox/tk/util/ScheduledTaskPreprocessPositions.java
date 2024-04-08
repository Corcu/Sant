/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.util.bean.CSVPositionsBean;
import calypsox.tk.util.exception.SantExceptionUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

/**
 * This task reads a positions file that may have same positions STATUS like PENDING, SETTLE; or positions TYPES like
 * REPO, BUYSELL, etc and compacts these positions into THEORICAL, summing their nominal quantities. Finally, it
 * generates the output compacted file with the name agreed in the attribute.
 * 
 * @author Guillermo Solano
 * @version 1.1.2; 04/07/2013, added discard COLLATERAL positions
 * 
 */
public class ScheduledTaskPreprocessPositions extends ScheduledTask {

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 15447854258L;
	// END OA OA 27/11/2013

	/*
	 * Name of this Schedule task
	 */
	private final static String TASK_INFORMATION = "Schedule Task Service to preprocess GD Positions file";
	
	/**
	 * Types of positions that must be not included in the preprocess ST (the same as giving them a 0 value).
	 */
	public static Collection<String> BLACK_LIST_POSITIONS_TYPES = new ArrayList<String>() {
		private static final long serialVersionUID = 660145948432567403L;
		{
			add("COLLATERAL"); // avoid duplicating the COLLATERAL position
		}
	};

	/**
	 * Available file extensions
	 */
	private static final List<String> FILE_EXTENSION_TYPES=new ArrayList<String>();
	static{
	    FILE_EXTENSION_TYPES.add("dat");
	    FILE_EXTENSION_TYPES.add("txt");
	    FILE_EXTENSION_TYPES.add("csv");
	}
	/* private constants */
	private final static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
	private static final String END_LINE_MARK = "*****";
	private static final String OK = "ok";
	private static final String APPLICATION_NAME = "Preprocess of positions application";

	/* CLASS VARIABLES */
	/**
	 * Point the summery log File.
	 */
	private File summeryLog;
	/**
	 * Points the preprocess file.
	 */
	private File preprocessFile;
	/**
	 * Points the input file to be processed
	 */
	private File inputFile;
	/**
	 * file and writer of the preprocess (new) file
	 */
	private FileWriter compactedPosFileFw;
	private PrintWriter compactedPosFilePw;
	/**
	 * file and pw to write the log
	 */
	private FileWriter summeryFw;
	private PrintWriter summeryPw;
	/**
	 * pointer to the read file
	 */
	private BufferedReader inputPosFileReader;
	/**
	 * Map of positions beans
	 */
	private Map<String, CSVPositionsBean> positionsMap;

	// Class to manage the different Exceptions.
	private SantExceptionUtil santExUtil;

	/**
	 * PREPROCESS SCHEDULED TASK OVERRIDE METHODS
	 */

	/**
	 * Main method to be executed in this Scheduled task
	 * 
	 * @param connection
	 *            to DS
	 * @param connection
	 *            to PS
	 * @return result of the process
	 */
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		// we start creating and opening the files for log and input, and creating the output file
		initPreprocessTask();

		// if any problem, add it to log and end execution
		if (!openWriteReadFiles()) {
			return false;
		}

		// read the original file and compact it in the positions map
		try {
			processInputFile();
			// end, close the log and the input file descriptor
			closeInputFileDescriptors();

		} catch (IOException e) {

			Log.error(APPLICATION_NAME, " Critical Error processing the input file" + this.inputFile.toString() + " \n"
					+ e.getLocalizedMessage());
			Log.error(this, e);//sonar
			return false;
		}

		// write the preprocessed file
		writePreprocessedFile();

		// close output
		closeOutputFileDescriptor();

		// move files and send the exceptions (if any) into the Task Station
		moveFileToOKFolder();

		return true;
	}

	/**
	 * Moves the input file to the OK folder and publish exceptions (if any) to the Task Station
	 */
	private void moveFileToOKFolder() {

		String filePath = super.getAttribute(DOMAIN_ATTRIBUTES.FILEPATH.getDesc());

		// if the slash has been forgotten
		if (!filePath.trim().endsWith("/")) {
			filePath = filePath + "/";
		}
		final String fileName = super.getAttribute(DOMAIN_ATTRIBUTES.FILENAME_1.getDesc());
		// files extensions
		String extension = super.getAttribute(DOMAIN_ATTRIBUTES.FILE_IN_EXTENSION.getDesc());
		// input file extension
		if (!extension.trim().contains(".")) {
			extension = "." + extension;
		}

		final String inputFileName = filePath + File.separator + fileName + extension;

		// If there are no errors, move the file to ok folder
		Log.info(this, "Feed has been processed successfully. No bad entries found.");
		final String outputFileName = filePath + OK + File.separator + fileName + "_" + getDateFormatted();

		try {
			FileUtility.moveFile(inputFileName, outputFileName);
		} catch (final IOException e) {
			Log.error(this, e.getMessage(), e);
		}

		// We save all the exceptions (if any) into the Task Station Window.
		this.santExUtil.publishTasks();

	}

	/**
	 * Process the input positions files, and compacts its due for its key. That means that for every SETTLED, PENDING,
	 * BUYSELL & REPO positions, we calculated and store only one position line
	 * 
	 * @throws IOException
	 */
	private void processInputFile() throws IOException {

		String line = this.inputPosFileReader.readLine();
		int counter = 1;

		while (line != null) {

			if (isEndLineOfFile(line)) {
				break;
			}

			final CSVPositionsBean bean = buildCSVPosition(line, counter++);

			// avoid preprocessing certain types of positions
			if (!excludePositionType(bean)) {

				// compacts the position bean
				compactPositions(bean);
			}

			line = this.inputPosFileReader.readLine();// read next line
		}
	}

	/**
	 * positions of the type BLACK_LIST_POSITIONS_TYPES must be excluded, like for example COLLATERALS. Adds log info
	 * 
	 * @param bean
	 *            that might be excluded.
	 * @return true if must be excluded, false otherwise
	 */
	private boolean excludePositionType(CSVPositionsBean bean) {

		final String posType = bean.getPosType();

		if (BLACK_LIST_POSITIONS_TYPES.contains(posType)) {

			final String lineToChange = bean.getLineNumber() + " " + bean.toString() + " @TO BE REMOVED!";
			final CSVPositionsBean processed = new CSVPositionsBean(bean);
			// put quantity to zero
			bean.setQtyNominalSec("0.0");
			// add log data
			generateLogChanges(processed, bean, lineToChange);

			return true; // must be excluded
		}

		return false;
	}

	/**
	 * If a position for a certain day is repeated, is compacted into one THEORICAL POSITION.
	 * 
	 * @param bean
	 *            that might be compacted into one position
	 */
	private void compactPositions(CSVPositionsBean bean) {

		if (bean == null) {
			return;
		}

		// unique key of a position Bean
		final String key = bean.getHashKey();

		if (this.positionsMap.containsKey(key)) {

			final CSVPositionsBean processed = this.positionsMap.get(key);

			final String lineToChange = processed.getLineNumber() + " " + processed.toString() + ":";

			final String newQuantity = processQuantity(processed.getQtyNominalSec(), bean.getQtyNominalSec());
			// add new compacted quantity
			processed.setQtyNominalSec(newQuantity);
			// add log data
			generateLogChanges(processed, bean, lineToChange);

		} else { // is a new Position, added to lines

			this.positionsMap.put(key, bean);
		}

	}

	/**
	 * Adds a log line when a position is compacted
	 * 
	 * @param processed
	 * @param bean
	 * @param lineToChange
	 */
	private void generateLogChanges(CSVPositionsBean processed, CSVPositionsBean bean, String lineToChange) {

		StringBuffer sb = new StringBuffer();
		sb.append(bean.toString());
		sb.append(" -> ").append(" ").append(processed);
		this.summeryPw.println(lineToChange);
		this.summeryPw.println(sb.toString());

	}

	/**
	 * Calculates the new quantity of a duplicate position that must be "compacted".
	 * 
	 * @param qtyNominalSec
	 * @param qtyNominalSec2
	 * @return
	 */
	private String processQuantity(String qtyNominalSec, String qtyNominalSec2) {

		Double initial = null;
		Double alpha = null;

		try {

			initial = Double.parseDouble(qtyNominalSec);
			alpha = Double.parseDouble(qtyNominalSec2);

		} catch (NumberFormatException e) {
			return "Number bad format";

		}

		final Double result = initial + alpha;
		final String resultFormatted = String.format("%.2f", result).replaceAll(",", ".");

		return resultFormatted;
	}

	/**
	 * Builds a CSVPosition bean from the line
	 * 
	 * @param line
	 * @return
	 */
	private CSVPositionsBean buildCSVPosition(String line, int lineNumber) {

		int fields = countFields(line, '|');

		if (!line.trim().endsWith("|")) {
			line = line + "|";
			fields++;
		}

		String[] values = CollateralUtilities.splitMejorado(fields, "|", true, line);
		String[] valuesOut = new String[14];

		for (int i = 0; i < valuesOut.length; i++) {
			valuesOut[i] = "";
		}

		for (int ii = 0; ii < values.length; ii++) {
			valuesOut[ii] = values[ii].trim();
		}

		return new CSVPositionsBean(valuesOut, lineNumber);

	}

	/**
	 * Writes the new compacted file using the format SUSI|BSTE|RMARQUES|US912828JB79|US TREASURY N/B T 3
	 * 1/2|05/31//0D/31/05/2013/3,5%|11/04/2013|NOM|123456789|USD|THEORETICAL|BUYSELL|Y|BNYN|015274| where all the
	 * positions will be THEORITICAL and BUYSELL
	 */
	private void writePreprocessedFile() {

		Collection<CSVPositionsBean> posCollection = this.positionsMap.values();

		for (CSVPositionsBean pos : posCollection) {

			final CSVPositionsBean current = pos;
			addGenericValuesPosBean(current);
			// write line compacted file
			this.compactedPosFilePw.println(pos.toString());
		}

		addControlLine(posCollection.size());

	}

	/**
	 * Compacted positions are stored as NOM; BUYSELL and THEORICAL positions
	 * 
	 * @param current
	 *            processed pos to be converted into generic
	 */
	private void addGenericValuesPosBean(CSVPositionsBean current) {
		// SUSI|BSTE|RMARQUES|US912828JB79|US TREASURY N/B T 3 1/2
		// 05/31//0D/31/05/2013/3,5%|11/04/2013|NOM|123456789|USD|THEORETICAL|BUYSELL|Y|BNYN|015274|
		// current.setQtyNom("NOM");
		current.setPosType("BUYSELL");
		current.setPosStatus("THEORETICAL");
	}

	/**
	 * Generates the line control and adds it to the file
	 * 
	 * @param size
	 *            or number of lines
	 */
	private void addControlLine(int size) {

		StringBuffer sb = new StringBuffer(END_LINE_MARK);
		sb.append(String.format("%08d", size));
		sb.append(timeFormat.format(getValuationDatetime().getJDate(TimeZone.getDefault()).getDate(this._timeZone)));
		// add to file
		this.compactedPosFilePw.println(sb.toString());
	}

	/**
	 * @return this task information, gathered from the constant TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {

		return TASK_INFORMATION;
	}

	/**
	 * Enum containing the domain attributes constants.
	 */
	public enum DOMAIN_ATTRIBUTES {
	    	
		FILEPATH("File Path",attribute("File Path")), // 1
		FILENAME_1("Input File Name:",attribute("Input File Name:")), // 2
		FILENAME_2("Output File Name:",attribute("Output File Name:")), // 3
		FILE_IN_EXTENSION("Input file extension",attribute("Input file extension").domain(FILE_EXTENSION_TYPES)), // 4
		FILE_OUT_EXTENSION("Output file extension",attribute("Output file extension").domain(FILE_EXTENSION_TYPES)), // 5
		USE_TIMESTAMP("Add Timestamp after File Name: ",attribute("Add Timestamp after File Name: ").booleanType()), // 6
		CONVERSION_LOG_SUMMARY("Log File Path & Name:",attribute("Log File Path & Name:")); // 7

		private final String desc;
		private final AttributeDefinition def;
		// add description
		private DOMAIN_ATTRIBUTES(String d,AttributeDefinition def) {
			this.desc = d;
			this.def=def;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

		public AttributeDefinition getDef(){
		    return this.def;
		}
		// list with domain values definitions
		public static List<AttributeDefinition> getDomainDef() {
			List<AttributeDefinition> a = new ArrayList<AttributeDefinition>();
			for (DOMAIN_ATTRIBUTES domain : DOMAIN_ATTRIBUTES.values()) {
				a.add(domain.getDef());
			}
			return a;

		}
	} // end ENUM

	  @Override
	    protected List<AttributeDefinition> buildAttributeDefinition() {
		return DOMAIN_ATTRIBUTES.getDomainDef();
	    }

	  
//	/**
//	 * @return a vector with all the domain attributes for this schedule task
//	 * 
//	 */
//	@SuppressWarnings("rawtypes")
//	@Override
//	public Vector getDomainAttributes() {
//
//		final Vector<String> result = new Vector<String>(DOMAIN_ATTRIBUTES.values().length);
//		result.addAll(DOMAIN_ATTRIBUTES.getDomainDescr());
//		return result;
//	}
//
//	/**
//	 * @param attribute
//	 *            name
//	 * @param hastable
//	 *            with the attributes declared
//	 * @return a vector with the values for the attribute name
//	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
//
//		Vector<String> vector = new Vector<String>();
//
//		if (attribute.equals(DOMAIN_ATTRIBUTES.FILE_IN_EXTENSION.getDesc())
//				|| attribute.equals(DOMAIN_ATTRIBUTES.FILE_OUT_EXTENSION.getDesc())) {
//
//			vector.add("txt");
//			vector.add("csv");
//			vector.add("dat");
//
//		} else if (attribute.equals(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc())) {
//
//			vector.add("yes");
//			vector.add("no");
//
//		} else {
//
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//		return vector;
//	}

	// ////////////////////////
	// // PRIVATE METHODS ////
	// //////////////////////

	/**
	 * Initialization for the log file and the writer service
	 * 
	 */
	private void initPreprocessTask() {

		// positions started
		this.positionsMap = new HashMap<String, CSVPositionsBean>();

		// start exception util
		this.santExUtil = new SantExceptionUtil(getDSConnection(), 0, this.getValuationDatetime(), getDatetime(),
				this.getUndoDatetime());

		// files startup
		this.summeryFw = null;
		this.summeryLog = null;
		this.summeryPw = null;
		this.compactedPosFilePw = null;
		this.inputFile = null;
		this.inputPosFileReader = null;

		// timemark
		final boolean timemark = (super.getAttribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc()) != null)
				&& super.getAttribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc()).equals("Yes") ? true : false;
		// files extensions
		String extension_in = super.getAttribute(DOMAIN_ATTRIBUTES.FILE_IN_EXTENSION.getDesc());
		String extension_out = super.getAttribute(DOMAIN_ATTRIBUTES.FILE_OUT_EXTENSION.getDesc());
		// dir and files names
		String dir = super.getAttribute(DOMAIN_ATTRIBUTES.FILEPATH.getDesc());
		final String fileNameIn = super.getAttribute(DOMAIN_ATTRIBUTES.FILENAME_1.getDesc());
		String fileNameOut = super.getAttribute(DOMAIN_ATTRIBUTES.FILENAME_2.getDesc());
		String filePathLog = super.getAttribute(DOMAIN_ATTRIBUTES.CONVERSION_LOG_SUMMARY.getDesc());

		if ((dir == null) || ((extension_in == null) | (extension_out == null))) {
			return;
		}

		// check filenames are different
		if (fileNameIn.equals(fileNameOut)) {
			fileNameOut = fileNameOut + "_NEW";
		}

		// if the slash has been forgotten
		if (!dir.trim().endsWith("/")) {
			dir = dir + "/";
		}
		// input file extension
		if (!extension_in.trim().contains(".")) {
			extension_in = "." + extension_in;
		}
		// input file extension
		if (!extension_out.trim().contains(".")) {
			extension_out = "." + extension_out;
		}

		// check log ends with extension
		if ((filePathLog != null) && !filePathLog.trim().contains(".")) {
			filePathLog = filePathLog + ".txt";
		}

		// check if timemark must be added
		String time = "";
		if (timemark) {
			time = "_" + getDateFormatted();
		}

		// build the files paths
		final String path1 = dir + fileNameIn;
		final String path2 = dir + fileNameOut;

		// create the Files
		this.inputFile = new File(path1 + time + extension_in);
		this.preprocessFile = new File(path2 + time + extension_in);
		this.summeryLog = new File(filePathLog);
	}

	/**
	 * opens the flow to write the log file and read the input file
	 */
	private boolean openWriteReadFiles() {

		// opens the positions GD input file
		try {

			this.inputPosFileReader = new BufferedReader(new FileReader(this.inputFile));

		} catch (FileNotFoundException e) {
			Log.error(APPLICATION_NAME, "Not possible to open the File to be processed. Does the file "
					+ this.inputFile.toString() + " exist? Check the configuration \n" + e.getLocalizedMessage());
			Log.error(this, e); //sonar
			return false;
		}

		// create the summery file and create printwriter
		try {

			this.summeryFw = new FileWriter(this.summeryLog.toString(), false);
			this.summeryPw = new PrintWriter(this.summeryFw, false);

		} catch (IOException e) {
			Log.error(
					APPLICATION_NAME,
					"Not possible to create the Log file. Does the folder exist? Check the configuration \n"
							+ e.getLocalizedMessage());
			Log.error(this, e); //sonar
			return false;
		}

		// create the output compacted file and create printwriter
		try {

			this.compactedPosFileFw = new FileWriter(this.preprocessFile.toString(), false);
			this.compactedPosFilePw = new PrintWriter(this.compactedPosFileFw, false);

		} catch (IOException e) {
			Log.error(APPLICATION_NAME,
					"Not possible to create the Output preprocessed file. Does the folder exist? Check the configuration \n"
							+ e.getLocalizedMessage());
			Log.error(this, e); //sonar
			return false;
		}

		return true;
	}

	/**
	 * Flushes the log file and close the file pointer
	 */
	private void closeOutputFileDescriptor() {

		if (this.compactedPosFilePw != null) {
			this.compactedPosFilePw.close();
		}

		if (this.compactedPosFileFw != null) {
			try {
				this.compactedPosFileFw.close();
			} catch (IOException e) {
				Log.error(this, e);
			}
		}
	}

	/**
	 * Flushes the log file and close the file pointer
	 */
	private void closeInputFileDescriptors() {

		if (this.summeryPw != null) {
			this.summeryPw.close();
		}

		if (this.summeryFw != null) {
			try {
				this.summeryFw.close();
			} catch (IOException e) {
				Log.error(this, e); //sonar
			}
		}

		if (this.inputPosFileReader != null) {
			try {
				this.inputPosFileReader.close();
			} catch (IOException e) {
				Log.error(this, e); //sonar
			}
		}
	}

	/**
	 * @returns a String with the format yyyyMMdd
	 */
	private synchronized String getDateFormatted() {
		return timeFormat.format(new Date());

	}

	/*
	 * returns true if reached end line
	 */
	private boolean isEndLineOfFile(String record) {

		return (record.substring(0, 5).equals(END_LINE_MARK));
	}

	/**
	 * just counts the number of appearances of the separator in the line
	 * 
	 * @param str
	 * @param separator
	 * @return
	 */
	private int countFields(final String str, final char separator) {
		int counter = 0;

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == separator) {
				counter++;
			}
		}
		return counter;
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

//import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * This schedule task creates or deletes EOF files, with or without timestamp appended at the end of the name.
 * 
 * @author eLab
 * @version 1.0
 * 
 */

public class ScheduledTaskEOFfiles extends ScheduledTask {

	// unique class id, important to avoid problems
	private static final long serialVersionUID = 1223986636762857194L;

	/**
	 * CONSTANTS DEFINITION 
	 */

	/**
	 * Enum containing the domain attributes constants.
	 */
	private enum DOMAIN_ATTRIBUTES {

		FILENAME("File Name Start:"), // 1
		IMPORT_DIR("Directory Path:"), // 2
		ACTION("Action create/delete:"), 
		USE_TIMESTAMP("Add date timestamp:"), 
		TIMESTAMP_FORMAT("Timestamp format:");

		private final String desc;

		// add description
		private DOMAIN_ATTRIBUTES(String d) {
			this.desc = d;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

	} // end ENUM DOMAIN_ATTRIBUTES
	/*--------------------------------------------------------------------------------*/

	/**
	 * Enum to define the oldest or the newest file timestamp
	 */
	private enum ENUM_ACTIONS {

		CREATE_EOF, // 1
		DELETE_EOF; // 2

		public static List<String> getNames() {
			ArrayList<String> n = new ArrayList<String>(ENUM_ACTIONS.values().length);
			for (ENUM_ACTIONS e : ENUM_ACTIONS.values())
				n.add(e.toString().toUpperCase());
			return n;
		}
	}
	

	/*----------------------------------------------------------------------------------------------*/
	/*
	 * Name of this Schedule task
	 */
	private final static String TASK_INFORMATION = "Creates or deletes signal files like .eof";

	// CLASS VARIABLES
	/**
	 * Keeps track of the ST attributes
	 */
	private Map<DOMAIN_ATTRIBUTES, String> domainValuesMap = null;
	/**
	 * path of the directory of the import
	 */
	private File importDirectory;

	// //////////////////////////////////////////////
	// //////// OVERRIDE METHODS ///////////////////
	// ////////////////////////////////////////////

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

		// read attributes
		
		initScheduleAttributesMap();

		if (checkDirectory()) {
			return false;
		}
		
		final String name = super.getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc());
		final String dir = super.getAttribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc());
		
		if (Util.isEmpty(name)){
			Log.error(this, "eof name is empty.");
			return false;
		
		} else if (Util.isEmpty(dir)){
			Log.error(this, "eof directory is empty.");
			return false;
		}
		

		if (conditionCreateEOF()) {
			
			File EOFfile = null;
			File Directory = null;
			// Calling Methods to CREATE the EOF file in the import dir given.
			
			Directory = new File(dir);
			EOFfile = createEOFfileToDirectory(name,Directory);
			File fichero = new File (EOFfile.getAbsolutePath());			
			try {
				  // A partir del objeto File creamos el fichero f?sicamente
				  if (fichero.createNewFile())
				    Log.info(this, "the file was created successfully");
				  else
					 Log.info(this, "Error. Could not create the file");
				} catch (IOException ioe) {
					 Log.error(this, ioe.getMessage());
					 Log.error(this, ioe); //sonar
					 return false;
				}


		} else { // condition delete eof
			// Calling Methods to DELETE the EOF file in the import dir given.
						
			/*
			 * Steps 1) Seek the file in the import dir given 2) If the file
			 * exist go to step 3 else thrown and exception saying that the file
			 * doesn't exit. 3) A Call to deleteFile has to be done
			 */

			File EOFfile = null;
			File Directory = null;
			
			Directory = new File(dir);
			EOFfile = createEOFfileToDirectory(name,Directory);
			if (EOFfile.exists()){
				deleteFile(EOFfile);
			}
			else{
				Log.error(this, "The file doesn't exist");
			}
			
		}

		return true;
	}

	/*---------------------------------------------------------------------------------------------*/
	private boolean conditionCreateEOF() {

		final String condition = this.domainValuesMap.get(DOMAIN_ATTRIBUTES.ACTION);
		if (!Util.isEmpty(condition)) {
			return condition.trim().equals(ENUM_ACTIONS.CREATE_EOF.toString().toUpperCase());
		}
		return false;
	}

	/*-----------------------------------------------------------------------------------------------*/
	// v14 Migration
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {

		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.ACTION.getDesc()).domain(ENUM_ACTIONS.getNames()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc()).booleanType());
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.TIMESTAMP_FORMAT.getDesc()));

		return attributeList;
	}

	/*-----------------------------------------------------------------------------------------------*/
	/**
	 * @return this task information, gathered from the constant
	 *         TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/*-----------------------------------------------------------------------------------------------*/
	/**
	 * Ensures that the attributes have a value introduced by who has setup the
	 * schedule task
	 * 
	 * @return if the attributes are ok
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

		boolean retVal = super.isValidInput(messages);

		for (DOMAIN_ATTRIBUTES attribute : DOMAIN_ATTRIBUTES.values()) {

			final String value = super.getAttribute(attribute.getDesc());

			if (Util.isEmpty(value)) {

				messages.addElement(attribute.getDesc() + " attribute is not specified.");
				retVal = false;
			}
		}

		return retVal;
	}
	/*-----------------------------------------------------------------------------------------------*/


	/*---------------------------------------------------------------------------------------------------------------*/
	/**
	 * @return true if any import directory error happened
	 */
	private boolean checkDirectory() {

		// read import dir
		this.importDirectory = new File(this.domainValuesMap.get(DOMAIN_ATTRIBUTES.IMPORT_DIR));
		// check how many files are on the directory
		return checkNumberFilesPerDirectory(this.importDirectory, DOMAIN_ATTRIBUTES.IMPORT_DIR);
	}

	/*----------------------------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @return true if the number of files expected for the directory is
	 *         incorrect
	 */
	private boolean checkNumberFilesPerDirectory(final File directory, final DOMAIN_ATTRIBUTES check) {

		if (!directory.exists()) {
			Log.error(ScheduledTaskEOFfiles.class, check.getDesc()
					+ " directory does NOT exist. Check attributes are correct and that the folder EXISTS.");
			return true;
		}

		final File[] listFilesAndDirectories = directory.listFiles();
		final List<File> listFiles = new ArrayList<File>(listFilesAndDirectories.length);

		// remove directories, no need to be checked
		for (File f : listFilesAndDirectories) {
			if (f.isFile()) {
				listFiles.add(f);
			}
		}
		// if empty, is ok for import directory, but error for the copying
		// directory
		if (listFiles.isEmpty()) {
			// import directory, then fine
			if (check.equals(DOMAIN_ATTRIBUTES.IMPORT_DIR)) {
				return false;
			}
			// has to be the copy, error:
			Log.error(ScheduledTaskEOFfiles.class, "ERROR: There NO files in the copy directory: " + check.getDesc()
					+ ". Check with Planification, no copy has been done!");
			return true;
		}

		// only one file, is ok for both
		else  {
			return false;
		}

//		return true; // will never reach this point
	}
	// Calling Methods to Create the EOF file in the import dir given.
	/*----------------------------------------------------------------------------------------------*/

	/*-----------------------------------------------------------------------------------------------*/
	private void deleteFile(final File file) {

		if (file.isFile()) {
			file.delete();
		}

	}
	/*This method seek a file given a fileName and a directory. It's return null if the file doesn't exit and
	 * return the File when it does.*/

	/**
	 * @param directory
	 *            to check
	 * @param filter
	 *            with the name of the file
	 * @return the last file (that accepts the filter) modified in time
	 */
	/*-----------------------------------------------------------------------------------------------*/
	/**
	 * Inserts the attributes on the map in pairs
	 * 
	 * @return fi the maps is fine
	 */
	private boolean initScheduleAttributesMap() {

		this.domainValuesMap = new HashMap<DOMAIN_ATTRIBUTES, String>();

		for (DOMAIN_ATTRIBUTES attribute : DOMAIN_ATTRIBUTES.values()) {

			final String value = super.getAttribute(attribute.getDesc());

			if (value.isEmpty()) {
				return true;
			}

			this.domainValuesMap.put(attribute, value);
		}
		return false;
	}

	/*-----------------------------------------------------------------------------------------------*/
	/* Create a new file given the directory and the name of such file. */
	public File createEOFfileToDirectory(String name, final File outputDirectory) {
		File outputEOFfile = null;

		if ((name == null) || (outputDirectory == null)) {
			return null;
		}

		if (outputDirectory.isDirectory()) {

			// give an opportunity to build path from import plus copy directory
			// name
			String path = outputDirectory.getAbsolutePath();
			name = constructFileName(name); //
			outputEOFfile = new File(path, name);
		}
		return outputEOFfile;
	}

	/*------------------------------------------------------------------------------------------------*/
	/*
	 * This methods check the fileName. First of all it checks if the file
	 * extension is given. If it's true, it respects it, if it's not true, it
	 * adds the eof extension to the fileName.
	 */
	private  String constructFileName(String fileName) {
		
		String extension = null;
		if (Util.isEmpty(fileName)) {
			Log.error(this, "filename is empty.");
			return null;
		}
		
		extension = getExtension(fileName);

		final String timeStamp = super.getAttribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc());
		Date date = super.getValuationDatetime().getJDate(TimeZone.getDefault()).getDate();
		final String dateFormat = super.getAttribute(DOMAIN_ATTRIBUTES.TIMESTAMP_FORMAT.getDesc());
		if (extension.equals("")) // if there's no extension, the extension has to																			// be .eof
		{
			if(timeStamp.equals("true")){
					
				final String str1 = fileName.substring(0, fileName.length());
				SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);    
				String dateWithFormat = sdf.format(date);
				String strDate = str1.concat(dateWithFormat);
				fileName = strDate.concat(".eof");
			} else if(timeStamp.equals("false")){
					final String str1 = fileName.substring(0, fileName.length());
					fileName = str1.concat(".eof");
				}
		} else if(extension.equals("eof")){
			
				if(timeStamp.equals("true")){
					String strwExt = fileName.substring(0, fileName.indexOf("."));
					SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);  
					String dateWithFormat = sdf.format(date);
					String strDate = strwExt.concat(dateWithFormat);
					String point = strDate.concat(".");
						fileName = point.concat(extension);
					}
		} else{
			Log.error(this, "Error. invalid format");
		}

		return fileName;
	}

	/*------------------------------------------------------------------------------------------------*/
	/* This methods returns the file extension. */
	public static String getExtension(String filename) {
		
		int index = filename.lastIndexOf('.');
		if (index == -1) {
			return "";
		} else {
			return filename.substring(index + 1);
		}
	}
	/*-----------------------------------------------------------------------------------------------*/
	/*This method check the attributes TimeStamp fileName and format*/
	public String getRealFilename(JDatetime valDatetime) {
		
		String fileName = getAttribute("File Name Start:");
		if (valDatetime != null) {
			String timeStamp = getAttribute("TIMESTAMP FILENAME");
			String timeFormat = getAttribute("TIMESTAMP FORMAT");
			if ((!Util.isEmpty(timeStamp)) && (timeStamp.equals("true"))) {
				String date = null;
				if (!Util.isEmpty(timeFormat)) {
					date = Util.datetimeToString(valDatetime, timeFormat, getTimeZone());
				} else {
					date = Util.dateToString(JDate.valueOf(valDatetime, getTimeZone()));
					date = date.replace('/', '-');
				}
				fileName = fileName + date;
			}
		}

		return fileName;
	}
	

}
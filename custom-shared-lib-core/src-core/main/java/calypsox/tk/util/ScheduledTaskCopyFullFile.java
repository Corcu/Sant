/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * This schedule task runs copies the FULL file to the import directory. Adds some logic to ensure that it is moved only
 * the last copy and that the import directory will have only the last copy
 * 
 * @author Guillermo Solano Mendez
 * @version 1.2, 08/08/2013, option to force to copy
 * 
 */

public class ScheduledTaskCopyFullFile extends ScheduledTask {

	// unique class id, important to avoid problems
	private static final long serialVersionUID = 1223986636762857194L;

	/**
	 * CONSTANTS DEFINITION COPY FULL FILE SCHEDULED TASK This section includes the constants to define the different
	 * attributes in the schedule task domain, as the possible values.
	 */
	/**
	 * Enum containing the domain attributes constants.
	 */
	private enum DOMAIN_ATTRIBUTES {

		FILENAME("File IMPORT Name Start:"), // 1
		IMPORT_DIR("IMPORT Directory Path:"), // 2
		COPY_DIR("COPY Directory:"), // 3
		ERASE_MORE_FILES("Erase old files:"), // 4
		FORCE_COPY("Force to copy snapshot:");// 5

		private final String desc;

		// add description
		private DOMAIN_ATTRIBUTES(String d) {
			this.desc = d;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

		// list with domain values descriptions
//		public static List<String> getDomainDescr() {
//			ArrayList<String> a = new ArrayList<String>(DOMAIN_ATTRIBUTES.values().length);
//			for (DOMAIN_ATTRIBUTES domain : DOMAIN_ATTRIBUTES.values()) {
//				a.add(domain.getDesc());
//			}
//			return a;
//
//		}
	} // end ENUM DOMAIN_ATTRIBUTES

	/**
	 * Enum to define the oldest or the newest file timestamp
	 */
	private enum FILE_TIME {

		OLDEST, // 1
		NEWEST; // 2
	}

	// PRIVATE CONSTANTS

	/*
	 * Yes or No response constants
	 */
	//private final static String[] BOOLEANS = new String[] { "Yes", "No" };

	/*
	 * Name of this Schedule task
	 */
	private final static String TASK_INFORMATION = "Copy Full File Schedule Task Service";

	// CLASS VARIABLES
	/**
	 * Keeps track of the ST attributes
	 */
	private Map<DOMAIN_ATTRIBUTES, String> domainValuesMap = null;
	/**
	 * path of the directory of the import
	 */
	private File importDirectory;
	/**
	 * path of the copy directory
	 */
	private File copyDirectory;

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

		/*
		 * checks if we have only one file in the import and copy files. If attribute is true, it will delete any old
		 * file and leaves the last one
		 */
		final boolean importDirErrors = checkImportDirectory();
		checkCopyDirectory();

		if (importDirErrors) {
			return false;
		}

		// if true, erase extra files if any exist
		if (attributeIsTrue(DOMAIN_ATTRIBUTES.ERASE_MORE_FILES)) {
			// note, directory are not considered
			leaveLastFileEraseOldOthers();
		}

		/*
		 * Copies the last file in the copy dir into the import file
		 */
		copyFullfileToImportDir();
		// everything went OK.
		return true;
	}
	
	//v14 Migration
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.COPY_DIR.getDesc()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.ERASE_MORE_FILES.getDesc()).booleanType());
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.FORCE_COPY.getDesc()).booleanType());
		
		return attributeList;	
	}

	/**
	 * @return a vector with all the domain attributes for this schedule task
	 * 
	 */
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
//		if (attribute.equals(DOMAIN_ATTRIBUTES.ERASE_MORE_FILES.getDesc())
//				|| attribute.equals(DOMAIN_ATTRIBUTES.FORCE_COPY.getDesc())) {
//
//			vector.addAll(Arrays.asList(BOOLEANS));
//
//		} else {
//
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//		return vector;
//	}

	/**
	 * @return this task information, gathered from the constant TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * Ensures that the attributes have a value introduced by who has setup the schedule task
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

	// /////////////////////////////////////////////
	// //////// PRIVATE METHODS ///////////////////
	// ///////////////////////////////////////////

	/**
	 * Looks for the file that matches the name. It takes the last file with this name from the copy directory. If the
	 * import has also this file, and the copy is newer, it will erase and copy it. If the import dir doesn't have any
	 * file, it will copy it into this directory.
	 */
	private void copyFullfileToImportDir() {

		// just check directories are fine
		if (!this.importDirectory.exists()) {
			Log.error(ScheduledTaskCopyFullFile.class, "Missing import directory. Stop copy process.");
			return;
		}

		if (!this.copyDirectory.exists()) {
			Log.error(ScheduledTaskCopyFullFile.class, "Missing import directory. Stop copy process.");
			return;

		}

		// filter to gather the files in each directory
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				return fileName.startsWith(ScheduledTaskCopyFullFile.this.domainValuesMap.get(
						DOMAIN_ATTRIBUTES.FILENAME).trim());
			}
		};

		final File[] filesImportDir = this.importDirectory.listFiles(filter);
		final File[] filesCopyDir = this.copyDirectory.listFiles(filter);

		// next BAU version - now we can have more than one files in copy (one per date) so we only throw error in case
		// of empty directory
		if ((filesCopyDir == null) || (filesCopyDir.length == 0)) {
			Log.error(ScheduledTaskCopyFullFile.class, "Missing copy file in copy directoy.\n");
		}

		// more than one file in import dir, this should not happen
		if ((filesImportDir == null) || (filesImportDir.length > 1)) {
			Log.error(ScheduledTaskCopyFullFile.class, "More than one file in import directory.\n");

		}
		// get last file in copy dir
		final File lastFileCopy = getLastModifiedFile(this.copyDirectory, filter);

		// if we want to copy and the import dir has the same file
		if (filesImportDir.length == 1) {
			// if force copy is enable, we just moved without considering if it is older or not
			if (attributeIsTrue(DOMAIN_ATTRIBUTES.FORCE_COPY)) {
				copyFileToDirectory(lastFileCopy, this.importDirectory);
				return;
			}
			// otherwise copy only if the copy is more new
			final File lastFileImport = getLastModifiedFile(this.importDirectory, filter);

			if (lastFileImport == null) {
				Log.info(ScheduledTaskCopyFullFile.class, "Missing file in Import dir " + this.importDirectory
						+ " .No erase will be done");
			}

			if (lastFileCopy == null) {
				Log.info(ScheduledTaskCopyFullFile.class, "Missing file in copy dir " + this.importDirectory
						+ " .No copy will be done");

			} else if ((lastFileImport != null) && (lastFileCopy.lastModified() > lastFileImport.lastModified())) {
				// delete last file from import dir
				lastFileImport.delete();
				// copy last file from /copy to /import
				copyFileToDirectory(lastFileCopy, this.importDirectory);
			}
			return;
		}

		// copy file from copy dir
		copyFileToDirectory(lastFileCopy, this.importDirectory);
	}

	/**
	 * @return true if any import directory error happened
	 */
	private boolean checkImportDirectory() {

		// read import dir
		this.importDirectory = new File(this.domainValuesMap.get(DOMAIN_ATTRIBUTES.IMPORT_DIR));
		// check how many files are on the directory
		return checkNumberFilesPerDirectory(this.importDirectory, DOMAIN_ATTRIBUTES.IMPORT_DIR);
	}

	/**
	 * @return true if any copy directory error happened
	 */
	private boolean checkCopyDirectory() {

		// read import dir
		this.copyDirectory = new File(this.domainValuesMap.get(DOMAIN_ATTRIBUTES.COPY_DIR));
		// might not be absolute path
		if (!this.copyDirectory.exists()) {

			// give an opportunity to build path from import plus copy directory name
			String path = this.domainValuesMap.get(DOMAIN_ATTRIBUTES.IMPORT_DIR).trim();
			if (!path.endsWith("/")) {
				path += "/";
			}
			String copy = this.domainValuesMap.get(DOMAIN_ATTRIBUTES.COPY_DIR).trim();
			if (copy.startsWith("/")) {
				copy.replaceFirst("//", "");
			}

			path += copy;
			this.copyDirectory = new File(path);
			if (!this.copyDirectory.exists()) {
				return true;
			}
		}
		// check how many files are on the directory
		return checkNumberFilesPerDirectory(this.copyDirectory, DOMAIN_ATTRIBUTES.COPY_DIR);
	}

	/**
	 * 
	 * @return true if the number of files expected for the directory is incorrect
	 */
	private boolean checkNumberFilesPerDirectory(final File directory, final DOMAIN_ATTRIBUTES check) {

		if (!directory.exists()) {
			Log.error(ScheduledTaskCopyFullFile.class, check.getDesc()
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
		// if empty, is ok for import directory, but error for the copying directory
		if (listFiles.isEmpty()) {
			// import directory, then fine
			if (check.equals(DOMAIN_ATTRIBUTES.IMPORT_DIR)) {
				return false;
			}
			// has to be the copy, error:
			Log.error(ScheduledTaskCopyFullFile.class,
					"ERROR: There NO files in the copy directory: " + check.getDesc()
							+ ". Check with Planification, no copy has been done!");
			return true;
		}

		// only one file, is ok for both
		if (listFiles.size() == 1) {
			return false;
		}

		// more than one, depends on the erase attribute
		if ((listFiles.size() > 1)) {
			if (attributeIsTrue(DOMAIN_ATTRIBUTES.ERASE_MORE_FILES)) {
				return false;

			} else {
				Log.error(ScheduledTaskCopyFullFile.class, check.getDesc() + " has MORE than ONE file. Put "
						+ DOMAIN_ATTRIBUTES.ERASE_MORE_FILES + " to Yes to delete extra files");
				return true;
			}
		}
		return true; // will never reach this point
	}

	/**
	 * Leaves only the last file modified in each directory, in case there is more than one
	 */
	private void leaveLastFileEraseOldOthers() {

		// from the import directory, remove all, keep the newest
		deleteExtraOldFiles(this.importDirectory, FILE_TIME.NEWEST);

		// next BAU version - we have to keep all files we got (one per date)
		// deleteExtraOldFiles(this.copyDirectory, FILE_TIME.NEWEST);

	}

	/**
	 * Deletes extra files on the directory, leaving only the last one that has been modified
	 * 
	 * @param directory
	 *            containing the files
	 * @param life
	 *            to chose with file should be kept (the NEWEST or the OLDEST).
	 * @see FILE_TIME
	 */
	private void deleteExtraOldFiles(final File directory, final FILE_TIME life) {

		Long fileTimeSelection = 0L;

		if (directory.exists() && directory.isDirectory()) {

			// filter to gather the files in each directory that starts with a concrete name
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File directory, String fileName) {
					return fileName.startsWith(ScheduledTaskCopyFullFile.this.domainValuesMap.get(
							DOMAIN_ATTRIBUTES.FILENAME).trim());
				}
			};

			final File[] listFiles = directory.listFiles(filter);
			// useless if only one file or no files at all
			if (oneOrZeroFiles(listFiles)) {
				return;
			}
			// take a sorted map to order files based on modification
			TreeMap<Long, File> sortedMap = new TreeMap<Long, File>();

			// we take all filenames of the directory and read their last modification time
			for (File file : listFiles) {

				if (file.isFile()) { // ensure not a directory
					long timeMil = file.lastModified();
					// rare, but in case two files have the same timestamp
					while (sortedMap.containsKey(timeMil)) {
						timeMil += 1L;
					}
					// add to map
					sortedMap.put(timeMil, file);
				}
			}

			// selection to keep the oldest or the newest file
			switch (life) {
			case OLDEST:
				// this was the oldest one modified
				fileTimeSelection = sortedMap.firstKey();
				break;
			default:
				// == case NEWEST:
				// this was the last one modified
				fileTimeSelection = sortedMap.lastKey();
				break;
			}

			sortedMap.remove(fileTimeSelection);

			// delete the rest of files
			for (File file : sortedMap.values()) {
				if (file.isFile()) {
					file.delete();
				}
			}
		}
	}

	/**
	 * @param directory
	 *            to check
	 * @param filter
	 *            with the name of the file
	 * @return the last file (that accepts the filter) modified in time
	 */
	// note, the latest modified file is the newest one!
	private File getLastModifiedFile(final File directory, FilenameFilter filter) {

		if (directory.exists() && directory.isDirectory()) {

			final File[] listFiles = directory.listFiles(filter);
			// useless if only one file
			if (listFiles.length < 1) {
				return null;
			}
			if (listFiles.length == 1) {
				if (listFiles[0].isDirectory()) {
					return null;
				}
				return listFiles[0];
			}
			// take a sorted map to order files based on modification
			TreeMap<Long, File> sortedMap = new TreeMap<Long, File>();

			// we take all filenames of the directory and read their last modification time
			for (File file : listFiles) {

				if (file.isDirectory()) { // looking only for files
					continue;
				}

				final long timeMil = file.lastModified();
				sortedMap.put(timeMil, file);
			}

			// this was the last one modified (newest)
			final Long fileTimeSelection = sortedMap.lastKey();
			return sortedMap.get(fileTimeSelection);
		}
		return null;
	}

	/**
	 * verifies there is only one file (not considering directories).
	 * 
	 * @param the
	 *            files to be checked that only one files remains
	 * 
	 */
	private boolean oneOrZeroFiles(File[] listFiles) {

		int count = 0;
		for (File file : listFiles) {

			if (file.isFile()) {
				count++;
				if (count > 1) {
					break;
				}
			}
		}
		// true if one or zero files in the list
		return (count < 2);
	}

	/**
	 * @param check
	 *            the attribute
	 * @return true if the attribute equals yes, false o.c.
	 */
	private boolean attributeIsTrue(DOMAIN_ATTRIBUTES check) {
		//v14
		return (getAttribute(check.getDesc()).equals(true));

//		if (this.domainValuesMap.get(check).equals(true)) {
//			return true;
//		}
//		return false;

	}

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

	/**
	 * Copy a file to another directory
	 * 
	 * @param inputFile
	 *            file
	 * @param outputDirectory
	 *            into which the file has to be copied
	 * @throws IOException
	 */
	public static void copyFileToDirectory(final File inputFile, final File outputDirectory) {

		if ((inputFile == null) || (outputDirectory == null)) {
			return;
		}

		if (inputFile.isFile() && outputDirectory.isDirectory()) {

			// give an opportunity to build path from import plus copy directory name
			String path = outputDirectory.getAbsolutePath();

			// next BAU version - remove from filename date in order to get original filename
			final File outputFile = new File(path, inputFile.getName().substring(0,
					inputFile.getName().lastIndexOf("_")));
			copyFile(inputFile, outputFile);
		}
	}

	/**
	 * Copy a file to another file
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public static void copyFile(final File inputFile, final File outputFile) {

		FileReader in = null;
		FileWriter out = null;
		try {

			try {

				in = new FileReader(inputFile);
				out = new FileWriter(outputFile);
				int c;
				while ((c = in.read()) != -1) {
					out.write(c);
				}
			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			}
		} catch (IOException e) { // bad
			Log.error(ScheduledTaskCopyFullFile.class, "ERROR: IO exception while copying file " + inputFile + " into "
					+ outputFile);
			Log.error(ScheduledTaskCopyFullFile.class, e); //sonar
		}
	}

}

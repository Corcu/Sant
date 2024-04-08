package calypsox.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;

public class FileUtility {

	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Create a PrintStream to manage a file
	 *
	 * @param fileName
	 *            FileName with path
	 * @param bAppend
	 *            true to add lines to an existing file, false to create a new file
	 * @return PrintStream to write in the file
	 * @throws Exception
	 */
	public static PrintStream getPrintStream(String fileName, boolean bAppend) throws Exception {
		Log.info("calypsox.util.FileUtility", "FileUtility.getPrintStream Start with fileName=" + fileName);
		PrintStream pStr = null;
		try {
			pStr = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName, bAppend)));
		} catch (Exception ex) {
			Log.error(Log.CALYPSOX, "FileUtility.getPrintStream Exception:", ex);
			throw new Exception("Error trying to open the file " + fileName + ":" + ex.getMessage());
		}
		Log.info("calypsox.util.FileUtility", "FileUtility.getPrintStream End");
		return pStr;
	}

	/**
	 * Copy a file to another directory
	 *
	 * @param inputFileName
	 * @param outputFileName
	 * @throws IOException
	 * @throws IOException
	 */
	public static void copyFile(String inputFileName, String outputFileName) throws IOException {
		Log.info("calypsox.util.FileUtility", "FileUtility.copyFile Start with inputFileName=" + inputFileName
				+ " and outputFileName=" + outputFileName);

		FileReader in = null;
		FileWriter out = null;
		try {
			File inputFile = new File(inputFileName);
			File outputFile = new File(outputFileName);

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

		Log.info("calypsox.util.FileUtility", "FileUtility.copyFile End");
	}

	/**
	 *
	 * @param dir
	 * @param name
	 * @return the File directory searched
	 */
	public static File findDirectoryIn(final File dir, final String name) {

		if ((dir == null) || Util.isEmpty(name)) {
			return null;
		}

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				final String criteria = name.trim();
				return fileName.equalsIgnoreCase(criteria);
			}
		};

		File[] directories = dir.listFiles(filter);
		// just to be sure, take the dir
		for (File f : directories) {
			if (f.isDirectory()) {
				return f;
			}
		}
		return null;
	}

	/**
	 * Moves a file to another directory. It is a two step(copy and delete) process.
	 *
	 * @param inputFileName
	 * @param outputFileName
	 * @throws IOException
	 *             throws IOExeption in case it can't copy or delete
	 */
	public static void moveFile(String inputFileName, String outputFileName) throws IOException {
		Log.info("calypsox.util.FileUtility", "moveFile Start with inputFileName=" + inputFileName
				+ " and outputFileName=" + outputFileName);

		FileUtility.copyFile(inputFileName, outputFileName);
		Log.info("calypsox.util.FileUtility", inputFileName + " has been copied to " + outputFileName);

		File fileToDelete = new File(inputFileName);
		boolean deleteSucceeded = fileToDelete.delete();
		if (!deleteSucceeded) {
			Log.error("calypsox.util.FileUtility", "Failed to delete file " + fileToDelete + " as part of move to "
					+ outputFileName);
			throw new IOException("Failed to delete file " + fileToDelete + " as part of move to " + outputFileName);
		}

		Log.info("calypsox.util.FileUtility", "moveFile End");
	}

	/**
	 * Copies a fileName (full path) into a directoryName (full path). For example: copy
	 * /calypso_interfaces/susi/import/exampleFile.txt to /calypso_interfaces/susi/import/copy/
	 *
	 * @param inputFileName
	 * @param outputDirectoryName
	 */
	public static void copyFileToDirectory(final String inputFileName, final String outputDirectoryName) {

		final File inputFile = new File(cleanSlashFileName(inputFileName.trim()));
		final File outputDirectory = new File(cleanSlashFileName(outputDirectoryName.trim()));

		// some checks
		if (!inputFile.exists()) {
			Log.error(FileUtility.class, inputFileName + " does NOT exist. Copy not possible.");
			return;
		}
		if (!outputDirectory.exists()) {
			Log.error(FileUtility.class, outputDirectory + " does NOT exist. Copy not possible.");
			return;
		}

		if (!inputFile.isFile()) {
			Log.error(FileUtility.class, inputFileName + " is not a File. Copy stopped.");
			return;
		}

		if (!outputDirectory.isDirectory()) {
			Log.error(FileUtility.class, outputDirectory + " is not a directory. Copy stopped.");
			return;
		}
		// ok, do copy
		copyFileToDirectory(inputFile, outputDirectory);

	}

	/**
	 * At String level, it replaces "//" to "/"
	 *
	 * @param name
	 *            to clean
	 * @return name with only one FS separator per level
	 */
	private static String cleanSlashFileName(String name) {

		// clean outputDirectory if it has more than one "/"
		if ((name == null) || name.isEmpty()) {
			return "";
		}

		name = name.replaceAll("//", "/");
		return name.replaceAll("\\\\\\\\", "\\\\");
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

			// BAU - updated to put in filename file date in order to keep historical files in copy
			// directory
			final Date d = new Date();
			String time = "";
			synchronized (timeFormat) {
				time = timeFormat.format(d);
			}

			final File outputFile = new File(outputDirectory.toString(), inputFile.getName() + "_" + time);
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
			Log.error("ScheduledTaskCopyFullFile", "ERROR: IO exception while copying file " + inputFile + " into "
					+ outputFile);
			Log.error("ScheduledTaskCopyFullFile", e); //sonar
		}
	}

}

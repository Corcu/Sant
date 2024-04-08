/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.interfaceImporter.ImportContext;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;

public class ScheduledTaskPDV_IMPORT extends AbstractProcessFeedScheduledTask {

	protected static final long serialVersionUID = 123L;

	protected static final String TASK_INFORMATION = "Import trades and allocations from PDV source";

	protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();

	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	protected static final String SEPARATOR_DOMAIN_STRING = "Separator";

	protected ImportContext context = null;

	private String file = "";

	
	@Override
	public Vector<String> getDomainAttributes() {
		@SuppressWarnings("deprecation")
		Vector<String> vectorAttr = super.getDomainAttributes();
		vectorAttr.add(SEPARATOR_DOMAIN_STRING);
		return vectorAttr;

	}

	@Override
	public String getFileName() {
		return this.file;
	}

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * Main method of the ST
	 */
	@Override
	public boolean process(DSConnection dsCon, PSConnection connPS) {

		boolean proccesOK = true;
		try {

			String path = getAttribute(FILEPATH);
			String startFileName = getAttribute(STARTFILENAME);

			// we add the header and assign the fileWriter to the logs files.
			// We check if the log files does'nt exist in the system. If it?s
			// the case then stop the process.
			String fileToProcess = getAndChekFileToProcess(path, startFileName);
			final String fullFileName = path + fileToProcess;

			if (!Util.isEmpty(fileToProcess)) {

				// Just after file verifications, this method will make a copy
				// into the
				// ./import/copy/ directory
				FileUtility.copyFileToDirectory(fullFileName, path + "/copy/");
				proccesOK = importFileContent(fullFileName,
						getValuationDatetime());
			} else {
				ControlMErrorLogger
						.addError(
								ErrorCodeEnum.InputFileNotFound,
								"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
				proccesOK = false;
			}

			// launch the post process
			feedPostProcess(proccesOK);
		} catch (Exception e) {
			ControlMErrorLogger.addError(
					ErrorCodeEnum.UndefinedException,
					new String[] {
							"Unexpected error while importing allocations",
							e.getMessage() });
			Log.error(ScheduledTaskPDV_IMPORT.class.getName(), e);
			proccesOK = false;
		} finally {

			if (proccesOK) {
				ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
			} else {
				ControlMErrorLogger.addError(
						ErrorCodeEnum.ScheduledTaskFailure, "");
			}
		}
		return proccesOK;
	}

	/**
	 * Optimize code to paralelized the exportation of trades in several
	 * threads.
	 * 
	 * @param fileToProcess
	 * @return
	 */
	@SuppressWarnings("unused")
	public boolean importFileContent(final String fileToProcess,
			JDatetime processingDate) {

		final int calculationOffSet = ServiceRegistry.getDefaultContext()
				.getValueDateDays() * -1;
		final JDate valuationDate = Holiday.getCurrent().addBusinessDays(
				processingDate.getJDate(TimeZone.getDefault()),
				DSConnection.getDefault().getUserDefaults().getHolidays(),
				calculationOffSet);

		context = new ImportContext();
		// getAttribute(SEPARATOR_DOMAIN_STRING),false);
		try {
			// context.init(processingDate, new JDatetime(valuationDate),
			// getPricingEnv());
		} catch (Exception e) {
			Log.error(this, e);
			return false;
		}

		//PDVImporter importer = new PDVImporter();

		return true;//importer.importExposureTrade(fileToProcess);
	}

	/**
	 * @param path
	 * @param startFileName
	 * @return the file name to import if every thing is okay (only one file
	 *         found as expected and the content of the file is correct)
	 */
	public String getAndChekFileToProcess(String path, String startFileName) {
		String fileToProcess = "";
		ArrayList<String> files = CollateralUtilities.getListFiles(path,
				startFileName);
		// We check if the number of matching files is 1.
		if (files.size() == 1) {
			fileToProcess = files.get(0);
			this.file = fileToProcess;

		} else {
			Log.error(
					ScheduledTaskPDV_IMPORT.class.getName(),
					"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
		}
		return fileToProcess;
	}

	// public List<Object> getInvalidItems() {
	// return context.getInvalidItems();
	// }

	/**
	 * @return the import context
	 */
	public ImportContext getImportContext() {
		return context;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		DSConnection ds = null;
		try {
			// Starts connection to DataServer.
			ds = ConnectionUtil.connect(args,
					ScheduledTaskPDV_IMPORT.class.getName());

			ScheduledTaskPDV_IMPORT st = new ScheduledTaskPDV_IMPORT();

			st.setAttribute(FILEPATH, "C://work//");
			st.setAttribute(STARTFILENAME, "PDV.txt");

			SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
			int i = Integer.valueOf(sdf.format(new JDatetime()));
			st.setValuationTime(i);
			st.setCurrentDate(JDate.getNow());

			st.process(DSConnection.getDefault(), null);

		} catch (Exception e) {
			Log.error(Log.CALYPSOX, e);
			return;
		} finally {
			DSConnection.logout();
			System.exit(0);
		}
	}
}

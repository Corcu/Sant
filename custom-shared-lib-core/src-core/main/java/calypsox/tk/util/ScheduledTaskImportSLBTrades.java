package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallsImporter;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

/**
 * Scheduled task to import exposure trades from a flat file in a paralellized
 * manner.
 * 
 */
public class ScheduledTaskImportSLBTrades extends
		AbstractProcessFeedScheduledTask {

	/**
	 * Serial UID
	 */
	protected static final long serialVersionUID = 123L;
	protected static final String TASK_INFORMATION = "Import Margin Call (Prestamo de valores)";

	protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();

	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	protected static final String SEPARATOR_DOMAIN_STRING = "Separator";

	// tmp attributes to tune the multi-threading of this scheduled task
	protected static final String NB_MARGINCALLS_MAPPERS = "Nb margin calls mappers";
	protected static final String NB_MARGINCALLS_PERSISTORS = "Nb margin calls persistors";
	protected static final String SIZE_MARGINCALLS_MAPPER_QUEUE = "Size MCs mapper queue";
	protected static final String SIZE_MARGINCALLS_PERSISTOR_QUEUE = "Size MCs persistor queue";
	protected static final String SIZE_PERSISTOR_BUFFER = "Size persistor buffer";
	protected static final String IS_SLB = "IS_SLB";
	
	protected static boolean isSLB = false;
	private String file = "";

	protected ExternalMarginCallImportContext context = new ExternalMarginCallImportContext(
			"|", true);

	/**
	 * ST Attributes Definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(NB_MARGINCALLS_MAPPERS));
		attributeList.add(attribute(NB_MARGINCALLS_PERSISTORS));
		attributeList.add(attribute(SIZE_MARGINCALLS_MAPPER_QUEUE));
		attributeList.add(attribute(SIZE_MARGINCALLS_PERSISTOR_QUEUE));
		attributeList.add(attribute(SIZE_PERSISTOR_BUFFER));
		attributeList.add(attribute(IS_SLB).booleanType());//Does it need this attribute

		return attributeList;
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

		Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Starting ST importation....");
		boolean proccesOK = true;

		try {
			String path = getAttribute(FILEPATH);
			String startFileName = getAttribute(STARTFILENAME);

			// we add the header and assign the fileWriter to the logs files.
			// We check if the log files does'nt exist in the system. If it?s
			// the case then stop the process.
			String fileToProcess = getAndChekFileToProcess(path, startFileName);
			if (!Util.isEmpty(fileToProcess)) {

				// Just after file verifications, this method will make a copy
				// into the ./import/copy/ directory
				FileUtility.copyFileToDirectory(path + fileToProcess, path
						+ "/copy/");
				
				// start multi-threading
				proccesOK = importFileContentMultiThreads(fileToProcess,
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
			ControlMErrorLogger
					.addError(
							ErrorCodeEnum.UndefinedException,
							new String[] {
									"Unexpected error while importing margin calls trades",
									e.getMessage() });
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
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
	public boolean importFileContentMultiThreads(final String fileToProcess,
			final JDatetime processingDate) {
		boolean processOk = false;
		String path = getAttribute(FILEPATH);
		final String fullFileName = path + fileToProcess;
		if(getAttribute(IS_SLB)!=null) {
			isSLB = (Boolean.TRUE.toString()).equals(getAttribute(IS_SLB)) ? true : false;//SLB
		}
		int numberMappers = 5;
		try {
			numberMappers = Integer
					.parseInt(getAttribute(NB_MARGINCALLS_MAPPERS));
		} catch (Exception e) {
			Log.error(this, e); // sonar
		}

		int numberPersistors = 5;
		try {
			numberPersistors = Integer
					.parseInt(getAttribute(NB_MARGINCALLS_PERSISTORS));
		} catch (Exception e) {
			Log.error(this, e); // sonar
		}

		int mapperQueueSize = 100;
		try {
			mapperQueueSize = Integer
					.parseInt(getAttribute(SIZE_MARGINCALLS_MAPPER_QUEUE));
		} catch (Exception e) {
			Log.error(this, e); // sonar
		}

		int persistorQueueSize = 100;
		try {
			persistorQueueSize = Integer
					.parseInt(getAttribute(SIZE_MARGINCALLS_PERSISTOR_QUEUE));
		} catch (Exception e) {
			Log.error(this, e); // sonar
		}

		// int saverBufferSize = 1;
		// try {
		// saverBufferSize = Integer
		// .parseInt(getAttribute(SIZE_PERSISTOR_BUFFER));
		// } catch (Exception e) {
		// Log.error(this, e); // sonar
		//
		// }
		// final int persistorBufferSize = saverBufferSize;

		// Get file input stream
		final FileInputStream fis = getFileInputStream(fullFileName);

		// Init the context
		initContext(processingDate);

		if (!Util.isEmpty(fileToProcess) && !Util.isEmpty(fullFileName)
				&& fis != null) {
			ExternalMarginCallsImporter importer = new ExternalMarginCallsImporter(
					this.context);
			
			processOk = importer.importFileMarginCalls(new JDatetime(),
					numberMappers, numberPersistors, mapperQueueSize,
					persistorQueueSize, persistorQueueSize, fis);
		}

		return processOk;
	}

	/**
	 * Init the context
	 * 
	 * @param processingDate
	 */
	private void initContext(final JDatetime processingDate) {
		final int calculationOffSet = ServiceRegistry.getDefaultContext()
				.getValueDateDays() * -1;
		final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
				processingDate.getJDate(TimeZone.getDefault()),
				DSConnection.getDefault().getUserDefaults().getHolidays(),
				calculationOffSet);

		String pricingEnv = (Util.isEmpty(getPricingEnv()) ? "DirtyPrice"
				: getPricingEnv());

		try {
			this.context.init(processingDate, new JDatetime(valuatioDate,
					TimeZone.getDefault()), pricingEnv);
		} catch (Exception e) {
			Log.error(this, e); // sonar
		}
	}

	/**
	 * Return FileInputStream from file name.
	 * 
	 * @param filename
	 * @return
	 */
	private FileInputStream getFileInputStream(final String filename) {
		File file = new File(filename);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Log.error(this, e); // sonar
		}

		return fis;
	}

	/**
	 * @param path
	 * @param startFileName
	 * @return the file name to import if every thing is okay (only one file
	 *         found as expected and the content of the file is correct)
	 */
	private String getAndChekFileToProcess(String path, String startFileName) {
		String fileToProcess = "";
		ArrayList<String> files = CollateralUtilities.getListFiles(path,
				startFileName);
		// We check if the number of matching files is 1.
		if (files.size() == 1) {
			fileToProcess = files.get(0);
			this.file = fileToProcess;

		} else {

			Log.error(
					LOG_CATEGORY_SCHEDULED_TASK,
					"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");

		}
		return fileToProcess;
	}

}

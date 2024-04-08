package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

import calypsox.ErrorCodeEnum;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportContext;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImporter;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

/**
 * Scheduled task to import allocations from a flat file.
 * 
 * @author aela
 * 
 */
public class ScheduledTaskOPTIM_ALLOC_IMPORT extends
		AbstractProcessFeedScheduledTask {

	protected static final long serialVersionUID = 123L;

	protected static final String TASK_INFORMATION = "Import Allocations from the optimizer file.";

	protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();

	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	protected static final String SEPARATOR_DOMAIN_STRING = "Separator";

	// tmp attributes to tune the multi-threading of this scheduled task
	protected static final String NB_ALLOCS_MAPPERS = "Nb allocs mappers";

	protected static final String NB_ALLOCS_PERSISTORS = "Nb allocs persistors";

	protected static final String SIZE_ALLOCS_MAPPER_QUEUE = "Size allocs mapper queue";

	protected static final String SIZE_ALLOCS_PERSISTOR_QUEUE = "Size allocs persistor queue";

	protected static final String SIZE_PERSISTOR_BUFFER = "Size persistor buffer";
	
	protected OptimAllocsImportContext context = null;

	private String file = "";
	
	/**
	 * ST Attribute Definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(NB_ALLOCS_MAPPERS));
		attributeList.add(attribute(NB_ALLOCS_PERSISTORS));
		attributeList.add(attribute(SIZE_ALLOCS_MAPPER_QUEUE));
		attributeList.add(attribute(SIZE_ALLOCS_PERSISTOR_QUEUE));
		attributeList.add(attribute(SIZE_PERSISTOR_BUFFER));

		return attributeList;
	}
	
//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//
//		Vector<String> vectorAttr = super.getDomainAttributes();
//		vectorAttr.add(SEPARATOR_DOMAIN_STRING);
//		vectorAttr.add(NB_ALLOCS_MAPPERS);
//		vectorAttr.add(NB_ALLOCS_PERSISTORS);
//		vectorAttr.add(SIZE_ALLOCS_MAPPER_QUEUE);
//		vectorAttr.add(SIZE_ALLOCS_PERSISTOR_QUEUE);
//		vectorAttr.add(SIZE_PERSISTOR_BUFFER);
//
//		return vectorAttr;
//
//	}

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
				proccesOK = importFileContentMultiThreads(fullFileName,
						getValuationDatetime());
			}
			else {
				ControlMErrorLogger
						.addError(
								ErrorCodeEnum.InputFileNotFound,
								"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
				proccesOK = false;
			}

			// launch the post process
			feedPostProcess(proccesOK);
		}
		catch (Exception e) {
			ControlMErrorLogger.addError(
					ErrorCodeEnum.UndefinedException,
					new String[] {
							"Unexpected error while importing allocations",
							e.getMessage() });
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
			proccesOK = false;
		}
		finally {

			if (proccesOK) {
				ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
			}
			else {
				ControlMErrorLogger.addError(
						ErrorCodeEnum.ScheduledTaskFailure, "");
			}
			// Control M messages
			// this.tradeImportTracker.flushControlMMessages(proccesOK);
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
	@SuppressWarnings("deprecation")
	public boolean importFileContentMultiThreads(final String fileToProcess,
			JDatetime processingDate) {

		final int calculationOffSet = ServiceRegistry.getDefaultContext()
				.getValueDateDays() * -1;
		final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(
				processingDate.getJDate(TimeZone.getDefault()),
				DSConnection.getDefault().getUserDefaults().getHolidays(),
				calculationOffSet);

		context = new OptimAllocsImportContext(
				getAttribute(SEPARATOR_DOMAIN_STRING), false);
		try {
			context.init(processingDate, new JDatetime(valuatioDate),
					getPricingEnv());
		}
		catch (Exception e) {
			Log.error(this, e);
			return false;
		}

		int numberMappers = 5;
		try {
			numberMappers = Integer.parseInt(getAttribute(NB_ALLOCS_MAPPERS));
		}
		catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int numberPersistors = 5;
		try {
			numberPersistors = Integer
					.parseInt(getAttribute(NB_ALLOCS_PERSISTORS));
		}
		catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int mapperQueueSize = 100;
		try {
			mapperQueueSize = Integer
					.parseInt(getAttribute(SIZE_ALLOCS_MAPPER_QUEUE));
		}
		catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int persistorQueueSize = 100;
		try {
			persistorQueueSize = Integer
					.parseInt(getAttribute(SIZE_ALLOCS_PERSISTOR_QUEUE));
		}
		catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int saverBufferSize = 100;
		try {
			saverBufferSize = Integer
					.parseInt(getAttribute(SIZE_PERSISTOR_BUFFER));
		}
		catch (Exception e) {
			Log.error(this, e); //sonar
		}

		final int persistorBufferSize = saverBufferSize;

		OptimAllocsImporter importer = new OptimAllocsImporter(context);

		return importer.importFileAllocations(fileToProcess, processingDate,
				new JDatetime(valuatioDate), numberMappers, numberPersistors,
				mapperQueueSize, persistorQueueSize, persistorBufferSize);
	}

	/**
	 * @param path
	 * @param startFileName
	 * @return the file name to import if every thing is okay (only one file
	 * found as expected and the content of the file is correct)
	 */
	public String getAndChekFileToProcess(String path, String startFileName) {
		String fileToProcess = "";
		ArrayList<String> files = CollateralUtilities.getListFiles(path,
				startFileName);
		// We check if the number of matching files is 1.
		if (files.size() == 1) {
			fileToProcess = files.get(0);
			this.file = fileToProcess;

		}
		else {
			Log.error(
					LOG_CATEGORY_SCHEDULED_TASK,
					"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
		}
		return fileToProcess;
	}
	
	public List<Object> getInvalidItems(){
		return context.getInvalidItems();
	}
	
	/**
	 * @return the import context
	 */
	public OptimAllocsImportContext getImportContext() {
		return context;
	}
}

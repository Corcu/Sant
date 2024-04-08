package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import calypsox.ErrorCodeEnum;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.persistor.InterfacePDVTradePersistor;
import calypsox.tk.collateral.pdv.importer.processor.InterfacePDVFileProcessor;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.tk.util.interfaceImporter.*;
import calypsox.util.FileUtility;
import calypsox.util.InterfaceTradeAndPLMarks;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

/**
 * Scheduled task to import exposure trades from a flat file in a paralellized manner.
 * 
 * @author aela & GSM
 * @version 2.4
 * 
 */
public class ScheduledTaskImportCSVExposureTrades extends AbstractProcessFeedScheduledTask {

	/**
	 * Serial UID
	 */
	protected static final long serialVersionUID = 123L;
	private static boolean isPDV = false;
	private static boolean classNew = false;
	private static boolean useTradesCache = false;

	/*
	 * GSM: Adaptation to use 34 or 41 fields file options. It might happen that we'll have two versions for a time.
	 * This implementation allows to have flat fields formats.
	 */
	public enum FILES_TYPES {
		OLD(34),
		// GSM: 22/08/13. Added the 7? field for Port. Reconciliation
		ORIGINAL(42),
		// GSM: 24/04/2014. PdV adaptation in exposure importation (2 more fields)
		//UPI IM
		NEW(47);
		//SLB
	
		private final int numberOfAttributes;

		private FILES_TYPES(int i) {
			this.numberOfAttributes = i;
		}

		public int getNumberOfFields() {
			return this.numberOfAttributes;
		}
	}

	/* true to allow lines with more than 34 fields */
	public static final boolean ALLOW_FILES_WITH_MORE_FIELDS = true;
	
	private static final String SOURCE_SYSTEM = "SOURCE_SYSTEM";
	private static final String PROCESS_EXPOSURE_TRADES = "EXPOSURE_TRADES";
	private static final String SUBJECT = "Log files for Exposure Trades Import on " + Defaults.getEnvName();
	private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	// GSM: Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
	protected final static String FILTER_EXCLUSION_TYPE = "Filters Exclusion MODE:";
	private final static String NAMES_DECLARED_DV = "SDF Names Declared in DV";
	private final static String USE_NAME_PREFIX = "Use SDF Start With Prefix";
	// GSM: 07/03/2014 - Added modification to allow PdV: not receiving control line
	private final static String DO_NOT_USE_CONTROL_LINE = "Do not use Control Line";

	protected static final String STATIC_DATA_LOG = "Static Data Log";
	protected static final String DESTINATION_EMAIL = "Destination email";
	protected static final String TASK_INFORMATION = "Import TRADES from a CSV file.";
	protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	protected static final String BUY_DIRECTION_STRING = "BUY";
	protected static final String SELL_DIRECTION_STRING = "SELL";
	protected static final String GIVE_DIRECTION = "GIVE";
	protected static final String RECEIVE_DIRECTION = "RECEIVE";
	protected static final String PROCESS = "Load of Exposure Trades from GBO";
	protected static final String SEPARATOR_DOMAIN_STRING = "Separator";

	// tmp attributes to tune the multithreding of this scheduled task
	protected static final String NB_TRADE_CACHE_WORKERS = "Nb trade cache workers";
	protected static final String NB_TRADE_MAPPERS = "Nb trade mappers";
	protected static final String NB_TRADE_PERSISTORS = "Nb trade persistors";
	protected static final String SIZE_TRADE_MAPPER_QUEUE = "Size trade mapper queue";
	protected static final String SIZE_TRADE_PERSISTOR_QUEUE = "Size trade persistor queue";
	protected static final String SIZE_PERSISTOR_BUFFER = "Size persistor buffer";
	protected static final String IMPORT_TYPE = "Import Reason";
	
	public static Boolean USE_FULL_DV_FILTERS_NAMES = Boolean.FALSE;
	private static final String IS_PDV = "IS_PDV";
	private static final String IS_SLB = "IS_SLB";
	private static final String CLASS_NEW = "DUP CONUK";
	private static final String OPT_COLUMNS = "OPT_COLUMNS";
	
	public static String USE_TRADES_CACHE_BLOCKS = "Cache trades in BLOCKS";

	protected List<String> recipientsEmails = null;
	private RemoteSantCollateralService remoteSantColService = null;
	private String file = "";
	private boolean useControlLine = true;

	private static final String DEFAULT_SOURCE_SYSTEM = "GBO";
	protected final ImportContext context = new ImportContext();
	protected InterfaceImportExecutor<TradeImportStatus, TradeImportStatus> fileLogWriter = null;
	
	//private HashMap <String,Trade> tradesMap = new HashMap <String,Trade>();
	private Map<String,SantTradeContainer> tradesCacheMap = null;

	private TradeImportTracker tradeImportTracker = null;
	
	public TradeImportTracker getTradeImportTracker() {
		return tradeImportTracker;
	}

	public void setTradeImportTracker(TradeImportTracker tradeImportTracker) {
		this.tradeImportTracker = tradeImportTracker;
	}

	protected CollateralServiceRegistry collateralServiceRegistry = ServiceRegistry.getDefault();

	/**
	 * ST Attributes Definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(SOURCE_SYSTEM));
		attributeList.add(attribute(IMPORT_TYPE));
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(FULL_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));
		attributeList.add(attribute(DESTINATION_EMAIL));
		attributeList.add(attribute(USE_TRADES_CACHE_BLOCKS).booleanType());
		attributeList.add(attribute(NB_TRADE_CACHE_WORKERS));
		attributeList.add(attribute(NB_TRADE_MAPPERS));
		attributeList.add(attribute(NB_TRADE_PERSISTORS));
		attributeList.add(attribute(SIZE_TRADE_MAPPER_QUEUE));
		attributeList.add(attribute(SIZE_TRADE_PERSISTOR_QUEUE));
		attributeList.add(attribute(SIZE_PERSISTOR_BUFFER));
		attributeList.add(attribute(FILTER_EXCLUSION_TYPE)
				.domain(new ArrayList<String>(Arrays.asList(new String[] { NAMES_DECLARED_DV, USE_NAME_PREFIX }))));
		attributeList.add(attribute(DO_NOT_USE_CONTROL_LINE).booleanType());
		attributeList.add(attribute(IS_PDV).booleanType());
		attributeList.add(attribute(IS_SLB).booleanType());
		attributeList.add(attribute(CLASS_NEW).booleanType());
		attributeList.add(attribute(OPT_COLUMNS).booleanType());
		
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
		Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Starting CSA importation....");
		boolean proccesOK = true;
		try {

			// init the ST context
			proccesOK = init(dsCon);
			
			if (!proccesOK) {
				if (this.context != null) {
					this.context.stopLogWriterProcess();
				}
				return false;
			}
			classNew = (Boolean.TRUE.toString()).equals(getAttribute(CLASS_NEW)) ? true : false;//classNew
			String path = getAttribute(FILEPATH);
			String startFileName = getAttribute(STARTFILENAME);
			String useDVFilter = getAttribute(FILTER_EXCLUSION_TYPE);
			// GSM: 07/03/2014 - Added modification to allow PdV: not receiving control line
			this.useControlLine = (getAttribute(DO_NOT_USE_CONTROL_LINE) == null)
					|| getAttribute(DO_NOT_USE_CONTROL_LINE).equalsIgnoreCase("No");

			// GSM: Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
			/*
			 * caches the SDFilters use for exclusions. This allows to avoid some rare problems that ocurrer when a
			 * custom SDF is declared in the DV but the code is not included (was skipping some trades).
			 */
			InterfaceTradeMapper.cacheExclusionSDFilters();

			// check which option it must be used (prefix name or DV names for the SDF).
			if (!Util.isEmpty(useDVFilter)) {
				USE_FULL_DV_FILTERS_NAMES = useDVFilter.contains(NAMES_DECLARED_DV);
			}

			// we add the header and assign the fileWriter to the logs files.
			// We check if the log files does'nt exist in the system. If it?s
			// the case then stop the process.
			String fileToProcess = getAndChekFileToProcess(path, startFileName);
			if (!Util.isEmpty(fileToProcess)) {

				// Just after file verifications, this method will make a copy into the
				// ./import/copy/ directory
				FileUtility.copyFileToDirectory(path + fileToProcess, path + "/copy/");

				this.tradeImportTracker.setFileBeingImported(fileToProcess);
				this.tradeImportTracker.setFileBeingImportedFullPath(path);
				
				/*
				 * JAT & GSM 02/05/2016 - Preload all trades in cache
				 */
				useTradesCache = (Boolean.TRUE.toString()).equals(getAttribute(USE_TRADES_CACHE_BLOCKS)) ? true : false;
				this.context.setUseCacheTrades(useTradesCache);
				if (useTradesCache){
					
					tradesCacheMap = preProcessTradesCache(fileToProcess);
					
					if (tradesCacheMap == null){
						ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeRead,
								"Cannot read file for process trades cache. Please fix the problem");
						this.context. markThreadsAsFalse();
						return false;
					}
				}
				//start multi-threading
				proccesOK = importFileContentMultiThreads(fileToProcess);
				
				
				if (isPDV) {
					TaskArray tasks = new TaskArray();
					tasks.add(PDVUtil.getTasksToPublish(this.tradeImportTracker));
					if (tasks != null && tasks.size() > 0) {
						DSConnection.getDefault().getRemoteBackOffice().saveAndPublishTasks(tasks, 0, null);
					}
				}
			} else {
				ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
						"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
				proccesOK = false;
				this.context.stopLogWriterProcess(); // DPM - 4.5 - avoid unlimited wait in next while
			}
		} catch (Exception e) {
			ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException, new String[] {
					"Unexpected error while importing trades", e.getMessage() });
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
			proccesOK = false;
		} finally {
			try {
				ArrayList<String> attachments = new ArrayList<String>();
				if (this.context != null) {
					// stop the log writer in case an error happend before going inside the import process
					this.context.stopTradePersistorProcess();

					while (this.context.isLogWriterRunning()) {
						Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
								"Waiting for logger to finish flusing info into log files");
						Thread.sleep(10000);
					}
					// flush log files
					attachments = this.context.getTradeImportTracker().getLogFiles();
					// send log files by email
				}
				// clear the pricingEnv caches used for this ST
				if (this.remoteSantColService != null) {
					this.remoteSantColService.clearPricingEnvCache();
				}

				// init the recipients from the domainValue
				List<String> to = this.recipientsEmails;

				if (Util.isEmpty(to)) {
					to = new ArrayList<String>();
				}
				// add the scheduledTask attribute emails
				String emails = getAttribute(DESTINATION_EMAIL);
				if (!Util.isEmpty(emails)) {
					to.addAll(Arrays.asList(emails.split(";")));
				}
				try {
					if (!Util.isEmpty(to) && !isPDV) {
						CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
					}
				} catch (Exception e) {
					// do nothing, consider that the ScheduledTask is OK
					Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
				}
				// launch the post process
				feedPostProcess(proccesOK);

			} catch (RemoteException e) {

				ControlMErrorLogger
						.addError(ErrorCodeEnum.PricingEnviroment, "Unable to clear remote pricingEnv cache");
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
				proccesOK = false;
			} catch (Exception e) {
				ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException, new String[] {
						"Unexpected error while importing trades", e.getMessage() });
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
				proccesOK = false;
			}
			// Control M messages
			this.tradeImportTracker.flushControlMMessages(proccesOK);
		}
		
		return proccesOK;
	}

	/**
	 * Optimize code to paralelized the exportation of trades in several threads.
	 * 
	 * @param fileToProcess
	 * @return
	 */
	public boolean importFileContentMultiThreads(final String fileToProcess) {

		String path = getAttribute(FILEPATH);
		isPDV = (Boolean.TRUE.toString()).equals(getAttribute(IS_PDV)) ? true : false;
		final Boolean isSLB = (Boolean.TRUE.toString()).equals(getAttribute(IS_SLB)) ? true : false;//SLB
		final String fullFileName = path + fileToProcess;

		int numberMappers = 5;
		try {
			numberMappers = Integer.parseInt(getAttribute(NB_TRADE_MAPPERS));
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int numberPersistors = 5;
		try {
			numberPersistors = Integer.parseInt(getAttribute(NB_TRADE_PERSISTORS));
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int mapperQueueSize = 100;
		try {
			mapperQueueSize = Integer.parseInt(getAttribute(SIZE_TRADE_MAPPER_QUEUE));
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int persistorQueueSize = 100;
		try {
			persistorQueueSize = Integer.parseInt(getAttribute(SIZE_TRADE_PERSISTOR_QUEUE));
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}

		int saverBufferSize = 1;
		try {
			saverBufferSize = Integer.parseInt(getAttribute(SIZE_PERSISTOR_BUFFER));
		} catch (Exception e) {
			Log.error(this, e); //sonar

		}

		final int persistorBufferSize = saverBufferSize;

		BlockingQueue<InterfaceTradeBean> recordsList = new LinkedBlockingQueue<InterfaceTradeBean>(mapperQueueSize);
		BlockingQueue<InterfaceTradeAndPLMarks> calypsoTradesAndPlMarksList = new LinkedBlockingQueue<InterfaceTradeAndPLMarks>(
				persistorQueueSize);
		
		// start the file reader
		InterfaceImportExecutor<InterfaceTradeBean, InterfaceTradeBean> fileReaderExecutor = new InterfaceImportExecutor<InterfaceTradeBean, InterfaceTradeBean>(
				1, null, recordsList, this.context,
				new ImportExecutorFactory<InterfaceTradeBean, InterfaceTradeBean>() {

					@Override
					public ImportExecutor<InterfaceTradeBean, InterfaceTradeBean> createExecutor(
							BlockingQueue<InterfaceTradeBean> inWorkQueue,
							BlockingQueue<InterfaceTradeBean> outWorkQueue, ImportContext context) {
						if(classNew) {
							if (isSLB) {
								return new InterfaceFileProcessor(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine, isSLB);
							} else if (!isPDV) {
								// GSM: 07/03/2014 - Added modification to allow PdV: not receiving control line
								return new InterfaceFileProcessor(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine);
							} else {//isPDV
								return new InterfacePDVFileProcessor(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine);
							}
						} else{
							if (isSLB) {
								return new InterfaceFileProcessor_old(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine, isSLB);
							} else if (!isPDV) {
								// GSM: 07/03/2014 - Added modification to allow PdV: not receiving control line
								return new InterfaceFileProcessor_old(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine);
							} else {//isPDV
								return new InterfacePDVFileProcessor(new File(fullFileName), null, outWorkQueue, context,
										ScheduledTaskImportCSVExposureTrades.this.useControlLine);
							}
						}
					}

					@Override
					public String getName() {
						return "FileReaderExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return ScheduledTaskImportCSVExposureTrades.this.context.getFileReaderWaitQueue();
					}

				});
		// start the trade mapper
		InterfaceImportExecutor<InterfaceTradeBean, InterfaceTradeAndPLMarks> tradeMapperExecutor = new InterfaceImportExecutor<InterfaceTradeBean, InterfaceTradeAndPLMarks>(
				numberMappers, recordsList, calypsoTradesAndPlMarksList, this.context,
				new ImportExecutorFactory<InterfaceTradeBean, InterfaceTradeAndPLMarks>() {

					@Override
					public ImportExecutor<InterfaceTradeBean, InterfaceTradeAndPLMarks> createExecutor(
							BlockingQueue<InterfaceTradeBean> inWorkQueue,
							BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue, ImportContext context) {
						if(classNew) {
							return new InterfaceTradeProcessor(tradesCacheMap, inWorkQueue, outWorkQueue, context);
						}else{
							return new InterfaceTradeProcessor_old(tradesCacheMap, inWorkQueue, outWorkQueue, context);
						}
					}

					@Override
					public String getName() {
						return "TradeMapperExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return ScheduledTaskImportCSVExposureTrades.this.context.getTradeMapperWaitQueue();
					}

				});

		// start the trade persistor reader
		InterfaceImportExecutor<InterfaceTradeAndPLMarks, InterfaceTradeAndPLMarks> tradePersistorExecutor = new InterfaceImportExecutor<InterfaceTradeAndPLMarks, InterfaceTradeAndPLMarks>(
				numberPersistors, calypsoTradesAndPlMarksList, null, this.context,
				new ImportExecutorFactory<InterfaceTradeAndPLMarks, InterfaceTradeAndPLMarks>() {

					@Override
					public ImportExecutor<InterfaceTradeAndPLMarks, InterfaceTradeAndPLMarks> createExecutor(
							BlockingQueue<InterfaceTradeAndPLMarks> inWorkQueue,
							BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue, ImportContext context) {
						if (!isPDV) {
							return new InterfaceTradePersistor(inWorkQueue, null, context, persistorBufferSize);
						} else {
							return new InterfacePDVTradePersistor(inWorkQueue, null, context, persistorBufferSize);
						}
					}

					@Override
					public String getName() {
						return "TradePersistorExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return ScheduledTaskImportCSVExposureTrades.this.context.getTradePersistorWaitQueue();
					}

				});

		// System.out.println("Before shutting down;");
		// GSM: with one or two lines is too fast and sometimes kills threads before finishing the process.
		// Just 3 seconds waiting seems to allow processing a file with one just one or two lines.

		// GSM:this gives some time to the fileLog to finish with very short files
		while (!this.context.allThreadsHaveFinished()) {
			giveGraceTime(1000);
		}

		tradeMapperExecutor.shutDown();
		giveGraceTime(500);
		tradePersistorExecutor.shutDown();
		giveGraceTime(500);
		fileReaderExecutor.shutDown();
		giveGraceTime(500);
		this.fileLogWriter.shutDown();
		giveGraceTime(500);

		// System.out.println("After shutting down;");
		this.tradeImportTracker = this.context.getTradeImportTracker();
		return true;
	}

	// just give some time...
	private void giveGraceTime(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * @param path
	 * @param startFileName
	 * @return the file name to import if every thing is okay (only one file found as expected and the content of the
	 *         file is correct)
	 */
	private String getAndChekFileToProcess(String path, String startFileName) {
		String fileToProcess = "";
		ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName);
		// We check if the number of matche?s files is 1.
		if (files.size() == 1) {
			fileToProcess = files.get(0);
			this.file = fileToProcess;

			// GSM: 07/03/2014 - Added modification to allow PdV: not receiving control line
			if (!this.useControlLine) {
				// don't check control line (next try) if setup in ST attribute
				return fileToProcess;
			}

			try {
				if (!feedPreProcessSusiGBO(path + fileToProcess)) {
					this.tradeImportTracker.addError(new InterfaceTradeBean(), 3,
							"Number of lines in file does not match with number of lines in control record");
					return null;
				}
			} catch (Exception e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
				this.tradeImportTracker.addError(new InterfaceTradeBean(), 2, e.getMessage());
				return null;
			}
		} else {

			Log.error(LOG_CATEGORY_SCHEDULED_TASK,
					"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");

			this.tradeImportTracker.addError(new InterfaceTradeBean(), 1, "Expected File Not Found");
		}
		return fileToProcess;
	}

	/**
	 * Initialize the ScheduledTask context and logger
	 */
	protected boolean init(DSConnection dsCon) {

		// init the import context
		BlockingQueue<TradeImportStatus> logList = new LinkedBlockingQueue<TradeImportStatus>(10000);
		
		// START CALYPCROSS-38 - mromerod
//		this.context.init(DSConnection.getDefault(), "", getAttribute(SUMMARY_LOG), getAttribute(DETAILED_LOG),
//				getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG), getSourceSystem(), getAttribute(IMPORT_TYPE),
//				PROCESS_EXPOSURE_TRADES, getAttribute(SEPARATOR_DOMAIN_STRING), logList);
		
		this.context.initExposureTrades(DSConnection.getDefault(), "", getAttribute(SUMMARY_LOG), getAttribute(DETAILED_LOG),
				getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG), getSourceSystem(), getAttribute(IMPORT_TYPE),
				PROCESS_EXPOSURE_TRADES, getAttribute(SEPARATOR_DOMAIN_STRING), logList);
		// END CALYPCROSS-38 - mromerod

		//New Optional Columns
		boolean optColumns = getBooleanAttribute(OPT_COLUMNS);

		this.tradeImportTracker = this.context.getTradeImportTracker();

		this.tradeImportTracker.setOptColumns(optColumns);
		if (isPDV) {
			this.tradeImportTracker.setPDV(isPDV);
		}

		this.recipientsEmails = getImportRecipientsEmails(dsCon, getSourceSystem());

		this.remoteSantColService = (RemoteSantCollateralService) DSConnection.getDefault()
				.getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);

		// start the log tracker
		ImportExecutorFactory<TradeImportStatus, TradeImportStatus> loggerFactory = new ImportExecutorFactory<TradeImportStatus, TradeImportStatus>() {
			// ImportContext execContext = null;

			@Override
			public ImportExecutor<TradeImportStatus, TradeImportStatus> createExecutor(
					BlockingQueue<TradeImportStatus> inWorkQueue, BlockingQueue<TradeImportStatus> outWorkQueue,
					ImportContext context) {
				// this.execContext = context;
				return new InterfaceLogWriter(inWorkQueue, outWorkQueue, context, 100);
			}

			@Override
			public String getName() {
				return "FileLogWriter";
			}

			@Override
			public BlockingQueue<String> getWaitingQueue() {
				return ScheduledTaskImportCSVExposureTrades.this.context.getLogWriterWaitQueue();
			}

		};
		try {
			
			this.fileLogWriter = new InterfaceImportExecutor<TradeImportStatus, TradeImportStatus>(1, logList, null,
					this.context, loggerFactory);
			
		} catch (Exception e) {
			
			Log.error(this, e);
			this.tradeImportTracker.addError(new InterfaceTradeBean(), 2, "Unexpected error opening the file.");

			return false;
		}
		return true;
	}

	/**
	 * @return the source system for this import, GBO if it's not filled
	 */
	private String getSourceSystem() {
		return (Util.isEmpty(getAttribute(SOURCE_SYSTEM)) ? DEFAULT_SOURCE_SYSTEM : getAttribute(SOURCE_SYSTEM));
	}
	
	//GSM 01/05/2016 - v14 Migration
	/**
	 * @param fileToProcess
	 * @return cache of trades as map of {bo_reference+bo_system, SantTradeContainer}
	 */
	private Map<String,SantTradeContainer> preProcessTradesCache(final String fileToProcess){
		
		ArrayList<String> boReferencesList = getBoReferencesListFromFile(fileToProcess);
		
		if (Util.isEmpty(boReferencesList))
			return null;
		HashMap<String, SantTradeContainer> tradesMap = new HashMap<String, SantTradeContainer>();
		if(classNew) {
			List<String[]> list = TradeInterfaceUtils.splitArray(boReferencesList.toArray(new String[]{}), SantDerivativeTradesLoader.SQL_IN_ITEM_COUNT);


			Iterator<String[]> ite = list.iterator();

			int numberThreads = 4;
			try {
				numberThreads = Integer.parseInt(getAttribute(NB_TRADE_CACHE_WORKERS));
			} catch (Exception e) {
				Log.error(this, e); //sonar
			}

			SantDerivativeTradesLoader thread1 = null;
			SantDerivativeTradesLoader threads[] = new SantDerivativeTradesLoader[numberThreads - 1];
			while (ite.hasNext()) {

				thread1 = new SantDerivativeTradesLoader(ite.next());

				for (int i = 0; i < numberThreads - 1; i++) {
					threads[i] = null;
					if (ite.hasNext()) {
						threads[i] = new SantDerivativeTradesLoader(ite.next());
					}
				}

				thread1.start();

				for (int i = 0; i < numberThreads - 1; i++) {
					if (threads[i] != null) {
						threads[i].start();
					}
				}

				try {
					thread1.join();
					for (int i = 0; i < numberThreads - 1; i++) {
						if (threads[i] != null) {
							threads[i].join();
						}
					}

				} catch (InterruptedException e) {
					Log.error(this, "Thread interruption building threads cache");
				}
				tradesMap.putAll(thread1.getTradesMap());
				for (int i = 0; i < numberThreads - 1; i++) {
					if (threads[i] != null) {
						tradesMap.putAll(threads[i].getTradesMap());
					}
				}
			}
		} else{
			List<String[]> list = TradeInterfaceUtils.splitArray(boReferencesList.toArray(new String[]{}), SantDerivativeTradesLoader_old.SQL_IN_ITEM_COUNT);

			Iterator<String[]> ite = list.iterator();

			int numberThreads = 4;
			try {
				numberThreads = Integer.parseInt(getAttribute(NB_TRADE_CACHE_WORKERS));
			} catch (Exception e) {
				Log.error(this, e); //sonar
			}

			SantDerivativeTradesLoader_old thread1 = null;
			SantDerivativeTradesLoader_old threads[] = new SantDerivativeTradesLoader_old[numberThreads - 1];
			while (ite.hasNext()) {

				thread1 = new SantDerivativeTradesLoader_old(ite.next());

				for (int i = 0; i < numberThreads - 1; i++) {
					threads[i] = null;
					if (ite.hasNext()) {
						threads[i] = new SantDerivativeTradesLoader_old(ite.next());
					}
				}

				thread1.start();

				for (int i = 0; i < numberThreads - 1; i++) {
					if (threads[i] != null) {
						threads[i].start();
					}
				}

				try {
					thread1.join();
					for (int i = 0; i < numberThreads - 1; i++) {
						if (threads[i] != null) {
							threads[i].join();
						}
					}

				} catch (InterruptedException e) {
					Log.error(this, "Thread interruption building threads cache");
				}
				tradesMap.putAll(thread1.getTradesMap());
				for (int i = 0; i < numberThreads - 1; i++) {
					if (threads[i] != null) {
						tradesMap.putAll(threads[i].getTradesMap());
					}
				}
			}
		}
		return tradesMap;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return list of bo references (position 17 in CSA record format)
	 */
	private ArrayList<String> getBoReferencesListFromFile(final String fileName){
		
		String path = getAttribute(FILEPATH);
		if (Util.isEmpty(path))
			return null;
		
		if (!path.endsWith("/")) {
			path += "/";
		}
		final File file = new File(path+fileName);
		if (!file.exists() || !file.isFile())
			return null;
		
		ArrayList<String> boReferences = new ArrayList<String>();
		BufferedReader reader = null;
		String record = null;
		
		// 1. leer todas las boRef
				try {
					reader = new BufferedReader(new FileReader(file));
					
					while ((record = reader.readLine()) != null) {
						
						String[] values = null;
						values = record.split("\\" + this.context.getRecordSpliter(), -1);
						
						if (values.length<17){
							if (values.length>2) //to avoid control line
							Log.error(this, "Try to recover boReference but record must have at least 17 fields: " + record);
							
							continue;
						}
						
						if(!boReferences.contains(values[17])){
							boReferences.add(values[17]);
						} 	
					}
					reader.close();
				} catch (Exception e) {
					Log.error(this, e.toString());
					Log.error(this, e); //sonar
				}
		
		return boReferences;
		
	}
	
//	private HashMap<String,Trade> preProcessTrades(File f){
//		
//		HashMap<String,Trade> tradesMap = new HashMap<String,Trade>();
//		BufferedReader reader = null;
//		String record = null;
//		ArrayList<String> boReferences = new ArrayList<String>();
//		
//		// 1. leer todas las boRef
//		try {
//			reader = new BufferedReader(new FileReader(f));
//			while ((record = reader.readLine()) != null) {
//				String[] values = null;
//				values = record.split("\\" + this.context.getRecordSpliter(), -1);
//				if(!boReferences.contains(values[17])){
//					boReferences.add(values[17]);
//				}
//				
//			}
//		} catch (Exception e) {
//			Log.error(this, e.toString());
//		}
//		
//		// 2. cargar trades en grupos de 1000
//		String consulta = "";
//		int i=0;
//		while(i < boReferences.size()){	
//			// construir consulta
//			for(int j=i; j<i+1000; j++){
//				if(j==i+1000-1){
//					consulta += "'"+boReferences.get(j)+"'";
//				} else{
//					if (j == boReferences.size()-1){
//						consulta += "'"+boReferences.get(j)+"'";
//						break;
//					} else{
//						consulta += "'"+boReferences.get(j)+"'"+",";
//					}
//				}
//			}
//			// carga
//			TradeArray trades = getTrades(consulta);
//			
//			// meter en map
//			for(Trade trade : trades.getTrades()){
//				tradesMap.put(trade.getKeywordValue("BO_REFERENCE"), trade);
//			}
//			
//			i += 1000;
//			consulta = "";
//			
//		}
//		
//		return tradesMap;
//		
//	}
//	
//	private TradeArray getTrades(String consulta){
//		
//		TradeArray existingTrades = null;
//		try {
//			existingTrades = DSConnection
//					.getDefault()
//					.getRemoteTrade()
//					.getTrades(
//							"trade, trade_keyword kwd1",
//							"trade.trade_id=kwd1.trade_id and kwd1.keyword_name='BO_REFERENCE' and kwd1.keyword_value IN (" + consulta + ")", null);
//		} catch (RemoteException e) {
//			Log.error(this, e.toString());
//		}
//		
//		return existingTrades;
//		
//	}
	
	
}

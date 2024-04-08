/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import static calypsox.tk.collateral.service.efsonlineservice.OnlinePricesEFSService.APPLICATION_NAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.isban.efs2.webservice.EFS2WSService;

import calypsox.tk.collateral.service.efsonlineservice.EFSContext;
import calypsox.tk.collateral.service.efsonlineservice.OnlinePricesEFSService;

/**
 * This schedule task runs the Threads that make possible the online prices
 * interface.
 * 
 * @author Guillermo Solano
 * @version 1.3, 26/08/2013, added end time attribute & check WS configuration
 *          file is accesible
 * 
 */
public class ScheduledTaskOnlinePrices extends ScheduledTask {

	// unique class id, important to avoid problems
	private static final long serialVersionUID = 2233295722857754L;

	/*
	 * Changes this constants to: 1? to just append the log, not create new
	 * file. 2? to flush inmediately the log
	 */
	public final static boolean TRUE_TO_APPEND_LOG = false;
	public final static boolean TRUE_TO_FLUSH_LOG = false;
	/*
	 * Name of this Schedule task
	 */
	private final static String TASK_INFORMATION = "Online EFS Prices Schedule Task Service";

	/**
	 * CONSTANTS DEFINITION ONLINE PRICES SCHEDULED TASK This section includes
	 * the constants to define the different attributes in the schedule task
	 * domain, as the possible values.
	 */
	/**
	 * Enum containing the domain attributes constants.
	 */
	public enum DOMAIN_ATTRIBUTES {

		FILENAME("Log Summary File Name:", attribute("Log Summary File Name:")), END_TIME("End time (format HH:mm)",
				attribute("End time (format HH:mm)")), SUMMARY_LOG_DIR("Log Summary File Path:",
						attribute("Log Summary File Path:")), USE_TIMESTAMP("Add Timestamp after File Name: ",
								attribute("Add Timestamp after File Name: ").booleanType()), EXEC_READ_POS_TIME(
										"Read Quotes Thread, in minutes",
										attribute("Read Quotes Thread, in minutes")
												.domain(Arrays.asList(TIME_OPTIONS))), EXEC_WEB_SERVICE_CALL_TIME(
														"Online Prices Thread, in minutes",
														attribute("Online Prices Thread, in minutes").domain(Arrays
																.asList(TIME_OPTIONS))), EXEC_QUOTES_PERSISTANCE_TIME(
																		"Write DB Thread, in minutes",
																		attribute("Online Prices Thread, in minutes")
																				.domain(Arrays
																						.asList(TIME_OPTIONS))), LOG_SUMMERY_TIME(
																								"Log Summery Refresh, in minutes",
																								attribute(
																										"Log Summery Refresh, in minutes")
																												.domain(Arrays
																														.asList(TIME_OPTIONS)));

		private final String desc;
		private final AttributeDefinition def;

		// add description
		private DOMAIN_ATTRIBUTES(String d, AttributeDefinition def) {
			this.desc = d;
			this.def = def;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

		// return the definition
		public AttributeDefinition getDef() {
			return this.def;
		}

		// list with domain values definitions
		public static List<AttributeDefinition> getDomainDef() {
			ArrayList<AttributeDefinition> a = new ArrayList<AttributeDefinition>(DOMAIN_ATTRIBUTES.values().length);
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
	// /**
	// * @return a vector with all the domain attributes for this schedule task
	// *
	// */
	// @SuppressWarnings("rawtypes")
	// @Override
	// public Vector getDomainAttributes() {
	//
	// final Vector<String> result = new
	// Vector<String>(DOMAIN_ATTRIBUTES.values().length);
	// result.addAll(DOMAIN_ATTRIBUTES.getDomainDef());
	// return result;
	// }
	//
	// /**
	// * @param attribute
	// * name
	// * @param hastable
	// * with the attributes declared
	// * @return a vector with the values for the attribute name
	// */
	// @SuppressWarnings({ "rawtypes", "unchecked" })
	// @Override
	// public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
	//
	// Vector<String> vector = new Vector<String>();
	//
	// if (attribute.equals(DOMAIN_ATTRIBUTES.EXEC_READ_POS_TIME.getDesc())
	// ||
	// attribute.equals(DOMAIN_ATTRIBUTES.EXEC_WEB_SERVICE_CALL_TIME.getDesc())
	// ||
	// attribute.equals(DOMAIN_ATTRIBUTES.EXEC_QUOTES_PERSISTANCE_TIME.getDesc())
	// || attribute.equals(DOMAIN_ATTRIBUTES.LOG_SUMMERY_TIME.getDesc())) {
	//
	// vector.addAll(Arrays.asList(TIME_OPTIONS));
	//
	// } else if (attribute.equals(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc())) {
	//
	// vector.addAll(Arrays.asList(BOOLEANS));
	//
	// } else {
	//
	// vector = super.getAttributeDomain(attribute, hashtable);
	// }
	// return vector;
	// }

	/* private constants */
	private final static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
	private final static String[] TIME_OPTIONS = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "15",
			"20", "30", "60", "120", "240", "300", "480" };
	// representation of a minute in milliseconds ms
	private final static int MINUTE_MS = 60 * 1000;
	// correct time pattern
	private static final Pattern timePatter = Pattern.compile("([01]?[0-9]|2[0-3])(:|.)[0-5][0-9]");

	/* CLASS VARIABLES */
	/**
	 * Point the summery log File.
	 */
	private File summeryLog;
	/**
	 * file and pw to write the log
	 */
	private FileWriter summeryFw;
	private PrintWriter pw;
	/**
	 * Sleep time this thread. After it, log file will be write.
	 */
	private int SLEEP_TIME_MAIN;
	/**
	 * Copy to the context
	 */
	private EFSContext context;
	/**
	 * deactivates the log if configuration is not fine.
	 */
	private boolean noLog;

	/**
	 * End time to finish the job
	 */
	private Date endTime;

	/**
	 * ONLINE PRICES SCHEDULED TASK OVERRIDE METHODS
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

		initTask();
		initLogFiles();
		resetPreviousEFSCache();
		// result of the process
		boolean status = false;

		// check the WS configuration file is on the server
		if (!checkWSConfigurationFile()) {
			return status;
		}

		// declare threads
		final OnlinePricesEFSService threads = new OnlinePricesEFSService();
		// start all the threads.
		threads.executeThreads();

		do {
			// sleep this thread while WS threads are working
			sleepMainThread();

			// update the log summary
			generateLogSummary(threads);

			// if reach end time, kill threads
			if (endTimePassed()) {
				threads.killAllThreads();
				status = true;
			}

			// run while we haven't reach time limit and threads are alive
		} while (threads.areAlive());

		/*
		 * If reachs the end time read from the attribute it has finished OK.
		 * Othercase, one thread has died (due to an error) it will return false
		 * to the ScheduleRunner
		 */
		return status;
	}

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

				messages.addElement(attribute.getDesc() + " attribute not specified.");
				retVal = false;
			}

			else if (attribute.equals(DOMAIN_ATTRIBUTES.END_TIME)) {

				if (!timeAttributeHasCorrectFormat()) {
					messages.addElement(attribute.getDesc() + " must have the correct format in 24h");
					retVal = false;
				}
			}
		}

		return retVal;
	}

	/**
	 * @return this task information, gathered from the constant
	 *         TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	// /////////////////////////////////////////////
	// //////// PRIVATE METHODS ///////////////////
	// ///////////////////////////////////////////

	/**
	 * Initializes the context and inserts the attributes timing set for each
	 * thread, also the log update time condition.
	 */
	private void initTask() {

		this.context = EFSContext.getEFSInstance(); // singleton pattern
		this.noLog = false;
		this.endTime = readTimeAttribute();

		readThreadsTimingAttributes();
		readLogTimingAttribute();
	}

	/**
	 * @return true if the end time of the attribute has been reached
	 */
	private boolean endTimePassed() {

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
		final Date currentTime = calendar.getTime();
		// take time in milliseconds
		long thisTime = currentTime.getTime();
		long anotherTime = this.endTime.getTime(); // read from attribute
		final int result = (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
		final boolean endTime = result >= 0; // ok, end time equals or higher
												// this time
		return endTime;
	}

	/**
	 * @return if the time read from the attribute has a correct format (24
	 *         hours)
	 */
	private boolean timeAttributeHasCorrectFormat() {

		final String stringTime = super.getAttribute(DOMAIN_ATTRIBUTES.END_TIME.getDesc());

		if ((stringTime == null) || stringTime.isEmpty()) {
			return false;
		}

		final Matcher m = timePatter.matcher(stringTime);
		return m.matches();
	}

	/**
	 * @return the Date of today with the time read from the attribute from the
	 *         ST.
	 */
	private Date readTimeAttribute() {

		String stringTime = super.getAttribute(DOMAIN_ATTRIBUTES.END_TIME.getDesc());

		if ((stringTime == null) || stringTime.isEmpty()) {
			return null;
		}
		// replace dot if any
		stringTime = stringTime.replaceAll("\\.", ":");
		// add seconds
		stringTime = stringTime.trim() + ":00";

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss", new Locale("es", "ES"));
			final Calendar endTime = GregorianCalendar.getInstance();
			final Calendar currentTime = GregorianCalendar.getInstance();
			endTime.setTimeInMillis(sdf.parse(stringTime).getTime());
			// put today yy-mm-dd
			endTime.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
					currentTime.get(Calendar.DATE));

			return endTime.getTime();

		} catch (Exception ex) {
			Log.error(APPLICATION_NAME, "Not possible to convert the time attribute" + "\n" + ex ); //sonar
			return null;
		}
	}

	/**
	 * Reads the attributes timing and stores it in the Context. If parsing
	 * error happens, it will use the default timing set on the EFSContext
	 * class.
	 */
	private void readThreadsTimingAttributes() {

		try {
			final int pos = Integer.parseInt(super.getAttribute(DOMAIN_ATTRIBUTES.EXEC_READ_POS_TIME.getDesc()));
			EFSContext.setReadPositionsSleepTime(pos);

			final int ws = Integer.parseInt(super.getAttribute(DOMAIN_ATTRIBUTES.EXEC_WEB_SERVICE_CALL_TIME.getDesc()));
			EFSContext.setWebServiceSleepTime(ws);

			final int db = Integer
					.parseInt(super.getAttribute(DOMAIN_ATTRIBUTES.EXEC_QUOTES_PERSISTANCE_TIME.getDesc()));
			EFSContext.setPersistanceQuotesSleepTime(db);

		} catch (NumberFormatException e) {

			Log.error(APPLICATION_NAME,
					"Not possible to parser the threads time configurations, use default one. Check the configuration. \n"
							+ e.getLocalizedMessage());
		}
	}

	/**
	 * reads the attribute to set the log generation timing.
	 */
	private void readLogTimingAttribute() {

		try {
			final int log = Integer.parseInt(super.getAttribute(DOMAIN_ATTRIBUTES.LOG_SUMMERY_TIME.getDesc()));
			this.SLEEP_TIME_MAIN = log;

		} catch (NumberFormatException e) {

			Log.error(APPLICATION_NAME,
					"Not possible to parser the log time configuration, use 10 min as default. Check the configuration \n"
							+ e.getLocalizedMessage());
			this.SLEEP_TIME_MAIN = 10; // default

		}
	}

	/**
	 * Sleeps this main thread based on the ST attribute configuration.
	 */
	private synchronized void sleepMainThread() {

		try {

			Thread.sleep(MINUTE_MS * this.SLEEP_TIME_MAIN);

		} catch (InterruptedException e) {

			Log.error(APPLICATION_NAME, e.getLocalizedMessage());
		}
	}

	/**
	 * Initialization for the log file and the writer service
	 * 
	 */
	private void initLogFiles() {

		this.summeryFw = null;
		this.summeryLog = null;
		this.pw = null;

		final boolean timemark = (super.getAttribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc()) != null)
				&& super.getAttribute(DOMAIN_ATTRIBUTES.USE_TIMESTAMP.getDesc()).equals("Yes") ? true : false;

		String dir = super.getAttribute(DOMAIN_ATTRIBUTES.SUMMARY_LOG_DIR.getDesc());
		final String fileName = super.getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc());

		if (dir == null) {
			return;
		}

		if (!dir.trim().endsWith("/")) {
			dir = dir + "/";
		}

		String time = "";
		if (timemark) {
			time = "_" + getDateFormatted();
		}

		final String path = dir + fileName;

		this.summeryLog = new File(path + time + ".txt");
	}

	/*
	 * As the system is 24 hours but this thread starts every day, this will
	 * reset the data from the previous day.
	 */
	private void resetPreviousEFSCache() {

		EFSContext.resetPreviousCachedData();

	}

	/**
	 * @return true if the file EFS2WSService.wsdl is found in the classpatch
	 *         under com.isban.efs2.webservice.EFS2WSService.wsdl
	 */
	private boolean checkWSConfigurationFile() {

		try {

			final EFS2WSService service = new EFS2WSService();
			service.getEFS2WSPort();

		} catch (Exception e) {

			Log.error(APPLICATION_NAME,
					"WS configuration file:wsdl/EFS2WSService.wsdl NOT found in com.isban.efs2.webservice.EFS2WSService. NOT Possible to run this service.");
			Log.error(this, e);//sonar
			return false;
		}

		return true;
	}

	/**
	 * opens log file, generates status info of threads and contexts and closes
	 * the log file.
	 * 
	 * @param threads
	 */
	private void generateLogSummary(OnlinePricesEFSService threads) {

		// open the write handler
		openWriteLogFile();

		if (this.noLog || (this.pw == null)) { // log is disabled, do nothing
			return;
		}

		this.pw.println(threads.getStatusInformation());
		this.pw.println(this.context.getStatusInfo());

		// close the write handler
		closeLogfile();
	}

	/**
	 * opens the flow to write the log file
	 */
	private void openWriteLogFile() {

		try {

			this.summeryFw = new FileWriter(this.summeryLog.toString(), TRUE_TO_APPEND_LOG);
			this.pw = new PrintWriter(this.summeryFw, TRUE_TO_FLUSH_LOG);
			this.noLog = false; // no problems, activate log

		} catch (IOException e) {

			Log.error(APPLICATION_NAME,
					"Not possible to create the Log file. Does the folder exist? Check the configuration \n"
							+ e.getLocalizedMessage());
			Log.error(this, e);//sonar
			this.noLog = true; // arggh, deativate the log
		}
	}

	/**
	 * Flushes the log file and close the file pointer
	 */
	private void closeLogfile() {

		if (this.pw != null) {
			this.pw.close();
		}

		if (this.summeryFw != null) {
			try {
				this.summeryFw.close();
			} catch (IOException e) {
				Log.error(this, e); //sonar
			}
		}

	}

	/**
	 * @returns a String with the format
	 */
	private synchronized String getDateFormatted() {
		return timeFormat.format(new Date());

	}

}

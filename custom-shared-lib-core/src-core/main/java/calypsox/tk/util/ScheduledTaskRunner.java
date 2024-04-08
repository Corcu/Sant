//package com.santander.calypso.admin;

package calypsox.tk.util;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

import com.calypso.apps.startup.AppStarter;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.ESStarter;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSException;
import com.calypso.tk.event.PSSubscriber;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;

import calypsox.ErrorCodeEnum;

/**
 * Tool to start a given ScheduledTask from the ControlM/Commandline.
 * 
 * parameters are:
 * 
 * -env <environmentname> -user <username> -password <password> -taskId <taskid> [-valDate <date in format yyyy.MM.dd>]
 * 
 * if a valDate parameter is provided the scheduled task will be started with a valuation date as valDate but the time
 * is taken from the scheduled task.
 * 
 * If valDate is not passed the scheduled task will be started with valuation date = today and valuation time as
 * specified in the scheduled task.
 */

public class ScheduledTaskRunner implements PSSubscriber {

	// final static String LOG_CATEGORY = this;;
	protected final static String LOG_CATEGORY = "ScheduledTaskRunner";

	private DSConnection dsCon = null;

	private PSConnection pscon = null;

	private ScheduledTask task = null;

	private JDate valDate = null;

	/** checks if execution date is a holiday and if date rule allows execution */
	private boolean checkHolidays = true;

	/**
	 * Helper class to create Tasks in case of exceptions/errors in running the task.
	 */
	// private final ScheduledTaskHelper helper;

	/**
	 * Creates a new instance of the Class.
	 * 
	 * @param dscon
	 *            the calypso dataserver connection
	 * @param externalRef
	 *            the task to call
	 * @param valuationDate
	 * @param runNext
	 *            true if next task should be executed
	 */
	public ScheduledTaskRunner(final DSConnection dscon, final String externalRef, final JDate valuationDate)
			throws Exception {

		this.dsCon = dscon;
		this.task = getScheduledTask(externalRef);
		this.valDate = valuationDate;

		// initPSConnection(dscon, externalRef);
		// helper = new ScheduledTaskHelper(dscon, task.getId(),
		// task.getValuationDatetime(), task.getDatetime(),
		// task.getUndoDatetime());
	}

	/**
	 * Creates a new instance of the Class.
	 * 
	 * @param dscon
	 *            the calypso dataserver connection
	 * @param pscon
	 *            the calypso eventserver connection
	 * @param externalRef
	 *            the task to call
	 * 
	 */
	public ScheduledTaskRunner(final DSConnection dscon, final String externalRef) throws Exception {

		this(dscon, externalRef, JDate.getNow());

	}

	/**
	 * Initializes the PSConnection
	 * 
	 * @param dscon
	 * @param externalRef
	 * @throws Exception
	 */
	@SuppressWarnings({ "unused", "deprecation" })
	private void initPSConnection(final DSConnection dscon, final String externalRef) throws Exception {

		// this.pscon = ESStarter.startConnection(this.dsCon, null);
		this.pscon = ESStarter.startConnection(this, new Class[] {});

		if (this.pscon == null) {
			throw new ConnectException("Error connecting to EventServer");
		}

		try {

			this.pscon.start();
			this.pscon.setApplicationName("ScheduledTaskRunner-" + externalRef);

		} catch (final PSException pse) {

			try {
				this.pscon.stop();
			} catch (final PSException exc) {
				Log.error(this, "Error stopping PSConnection", exc);
				throw exc;
			}

			Log.error(this, "Error connecting to Event Server", pse);

			throw pse;
		}
	}

	/**
	 * retrieves ScheduledTask for the given external_reference
	 * 
	 * @param externalRef
	 */
	private ScheduledTask getScheduledTask(final String externalRef) {
		ScheduledTask sct = null;
		try {

			sct = this.dsCon.getRemoteBackOffice().getScheduledTaskByExternalReference(externalRef);

		} catch (final Exception exc) {
			Log.error(LOG_CATEGORY, "Error loading ScheduledTask with externalRef=" + externalRef, exc);
			System.out.println(ErrorCodeEnum.ScheduledTaskUnexpectedException.getFullTextMesssage(new String[] {
					exc.getClass().getName(), exc.getMessage() }));
			System.err.println("Exit status = " + ErrorCodeEnum.ScheduledTaskUnexpectedException.getCode() + " "
					+ ErrorCodeEnum.ScheduledTaskUnexpectedException.toString());
			System.exit(ErrorCodeEnum.ScheduledTaskUnexpectedException.getCode());
		}

		// Just in case if the ST is not loaded for some reason
		if (sct == null) {
			Log.error(LOG_CATEGORY, "Error getting ScheduledTask with externalRef=" + externalRef);
			System.out.println("Error getting ScheduledTask with externalRef=" + externalRef);
			System.err.println("Exit status = " + ErrorCodeEnum.ScheduledTaskConfigError.getCode() + " "
					+ ErrorCodeEnum.ScheduledTaskConfigError.toString());
			System.exit(ErrorCodeEnum.ScheduledTaskConfigError.getCode());
		}

		return sct;
	}

	/**
	 * Checks if the task can be run on the valDate. Executes the ScheduledTask and returns true if successful, false
	 * otherwise.
	 * 
	 * @return the exit code of the scheduled task
	 */
	@SuppressWarnings("unchecked")
	public int execute() throws Exception {

		boolean result = false;

		// Check if it is setup to execute
		if (!this.task.getExecuteB()) {
			final String msg = "ScheduledTaskRunner : ScheduledTask with externalRef="
					+ this.task.getExternalReference() + " is not setup to be executed";
			Log.error(LOG_CATEGORY, msg);
			// helper.addExceptionToTask(SantExceptionType.TECHNICAL_EXCEPTION,
			// task.getExternalReference(), 0, msg);
			System.out.println(msg);
			ControlMErrorLogger.addError(ErrorCodeEnum.ScheduledTaskConfigError, msg);
			return ErrorCodeEnum.ScheduledTaskConfigError.getCode();
		}

		// checking for the task if the date rule is valid otherwise stop
		if (isCheckHoliday() && (!this.task.getExecuteOnHolidays())) {
			@SuppressWarnings("rawtypes")
			final Vector holidays = this.task.getHolidays();
			if ((holidays == null) || (holidays.size() == 0)) {
				final String msg = "ScheduledTaskRunner : ScheduledTask with externalRef="
						+ this.task.getExternalReference() + " is not setup to run on holidays."
						+ " So holidays must be setup.";
				Log.error(LOG_CATEGORY, msg);
				// helper.addExceptionToTask(
				// SantExceptionType.TECHNICAL_EXCEPTION,
				// task.getExternalReference(), 0, msg);
				System.out.println(msg);

				ControlMErrorLogger.addError(ErrorCodeEnum.ScheduledTaskConfigError, msg);
				return ErrorCodeEnum.ScheduledTaskConfigError.getCode();
			}

			if (!isBusinessDay(this.valDate, this.task)) {
				final String msg = "ScheduledTaskRunner : ScheduledTask with externalRef="
						+ this.task.getExternalReference() + " is not setup to run on holiday";
				Log.error(LOG_CATEGORY, msg);
				// helper.addExceptionToTask(
				// SantExceptionType.TECHNICAL_EXCEPTION,
				// task.getExternalReference(), 0, msg);
				System.out.println(msg);

				ControlMErrorLogger.addError(ErrorCodeEnum.ScheduledTaskConfigError, msg);
				return ErrorCodeEnum.ScheduledTaskConfigError.getCode();
			}
		}

		final JDatetime currServerTime = this.dsCon.getServerCurrentDatetime();
		this.task.setCurrentDate(this.valDate);
		// task.setValuationTime(valDate.get)
		this.task.setDatetime(currServerTime);
		this.task.setUser(this.dsCon.getUser());

		@SuppressWarnings("rawtypes")
		final Vector errors = new Vector();
		if (!this.task.isValidInput(errors)) {
			final String msg = "ScheduledTaskRunner : ScheduledTask with externalRef="
					+ this.task.getExternalReference() + " has not valid parameters";
			Log.error(LOG_CATEGORY, msg);
			// helper.addExceptionToTask(SantExceptionType.TECHNICAL_EXCEPTION,
			// task.getExternalReference(), 0, msg);

			System.out.println(msg);
			return ErrorCodeEnum.ScheduledTaskConfigError.getCode();
		} else {
			// mark the scheduled task as started
			this.task.registerScheduledTaskStart(this.dsCon, currServerTime, this.task.getDescription());

			Log.info(LOG_CATEGORY, "Starting " + gettaskInfoStr(this.task));

			try {
				result = this.task.execute(this.dsCon, this.pscon);

				// Updated the scheduled Task completion
				this.dsCon.getRemoteBO().updateScheduledTaskTime(this.task.getId(), this.task.getType(),
						currServerTime, result, this.task.getDescription(), this.task.getValuationDatetime(),
						DSConnection.getDefault().getUser());

				if (result) {
					Log.info(LOG_CATEGORY, "Completed with return Status SUCCESS; " + gettaskInfoStr(this.task));
				} else {
					Log.info(LOG_CATEGORY, "Completed with return Status FAILURE; " + gettaskInfoStr(this.task));
				}

			} catch (final Exception exc) {
				Log.error(LOG_CATEGORY, "Error executing " + gettaskInfoStr(this.task), exc);

				try {
					this.dsCon.getRemoteBO().updateScheduledTaskTime(this.task.getId(), this.task.getType(),
							currServerTime, result, this.task.getDescription(), this.task.getValuationDatetime(),
							DSConnection.getDefault().getUser());
				} catch (final RemoteException e) {
					Log.error(LOG_CATEGORY, "Error updating scheduled task status", e);
				}

				throw exc;
			}

			if (result) {
				return ErrorCodeEnum.NoError.getCode();
			} else {
				final ErrorCodeEnum errorCode = ControlMErrorLogger.getErrorCode();
				if (errorCode != null) {
					ControlMErrorLogger.sendMessageToControlM();
					return errorCode.getCode();
				} else {
					return ErrorCodeEnum.ScheduledTaskFailure.getCode();
				}
			}
		}
	}

	/**
	 * This method returns task info.
	 * 
	 * @param task
	 * @return
	 */
	private String gettaskInfoStr(final ScheduledTask task) {
		final String infoStr = " ScheduledTask externalRef=" + task.getExternalReference() + "; taskType= "
				+ task.getType() + "; TaskId=" + task.getId() + "; description=" + task.getDescription();

		return infoStr;
	}

	@Override
	public void newEvent(final PSEvent evt) {

	}

	@Override
	public void onDisconnect() {
	}

	/**
	 * checks if holidays should be checked or not
	 * 
	 * @param checkHoliday
	 */
	private void setCheckHoliday(final boolean checkHolidays) {
		this.checkHolidays = checkHolidays;
	}

	/**
	 * @return true if holiday should be checked, false otherwise
	 */
	private boolean isCheckHoliday() {
		return this.checkHolidays;
	}

	/**
	 * checks if valuation date is a business day and if it is valid for execution regarding the date rules set to this
	 * scheduled task
	 * 
	 * @return
	 */
	private boolean isBusinessDay(final JDate valDate, final ScheduledTask task) {
		boolean result = false;
		if (task.getExecuteOnHolidays() == false) {
			final DateRule dr = task.getDateRule();
			if (dr != null) {
				result = Holiday.getCurrent().isBusinessDay(valDate, task.getHolidays())
						&& dr.generate(valDate, 1).contains(valDate);
			} else {
				result = Holiday.getCurrent().isBusinessDay(valDate, task.getHolidays());
			}
		} else {
			result = true;
		}
		if (!result) {
			Log.warn(LOG_CATEGORY, "Scheduled task cannot be executed cause either no business "
					+ "day or date rule is not valid for valuation date");
		}
		return result;
	}

	/**
	 * Main method called from the commandline. The params -env, -user, -password and -externalRef are mandatory.
	 * -valDatetime is optional
	 * 
	 * @param args
	 *            the command line arguments
	 */
	static public void main(final String args[]) {
		try {
			// Work-around: the calypso logger catch the writing to the
			// System.err
			// and System.out, but we need write to the standard output to send
			// this
			// information to Control - M
			final PrintStream defaultErr = System.err;
			final PrintStream defaultOut = System.out;

			// Start the logging
			AppStarter.startLog(args, LOG_CATEGORY);

			// Work-around: we setup again the defaults
			System.setErr(defaultErr);
			System.setOut(defaultOut);

			// show the version on the log file
			// showClientVersion();

			// Below method call creates ScheduleTask and Helper instances.
			final ScheduledTaskRunner taskRunner = checkInputAndCreateTaskRunner(args);

			int exit_code = ErrorCodeEnum.ScheduledTaskFailure.getCode();

			try {
				exit_code = taskRunner.execute();
			} catch (final Exception t) {
				Log.error(LOG_CATEGORY,
						"Error executing " + taskRunner.task.getType() + " : " + taskRunner.task.getId(), t);
				if (ControlMErrorLogger.getErrorCode() == null) {
					exit_code = ErrorCodeEnum.ScheduledTaskUnexpectedException.getCode();
					final String msg = ErrorCodeEnum.ScheduledTaskUnexpectedException.getFullTextMesssage(new String[] {
							t.getClass().getName(), t.getMessage() });
					ControlMErrorLogger.addError(ErrorCodeEnum.ScheduledTaskUnexpectedException, msg);
					System.out.println(msg);
				} else {
					exit_code = ControlMErrorLogger.getErrorCode().getCode();
					ControlMErrorLogger.sendMessageToControlM();
				}
			}

			// Save the task
			// taskRunner.helper.publishTasks();
			if (exit_code == 0) {
				System.err.println("Exit status = 0 Success");
			} else {
				final ErrorCodeEnum code = ControlMErrorLogger.getErrorCode();
				if (code != null) {
					System.err.println("Exit status = " + exit_code + " " + code.toString());
				} else {
					System.err.println("Exit status = " + exit_code + " (code = null)");
				}
			}
			System.exit(exit_code);
		} catch (final java.lang.OutOfMemoryError ex) {
			Log.error(ScheduledTaskRunner.class, ex); //sonar
			System.out.println(ErrorCodeEnum.OutOfMemoryError.getMessage());
			System.exit(ErrorCodeEnum.OutOfMemoryError.getCode());
		}
	}

	/** show the version on the log file */
	// private static void showClientVersion() {
	// ClientVersion clientVersion = new ClientVersion();
	// Log.info(LOG_CATEGORY,
	// "ScheduledTaskRunner version [name=" + clientVersion.getName()
	// + ", version=" + clientVersion.getVersion()
	// + ", versionDate=" + clientVersion.getVersionDate()
	// + "]");
	// }

	/**
	 * This method checks if the input is valid and creates ScheduledTaskRunner object.
	 * 
	 * @param args
	 * @return
	 */
	public static ScheduledTaskRunner checkInputAndCreateTaskRunner(final String... args) {

		DSConnection dscon = null;
		ScheduledTaskRunner taskRunner = null;
		JDate dVal = null;

		final String externalRefStr = AppStarter.getOption(args, "-externalRef");
		final String valDateStr = AppStarter.getOption(args, "-valDate");

		final boolean checkHoliday = AppStarter.isOption(args, "-disableHolidayCheck");

		// 1. Check if externalRef is valid
		if (Util.isEmpty(externalRefStr)) {
			Log.error(LOG_CATEGORY, "externalRef of the Scheduled Taks must be specified.");
			displayUsage();
			System.exit(ErrorCodeEnum.ScheduledTaskRunnerParamError.getCode());
		}

		// 2. Check if valDate is valid
		if (!Util.isEmpty(valDateStr)) {
			try {
				final DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
				dVal = JDate.valueOf(df.parse(valDateStr));
			} catch (final Exception e) {
				Log.error(LOG_CATEGORY,
						"valDate passed to the scheduled task is in wrong format. It should be yyyy.MM.dd", e);
				System.err.println("valDate passed to the scheduled task is in wrong format. It should be yyyy.MM.dd");
				System.exit(ErrorCodeEnum.ScheduledTaskRunnerParamError.getCode());
			}
		} else {
			dVal = JDate.getNow();
		}

		// Create DataServer Connection
		try {

			dscon = ConnectionUtil.connect(args, LOG_CATEGORY);
			if (dVal != null) {
				taskRunner = new ScheduledTaskRunner(dscon, externalRefStr, dVal);
			} else {
				taskRunner = new ScheduledTaskRunner(dscon, externalRefStr);
			}
			taskRunner.setCheckHoliday(!checkHoliday);

		} catch (final ConnectException e) {
			Log.error(LOG_CATEGORY, "Error connecting to DataServer", e);
			System.out.println(ErrorCodeEnum.ConnectException.getFullTextMesssage(new String[] { e.getMessage() }));
			System.exit(ErrorCodeEnum.ConnectException.getCode());
		} catch (final Exception e) {
			Log.error(LOG_CATEGORY, "Error connecting to DataServer", e);
			System.out.println(ErrorCodeEnum.DataserverError.getFullTextMesssage(new String[] { e.getMessage() }));
			System.exit(ErrorCodeEnum.DataserverError.getCode());
		}

		return taskRunner;
	}

	public static void displayUsage() {
		// Usage message
		Log.error(LOG_CATEGORY, "Usage: " + " -env <environment> -user <username> -password <password>"
				+ " -externalRef <externalRef> [ -valDate <yyyy.MM.dd> ] [-disableHolidayCheck]");
	}

}

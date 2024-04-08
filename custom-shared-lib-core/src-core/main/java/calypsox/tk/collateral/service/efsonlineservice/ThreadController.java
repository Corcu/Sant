/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

package calypsox.tk.collateral.service.efsonlineservice;

import static calypsox.tk.collateral.service.efsonlineservice.OnlinePricesEFSService.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;

import calypsox.tk.collateral.service.efsonlineservice.interfaces.EFSThreadsInterface;

import com.calypso.tk.core.Log;

/**
 * Skeleton of each thread, allowing to share the common thread logic.
 * 
 * @author Guillermo Solano
 * @version 1.0, 07/06/2013
 * 
 * @param <V>
 *            generic param to store inside
 */
public abstract class ThreadController<V> implements EFSThreadsInterface {

	/* Some internal constants to represent time */
	// representation of a minute in milliseconds ms
	private final static int MINUTE_MS = 60 * 1000;
	// this allows to know exactly how much time of execution has been running a thread
	private final static long start = System.currentTimeMillis();
	// in case a thread has to wait for a previous one data, it will wait this time
	private static int PENDING_DATA_SLEEP_TIME = 1;
	// number of full executions of the thread
	private int executions;

	/* Variables */
	/**
	 * number of minutes by default to sleep this thread. Allows to be changed by the method. The idea is to be
	 * configurable by the Schedule Task and read from the context.
	 */
	private static int DEFAULT_SLEEP_TIME = 30;
	/**
	 * Generic list of data, to be used specifically from each class to extends the father.
	 */
	protected List<V> dataList;
	/**
	 * Thread instance to run
	 */
	private Thread innerThread;
	/**
	 * When true, runs in multhread mode.
	 */
	protected boolean enableThreading;

	/**
	 * Constructor
	 * 
	 * @param enableThreading
	 */
	public ThreadController(boolean enableThreading) {

		this.dataList = new ArrayList<V>();
		this.enableThreading = enableThreading;
		this.executions = 0;
	}

	/**
	 * Generic Constructor. Multithreading activated.
	 */
	public ThreadController() {
		this(true);
	}

	/**
	 * Main method of the thread, contains the main logic of it (the magic). This is the method override on each class
	 * that extends this one, that actually will allow to run it in thread mode.
	 */
	@Override
	public abstract void runThreadMethod();

	/**
	 * Main method to create the inner thread and later on execute it, by calling the runThreadMethod implementation.
	 */
	public void load() {

		if (this.enableThreading) {

			this.innerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					runThreadMethod(); // it call the abstractMethod, to be overrided!
				}
			});
			// starts the thread.
			this.innerThread.start();

		} else {
			// secuential mode.
			runThreadMethod();
		}
	}

	/**
	 * @return if the thread is alive (when enableThreading is true).
	 */
	public boolean isAlive() {

		return this.enableThreading;
	}

	/**
	 * @return a copy of the data in the main List
	 */
	public synchronized List<V> getDataAsList() {

		if (!this.dataList.isEmpty()) {
			return this.dataList;
		}
		// else
		return null;

	}

	/**
	 * Allows the Log for errors and warnings using exception control. In case the exception is critic, it will stop the
	 * thread. If the exception requires some time (ocurs in case some input data is missing) it will sleep. en case
	 * there is a warning, it will show the Log.
	 * 
	 * @param e
	 *            the current exception
	 */
	public void processException(EFSException e) {

		if (e.isCritical()) {

			killThread();
			Log.error(APPLICATION_NAME, e.getLocalizedMessage());
			return;
		}

		Log.warn(APPLICATION_NAME, e.getLocalizedMessage());

		if (e.mustWait()) {

			Log.warn(APPLICATION_NAME, "Thread is going to sleep " + PENDING_DATA_SLEEP_TIME + " minutes");
			this.sleepThread(PENDING_DATA_SLEEP_TIME);
		}
	}

	/**
	 * @returns the execution time in seconds of the thread.
	 */
	public long getRunningTime() {

		long end = System.currentTimeMillis();
		return ((end - start) / 1000);
	}

	/**
	 * get time in hours/minutes/seconds
	 * 
	 * @return String
	 */
	public String getTimeHoursMinutesSecondsString(final long time) {

		long elapsedTime = time;
		String format = String.format("%%0%dd", 2);
		// elapsedTime = elapsedTime / 1000;

		final String seconds = String.format(format, elapsedTime % 60);
		final String minutes = String.format(format, (elapsedTime % 3600) / 60);
		final String hours = String.format(format, elapsedTime / 3600);

		return hours + ":" + minutes + ":" + seconds;
	}

	/**
	 * if the thread is running, this will stop it (stop signal)
	 */
	public synchronized void killThread() {
		this.enableThreading = false;

	}

	/**
	 * Increases a the executions counter
	 */
	public void increaseExecutionsCounter() {
		this.executions++;
	}

	/**
	 * @return the number of times this thread has been executed
	 */
	public int executionsCounter() {
		return this.executions;
	}

	// /////////////////////////////////////////////////////////////////////////
	// ////////////////PRIVATE METHODS THREAD CONTROLLER ///////////////////////
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * @param minutesTo
	 *            sleep this thread.
	 */
	private synchronized void sleepThread(int minutesTo) {

		try {
			
			Thread.sleep(MINUTE_MS * minutesTo);

		} catch (InterruptedException e) {

			Log.error(APPLICATION_NAME, e.getLocalizedMessage());

		}
	}

	/**
	 * sleeps the thread using the default configuration to be read from the context.
	 */
	protected synchronized void sleepThread() {

		this.sleepThread(DEFAULT_SLEEP_TIME);
	}

	/**
	 * @param howMuch
	 *            minutes must sleep this thread. Changes the default sleep time configuration.
	 * @see sleepThread()
	 */
	protected void setSleepTime(int howMuch) {

		DEFAULT_SLEEP_TIME = howMuch;
	}

}

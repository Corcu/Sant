/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import calypsox.tk.collateral.service.efsonlineservice.interfaces.BoISINProcessingInterface;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.EFSQuotesServiceInterface;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.QuotesDBPersistanceInterface;

/**
 * Main class to control the 3 threads used in the online prices interface.
 * 
 * 
 * @author Guillermo Solano
 * @version 1.0, 12/06/2013
 * 
 */
public class OnlinePricesEFSService {

	public static final String APPLICATION_NAME = "ONLINE_PRICES_EFS_SERVICE";

	/**
	 * Read products ISIN+CCY from positions thread interface
	 */
	private final BoISINProcessingInterface readPos;
	/**
	 * Calls the WS and saves quotes on context thread interface:
	 */
	private final EFSQuotesServiceInterface callws;
	/**
	 * Inserts quotes on DB thread interface
	 */
	private final QuotesDBPersistanceInterface writeDb;

	/**
	 * @param threadMode
	 *            if true, run in independent threads
	 */
	public OnlinePricesEFSService(boolean threadMode) {

		this.readPos = new ISINsFromBOPositions(threadMode);
		this.callws = new EFSBloombergService(threadMode);
		this.writeDb = new QuotesDBPersistance(threadMode);
	}

	/**
	 * Generic constructor, thread oriented
	 */
	public OnlinePricesEFSService() {

		this(true);
	}

	/**
	 * Starts the three threads: ------------------------------------------------------------------------------------
	 * Thread 1: reads alive positions and extract bonds and equities to subscribe. ---------------------------------
	 * Thread 2: Reads the quotes subscriptions, calls the webservice and stores response in context ----------------
	 * Thread 3: Reads responses from context and stores is in Calypso's DB
	 * 
	 */
	public void executeThreads() {

		if (this.readPos != null) {
			this.readPos.readProductsFromDatabasePositions();
		}

		if (this.callws != null) {
			this.callws.startEfsWebService();
		}

		if (this.writeDb != null) {
			this.writeDb.updateDatabase();
		}
	}

	/**
	 * @return true if all three threads are alive
	 */
	public synchronized boolean areAlive() {

		if ((this.readPos == null) || (this.callws == null) || (this.writeDb == null)) {
			return false;
		}

		return (this.readPos.isAlive() && this.callws.isAlive() && this.writeDb.isAlive());
	}

	/**
	 * Kills all the threads running (after finishing the execution if they have already started).
	 */
	public synchronized void killAllThreads() {

		if (this.readPos != null) {
			this.readPos.killUpdatingBOPostionsThread();
		}

		if (this.callws != null) {
			this.callws.killEfsWebService();
		}

		if (this.writeDb != null) {
			this.writeDb.killUpdateDataseThread();
		}

	}

	/**
	 * @return information of the service. Time and executions for each thread, number of quotes requested, received and
	 *         the list of Isins+ccy that are not receiving prices from the WS.
	 */
	public String getStatusInformation() {

		final StringBuffer sb = new StringBuffer();
		final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm");
		Calendar cal = Calendar.getInstance();

		sb.append("-----------------------------------------------------------\n");
		sb.append(APPLICATION_NAME).append(" - Log Summary \n");
		sb.append("Snapshot taken at: ").append(format.format(cal.getTime()));
		sb.append("\n");
		sb.append("-----------------------------------------------------------\n");

		sb.append(this.callws.getStatusInfo());
		sb.append(this.readPos.getStatusInfo());
		sb.append(this.writeDb.getStatusInfo());

		return sb.toString();
	}

}

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import calypsox.tk.optimization.service.RemoteSantOptimizationService;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.importer.ProcessContext;

import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

public class OptimAllocsImportContext implements ProcessContext {

	public static final String LOG_CATERGORY = "OPTIM_ALLOC_IMPORT";

	protected boolean skipHeader;
	protected String rowSpliter;

	protected PricingEnv pricingEnv;

	protected Map<String, Double> contractsNameForId;

	protected ExecutionContext executionContext;

	private final HashSet<String> fileReaderCounter = new HashSet<String>();

	private final HashSet<String> allocationMapperCounter = new HashSet<String>();

	private final HashSet<String> allocationPersistorCounter = new HashSet<String>();

	private final HashSet<String> allocationImportLoggerCounter = new HashSet<String>();

	// private final AtomicBoolean logWriterRunning = new AtomicBoolean(true);
	private final BlockingQueue<String> fileReaderWaitQueue = new LinkedBlockingQueue<String>();

	private final BlockingQueue<String> allocationMapperWaitQueue = new LinkedBlockingQueue<String>();

	private final BlockingQueue<String> allocationPersistorWaitQueue = new LinkedBlockingQueue<String>();

	private final BlockingQueue<String> allocationImportLoggerWaitQueue = new LinkedBlockingQueue<String>();

	private List<Object> invalidItems = new ArrayList<Object>();

	private Long executionId;

	// private final BlockingQueue<String> logWriterWaitQueue = new
	// LinkedBlockingQueue<String>();

	public OptimAllocsImportContext(String separator, boolean skipHeader) {
		this.rowSpliter = separator;
		this.contractsNameForId = new HashMap<String, Double>();
		this.executionId = 0L;
		this.skipHeader = skipHeader;
	}

	/**
	 * @return the rowSpliter
	 */
	public String getRowSpliter() {
		return this.rowSpliter;
	}

	public boolean isSkipHeader() {
		return this.skipHeader;
	}

	public void init(JDatetime processingDate, JDatetime valuatioDate,
			String pricingEnvName) throws Exception {
		// init the pricing env
		pricingEnv = PricingEnv.loadPE(pricingEnvName, processingDate);
		// init the colalteral management execution context
		executionContext = initExecutionContext(processingDate, valuatioDate);
		// TODO delegte this test

		// contractsNameForId.put("ISMA - BBIL",25004.0);
		// contractsNameForId.put("CSA - BAIG",24977.0);
		// executionId = calculateExecutionId(0L);
		RemoteSantOptimizationService remoteSantOptimService = (RemoteSantOptimizationService) DSConnection
				.getDefault().getRMIService("baseSantOptimizationService",
						RemoteSantOptimizationService.class);
		contractsNameForId = remoteSantOptimService.getAllContractNamesForIds();
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		//AAP MIG 14.4
		long execId = Long.parseLong(sdf.format(executionContext
				.getProcessDate().getDate()));

		// GSY 23/12/14 - Incidencia Boleteo Interno
		int allocatedSeed = remoteSantOptimService
				.nextSeed("OPTIM_ALLOC_EXECUTION");

		// executionId =
		// calculateExecutionId(remoteSantOptimService.getOptimImportExecutionID(execId));
		executionId = calculateExecutionId(execId, allocatedSeed);

	}

	/**
	 * 
	 * @param executionId
	 * @param allocatedSeed
	 * @return
	 */
	private Long calculateExecutionId(Long executionId, int allocatedSeed) {

		return Long.parseLong(String.valueOf(executionId)
				+ String.valueOf(allocatedSeed));

	}

	/**
	 * Initialize the execution context to use with the collateral manager
	 * utility
	 * 
	 * @param processingDate
	 * @param valuatioDate
	 * @return
	 */
	private ExecutionContext initExecutionContext(
			final JDatetime processingDate, final JDatetime valuatioDate) {

		final ExecutionContext ec = CollateralManagerUtil
				.getDefaultExecutionContext();
		ec.setProcessDate(processingDate.getJDate(TimeZone.getDefault()));
		// set the default user action to use
		ec.setUserAction("Price");
		return ec;
	}

	public ExecutionContext getExecutionContextWithUserAction(String userAction) {

		if (executionContext == null) {
			return null;
		}
		final ExecutionContext clonedExecCtx = executionContext.clone();
		clonedExecCtx.setUserAction(userAction);
		return clonedExecCtx;
	}

	/**
	 * @return the pricingEnv
	 */
	public PricingEnv getPricingEnv() {
		return pricingEnv;
	}

	/**
	 * @return the contractsNameForId
	 */
	public Map<String, Double> getContractsNameForId() {
		return contractsNameForId;
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	/**
	 * if file reader is running
	 * 
	 * @return
	 */
	public boolean isFileReaderRunning() {
		return !this.fileReaderCounter.isEmpty();
	}

	/**
	 * 
	 */
	public void stopFileReaderProcess(String process) {
		Log.debug(LOG_CATERGORY, "*****************>stopFileReaderProcess");
		this.fileReaderCounter.remove(process);
		this.fileReaderWaitQueue.add("");
	}

	// public void stopLogWriterProcess() {
	// Log.debug(LOG_CATERGORY, "*****************>stopLogWriterProcess");
	// this.logWriterRunning.set(false);
	// this.logWriterWaitQueue.add("");
	// }

	public boolean isAllocationMapperRunning() {
		return !this.allocationMapperCounter.isEmpty();
	}

	public void stopAllocationMapperProcess(String processor) {
		Log.debug(LOG_CATERGORY,
				"*****************>stopAllocationMapperProcess");
		this.allocationMapperCounter.remove(processor);
		this.allocationMapperWaitQueue.add("");
	}

	public boolean isAllocationPersistorRunning() {
		return !this.allocationPersistorCounter.isEmpty();
	}

	public boolean isAllocationImportLoggerRunning() {
		return !this.allocationImportLoggerCounter.isEmpty();
	}

	/**
	 * @return the fileReaderWaitQueue
	 */
	public BlockingQueue<String> getFileReaderWaitQueue() {
		return this.fileReaderWaitQueue;
	}

	/**
	 * @return the tradeMapperWaitQueue
	 */
	public BlockingQueue<String> getAllocationMapperWaitQueue() {
		return this.allocationMapperWaitQueue;
	}

	public void stopAllocationPersistorProcess(String processor) {
		Log.debug(LOG_CATERGORY,
				"*****************>stopAllocationPersistorProcess");
		this.allocationPersistorCounter.remove(processor);
		this.allocationPersistorWaitQueue.add("");
	}

	public void stopAllocationImportLoggerProcess(String processor) {
		Log.debug(LOG_CATERGORY,
				"*****************>stopAllocationImportLoggerProcess");
		this.allocationImportLoggerCounter.remove(processor);
		this.allocationImportLoggerWaitQueue.add("");
	}

	/**
	 * @return the tradeMapperWaitQueue
	 */
	public BlockingQueue<String> getAllocationPersistorWaitQueue() {
		return this.allocationPersistorWaitQueue;
	}

	public void addFileReaderProcessor(String processor) {
		fileReaderCounter.add(processor);
	}

	public void addAllocationMapperProcessor(String processor) {
		allocationMapperCounter.add(processor);
	}

	public void addAllocationPersistoProcessor(String processor) {
		allocationPersistorCounter.add(processor);
	}

	public HashSet<String> getFileReaderProcessor() {
		return fileReaderCounter;
	}

	public HashSet<String> getAllocationMapperProcessor() {
		return allocationMapperCounter;
	}

	public HashSet<String> getAllocationPersistoProcessor() {
		return allocationPersistorCounter;
	}

	/**
	 * @return the executionId
	 */
	public Long getExecutionId() {
		return executionId;
	}

	/**
	 * @return the allocationImportLoggerCounter
	 */
	public HashSet<String> getAllocationImportLoggerCounter() {
		return allocationImportLoggerCounter;
	}

	/**
	 * @return the invalidItems
	 */
	public List<Object> getInvalidItems() {
		return invalidItems;
	}

	/**
	 * @param invalidItems
	 *            the invalidItems to set
	 */
	public void setInvalidItems(List<Object> invalidItems) {
		this.invalidItems = invalidItems;
	}
}

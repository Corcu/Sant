/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.util.interfaceImporter;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

/**
 * Context to control the fileReader, tradeMapper, tradePersistor and the logWriter. This context is the main skeleton
 * for the Exposure trades multithread interface.
 * 
 * @author aela
 * 
 */
public class ImportContext {

	private HashMap<String, String> productSubTypeMapping;
	private HashMap<String, InterfaceTradeBean> nonProcessedYet2LegProducts;
	private TradeImportTracker tradeImportTracker;
	private String sourceSystem;
	private String importType;
	private String importProcess;
	private String recordSpliter;
	private final AtomicBoolean fileReaderRunning = new AtomicBoolean(true);
	private final AtomicBoolean tradeMapperRunning = new AtomicBoolean(true);
	private final AtomicBoolean tradePersistorRunning = new AtomicBoolean(true);
	private final AtomicBoolean logWriterRunning = new AtomicBoolean(true);
	private final AtomicBoolean cacheTrades = new AtomicBoolean(true);
	private final BlockingQueue<String> fileReaderWaitQueue = new LinkedBlockingQueue<String>();
	private final BlockingQueue<String> tradeMapperWaitQueue = new LinkedBlockingQueue<String>();;
	private final BlockingQueue<String> tradePersistorWaitQueue = new LinkedBlockingQueue<String>();;
	private final BlockingQueue<String> logWriterWaitQueue = new LinkedBlockingQueue<String>();;
	
	// private final BlockingQueue<String> fileReaderWaitQueue;

	public boolean init(DSConnection dsCon, String importedFile, String summaryLog, String detailedLog, String fullLog,
			String staticLog, String sourceSystem, String importType, String importProcessName, String recordSpliter,
			BlockingQueue<TradeImportStatus> errorsLog) {
		// clear the list of errors
		// initialize the product mapping for this system
		this.sourceSystem = sourceSystem;
		this.importType = importType;
		this.importProcess = importProcessName;
		// this.productSubTypeMapping = CollateralUtilities.initMappingFromDomainValues(dsCon, sourceSystem); //GSM:
		// removed one call.
		this.tradeImportTracker = new TradeImportTracker(sourceSystem, importedFile, importProcessName, errorsLog);
		this.productSubTypeMapping = CollateralUtilities.initMappingFromDomainValues(dsCon, sourceSystem);
		this.recordSpliter = recordSpliter;
		// check if log file already exist
		this.tradeImportTracker.initLogFiles(summaryLog, detailedLog, fullLog, staticLog);

		this.nonProcessedYet2LegProducts = new HashMap<String, InterfaceTradeBean>();

		return true;
	}
	
	// START CALYPCROSS-38 - mromerod
	/**
	 * Method that allows to initialize the context of the ST ScheduledTaskImportCSVExposureTrades
	 * 
	 * @param dsCon
	 * @param importedFile
	 * @param summaryLog
	 * @param detailedLog
	 * @param fullLog
	 * @param staticLog
	 * @param sourceSystem
	 * @param importType
	 * @param importProcessName
	 * @param recordSpliter
	 * @param errorsLog
	 * @return
	 */
	public boolean initExposureTrades(DSConnection dsCon, String importedFile, String summaryLog, String detailedLog, String fullLog,
			String staticLog, String sourceSystem, String importType, String importProcessName, String recordSpliter,
			BlockingQueue<TradeImportStatus> errorsLog) {
		// clear the list of errors
		// initialize the product mapping for this system
		this.sourceSystem = sourceSystem;
		this.importType = importType;
		this.importProcess = importProcessName;
		this.tradeImportTracker = new TradeImportTracker(sourceSystem, importedFile, importProcessName, errorsLog);
		this.productSubTypeMapping = CollateralUtilities.initMappingFromDomainValuesExposureTrades(dsCon, sourceSystem);
		this.recordSpliter = recordSpliter;
		// check if log file already exist
		this.tradeImportTracker.initLogFiles(summaryLog, detailedLog, fullLog, staticLog);

		this.nonProcessedYet2LegProducts = new HashMap<String, InterfaceTradeBean>();

		return true;
	}
	// END CALYPCROSS-38 - mromerod
	
	public void markThreadsAsFalse(){	
		fileReaderRunning.set(false);
		tradeMapperRunning.set(false);
		tradePersistorRunning.set(false);
		logWriterRunning.set(false);
	}

	/**
	 * @return the productSubTypeMapping
	 */
	public HashMap<String, String> getProductSubTypeMapping() {
		return this.productSubTypeMapping;
	}

	/**
	 * @param productSubTypeMapping
	 *            the productSubTypeMapping to set
	 */
	public void setProductSubTypeMapping(HashMap<String, String> productSubTypeMapping) {
		this.productSubTypeMapping = productSubTypeMapping;
	}

	/**
	 * @return the nonProcessedYet2LegProducts
	 */
	public HashMap<String, InterfaceTradeBean> getNonProcessedYet2LegProducts() {
		return this.nonProcessedYet2LegProducts;
	}

	/**
	 * @param nonProcessedYet2LegProducts
	 *            the nonProcessedYet2LegProducts to set
	 */
	public void setNonProcessedYet2LegProducts(HashMap<String, InterfaceTradeBean> nonProcessedYet2LegProducts) {
		this.nonProcessedYet2LegProducts = nonProcessedYet2LegProducts;
	}

	/**
	 * @return the tradeImportTracker
	 */
	public TradeImportTracker getTradeImportTracker() {
		return this.tradeImportTracker;
	}

	/**
	 * @param tradeImportTracker
	 *            the tradeImportTracker to set
	 */
	public void setTradeImportTracker(TradeImportTracker tradeImportTracker) {
		this.tradeImportTracker = tradeImportTracker;
	}

	/**
	 * @return the sourceSystem
	 */
	public String getSourceSystem() {
		return this.sourceSystem;
	}

	/**
	 * @return the importProcess
	 */
	public String getImportProcess() {
		return this.importProcess;
	}

	/**
	 * @return the recordSpliter
	 */
	public String getRecordSpliter() {
		return this.recordSpliter;
	}

	/**
	 * @param recordSpliter
	 *            the recordSpliter to set
	 */
	public void setRecordSpliter(String recordSpliter) {
		this.recordSpliter = recordSpliter;
	}

	/**
	 * @return true if fileReader, tradePersistor and trademapper threads have finished!
	 */
	public boolean allThreadsHaveFinished() {
		return (!isFileReaderRunning() && !isTradePersistorRunning() && !isTradeMapperRunning());
	}

	/**
	 * if file reader is running
	 * 
	 * @return
	 */
	public boolean isFileReaderRunning() {
		return this.fileReaderRunning.get();
	}

	/**
	 * 
	 */
	public void stopFileReaderProcess() {
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "*****************>stopFileReaderProcess");
		this.fileReaderRunning.set(false);
		this.fileReaderWaitQueue.add("");
	}

	public void stopLogWriterProcess() {
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "*****************>stopLogWriterProcess");
		this.logWriterRunning.set(false);
		this.logWriterWaitQueue.add("");
	}

	public boolean isTradeMapperRunning() {
		return this.tradeMapperRunning.get();
	}

	public void stopTradeMapperProcess() {
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "*****************>stopTradeMapperProcess");
		this.tradeMapperRunning.set(false);
		this.tradeMapperWaitQueue.add("");
	}

	public boolean isTradePersistorRunning() {
		return this.tradePersistorRunning.get();
	}

	public boolean isLogWriterRunning() {
		return this.logWriterRunning.get();
	}

	/**
	 * @return the fileReaderWaitQueue
	 */
	public BlockingQueue<String> getFileReaderWaitQueue() {
		return this.fileReaderWaitQueue;
	}

	/**
	 * @return the fileReaderWaitQueue
	 */
	public BlockingQueue<String> getLogWriterWaitQueue() {
		return this.logWriterWaitQueue;
	}

	/**
	 * @return the tradeMapperWaitQueue
	 */
	public BlockingQueue<String> getTradeMapperWaitQueue() {
		return this.tradeMapperWaitQueue;
	}

	public void stopTradePersistorProcess() {
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "*****************>stopTradePersistorProcess");
		this.tradePersistorRunning.set(false);
		this.tradePersistorWaitQueue.add("");
	}

	/**
	 * @return the tradeMapperWaitQueue
	 */
	public BlockingQueue<String> getTradePersistorWaitQueue() {
		return this.tradePersistorWaitQueue;
	}

	/**
	 * @return the importType
	 */
	public String getImportType() {
		return this.importType;
	}

	/**
	 * @param importType
	 *            the importType to set
	 */
	public void setImportType(String importType) {
		this.importType = importType;
	}

	public void setUseCacheTrades(boolean useTradesCache) {
		cacheTrades.set(useTradesCache);		
	}
	
	public boolean isCacheTradesEnable(){
		return cacheTrades.get();
	}

}

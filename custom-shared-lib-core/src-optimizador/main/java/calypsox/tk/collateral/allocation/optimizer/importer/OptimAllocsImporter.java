/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer;

import java.io.File;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.util.importer.ProcessContext;
import calypsox.util.importer.ProcessExecutor;
import calypsox.util.importer.ProcessExecutorFactory;
import calypsox.util.importer.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;

/**
 * @author aela
 * 
 */
public class OptimAllocsImporter {

	protected OptimAllocsImportContext context = null;

	public OptimAllocsImporter(OptimAllocsImportContext context) {
		this.context = context;
	}

	/**
	 * 
	 * @param fileToProcess
	 * @return
	 */
	public boolean importFileAllocations(final String fileToProcess, JDatetime processingDate, JDatetime valuatioDate,
			int numberMappers, int numberPersistors, int mapperQueueSize, int persistorQueueSize,
			final int persistorBufferSize) {
		String path = "";
		final String fullFileName = path + fileToProcess;
		BlockingQueue<OptimContractAllocsBean> recordsList = new LinkedBlockingQueue<OptimContractAllocsBean>(
				mapperQueueSize);
		BlockingQueue<MarginCallEntry> marginCallEntriesList = new LinkedBlockingQueue<MarginCallEntry>(
				persistorQueueSize);
		BlockingQueue<Task> loggingQueue = new LinkedBlockingQueue<Task>(persistorBufferSize);

		// start the file reader
		ProcessExecutorLauncher<Object, OptimContractAllocsBean, Task> allocationsFileReaderExecutor = new ProcessExecutorLauncher<Object, OptimContractAllocsBean, Task>(
				1, null, recordsList, loggingQueue, this.context,
				new ProcessExecutorFactory<Object, OptimContractAllocsBean, Task>() {
					// ImportContext execContext = null;

					@Override
					public ProcessExecutor<Object, OptimContractAllocsBean, Task> createExecutor(
							BlockingQueue<Object> inWorkQueue, BlockingQueue<OptimContractAllocsBean> outWorkQueue,
							BlockingQueue<Task> loggingQueue, ProcessContext context) {
						// this.execContext = context;
						return new OptimAllocsFileProcessor(new File(fullFileName), null, outWorkQueue, loggingQueue,
								(OptimAllocsImportContext) context);
					}

					@Override
					public String getName() {
						return "AllocationsFileReaderExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return OptimAllocsImporter.this.context.getFileReaderWaitQueue();
					}

				});
		// start the allocation mapper
		ProcessExecutorLauncher<OptimContractAllocsBean, MarginCallEntry, Task> allocationsMapperExecutor = new ProcessExecutorLauncher<OptimContractAllocsBean, MarginCallEntry, Task>(
				numberMappers, recordsList, marginCallEntriesList, loggingQueue, this.context,
				new ProcessExecutorFactory<OptimContractAllocsBean, MarginCallEntry, Task>() {

					@Override
					public ProcessExecutor<OptimContractAllocsBean, MarginCallEntry, Task> createExecutor(
							BlockingQueue<OptimContractAllocsBean> inWorkQueue,
							BlockingQueue<MarginCallEntry> outWorkQueue, BlockingQueue<Task> loggingQueue,
							ProcessContext context) {
						return new OptimAllocsMapper(inWorkQueue, outWorkQueue, loggingQueue, context);
					}

					@Override
					public String getName() {
						return "AllocationsMapperExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return OptimAllocsImporter.this.context.getAllocationMapperWaitQueue();
					}

				});

		// start the allocation persistor
		ProcessExecutorLauncher<MarginCallEntry, Object, Task> allocationsPersistorExecutor = new ProcessExecutorLauncher<MarginCallEntry, Object, Task>(
				numberPersistors, marginCallEntriesList, null, loggingQueue, this.context,
				new ProcessExecutorFactory<MarginCallEntry, Object, Task>() {

					@Override
					public ProcessExecutor<MarginCallEntry, Object, Task> createExecutor(
							BlockingQueue<MarginCallEntry> inWorkQueue, BlockingQueue<Object> outWorkQueue,
							BlockingQueue<Task> loggingQueue, ProcessContext context) {
						return new OptimAllocsPersistor(inWorkQueue, null, loggingQueue,
								(OptimAllocsImportContext) context, persistorBufferSize);
					}

					@Override
					public String getName() {
						return "AllocationsPersistorExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return OptimAllocsImporter.this.context.getAllocationPersistorWaitQueue();
					}

				});

		// start the allocation persistor
		ProcessExecutorLauncher<Task, Object, Task> allocationsImportLoggerExecutor = new ProcessExecutorLauncher<Task, Object, Task>(
				1, loggingQueue, null, loggingQueue, this.context, new ProcessExecutorFactory<Task, Object, Task>() {

					@Override
					public ProcessExecutor<Task, Object, Task> createExecutor(BlockingQueue<Task> inWorkQueue,
							BlockingQueue<Object> outWorkQueue, BlockingQueue<Task> loggingQueue, ProcessContext context) {
						return new OptimAllocsImportLogger(inWorkQueue, null, loggingQueue,
								(OptimAllocsImportContext) context, persistorBufferSize);
					}

					@Override
					public String getName() {
						return "AllocationsImportLoggerExecutor";
					}

					@Override
					public BlockingQueue<String> getWaitingQueue() {
						return OptimAllocsImporter.this.context.getAllocationPersistorWaitQueue();
					}

				});

		allocationsFileReaderExecutor.shutDown();
		allocationsMapperExecutor.shutDown();
		allocationsPersistorExecutor.shutDown();
		allocationsImportLoggerExecutor.shutDown();

		return true;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		try {
			DSConnection dsConDevCo4 = ConnectionUtil.connect(args, "MainEntry");

			OptimAllocsImportContext context = new OptimAllocsImportContext("|", true);
			final JDate valuatioDate = Holiday.getCurrent().addBusinessDays(new JDatetime().getJDate(TimeZone.getDefault()),
					DSConnection.getDefault().getUserDefaults().getHolidays(), -1);
			context.init(new JDatetime(), new JDatetime(valuatioDate, TimeZone.getDefault()), "DirtyPrice");
			new OptimAllocsImporter(context).importFileAllocations("/Users/amino/Downloads/Allocations.txt",
					new JDatetime(), new JDatetime(), 10, 10, 100, 100, 100);
		} catch (Exception e) {
			Log.error(OptimAllocsImporter.class, e); //sonar
		}
		System.out.println("------");
	}
}

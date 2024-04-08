/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import calypsox.tk.util.ScheduledTaskUnPriceTrades;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

public class SantImportMTMThreadPolFactory implements ThreadFactory {

	private final AtomicInteger threadNumber = new AtomicInteger(0);
	protected String poolName;
	protected ArrayList<String> errorMessages;

	SantImportMTMThreadPolFactory(String poolName, ArrayList<String> errorMessages) {
		this.poolName = poolName;
		this.errorMessages = errorMessages;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread t = new Thread(runnable, this.poolName + "_" + this.threadNumber.incrementAndGet());

		t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				SantImportMTMThreadPolFactory.this.errorMessages.add("Error in thread " + thread.getName()
						+ "; Message=" + throwable.getMessage());
				Log.error("SantImportMTMWindow", "Error in thread " + thread.getName(), throwable);
			}
		});

		return t;
	}

	public static ExecutorService getBoundedQueueThreadPoolExecutor(int noOfThreads, ArrayList<String> errorMessages) {
		BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(noOfThreads * 2);
		ExecutorService executor = new ThreadPoolExecutor(noOfThreads, noOfThreads, 0, TimeUnit.SECONDS, blockingQueue,
				new SantImportMTMThreadPolFactory("UnPriceTradeThreadPool", errorMessages),
				new ThreadPoolExecutor.CallerRunsPolicy());
		return executor;
	}

	public static void shutdownExecutor(ExecutorService executor) {

		executor.shutdown();
		try {
			// Await until all the tasks are completed
			executor.awaitTermination(2, TimeUnit.MINUTES);
			Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been terminated=" + executor.isTerminated());
			Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been shutdown=" + executor.isShutdown());
		} catch (InterruptedException e) {
			Log.error(ScheduledTaskUnPriceTrades.class, e);
			executor.shutdownNow();
		}
	}

	public static int calculateThredPoolSize(HashSet<Trade> tradesToSave) {
		int noOfThreads = 1;
		int tradeCount = tradesToSave.size();

		if (tradeCount > 10) {
			if (tradeCount < 100) {
				noOfThreads = 5;
			} else {
				noOfThreads = 10;
			}
		}

		return noOfThreads;
	}
}

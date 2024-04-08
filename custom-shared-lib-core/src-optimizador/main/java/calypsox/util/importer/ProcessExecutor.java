/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.importer;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.calypso.tk.core.Log;

public abstract class ProcessExecutor<IN, OUT, LOG> implements Runnable {

	private final BlockingQueue<IN> inWorkQueue;

	private final BlockingQueue<OUT> outWorkQueue;

	private BlockingQueue<LOG> logWorkQueue;

	protected final ProcessContext context;

	public ProcessExecutor(BlockingQueue<IN> inWorkQueue,
			BlockingQueue<OUT> outWorkQueue, BlockingQueue<LOG> logWorkQueue,
			ProcessContext context) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.logWorkQueue = logWorkQueue;
		this.context = context;
	}

	@Override
	public void run() {
		getProcessCounter().add(getExecutorName());
		try {
			if (this.inWorkQueue != null) {
				IN inItem = this.inWorkQueue.poll(2, TimeUnit.SECONDS);
				while (getHasToContinue() || (inItem != null)) {
					try {
						OUT outItem = execute(inItem);

						if (outItem != null) {
							this.outWorkQueue.put(outItem);
						}
					}
					catch (Exception ex) {
						Log.error(this, ex);
					}
					finally {
						inItem = this.inWorkQueue.poll(2, TimeUnit.SECONDS);
					}
				}
			}
			else {
				try {
					execute(null);
				}
				catch (Exception e) {
					Log.error(this, e);
				}
			}
		}
		catch (InterruptedException ex) {
			// Thread.currentThread().interrupt();
		}
		finally {
			dispose();
		}
	}

	/**
	 * @param item
	 * @return
	 * @throws Exception
	 */
	public OUT execute(IN item) throws Exception {
		return null;
	};

	/**
	 * @param log
	 */
	public void addLog(LOG log) {
		if (log != null) {
			logWorkQueue.add(log);
		}
	}

	/**
	 * @param log
	 */
	public void addLogs(List<LOG> logs) {
		if (logs != null && logs.size() > 0) {
			logWorkQueue.addAll(logs);
		}
	}

	/**
	 * @return the hasToContinue
	 */
	public boolean getHasToContinue() {
		return false;
	}

	public void dispose() {
		finishPendingWork();
		stopProcess();
	}

	protected void stopProcess() {
	}

	protected abstract HashSet<String> getProcessCounter();

	public void finishPendingWork() {
	}

	public static String getExecutorName() {
		return Thread.currentThread().getName();
	}

	protected int getInQueueSize() {
		return inWorkQueue == null ? 0 : inWorkQueue.size();
	}

	protected int getOutQueueSize() {
		return outWorkQueue == null ? 0 : outWorkQueue.size();
	}

}

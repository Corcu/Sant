/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.util.processor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;

/**
 * @author aela
 * 
 * @param <IN>
 * @param <OUT>
 * @param <LOG>
 */
public abstract class ProcessExecutor<IN, OUT, LOG> implements Runnable {

	private final BlockingQueue<IN> inWorkQueue;

	private final BlockingQueue<OUT> outWorkQueue;

	private BlockingQueue<LOG> logWorkQueue;

	protected final ProcessContext context;
	
	private boolean stillRunning;	
	
	protected List<ProcessExecutorLauncher> prodcuersLauncher;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 * @param prodcuersLauncher
	 */
	public ProcessExecutor(BlockingQueue<IN> inWorkQueue,
			BlockingQueue<OUT> outWorkQueue, BlockingQueue<LOG> logWorkQueue,
			ProcessContext context, List<ProcessExecutorLauncher> prodcuersLauncher) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.logWorkQueue = logWorkQueue;
		this.prodcuersLauncher = prodcuersLauncher;
		stillRunning = true;
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
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
	public abstract OUT execute(IN item) throws Exception;

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
		// by default we will consider that if no producer is there so it means that it's still running
		boolean isStillRunning = true;
		
		if(!Util.isEmpty(prodcuersLauncher)) {
			isStillRunning = false;
			for (ProcessExecutorLauncher launcher : prodcuersLauncher) {
				isStillRunning |= launcher.isProcessStillRunning();
			}
		}
		return isStillRunning || !Util.isEmpty(inWorkQueue);
	}

	public void dispose() {
		finishPendingWork();
		stopProcess();
		setStillRunning(false);
	}

	protected void stopProcess() {
	}

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

	/**
	 * @return the logWorkQueue
	 */
	public BlockingQueue<LOG> getLogWorkQueue() {
		return logWorkQueue;
	}

	/**
	 * @param logWorkQueue the logWorkQueue to set
	 */
	public void setLogWorkQueue(BlockingQueue<LOG> logWorkQueue) {
		this.logWorkQueue = logWorkQueue;
	}

	/**
	 * @return the inWorkQueue
	 */
	public BlockingQueue<IN> getInWorkQueue() {
		return inWorkQueue;
	}

	/**
	 * @return the outWorkQueue
	 */
	public BlockingQueue<OUT> getOutWorkQueue() {
		return outWorkQueue;
	}

	/**
	 * @return the context
	 */
	public ProcessContext getContext() {
		return context;
	}

	/**
	 * @return the producersStillRunning
	 */
	public boolean isStillRunning() {
		return stillRunning;
	}

	/**
	 * @param producersStillRunning the producersStillRunning to set
	 */
	public void setStillRunning(boolean producersStillRunning) {
		this.stillRunning = producersStillRunning;
	}

}

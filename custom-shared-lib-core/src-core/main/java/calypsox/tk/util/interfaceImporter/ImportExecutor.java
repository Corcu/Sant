/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.interfaceImporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.calypso.tk.core.Log;

public class ImportExecutor<IN, OUT> implements Runnable {

	private final BlockingQueue<IN> inWorkQueue;
	private final BlockingQueue<OUT> outWorkQueue;
	protected final ImportContext context;

	public ImportExecutor(BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue, ImportContext context) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.context = context;
	}

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
					} catch (Exception ex) {
						Log.error(this, ex);
					} finally {
						inItem = this.inWorkQueue.poll(2, TimeUnit.SECONDS);
					}
				}
			} else {
				try {
					execute(null);
				} catch (Exception e) {
					Log.error(this, e);
				}
			}
		} catch (InterruptedException ex) {
			// Thread.currentThread().interrupt();
		} finally {
			dispose();
		}
	}

	public OUT execute(IN item) throws Exception {
		return null;
	};

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

	public void finishPendingWork() {
	}

	public static String getExecutorName() {
		return Thread.currentThread().getName();
	}

}

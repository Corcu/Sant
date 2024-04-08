/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.interfaceImporter;

import java.util.concurrent.BlockingQueue;

public abstract class ImportExecutorFactory<IN, OUT> {
	public abstract ImportExecutor<IN, OUT> createExecutor(BlockingQueue<IN> inWorkQueue,
			BlockingQueue<OUT> outWorkQueue, ImportContext context);

	public abstract String getName();

	public abstract BlockingQueue<String> getWaitingQueue();
}

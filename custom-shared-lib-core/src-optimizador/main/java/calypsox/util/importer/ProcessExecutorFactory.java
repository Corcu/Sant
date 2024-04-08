/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.importer;

import java.util.concurrent.BlockingQueue;

public abstract class ProcessExecutorFactory<IN, OUT, LOG> {
	public abstract ProcessExecutor<IN, OUT, LOG> createExecutor(
			BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue,
			BlockingQueue<LOG> loggingQueue, ProcessContext context);

	public abstract String getName();

	public abstract BlockingQueue<String> getWaitingQueue();
}

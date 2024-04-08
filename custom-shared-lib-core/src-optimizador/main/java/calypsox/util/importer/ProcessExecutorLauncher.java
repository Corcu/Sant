package calypsox.util.importer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.calypso.tk.core.Log;

@SuppressWarnings("unused")
public class ProcessExecutorLauncher<IN, OUT, LOG> {

	private final BlockingQueue<IN> inWorkQueue;

	private final BlockingQueue<OUT> outWorkQueue;

	private final BlockingQueue<LOG> loggingQueue;

	private final BlockingQueue<ProcessExecutor<IN, OUT, LOG>> executorQueue;

	private final ExecutorService service;

	private ProcessContext context = null;

	private final int numWorkers;

	private final BlockingQueue<String> waitQueue;

	public ProcessExecutorLauncher(int numWorkers,
			BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue,
			BlockingQueue<LOG> loggingQueue, ProcessContext context,
			ProcessExecutorFactory<IN, OUT, LOG> factory) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.loggingQueue = loggingQueue;
		this.numWorkers = numWorkers;
		this.waitQueue = factory.getWaitingQueue();
		this.executorQueue = new ArrayBlockingQueue<ProcessExecutor<IN, OUT, LOG>>(
				numWorkers);
		this.context = context;
		this.service = Executors.newFixedThreadPool(numWorkers,
				new ProcessExecutorThreadFactory(factory.getName()));

		for (int i = 0; i < numWorkers; i++) {
			ProcessExecutor<IN, OUT, LOG> executor = factory.createExecutor(
					inWorkQueue, outWorkQueue, loggingQueue, context);
			this.executorQueue.add(executor);
			this.service.execute(executor);
		}
	}

	public void shutDown() {
		try {
			if (this.waitQueue != null) {
				for (int i = 0; i < this.numWorkers; i++) {
					this.waitQueue.take();
				}
			}
		}
		catch (InterruptedException e) {
			Log.error(this, e); //sonar
		}
		if ((this.executorQueue != null) && (this.executorQueue.size() > 0)) {
			for (ProcessExecutor<IN, OUT, LOG> executor : this.executorQueue) {
				executor.dispose();
			}
		}
		this.service.shutdown();
	}

}
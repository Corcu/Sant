package calypsox.tk.util.interfaceImporter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.calypso.tk.core.Log;


@SuppressWarnings("unused")
public class InterfaceImportExecutor<IN, OUT> {

	private final BlockingQueue<IN> inWorkQueue;
	private final BlockingQueue<OUT> outWorkQueue;
	private final BlockingQueue<ImportExecutor<IN, OUT>> executorQueue;
	private final ExecutorService service;
	private ImportContext context = null;
	private final int numWorkers;

	private final BlockingQueue<String> waitQueue;

	public InterfaceImportExecutor(int numWorkers, BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue,
			ImportContext context, ImportExecutorFactory<IN, OUT> factory) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.numWorkers = numWorkers;
		this.waitQueue = factory.getWaitingQueue();
		this.executorQueue = new ArrayBlockingQueue<ImportExecutor<IN, OUT>>(numWorkers);
		this.context = context;
		this.service = Executors.newFixedThreadPool(numWorkers, new ImportExecutorThreadFactory(factory.getName()));

		for (int i = 0; i < numWorkers; i++) {
			ImportExecutor<IN, OUT> executor = factory.createExecutor(inWorkQueue, outWorkQueue, context);
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
		} catch (InterruptedException e) {
			Log.error(this, e); //sonar
		}
		if ((this.executorQueue != null) && (this.executorQueue.size() > 0)) {
			for (ImportExecutor<IN, OUT> executor : this.executorQueue) {
				executor.dispose();
			}
		}
		this.service.shutdown();
	}

}
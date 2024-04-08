package calypsox.tk.collateral.util.processor;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.calypso.tk.core.Log;

/**
 * @author aela
 *
 * @param <IN>
 * @param <OUT>
 * @param <LOG>
 */
public class DefaultProcessExecutorLauncher<IN, OUT, LOG> implements ProcessExecutorLauncher {

	private  BlockingQueue<IN> inWorkQueue;

	private  BlockingQueue<OUT> outWorkQueue;

	private  BlockingQueue<LOG> loggingQueue;

	private  BlockingQueue<ProcessExecutor<IN, OUT, LOG>> executorQueue;

	private  ExecutorService service;

	private ProcessContext context = null;
	
	private DefaultProcessExecutorFactory<IN, OUT, LOG> executorFactory;

	private  int numWorkers;
	
	protected List<ProcessExecutorLauncher> prodcuersLauncher;
	
	/**
	 * @param numWorkers
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param loggingQueue
	 * @param context
	 * @param executorFactory
	 */
	public DefaultProcessExecutorLauncher(int numWorkers,
			BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue,
			BlockingQueue<LOG> loggingQueue, ProcessContext context, List<ProcessExecutorLauncher> prodcuersLauncher) {
		this.inWorkQueue = inWorkQueue;
		this.outWorkQueue = outWorkQueue;
		this.loggingQueue = loggingQueue;
		this.numWorkers = numWorkers;
		this.prodcuersLauncher = prodcuersLauncher;
		this.executorQueue = new ArrayBlockingQueue<ProcessExecutor<IN, OUT, LOG>>(
				numWorkers);
		this.context = context;
	}
	
	/**
	 * 
	 */
	public DefaultProcessExecutorLauncher() {
	}
	/**
	 * 
	 */
	public void start() throws Exception{
		if (executorFactory == null) {
			throw new Exception("No executor factory found");
		}
		this.service = Executors.newFixedThreadPool(numWorkers,
				new ProcessExecutorThreadFactory(executorFactory.getName()));

		for (int i = 0; i < numWorkers; i++) {
			ProcessExecutor<IN, OUT, LOG> executor = executorFactory
					.createExecutor(inWorkQueue, outWorkQueue, loggingQueue,
							context, prodcuersLauncher);
			this.executorQueue.add(executor);
			this.service.execute(executor);
		}
		
	}

	/**
	 * 
	 */
	public void shutDown() {
		try {
			
			this.service.shutdown();
			this.service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

			if ((this.executorQueue != null) && (this.executorQueue.size() > 0)) {
				for (ProcessExecutor<IN, OUT, LOG> executor : this.executorQueue) {
					executor.dispose();
				}
			}
		}
		catch (InterruptedException e) {
			Log.error(this, e); //sonar
		}
	}


	/**
	 * @return the context
	 */
	public ProcessContext getContext() {
		return context;
	}


	/**
	 * @param context the context to set
	 */
	public void setContext(ProcessContext context) {
		this.context = context;
	}


	/**
	 * @return the executorFactory
	 */
	public DefaultProcessExecutorFactory<IN, OUT, LOG> getExecutorFactory() {
		return executorFactory;
	}


	/**
	 * @param executorFactory the executorFactory to set
	 */
	public void setExecutorFactory(
			DefaultProcessExecutorFactory<IN, OUT, LOG> executorFactory) {
		this.executorFactory = executorFactory;
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
	 * @return the loggingQueue
	 */
	public BlockingQueue<LOG> getLoggingQueue() {
		return loggingQueue;
	}


	/**
	 * @return the executorQueue
	 */
	public BlockingQueue<ProcessExecutor<IN, OUT, LOG>> getExecutorQueue() {
		return executorQueue;
	}


	/**
	 * @return the numWorkers
	 */
	public int getNumWorkers() {
		return numWorkers;
	}
	
	/**
	 * @return
	 */
	public boolean isProcessStillRunning(){
		boolean stillRunning = false;
		if ((this.executorQueue != null) && (this.executorQueue.size() > 0)) {
			for (ProcessExecutor<IN, OUT, LOG> executor : this.executorQueue) {
				stillRunning |=executor.isStillRunning();
			}
		} 
		return stillRunning;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setExecutorFactory(ProcessExecutorFactory factory) {
		executorFactory = (DefaultProcessExecutorFactory<IN, OUT, LOG>)factory;
	}

}
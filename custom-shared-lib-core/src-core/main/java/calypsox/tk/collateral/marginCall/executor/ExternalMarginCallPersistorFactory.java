package calypsox.tk.collateral.marginCall.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Trade;

public class ExternalMarginCallPersistorFactory extends
		DefaultProcessExecutorFactory<Trade, Object, Task> {

	public static final String NAME = "ExternalMarginCallPersistor";
	private int mcToSaveBufferSize = 1;

	public ExternalMarginCallPersistorFactory() {
	}

	public ExternalMarginCallPersistorFactory(int mcToSaveBufferSize) {
		this.mcToSaveBufferSize = mcToSaveBufferSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutorFactory#createExecutor
	 * (java.util.concurrent.BlockingQueue, java.util.concurrent.BlockingQueue,
	 * java.util.concurrent.BlockingQueue,
	 * calypsox.tk.collateral.util.processor.ProcessContext)
	 */
	@Override
	public ProcessExecutor<Trade, Object, Task> createExecutor(
			BlockingQueue<Trade> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,
			List<ProcessExecutorLauncher> producersLauncher) {
		return new ExternalMarginCallPersistorExecutor(inWorkQueue,
				outWorkQueue, loggingQueue, context, producersLauncher,
				this.mcToSaveBufferSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutorFactory#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

}

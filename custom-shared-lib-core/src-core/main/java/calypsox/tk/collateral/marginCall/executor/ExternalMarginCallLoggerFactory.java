package calypsox.tk.collateral.marginCall.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;

public class ExternalMarginCallLoggerFactory extends
		DefaultProcessExecutorFactory<Task, Object, Task> {

	public static final String NAME = "ExternalMarginCallLogger";

	@Override
	public ProcessExecutor<Task, Object, Task> createExecutor(
			BlockingQueue<Task> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,
			List<ProcessExecutorLauncher> prodcuersLauncher) {
		return new ExternalMarginCallLoggerExecutor(inWorkQueue, outWorkQueue,
				loggingQueue, context, prodcuersLauncher);
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

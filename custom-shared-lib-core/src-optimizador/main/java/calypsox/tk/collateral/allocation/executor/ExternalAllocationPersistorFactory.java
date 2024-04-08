/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 *
 */
public class ExternalAllocationPersistorFactory extends
		DefaultProcessExecutorFactory<MarginCallEntry, Object, Task> {
	
	public static final String NAME = "ExternalAlloationPersistor";

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutorFactory#createExecutor(java.util.concurrent.BlockingQueue, java.util.concurrent.BlockingQueue, java.util.concurrent.BlockingQueue, calypsox.tk.collateral.util.processor.ProcessContext)
	 */
	@Override
	public ProcessExecutor<MarginCallEntry, Object, Task> createExecutor(
			BlockingQueue<MarginCallEntry> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,List<ProcessExecutorLauncher> prodcuersLauncher ) {
		return new ExternalAllcoationPersistorExecutor(inWorkQueue, outWorkQueue, loggingQueue, context, prodcuersLauncher);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutorFactory#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

}

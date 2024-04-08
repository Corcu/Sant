/**
 * 
 */
package calypsox.tk.collateral.marginCall.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Trade;

public class ExternalMarginCallMapperFactory extends
		DefaultProcessExecutorFactory<ExternalMarginCallBean, Trade, Task> {

	public static final String NAME = "ExternalMarginCallMapper";

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
	public ProcessExecutor<ExternalMarginCallBean, Trade, Task> createExecutor(
			BlockingQueue<ExternalMarginCallBean> inWorkQueue,
			BlockingQueue<Trade> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,
			List<ProcessExecutorLauncher> prodcuersLauncher) {

		return new ExternalMarginCallMapperExecutor(inWorkQueue, outWorkQueue,
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

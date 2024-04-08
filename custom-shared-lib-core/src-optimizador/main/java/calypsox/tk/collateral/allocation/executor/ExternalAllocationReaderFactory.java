/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;

/**
 * @author aela
 * 
 */
public class ExternalAllocationReaderFactory extends
		DefaultProcessExecutorFactory<Object, ExternalAllocationBean, Task> {

	public static final String NAME = "ExternalAlloationReader";
	
	protected InputStream is;

	/**
	 * @param is
	 */
	public ExternalAllocationReaderFactory(InputStream is) {
		this.is = is;
	}
	
	@Override
	public ProcessExecutor<Object, ExternalAllocationBean, Task> createExecutor(
			BlockingQueue<Object> inWorkQueue,
			BlockingQueue<ExternalAllocationBean> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,List<ProcessExecutorLauncher> prodcuersLauncher) {

		ExternalAllocationReaderExecutor reader = new ExternalAllocationReaderExecutor(inWorkQueue, outWorkQueue, loggingQueue, context, prodcuersLauncher);
		reader.setIs(is);
		return reader;
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

	/**
	 * @return the is
	 */
	public InputStream getIs() {
		return is;
	}
}

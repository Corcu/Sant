package calypsox.tk.collateral.marginCall.executor;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.util.processor.DefaultProcessExecutorFactory;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;

public class ExternalMarginCallReaderFactory extends
		DefaultProcessExecutorFactory<Object, ExternalMarginCallBean, Task> {

	public static final String NAME = "ExternalMarginCallReader";

	protected InputStream is;

	/**
	 * @param is
	 */
	public ExternalMarginCallReaderFactory(InputStream is) {
		this.is = is;
	}

	@Override
	public ProcessExecutor<Object, ExternalMarginCallBean, Task> createExecutor(
			BlockingQueue<Object> inWorkQueue,
			BlockingQueue<ExternalMarginCallBean> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context,
			List<ProcessExecutorLauncher> prodcuersLauncher) {

		ExternalMarginCallReaderExecutor reader = new ExternalMarginCallReaderExecutor(
				inWorkQueue, outWorkQueue, loggingQueue, context,
				prodcuersLauncher);
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

package calypsox.tk.collateral.marginCall.executor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.reader.FileExternalMarginCallReader;
import calypsox.tk.collateral.util.ExternalMarginCallImportUtils;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;

public class ExternalMarginCallReaderExecutor extends
		ProcessExecutor<Object, ExternalMarginCallBean, Task> {

	protected InputStream is;
	protected ExternalMarginCallImportContext importContext;

	public ExternalMarginCallReaderExecutor(BlockingQueue<Object> inWorkQueue,
			BlockingQueue<ExternalMarginCallBean> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,
			List<ProcessExecutorLauncher> producersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context,
				producersLauncher);
		importContext = (ExternalMarginCallImportContext) context;
	}

	@Override
	public ExternalMarginCallBean execute(Object item) throws Exception {

		FileExternalMarginCallReader reader = new FileExternalMarginCallReader(
				is, importContext);
		List<MarginCallImportErrorBean> errors = new ArrayList<MarginCallImportErrorBean>();
		getOutWorkQueue().addAll(reader.readMarginCalls(errors));

		if (!Util.isEmpty(errors)) {
			importContext.getInvalidItems().addAll(errors);
			List<String> messagesToTasks = new ArrayList<String>();
			for (MarginCallImportErrorBean error : errors) {
				messagesToTasks.add(error.getValue());
			}
			logMessages(null, importContext, messagesToTasks);
			return null;
		}

		return null;
	}

	/**
	 * @return the is
	 */
	public InputStream getIs() {
		return is;
	}

	/**
	 * @param is
	 *            the is to set
	 */
	public void setIs(InputStream is) {
		this.is = is;
	}

	/**
	 * @param entry
	 * @param messages
	 */
	private void logMessages(MarginCall mc,
			ExternalMarginCallImportContext importContext, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(ExternalMarginCallImportUtils.messagesToTasks(mc,
					importContext, messages));
		}
		messages.clear();
	}

}

/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.reader.StringExternalAllocationReader;
import calypsox.tk.collateral.util.ExternalAllocationImportUtils;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 *
 */
public class ExternalAllocationReaderExecutor extends
		ProcessExecutor<Object, ExternalAllocationBean, Task> {
	
	protected InputStream is;
	protected ExternalAllocationImportContext importContext;
	
	public ExternalAllocationReaderExecutor(BlockingQueue<Object> inWorkQueue,
			BlockingQueue<ExternalAllocationBean> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context, List<ProcessExecutorLauncher> prodcuersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context, prodcuersLauncher);
		 importContext = (ExternalAllocationImportContext)context;
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang.Object)
	 */
	@Override
	public ExternalAllocationBean execute(Object item)
			throws Exception {
		
		StringExternalAllocationReader reader = new StringExternalAllocationReader(is,importContext);
		List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
		getOutWorkQueue().addAll(reader.readAllocations(errors));
		if (!Util.isEmpty(errors)) {
			importContext.getInvalidItems().addAll(errors);
			List<String> messagesToTasks = new ArrayList<String>();
			for(AllocImportErrorBean error:errors) {
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
	 * @param is the is to set
	 */
	public void setIs(InputStream is) {
		this.is = is;
	}
	/**
	 * @param entry
	 * @param messages
	 */
	private void logMessages(MarginCallEntry entry,ExternalAllocationImportContext importContext, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(ExternalAllocationImportUtils.messagesToTasks(entry, importContext, messages));
		}
		messages.clear();
	}
}

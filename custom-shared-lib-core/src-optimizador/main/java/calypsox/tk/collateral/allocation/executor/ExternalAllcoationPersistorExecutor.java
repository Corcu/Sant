/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.persistor.AbstractExternalAllocationPersistor;
import calypsox.tk.collateral.allocation.persistor.ExternalAllocationPersistor;
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
public class ExternalAllcoationPersistorExecutor extends
		ProcessExecutor<MarginCallEntry, Object, Task> {

	protected ExternalAllocationPersistor persistor;

	protected ExternalAllocationImportContext context;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 * @param prodcuersLauncher
	 */
	public ExternalAllcoationPersistorExecutor(
			BlockingQueue<MarginCallEntry> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,
			List<ProcessExecutorLauncher> prodcuersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context,
				prodcuersLauncher);
		this.context = (ExternalAllocationImportContext) context;
		persistor = new AbstractExternalAllocationPersistor(
				(ExternalAllocationImportContext) context);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang.Object)
	 */
	@Override
	public Object execute(MarginCallEntry entry) throws Exception {

		if (entry == null) {
			return null;
		}
		
		List<String> messages = new ArrayList<String>();
		String actionToApply = "ALLOCATE";
		if (entry.getAction() != null) {
			actionToApply = entry.getAction().toString();
		}
		//
		// List<String> allocationsExtId = getAllocationsExtId(entry);
		// execute the mapping
		persistor.persistEntry(entry, actionToApply, messages);
		// add the generated logs to the logging queue
		if (!Util.isEmpty(messages)) {
			logMessages(entry, messages);
		}
		return null;
	}

	/**
	 * @return the persistor
	 */
	public ExternalAllocationPersistor getPersistor() {
		return persistor;
	}

	/**
	 * @param persistor the persistor to set
	 */
	public void setPersistor(ExternalAllocationPersistor persistor) {
		this.persistor = persistor;
	}

	/**
	 * @param entry
	 * @param messages
	 */
	private void logMessages(MarginCallEntry entry, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(ExternalAllocationImportUtils.messagesToTasks(entry,
					context, messages));
		}
		messages.clear();
	}
}

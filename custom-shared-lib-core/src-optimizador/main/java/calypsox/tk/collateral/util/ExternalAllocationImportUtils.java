/**
 * 
 */
package calypsox.tk.collateral.util;

import java.util.List;

import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsLoggerHelper;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 * 
 */
public class ExternalAllocationImportUtils {

	public static final String TASK_EXCEPTION_TYPE = "EX_PDV_ALLOC";

	public static List<Task> messagesToTasks(MarginCallEntry entry,
			ExternalAllocationImportContext context, List<String> messages) {
		List<Task> tasks = messagesToTasks(entry, context.getExecutionId(), messages);
		// Add tasks to context in order to publish them
		if (!Util.isEmpty(tasks)) {
			context.getTasksToPublish().addAll(tasks);
		}
		return tasks;
	}

	
	/**
	 * @param entry
	 * @param executionId
	 * @param messages
	 * @return
	 */
	public static List<Task> messagesToTasks(MarginCallEntry entry,
			Long executionId, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			return OptimAllocsLoggerHelper.getLogsAsTasks(entry,
					TASK_EXCEPTION_TYPE, executionId, messages);
		}
		messages.clear();
		return null;
	}
}

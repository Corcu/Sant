/**
 * 
 */
package calypsox.tk.collateral.util;

import java.util.List;

import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.logger.MarginCallLoggerHelper;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.product.MarginCall;

/**
 * @author aela
 * 
 */
public class ExternalMarginCallImportUtils {

	public static final String TASK_EXCEPTION_TYPE = "EX_PDV_MARGIN_CALL";

	public static List<Task> messagesToTasks(MarginCall mc,
			ExternalMarginCallImportContext context, List<String> messages) {
		List<Task> tasks = messagesToTasks(mc, context.getExecutionId(),
				messages);
		// Add tasks to context in order to publish them
		if (!Util.isEmpty(tasks)) {
			context.getTasksToPublish().addAll(tasks);
		}
		return tasks;
	}

	/**
	 * @param mc
	 * @param executionId
	 * @param errors
	 * @return
	 */
	public static List<Task> messagesToTasks(MarginCall mc, Long executionId,
			List<String> errors) {
		if (!Util.isEmpty(errors)) {
			return MarginCallLoggerHelper.getLogsAsTasks(mc,
					TASK_EXCEPTION_TYPE, executionId, errors);
		}
		errors.clear();
		return null;
	}
}

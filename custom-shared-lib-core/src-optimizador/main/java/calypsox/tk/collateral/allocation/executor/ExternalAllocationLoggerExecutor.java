/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.core.Util;

/**
 * @author aela
 *
 */
public class ExternalAllocationLoggerExecutor extends
		ProcessExecutor<Task, Object, Task> {
	
	private List<Task> tasksToBeSavedBuffer = new ArrayList<Task>();
	private int tasksToSaveBufferSize = 100;
	protected ExecutionContext allocExecContext;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 */
	public ExternalAllocationLoggerExecutor(BlockingQueue<Task> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,List<ProcessExecutorLauncher> prodcuersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context, prodcuersLauncher);
	}	

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang.Object)
	 */
	@Override
	public Object execute(Task task) throws Exception {
		if (task == null) {
			return null;
		}
		
		tasksToBeSavedBuffer.add(task);
		if (this.tasksToBeSavedBuffer.size() >= this.tasksToSaveBufferSize) {
			// call to flush the detailed, full log. Also the ".bad" to be reprocessed (warnings)
			// Do not publish tasks, engine will do it
			//OptimAllocsLoggerHelper.publishTask(OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,this.tasksToBeSavedBuffer);
			this.tasksToBeSavedBuffer.clear();

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutor#finishPendingWork()
	 */
	@Override
	public void finishPendingWork() {
		if(!Util.isEmpty(tasksToBeSavedBuffer)){
			// Do not publish tasks, engine will do it
			//OptimAllocsLoggerHelper.publishTask(OptimAllocsImportConstants.TASK_SOURCE_OPTIMIZER,this.tasksToBeSavedBuffer);
			tasksToBeSavedBuffer.clear();
		}
	}
}

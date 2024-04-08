package calypsox.tk.collateral.allocation.optimizer.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.util.importer.ProcessExecutor;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.command.ExecutionContext;

/**
 * 
 * @author aela
 * 
 */
public class OptimAllocsImportLogger extends
		ProcessExecutor<Task, Object, Task> {

	private List<Task> tasksToBeSavedBuffer = null;
	protected OptimAllocsLoggerHelper loggingHelper = new OptimAllocsLoggerHelper();

	private int tasksToSaveBufferSize = 100;

	protected OptimAllocsImportContext context;

	protected ExecutionContext allocExecContext;

	public OptimAllocsImportLogger(BlockingQueue<Task> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, OptimAllocsImportContext context) {
		super(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.context = context;
		allocExecContext = context
				.getExecutionContextWithUserAction("Allocate");
		this.tasksToBeSavedBuffer = new ArrayList<Task>(
				this.tasksToSaveBufferSize);
	}

	public OptimAllocsImportLogger(BlockingQueue<Task> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, OptimAllocsImportContext context,
			int tasksToSaveBufferSize) {
		this(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.tasksToSaveBufferSize = tasksToSaveBufferSize;
	}

	@SuppressWarnings("static-access")
	@Override
	public Object execute(Task task) throws Exception {
		if (task == null) {
			return null;
		}
		
		tasksToBeSavedBuffer.add(task);
		if (this.tasksToBeSavedBuffer.size() >= this.tasksToSaveBufferSize) {
			// call to flush the detailed, full log. Also the ".bad" to be reprocessed (warnings)
			loggingHelper.publishTask(OptimAllocsImportConstants.TASK_SOURCE_OPTIMIZER,this.tasksToBeSavedBuffer);
			this.tasksToBeSavedBuffer.clear();

		}
		return null;
	}

	@SuppressWarnings("static-access")
	@Override
	public void finishPendingWork() {
		if(!Util.isEmpty(tasksToBeSavedBuffer)){
			loggingHelper.publishTask(OptimAllocsImportConstants.TASK_SOURCE_OPTIMIZER,this.tasksToBeSavedBuffer);
		}
	}

	@Override
	protected HashSet<String> getProcessCounter() {
		return context.getAllocationImportLoggerCounter();
	}

	/**
	 * @return the hasToContinue
	 */
	@Override
	public boolean getHasToContinue() {
		return this.context.isAllocationPersistorRunning() || getInQueueSize() > 0;
	}

	/**
	 * Stops this process
	 */
	@Override
	protected void stopProcess() {
		this.context.stopAllocationImportLoggerProcess(getExecutorName());
	}
}
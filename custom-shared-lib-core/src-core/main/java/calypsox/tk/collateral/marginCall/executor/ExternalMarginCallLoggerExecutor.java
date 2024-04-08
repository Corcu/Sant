package calypsox.tk.collateral.marginCall.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.core.Util;

public class ExternalMarginCallLoggerExecutor extends
		ProcessExecutor<Task, Object, Task> {

	private List<Task> tasksToBeSavedBuffer = new ArrayList<Task>();
	private int tasksToSaveBufferSize = 100;
	protected ExecutionContext mcExecContext;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 */
	public ExternalMarginCallLoggerExecutor(BlockingQueue<Task> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,
			List<ProcessExecutorLauncher> producersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context,
				producersLauncher);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang
	 * .Object)
	 */
	@Override
	public Object execute(Task task) throws Exception {
		if (task == null) {
			return null;
		}

		tasksToBeSavedBuffer.add(task);
		if (this.tasksToBeSavedBuffer.size() >= this.tasksToSaveBufferSize) {
			this.tasksToBeSavedBuffer.clear();

		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutor#finishPendingWork()
	 */
	@Override
	public void finishPendingWork() {
		if (!Util.isEmpty(tasksToBeSavedBuffer)) {
			tasksToBeSavedBuffer.clear();
		}
	}

}

package calypsox.tk.collateral.allocation.optimizer.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.importer.ProcessExecutor;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;

/**
 * Read
 * 
 * @author aela
 * 
 */
public class OptimAllocsPersistor extends
		ProcessExecutor<MarginCallEntry, Object, Task> {

	@SuppressWarnings("unused")
	private List<MarginCallEntry> persistenceBuffer = null;

	protected OptimAllocsLoggerHelper loggingHelper = new OptimAllocsLoggerHelper();

	// private RemoteSantCollateralService remoteSantColService = null;
	private int tradeToSaveBufferSize = 5;

	protected OptimAllocsImportContext context;

	protected ExecutionContext allocExecContext;

	public OptimAllocsPersistor(BlockingQueue<MarginCallEntry> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, OptimAllocsImportContext context) {
		super(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.context = context;
		this.allocExecContext = context
				.getExecutionContextWithUserAction("Allocate");
		this.persistenceBuffer = new ArrayList<MarginCallEntry>(
				this.tradeToSaveBufferSize);
		// this.remoteSantColService = (RemoteSantCollateralService)
		// DSConnection.getDefault().getRMIService(
		// "baseSantCollateralService", RemoteSantCollateralService.class);

	}

	public OptimAllocsPersistor(BlockingQueue<MarginCallEntry> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> loggingQueue, OptimAllocsImportContext context,
			int persistorBufferSize) {
		this(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.tradeToSaveBufferSize = persistorBufferSize;
	}

	@SuppressWarnings("static-access")
	@Override
	public Object execute(MarginCallEntry entry) throws Exception {
		
		if (entry == null) {
			return null;
		}
		List<String> errors = new ArrayList<String>();
		calculateAndSaveEntry(entry, this.allocExecContext, errors);
		addLogs(this.loggingHelper.getLogsAsTasks(entry,
				OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
				this.context.getExecutionId(), errors));

		if (Util.isEmpty(errors)) {
			// complete the allocation import tasks previously created
			// DSConnection.getDefault().getRemoteBackOffice().applyTasksAutomatically(arg0)
		}
		
		return null;
	}

	@Override
	public void finishPendingWork() {

	}

	@Override
	protected HashSet<String> getProcessCounter() {
		return this.context.getAllocationPersistoProcessor();
	}

	/**
	 * @return the hasToContinue
	 */
	@Override
	public boolean getHasToContinue() {
		return this.context.isAllocationMapperRunning()
				|| (getInQueueSize() > 0);
	}

	/**
	 * Stops this process
	 */
	@Override
	protected void stopProcess() {
		this.context.stopAllocationPersistorProcess(getExecutorName());
	}

	/**
	 * Calculate and save the given margin call entries
	 * 
	 * @param entries
	 * @param execContext
	 * @param errors
	 */
	public static void calculateAndSaveEntry(final MarginCallEntry entry,
			final ExecutionContext execContext, final List<String> errors) {

		if (entry == null) {
			return;
		}

		List<MarginCallEntry> entries = new ArrayList<MarginCallEntry>();
		entries.add(entry);
		//CollateralManagerUtil.calculateEntries(entries, execContext, errors);
		//if (!Util.isEmpty(errors)) {
		//	return;
		//}
		CollateralManagerUtil.saveEntries(entries, "ALLOCATE", errors);
	}
}
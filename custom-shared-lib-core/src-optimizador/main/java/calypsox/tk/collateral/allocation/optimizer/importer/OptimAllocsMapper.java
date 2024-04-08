package calypsox.tk.collateral.allocation.optimizer.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.tk.collateral.allocation.optimizer.importer.mapper.OptimizerAllocationMapper;
import calypsox.tk.util.SantCollateralOptimConstants;
import calypsox.tk.util.optimizer.OptimizerStatusUtil;
import calypsox.util.importer.ProcessContext;
import calypsox.util.importer.ProcessExecutor;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Log;

/**
 * Save trades with their MTM information (call to the DataServer)
 * 
 * @author aela
 * 
 */
public class OptimAllocsMapper extends
		ProcessExecutor<OptimContractAllocsBean, MarginCallEntry, Task> {

	private OptimizerAllocationMapper allocationMapper = null;

	protected OptimAllocsLoggerHelper loggingHelper;

	protected OptimAllocsImportContext context;

	/**
	 * Constructor
	 * 
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 */
	public OptimAllocsMapper(
			BlockingQueue<OptimContractAllocsBean> inWorkQueue,
			BlockingQueue<MarginCallEntry> outWorkQueue,
			BlockingQueue<Task> loggingQueue, ProcessContext context) {
		super(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.allocationMapper = new OptimizerAllocationMapper(
				(OptimAllocsImportContext) context);
		loggingHelper = new OptimAllocsLoggerHelper();
		this.context = (OptimAllocsImportContext) context;
	}

	/**
	 * Returns the Trade & PL Marks of the trade from the tradeBean
	 */
	@SuppressWarnings("static-access")
	@Override
	public MarginCallEntry execute(OptimContractAllocsBean allocBean)
			throws Exception {

		MarginCallEntry entry = null;

		if (allocBean == null) {
			return null;
		}

		try {
			List<String> messages = new ArrayList<String>();
			List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
			// check that the allocation bean are valid to create margin call
			// allocations
			this.allocationMapper.isValidAllocation(allocBean, errors);
			if (!Util.isEmpty(errors)) {
				context.getInvalidItems().add(allocBean);
				for (AllocImportErrorBean error : errors) {
					messages.add(error.getValue());
				}
				logMessages(entry, messages);
				return null;
			}

			// first get the entry for the given bean
			entry = this.allocationMapper.mapEntry(allocBean, messages);
			if (!Util.isEmpty(messages)) {
				logMessages(entry, messages);
				return null;
			}
			if (entry == null || entry.getId() == 0) {
				messages.add("No margin call entry found for the contract  "
						+ allocBean.getContractName()
						+ " for the processing date "
						+ context.getExecutionContext().getProcessDate());
				addLogs(loggingHelper.getLogsAsTasks(entry,
						OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
						context.getExecutionId(), messages));
				return null;
			}
			// by default we suppose that the allocation import will be OK
			entry.addAttribute(OptimizerStatusUtil.OPTIMIZER_STATUS, OptimizerStatusUtil.OPTIMIZED);
			entry.addAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS,
					SantCollateralOptimConstants.OPTIMIZER_SENT_STATUS_VALUE);
			entry.addAttribute(SantCollateralOptimConstants.OPTIMIZER_LAST_TREATMENT_ID,
					context.getExecutionId());
			
			// map the allocations
			List<MarginCallAllocation> mappedAllocs = this.allocationMapper
					.mapListAllocation(allocBean.getAllocations(), errors);

			if (!Util.isEmpty(errors)) {
				for (AllocImportErrorBean error : errors) {
					messages.add(error.getValue());
				}
				logMessages(entry, messages);
				return null;
			}

			// get rid of the pending allocations ...
			// entry.cleanPendingAllocation();
			// add th new mapped allocations
			for (MarginCallAllocation allocation : mappedAllocs) {
				entry.addAllocation(allocation);
			}
		}
		catch (Exception e) { // unknown error
			Log.error(this, e);
			List<String> errors = new ArrayList<String>();
			errors.add("Unexpected error while handling allocations for the contract  "
					+ allocBean.getContractName());
			addLogs(loggingHelper.getLogsAsTasks(entry,
					OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
					context.getExecutionId(), errors));
		}
		return entry;
	}

	/**
	 * if the thread must continue
	 */
	@Override
	public boolean getHasToContinue() {
		return context.isFileReaderRunning() || getInQueueSize() > 0;
	}

	/**
	 * Stops de process
	 */
	@Override
	protected void stopProcess() {
		System.out.println(getExecutorName() + " is being stopped");
		this.context.stopAllocationMapperProcess(getExecutorName());
	}

	@SuppressWarnings("static-access")
	private void logMessages(MarginCallEntry entry, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(loggingHelper.getLogsAsTasks(entry,
					OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
					context.getExecutionId(), messages));
		}
		messages.clear();
	}

	@Override
	protected HashSet<String> getProcessCounter() {
		return context.getAllocationMapperProcessor();
	}

}
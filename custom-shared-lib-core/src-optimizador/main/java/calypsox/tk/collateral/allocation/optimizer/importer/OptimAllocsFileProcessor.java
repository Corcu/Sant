package calypsox.tk.collateral.allocation.optimizer.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.tk.util.SantCollateralOptimConstants;
import calypsox.tk.util.optimizer.OptimizerStatusUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.importer.ProcessExecutor;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Log;

/**
 * Gathers the trade beans to be sent to the TradeMapper thread.
 * 
 * @author aela
 * 
 */
public class OptimAllocsFileProcessor extends
		ProcessExecutor<Object, OptimContractAllocsBean, Task> {

	// class variables
	private final File file;

	private BlockingQueue<OptimContractAllocsBean> recodsList = null;

	private final OptimAllocsReader filerProcessor;

	private final Map<String, OptimContractAllocsBean> contrAllocsUnderConstruction = new HashMap<String, OptimContractAllocsBean>();

	protected OptimAllocsImportContext context;

	protected OptimAllocsLoggerHelper loggingHelper = new OptimAllocsLoggerHelper();

	/**
	 * Constructor
	 * 
	 * @param f file
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 */
	public OptimAllocsFileProcessor(File f, BlockingQueue<Object> inWorkQueue,
			BlockingQueue<OptimContractAllocsBean> outWorkQueue,
			BlockingQueue<Task> loggingQueue, OptimAllocsImportContext context) {
		super(inWorkQueue, outWorkQueue, loggingQueue, context);
		this.recodsList = outWorkQueue;
		this.file = f;
		this.filerProcessor = new OptimAllocsReader(context);
		this.context = context;

	}

	/**
	 * checks on trade bean data .
	 */
	@SuppressWarnings("static-access")
	@Override
	public OptimContractAllocsBean execute(Object item) throws Exception {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this.file));
			OptimContractAllocsBean ctrAllocs = null;
			OptimAllocationBean alloc = null;
			String record = null;
			if(context.isSkipHeader()){
				reader.readLine();
			}
			while ((record = reader.readLine()) != null) {
				try {
					// read a file row and map it to an ExternalAllocationBean
					// object
					alloc = this.filerProcessor.readLine(record);
					// depending on the allocations number per contract,
					// continue the process
					// with allocation or gather the allocations before handling
					// a contract
					if (alloc.getNbCtrAllocs() == 1) {
						ctrAllocs = new OptimContractAllocsBean(alloc);
					}
					else {
						ctrAllocs = this.contrAllocsUnderConstruction.get(alloc
								.getContractName());
						if (ctrAllocs == null) {
							ctrAllocs = new OptimContractAllocsBean(alloc);
							this.contrAllocsUnderConstruction.put(
									alloc.getContractName(), ctrAllocs);
							continue;
						}
						else {
							ctrAllocs.getAllocations().add(alloc);
							if (ctrAllocs.getAllocations().size() < alloc
									.getNbCtrAllocs()) {
								continue;
							}
						}
					}
					this.contrAllocsUnderConstruction.remove(alloc
							.getContractName());
					this.recodsList.put(ctrAllocs);
				} catch(ArrayIndexOutOfBoundsException e) {
					String errorMessage = "Unable to read the record "
							+ (Util.isEmpty(record) ? "empty"
									: record)+". Wrong number of fields ";
					addLog(loggingHelper
							.getLogAsTask(
									null,
									OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,context.getExecutionId(),null,
									errorMessage));
					context.getInvalidItems().add(new AllocImportErrorBean(OptimAllocsImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,errorMessage));
					Log.error(this, e);
					
				} catch (Exception e) {
					String errorMessage = "Unable to read the record "
							+ (Util.isEmpty(record) ? "empty"
									: record)+". Cause: "+e.getMessage();
					addLog(loggingHelper
							.getLogAsTask(
									null,
									OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,context.getExecutionId(),null,errorMessage));
					
					context.getInvalidItems().add(new AllocImportErrorBean(OptimAllocsImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,errorMessage));
					Log.error(this, e);
				}
			}
		}
		catch (Exception ex) {
			addLog(loggingHelper
			.getLogAsTask(
					null,
					OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,context.getExecutionId(),null,
					"Unable to read allocations file"+". Cause: "+ex.getMessage()));
			Log.error(this, ex);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {
					Log.error(
							this,
							"Error while trying close input stream for the file <"
									+ this.file.getName() + "> open previously",
							e);
				}
			}
			
			List<MarginCallEntryDTO> failedEntries = new ArrayList<MarginCallEntryDTO>();
			//if there is still incomplete some allocations, log them as errors
			if(contrAllocsUnderConstruction != null && contrAllocsUnderConstruction.size()>0){
				for(String ctrName : contrAllocsUnderConstruction.keySet()) {
					if (context != null && context.getContractsNameForId() != null) {
						Double contractId = context.getContractsNameForId().get(ctrName);
						int mccId = (contractId == null ? 0 : contractId
								.intValue());				
						List<String> errors = new ArrayList<String>();
						MarginCallEntry entry = CollateralManagerUtil
								.loadEntry(mccId, context.getExecutionContext(), errors);						
						if (entry != null && entry.getId() > 0) {
							// set the treatment id
							entry.addAttribute(SantCollateralOptimConstants.OPTIMIZER_LAST_TREATMENT_ID,
									context.getExecutionId());
							failedEntries.add(entry.toDTO());
						}
					}

					addLog(loggingHelper
							.getLogAsTask(
									null,
									OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,context.getExecutionId(),null,
									"Incomplete allocations for the contract "+ctrName+" expected  "+contrAllocsUnderConstruction.get(ctrName).getAllocations().get(0).getNbCtrAllocs()
											+", received "+contrAllocsUnderConstruction.get(ctrName).getAllocations().size()));
				}
				// update the status of the margin call
				// contracts as failed optimization
				if (!Util.isEmpty(failedEntries)) {
					OptimizerStatusUtil.updateOptimizerStatus(
							failedEntries,
							OptimizerStatusUtil.FAILED_OPTIMIZATION);
				}
			}
		}
		// send the signal to stop everything
		dispose();
		return null;
	}

	@Override
	protected void stopProcess() {
		context.stopFileReaderProcess(getExecutorName());
	}

	@Override
	protected HashSet<String> getProcessCounter() {
		return context.getFileReaderProcessor();
	}

}
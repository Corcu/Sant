/**
 * 
 */
package calypsox.tk.collateral.allocation.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.bean.ExternalContractAllocationsBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.mapper.AllocationMapperFactory;
import calypsox.tk.collateral.allocation.mapper.ExternalAllocationMapper;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.util.ExternalAllocationImportUtils;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;

/**
 * @author aela
 *
 */
public class ExternalAllocationMapperExecutor extends
		ProcessExecutor<ExternalAllocationBean, MarginCallEntry, Task> {
	
	protected ExternalAllocationMapper mapper;
	protected ExternalAllocationImportContext importContext;
	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 */
	public ExternalAllocationMapperExecutor(
			BlockingQueue<ExternalAllocationBean> inWorkQueue,
			BlockingQueue<MarginCallEntry> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,List<ProcessExecutorLauncher> prodcuersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context, prodcuersLauncher);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang.Object)
	 */
	@Override
	public MarginCallEntry execute(ExternalAllocationBean allocBean)
			throws Exception {
		
		if(allocBean == null) {
			return null;
		}
		
		 importContext = (ExternalAllocationImportContext)context;
		//MarginCallEntry entry = allocBean.getEntry();

		List<AllocImportErrorBean> messages = new ArrayList<AllocImportErrorBean>();
		List<MarginCallAllocation> mappedAllocations = new ArrayList<MarginCallAllocation>();
	
		if (allocBean instanceof ExternalContractAllocationsBean) {
			ExternalContractAllocationsBean contractAllocsBean = (ExternalContractAllocationsBean)allocBean;
			if(!Util.isEmpty(contractAllocsBean.getEntryAllocations())) {
				
				for(ExternalAllocationBean allocationBean : contractAllocsBean.getEntryAllocations()) {
					// get the right mapper from the passed allocation bean (Cash or security)
					 mapper = AllocationMapperFactory.getInstance(importContext).getAllocationMapper(allocBean);		
						// execute the mapping
					 mappedAllocations.add(mapper.mapAllocation(allocationBean,messages ));
				}
				
			}

		}
		else {
			// get the right mapper from the passed allocation bean (Cash or security)
			mapper = AllocationMapperFactory.getInstance(importContext).getAllocationMapper(allocBean);		
			// execute the mapping			
			mappedAllocations.add(mapper.mapAllocation(allocBean,messages ));
		}

		// add the generated logs to the logging queue
		//logMessages(allocBean.getEntry(), messages);
		if (!Util.isEmpty(messages)) {
			// those errors should be tracked as tasks but also sent back to the sending system
			List<String> errors = new ArrayList<String>();
			//context.getInvalidItems().add(allocBean);
			for (AllocImportErrorBean error : messages) {
				errors.add(error.getValue());
			}
			importContext.getInvalidItems().addAll(messages);
			logMessages(allocBean.getEntry(), importContext, errors);
			return null;
		}
		List<String> mappedIds = new ArrayList<String>();
		if(!Util.isEmpty(mappedAllocations) && allocBean.getEntry() != null ) {
			for(MarginCallAllocation allocation:  mappedAllocations) {
				if(allocation == null)
					continue;
				String frontID = (String)allocation.getAttribute(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD);
				if(!Util.isEmpty(frontID)) {
					mappedIds.add(frontID);
				}
				allocBean.getEntry().addAllocation(allocation);			
			}
		}
		
		// 
		importContext.getValidItems().addAll(mappedIds);
		
		return allocBean.getEntry();
	}
	
	/**
	 * @return the mapper
	 */
	public ExternalAllocationMapper getMapper() {
		return mapper;
	}

	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(ExternalAllocationMapper mapper) {
		this.mapper = mapper;
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

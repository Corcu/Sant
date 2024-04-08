/**
 * 
 */
package calypsox.tk.collateral.marginCall.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.mapper.ExternalMarginCallMapper;
import calypsox.tk.collateral.marginCall.mapper.MarginCallMapper;
import calypsox.tk.collateral.util.ExternalMarginCallImportUtils;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;

/**
 * @author aela
 *
 */
public class ExternalMarginCallMapperExecutor extends
		ProcessExecutor<ExternalMarginCallBean, Trade, Task> {

	protected ExternalMarginCallMapper mapper;
	protected ExternalMarginCallImportContext importContext;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 */
	public ExternalMarginCallMapperExecutor(
			BlockingQueue<ExternalMarginCallBean> inWorkQueue,
			BlockingQueue<Trade> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,
			List<ProcessExecutorLauncher> prodcuersLauncher) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context,
				prodcuersLauncher);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang
	 * .Object)
	 */
	@Override
	public Trade execute(ExternalMarginCallBean mcBean) throws Exception {

		if (mcBean == null) {
			return null;
		}

		importContext = (ExternalMarginCallImportContext) context;

		List<MarginCallImportErrorBean> messages = new ArrayList<MarginCallImportErrorBean>();

		mapper = new MarginCallMapper(importContext);
		Trade trade = mapper.mapMarginCallTrade(mcBean, messages);

		// add the generated logs to the logging queue
		if (!Util.isEmpty(messages)) {
			// those errors should be tracked as tasks but also sent back to
			// the sending system
			List<String> errors = new ArrayList<String>();
			for (MarginCallImportErrorBean error : messages) {
				errors.add(error.getValue());
			}
			importContext.getInvalidItems().addAll(messages);
			logMessages(mcBean.getMarginCall(), importContext, errors);
			return null;
		}

		// // Insert the ..... into valid items.
		// List<String> mappedIds = new ArrayList<String>();
		// importContext.getValidItems().addAll(mappedIds);

		return trade;
	}

	/**
	 * @return the mapper
	 */
	public ExternalMarginCallMapper getMapper() {
		return mapper;
	}

	/**
	 * @param mapper
	 *            the mapper to set
	 */
	public void setMapper(ExternalMarginCallMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * @param mc
	 * @param messages
	 */
	private void logMessages(MarginCall mc,
			ExternalMarginCallImportContext importContext, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(ExternalMarginCallImportUtils.messagesToTasks(mc,
					importContext, messages));
		}
		messages.clear();
	}
}

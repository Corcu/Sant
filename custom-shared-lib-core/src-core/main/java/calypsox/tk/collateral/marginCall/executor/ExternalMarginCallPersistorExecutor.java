package calypsox.tk.collateral.marginCall.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.persistor.AbstractExternalMarginCallPersistor;
import calypsox.tk.collateral.marginCall.persistor.ExternalMarginCallPersistor;
import calypsox.tk.collateral.util.ExternalMarginCallImportUtils;
import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.collateral.util.processor.ProcessExecutor;
import calypsox.tk.collateral.util.processor.ProcessExecutorLauncher;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;

public class ExternalMarginCallPersistorExecutor extends
		ProcessExecutor<Trade, Object, Task> {

	protected ExternalMarginCallPersistor persistor;

	protected ExternalMarginCallImportContext context;

	private List<Trade> persistenceBuffer = null;
	private int marginCallToSaveBufferSize = 1;

	/**
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param logWorkQueue
	 * @param context
	 * @param producersLauncher
	 */
	public ExternalMarginCallPersistorExecutor(
			BlockingQueue<Trade> inWorkQueue,
			BlockingQueue<Object> outWorkQueue,
			BlockingQueue<Task> logWorkQueue, ProcessContext context,
			List<ProcessExecutorLauncher> producersLauncher,
			final int mcToSaveBufferSize) {
		super(inWorkQueue, outWorkQueue, logWorkQueue, context,
				producersLauncher);
		this.context = (ExternalMarginCallImportContext) context;
		this.persistor = new AbstractExternalMarginCallPersistor(
				(ExternalMarginCallImportContext) context);
		this.marginCallToSaveBufferSize = mcToSaveBufferSize;
		this.persistenceBuffer = new ArrayList<Trade>(
				this.marginCallToSaveBufferSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.util.processor.ProcessExecutor#execute(java.lang
	 * .Object)
	 */
	@Override
	public Object execute(Trade item) throws Exception {

		if (item == null) {
			return null;
		}

		List<String> messages = new ArrayList<String>();

		this.persistenceBuffer.add(item);
		// save the trade
		if (this.persistenceBuffer.size() >= this.marginCallToSaveBufferSize) {
			this.persistor.persistEntry(this.persistenceBuffer, messages);
			this.persistenceBuffer.clear();
		}

		// add the generated logs to the logging queue
		if (!Util.isEmpty(messages)) {
			logMessages((MarginCall) item.getProduct(), messages);
		}

		return null;

	}

	/**
	 * @return the persistor
	 */
	public ExternalMarginCallPersistor getPersistor() {
		return this.persistor;
	}

	/**
	 * @param persistor
	 *            the persistor to set
	 */
	public void setPersistor(ExternalMarginCallPersistor persistor) {
		this.persistor = persistor;
	}

	/**
	 * @param entry
	 * @param messages
	 */
	private void logMessages(MarginCall mc, List<String> messages) {
		if (!Util.isEmpty(messages)) {
			addLogs(ExternalMarginCallImportUtils.messagesToTasks(mc,
					this.context, messages));
		}
		messages.clear();
	}

}

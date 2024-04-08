/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.marginCall.importer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import calypsox.tk.collateral.util.processor.ProcessContext;
import calypsox.tk.optimization.service.RemoteSantOptimizationService;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 *
 */
public class ExternalMarginCallImportContext implements ProcessContext {

	protected boolean skipHeader;
	protected String rowSpliter;
	protected String importFolder;
	protected PricingEnv pricingEnv;
	private List<Object> invalidItems = new ArrayList<Object>();
	private List<Object> validItems = new ArrayList<Object>();
	private List<Task> tasksToPublish = new ArrayList<Task>();

	public List<Task> getTasksToPublish() {
		return tasksToPublish;
	}

	public void setTasksToPublish(List<Task> tasksToPublish) {
		this.tasksToPublish = tasksToPublish;
	}

	protected Map<String, Double> contractsNameForId;

	protected ExecutionContext executionContext;
	private Long executionId;

	public ExternalMarginCallImportContext(String separator, boolean skipHeader) {
		this.rowSpliter = separator;
		this.contractsNameForId = new HashMap<String, Double>();
		this.executionId = 0L;
		this.skipHeader = skipHeader;
	}

	/**
	 * @return the rowSpliter
	 */
	public String getRowSpliter() {
		return this.rowSpliter;
	}

	/**
	 * @return true if the header of the imported file should be skipped
	 */
	public boolean isSkipHeader() {
		return this.skipHeader;
	}

	/**
	 * @param processingDate
	 * @param valuatioDate
	 * @param pricingEnvName
	 * @throws Exception
	 */
	public void init(JDatetime processingDate, JDatetime valuatioDate,
			String pricingEnvName) throws Exception {
		// init the pricing env
		pricingEnv = PricingEnv.loadPE(pricingEnvName, processingDate);
		// init the colalteral management execution context
		executionContext = initExecutionContext(processingDate, valuatioDate);

		RemoteSantOptimizationService remoteSantOptimService = (RemoteSantOptimizationService) DSConnection
				.getDefault().getRMIService("baseSantOptimizationService",
						RemoteSantOptimizationService.class);
		contractsNameForId = remoteSantOptimService.getAllContractNamesForIds();
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		long execId = Long.parseLong(sdf.format(executionContext
				.getProcessDate().getDate()) + "00"); // TODO he tenido que
														// añadir el getDAte
		executionId = calculateExecutionId(remoteSantOptimService
				.getOptimImportExecutionID(execId));
		if (validItems != null) {
			validItems.clear();
		}
	}

	/**
	 * @param executionId
	 * @return
	 */
	private Long calculateExecutionId(Long executionId) {
		if (executionId == 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
			return Long.parseLong(sdf.format(executionContext.getProcessDate()
					.getDate()) + "00"); // TODO he tenido que añadir el getDate
		} else {
			return executionId + 1;
		}
	}

	/**
	 * Initialize the execution context to use with the collateral manager
	 * utility
	 * 
	 * @param processingDate
	 * @param valuatioDate
	 * @return
	 */
	private ExecutionContext initExecutionContext(
			final JDatetime processingDate, final JDatetime valuatioDate) {

		final ExecutionContext ec = CollateralManagerUtil
				.getDefaultExecutionContext();
		ec.setProcessDate(processingDate.getJDate(TimeZone.getDefault()));
		// set the default user action to use
		ec.setUserAction("Price");
		return ec;
	}

	/**
	 * @param userAction
	 * @return
	 */
	public ExecutionContext getExecutionContextWithUserAction(String userAction) {

		if (executionContext == null) {
			return null;
		}
		final ExecutionContext clonedExecCtx = executionContext.clone();
		clonedExecCtx.setUserAction(userAction);
		return clonedExecCtx;
	}

	/**
	 * @return the pricingEnv
	 */
	public PricingEnv getPricingEnv() {
		return pricingEnv;
	}

	/**
	 * @return the contractsNameForId
	 */
	public Map<String, Double> getContractsNameForId() {
		return contractsNameForId;
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	/**
	 * @return the executionId
	 */
	public Long getExecutionId() {
		return executionId;
	}

	/**
	 * @return the importFolder
	 */
	public String getImportFolder() {
		return importFolder;
	}

	/**
	 * @param importFolder
	 *            the importFolder to set
	 */
	public void setImportFolder(String importFolder) {
		this.importFolder = importFolder;
	}

	/**
	 * @return the invalidItems
	 */
	public List<Object> getInvalidItems() {
		if (Util.isEmpty(invalidItems)) {
			invalidItems = new ArrayList<Object>();
		}
		return invalidItems;
	}

	/**
	 * @param invalidItems
	 *            the invalidItems to set
	 */
	public void setInvalidItems(List<Object> invalidItems) {
		this.invalidItems = invalidItems;
	}

	/**
	 * @param skipHeader
	 *            the skipHeader to set
	 */
	public void setSkipHeader(boolean skipHeader) {
		this.skipHeader = skipHeader;
	}

	/**
	 * @param rowSpliter
	 *            the rowSpliter to set
	 */
	public void setRowSpliter(String rowSpliter) {
		this.rowSpliter = rowSpliter;
	}

	/**
	 * @param pricingEnv
	 *            the pricingEnv to set
	 */
	public void setPricingEnv(PricingEnv pricingEnv) {
		this.pricingEnv = pricingEnv;
	}

	/**
	 * @param executionContext
	 *            the executionContext to set
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * @param executionId
	 *            the executionId to set
	 */
	public void setExecutionId(Long executionId) {
		this.executionId = executionId;
	}

	/**
	 * @return the validItems
	 */
	public List<Object> getValidItems() {
		if (Util.isEmpty(validItems)) {
			validItems = new ArrayList<Object>();
		}
		return validItems;
	}

	/**
	 * @param validItems
	 *            the validItems to set
	 */
	public void setValidItems(List<Object> validItems) {
		this.validItems = validItems;
	}
}

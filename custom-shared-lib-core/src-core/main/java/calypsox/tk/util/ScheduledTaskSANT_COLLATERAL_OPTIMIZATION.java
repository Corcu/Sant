package calypsox.tk.util;

import com.calypso.tk.util.ScheduledTaskREPORT;

/**
 * ScheduledTask to optimize contracts.
 */
public class ScheduledTaskSANT_COLLATERAL_OPTIMIZATION extends ScheduledTaskREPORT {
//	static final long serialVersionUID = 123L;
//
//	public static final String ATTRIBUTE_REPORT_TEMPLATE_NAME = "Template";
//	public static final String ATTRIBUTE_OPTIMIZATION = "Optimization";
//	public static final String ATTRIBUTE_CONCENTRATION = "Concentration";
//	public static final String ATTRIBUTE_RELOAD_POSITIONS = "Reload Positions";
//	public static final String ATTRIBUTE_USE_CONTRACT_FREQUENCY = "Use Frequency (Add. Field)";
//
//	public static final String ATTRIBUTE_TOTAL_THREAD_POOL_SIZE = "Total Thread Pool Size";
//	public static final String ATTRIBUTE_MAX_DS_THREAD_NUMBER = "Max Threads querying DS";
//	public static final String ATTRIBUTE_DISPATCHER_NAME = "Dispatcher config name";
//	public static final String ATTRIBUTE_USE_GRID_CALCULATOR = "Use grid calculator";
//
//	public static final String ATTRIBUTE_DRY_RUN = "Dry run (not saving)";
//
//	public static final String OPTIMIZATION_RESULT_LOG = "optimization Result Log";
//	public static final String DOMAIN_OPT_ERRORS_TOLOG = "OptimizationBatch_ErrorsToLog";
//
//	List<String> resultLogMsgs = new ArrayList<String>();
//
//	static final public String REPORT_TYPE = "MarginCall";
//
//	public ScheduledTaskSANT_COLLATERAL_OPTIMIZATION() {
//		super();
//	}
//
//	@Override
//	public boolean process(DSConnection ds, PSConnection ps) {
//
//		if (Log.isCategoryLogged(Log.OLD_TRACE)) {
//			Log.debug(Log.OLD_TRACE, "ScheduledTask COLLATERAL_MANAGEMENT");
//		}
//		boolean ret = true;
//		try {
//			Task task = new Task();
//			task.setObjectId(getId());
//			task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
//			task.setNewDatetime(getValuationDatetime(false));
//			task.setUnderProcessingDatetime(getDatetime());
//			task.setUndoTradeDatetime(getUndoDatetime());
//			task.setDatetime(getDatetime());
//			task.setPriority(Task.PRIORITY_NORMAL);
//			task.setId(0);
//			task.setSource(getType());
//			StringBuffer sb = new StringBuffer("ScheduledTask " + getId() + ": ");
//			TaskArray tasks = new TaskArray();
//			boolean executed = processReport(sb);
//			if (!executed) {
//				task.setComment(sb.toString());
//				task.setEventType("EX_" + BOException.EXCEPTION);
//				task.setPriority(Task.PRIORITY_HIGH);
//			} else {
//				task.setComment(sb.toString() + " Successfully processed");
//				task.setEventType("EX_" + BOException.INFORMATION);
//			}
//			task.setCompletedDatetime(new JDatetime());
//			task.setStatus(Task.NEW);
//
//			TaskArray v = new TaskArray();
//			if (!executed) {
//				v = tasks;
//			}
//			v.add(task);
//			try {
//				getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
//			} catch (Exception e) {
//				Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			}
//		} finally {
//			// Separated because was sending email even if the report is
//			// not created.
//			if (getPublishB()) {
//				try {
//					PSEventScheduledTask ev = new PSEventScheduledTask();
//					ev.setScheduledTask(this);
//					ps.publish(ev);
//					ret = true;
//				} catch (Exception e) {
//					Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//					ret = false;
//				}
//			}
//			if (getSendEmailB() && ret) {
//				sendMail(ds, ps);
//			}
//		}
//		return ret;
//	}
//
//	public boolean processReport(StringBuffer sb) {
//		String msg = "";
//		boolean result = true;
//
//		try {
//
//			List<String> errors = new ArrayList<String>();
//
//			MarginCallReportTemplate template = instanciateReportTemplate(getTemplateName(), REPORT_TYPE, errors);
//			msg = "TemplateName=" + getTemplateName() + "; Process DateTime=" + getMCProcessDatetime()
//					+ "; Val DateTime=" + getMCValuationDatetime();
//			this.resultLogMsgs.add(msg);
//			this.resultLogMsgs.add("Optimizer=" + getAttribute(ATTRIBUTE_OPTIMIZATION));
//
//			ExecutionContext context = ExecutionContext.getInstance((CollateralContext) ServiceRegistry
//					.getDefaultContext().clone(), ServiceRegistry.getDefaultExposureContext(), template);
//
//			initContext(context);
//
//			CollateralManager manager = new CollateralManager(context);
//
//			List<String> messages = new ArrayList<String>();
//
//			List<MarginCallEntry> entries = manager.loadEntries(messages);
//			List<MarginCallEntry> pricedEntries = new ArrayList<MarginCallEntry>();
//
//			msg = "No. of entries loaded=" + entries.size();
//			Log.info(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, msg);
//			this.resultLogMsgs.add(msg);
//
//			if (!Util.isEmpty(entries)) {
//				for (MarginCallEntry entry : entries) {
//					Log.warn(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, "Status=" + entry.getStatus()
//							+ "; Pricing Status=" + entry.getPricingStatus());
//					if (entry.getPricingStatus().equals("PRICED")) {
//						pricedEntries.add(entry);
//					} else {
//						msg = "Entry with id=" + entry.getId() + "; Contract id=" + entry.getCollateralConfigId()
//								+ "; Contract Name=" + entry.getCollateralConfig().getName()
//								+ " is not PRICED so excluding it from Optimization";
//						Log.info(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, msg);
//						this.resultLogMsgs.add(msg);
//					}
//				}
//			}
//
//			msg = "No. of entries in PRICED Status=" + entries.size();
//			Log.info(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, msg);
//			this.resultLogMsgs.add(msg);
//
//			List<MarginCallEntry> finalEntries = new ArrayList<MarginCallEntry>();
//
//			for (MarginCallEntry entry : pricedEntries) {
//				if (entry.getCollateralConfig().isExcludeFromOptimizer()) {
//					msg = "Entry with id=" + entry.getId() + "; Contract id=" + entry.getCollateralConfigId()
//							+ "; Contract Name=" + entry.getCollateralConfig().getName()
//							+ " is setup to exclude from Optimizer";
//					Log.info(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, msg);
//					this.resultLogMsgs.add(msg);
//					continue;
//				}
//
//				if (useContractFrequency()) {
//					String frequency = entry.getCollateralConfig().getAdditionalField("FREQUENCY");
//					if (!acceptDateRule(frequency, getMCProcessDatetime().getJDate(TimeZone.getDefault()))) {
//						msg = "Entry with id=" + entry.getId() + "; Contract id=" + entry.getCollateralConfigId()
//								+ "; Contract Name=" + entry.getCollateralConfig().getName()
//								+ " is excluded by the Frequency DateRule";
//						Log.info(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, msg);
//						this.resultLogMsgs.add(msg);
//						continue;
//					}
//				}
//				finalEntries.add(entry);
//			}
//
//			// Optimize
//			if (isOptimize()) {
//				OptimizationConfiguration config = getOptimizationConfiguration();
//
//				if ((config != null) && !Util.isEmpty(finalEntries)) {
//					// manager.optimize(config, filteredEntries, errors);
//					optimize(context, config, finalEntries, errors);
//
//					// Calculate each entry and apply the user defined action
//					List<CollateralCommand> commands = new ArrayList<CollateralCommand>();
//					for (MarginCallEntry entry : finalEntries) {
//						commands.add(new SantAllocateActionCommand(context, entry));
//					}
//
//					CollateralExecutor allocExecutor = CollateralExecutorFactory.getInstance(context).createExecutor(
//							commands);
//					allocExecutor.execute();
//
//				}
//			}
//
//			if (!Util.isEmpty(errors)) {
//				for (int i = 0; i < errors.size(); i++) {
//					sb.append(errors.get(i));
//					Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, errors.get(i));
//					this.resultLogMsgs.add(errors.get(i));
//				}
//				result = false;
//			}
//			createResultLog(this.resultLogMsgs);
//		} catch (Exception e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			result = false;
//		}
//
//		return result;
//	}
//
//	@SuppressWarnings({ "unchecked" })
//	private void scanLogFileForErrors() {
//		SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS", Locale.UK);
//
//		Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, "Scanned entries from the log file - Start");
//		Vector<String> errorPatternToLookFor = LocalCache.getDomainValues(getDSConnection(), DOMAIN_OPT_ERRORS_TOLOG);
//		String fileName = Log.getFileName();
//		if (Util.isEmpty(fileName)) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, "No MainEntry Log found. Can't scan for errors.");
//			return;
//		}
//		Vector<LoggingRecord> loggingRecords = Log.readContents(fileName, true, null);
//		Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, "Scanned entries from the log file - Start");
//		this.resultLogMsgs.add("Scanned entries from the log file - Start");
//
//		JDatetime stStartTime = getDatetime();
//
//		for (LoggingRecord logRecord : loggingRecords) {
//			if (isOptErrorToLog(errorPatternToLookFor, logRecord.getMessage())) {
//				JDatetime timeLogged = null;
//				try {
//					timeLogged = new JDatetime(df.parse(logRecord.getTime()));
//					Log.error("ScheduledTaskSANT_COLLATERAL_OPTIMIZATION",
//							"****Parsed Date successfully - logRecord.getTime()=" + logRecord.getTime()
//									+ "; After conversion timeLogged=" + timeLogged);
//
//				} catch (ParseException e) {
//					Log.error("ScheduledTaskSANT_COLLATERAL_OPTIMIZATION", "****Failed to parse - logRecord.getTime()="
//							+ logRecord.getTime(), e);
//				}
//
//				Log.error("ScheduledTaskSANT_COLLATERAL_OPTIMIZATION", "****Scheduled Task Start Time =" + stStartTime);
//
//				if ((timeLogged == null) || timeLogged.gte(stStartTime)) {
//					Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class,
//							logRecord.getTime() + " - " + logRecord.getMessage());
//					this.resultLogMsgs.add(logRecord.getTime() + " - " + logRecord.getMessage());
//				}
//			}
//
//		}
//		Log.error("ScheduledTaskSANT_COLLATERAL_OPTIMIZATION", "Scanned entries from the log file - End");
//		this.resultLogMsgs.add("Scanned entries from the log file - End");
//	}
//
//	private boolean isOptErrorToLog(Vector<String> errorPatternToLookFor, String line) {
//		for (String pattern : errorPatternToLookFor) {
//			if (line.contains(pattern)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean acceptDateRule(String dateRuleName, JDate processDate) throws Exception {
//		DateRule dateRule = DSConnection.getDefault().getRemoteReferenceData().getDateRule(dateRuleName);
//
//		if (dateRule == null) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, "Date Rule " + dateRuleName
//					+ " Doesn't exist. Contract will be excluded");
//			return false;
//		}
//
//		JDate nextDate = DateRule.nextDate(processDate.addDays(-1), dateRule);
//		if ((nextDate != null) && nextDate.equals(processDate)) {
//			// Matches the Date Rule
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	// in the ScheduledTask, we may want to have different settings than the GUI
//	// so some parameters can be overwritten in here
//	private void initContext(ExecutionContext context) {
//		context.setCalculateConcentration(isConcentration());
//
//		// overwrite context settings
//		context.getCollateralContext().setAttribute(CollateralContext.ATTRIBUTE_THREAD_POOL_SIZE, getThreadPoolSize());
//		context.getCollateralContext().addUsedAttribute(CollateralContext.ATTRIBUTE_THREAD_POOL_SIZE);
//
//		context.getCollateralContext()
//				.setAttribute(CollateralContext.ATTRIBUTE_MAX_DS_THREAD, getMaxDataServerThread());
//		context.getCollateralContext().addUsedAttribute(CollateralContext.ATTRIBUTE_MAX_DS_THREAD);
//
//		context.getCollateralContext().setAttribute(CollateralContext.ATTRIBUTE_DISPATCHER_NAME, getDispatcherName());
//		context.getCollateralContext().addUsedAttribute(CollateralContext.ATTRIBUTE_DISPATCHER_NAME);
//
//		context.getCollateralContext().setAttribute(CollateralContext.ATTRIBUTE_USE_GRID, useGridCalculator());
//		context.getCollateralContext().addUsedAttribute(CollateralContext.ATTRIBUTE_USE_GRID);
//
//		// remove context user actions in the scheduled task
//		context.setUserAction("");
//
//		context.getCollateralContext().addUsedAttribute(CollateralContext.ATTRIBUTE_PRICE_METHOD_AT_LOADING);
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public void optimize(ExecutionContext executionContext, OptimizationConfiguration optimizationConfiguration,
//			List<MarginCallEntry> entries, List<String> errors) {
//		// List optimizables = MarginCallUtil.filterOptimizable(entries, optimizationConfiguration);
//
//		if (optimizationConfiguration != null) {
//			OptimizationExecutionContext optimizationContext = new OptimizationExecutionContext(executionContext);
//			optimizationContext.setOptimizationConfiguration(optimizationConfiguration);
//			optimizationContext.setTargetConfiguration(optimizationConfiguration.getTarget());
//
//			OptimizerFactory factory = OptimizerFactory.getInstance(optimizationContext);
//
//			List<? extends AllocationOptimizer<? extends AllocationOptimization>> optimizers = factory
//					.createAllocationOptimizers(entries, executionContext.getValuationDatetime());
//
//			List optimizations = new ArrayList();
//
//			for (AllocationOptimizer optimizer : optimizers) {
//				ArrayList errorMsgs = new ArrayList();
//				optimizer.optimize(errorMsgs);
//				if (!Util.isEmpty(errorMsgs)) {
//					Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, errorMsgs.toString());
//					this.resultLogMsgs.add(errorMsgs.toString());
//					continue;
//				}
//				List allocations = optimizer.getAllocations();
//
//				if (!Util.isEmpty(allocations)) {
//					optimizations.addAll(allocations);
//				}
//			}
//
//			scanLogFileForErrors();
//
//			if (!Util.isEmpty(optimizations)) {
//				allocate(optimizations, entries, errors);
//			}
//		}
//	}
//
//	private void allocate(List<AllocationOptimization> optimizations, List<MarginCallEntry> entries,
//			List<String> messages) {
//		Hashtable<Integer, MarginCallEntry> entryHash = new Hashtable<Integer, MarginCallEntry>();
//		if (!Util.isEmpty(entries)) {
//			for (MarginCallEntry entry : entries) {
//				entryHash.put(Integer.valueOf(entry.getCollateralConfigId()), entry);
//			}
//		}
//
//		if (!Util.isEmpty(optimizations)) {
//			for (AllocationOptimization optimization : optimizations) {
//				allocate(optimization, entryHash, messages);
//			}
//		}
//	}
//
//	private void allocate(AllocationOptimization optimization, Hashtable<Integer, MarginCallEntry> entryHash,
//			List<String> messages) {
//		MarginCallEntry entry = entryHash.get(Integer.valueOf(optimization.getId()));
//		if (entry != null) {
//			entry.allocateCash(optimization.getCashAllocations(true));
//			entry.allocateSecurities(optimization.getSecurityAllocations(true));
//			entry.priceAllocations();
//		}
//	}
//
//	@Override
//	public Vector<String> getDomainAttributes() {
//		Vector<String> v = new Vector<String>();
//		v.addElement(ATTRIBUTE_REPORT_TEMPLATE_NAME);
//
//		v.addElement(ATTRIBUTE_OPTIMIZATION);
//		v.addElement(OPTIMIZATION_RESULT_LOG);
//		v.addElement(ATTRIBUTE_USE_CONTRACT_FREQUENCY);
//		v.addElement(ATTRIBUTE_CONCENTRATION);
//		v.addElement(ATTRIBUTE_RELOAD_POSITIONS);
//		v.addElement(ATTRIBUTE_TOTAL_THREAD_POOL_SIZE);
//		v.addElement(ATTRIBUTE_MAX_DS_THREAD_NUMBER);
//		v.addElement(ATTRIBUTE_DISPATCHER_NAME);
//		v.addElement(ATTRIBUTE_USE_GRID_CALCULATOR);
//
//		return v;
//	}
//
//	@SuppressWarnings("rawtypes")
//	@Override
//	public Vector<String> getAttributeDomain(String attribute, Hashtable currentAttr) {
//		Vector<String> result = null;
//
//		if (ATTRIBUTE_REPORT_TEMPLATE_NAME.equals(attribute)) {
//			Vector<ReportTemplateName> names = BOCache.getReportTemplateNames(DSConnection.getDefault(), REPORT_TYPE,
//					null);
//			if (!Util.isEmpty(names)) {
//				result = new Vector<String>();
//				for (int i = 0; i < names.size(); i++) {
//					ReportTemplateName template = names.elementAt(i);
//					result.add(template.getTemplateName());
//				}
//			}
//		} else if (ATTRIBUTE_OPTIMIZATION.equals(attribute)) {
//			result = new Vector<String>();
//			result.addAll(getOptimizationConfigs());
//			// } else if (ATTRIBUTE_CONCENTRATION.equals(attribute)) {
//			// result = new Vector<String>();
//			// result.add(Boolean.toString(true));
//			// result.add(Boolean.toString(false));
//		} else if (ATTRIBUTE_RELOAD_POSITIONS.equals(attribute) || (ATTRIBUTE_USE_CONTRACT_FREQUENCY.equals(attribute))
//				|| ATTRIBUTE_CONCENTRATION.equals(attribute)) {
//			result = new Vector<String>();
//			result.addElement(Boolean.toString(true));
//			result.addElement(Boolean.toString(false));
//			return result;
//		}
//
//		return result;
//	}
//
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Override
//	public boolean isValidInput(Vector messages) {
//
//		// boolean ret = super.isValidInput(messages);
//
//		boolean ret = true;
//
//		if (Util.isEmpty(getTemplateName())) {
//			messages.addElement("Must select Template");
//			ret = false;
//		}
//
//		if (Util.isEmpty(getAttribute(OPTIMIZATION_RESULT_LOG))) {
//			messages.addElement("Please specify Optimization Result log file");
//			ret = false;
//		}
//
//		if (getValDateOffset() < 0) {
//			messages.addElement("Val Date Offset must be positive");
//			ret = false;
//		}
//
//		return ret;
//	}
//
//	@Override
//	public String getTaskInformation() {
//		return "Scheduled Task for Margin Call Optimization.";
//	}
//
//	protected MarginCallReport instanciateReport(String templateName, List<String> messages) {
//		return (MarginCallReport) instanciateReport(templateName, REPORT_TYPE, messages);
//	}
//
//	/**
//	 * @param templateName
//	 * @param messages
//	 * @return
//	 */
//	protected Report instanciateReport(String templateName, String reportType, List<String> messages) {
//		Report result = null;
//
//		try {
//			String className = "tk.report." + reportType + "Report";
//			result = (Report) InstantiateUtil.getInstance(className, true);
//
//			result.setPricingEnv(instanciatePricingEnv());
//			result.setFilterSet(getTradeFilter());
//			result.setValuationDatetime(getMCValuationDatetime());
//			result.setUndoDatetime(getUndoDatetime());
//
//			result.setReportTemplate(instanciateReportTemplate(templateName, reportType, messages));
//
//		} catch (Exception e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			result = null;
//		}
//
//		if (result == null) {
//			messages.add("Invalid report type: " + reportType + "\n");
//		}
//
//		return result;
//	}
//
//	/**
//	 * @param templateName
//	 * @param messages
//	 * @return
//	 */
//	protected MarginCallReportTemplate instanciateReportTemplate(String templateName, String reportType,
//			List<String> messages) {
//
//		MarginCallReportTemplate result = null;
//
//		try {
//			result = (MarginCallReportTemplate) DSConnection.getDefault().getRemoteReferenceData()
//					.getReportTemplate(reportType, templateName);
//		} catch (Exception e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//		}
//
//		if (result != null) {
//			result.setHolidays(getHolidays());
//			//result.put(MarginCallReportTemplate.VAL_DATE, getMCValuationDatetime());
//			result.put(MarginCallReportTemplate.PROCESS_DATE, getMCProcessDatetime());
//
//			Boolean reloadPosition = isReloadPosition();
//			if (reloadPosition != null) {
//				//result.put(MarginCallReportTemplate.IS_RELOAD_POSITIONS, reloadPosition);
//			}
//
//			result.callBeforeLoad();
//		} else {
//			messages.add("Template " + templateName + " Not Found for " + reportType + " Report");
//		}
//
//		return result;
//	}
//
//	protected JDatetime getMCProcessDatetime() {
//		return getValuationDatetime(true);
//	}
//
//	protected JDatetime getMCValuationDatetime() {
//		JDatetime result = getMCProcessDatetime();
//
//		int offset = ServiceRegistry.getDefaultContext().getValueDateDays();
//
//		TimeZone tz = getTimeZone();
//		if (tz == null) {
//			tz = TimeZone.getDefault();
//		}
//		result = result.addBusiness(result, -offset, 0, 0, getHolidays(), tz);
//
//		return result;
//	}
//
//	// private void setOffsetFromContext() {
//	// int calculationOffSet = ServiceRegistry.getDefaultContext().getValueDateDays();
//	// if (calculationOffSet != 0) {
//	// setValDateOffset(calculationOffSet);
//	// }
//	// }
//
//	/**
//	 * @return
//	 */
//	private PricingEnv instanciatePricingEnv() {
//		return instanciatePricingEnv(getPricingEnv());
//	}
//
//	/**
//	 * instanciate a new PricingEnv based on ScheduledTask attributes
//	 * 
//	 * @param pricingEnv
//	 *            the pricingEnv name
//	 * @return a new PricingEnv
//	 */
//	private PricingEnv instanciatePricingEnv(String pricingEnv) {
//		PricingEnv result = null;
//		try {
//			result = DSConnection.getDefault().getRemoteMarketData()
//					.getPricingEnv(getPricingEnv(), getMCValuationDatetime());
//
//		} catch (Exception e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//		}
//		return result;
//	}
//
//	/**
//	 * @param mc
//	 * @param comment
//	 * @param tasks
//	 * @param valDatetime
//	 */
//	protected void addTask(MarginCallConfig mc, String comment, TaskArray tasks, JDatetime valDatetime) {
//		Task task = new Task();
//		task.setObjectId(mc.getId());
//		task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
//		task.setNewDatetime(valDatetime);
//		task.setDatetime(valDatetime);
//		task.setPriority(Task.PRIORITY_NORMAL);
//		task.setId(0);
//		task.setObjectId(mc.getId());
//		task.setStatus(Task.NEW);
//		task.setEventType("EX_" + BOException.MARGIN_CALL);
//		task.setComment(comment);
//		task.setSource(getType());
//		tasks.add(task);
//	}
//
//	protected String getThreadPoolSize() {
//		String result = "";
//
//		String attribute = getAttribute(ATTRIBUTE_TOTAL_THREAD_POOL_SIZE);
//		if (!Util.isEmpty(attribute)) {
//			try {
//				// just checking if the attribute is an integer
//				Integer.valueOf(attribute);
//				result = attribute;
//			} catch (Exception e) {
//				Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			}
//		}
//		return result;
//	}
//
//	public String getMaxDataServerThread() {
//		String result = "";
//
//		String attribute = getAttribute(ATTRIBUTE_MAX_DS_THREAD_NUMBER);
//		if (!Util.isEmpty(attribute)) {
//			try {
//				// just checking if the attribute is an integer
//				Integer.valueOf(attribute);
//				result = attribute;
//			} catch (Exception e) {
//				Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			}
//		}
//		return result;
//	}
//
//	public String useGridCalculator() {
//		String result = "";
//
//		String attribute = getAttribute(ATTRIBUTE_USE_GRID_CALCULATOR);
//		if (!Util.isEmpty(attribute)) {
//			try {
//				// just checking if the attribute is a boolean
//				Boolean.valueOf(attribute);
//				result = attribute;
//			} catch (Exception e) {
//				Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			}
//		}
//		return result;
//	}
//
//	public boolean useContractFrequency() {
//		Boolean result = Boolean.FALSE;
//		String attribute = getAttribute(ATTRIBUTE_USE_CONTRACT_FREQUENCY);
//		if (!Util.isEmpty(attribute)) {
//			result = Util.toBoolean(attribute);
//		}
//		return result;
//	}
//
//	public String getDispatcherName() {
//		String attribute = getAttribute(ATTRIBUTE_DISPATCHER_NAME);
//		return attribute;
//	}
//
//	protected Boolean isReloadPosition() {
//		Boolean result = Boolean.TRUE;
//
//		String attribute = getAttribute(ATTRIBUTE_RELOAD_POSITIONS);
//		if (!Util.isEmpty(attribute)) {
//			result = Util.toBoolean(attribute);
//		}
//
//		return result;
//	}
//
//	protected boolean isOptimize() {
//		String attribute = getAttribute(ATTRIBUTE_OPTIMIZATION);
//		return !Util.isEmpty(attribute);
//	}
//
//	protected boolean isConcentration() {
//		boolean result = true;
//
//		String attribute = getAttribute(ATTRIBUTE_CONCENTRATION);
//		if (!Util.isEmpty(attribute)) {
//			result = Util.toBoolean(attribute);
//		}
//
//		return result;
//	}
//
//	protected List<String> getOptimizationConfigs() {
//		List<String> result = new ArrayList<String>();
//		result.add("");
//		try {
//			List<OptimizationConfiguration> configList = getServiceRegistry().getCollateralDataServer()
//					.loadAllOptimizationConfiguration();
//			if (!Util.isEmpty(configList)) {
//				for (OptimizationConfiguration config : configList) {
//					result.add(config.getName());
//				}
//			}
//		} catch (Exception e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//		}
//		Collections.sort(result);
//		return result;
//	}
//
//	public OptimizationConfiguration getOptimizationConfiguration() {
//		OptimizationConfiguration result = null;
//		String name = getAttribute(ATTRIBUTE_OPTIMIZATION);
//		if (!Util.isEmpty(name)) {
//			try {
//				result = getServiceRegistry().getCollateralDataServer().loadOptimizationConfiguration(name);
//			} catch (Exception e) {
//				Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			}
//		}
//		return result;
//	}
//
//	/**
//	 * The name of the template to process
//	 * 
//	 * @return the template name
//	 */
//	protected String getTemplateName() {
//		return getAttribute(ATTRIBUTE_REPORT_TEMPLATE_NAME);
//	}
//
//	public CollateralServiceRegistry getServiceRegistry() {
//		return ServiceRegistry.getDefault();
//	}
//
//	private void createResultLog(List<String> resultLogMsgs) {
//		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
//		String fileName = getAttribute(OPTIMIZATION_RESULT_LOG);
//		String calculationDate = dateFormat.format(new Date());
//
//		fileName = fileName + "_" + getTemplateName() + "_" + calculationDate + ".log";
//
//		BufferedWriter bw = null;
//		try {
//			bw = new BufferedWriter(new FileWriter(fileName));
//
//			for (String msg : resultLogMsgs) {
//				bw.write(msg + "\n");
//			}
//		} catch (IOException e) {
//			Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class, e);
//			return;
//		} finally {
//			if (bw != null) {
//				try {
//					bw.flush();
//					bw.close();
//				} catch (IOException e) {
//				}
//
//			}
//
//		}
//
//	}

}

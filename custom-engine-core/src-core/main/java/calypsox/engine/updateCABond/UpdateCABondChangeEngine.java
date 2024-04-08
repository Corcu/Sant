package calypsox.engine.updateCABond;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.calypso.engine.Engine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondDefault;
import com.calypso.tk.product.BondInfo;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CAApplyInfo;
import com.calypso.tk.product.corporateaction.CAApplyInfoBuilder;
import com.calypso.tk.product.corporateaction.CAGenerationAction;
import com.calypso.tk.product.corporateaction.CAGenerationContext;
import com.calypso.tk.product.corporateaction.CAGenerationContextBuilder;
import com.calypso.tk.product.corporateaction.CAGenerationHandlerUtil;
import com.calypso.tk.product.corporateaction.CATradeSaverCancellableJob;
import com.calypso.tk.risk.util.AnalysisProgressUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.tk.event.PSEventProduct;

/**
 * The Class tk.
 */
public class UpdateCABondChangeEngine extends Engine {
	private transient CATradeSaverCancellableJob __caTradeSaver;
	private String otcFilter = "_CAs_Bonds";
	private String positionType = "ACTUAL";
	private String balanceType = "Balance";
	private boolean isOTCProcess = true;
	private boolean isPositionProcess = true;
	private String processDate = "Payment Date";
	private int fromDays = 15;
	private int toDays = 15;
	public UpdateCABondChangeEngine(final DSConnection dsCon, final String hostName, final int esPort) {
		super(dsCon, hostName, esPort);
		Log.debug(this.getClass().getName(), "Arranque UpdateCABondChangeEngine ");
	}

	protected void init(EngineContext engineContext) {
		super.init(engineContext);
		otcFilter = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "OTCFilter");
		balanceType = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "BalanceType");
		positionType = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "PositionType");
		String positionProcess = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange",
				"IsPositionProcess");
		String otcProcess = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "IsOTCProcess");
		processDate = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "ProcessDate");
		String strFromDays = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "ProductFromDays");
		String strToDays = LocalCache.getDomainValueComment(this.getDS(), "UpdateCABondChange", "ProductToDays");
		otcFilter = !Util.isEmpty(otcFilter) ? otcFilter : "_CAs_Bonds";
		positionType = !Util.isEmpty(positionType) ? positionType : "ACTUAL";
		balanceType = !Util.isEmpty(balanceType) ? balanceType : "Balance";
		isPositionProcess = (!Util.isEmpty(positionProcess) && positionProcess.trim().toLowerCase().equals("false"))
				? false
				: true;
		isOTCProcess = (!Util.isEmpty(otcProcess) && otcProcess.trim().toLowerCase().equals("false")) ? false : true;
		processDate = !Util.isEmpty(processDate) ? processDate : "Payment Date";
		fromDays = Integer.getInteger(strFromDays, fromDays);
		toDays = Integer.getInteger(strToDays, toDays);

	}
	@Override
	public void processDomainChange(PSEventDomainChange event) {
		super.processDomainChange(event);
		if (event instanceof PSEventDomainChange
				&& (((PSEventDomainChange) event).getType() == PSEventDomainChange.EXCHANGE_TRADED_PRODUCT
						|| ((PSEventDomainChange) event).getType() == PSEventDomainChange.BOND_INFO)) {
			if (((PSEventDomainChange) event).getObject() == null
					|| ((PSEventDomainChange) event).getObject() instanceof Bond
					|| ((PSEventDomainChange) event).getObject() instanceof BondInfo
					|| ((PSEventDomainChange) event).getObject() instanceof BondDefault) {

				if (((PSEventDomainChange) event).getObject() == null
						&& ((PSEventDomainChange) event).getValueId() >= 0) {
					try {
						Bond b = (Bond) getDS().getRemoteProduct()
								.getProduct(((PSEventDomainChange) event).getValueId());
						event.setObject(b);
					} catch (CalypsoServiceException e) {
						Log.error(this, e);
					}
				}
				putInPool(event);
			} else {
				this.processEvent(event.getLongId(), getEngineName());
			}
		} else {
			this.processEvent(event.getLongId(), getEngineName());
		}
	}
	@Override
	public boolean process(PSEvent event) {
		Bond b = null;
		if (event instanceof PSEventProduct) {
			Product product = ((PSEventProduct) event).getProduct();
			if (product != null && product instanceof Bond) {
				b = (Bond) product;
			}
		} else if (event instanceof PSEventDomainChange) {
			if (event instanceof PSEventDomainChange
					&& (((PSEventDomainChange) event).getType() == PSEventDomainChange.EXCHANGE_TRADED_PRODUCT
							|| ((PSEventDomainChange) event).getType() == PSEventDomainChange.BOND_INFO)) {
				if (((PSEventDomainChange) event).getObject() == null
						|| ((PSEventDomainChange) event).getObject() instanceof Bond
						|| ((PSEventDomainChange) event).getObject() instanceof BondInfo
						|| ((PSEventDomainChange) event).getObject() instanceof BondDefault) {

					if (((PSEventDomainChange) event).getObject() == null
							&& ((PSEventDomainChange) event).getValueId() >= 0) {
						try {
							b = (Bond) getDS().getRemoteProduct()
									.getProduct(((PSEventDomainChange) event).getValueId());
						} catch (CalypsoServiceException e) {
							Log.error(this, e);
						}
					} else if (((PSEventDomainChange) event).getObject() instanceof Bond) {
						b = (Bond) ((PSEventDomainChange) event).getObject();
					} else if (((PSEventDomainChange) event).getObject() instanceof BondInfo) {
						BondInfo bi = (BondInfo) ((PSEventDomainChange) event).getObject();
						try {
							b = (Bond) getDS().getRemoteProduct().getProduct(bi.getId());
						} catch (CalypsoServiceException e) {
							Log.error(this.getEngineName(), "Error getting Bond.", e);
						}
					} else if (((PSEventDomainChange) event).getObject() instanceof BondDefault) {
						BondDefault bd = (BondDefault) ((PSEventDomainChange) event).getObject();
						b = bd.makeBond();
					}
				}
			}
		}
		if (b != null) {
			TaskArray tasks = new TaskArray();
			String error = handleProcessGenerate(getDS(), tasks, b);
			String apply = handleProcessApply(getDS(), tasks, b);
			if (error != null && apply != null) {
				if (error != null) {
					Log.error(this, "Error generating CAs." + error);
				}
				if (apply != null) {
					Log.error(this, "Error applying CA trades." + apply);
				}
				return false;
			}
		}
		this.processEvent(event.getLongId(), getEngineName());
		return true;
	}

	protected String handleProcessGenerate(DSConnection ds, TaskArray tasks, Bond b) {
		String exec = null;

		CAGenerationContext context = caGenerationContextBuilder(ds, b).build();

		exec = taskGenerateCA(context, context.getUnderlyings());
		Collection<Task> exceptionTasks = context.getExceptionTasks();
		tasks.add(exceptionTasks.<Task>toArray(new Task[exceptionTasks.size()]), exceptionTasks.size());

		return exec;
	}

	protected String handleProcessApply(DSConnection ds, TaskArray tasks, Bond b) {
		String exec = null;

		CAGenerationContext caGenerationContext = caGenerationContextBuilder(ds, b).caIds(null).build();

		exec = taskApplyCA(caGenerationContext.getUnderlyings(), caGenerationContext, tasks);

		return exec;
	}

	private String taskGenerateCA(CAGenerationContext context, Set<Product> products) {
		Map<CA, CAGenerationAction> generatedCAs = null;
		Map<Product, Map<CAGenerationAction, List<CA>>> caToSave = null;
		try {
			generatedCAs = CAGenerationHandlerUtil.generateLoadAndMergeCA(context);
			caToSave = CAGenerationHandlerUtil.generationPhaseToSavingPhase(generatedCAs);
			if (!Util.isEmpty(caToSave)) {
				for (Product product : caToSave.keySet()) {
					boolean removeNew = false;
					Map<CAGenerationAction, List<CA>> map = caToSave.get(product);
					for (CAGenerationAction act : map.keySet()) {
						if (CAGenerationAction.NEW.equals(act)) {
							List<CA> lstCA = map.get(act);
							List<CA> lstCA2 = new ArrayList<CA>();

							for (CA pCA : lstCA) {
								JDate rDate = pCA.getRecordDate();
								if (JDate.getNow().gte(rDate)) {
									lstCA2.add(pCA);
								}
							}
							if (Util.isEmpty(lstCA2)) {
								removeNew = true;
							} else {
								lstCA.clear();
								lstCA.addAll(lstCA2);
							}
						}
					}
					if (removeNew) {
						map.remove(CAGenerationAction.NEW);
					}
					if (map.isEmpty()) {
						caToSave.remove(product);
					}
				}
				if (!Util.isEmpty(caToSave)) {
					context.setCAToSaveByUnderlying(caToSave);
					List<String> messages = CAGenerationHandlerUtil.saveAllCorporateAction(context);
					if (!Util.isEmpty(messages)) {
						for (String message : messages) {
							Task task = new Task(new BOException(0L, "CORPORATE_ACTION", message, "CORPORATE_ACTION"),
									null);
							context.addExceptionTask(task);

						}
					}
				}
			}
		} catch (Exception e) {
			String msg = "ScheduledTaskCORPORATE_ACTION : Error Generating CAs";
			AnalysisProgressUtil.logError(msg, e);
			Task task = new Task(new BOException(0L, "CORPORATE_ACTION", e.getMessage(), "CORPORATE_ACTION"), null);
			context.addExceptionTask(task);
			return msg;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private String taskApplyCA(Set<Product> products, CAGenerationContext info, TaskArray tasks) {
		Collection<CA> applicableCA = null;
		DSConnection ds = getReadOnlyConnection(getDS());
		PricingEnv env = info.getPricingEnv();
		String exec = null;

		TradeFilter underlyingFilter = getFilter(otcFilter);
		try {
			applicableCA = CAGenerationHandlerUtil.loadApplicableCAs(products, info);
			applicableCA = (applicableCA instanceof Set) ? applicableCA
					: ((applicableCA == null) ? Collections.EMPTY_SET : new LinkedHashSet<>(applicableCA));
			Log.system(this.getEngineName(), "Number of CA product to apply for this period : " + applicableCA.size());
		} catch (Exception e) {
			exec = "Error Loading CAs";
			Log.error(this.getEngineName(), exec, e);
			return exec;
		}
		if (Util.isEmpty(applicableCA)) {
			Log.system(this.getEngineName(), "No CA to apply.");
			return exec;

		}


		TradeFilter positionFilter = Util.isEmpty(otcFilter) ? underlyingFilter : BOCache.getTradeFilter(ds, otcFilter);
		if (positionFilter == null) {
			AnalysisProgressUtil.logProgressDetail("Could not load POSITION Trade Filter: " + otcFilter);
			positionFilter = underlyingFilter;
		}

		TradeFilter otcTradeFilter = underlyingFilter;

		try {
			CAApplyInfo caApplyInfo = CAApplyInfoBuilder.builder(applicableCA, env)
					.positionBasedTradeFilter(positionFilter).otcTradeFilter(otcTradeFilter).positionType(positionType)
					.isByBOPositionTradeDate(false).isApplyToPosition(isPositionProcess).isApplyToOTC(isOTCProcess)
					.isApplyToBasket(isOTCProcess).threadPoolSize(1).executionDate(JDate.getNow())
					.inventoryAggregationType(null).isApplyOnRepoedPLPosition(false).balanceType(balanceType)
					.isApplyOnMarginCall(true).isSaveInternalTrades(true).batchSize(0).isAggregationBySubAccount(false)
					.build();
			Log.system(this.getEngineName(), caApplyInfo.toString());
			TradeArray trades = caApplyInfo.parallelRun(ds, (PSConnection) null);
			if (!Util.isEmpty(trades)) {
				Iterator<Trade> it = trades.iterator();
				while (it.hasNext()) {
					Trade trade = it.next();
					if (trade.getLongId() <= 0 && Action.NEW.equals(trade.getAction())
							&& JDate.getNow().before(((CA) trade.getProduct()).getRecordDate())) {
						it.remove();
					}
				}
			}
			exec = saveTrades(getDS(), trades, caApplyInfo);

			Collection<Task> exceptionTasks = caApplyInfo.getExceptionTasks();
			tasks.add(exceptionTasks.<Task>toArray(new Task[exceptionTasks.size()]), exceptionTasks.size());

		} catch (Throwable t) {
			exec = "Error applying CA(s) " + t.getMessage();
			Log.error(this, exec, t);
		}
		return exec;
	}

	private String saveTrades(DSConnection ds, TradeArray trades, CAApplyInfo caApplyInfo) throws InterruptedException {
		List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

		this.__caTradeSaver = new CATradeSaverCancellableJob(ds, trades, errorMessages, false, caApplyInfo);
		this.__caTradeSaver.performSave();
		if (trades != null)
			for (int i = 0; i < trades.size(); i++) {
				Trade trade = trades.get(i);
				Log.system(this.getEngineName(), "Saved trade: " + trade.getLongId());
			}
		String exec = Util.isEmpty(errorMessages) ? null : "Error Processing";

		for (String errorMessage : errorMessages) {
			Log.error(this, errorMessage);
		}
		return exec;
	}

	protected DSConnection getReadOnlyConnection(DSConnection ds) {
		DSConnection rods = getReadOnlyDS("CORPORATE_ACTION", ds, false);
		if (rods == null) {
			Log.error(this.getEngineName(), "Could not get a READONLY DS connection");
		}
		return rods;
	}

	protected TradeFilter getFilter(String name) {
		if (Util.isEmpty(name))
			return null;

		TradeFilter tf = null;
		try {
			tf = BOCache.getTradeFilter(DSConnection.getDefault(), name);
			if (tf != null)
				tf = (TradeFilter) tf.clone();

		} catch (CloneNotSupportedException e) {
			Log.error(this, e);
		}
		if (tf == null) {
			Log.error(this.getEngineName(), "Could not find Trade Filter: " + name);
		}
		return tf;
	}

	protected CAGenerationContextBuilder caGenerationContextBuilder(DSConnection ds, Bond b) {
		PricingEnv env = null;
		try {
			env = getDS().getRemoteMarketData().getPricingEnv(this.getPricingEnvName());
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error loading PE.");
		}
		Set<Product> products = new HashSet<Product>();
		products.add(b);
		JDate startDate = JDate.getNow();
		JDate endDate = b.getMaturityDate();
		return CAGenerationContextBuilder.builder(env, products).processDate(processDate).startDate(startDate)
				.endDate(endDate).caModel(null).caSubType(null).staticDataFilter(null).threadPoolSize(1)
				.deactivateCA(true).defaultRoundingMethod(null).includeSwiftInformation(false);
	}

	public static DSConnection getReadOnlyDS(String function, DSConnection ds, boolean insideServer) {
		if (insideServer)
			return ds;

		Vector<String> v = LocalCache.getDomainValues(ds, "roDSFunction");

		if (v == null || !v.contains(function)) {
			Log.warn("ScheduledTask", "Function " + function + " not found in domain");
			return ds;
		}
		DSConnection readOnly = null;
		try {
			readOnly = ds.getReadOnlyConnection();
			if (readOnly == null) {
				Log.debug("ScheduledTask", "Can not Connect to ReadOnly DS");
			} else {
				Log.info("ScheduledTask", "Using ReadOnly Connection to Load Data for " + function);

			}
		} catch (Throwable ex) {
			Log.error("ScheduledTask", ex);
			readOnly = null;
		}
		if (readOnly == null)
			readOnly = ds;
		return readOnly;
	}

	/**
	 * @param id
	 * @param engineName
	 * @return
	 */
	private boolean processEvent(long id, String engineName) {
		try {
			Log.debug(this.getClass().getName(), "Metodo Process . processEvent");
			this.getDS().getRemoteTrade().eventProcessed(id, engineName);
		} catch (Exception e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error in Method processEvent saving the event ", e);

			return false;
		}
		return true;
	}
}
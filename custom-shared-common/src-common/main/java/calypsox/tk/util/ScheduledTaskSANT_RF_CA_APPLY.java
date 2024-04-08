package calypsox.tk.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CAApplyInfo;
import com.calypso.tk.product.corporateaction.CAApplyInfoBuilder;
import com.calypso.tk.product.corporateaction.CAElectionUtil;
import com.calypso.tk.product.corporateaction.CAGenerationContext;
import com.calypso.tk.product.corporateaction.CAGenerationHandlerUtil;
import com.calypso.tk.product.corporateaction.CATradeSaverCancellableJob;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.risk.AnalysisProgress;
import com.calypso.tk.risk.util.AnalysisProgressUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.BaseScheduledTaskCorporateAction;
import com.calypso.tk.util.IExecutionContext;
import com.calypso.tk.util.ParallelExecutionException;
import com.calypso.tk.util.ReflectUtil;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;


public class ScheduledTaskSANT_RF_CA_APPLY extends BaseScheduledTaskCorporateAction<TradeArray, Object> {


    public static final String BO_POSITION = "BO_POSITION";
    public static final String BO_POSITION_DATE = "BO_POSITION_DATE";
    public static final String BO_POSITION_AGGREGATION_TYPE = "BO_POSITION_AGGREGATION_TYPE";
    public static final String BO_POSITION_BALANCE_TYPE = "BO_POSITION_BALANCE_TYPE";
    public static final String PL_POSITION_REPOED = "PL_POSITION_REPOED";
    public static final String OTC_PROCESS = "OTC_PROCESS";
    public static final String POSITION_PROCESS = "POSITION_PROCESS";
    public static final String STRUCTURED_PROCESS = "STRUCTURED_PROCESS";
    public static final String OTC_TRADE_FILTER = "OTC Trade Filter";
    public static final String POSITION_TRADE_FILTER = "POSITION Trade Filter";
    public static final String FROM_EX_TO_RECORD_DATE = "From Ex To Record Date";
    public static final String FROM_RECORD_TO_VALUE_DATE = "From Record To Value Date";
    public static final String APPLY_TO_BASKET = "Apply to basket";
    public static final String CA_ID = "CAID";
    public static final String APPLY_TO_MARGIN_CALL = "Apply to MarginCall";
    public static final String SAVE_INTERNAL_TRADES = "Save Internal Trades";
    public static final String AGGREGATION_BY_SUB_ACCOUNT = "Aggregation By SubAccount";
    private transient CATradeSaverCancellableJob __caTradeSaver;


    public static List<AttributeDefinition> delegateAttributeDefifinition() {
        ScheduledTaskSANT_RF_CA_APPLY stCA = new ScheduledTaskSANT_RF_CA_APPLY();
        return stCA.buildSpecificAttributeDefinition();
    }


    public static boolean delegateValidationInput(ScheduledTaskSANT_CORPORATE_ACTION stCA) throws IOException {
        ScheduledTaskSANT_RF_CA_APPLY stApplyCA = new ScheduledTaskSANT_RF_CA_APPLY();
        Map<String, ?> fieldValues = ReflectUtil.getFieldValues(stCA, Object.class);
        ReflectUtil.setFieldValues(stApplyCA, Object.class, fieldValues);
        stApplyCA.__attributes = stCA.getAttributes();
        stApplyCA._messages = stCA._messages;
        return stApplyCA.specificValidationInput();
    }


    public static String delegateProcessRequest(ScheduledTaskSANT_CORPORATE_ACTION stCA, DSConnection ds, TaskArray tasks) throws IOException {
        ScheduledTaskSANT_RF_CA_APPLY stApplyCA = new ScheduledTaskSANT_RF_CA_APPLY();
        Map<String, ?> fieldValues = ReflectUtil.getFieldValues(stCA, Object.class);
        ReflectUtil.setFieldValues(stApplyCA, Object.class, fieldValues);
        stApplyCA.__attributes = stCA.getAttributes();
        return stApplyCA.handleProcess(ds, tasks);
    }


    public ScheduledTaskSANT_RF_CA_APPLY() {
    }


    public String getTaskInformation() {
        return this.getClass().getSimpleName() + " creates the underlying products if necessary, and creates or updates the associated Trades for further processing.";
    }


    protected String handleProcess(DSConnection ds, TaskArray tasks) {
        String exec = null;
        CAGenerationContext caGenerationContext = this.caGenerationContextBuilder(ds).caIds(this.getCAIds()).build();
        exec = this.taskApplyCA(caGenerationContext.getUnderlyings(), caGenerationContext, tasks);
        return exec;
    }


	private String taskApplyCA(Set<Product> products, CAGenerationContext info, TaskArray tasks) {
		Collection<CA> applicableCA = null;
		DSConnection ds = this.getReadOnlyConnection(this.getDSConnection());
		PricingEnv env = info.getPricingEnv();
		String exec = null;
		TradeFilter underlyingFilter = this.getFilter(this.getTradeFilter());
		try {
			if (!CAElectionUtil.getElectionProcessDateRangeDomain().contains(info.getProcessDate())) {
				applicableCA = CAGenerationHandlerUtil.loadApplicableCAs(products, info);
			} else {
				applicableCA = CAElectionUtil.loadApplicableCAs(this);
			}
			applicableCA = applicableCA instanceof Set ? applicableCA
					: (applicableCA == null ? Collections.EMPTY_SET : new LinkedHashSet(applicableCA));
			AnalysisProgressUtil.logProgressResult(
					"Number of CA product to apply for this period : " + ((Collection) applicableCA).size());
		} catch (Exception var17) {
			exec = "ScheduledTaskSANT_CORPORATE_ACTIONError Loading CAs";
			AnalysisProgressUtil.logError(exec, var17);
			return exec;
		}
		if (Util.isEmpty((Collection) applicableCA)) {
			AnalysisProgressUtil.logProgressResult("No CA to apply.");
			return exec;
		} else {
			boolean isPositionProcess = this.isPositionProcess();
			String s = this.getAttribute("POSITION Trade Filter");
			TradeFilter positionFilter = Util.isEmpty(s) ? underlyingFilter : BOCache.getTradeFilter(ds, s);
			if (positionFilter == null) {
				AnalysisProgressUtil.logProgressDetail("Could not load POSITION Trade Filter: " + s);
				positionFilter = underlyingFilter;
			}
			boolean isOTCProcess = this.isOTCProcess() || this.isStructuredProcess();
			TradeFilter otcTradeFilter = this.getFilter(this.getAttribute("OTC Trade Filter"));
			try {
				if (this.isStructuredProcess()) {
					if (otcTradeFilter == null) {
						otcTradeFilter = new TradeFilter("StructuredProduct", "StructuredProduct");
					}
					otcTradeFilter.setCriterion("ProductType", new Vector(Arrays.asList("StructuredProduct")));
				}
				if (!Util.isEmpty(this._allLE)) {
					Vector<String> leCodes = new Vector();
					Iterator var15 = this._allLE.iterator();
					while (var15.hasNext()) {
						LegalEntity le = (LegalEntity) var15.next();
						leCodes.add(le.getCode());
					}
					if (positionFilter != null) {
						positionFilter = (TradeFilter) positionFilter.clone();
						positionFilter.setCriterion("LegalEntity", leCodes);
					}
					if (otcTradeFilter != null) {
						otcTradeFilter.setCriterion("LegalEntity", leCodes);
					}
				}
				CAApplyInfo caApplyInfo = CAApplyInfoBuilder.builder((Collection) applicableCA, env)
						.positionBasedTradeFilter(positionFilter).otcTradeFilter(otcTradeFilter)
						.positionType(this.getBOPositionType()).isByBOPositionTradeDate(this.isByBOPositionTradeDate())
						.isApplyToPosition(isPositionProcess).isApplyToOTC(isOTCProcess)
						.isApplyToBasket(this.isApplyToBasket()).threadPoolSize(this.getThreadCount())
						.executionDate(JDate.valueOf(this.getValuationDatetime(true), this.getTimeZone()))
						.inventoryAggregationType(this.getAttribute("BO_POSITION_AGGREGATION_TYPE"))
						.isApplyOnRepoedPLPosition(Util.isTrue(this.getAttribute("PL_POSITION_REPOED")))
						.balanceType(this.getAttribute("BO_POSITION_BALANCE_TYPE"))
						.isApplyOnMarginCall(Util.isTrue(this.getAttribute("Apply to MarginCall")))
						.isSaveInternalTrades(Util.isTrue(this.getAttribute("Save Internal Trades"), true))
						.batchSize(this.getBatchSize())
						.isAggregationBySubAccount(Util.isTrue(this.getAttribute("Aggregation By SubAccount"), false))
						.build();
				AnalysisProgressUtil.logProgressStep(caApplyInfo.toString());
				this.setParallelTask(caApplyInfo);
				TradeArray trades = (TradeArray) this.parallelRun(ds, (PSConnection) null);
				FilterSet fs = this.getFilterSet() != null ? BOCache.getFilterSet(ds, this.getFilterSet()) : null;
				// Filtrado por FilterSet
				TradeArray tradesToProcess = new TradeArray();
				if (trades != null && trades.size() > 0) {
					for (Trade trade : trades.getTrades()) {
						if (fs != null && !fs.accept(trade)) {
							continue;
						} else {
							tradesToProcess.add(trade);
						}
					}
				}
				exec = this.saveTrades(getReadWriteDS(ds), tradesToProcess, caApplyInfo);
				Collection<Task> exceptionTasks = caApplyInfo.getExceptionTasks();
				tasks.add((Task[]) exceptionTasks.toArray(new Task[exceptionTasks.size()]), exceptionTasks.size());
			} catch (Throwable var18) {
				exec = "Error applying CA(s) " + var18.getMessage();
				AnalysisProgressUtil.logError(exec, var18);
			}
			return exec;
		}
	}


    protected List<AttributeDefinition> buildSpecificAttributeDefinition() {
        List<AttributeDefinition> attributes = new ArrayList();
        Vector positionBalanceTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "CABOPositionBalanceType");
        positionBalanceTypes.add(InventorySecurityPosition.BALANCE_DEFAULT);
        positionBalanceTypes.add(InventorySecurityPosition.BALANCE_TRADING);
        String category = "Apply CA on position based product type";
        attributes.addAll(Arrays.asList(attribute("CAID").category(category).description("CA identifier").type(String.class), attribute("POSITION_PROCESS").category(category).description("true or false").type(Boolean.class), attribute("BO_POSITION").category(category).description("inventory position type").domain(Arrays.asList("ACTUAL", "THEORETICAL")), attribute("BO_POSITION_DATE").category(category).description("inventory position date type").domain(Arrays.asList("SETTLE", "TRADE")), attribute("BO_POSITION_AGGREGATION_TYPE").category(category).description("inventory position aggregation type - in the case when inventory security position is computed for a finer level than Book/agent/Account").domainName("InventoryAggregations"), attribute("BO_POSITION_BALANCE_TYPE").category(category).description("inventory position movement type: 'Balance' includes trading and SecFinance while 'Balance Trading' exclude SecFinance (repo, secLending)").domain(positionBalanceTypes), attribute("POSITION Trade Filter").category(category).description("filter positions according with applicable TradeFilter criteria").domain(AccessUtil.getAllNames(5)), attribute("PL_POSITION_REPOED").category(category).description("true or false (default). If true, generates CA internal (PO) Trade from trading position, including repoed quantites").type(Boolean.class), attribute("Aggregation By SubAccount").category(category).description("true or false").type(Boolean.class)));
        category = "Apply CA on OTC product type";
        attributes.addAll(Arrays.asList(attribute("OTC_PROCESS").category(category).description("true or false").type(Boolean.class), attribute("OTC Trade Filter").category(category).description("filter OTC Trade according with TradeFilter criteria, including product type").domain(AccessUtil.getAllNames(5)), attribute("STRUCTURED_PROCESS").category(category).description("true or false to apply on StructuredProduct type").type(Boolean.class), attribute("Apply to basket").category(category).description("true or false to apply CA to reference basket of securities").type(Boolean.class), attribute("Apply to MarginCall").category(category).description("true or false").type(Boolean.class), attribute("Save Internal Trades").category(category).description("true or false").type(Boolean.class)));
        return attributes;
    }


    protected boolean specificValidationInput() {
        boolean ret = true;
        String s = this.getAttribute("OTC Trade Filter");
        if (this.isOTCProcess() && Util.isEmpty(s)) {
            this._messages.add("Please select an OTC Trade Filter in the Attribute for OTC Process");
            ret = false;
        }
        return ret;
    }


    public IExecutionContext createExecutionContext(DSConnection ds, PSConnection ps) throws ParallelExecutionException {
        return this.getParallelTask().createExecutionContext(ds, ps);
    }


    public List<? extends Callable<Object>> split(IExecutionContext context) throws ParallelExecutionException {
        return this.getParallelTask().split(context);
    }


    public TradeArray epilogue(List<? extends Callable<Object>> jobs, List<Future<Object>> futures, IExecutionContext context) throws ParallelExecutionException {
        return (TradeArray)this.getParallelTask().epilogue(jobs, futures, context);
    }


    public void dirtyShutdown() {
        super.dirtyShutdown();
        if (this.__caTradeSaver != null) {
            this.__caTradeSaver.jobCanceled((Object)null, (AnalysisProgress)null);
        }
    }


    private String saveTrades(DSConnection ds, TradeArray trades, CAApplyInfo caApplyInfo) throws InterruptedException, ExecutionException {
        List<String> errorMessages = Collections.synchronizedList(new ArrayList());
        this.__caTradeSaver = new CATradeSaverCancellableJob(ds, trades, errorMessages, false, caApplyInfo);
        this.__caTradeSaver.performSave();
        if (trades != null) {
            for(int i = 0; i < trades.size(); ++i) {
                trades.get(i);
            }
        }
        String exec = Util.isEmpty(errorMessages) ? null : "Error Processing";
        Iterator var6 = errorMessages.iterator();

        while(var6.hasNext()) {
            String errorMessage = (String)var6.next();
            caApplyInfo.addExceptionTask(this.createException(errorMessage));
        }
        return exec;
    }


    private String getBOPositionType() {
        String value = this.getAttribute("BO_POSITION");
        return Util.isEmpty(value) ? "ACTUAL" : value;
    }


    private boolean isByBOPositionTradeDate() {
        return "TRADE".equals(this.getAttribute("BO_POSITION_DATE"));
    }


    private boolean isOTCProcess() {
        return this.getBooleanAttribute("OTC_PROCESS", false);
    }


    private boolean applyToMarginCall() {
        return this.getBooleanAttribute("Apply to MarginCall", false);
    }


    private boolean isPositionProcess() {
        return this.getBooleanAttribute("POSITION_PROCESS", true);
    }


    private boolean isStructuredProcess() {
        return this.getBooleanAttribute("STRUCTURED_PROCESS", false);
    }


    private boolean isApplyToBasket() {
        return this.getBooleanAttribute("Apply to basket", this.isOTCProcess());
    }


    protected List<String[]> initTradeFilter() {
        List<String[]> tradeFilters = new ArrayList();
        tradeFilters.add(new String[]{"CA Underlying TradeFilter", this.getTradeFilter()});
        tradeFilters.add(new String[]{"OTC Trade Filter", this.getAttribute("OTC Trade Filter")});
        tradeFilters.add(new String[]{"POSITION Trade Filter", this.getAttribute("POSITION Trade Filter")});
        return tradeFilters;
    }


    protected List<String> getProcessDateDomain() {
        List<String> processDateDomain = super.getProcessDateDomain();
        processDateDomain.addAll(Arrays.asList("From Ex To Record Date", "From Record To Value Date"));
        processDateDomain.addAll(CAElectionUtil.getElectionProcessDateRangeDomain());
        return processDateDomain;
    }
}
